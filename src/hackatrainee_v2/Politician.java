package hackatrainee_v2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Politician extends AttackRobot {

	public Politician(RobotController rc) {
		super(rc);
	}
	
	public void run() throws GameActionException {
		super.run();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemyTeam);
        boolean tryAttack = false;
    	for (RobotInfo r : attackable) {
    		if (r.getType() == RobotType.MUCKRAKER || r.getType() == RobotType.ENLIGHTENMENT_CENTER && rc.getRoundNum() > 800) {
    			tryAttack = true;
    			break;
    		}
    	}
        RobotInfo[] convertable = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        if (convertable.length > 0) {
        	tryAttack = true;
        }
    	if (tryAttack && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        moveTowardsDestination();
    }
}
