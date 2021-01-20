package hackatrainee_v2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class AttackRobot extends MovingRobot {
	static boolean attacking;
	
	public AttackRobot(RobotController rc) {
		super(rc);
	}
	
	public void run() throws GameActionException {
		super.run();
		if (flagEC.attack) {
			attacking = true;
			destination = flagEC.location;
		}
	}
}