package hackatrainee_v1;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Map;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;
    
    // Mobile robot variables
    static int masterID = 0;
    static MapLocation destination;
    static int safeDistanceFromEcForSlanderer = 16;

    // Politicians and Muckrakers
    static boolean attacking = false;
    
    // Enlightenment Center variables
    static HashMap<RobotType, ArrayList<Integer>> minionIDs =
    		new HashMap<RobotType, ArrayList<Integer>>(); // robots created and commanded by this EC
    static int buildIdx = 0;
    static RobotType[] buildQueue = {
    		spawnableRobot[0],
    		spawnableRobot[1],
    		spawnableRobot[2]
    };
    static HashMap<RobotType, Double> desiredRatios = new HashMap<RobotType, Double>();
    static HashMap<MapLocation, ECInfo> knownECs = new HashMap<MapLocation, ECInfo>();
    static int prevVotes = 0; 
    static int bidInfluence = 1;
    static boolean didBid = false;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

	    if (rc.getType() == RobotType.ENLIGHTENMENT_CENTER) {
        	for (RobotType rt : RobotType.values()) {
	        	minionIDs.put(rt, new ArrayList<Integer>());
	        }
            desiredRatios.put(RobotType.POLITICIAN, 0.38);
            desiredRatios.put(RobotType.SLANDERER, 0.5);
            desiredRatios.put(RobotType.MUCKRAKER, 0.12);

	    } else { // Find EC that probably created this robot
	        for (Direction dir : directions) {
	        	try {
	        		RobotInfo maybeEC = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
		            if (maybeEC != null && maybeEC.getType() == RobotType.ENLIGHTENMENT_CENTER) {
		            	masterID = maybeEC.getID();
		            }
	        	} catch (GameActionException e) {
	        		// adjacent location not on the map
	        		continue;
	        	}
	        }
	    }
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
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
//    				System.out.println("minion of type " +
//    						rc.senseRobot(id).getType() + " with ID " + id +
//    						" has flag " + rc.getFlag(id));
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
    
    static void runPolitician() throws GameActionException {
    	maybeSendECLocation();
    	
    	// Check signal, should we be attacking?
    	int flagInt = rc.getFlag(masterID);
    	FlagInfoEC fi = new FlagInfoEC();
    	fi.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
    	if (fi.attack) {
    		attacking = true;
    		destination = fi.location;
    	} else {
    		attacking = false;
    		destination = null;
    	
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
        poliOrMuckMove();
    }

    static void runSlanderer() throws GameActionException {
    	maybeSendECLocation();
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
    		Direction direct = escapeFromDirection(EcLocation);
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
    		Direction direct = escapeFromDirection(muckrakerLocation);
    		if (tryMove(direct)) {
    			System.out.println("I moved!");
    			return;
    		}
    	}    	
    	//go toward our other Slanderers
    	
    	if (tryMove(randomDirection()))
            System.out.println("I moved!");
    	//all Slanderers move to right now!
//    	MapLocation current = rc.getLocation();
//    	int newX = current.x +3;
//    	int newY = current.y ;
//    	MapLocation dest = new MapLocation(newX,newY);
//    	
//        if (tryMove(nonRandomDirection(dest)))
//            System.out.println("I moved!");*/
    }

    static void runMuckraker() throws GameActionException {
    	maybeSendECLocation();
    	
    	// Check signal, should we be attacking?
    	int flagInt = rc.getFlag(masterID);
    	FlagInfoEC fi = new FlagInfoEC();
    	fi.setFromEncoded(flagInt, rc.getLocation(), rc.getTeam());
    	if (fi.attack) {
    		attacking = true;
    		destination = fi.location;
    	} else {
    		attacking = false;
    		destination = null;
    	}
    	
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        poliOrMuckMove();
    }

    static void poliOrMuckMove() {
        Direction[] moveChoices = nonRandomDirections(destination);
        Random random = new Random();
        int randIdx = random.nextInt(moveChoices.length);
        int tries = 0;
        try {
	        while(!tryMove(moveChoices[randIdx])) {
	        	randIdx = (randIdx + 1) % moveChoices.length;
	        	tries++;
	        	if (tries == moveChoices.length) {
	        		break;
	        	}
	        }
        } catch (GameActionException e) {
        	tries = moveChoices.length; // Just go for random
        }
        if (tries == moveChoices.length) {
        	try {
        		tryMove(randomDirection());
        	} catch (GameActionException e) {
        		// Well nevermind then.
        	}
        }
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
    
    static Direction escapeFromDirection(MapLocation suspiciousPlace) {
    	MapLocation source = rc.getLocation();
    	
    	if (source.equals(suspiciousPlace)) {
    		Direction direct= randomDirection();
    		while (direct == Direction.CENTER)
    			direct= randomDirection();
    		return direct;
    	}
    		
    	int xDiff = suspiciousPlace.x - source.x;
    	int yDiff = suspiciousPlace.y - source.y;

		//Vertical path
		if(xDiff==0) { 
			if(yDiff>0) {
				return Direction.SOUTH;
			}
			else {//yDiff<0
				return Direction.NORTH;
			}		
		}
		
		//Horizontal path
		else if(yDiff==0) { 
			if(xDiff>0) {
				return Direction.WEST;
			}
			else {//xDiff<0
				return Direction.EAST;
			}		
		}
		
		//diagonal path
		else {
			if(xDiff>0 && yDiff>0) {
				return Direction.SOUTHWEST;
			}
			else if(xDiff>0 && yDiff<0) {
				return Direction.NORTHWEST;
			}
			else if(xDiff<0 && yDiff>0) {
				return Direction.SOUTHEAST;
			}
			else { //(xDiff<0 && yDiff<0) 
				return Direction.NORTHEAST;
			}
		}
    	
    }
    
    static Direction[] nonRandomDirections(MapLocation destination) {
    	if (destination == null) {
    		return Direction.allDirections();
    	}
    	MapLocation source = rc.getLocation();
    	
    	if (source.equals(destination))
    		return new Direction[]{ Direction.CENTER };
    	
    	int xDiff = destination.x - source.x;
    	int yDiff = destination.y - source.y;

    	Set<Direction> choices = new HashSet<Direction>();
		if (xDiff > 0) {
			choices.add(Direction.EAST);
			if (Math.abs(xDiff) > Math.abs(yDiff)) {
				choices.add(Direction.NORTHEAST);
				choices.add(Direction.SOUTHEAST);
			}
		}
		if (xDiff < 0) {
			choices.add(Direction.WEST);
			if (Math.abs(xDiff) > Math.abs(yDiff)) {
				choices.add(Direction.NORTHWEST);
				choices.add(Direction.SOUTHWEST);
			}
		}
		if (yDiff > 0) {
			choices.add(Direction.NORTH);
			if (Math.abs(xDiff) < Math.abs(yDiff)) {
				choices.add(Direction.NORTHWEST);
				choices.add(Direction.NORTHEAST);
			}
		}
		if (yDiff < 0) {
			choices.add(Direction.SOUTH);
			if (Math.abs(xDiff) < Math.abs(yDiff)) {
				choices.add(Direction.SOUTHEAST);
				choices.add(Direction.SOUTHWEST);
			}
		}
		if (xDiff>0 && yDiff>0) {
			choices.add(Direction.NORTHEAST);
		} else if(xDiff>0 && yDiff<0) {
			choices.add(Direction.SOUTHEAST);
		} else if(xDiff<0 && yDiff>0) {
			choices.add(Direction.NORTHWEST);
		} else { //(xDiff<0 && yDiff<0) 
			choices.add(Direction.SOUTHWEST);
		}
		return choices.toArray(new Direction[choices.size()]);
    }
//    
//    static Direction findEmptyDirection(Direction occupiedDirection) {
//    	double coin = Math.random();    	
//    	if(occupiedDirection == Direction.NORTHEAST) {
//    		MapLocation north = rc.adjacentLocation(Direction.NORTH);
//    		MapLocation east = rc.adjacentLocation(Direction.EAST);
//    		
//    		if(rc.isLocationOccupied(north) && rc.isLocationOccupied(east)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.NORTHEAST || direct == Direction.NORTH || direct == Direction.EAST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(north) && !rc.isLocationOccupied(east)) { 
//    			return Direction.EAST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(north) && rc.isLocationOccupied(east)) { 
//    			return Direction.NORTH;
//    		}
//    		
//    		else { //both directions are empty
//    			double nPass = rc.sensePassability(north);
//        		double ePass = rc.sensePassability(east);
//        		if(nPass > ePass) //nPass in better
//        			return Direction.NORTH;
//        		else if (nPass < ePass)
//        			return Direction.EAST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.NORTH;
//            		else
//            			return Direction.EAST;
//        		}
//    		}
//    	}
//    	
//    	else if(occupiedDirection == Direction.NORTHWEST) {
//    		MapLocation north = rc.adjacentLocation(Direction.NORTH);
//    		MapLocation west = rc.adjacentLocation(Direction.WEST);
//    		
//    		if(rc.isLocationOccupied(north) && rc.isLocationOccupied(west)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.NORTHWEST || direct == Direction.NORTH || direct == Direction.WEST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(north) && !rc.isLocationOccupied(west)) { 
//    			return Direction.WEST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(north) && rc.isLocationOccupied(west)) { 
//    			return Direction.NORTH;
//    		}
//    		
//    		else { //both directions are empty
//    			double nPass = rc.sensePassability(north);
//        		double wPass = rc.sensePassability(west);
//        		if(nPass > wPass) //nPass in better
//        			return Direction.NORTH;
//        		else if (nPass < wPass)
//        			return Direction.WEST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.NORTH;
//            		else
//            			return Direction.WEST;
//        		}
//    		}
//    	}
//    	
//    	else if(occupiedDirection == Direction.SOUTHEAST) {
//    		MapLocation south = rc.adjacentLocation(Direction.SOUTH);
//    		MapLocation east = rc.adjacentLocation(Direction.EAST);
//    		
//    		if(rc.isLocationOccupied(south) && rc.isLocationOccupied(east)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.SOUTHEAST || direct == Direction.SOUTH || direct == Direction.EAST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(south) && !rc.isLocationOccupied(east)) { 
//    			return Direction.EAST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(south) && rc.isLocationOccupied(east)) { 
//    			return Direction.SOUTH;
//    		}
//    		
//    		else { //both directions are empty
//    			double sPass = rc.sensePassability(south);
//        		double ePass = rc.sensePassability(east);
//        		if(sPass > ePass) //nPass in better
//        			return Direction.SOUTH;
//        		else if (sPass < ePass)
//        			return Direction.EAST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.SOUTH;
//            		else
//            			return Direction.EAST;
//        		}
//    		}
//    	}    	
//    	
//    	else if(occupiedDirection == Direction.SOUTHWEST) {
//    		MapLocation south = rc.adjacentLocation(Direction.SOUTH);
//    		MapLocation west = rc.adjacentLocation(Direction.WEST);
//    		
//    		if(rc.isLocationOccupied(south) && rc.isLocationOccupied(west)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.SOUTHWEST || direct == Direction.SOUTH || direct == Direction.WEST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(south) && !rc.isLocationOccupied(west)) { 
//    			return Direction.WEST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(south) && rc.isLocationOccupied(west)) { 
//    			return Direction.SOUTH;
//    		}
//    		
//    		else { //both directions are empty
//    			double sPass = rc.sensePassability(south);
//        		double wPass = rc.sensePassability(west);
//        		if(sPass > wPass) //nPass in better
//        			return Direction.SOUTH;
//        		else if (sPass < wPass)
//        			return Direction.WEST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.SOUTH;
//            		else
//            			return Direction.WEST;
//        		}
//    		}
//    	}
//    	/////////////
//    	
//    	else if(occupiedDirection == Direction.SOUTH) {
//    		MapLocation southE = current.add(Direction.SOUTHEAST);
//    		MapLocation southW = current.add(Direction.SOUTHWEST);
//    		
//    		if(rc.isLocationOccupied(southE) && rc.isLocationOccupied(southW)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.SOUTH || direct == Direction.SOUTHEAST || direct == Direction.SOUTHWEST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(southE) && !rc.isLocationOccupied(southW)) { 
//    			return Direction.SOUTHWEST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(southE) && rc.isLocationOccupied(southW)) { 
//    			return Direction.SOUTHEAST;
//    		}
//    		
//    		else { //both directions are empty
//    			double ePass = rc.sensePassability(southE);
//        		double wPass = rc.sensePassability(southW);
//        		if(ePass > wPass) //nPass in better
//        			return Direction.SOUTHEAST;
//        		else if (ePass < wPass)
//        			return Direction.SOUTHWEST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.SOUTHEAST;
//            		else
//            			return Direction.SOUTHWEST;
//        		}
//    		}
//    	}    
//    	
//    	else if(occupiedDirection == Direction.NORTH) {
//    		MapLocation northE = current.add(Direction.NORTHEAST);
//    		MapLocation northW = current.add(Direction.NORTHWEST);
//    		
//    		if(rc.isLocationOccupied(northE) && rc.isLocationOccupied(northW)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.NORTH || direct == Direction.NORTHEAST || direct == Direction.NORTHWEST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(northE) && !rc.isLocationOccupied(northW)) { 
//    			return Direction.NORTHWEST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(northE) && rc.isLocationOccupied(northW)) { 
//    			return Direction.NORTHEAST;
//    		}
//    		
//    		else { //both directions are empty
//    			double ePass = rc.sensePassability(northE);
//        		double wPass = rc.sensePassability(northW);
//        		if(ePass > wPass) //nPass in better
//        			return Direction.NORTHEAST;
//        		else if (ePass < wPass)
//        			return Direction.NORTHWEST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.NORTHEAST;
//            		else
//            			return Direction.NORTHWEST;
//        		}
//    		}
//    	}  
//    	else if(occupiedDirection == Direction.EAST) {
//    		MapLocation northE = current.add(Direction.NORTHEAST);
//    		MapLocation southE = current.add(Direction.SOUTHEAST);
//    		
//    		if(rc.isLocationOccupied(northE) && rc.isLocationOccupied(southE)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.EAST || direct == Direction.NORTHEAST || direct == Direction.SOUTHEAST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(northE) && !rc.isLocationOccupied(southE)) { 
//    			return Direction.SOUTHEAST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(northE) && rc.isLocationOccupied(southE)) { 
//    			return Direction.NORTHEAST;
//    		}
//    		
//    		else { //both directions are empty
//    			double ePass = rc.sensePassability(northE);
//        		double wPass = rc.sensePassability(southE);
//        		if(ePass > wPass) //nPass in better
//        			return Direction.NORTHEAST;
//        		else if (ePass < wPass)
//        			return Direction.SOUTHEAST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.NORTHEAST;
//            		else
//            			return Direction.SOUTHEAST;
//        		}
//    		}
//    	} 
//    	else { //(occupiedDirection == Direction.WEST)
//    		MapLocation northE = current.add(Direction.NORTHWEST);
//    		MapLocation southE = current.add(Direction.SOUTHWEST);
//    		
//    		if(rc.isLocationOccupied(northE) && rc.isLocationOccupied(southE)) {
//    			Direction direct= randomDirection();
//        		while (direct == Direction.WEST || direct == Direction.NORTHWEST || direct == Direction.SOUTHWEST)
//        			direct= randomDirection();
//        		return direct;
//    		}
//    		
//    		else if(rc.isLocationOccupied(northE) && !rc.isLocationOccupied(southE)) { 
//    			return Direction.SOUTHWEST;
//    		}
//    		
//    		else if(!rc.isLocationOccupied(northE) && rc.isLocationOccupied(southE)) { 
//    			return Direction.NORTHWEST;
//    		}
//    		
//    		else { //both directions are empty
//    			double ePass = rc.sensePassability(northE);
//        		double wPass = rc.sensePassability(southE);
//        		if(ePass > wPass) //nPass in better
//        			return Direction.NORTHWEST;
//        		else if (ePass < wPass)
//        			return Direction.SOUTHWEST;
//        		else { //nPass == ePass -> add some randomness
//        			if(coin <0.5)
//            			return Direction.NORTHWEST;
//            		else
//            			return Direction.SOUTHWEST;
//        		}
//    		}
//    	} 
//    }

   
    
    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
//        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
    
    static void maybeSendECLocation() throws GameActionException {
    	RobotInfo[] nearbyBots = rc.senseNearbyRobots();
    	if (rc.getFlag(rc.getID()) != 0) {
    		rc.setFlag(0);
    	} else {
	    	for (RobotInfo ri : nearbyBots) {
	    		if (ri.getType() == RobotType.ENLIGHTENMENT_CENTER) {
	    			FlagInfo fi = new FlagInfo();
	    			fi.location = ri.getLocation();
	    			fi.team = ri.getTeam();
	    			fi.conviction = ri.getConviction();
	    			System.out.println("Setting flag to " + fi.encoded());
	    			rc.setFlag(fi.encoded());
	    		}
	    	}
    	}
    }
    
    static void signalAttackEC(ECInfo ec) throws GameActionException {
    	FlagInfoEC fi = new FlagInfoEC();
    	fi.location = ec.location;
    	fi.attack = true;
    	rc.setFlag(fi.encoded());
    }
    
    static void clearFlag() throws GameActionException {
    	rc.setFlag(0);
    }
    
}
