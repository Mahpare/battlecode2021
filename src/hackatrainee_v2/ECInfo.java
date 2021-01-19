package hackatrainee_v2;

import battlecode.common.MapLocation;
import battlecode.common.Team;

public class ECInfo {
	public ECInfo(MapLocation location, Team team, int conviction) {
		this.location = location;
		this.team = team;
		this.conviction = conviction;
	}
	MapLocation location;
	Team team;
	int conviction;
}
