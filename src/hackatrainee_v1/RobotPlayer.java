package hackatrainee_v1;
import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;
    
    // Mobile robot variables
    static int masterID = 0;
    static MapLocation destination;
    
    // Enlightenment Center variables
    static HashMap<RobotType, ArrayList<Integer>> minionIDs =
    		new HashMap<RobotType, ArrayList<Integer>>(); // robots created and commanded by this EC
    static int buildIdx = 0;
    static RobotType[] buildQueue = {
    		spawnableRobot[0],
    		spawnableRobot[1],
    		spawnableRobot[2]
    };
    static HashMap<MapLocation, ECInfo> knownECs = new HashMap<MapLocation, ECInfo>();
    static int prevVotes = 0; 
    static int bidInfluence = 1;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
	    if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER) {
        	for (RobotType rt : RobotType.values()) {
	        	minionIDs.put(rt, new ArrayList<Integer>());
	        }
	    } else { // Find EC that probably created this robot
	        for (Direction dir : directions) {
	        	try {
	        		RobotInfo maybeEC = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
		            if (maybeEC != null && maybeEC.getType() == RobotType.ENLIGHTENMENT_CENTER) {
		            	masterID = maybeEC.getID();
		            }
	        	} catch (GameActionException e) {
	        		// adjacent location not on the map
	        		continue;
	        	}
	        }
	    }
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
    	// Prune minions, some may have been lost
    	for (ArrayList<Integer> minionList : minionIDs.values()) {
    		ArrayList<Integer> minionsCopy = new ArrayList<Integer>(minionList);
    		for (int id : minionsCopy) {
	    		try {
	    			RobotInfo minion = rc.senseRobot(id);
	    			if (minion.getTeam() != rc.getTeam()) {  // remote possibility of same ID occurring again?
	    				throw new GameActionException(null, null);
	    			}
	    		} catch (GameActionException e) {
	    			minionList.remove(Integer.valueOf(id));
	    		}
    		}
    	}
    	
    	// Get flag info from remaining minions
    	for (ArrayList<Integer> minionList : minionIDs.values()) {
    		for (int id : minionList) {
    			try {
    				int flagInt = rc.getFlag(id);
    				FlagInfo fi = new FlagInfo();
    				fi.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
    				if (fi.signaling) {
    					MapLocation location = fi.location;
    					Team team = fi.team;
    					knownECs.put(location, new ECInfo(location, team));
    					System.out.println(knownECs.size());
    				}
    			} catch (GameActionException e) {
                    System.out.println(rc.getType() + " Exception");
                    e.printStackTrace();
    			}
    		}
    	}
    	
        // Produce new robots
    	RobotType buildType = buildQueue[0];
        int influence = 50;
        for (Direction dir : directions) {
        	buildType = buildQueue[buildIdx];
            if (rc.canBuildRobot(buildType, dir, influence)) {
                rc.buildRobot(buildType, dir, influence);
                // Add the newly built robot to the minions list
                RobotInfo freshMinion = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
                minionIDs.get(buildType).add(freshMinion.getID());
                buildIdx = (buildIdx + 1) % buildQueue.length;
                buildType = buildQueue[buildIdx];
            }
        }
        // Make bids for votes
        int currentVotes = rc.getTeamVotes();
        if (currentVotes == prevVotes) { // We did not win the vote
        	bidInfluence += 1;
        } else {
        	bidInfluence = Math.max(1, bidInfluence - 1);
        }
        prevVotes = currentVotes;
        int currentInfluence = rc.getInfluence();
        if (bidInfluence < currentInfluence) {
        	rc.bid(bidInfluence);
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        boolean tryAttack = false;
    	for (RobotInfo r : attackable) {
    		if (r.getType() == RobotType.MUCKRAKER) {
    			tryAttack = true;
    			break;
    		}
    	}
        RobotInfo[] convertable = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        if (convertable.length > 0) {
        	tryAttack = true;
        } 
    	if (tryAttack && rc.canEmpower(actionRadius)) {
    		System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runSlanderer() throws GameActionException {
    	//all Slanderers move to right now!
    	MapLocation current = rc.getLocation();
    	int newX = current.x +3;
    	int newY = current.y ;
    	MapLocation dest = new MapLocation(newX,newY);
        if (tryMove(nonRandomDirection(dest)))
            System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
    
    static Direction nonRandomDirection(MapLocation destination) {
    	MapLocation source = rc.getLocation();
    	
    	if (source.equals(destination))
    		return Direction.CENTER;
    	
    	int xDiff = destination.x - source.x;
    	int yDiff = destination.y - source.y;

		//Vertical path
		if(xDiff==0) { 
			if(yDiff>0) {
				return Direction.NORTH;
			}
			else {//yDiff<0
				return Direction.SOUTH;
			}		
		}
		
		//Horizontal path
		else if(yDiff==0) { 
			if(xDiff>0) {
				return Direction.EAST;
			}
			else {//xDiff<0
				return Direction.WEST;
			}		
		}
		
		//diagonal path
		else {
			if(xDiff>0 && yDiff>0) {
				return Direction.NORTHEAST;
			}
			else if(xDiff>0 && yDiff<0) {
				return Direction.SOUTHEAST;
			}
			else if(xDiff<0 && yDiff>0) {
				return Direction.NORTHWEST;
			}
			else { //(xDiff<0 && yDiff<0) 
				return Direction.SOUTHWEST;
			}
		}

    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
    
    static void maybeSendECLocation() throws GameActionException {
    	RobotInfo[] nearbyBots = rc.senseNearbyRobots();
    	for (RobotInfo ri : nearbyBots) {
    		if (ri.getType() == RobotType.ENLIGHTENMENT_CENTER) {
    			FlagInfo fi = new FlagInfo();
    			fi.location = ri.getLocation();
    			fi.team = ri.getTeam();
    			rc.setFlag(fi.encoded());
    		}
    	}
    }
}
