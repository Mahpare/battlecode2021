package hackatrainee_v2;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class AttackRobot extends MovingRobot {
	boolean attacking;
	
	public AttackRobot(RobotController rc) {
		super(rc);
	}
}
