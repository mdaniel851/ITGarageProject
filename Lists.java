/*
 * Lists is a class made for keeping the lists and synchronizing their access.
 * This class is also used in many places so access must be synchronized as well
 * as limited.  The singleton pattern is employed here so there is only one 
 * instance of the class and one set of lists. 
 */

package servers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Lists {
	public static Lists lists;
	public List<Battle> activeBattles;
	public List<Client> idle;
	public List<Client> waiting;
	public List<Client> waitingQuick;
	public List<Client> map;

	private Lists() {

		map = Collections.synchronizedList(new ArrayList<Client>());
		idle = Collections.synchronizedList(new ArrayList<Client>());
		waiting = Collections.synchronizedList(new ArrayList<Client>());
		waitingQuick = Collections.synchronizedList(new ArrayList<Client>());
		activeBattles = Collections.synchronizedList(new ArrayList<Battle>());
	}

	public static Lists getLists() {
		if (lists == null) {
			lists = new Lists();
		}
		return lists;
	}

	
	/*
	 * This method is used to increase the priority of clients left over in the waiting
	 * list.
	 * 
	 * no input/output
	 */
	
	public void prioritizeWaiting() {
		for (Client c : waiting) {
			c.increasePriority();
		}
	}

	
	/*
	 * flush is used to purge all the lists.  This is used during debug or testing.
	 * 
	 * no input/output
	 */
	public synchronized void flush() {
		if (this.activeBattles.size() > 0) {
			for (Battle b : activeBattles) {
				b.stop();
			}
			activeBattles.clear();
		}

		if (waiting.size() > 0) {
			for (Client c : waiting) {
				c.getService().stop();
			}

			waiting.clear();
		}

		if (waitingQuick.size() > 0) {
			for (Client c : waitingQuick) {
				c.getService().stop();
			}
			waitingQuick.clear();
		}

		if (idle.size() > 0) {
			for (Client c : idle) {
				c.getService().stop();
			}
			idle.clear();
		}

		map.clear();
	}

	
	/*
	 * The jump method is used by a client to place themselves into the waiting for a
	 * match queue
	 * 
	 * Input: Client object, usually 'this'.
	 * Output: none
	 */
	
	public synchronized void jump(Client c) {
		for (int i = 0; i < waiting.size(); i++) {
			if (c.compareTo(waiting.get(i)) > 0) {
				waiting.add(i, c);
				break;
			}
		}

		idle.remove(c);
	}

	
	// Same as above, but for the unordered first come first serve match type. No ranking
	
	public synchronized void jumpQuick(Client c) {
		waitingQuick.add(c);
		idle.remove(c);
	}

	
	/*
	 * This method allows a client to remove themselves from the list waiting.  This means 
	 * they are still in a position to opt out of facing off in a battle.
	 * 
	 * Input: a client object
	 * Output: none 
	 */
	public synchronized void jumpBack(Client c) {
		
		if(c.isMoveable()){
			waitingQuick.remove(c);
			idle.add(c);
		}
	}

	
	/*
	 * getClosestOpponents 
	 * 
	 * Input: a client object
	 * Output: returns a csv string with the  closest opponents to the client object 
	 */
	
	public String getClosestOpponents(Client c) {
		
		String returnString = "";
		List<Client> list = new ArrayList<Client>();

		if (idle.size() < 10) {
			list.addAll(idle);
		} else {
			list.addAll(idle.subList(0, 9));
			if (!list.contains(c)) {
				list.remove(0);
				list.add(c);
			}
		}

		for (Client client : list) {
			returnString = returnString + client.toString() + ",";
		}
		return returnString;
	}

	
	/*
	 * This method is used when a player is taken out of the logged in list and returned
	 * to the idle list
	 * 
	 * Input: client object
	 * Output: none
	 */
	
	public synchronized void removeFromLoggedIn(Client c) {
		map.remove(c);
		idle.remove(c);
	}

	
	/*
	 * The method removeFromActiveBattles takes the two clients who participated in the
	 * battle and returns them to the idle list.  The battle instance is discarded and
	 * left for garbage collection.
	 * 
	 * Input: battle object to be discarded
	 * Output: none
	 */
	
	public synchronized void removeFromActiveBattles(Battle b) {
		Client c1 = b.getPlayer1();
		Client c2 = b.getPlayer2();
		activeBattles.remove(b);

		try {
			idle.add(c1);
		} catch (Exception e) {
			c1.endSession();
			map.remove(c1);
		}

		try {
			idle.add(c2);
		} catch (Exception e) {
			c2.endSession();
			map.remove(c2);
		}
	}

	
	/*
	 * showMap is used to display all active clients.  This is generally used for
	 * debugging purposes.
	 * 
	 * Input: none
	 * Output: string user names are written to stdout
	 */
	public void showMap() {
		int i = 1;
		List<Client> cs = new ArrayList<Client>();

		cs.addAll(map);
		Collections.sort(cs);

		if (map.size() > 0) {
			for (Client c : cs) {
				System.out.print("User Name: "
						+ c.toString().replaceAll(";", " ") + "\t\t");

				if (i % 3 == 0) {
					System.out.println();
					i = 1;
				}

				i++;
			}
			System.out.println();
		} else {
			System.out.println("Empty Map");
		}
	}

	
	/*
	 * This method adds a client to the active players list
	 * 
	 * Input: a client object
	 * Output: none
	 */
	
	public void addToMap(Client c) {
		map.add(c);
	}

	
	/*
	 * This method is used to determine if a player is already signed in or not.
	 * 
	 * Input: string user id 
	 * Output: boolean, true if the client is in the list, false otherwise
	 */
	public boolean isInMap(String userId) {
		for (Client client : map) {
			if (client.getUserID().equals(userId)) {
			}
		}
		return false;
	}
}
