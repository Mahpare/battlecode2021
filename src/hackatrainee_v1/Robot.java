package hackatrainee_v1;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Robot {
	static RobotController rc;
	static int turnCount = 0;
	
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
    
    static MapLocation destination;
    static int safeDistanceFromEcForSlanderer = 16;
	
	public void run() throws GameActionException {
        turnCount += 1;
	};
    
	public Robot(RobotController rc) {
		Robot.rc = rc;
	}
	
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static void clearFlag() throws GameActionException {
    	rc.setFlag(0);
    }
}
