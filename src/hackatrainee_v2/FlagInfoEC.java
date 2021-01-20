package hackatrainee_v2;

import battlecode.common.MapLocation;
import battlecode.common.Team;

public class FlagInfoEC extends FlagInfo {
	public boolean attackEC;
	public boolean attackSlanderers;
	public boolean explore;
	
	public void setFromEncoded(int encodedInfo, MapLocation ownLocation, Team ownTeam) {
		if (Util.getBit(encodedInfo, 0)) {
			attackEC = true;
		} else {
			explore = true;
		}
		location = getLocationFromFlag(encodedInfo, ownLocation);
	}
	
	private int encodeExtraInfo() {
		int result = 0;
		if (attack) {
			result = 1 << 9;  // We are attacking
		}
		result = result << 14;
		return result;
	}
}
