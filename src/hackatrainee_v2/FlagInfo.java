package hackatrainee_v2;

import battlecode.common.MapLocation;
import battlecode.common.Team;

public class FlagInfo {
	public Team team;
	public MapLocation location;
	public boolean signaling;
	public int conviction;
	
	public FlagInfo() {}
	
	public void setFromEncoded(int encodedInfo, MapLocation ownLocation, Team ownTeam) {
		if (Util.getBit(encodedInfo, 0)) {
			signaling = true;
		}
		location = getLocationFromFlag(encodedInfo, ownLocation);
		if (Util.getBit(encodedInfo, 8)) {
			team = Team.NEUTRAL;
		} else if (Util.getBit(encodedInfo, 9)) {
			team = Team.B;
		} else {
			team = Team.A;
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
		int result = 1 << 9;  // We are signaling
		if (team == Team.NEUTRAL) {
			result += 2;
		} else if (team == Team.B) {
			result += 1;
		}
		int convictionEncoding = 0;
		if (conviction <= 80) {
			conviction = 0;
		} else if (conviction <= 200) {
			convictionEncoding = 1;
		} else if (conviction <= 500) {
			convictionEncoding = 2;
		} else {
			convictionEncoding = 3;
		}
		result += convictionEncoding << 2;
		result = result << 14;
		return result;
	}
	
	protected MapLocation getLocationFromFlag(int flag, MapLocation ownLocation) {
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
