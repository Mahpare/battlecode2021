package hackatrainee_v1;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public abstract class MovingRobot extends Robot {
	static int masterID;

	public MovingRobot(RobotController rc) {
		super(rc);
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

	public void run() throws GameActionException {
		super.run();
		maybeSendECLocation();
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
        	tries = moveChoices.length; // Just go for random
        }
        if (tries == moveChoices.length) {
        	try {
        		tryMove(randomDirection());
        	} catch (GameActionException e) {
        		// Well nevermind then.
        	}
        }
    }
	
	static Direction[] directionsTowardsDestination(MapLocation destination) {
    	if (destination == null) {
    		return Direction.allDirections();
    	}
    	MapLocation source = rc.getLocation();
    	
    	if (source.equals(destination))
    		return new Direction[]{ Direction.CENTER };
    	
    	int xDiff = destination.x - source.x;
    	int yDiff = destination.y - source.y;

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
        } else return false;
    }
    
    static Direction escapeFromDirection(MapLocation suspiciousPlace) {
    	MapLocation source = rc.getLocation();
    	if (source.equals(suspiciousPlace)) {
    		Direction direct= randomDirection();
    		while (direct == Direction.CENTER)
    			direct= randomDirection();
    		return direct;
    	}
    	int xDiff = suspiciousPlace.x - source.x;
    	int yDiff = suspiciousPlace.y - source.y;
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
    	if (rc.getFlag(rc.getID()) != 0) { // Reset my own flag
    		rc.setFlag(0);
    	} else {
	    	for (RobotInfo ri : nearbyBots) {
	    		if (ri.getType() == RobotType.ENLIGHTENMENT_CENTER) {
	    			FlagInfo fi = new FlagInfo();
	    			fi.location = ri.getLocation();
	    			fi.team = ri.getTeam();
	    			fi.conviction = ri.getConviction();
	    			System.out.println("Setting flag to " + fi.encoded());
	    			rc.setFlag(fi.encoded());
	    		}
	    	}
    	}
    }
}
