package hackatrainee_v2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Muckraker extends AttackRobot {

	public Muckraker(RobotController rc) {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();    	
    	// Check signal, should we be attacking?
    	int flagInt = rc.getFlag(masterID);
    	FlagInfoEC fi = new FlagInfoEC();
    	fi.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
    	if (fi.attack) {
    		attacking = true;
    		destination = fi.location;
    	} else {
    		attacking = false;
    		destination = null;
    	}
    	
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
        moveTowardsDestination();
    }
}
