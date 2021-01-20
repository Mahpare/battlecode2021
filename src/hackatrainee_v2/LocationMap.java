package hackatrainee_v2;

import java.util.Arrays;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class LocationMap {
	RobotController rc;
	// map holds values of 1.0 / passability, so "action cost" between 1.0 and 10.0
	//  0.0 = unexplored
	// -1.0 = friendly EC
	// -2.0 = enemy EC
	// -3.0 = neutral EC
	// -4.0 = outside the map
	static float[][] map = new float[128][128];
	// Robot's initial location; we don't know in which direction the map extends
	// All locations in the map will be at coordinate
	static MapLocation center;
	
	public LocationMap(RobotController rc) {
		this.rc = rc;
		center = rc.getLocation();
		updateMap(rc);
	}

	public void updateMap(RobotController rc) {
		int r2 = rc.getType().sensorRadiusSquared;
		int r = (int) Math.sqrt(r2);
		MapLocation myLoc = rc.getLocation();
		int mapIdxX = 0;
		int mapIdxY = 0;
		// scan rows of circle bottom to top (increasing y)
		for (int i = myLoc.y-r; i < myLoc.y+r; i++) {
			mapIdxY = i - center.y + 64;
			// scan left half of current row
		    for (int j = myLoc.x; Math.pow(j-myLoc.x, 2) + Math.pow((i-myLoc.y), 2) <= r2; j--) {
		        float newValue = getMapValueAtCoordinates(j, i);
				mapIdxX = j - center.x + 64;
		        map[mapIdxY][mapIdxX] = newValue;
		    }
		    // scan right half of current row
		    for (int j = myLoc.x+1; Math.pow((j-myLoc.x), 2) + Math.pow((i-myLoc.y), 2) <= r2; j++) {
		        float newValue = getMapValueAtCoordinates(j, i);
		        mapIdxX = j - center.x + 64;
		        map[mapIdxY][mapIdxX] = newValue;
		    }
		}
	}
	
	public float getMapValueAtCoordinates(int x, int y) {
		MapLocation toCheck = new MapLocation(x, y);
		float mapValue = (float) 0.0;
		RobotInfo maybeEC = null;
		try {
			maybeEC = rc.senseRobotAtLocation(toCheck);
		} catch (GameActionException e1) {
			// outside sensor range (should not happen) OR not on the map
		}
		if (maybeEC != null && maybeEC.getType() == RobotType.ENLIGHTENMENT_CENTER) {
			Team ecTeam = maybeEC.getTeam();
			if (ecTeam == rc.getTeam()) {
				mapValue = (float) -1.0;
			} else if (ecTeam == Team.NEUTRAL) {
				mapValue = (float) -3.0;
			} else {
				mapValue = (float) -2.0; // enemy
			}
		} else {
	        try {
		        if (rc.onTheMap(toCheck)) {
		        	double pass = rc.sensePassability(toCheck);
		        	mapValue = (float) (1.0 / pass);
		        }
	        } catch (GameActionException e) {
	        	mapValue = (float) -4.0; // outside the map
	        }
		}
		return mapValue;
	}
}
