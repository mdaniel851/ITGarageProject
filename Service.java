/*
 * The purpose of the Service class is to be a helper class to Client.  All of the thread
 * activities of a client will be carried out here.  This class is mainly for code readability.
 * 
 * The run method is used to implement the Runnable interface.  The run method will call one of
 * several methods depending on the state which can be changed.  It is one loop acting like several
 * different loops together.
 * 
 * run -> idle 	-> idleList -> run
 * 
 * run -> idle	->joinQuickMatchLobby -> ( battle <-> run ) -> run
 * 									  -> run
 * 
 * run -> idle	->joinMatchLobby 	  -> ( battle <-> run ) -> run
 * 									  -> run
 * 
 * run -> idle	-> ( shop	<-> run ) -> run
 * 
 * run -> Client.endSession (Kills the thread)
 * 
 * Note: the battle method runs as an uninterrupted loop.  The state is changed when GameManager 
 * invokes the setCurrentBattle method in service.  Any other method, such as idle, are ignored
 * and battle is run iteratively until the battle thread calls the interrupt method changing the 
 * state back to idle.
 */

package servers;

import java.io.IOException;

public class Service implements Runnable {
	
	private boolean 	isStopped;
	private boolean 	interrupted;
	private boolean 	started;
	private boolean 	request;
	private Client 		client;
	private Timer 		timer;
	private Lists 		lists;
	private Comms 		comms;
	private Values.CA 	perform;
	private String 		message;
	private Battle 		currentBattle;

	public Service(Client c, Comms cs) {
		
		this.comms = cs;
		this.lists = Lists.getLists();
		this.client = c;
		this.timer = new Timer(15);
		this.isStopped = false;
		this.request = false;
		this.interrupted = false;
		this.started = false;
	}
 
	public void run() {
		synchronized (this) {
			
			while (!isStopped) {
			
				switch (client.getCurrentStatus()) {
				
				case idle:
					idle();
					break;
				case shopping:
					shop();
					break;
				case waiting:
					waitingForBattle();
					break;
				case waitingQuick:
					waitingForBattle();
					break;
				case battling:
					battle();
					break;
				default:
					break;
				
				}// end switch

			}// end while
		}
	}

	private void idle() {
		String[] temp = null;

		//client.setCurrentStatus(Values.States.idle);

		try {
			temp = interruptableRead(1, 180);
			requestedService(temp);
		} catch (Exception e) {
			client.endSession();
		}
	}

   /*
    * This methods decodes the desired action sent by the user and then calls
    * the corresponding method.
    * 
    * Input: The string sent by the client
    * Output: none
    */
	
    private void requestedService(String[] s){
     
    	
    	switch(s[1]){
    	
    		case "list":	idleList();
    						client.setCurrentStatus(Values.States.idle);
    						break;
    	
    		case "match":	joinMatchLobby();
    						timer.start();
    						client.setCurrentStatus(Values.States.waiting);
    						break;
    						
    		case "quick":	joinQuickMatchLobby();
    						timer.start();
    						client.setCurrentStatus(Values.States.waitingQuick);
    						break;
    						
    		case "store":	shop();
    						client.setCurrentStatus(Values.States.shopping);
    						break;
    						
    		case "signout": client.endSession();				
    						break;
    	}
    	
    }
    	
   	
    /*
     * idleList writes back to the client a string containing the closest opponents.
     * 
     * no input/output	
     */
    
	private void idleList() {
		try {
			comms.write(lists.getClosestOpponents(client));
		} catch (Exception e) {
			if (!Values.debug) {
				System.out.println("Failure to write nearest in list, Service");
			}
			client.endSession();
		}
	}

	
	/*
	 * Unimplemented, for unranked match lobby
	 */
	private void joinMatchLobby() {
	}

	
	/*
	 * When a client requests a match they must send the information regarding
	 * the robot parts they are using for the match.  The purpose of this method is
	 * to read that selection and put the client into the correct list quickMatchlobby.
	 * 
	 *  no input/output
	 */
	
	private void joinQuickMatchLobby() {
		
		try {
			client.unpack(interruptableRead(1, 60));

		} catch (Exception e) {

			client.endSession();
			
		}

		lists.jumpQuick(client);
	}

	
	/*
	 * Unimplemented, shop under construction. 
	 */
	
	private void shop() {
		client.setCurrentStatus(Values.States.idle);
	}

	
	/*
	 * This method is invoked when the client has sent their robot information but is yet 
	 * to be assigned to a match.  This method will call Thread.wait while the flag started
	 * is set to false.  The flag is reset here after the state is changed.
	 * 
	 * no input/output
	 */
	
	private void waitingForBattle() {
		
		if (!started) {
			if ((timer.waitedLongEnough()) && (client.isMoveable())) {
				lists.jumpBack(client);
				client.setCurrentStatus(Values.States.idle);
				try {
					comms.write(",,,,,,,,,");
				} catch (Exception e) {
					client.endSession();
				}
			} else {
				try {
					Thread.sleep(100L);

				} catch (Exception Exception1) {
				}
			}

		} else {
			started = false;
			client.setCurrentStatus(Values.States.battling);
		}
	}

	
	/*
	 * the battle method is used to make moves in the Battle thread.  It waits for
	 * a request while the run-battle loop is executing.  When the request is received
	 * the corresponding method is executed: read, update, write.
	 * 
	 * no input/output
	 */
	
	private void battle() {
		
		if ((request) && (!interrupted)) {
		
			switch (perform) {
			
			case read:
				try {
					comms.read();
					currentBattle.synchronize();
				} catch (IOException e) {
					interrupted = true;
					client.endSession();
				}

			case write:
				client.updateAction();
				currentBattle.synchronize();
				break;
				
			case update:
				try {
					comms.write(message);
					currentBattle.synchronize();
				} catch (Exception e) {
					interrupted = true;
					client.endSession();
				}

			default:
				isStopped = true;
				interrupted = true;
				
			}// end switch

			request = false;
			perform = null;
		}else {
			try {
				Thread.sleep(100L);

			} catch (Exception Exception1) {
				client.endSession();
			}
		}
	}

	
	/*
	 * This method is called from the battle thread to let battle() know what to do.
	 */
	public void request(Values.CA perform, String message) {
		this.perform = perform;
		this.message = message;
		request = true;
	}

	
	/*
	 * This method allows for a read that can be interrupted.  This is necessary so that the 
	 * stop method can be carried out which cannot happen while blocking.
	 */
	private String[] interruptableRead(int to, int tries) throws Exception {
		String[] msg = null;
		int i = 0;

		comms.setTimeOut(to);

		while ((i++ < tries) && (!isStopped)) {
			try {
				msg = comms.read();
			} catch (Exception localException) {
			}
		}

		this.comms.setTimeOut(60);

		if (msg == null) {
			throw new Exception();
		}
		return msg;
	}

	
	/*
	 * This is called when the battle is over and the state must be changed.  It signals that
	 * the battle is over and that the main loop should go back to the idle state.
	 */
	public void interrupt() {
		interrupted = true;
		currentBattle = null;
		started = false;
		client.setCurrentStatus(Values.States.idle);
	}

	/*
	 * This method kills the thread.
	 */
	public void stop() {
		isStopped = true;
		interrupted = true;
		started = false;
	}

	
	/*
	 * This method records the current battle and changed the started flag to true.
	 */
	public void setCurrentBattle(Battle currentBattle) {
		this.currentBattle = currentBattle;
		started = true;
	}
}
