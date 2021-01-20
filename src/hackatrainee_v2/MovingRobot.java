package hackatrainee_v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import hackatrainee_v1.FlagInfoEC;

public abstract class MovingRobot extends Robot {
	static int masterID;
	static RobotInfo masterEC;
	static FlagInfoEC flagEC;
	static boolean exploring = true;
	static Direction moveDirection;
	static MapLocation myLoc;
	static MapLocation destination;

	public MovingRobot(RobotController rc) {
		super(rc);
		flagEC = new FlagInfoEC();
		for (Direction dir : directions) {
	    	try {
	    		RobotInfo maybeEC = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
	            if (maybeEC != null && maybeEC.getType() == RobotType.ENLIGHTENMENT_CENTER) {
	            	masterID = maybeEC.getID();
	            	masterEC = rc.senseRobot(masterID);
	            	Direction awayFromEC = dir.opposite();
	            	destination = rc.getLocation().translate(64 * awayFromEC.dx, 64 * awayFromEC.dy);
	            }
	    	} catch (GameActionException e) {
	    		// adjacent location not on the map
	    		continue;
	    	}
	    }
		
	}

	public void run() throws GameActionException {
		super.run();
		myLoc = rc.getLocation();
		maybeSendECLocation();
		readECFlag();
	}
	
	// Move away from allied Robots, stay away from map edges
	public void explore() throws GameActionException {
		RobotInfo[] sensed = rc.senseNearbyRobots(-1, allyTeam);
		ArrayList<MapLocation> locsToAvoid = new ArrayList<MapLocation>();
		//TODO: move away from master EC
		MapLocation awayFromOthers = myLoc;
		if (sensed.length > 0) {
			for (RobotInfo ri : sensed) {
				awayFromOthers = awayFromOthers.subtract(myLoc.directionTo(ri.getLocation()));
			}
		} else {
			awayFromOthers = myLoc.add(directionAwayFrom(masterEC.getLocation()));
			System.out.println("setting destination to " + awayFromOthers + ", coming from " + myLoc);
		}
		destination = awayFromOthers;
	}
	
	static void moveTowardsDestination() {
        Direction[] moveChoices = directionsTowardsDestination(destination);
        Random random = new Random();
        int randIdx = random.nextInt(moveChoices.length);
        int tries = 0;
        try {
	        while(!tryMove(moveChoices[randIdx])) {
	        	randIdx = (randIdx + 1) % moveChoices.length;
	        	tries++;
	        	if (tries == moveChoices.length) {
	        		break;
	        	}
	        }
        } catch (GameActionException e) {
        	System.out.println("Exception while trying to move.");
        }
    }
	
	static Direction[] directionsTowardsDestination(MapLocation destination) {
    	if (destination == null) {
    		return Direction.allDirections();
    	}    	
    	if (myLoc.equals(destination))
    		return new Direction[] { Direction.CENTER };
    	if (myLoc.isAdjacentTo(destination)) {
    		return new Direction[] { myLoc.directionTo(destination) };
    	}
    	
    	int xDiff = destination.x - myLoc.x;
    	int yDiff = destination.y - myLoc.y;

    	Set<Direction> choices = new HashSet<Direction>();
		if (xDiff > 0) {
			choices.add(Direction.EAST);
			if (Math.abs(xDiff) > Math.abs(yDiff)) {
				choices.add(Direction.NORTHEAST);
				choices.add(Direction.SOUTHEAST);
			}
		}
		if (xDiff < 0) {
			choices.add(Direction.WEST);
			if (Math.abs(xDiff) > Math.abs(yDiff)) {
				choices.add(Direction.NORTHWEST);
				choices.add(Direction.SOUTHWEST);
			}
		}
		if (yDiff > 0) {
			choices.add(Direction.NORTH);
			if (Math.abs(xDiff) < Math.abs(yDiff)) {
				choices.add(Direction.NORTHWEST);
				choices.add(Direction.NORTHEAST);
			}
		}
		if (yDiff < 0) {
			choices.add(Direction.SOUTH);
			if (Math.abs(xDiff) < Math.abs(yDiff)) {
				choices.add(Direction.SOUTHEAST);
				choices.add(Direction.SOUTHWEST);
			}
		}
		if (xDiff>0 && yDiff>0) {
			choices.add(Direction.NORTHEAST);
		} else if(xDiff>0 && yDiff<0) {
			choices.add(Direction.SOUTHEAST);
		} else if(xDiff<0 && yDiff>0) {
			choices.add(Direction.NORTHWEST);
		} else { //(xDiff<0 && yDiff<0) 
			choices.add(Direction.SOUTHWEST);
		}
		return choices.toArray(new Direction[choices.size()]);
    }
	
    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
//        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else {
        	return false;
        }
    }
    
    static Direction directionAwayFrom(MapLocation awayFrom) {
    	if (myLoc.equals(awayFrom)) {
    		Direction direct= randomDirection();
    		while (direct == Direction.CENTER)
    			direct= randomDirection();
    		return direct;
    	}
    	int xDiff = awayFrom.x - myLoc.x;
    	int yDiff = awayFrom.y - myLoc.y;
		// Vertical path
		if(xDiff==0) { 
			if(yDiff>0) {
				return Direction.SOUTH;
			}
			else {//yDiff<0
				return Direction.NORTH;
			}		
		}
		// Horizontal path
		else if(yDiff==0) { 
			if(xDiff>0) {
				return Direction.WEST;
			}
			else {//xDiff<0
				return Direction.EAST;
			}		
		}
		// Diagonal path
		else {
			if(xDiff>0 && yDiff>0) {
				return Direction.SOUTHWEST;
			}
			else if(xDiff>0 && yDiff<0) {
				return Direction.NORTHWEST;
			}
			else if(xDiff<0 && yDiff>0) {
				return Direction.SOUTHEAST;
			}
			else { //(xDiff<0 && yDiff<0) 
				return Direction.NORTHEAST;
			}
		}
    }
	
	static void maybeSendECLocation() throws GameActionException {
    	RobotInfo[] nearbyBots = rc.senseNearbyRobots();
    	if (rc.getFlag(rc.getID()) != 0) {
    		rc.setFlag(0);
    	} else {
	    	for (RobotInfo ri : nearbyBots) {
	    		if (ri.getType() == RobotType.ENLIGHTENMENT_CENTER) {
	    			FlagInfo fi = new FlagInfo();
	    			fi.location = ri.getLocation();
	    			fi.team = ri.getTeam();
	    			fi.conviction = ri.getConviction();
//	    			System.out.println("Setting flag to " + fi.encoded());
	    			rc.setFlag(fi.encoded());
	    		}
	    	}
    	}
    }
	
	static void readECFlag() throws GameActionException {
		// Check signal, should we be attacking?
    	int flagInt = rc.getFlag(masterID);
    	flagEC.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
	}
}
