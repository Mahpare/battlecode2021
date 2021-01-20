package hackatrainee_v2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Muckraker extends AttackRobot {
	static boolean chasing;
	
	public Muckraker(RobotController rc) {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// Seeing a slanderer overrides any other considerations of the Muckraker.
		// Even politicians are not scary yet since they likely have overkill.
		if (tryDestroySlanderer()) {
			return;
		} else {
			tryChaseSlanderer();
		}
        moveTowardsDestination();
    }
	
	public boolean tryDestroySlanderer() throws GameActionException {
		// If in action range, destroy the slanderer
		int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemyTeam)) {
            if (robot.type == RobotType.SLANDERER) {
                if (rc.canExpose(robot.location)) {
                    rc.expose(robot.location);
                    return true;
                }
            }
        }
        return false;
	}
	
	public boolean tryChaseSlanderer() throws GameActionException {
		int dist2 = Integer.MAX_VALUE;
		boolean nowChasing = false;
        for (RobotInfo robot : nearbyEnemies) {
            if (robot.type == RobotType.SLANDERER) {
            	nowChasing = true;
            	int newDist2 = myLoc.distanceSquaredTo(robot.getLocation());
            	if (newDist2 < dist2) {
            		dist2 = newDist2;
            		destination = robot.getLocation();
            	}
            }
        }
        return nowChasing;
	}
}
