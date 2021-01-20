package hackatrainee_v2;

import java.util.ArrayList;

import battlecode.common.MapLocation;

public class Pathing {

	public static MapLocation averageLocation(ArrayList<MapLocation> locations) {
		if (locations.size() == 0) {
			return new MapLocation(0, 0);
		}
		MapLocation total = null;
		for	(MapLocation loc : locations) {
			if (total == null) {
				total = loc;
			} else {
				total.translate(loc.x, loc.y);
			}
		}
		return new MapLocation(total.x / locations.size(), total.y / locations.size());
	}	
}
