package hackatrainee_v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class EC extends Robot {
    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };
    // robots created and commanded by this EC
    static HashMap<RobotType, ArrayList<Integer>> minionIDs =
    		new HashMap<RobotType, ArrayList<Integer>>();
    static HashMap<RobotType, Double> desiredRatios = new HashMap<RobotType, Double>();
    static HashMap<MapLocation, ECInfo> knownECs = new HashMap<MapLocation, ECInfo>();
    static int prevVotes = 0; 
    static int bidInfluence = 1;
    static boolean didBid = false;
	
    public EC(RobotController rc) {
		super(rc);
		for (RobotType rt : RobotType.values()) {
        	minionIDs.put(rt, new ArrayList<Integer>());
        }
        desiredRatios.put(RobotType.POLITICIAN, 0.38);
        desiredRatios.put(RobotType.SLANDERER, 0.5);
        desiredRatios.put(RobotType.MUCKRAKER, 0.12);
	}

    public void run() throws GameActionException {
    	super.run();
    	// Prune minions, some may have been lost
		for (ArrayList<Integer> minionList : minionIDs.values()) {
			ArrayList<Integer> minionsCopy = new ArrayList<Integer>(minionList);
			for (int id : minionsCopy) {
	    		try {
	    			if (!rc.canGetFlag(id)) {
	    				throw new GameActionException(null, null);
	    			}
	    		} catch (GameActionException e) {
	    			System.out.println("Removing minion " + id);
	    			minionList.remove(Integer.valueOf(id));
	    		}
			}
		}
		
		// Get flag info from remaining minions
		for (ArrayList<Integer> minionList : minionIDs.values()) {
			for (int id : minionList) {
				try {
					int flagInt = rc.getFlag(id);
					if (flagInt != 0) {
	    				FlagInfo fi = new FlagInfo();
	    				fi.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
	    				if (fi.signaling) {
	    					MapLocation location = fi.location;
	    					Team team = fi.team;
	    					int conviction = fi.conviction;
	    					knownECs.put(location, new ECInfo(location, team, conviction));
	    				}
					}
				} catch (GameActionException e) {
	                System.out.println(rc.getType() + " Exception");
	                e.printStackTrace();
				}
			}
		}
		
		// Signal to attack EC
		boolean attacking = false;
		for (ECInfo ec : knownECs.values()) {
			if (ec.team == Team.NEUTRAL || (rc.getRoundNum() > 800 && ec.team == rc.getTeam().opponent())) {
				signalAttackEC(ec);
				attacking = true;
			}
		}
		if (!attacking) {
			clearFlag();
		}
		
	    // Produce new robots
		RobotType buildType = pickBuildType();
		
	    int influence = (int) Math.max(50, 0.05 * rc.getInfluence());
	    for (Direction dir : directions) {
	        if (rc.canBuildRobot(buildType, dir, influence)) {
	            rc.buildRobot(buildType, dir, influence);
	            // Add the newly built robot to the minions list
	            RobotInfo freshMinion = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
	            minionIDs.get(buildType).add(freshMinion.getID());
	        }
	    }
	    // Make bids for votes
	    int currentVotes = rc.getTeamVotes();
	    if (didBid) {
	    	if (currentVotes == prevVotes) { // We did not win the vote
	    		bidInfluence *= 1.5; // Increase vote price
	    	} else {
	    		bidInfluence = Math.max(2, 3 * bidInfluence / 4); // Decrease vote price
	    	}
	    }
	    prevVotes = currentVotes;
	    int currentInfluence = rc.getInfluence();
	    if (bidInfluence < currentInfluence / 5) { // At most bid 20% of remaining influence
	    	rc.bid(bidInfluence);
	    	didBid = true;
	    } else {
	    	didBid = false;
	    }
	}
    
	static RobotType pickBuildType() {
    	int totalMyMinions = 0;
    	HashMap<RobotType, Double> robotRatios = new HashMap<RobotType, Double>();
    	for (Map.Entry<RobotType, ArrayList<Integer>> entry : minionIDs.entrySet()) {
    		totalMyMinions += entry.getValue().size();
    	}
    	for (Map.Entry<RobotType, ArrayList<Integer>> entry : minionIDs.entrySet()) {
    		double ratio = 0.0;
    		if (totalMyMinions > 0) {
    			ratio = ((double) entry.getValue().size()) / (double) totalMyMinions;
    		}
    		robotRatios.put(entry.getKey(), ratio);
    	}
    	RobotType buildType = RobotType.MUCKRAKER;
    	for (RobotType rt : robotRatios.keySet()) {
    		if (rt != RobotType.ENLIGHTENMENT_CENTER) {
	    		double desired = desiredRatios.get(rt);
	    		if (robotRatios.get(rt) < desired) {
	    			buildType = rt;
	    			break;
	    		}
    		}
    	}
    	return buildType;
    }
	
	static void signalAttackEC(ECInfo ec) throws GameActionException {
    	FlagInfoEC fi = new FlagInfoEC();
    	fi.location = ec.location;
    	fi.attack = true;
    	rc.setFlag(fi.encoded());
    }
}
