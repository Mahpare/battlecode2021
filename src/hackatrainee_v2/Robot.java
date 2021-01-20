package hackatrainee_v2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public abstract class Robot {
	static RobotController rc;
	static int turnCount = 0;
    static Team allyTeam; 
    static Team enemyTeam;
	static RobotInfo[] nearbyAllies;
	static RobotInfo[] nearbyEnemies;
	static RobotInfo[] nearbyNeutrals;
	static RobotInfo[] nearbyBots;
	
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
        nearbyAllies = rc.senseNearbyRobots(-1, allyTeam);
        nearbyEnemies = rc.senseNearbyRobots(-1, enemyTeam);
        nearbyNeutrals = rc.senseNearbyRobots(-1, Team.NEUTRAL);
        nearbyBots = rc.senseNearbyRobots();
	};
    
	public Robot(RobotController rc) {
		Robot.rc = rc;
		allyTeam = rc.getTeam();
		enemyTeam = allyTeam.opponent();
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
