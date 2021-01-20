package hackatrainee_v2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Slanderer extends MovingRobot {

	public Slanderer(RobotController rc) {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
    	//keep a distance from our ECs
    	Team team = rc.getTeam();
    	int sensorRadius = rc.getType().sensorRadiusSquared;
    	RobotInfo[] teammate = rc.senseNearbyRobots(sensorRadius, team);
    	boolean getAway = false;
    	MapLocation EcLocation = rc.getLocation();//just for initialization!
    	for (RobotInfo r : teammate) {
    		if (r.getType() == RobotType.ENLIGHTENMENT_CENTER) {
    			//calculate distance from that EC:
    			MapLocation current = rc.getLocation();
    			EcLocation = r.getLocation(); //EC location
    			int distanceSquaredToEc = current.distanceSquaredTo(EcLocation);
    			if(distanceSquaredToEc < safeDistanceFromEcForSlanderer) {
    				getAway = true;
        			break;
    			}
    		}
    	}
    	if(getAway == true) {
    		//find ECs location, then move to the opposite direction!
    		Direction direct = directionAwayFrom(EcLocation);
    		if (tryMove(direct)) {
    			System.out.println("I moved!");
    			return;
    		}
    	}
    	//escape from Muckrakers
    	Team enemy = rc.getTeam().opponent();
    	RobotInfo[] enemies = rc.senseNearbyRobots(sensorRadius, enemy);
     	getAway = false;
     	MapLocation muckrakerLocation = rc.getLocation();//just for initialization!
    	for (RobotInfo r : enemies) {
    		if (r.getType() == RobotType.MUCKRAKER) {
    			getAway = true;
    			muckrakerLocation = r.getLocation();
    			break;
    		}
    	}
    	if(getAway == true) {
    		//find muckrakerLocation, then move to the opposite direction!
    		Direction direct = directionAwayFrom(muckrakerLocation);
    		if (tryMove(direct)) {
    			System.out.println("I moved!");
    			return;
    		}
    	}    	
    	//go toward our other Slanderers
    	
    	if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}
