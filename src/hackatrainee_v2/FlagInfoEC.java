package hackatrainee_v2;

import battlecode.common.MapLocation;
import battlecode.common.Team;

public class FlagInfoEC extends FlagInfo {
	public boolean attack;
	
	public void setFromEncoded(int encodedInfo, MapLocation ownLocation, Team ownTeam) {
		if (Util.getBit(encodedInfo, 0)) {
			attack = true;
		}
		location = getLocationFromFlag(encodedInfo, ownLocation);
	}
	
	public int encoded() {
		int x = location.x;
		int y = location.y;
		int extraInfo = this.encodeExtraInfo();
		int encodedInfo = (x % 128) * 128 + (y %128) + extraInfo;
		return encodedInfo;
	}
	
	private int encodeExtraInfo() {
		int result = 1 << 9;  // We are attacking
		result = result << 14;
		return result;
	}
}
