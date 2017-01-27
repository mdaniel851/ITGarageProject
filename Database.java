/*
 * The purpose of the class Database is to provide connection to a mongo server.
 * Also, it is used to update the server for new client info, as well as query the
 * individual robot parts values.  
 * 
 * Since this class is used by several of the the most important classes and 
 * modification of the information needs to be controlled, Database is implemented
 * as a singleton.  
 */

package servers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import java.util.ArrayList;
import java.util.List;

public class Database {

	private static Database database;
	private MongoClient 	client;
	private DB 				db_Users;
	private DB 				db_Parts;
	private DBCollection 	collectionUsers;
	private DBCollection 	collectionParts;
	private int 			numberOfPartTypes = 0;
	private int 			numberOfParts = 0;
	
	
	/*
	 * This method along with the constructor are the implementation of the
	 * singleton pattern
	 * 
	 * --------------------------------------------------------------------------------
	 */
	
	public static synchronized Database getDatabase() {
		
		if (database == null) {
			database = new Database();
			return database;
		}
		return database;
	}

	private Database() {
		try {
			client = new MongoClient("localhost", 27017);
			db_Users = (DB) client.getDatabase("Users");
			db_Parts = (DB) client.getDatabase("Parts");
			collectionUsers = db_Users.getCollection("Users");
			collectionParts = db_Parts.getCollection("Parts");
			numberOfPartTypes = 0;
		} catch (Exception e) {
			if (!Values.debug)
				System.out.println("Could not connect to database");
		}
	}
	// end singleton -------------------------------------------------------------------
	
	
	
	/*
	 * The following methods are used to update a users record (win,loss,draw)
	 * as well as award scrap (in-game currency).  The updates are recorded in
	 * the mongo database.
	 * 
	 * Input: they all require a string, the user name of the client to be updated.
	 * 		  awardScrap also requires the int amount of scrap to be awarded. 	 
	 *
	 * ---------------------------------------------------------------------------------
	 */
	
	
	public synchronized void awardScrap(String userID, int scrap) {
		BasicDBObject query1 = new BasicDBObject("user", userID);
		BasicDBObject query2 = new BasicDBObject().append("$inc",
				new BasicDBObject().append("scrap", Integer.valueOf(scrap)));

		collectionUsers.update(query1, query2);
	}

	public synchronized void incrementWinDB(String userID) {
		BasicDBObject query1 = new BasicDBObject();
		BasicDBObject query2 = new BasicDBObject().append("$inc",
				new BasicDBObject().append("wins", Integer.valueOf(1)));

		query1.put("user", userID);

		collectionUsers.update(query1, query2);
	}

	public synchronized void incrementLossDB(String userID) {
		BasicDBObject query = new BasicDBObject();
		BasicDBObject query2 = new BasicDBObject().append("$inc",
				new BasicDBObject().append("losses", Integer.valueOf(1)));

		query.put("user", userID);

		collectionUsers.update(query, query2);
	}

	public synchronized void incrementDrawDB(String userID) {
		BasicDBObject query = new BasicDBObject();
		BasicDBObject query2 = new BasicDBObject().append("$inc",
				new BasicDBObject().append("draws", Integer.valueOf(1)));

		query.put("user", userID);

		collectionUsers.update(query, query2);
	}

	// end update methods ----------------------------------------------------------
	
	
	/*
	 * This method is used to verify a user on sign in.  If the user name does not
	 * match and exception is thrown.  Also, an exception is thrown if there is no
	 * corresponding user name in the database.  
	 */
	public synchronized DBObject authenticate(String userID, String password)
			throws Exception {
		
		DBObject obj = getUserInfoFromDB(userID, password);
		String s = (String) obj.get("password");
		if (s.equals(password))
			return obj;
		throw new Exception();
	}

	
	/*
	 * getUserInfoFromDB is used to query the database for a user name and return 
	 * the corresponding user data if the password matches.  
	 * 
	 *  Input: two strings, user name and the corresponding password
	 *  Output: returns mongo DBOject
	 */
	public DBObject getUserInfoFromDB(String userID, String password) {
		
		BasicDBObject query = new BasicDBObject();

		query.put("user", userID);
		DBCursor cursor = collectionUsers.find(query);
		DBObject obj = cursor.next();

		if (obj.get("password").equals(password))
			return obj;
		return null;
	}

	/* deprecated
	private DBObject getUserInfo(String userID) {
		BasicDBObject query = new BasicDBObject();

		query.put("user", userID);
		DBCursor cursor = collectionUsers.find(query);
		return cursor.next();
	}
	 */
	
	
	/*
	 * This method is used to ascertain the stats for a given robot setup.
	 * 
	 * Input: a string array containing the parts list for a robot belonging to 
	 * a given client
	 * Output: an int array containing 2 values, [0]attack, and [1]defend
	 */
	
	public synchronized int[] getAttackDefend(String[] partsList) {
		
		int[] ad = new int[2];
	
		for (int i = 0; i < partsList.length; i++) {
			String s = partsList[i];
			DBObject obj = getPartInfo(s);
			ad[0] += ((Integer) obj.get("attack")).intValue();
			ad[1] += ((Integer) obj.get("defend")).intValue();
		}

		return ad;
	}

	
	/*
	 * This method queries the database for a part number match the input.
	 * 
	 * Input: string, part number
	 * Output: mongo DBObject
	 */
	private DBObject getPartInfo(String partID) {
		BasicDBObject query = new BasicDBObject();
		query.put("id", partID);
		DBCursor c = collectionParts.find(query);
		return c.next();
	}

	// Utilities -----------------------------------------------------------
	
	/*
	 * This method returns the number of part types (int) in the database
	 */
	
	public int numberofPartTypes() {
		
		List<String> list = new ArrayList<String>();

		if (numberOfPartTypes == 0) {
			DBCursor cursor = collectionParts.find();

			while (cursor.hasNext()) {
				BasicDBObject dbo = (BasicDBObject) cursor.next();
				String s = dbo.getString("type");

				if (!list.contains(s)) {
					list.add(s);
				}
			}
			numberOfPartTypes = list.size();
		}

		return numberOfPartTypes;
	}
	
	
	/*
	 * This method return the number of parts (int) in the database
	 */
	public synchronized int numberOfParts() {
		
		if ( numberOfParts == 0 )
			numberOfParts = (int) collectionParts.count();
		
		return numberOfParts;
	}
}
