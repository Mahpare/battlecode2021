package hackatrainee_v1;

import battlecode.common.MapLocation;
import battlecode.common.Team;

public class FlagInfo {
	public Team team;
	public MapLocation location;
	public boolean signaling;
	
	public FlagInfo() {}
	
	public void setFromEncoded(int encodedInfo, MapLocation ownLocation, Team ownTeam) {
		if (Util.getBit(encodedInfo, 23)) {
			signaling = true;
		}
		location = getLocationFromFlag(encodedInfo, ownLocation);
		if (((encodedInfo >> 15) & 1) == 1) {
			team = Team.NEUTRAL;
		} else if (((encodedInfo >> 14) & 1) == 1) {
			team = Team.A;
		} else {
			team = Team.B;
		}
	}
	
	public int encoded() {
		int x = location.x;
		int y = location.y;
		int extraInfo = this.encodeExtraInfo();
		int encodedInfo = (x % 128) * 128 + (y %128) + extraInfo;
		return encodedInfo;
	}
	
	private int encodeExtraInfo() {
		int result = 1 << 10;  // We are signaling
		if (team == Team.NEUTRAL) {
			result += 2;
		} else if (team == Team.B) {
			result += 1;
		}
		result = result << 14;
		return result;
	}
	
	private MapLocation getLocationFromFlag(int flag, MapLocation ownLocation) {
    	int y = flag % 128;
    	int x = (flag / 128) % 128;
    	int offsetX128 = ownLocation.x / 128;
    	int offsetY128 = ownLocation.y / 128;
    	MapLocation actualLocation = new MapLocation(offsetX128 * 128 + x, offsetY128 * 128 + y);
    	MapLocation alternative = actualLocation.translate(-128,  0);
    	if (ownLocation.distanceSquaredTo(alternative) < ownLocation.distanceSquaredTo(actualLocation)) {
    		actualLocation = alternative;
    	}
    	alternative = actualLocation.translate(-128,  0);
       	if (ownLocation.distanceSquaredTo(alternative) < ownLocation.distanceSquaredTo(actualLocation)) {
    		actualLocation = alternative;
    	}
       	alternative = actualLocation.translate(128,  0);
       	if (ownLocation.distanceSquaredTo(alternative) < ownLocation.distanceSquaredTo(actualLocation)) {
    		actualLocation = alternative;
    	}
       	alternative = actualLocation.translate(0,  -128);
       	if (ownLocation.distanceSquaredTo(alternative) < ownLocation.distanceSquaredTo(actualLocation)) {
    		actualLocation = alternative;
    	}
       	alternative = actualLocation.translate(0, 128);
       	if (ownLocation.distanceSquaredTo(alternative) < ownLocation.distanceSquaredTo(actualLocation)) {
    		actualLocation = alternative;
    	}
       	return actualLocation;
    }
}
