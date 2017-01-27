/*
 * Values is a class made for key value stores.  This allows for easy adjustment
 * of various properties used for tuning the application.
 */

package servers;

public class Values {
	public static enum States {
		waiting, waitingQuick, shopping, idle, battling;
	}

	public static enum Result {
		win, lose, draw, forfeit, none;
	}

	public static enum Action {
		attack, defend, heal, forfeit, charge;
	}

	public static enum CA {
		read, write, update;
	}

	public static boolean debug = true;
	public static final int defaultTimeOut = 60;
	public static final int winPoints = 12;
	public static final int drawPoints = 5;
	public static final int lossPoints = 1;
	public static final int winScrap = 25;
	public static final int drawScrap = 10;
	public static final int lossScrap = 5;
	public static final int baseHealth = 50;
	public static final int waitTime = 15;
	public static final int maxTurns = 19;
}
