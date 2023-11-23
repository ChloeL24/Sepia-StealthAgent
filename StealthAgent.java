package src.pa1.agents;

// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;


// JAVA PROJECT IMPORTS prof doc 
import edu.bu.pa1.distance.DistanceMetric;
import edu.bu.pa1.graph.Vertex;
import edu.bu.pa1.graph.Path;



public class StealthAgent
    extends Agent
{

   
    // Fields of this class
    // TODO: add your fields here! For instance, it might be a good idea to
    // know when you've killed the enemy townhall so you know when to escape!
    
    private Vertex nextVertexToMoveTo;      // the next coordinate to move to
    private boolean isStuck;                // are we stuck? 
    private int myUnitID;
    private int enemyTownhallUnitID;        // the target enemy unit (who we want to destroy)
    private Set<Integer> otherEnemyUnitIDs;  // all other enemies on the map (we are alone)
    private boolean isEnemyTownhallDead; 
    private Vertex OriginalLocation;         //orginal location 

    // TODO: implement the state machine for following a path once we calculate it
    //       this will for sure adding your own fields.
    
    private final int enemyUnitSightRadius;

    //-----------------------constructor------------------------------------------------------
    public StealthAgent(int playerNum, String[] args)
    {
        super(playerNum);

        // set these fields to some invalid state and populate them in initialStep()
        this.myUnitID = -1;
        this.enemyTownhallUnitID = -1;
        this.otherEnemyUnitIDs = null;
        this.nextVertexToMoveTo = null;
        this.isEnemyTownhallDead = false;
        this.isStuck = false;
        this.OriginalLocation = null;
    // TODO: make sure to initialize your fields (to some invalid state) here!
       
        int enemyUnitSightRadius = -1;
        if(args.length == 2)
        {
            try
            {
                enemyUnitSightRadius = Integer.parseInt(args[1]);

                if(enemyUnitSightRadius <= 0)
                {
                    throw new Exception("ERROR");
                }
            } catch(Exception e)
            {
                System.err.println("ERROR: [StealthAgent.StealthAgent]: error parsing second arg=" + args[1]
                    + " which should be a positive integer");
            }
        } else
        {
            System.err.println("ERROR [StealthAgent.StealthAgent]: need to provide a second arg <enemyUnitSightRadius>");
            System.exit(-1);
        }

        this.enemyUnitSightRadius = enemyUnitSightRadius;
    }
 //--------------------------------------------------------------end of constructor-------------------------------------------------
// TODO: add some getter methods for your fields! Thats the java way to do things!


///GETTER METHODS----------------------------------------------------------------------------------------------------------------------
    public int getMyUnitID() { return this.myUnitID; }
    public int getEnemyTownhallUnitID() { return this.enemyTownhallUnitID; }
    public final Set<Integer> getOtherEnemyUnitIDs() { return this.otherEnemyUnitIDs; }
    public final Vertex getNextVertexToMoveTo() { return this.nextVertexToMoveTo; }
    public final int getEnemyUnitSightRadius() { return this.enemyUnitSightRadius; }
    public boolean getIsStuck() { return this.isStuck; }
    public Vertex getOrginalLocation() {return this.OriginalLocation;}

    // TODO: add some setter methods for your fields if they need them! Thats the java way to do things!
    private void setMyUnitID(int id) { this.myUnitID = id; }
    private void setEnemyTownhallUnitID(int id) { this.enemyTownhallUnitID = id; }
    private void setOtherEnemyUnitIDs(Set<Integer> s) { this.otherEnemyUnitIDs = s; }
    private void setNextVertexToMoveTo(Vertex v) {this.nextVertexToMoveTo = v; }
    private void setIsStuck(boolean isStuck) {this.isStuck = isStuck; }
   

    //make it a feild in the class, then in initial step, store the original location
//---------------------------------------------------------------------------------------------------------------------------------------
    /**
        TODO: if you add any fields to this class it might be a good idea to initialize them here
              if they need sepia information!
     */
    @Override
    public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
        // this method is typically used to discover the units in the game.
        // any units we want to pay attention to we probably want to store in some fields

        // first find out which units are mine and which units aren't
        Set<Integer> myUnitIDs = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(this.getPlayerNumber()))
        {
            myUnitIDs.add(unitID);
        }

        // should only be one unit controlled by me
        if(myUnitIDs.size() != 1)
        {
            System.err.println("ERROR: should only be 1 unit controlled by player=" +
                this.getPlayerNumber() + " but found " + myUnitIDs.size() + " units");
            System.exit(-1);
        } else
        {
            this.setMyUnitID(myUnitIDs.iterator().next()); // get the one unit id
        }
        
		//return this.middleStep(state, history);
        // there can be as many other players as we want, and they can controll as many units as they want,
        // but there should be only ONE enemy townhall unit
        Set<Integer> enemyTownhallUnitIDs = new HashSet<Integer>();
        Set<Integer> otherEnemyUnitIDs = new HashSet<Integer>();
        for(Integer playerNum : state.getPlayerNumbers())
        {
            if(playerNum != this.getPlayerNumber())
            {
                for(Integer unitID : state.getUnitIds(playerNum))
                {
                    if(state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("townhall"))
                    {
                        enemyTownhallUnitIDs.add(unitID);
                    } else
                    {
                        otherEnemyUnitIDs.add(unitID);
                    }
                }
            }
        }

        // should only be one unit controlled by me
        if(enemyTownhallUnitIDs.size() != 1)
        {
            System.err.println("ERROR: should only be 1 enemy townhall unit present on the map but found "
                + enemyTownhallUnitIDs.size() + " such units");
            System.exit(-1);
        } else
        {
            this.setEnemyTownhallUnitID(enemyTownhallUnitIDs.iterator().next()); // get the one unit id
            this.setOtherEnemyUnitIDs(otherEnemyUnitIDs);
        }

        //this.OriginalLocation = this.getCurrentLocation(state);
        return this.middleStep(state, history);
    }
    /**
        TODO: implement me! This is the method that will be called every turn of the game.
              This method is responsible for assigning actions to all units that you control
              (which should only be a single footman in this game)
     */
    @Override
    public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
        if(history == null){
            this.OriginalLocation = this.getCurrentLocation(state);
        }
       
        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        UnitView myUnit = state.getUnit(this.getMyUnitID());
        int myUnitx = myUnit.getXPosition();
        int myUnity = myUnit.getYPosition();
        Vertex currentPosition = new Vertex(myUnitx,myUnity);
    
        // get enemytownhall location
        Vertex enemyTargetCoordinate = getTownhallLocation(state);

        if(DistanceMetric.manhattanDistance(currentPosition, enemyTargetCoordinate) <= 1)
            {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(this.getMyUnitID(), Action.createPrimitiveAttack(this.getMyUnitID(),
                                                                             getEnemyTownhallUnitID())); 
                                                                             //getEnemyTownhallUnitID();
            } else //not adjacent to townhall and townhall is still alive...try to get there
            
            {
                // if we don't have a place to go to yet OR we've just arrived at the place we want to go to
            
                if(this.getNextVertexToMoveTo() == null || this.getNextVertexToMoveTo().equals(currentPosition))
            	{
                    if(this.getNextVertexToMoveTo() == null){ //we just started/first run 
                        //Vertex orignalLocation = getCurrentLocation(state);
                        //System.out.println(search(currentPosition, getNextPostion(state), state));
                        search(currentPosition, getNextPostion(state), state);

                    }

                    else if (townHallDestroyed(state)){
                        search(currentPosition, this.getOrginalLocation(),state); //how do i navigate back to where i came from? 
                    } 

                }
            		//this.setNextVertexToMoveTo(this.getNextPosition(currentPosition,
                                                                   // enemyTargetCoordinate,
                                                                    //state));
            
                if(this.shouldReplacePlan(state) || townHallDestroyed(state)){
                        search(currentPosition,getNextPostion(state),state);
                        //System.out.println(search(currentPosition,getNextPostion(state),state));
                    }
        /**
            I would suggest implementing a state machine here to calculate a path when neccessary.
            For instance beginning with something like:
  
/**             then after this, worry about how you will follow this path by submitting sepia actions
            the trouble is that we don't want to move on from a point on the path until we reach it
            so be sure to take that into account in your design

            once you have this working I would worry about trying to detect when you kill the townhall
            so that you implement escaping
         */

            Direction nextDirection = this.getDirection(currentPosition,
                                                              enemyTargetCoordinate);		//null error here	old: this.getNextVertexToMoveTo()
            actions.put(this.getMyUnitID(), Action.createPrimitiveMove(this.getMyUnitID(),
                                                                        nextDirection));
  }
  return actions;
    
}

    public Vertex getNextPostion(StateView state){

        Vertex src = this.getCurrentLocation(state);
        
        Set<Vertex> OutgoingNeighbors= this.getOutgoingNeighbors(this.getCurrentLocation(state),
                                            state);
           
        List <Float> edgeweights = new ArrayList<>();

        for (float edgeweight: edgeweights){
            edgeweights.add(edgeweight);
        }
        Vertex use = getVertexWithSmallestEdgeWeight(OutgoingNeighbors, src, edgeweights,state);
        return use;
    }

    public Vertex getVertexWithSmallestEdgeWeight(Set <Vertex> OutgoingNeighbors,Vertex src, List <Float> edgeweights, StateView state) {
            Float minWeight = Float.POSITIVE_INFINITY; // Initialize with a large value
            
            Vertex smallestVertex = null;
            for (Vertex neighbor :OutgoingNeighbors) {
                if (getEdgeWeight(state,src,neighbor) < minWeight) {
                    minWeight = getEdgeWeight(state,src,neighbor);
                    smallestVertex = neighbor;
                    //smallestVertex = edge.getDestination(); // or edge.getSource() depending on your edge representation
                }
            }
            return smallestVertex; 
        }

    public Vertex getCurrentLocation(StateView state){
        UnitView myUnit = state.getUnit(this.getMyUnitID());
        int myUnitx = myUnit.getXPosition();
        int myUnity = myUnit.getYPosition();
        Vertex currentPosition = new Vertex(myUnitx,myUnity);
        return currentPosition;
    }

    public Vertex getTownhallLocation(StateView state){
        UnitView enemyTownhallUnitIDs = state.getUnit(getEnemyTownhallUnitID());
        int x = enemyTownhallUnitIDs.getXPosition();
        int y = enemyTownhallUnitIDs.getYPosition();
        Vertex location = new Vertex(x,y);
        return location;
    }

     
    public Integer getEnemyTargetUnitID(StateView state){
        //List <Integer> myUnitIds = state.getUnit(this.getMyUnitID()); //list of enemy unit ids
        List<Integer> townhallIds = new ArrayList<Integer>();
        List<Integer> others = new ArrayList<Integer>();
        for(Integer unitID : townhallIds)
        {
            UnitView unit = state.getUnit(unitID);
            String unitTypeName = unit.getTemplateView().getName();
                if(unitTypeName.equals("TownHall")){
                        townhallIds.add(unitID);
                 //if i should be always gettting 0, but im thinking that there will only be one townhall
                }
                else{
                   others.add(unitID); 
                }
            //return townhallIds.get(0);
        }
        return townhallIds.get(0);
    }

   public boolean townHallDestroyed(StateView state){
    if (state.getUnit(getEnemyTownhallUnitID())== null){
        return true;
    }
    else{
        return false;
    }
   }

    // Please don't mess with this
    @Override
    public void terminalStep(StateView state,
                             HistoryView history)
    {
        boolean isMyUnitDead = state.getUnit(this.getMyUnitID()) == null;
        boolean isEnemyTownhallDead = state.getUnit(this.getEnemyTownhallUnitID()) == null;

        if(isMyUnitDead)
        {
            System.out.println("mission failed");
        } else if(isEnemyTownhallDead)
        {
            System.out.println("mission success");
        } else
        {
            System.out.println("how did we get here? Both my unit and the enemy townhall are both alive?");
        }
    }

    // You probably dont need to mess with this: we dont need to save our agent
    @Override
    public void savePlayerData(OutputStream os) {}

    // You probably dont need to mess with this: we dont need to load our agent from disk
    @Override
    public void loadPlayerData(InputStream is) {}
    /**
        TODO: implement me! This method should return "true" WHEN the current plan is bad,
              and return "false" when the path is still valid. I would recommend including
              figuring out when:
                    - the path you created is not blocked by another unit on the map (that has moved)
                    - you are getting too close to an enemy unit that is NOT the townhall
                        Remember, if you get too close to the enemy units they will kill you!
                        An enemy will see you if you get within a chebyshev distance of this.getEnemyUnitSightRadius()
                        squares away
     */
    public boolean shouldReplacePlan(StateView state)
    {
        List <Vertex> enemyLocs = new ArrayList <Vertex>();//make list of all enemy locations 
        for (int enemyUnitID: this.getOtherEnemyUnitIDs()){
            UnitView enemies = state.getUnit(enemyUnitID);
            enemyLocs.add(new Vertex(enemies.getXPosition(), enemies.getYPosition()));
        }
        int attackRadius = getEnemyUnitSightRadius();  //enemy sight radius
  
    //An enemy will see you if you get within a chebyshev distance of this.getEnemyUnitSightRadius()squares away
    Set <Vertex> enemyRadiusVertex = new HashSet<Vertex>(); //index through the enemy locatioins and store vertexes that are in their radius
    
    for (int u = 0; u < enemyLocs.size(); u++){ //building a radius around each enemy
        Vertex up = new Vertex((enemyLocs.get(u).getXCoordinate()),(enemyLocs.get(u).getYCoordinate()+this.getEnemyUnitSightRadius()));// at single vertex, look up
        Vertex down = new Vertex((enemyLocs.get(u).getXCoordinate()),(enemyLocs.get(u).getYCoordinate()-this.getEnemyUnitSightRadius())); //look down
        Vertex left = new Vertex ((enemyLocs.get(u).getXCoordinate()-this.getEnemyUnitSightRadius()),(enemyLocs.get(u).getYCoordinate())); //look left
        Vertex right = new Vertex((enemyLocs.get(u).getXCoordinate()+this.getEnemyUnitSightRadius()),(enemyLocs.get(u).getYCoordinate())); //look right
        Vertex upRight = new Vertex((enemyLocs.get(u).getXCoordinate()+ this.getEnemyUnitSightRadius()), (enemyLocs.get(u).getYCoordinate() + this.getEnemyUnitSightRadius())); // up right diagonal
        Vertex DownLeft = new Vertex((enemyLocs.get(u).getXCoordinate()-this.getEnemyUnitSightRadius()), (enemyLocs.get(u).getYCoordinate() - this.getEnemyUnitSightRadius())); // down left
        Vertex UpLeft = new Vertex((enemyLocs.get(u).getXCoordinate()-this.getEnemyUnitSightRadius()), (enemyLocs.get(u).getYCoordinate()+this.getEnemyUnitSightRadius())); // up left
        Vertex DownRight = new Vertex((enemyLocs.get(u).getXCoordinate()+this.getEnemyUnitSightRadius()), (enemyLocs.get(u).getYCoordinate() - this.getEnemyUnitSightRadius())); // down right

        enemyRadiusVertex.add(up);
        enemyRadiusVertex.add(down);
        enemyRadiusVertex.add(left);
        enemyRadiusVertex.add(right);
        enemyRadiusVertex.add(upRight);
        enemyRadiusVertex.add(DownLeft);
        enemyRadiusVertex.add(UpLeft);
        enemyRadiusVertex.add(DownRight);
    }

      UnitView myUnitView = state.getUnit(this.getMyUnitID());
      Vertex currentLoc = getCurrentLocation(state);
    
      if((enemyRadiusVertex.contains(currentLoc.getXCoordinate()+2)) || (enemyRadiusVertex.contains(currentLoc.getYCoordinate()+2)) || (enemyRadiusVertex.contains(currentLoc.getXCoordinate() -2)) || (enemyRadiusVertex.contains(currentLoc.getYCoordinate()-2))){
        return true; //if the next spot we go to is in the radius// if we're close to the radius. do we really need this though?? this may elimate too many spaces. but, by the time we're in the radius, we will be killed
       }
       else{
        return false;
       }

    }

    /**
        TODO: implement me! a helper function to get the outgoing neighbors of a vertex. //GOT FROM DIJKSTRA FILE
     */
    public Set<Vertex> getOutgoingNeighbors(Vertex src,
                                            StateView state)
    {
        Set<Vertex> outgoingNeighbors = new HashSet<Vertex>();
        int xCoord = src.getXCoordinate();
        int yCoord = src.getYCoordinate();

        //Vertex newVertex = null;
        for(int x = xCoord - 1; x <= xCoord + 1; x++)
        {
            for(int y = yCoord - 1; y <= yCoord + 1; y++)
            {
                // a neighbor has to satisfy the following criteria:
                //   - the vertex has to be in bounds
                //   - the vertex cannot be occupied by a resource (like a tree)
                //   - the vertex cannot be the same one as the src
                //   - the vertex cannot be occupied by an enemy unit UNLESS it is the enemy target id
                if(state.inBounds(x, y) && !state.isResourceAt(x, y) && (x != xCoord || y != yCoord) &&
                  (state.unitAt(x, y) == null || !this.getOtherEnemyUnitIDs().contains(state.unitAt(x,y))))
                {
                    outgoingNeighbors.add(new Vertex(x, y));
                }
            }
        }
        return outgoingNeighbors;
    }

    /**
        TODO: implement me! a helper function to get the edge weight of going from "src" to "dst"
              I would recommend discouraging your agent from getting near an enemy by producing
              really large edge costs for going to a vertex that is within the sight of an enemy
     */
    public float getEdgeWeight(StateView state,
                               Vertex src,
                               Vertex dst) //dst can be any vertex, not the final destination 
    {

        //get enemy locations
       // if that dst is in enemy radius, make the edge weight high
       //enemy location plus radius in every direction

        //List <Integer> enemyUnitIds = state.getUnitIds(this.enemyTownhallUnitID); //list of enemy unit ids
        List <Vertex> enemyLocs = new ArrayList<Vertex>();//make list of all enemy locations 

        int attackRadius = getEnemyUnitSightRadius();  //enemy sight radius
            for (int enemyUnitID: this.getOtherEnemyUnitIDs()){
            UnitView enemies = state.getUnit(enemyUnitID);
            enemyLocs.add(new Vertex(enemies.getXPosition(), enemies.getYPosition()));
        }

    //An enemy will see you if you get within a chebyshev distance of this.getEnemyUnitSightRadius()squares away
                        
    Set <Vertex> enemyRadiusVertex = new HashSet<Vertex>(); //index through the enemy locatioins and store vertexes that are in their radius
    
    for (int u = 0; u < enemyLocs.size(); u++){
        Vertex up = new Vertex((enemyLocs.get(u).getXCoordinate()),(enemyLocs.get(u).getYCoordinate()+this.getEnemyUnitSightRadius()));// at single vertex, look up, down, left, right
        Vertex down = new Vertex((enemyLocs.get(u).getXCoordinate()),(enemyLocs.get(u).getYCoordinate()-this.getEnemyUnitSightRadius()));
        Vertex left = new Vertex ((enemyLocs.get(u).getXCoordinate()-this.getEnemyUnitSightRadius()),(enemyLocs.get(u).getYCoordinate()));
        Vertex right = new Vertex((enemyLocs.get(u).getXCoordinate()+this.getEnemyUnitSightRadius()),(enemyLocs.get(u).getYCoordinate()));

        enemyRadiusVertex.add(up);
        enemyRadiusVertex.add(down);
        enemyRadiusVertex.add(left);
        enemyRadiusVertex.add(right);
    }

       int x = src.getXCoordinate();
       int y = src.getYCoordinate();

       if (enemyRadiusVertex.contains(dst)){
        return Float.POSITIVE_INFINITY;
       }else{
        return 1f;
       }      
  }
    /**
        TODO: implement me! This method should implement your A* search algorithm, which is very close
              to how dijkstra's algorithm works, but instead uses an estimated total cost to the goal
              to sort rather than the known cost
     */
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state) //search isnt direct. print path and see if its a stright
    {
        Path pathToTake = null;
        
        PriorityQueue<Path> heap = new PriorityQueue<Path>(
			new Comparator<Path>()
		{
			public int compare(Path a, Path b)
			{
				return Float.compare(a.getEstimatedPathCostToGoal(), b.getEstimatedPathCostToGoal());
			}
		});

        Set<Vertex> finalizedVertices = new HashSet<Vertex>();
		Map<Path, Float> path2Cost = new HashMap<Path, Float>();

        Path initPath = new Path(src);
		heap.add(initPath);
		path2Cost.put(initPath, initPath. getEstimatedPathCostToGoal());
		while(!heap.isEmpty())
        {
			Path currentMinPath = heap.poll();
			finalizedVertices.add(currentMinPath.getDestination());
			
			if(currentMinPath.getDestination().equals(goal))
			{
				return currentMinPath.getParentPath();
			}

			for(Vertex neighbor : this.getOutgoingNeighbors(currentMinPath.getDestination(), state)) 
			{
				if(!finalizedVertices.contains(neighbor))
				{
                    Path childPath = new Path(neighbor, //dst
                    (getEdgeWeight(state,src,goal)), //edge cost
                                              currentMinPath);//parent path


					// childPath (the coordinate) could be brand new OR
					// already seen before (but not popped from the heap yet)
					if(!path2Cost.containsKey(childPath))
					{
						// brand new vertex
						heap.add(childPath);
						path2Cost.put(childPath, childPath.
                        getEstimatedPathCostToGoal());
					} else
					{
						// we have two paths to the same destination!
						// one in the heap (the "old" one), and childPath!

						// get the "old" cost
						Float oldCost = path2Cost.get(childPath);

						// if the old cost is worse than childPath.f
						if(oldCost > childPath.getEstimatedPathCostToGoal())
						{
							// delete the old path from our heap and add childPath to the heap
							heap.remove(childPath);
							heap.add(childPath);
							path2Cost.replace(childPath, childPath.getEstimatedPathCostToGoal());
						}
					}
				}
			}
		}
        return null;
    }
    /**
        A helper method to get the direction we will need to go in order to go from src to an adjacent
        vertex dst. Knowing this direction is necessary in order to create primitive moves in Sepia which uses
        the following factory method:
            Action.createPrimitiveMove(<unitIDToMove>, <directionToMove>);
     */
    protected Direction getDirection(Vertex src,
                                     Vertex dst)
    {
        int xDiff = dst.getXCoordinate() - src.getXCoordinate();
        int yDiff = dst.getYCoordinate() - src.getYCoordinate();

        Direction dirToGo = null;

        if(xDiff == 1 && yDiff == 1)
        {
            dirToGo = Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            dirToGo = Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            dirToGo = Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            dirToGo = Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            dirToGo = Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            dirToGo = Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            dirToGo = Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            dirToGo = Direction.NORTHWEST;
        } else
        {
            System.err.println("ERROR: src=" + src + " and dst=" + dst + " are not adjacent vertices");
        }

        return dirToGo;
    }
}

