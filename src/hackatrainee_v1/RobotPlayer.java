package hackatrainee_v1;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Robot robo;

    static int turnCount;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        // The game demands we have a single RobotPlayer that acts as various others.
        // To not have ALL the code in this file, we use composition: we assign
        // a Robot of the right type below, which uses inheritance to share common
        // functionality with Robots of other types.
	    switch (rc.getType()) {
	    case ENLIGHTENMENT_CENTER:
	    	robo = new EC(rc);
	    	break;
	    case POLITICIAN:
	    	robo = new Politician(rc);
	    	break;
	    case SLANDERER:
	    	robo = new Slanderer(rc);
	    	break;
	    case MUCKRAKER:
	    	robo = new Muckraker(rc);
	    	break;
	    }
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
            	robo.run();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
