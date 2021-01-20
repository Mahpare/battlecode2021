package hackatrainee_v1;

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
    	if (!attacking) {    	
	    	//go away from base ECs
	    	Team team = rc.getTeam();
	    	int sensorRadius = rc.getType().sensorRadiusSquared;
	    	RobotInfo[] teammate = rc.senseNearbyRobots(sensorRadius, team);
	    	boolean getAway = false;
	    	MapLocation EcLocation = rc.getLocation();//just for initialization!
	    	for (RobotInfo r : teammate) {
	    		if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
	    			getAway = true;
	    			EcLocation = r.getLocation();
	    			break;
	    		}
	    	}
	    	if(getAway == true) {
	    		//find ECs location, then move to the opposite direction!
	    		Direction direct = escapeFromDirection(EcLocation);
	    		if (tryMove(direct)) {
	    			System.out.println("I moved!");
	    			return;
	    		}
	    	}
    	}
    	
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
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
