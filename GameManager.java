/*
 * The purpose of the GameManager class is to assign clients from the various queues
 * into their desired target.  The clients in the blocking queue coming from the Listener 
 * thread have their service thread started here if sign in is successful.  The other main 
 * task of this thread is to assign already logged in clients to battles.  This thread starts 
 * all the other necessary threads regarding clients and battlesBattles require2 clients.  
 * Since, clients can move between queues depending on their desired service, lock and unlock 
 * methods are used on waiting queues to keep a consistent list of clients.
 * 
 * Note: since the project was meant to be a vertical slice the ranked match was not fully
 * implemented, but key elements have been roughed-in.
 */

package servers;

import java.util.Collections;

import java.util.concurrent.BlockingQueue;

public class GameManager implements Runnable {
	
	private BlockingQueue<Client> clients;
	private boolean isStopped = false;
	private ThreadGroup tg;
	private Lists lists;
	private Thread runningThread = null;
	private Timer timer;

	//Constructor
	public GameManager( BlockingQueue<Client> blockingQueue ) {
		
		clients = blockingQueue;
		tg = new ThreadGroup("Battle");
		lists = Lists.getLists();
		timer = new Timer(15);
	}

	
	/*
	 * The run method is the implementation of the runnable interface.  This will allow
	 * GameManager to run as a thread.  In the main while loop of the thread there are
	 * 2 key functionalities.
	 *   
	 * 1. The method fillLoggedInList is called.  This function will move clients from 
	 * the blocking queue being populated by the Listener class.  If there are no new
	 * clients in the queue this action will not be taken.  Note: it can be adjusted so
	 * that only a minimum number will be taken at one time, since this is a prototype 
	 * low traffic is assumed
	 * 
	 * 2. The method makeQuickMatch is invoked to use the current list of clients waiting 
	 * to be assigned a match to pair them off into battles and start a thread to facilitate
	 * communication between them.  This method is invoked every 15 s so that players can pool
	 * which will facilitate ranking in a better way. Note: this is the implementation of just 
	 * one game lobby, others can be brought in by using the same scheme (another if statement).
	 * 
	 * no input/output
	 */
	
	public void run() {
		synchronized (this) {
			runningThread = Thread.currentThread();
		}

		System.out.println("GameManager Thread Running...");

		while (!isStopped) {
			try {

				if ((tg.activeCount() < 101) && (lists.idle.size() < 101)) {
					if (!clients.isEmpty()) {

						fillLoggedInList();
					}

					if (timer.waitedLongEnough()) {
						makeQuickMatch();
						timer.start();
					}

				} else {
					Thread.sleep(1000L);
				}
			} catch (Exception e) {
				if (!Values.debug) {
					System.out.println("failure in Game Manager Loop");
				}
			}
		}

		System.out.println("GameManager Stopped: " + runningThread.getId());
	}

	
	/*
	 * The purpose of makeQuickMatch is to assign clients from the waitingQuick queue to
	 * a battle.  lock, unlock are called to prevent the list from becoming corrupted.  lock
	 * forces the clients in the list to stay in the list until unlock is called.  Clients
	 * are sorted here before being assigned.  The matches are between closest ranked players
	 * 
	 * no input or output
	 */
	
	private void makeQuickMatch() {
		
		Client c1 = null;
		Client c2 = null;

		lock();

		if (lists.waitingQuick.size() > 1) {
			int limit = lists.waitingQuick.size() / 2;
			Collections.sort(lists.waitingQuick);

			for (int i = 0; i < limit; i++) {
				c1 = lists.waitingQuick.remove(0);
				c2 = lists.waitingQuick.remove(0);

				makeBattle(c1, c2);
			}
		}

		unlock();
	}

	
	/*
	 * The purpose of makeBattle is to take 2 clients and assign them to a new instance
	 * of Battle and then start the thread.  Also, their current queue location is 
	 * updated by placing the Battle instance into the list of battles.
	 * 
	 * Input: 2 Client objects
	 * Output: none
	 */
	
	private void makeBattle(Client c1, Client c2) {
		Battle b = new Battle(c1, c2);

		c1.getService().setCurrentBattle(b);
		c2.getService().setCurrentBattle(b);

		lists.activeBattles.add(b);

		new Thread(tg, b, "" + System.currentTimeMillis()).start();
	}

	
	/*
	 * fillLoggedInList will dequeue at most 10 clients at a time from the blocking queue
	 * and move them into the list of idle clients.  By signing in they have been put on 
	 * the map of active clients and need not be added again here.  Also, the thread 
	 * associated to each client must be started so that service can be provided.
	 * 
	 * no input or output
	 */
	private void fillLoggedInList() throws Exception {
		int i = 0;

		while (i++ < 10 && !clients.isEmpty()) {
			Client c = clients.take();
			new Thread(c.getService()).start();
			lists.idle.add(c);
		}
	}

	
	/*
	 * The following 2 methods, lock and unlock, are used to preserve the integrity of the 
	 * waiting list.
	 * 
	 * Since clients can move between lists while they logged in, they must be kept from
	 * leaving or being timed out when inserting them into a battle.  If they are inserted
	 * a null reference will be made inside the battle and an error will be generated.
	 * This is done by changing the moveable flag inside the client class.  This flag also
	 * influences the priority so that if clients come into the queue after lock has been 
	 * called they are pushed to the end of the queue.  Since there is a limit of the number
	 * of clients being assigned the lowest priority, or odd man out, will not be assigned 
	 * 
	 *  no input or output
	 *  
	 *  ----------------------------------------------------------------------------------
	 */
	
	private void lock() {
		for (Client c : lists.waitingQuick)
			c.stayPut();
	}

	private void unlock() {
		for (Client c : lists.waitingQuick) {
			c.moveFreely();
		}
	}

	// end lock/unlock -------------------------------------------------------------------
	
	
	/*
	 * stop is used to kill the main thread loop.  This is done when re-initializing or 
	 * exiting this server application.
	 * 
	 * no input/output
	 */
	
	public synchronized void stop() {
		isStopped = true;
	}

	// getter- setter --------------------------------------------------------------------
	public ThreadGroup getThreadGroup() {
		return tg;
	}
}
