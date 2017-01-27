/*
 * This is one of the main classes of the server app.  It is used to 
 * record all the relevant information regarding the client, and any constructs
 * made from the info.  It has instances of two helper classes, Comms and 
 * Service.  The tcp communication is handled by calling methods in Comms
 * and the thread associated to a client is handled in Service.  
 */

package servers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.net.Socket;

public class Client implements Comparable<Client> {
	
	private int 			points;
	private int 			priority = 0;
	private Comms 			comms;
	private Service 		service;
	private String 			userID = null;
	private String 			password = null;
	private String 			action = null;
	private int 			numPartTypes;
	private String[] 		robotPartsList;
	private Robot 			myRobot;
	private Lists 			lists;
	private Database 		db;
	private DBObject 		myDBInfo;
	private Values.States 	currentService;
	private boolean 		moveable = true;
   
	// Constructor
	public Client(Socket socket) throws Exception {
		comms = new Comms(socket);
		db = Database.getDatabase();
		lists = Lists.getLists();
		getLoginInfo();

		try {
			if (lists.isInMap(userID))
				throw new Exception();
			myDBInfo = db.authenticate(userID, password);
		} catch (Exception e) {
			comms.write("fail,");
			endSession();
			throw new Exception();
		}

		numPartTypes = db.numberofPartTypes();
		robotPartsList = new String[this.numPartTypes];

		points = calculatePoints();
		comms.write("logged in," + getAvailiblePartsList());

		currentService = Values.States.idle;

		service = new Service(this, comms);
		lists.addToMap(this);

		if (Values.debug) {
			System.out.println("Client Accepted: ID: " + getUserID()
					+ "\tSigned in: true");
		}
	}

	
	/*
	 * This is the implementation of the Comparable interface.  It is used for
	 * rank ordering the clients who are queued for a match.
	 */
	
	public int compareTo(Client c) {
		if (!moveable)
			return metric(c);
		return Integer.MIN_VALUE;
	}

	
	/*
	 * This method is used by compareTo to compute the rank of a client.
	 */
	public int metric(Client c) {
		int alpha = 90;
		int beta = 10;

		int temp;
		if (currentService.equals(Values.States.waiting)) {
			temp = alpha
					* (myRobot.getAttackStat() - c.getRobot().getAttackStat());
			temp += beta * (points - c.getPoints());
		} else {
			temp = points - c.getPoints();
		}

		return temp + priority;
	}

	public void increasePriority() {
		
		if(moveable)
			priority += 100;
	}


	/*
	 * The following 3 methods are used to keep clients in the appropriate queue
	 * or allow them to move.  Ex.  if a clients requests a match and is about to 
	 * be assigned, keep them from leaving.  
	 */
	
	public boolean isMoveable() {
		return moveable;
	}
	
	public void stayPut() {
		moveable = false;
	}

	public void moveFreely() {
		moveable = true;
	}

	
	/*
	 * This method is used to take a string array and assign the parts to
	 * the correct place and validate.  The string array contains info
	 * about the players robot that they have selected from their given parts 
	 * list.  If the format is not correct, if there is no content, or if the
	 * clients signs out, an exception is thrown 
	 */

	public void unpack(String[] input) throws Exception {
		int[] ac = new int[2];

		if ((input == null) || (input.length < 6)
				|| (input[1].equals("signout"))) {
			throw new Exception();
		}
		for (int i = 0; i < numPartTypes; i++) {
			robotPartsList[i] = input[(i + 1)].trim();
		}

		ac = db.getAttackDefend(robotPartsList);
		myRobot = new Robot(ac);
	}

	
	/*
	 * This following two methods are used to validate and update
	 * the next move to be made by the client
	 * 
	 * updateAction has no input or output
	 */
	
	public void updateAction() {
		
		String[] ss = null;
		action = null;

		try {
			ss = comms.read();
			action = ss[1].trim();
			if (!isAcceptableAction(action)) {
				action = Values.Action.forfeit.name();
			}
		} catch (Exception e) {
			this.action = Values.Action.forfeit.name();
		}
		myRobot.setAction(action);
	}

	
	/*
	 * Input: string
	 * 
	 * Output: boolean, true if the string corresponds to a valid move,
	 * false otherwise
	 */
	private boolean isAcceptableAction(String s) {
		
		Values.Action act = Values.Action.valueOf(s);

		switch (act) {
			case attack: 	return true;
			case heal:		return true;
			case charge:	return true;
			case forfeit:	return true;
			case defend:	return true;
			default:		return false;
		}
	}


	/*
	 * This method logs out the user by removing the client from all
	 * salient lists, and stopping the Service thread from running.  It 
	 * will also invoke the cleanUp method to make null all references
	 * to instance parameters
	 * 
	 * no input/output
	 */
	
	public void endSession() {
		
		lists.removeFromLoggedIn(this);
		service.stop();
		
		try {
			comms.closeConnection();
		} catch (Exception e) {
			System.out.println("Client: error closing connection");
		}
		cleanUp();
	}
   
	/*
	 * This method points all instance variables to null for garbage
	 * collection to be as quick as possible
	 * 
	 * No input or output
	 */
	
	private void cleanUp(){
		
		comms = null;
		service = null;
		userID = null;
		password = null;
		action = null;
		robotPartsList = null;
		myRobot = null;
		lists = null;
		db = null;
		myDBInfo = null;
		currentService = null;
		
	}
  
  
	/*
	 * Calculate the points a player has earned in their lifetime
	 * 
	 * Input: none
	 * Output: integer value corresponding to points earned
	 */
	
	private int calculatePoints() {
		
		int temp = 0;
		
		temp += (int) myDBInfo.get("wins") * Values.winPoints;
		temp += (int) myDBInfo.get("draws") * Values.drawPoints;
		temp += (int) myDBInfo.get("losses") * Values.lossPoints;

		return temp;
	}

  
	// Getter-Setter Methods ---------------------------------------------------------
	
	public String essentials() {
		
		int[] stats = new int[5];
		String s = "";
		
		myDBInfo = db.getUserInfoFromDB(userID, password);
		
		stats[0] = (int) myDBInfo.get("wins");
		stats[1] = (int) myDBInfo.get("losses");
		stats[2] = (int) myDBInfo.get("draws");
		stats[3] = calculatePoints();
		stats[4] = (int) myDBInfo.get("scrap");

		for (int i = 0; i < stats.length; i++) {

			s = s + i + ",";
		}
		
		return s;
	}
	private void getLoginInfo() throws Exception {
		String[] ss = comms.read();
		userID = ss[0].trim();
		password = ss[1].trim();
	}
	
	public void setCurrentStatus(Values.States serve) {
		currentService = serve;
	}

	public Values.States getCurrentStatus() {
		return currentService;
	}

	public String getAvailiblePartsList() {
		String str = "";

		DBObject parts = (BasicDBObject) myDBInfo.get("parts");

		for (int i = 0; i < db.numberOfParts(); i++) {
			boolean hasPart = (boolean) parts.get(i + "");

			if (hasPart) {
				str = str + i + ",";
			}
		}

		return str;
	}
  
	public String getFlatPartsList() {
		
		String str = "";
		
		for (int i = 0; i < robotPartsList.length; i++) {
			str = str + ",";
		}
		
		return str;
	}
  
	public Comms getComms() {
		return comms;
	}

	public Service getService() {
		return service;
	}

	public String getUserID() {
		return userID;
	}

	public String getAction() {
		return action;
	}

	public Robot getRobot() {
		return myRobot;
	}

	public int getPoints() {
		return points;
	}

	public String toString() {
		return userID + ";" + points;
	}
}