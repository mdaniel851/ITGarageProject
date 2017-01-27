package servers;
 
public class Battle implements Runnable { 

	private int 			turnCounter = 1;
	private boolean 		isStopped = false;
	private Values.Result 	statusP1;
	private Values.Result 	statusP2;
	private Client 			player1;
	private Client 			player2;
	private Database 		db;
	private Thread 			current;
	private String 			sessionID; 
	private Lists 			lists; 
	private int 			synchronizer = 0;
   
	/*
	 * The constructor for this class takes in the two client objects 
	 * which are to do battle.
	 */
	public Battle(Client c1, Client c2){
		
		this.player1 = c1;
		this.player2 = c2;
		this.statusP1 = Values.Result.none;
		this.statusP2 = Values.Result.none;
		this.lists = Lists.getLists();
		this.db = Database.getDatabase();
		
	} // end constructor
	
	
	/*
	 * This method is the implementation of runnable interface.  This code
	 * block will execute as a thread.  This thread mediates between two client
	 * threads as moves are made during the match.  IO for the clients are done 
	 * their own, already running, threads.  This thread will only block when
	 * waiting for the clients to respond.
	 * 
	 * This method maintains data regarding the match status and updates it on 
	 * every turn.  The match will be decided to be win, loss, draw, or forfeit
	 * here and the players will be updated with their current stats and the stats 
	 * of their opponent. 
	 * 
	 *  There is error checking being done here.  If either player does not respond 
	 *  in time, times out, then the match will still be decided as a loss and it 
	 *  will be recorded in the database.
	 *  
	 */
	
	public void run(){

		this.current = Thread.currentThread();
		this.sessionID = this.current.getName();
		
		try{ // pre-Battle 
			
			if (Values.debug) {
				System.out.println( "Making Match: " + current.getName() );
				System.out.println( player1.getRobot().toString() + " " + 
									player2.getRobot().toString());
			}
			
			// Notify players match is starting
			player1.getService().request(	Values.CA.write, sessionID + "," + 
											player2.getUserID() + "," + 
											player2.getFlatPartsList() + 
											player1.getRobot().toString() + "," + 
											player2.getRobot().toString() + ","	);
       
			player2.getService().request(	Values.CA.write, sessionID + "," + 
											player1.getUserID() + "," + 
											player1.getFlatPartsList() + 
											player2.getRobot().toString() + "," + 
											player1.getRobot().toString() + ","	);
       
 
			hold(); // wait for responses
      
 			
 			
			player1.getService().request( Values.CA.read, null );
			player2.getService().request( Values.CA.read, null );
       
			hold(); // wait for responses
       
			updatePlayers();

		} catch ( Exception e ){
			this.isStopped = true;
			System.out.println("Failure to start battle action, Battle");
		}

		
		while ( playing() && !isStopped ){ // main loop for the battle

			if (Values.debug) { 
				System.out.println( "Round: " + turnCounter );
			}
       
			try{
				
			
				// Get the players actions, wait for both to respond
				player1.getService().request(Values.CA.update, null);
				player2.getService().request(Values.CA.update, null);
				hold();
        

				if ( Values.debug ) {
					System.out.println( "ID: " + player1.getUserID() + "\tID: " + player2.getUserID() );
					System.out.println( player1.getAction() + "\t\t" + player2.getAction() );
				}
         
 
				if ( isForfeit() ) {
					updatePlayers();
					
				}else{
					
					// Take turn, get result
					player1.getRobot().attackedBy( player2.getRobot().attacks() );
					player2.getRobot().attackedBy( player1.getRobot().attacks() );
 					turnCounter += 1;
 				          
					if ( turnCounter > Values.maxTurns ) { // check for max turns
						tooManyTurns();
						updatePlayers();
						break;
					}
           
					outcomeOfRound();
					updatePlayers();
         				
					if ( Values.debug ) {
						System.out.println( player1.getRobot().getHealth() 
									+ "\t\t" + player2.getRobot().getHealth() );

						System.out.println( player1.getRobot().getCoolDowns() 
									+ " \t\t" + player2.getRobot().getCoolDowns() );
	
					}
				}
			} catch ( Exception e ) {
				this.isStopped = true;
			}
		}




		lists.removeFromActiveBattles(this);
		recordMatchDB();
		reportPlayerStats();
		player1.getService().interrupt();
		player2.getService().interrupt();
		player1.moveFreely();
		player2.moveFreely();
		
		if (Values.debug) {
			System.out.println("Ending Battle: " + current.getName());
		}
		cleanUp();

	}// end run


	/*
	 * This method records the match results to the database.
	 * 
	 */
	private void recordMatchDB(){

		switch (statusP1) {
		
		case win:
			db.incrementWinDB(player1.getUserID());
			db.awardScrap(player1.getUserID(), Values.winScrap);
			break;	
		case draw:
			db.incrementDrawDB(player1.getUserID());
			db.awardScrap(player1.getUserID(), Values.drawScrap);
			break;
		case forfeit:
			db.incrementLossDB(player1.getUserID());
			db.awardScrap(player1.getUserID(), Values.lossScrap);
			break;
		case lose:
			db.incrementLossDB(player1.getUserID());
			db.awardScrap(player1.getUserID(), Values.lossScrap);
			break;

		default:
			break;
		}     
		
		switch (statusP2) {
		
		case win:
			db.incrementWinDB(player2.getUserID());
			db.awardScrap(player2.getUserID(), Values.winScrap);
			break;
			
		case draw:
			db.incrementDrawDB(player2.getUserID());
			db.awardScrap(player2.getUserID(), Values.drawScrap);
			break;
		case forfeit:
			db.incrementLossDB(player2.getUserID());
			db.awardScrap(player2.getUserID(), Values.lossScrap);
			break;
		case lose:
			db.incrementLossDB(player2.getUserID());
			db.awardScrap(player2.getUserID(), Values.lossScrap);
			break;

		default:
			break;
		}  
	} // end recordMatchDB

	
	/*
	 * This method reports the win/loss/draw record and the amount of
	 * scrap each player as earned to the corresponding player.  This is
	 * an update of the clients information to be recorded in the client class
	 * and written to the stream so it can be displayed on screen
	 * 
	 * no input or output
	 */
	
	private void reportPlayerStats() {
		try {
			player1.getService().request(Values.CA.write,
					this.player1.essentials());

		} catch (Exception e) {

			player1.endSession();
		}

		try {
			player2.getService().request(Values.CA.write,
					this.player2.essentials());

		} catch (Exception e) {

			player2.endSession();
		}
	} // end reportPlayerStats

	
	/*
	 * This method checks to see if both players are still alive,
	 * and thus still playing
	 * 
	 *  Input: none
	 *  Output: returns boolean, true is both are alive false otherwise
	 */
	
	private boolean playing() {
		return (this.player1.getRobot().isAlive())
				&& (this.player2.getRobot().isAlive());
	} // end playing

	
	/*
	 * This method requests a write to both players.  The current stats are
	 * supplied so they can be displayed on screen.
	 * 
	 * no input or output
	 */
 
	private void updatePlayers() throws Exception {
		
		String s1 = player1.getRobot().getHealth() + ","
				+ player2.getRobot().getHealth() + "," + statusP1.name() + ","
				+ player1.getRobot().getCool() + ","
				+ player1.getRobot().getCoolHeal() + "," + turnCounter + ","
				+ player2.getAction() + ",";

		String s2 = player2.getRobot().getHealth() + ","
				+ player1.getRobot().getHealth() + "," + statusP2.name() + ","
				+ player2.getRobot().getCool() + ","
				+ player2.getRobot().getCoolHeal() + "," + turnCounter + ","
				+ player1.getAction() + ",";

		player1.getService().request(Values.CA.write, s1);
		player2.getService().request(Values.CA.write, s2);

		hold();
	} // end updatePlayers
  

 
 

	private void outcomeOfRound() {
		if ((player1.getRobot().getHealth() > player2.getRobot().getHealth())
				&& (player2.getRobot().getHealth() < 1)) {
			statusP1 = Values.Result.win;
			statusP2 = Values.Result.lose;
		}
		if ((player1.getRobot().getHealth() < player2.getRobot().getHealth())
				&& (player1.getRobot().getHealth() < 1)) {
			statusP1 = Values.Result.lose;
			statusP2 = Values.Result.win;
		}
		if ((player1.getRobot().getHealth() == player2.getRobot().getHealth())
				&& (player1.getRobot().getHealth() < 1)) {
			statusP1 = Values.Result.draw;
			statusP2 = Values.Result.draw;
		}
	} // end outcomeOfRound
  
	
	/*
	 * This method checks to see if either player has forfeited.
	 * 
	 * Input: none
	 * Output: returns boolean. True if one of the player has forfeited, false otherwise
	 */
	
	private boolean isForfeit() {
		if ((player1.getAction().equals("forfeit"))
				&& (player2.getAction().equals("forfeit"))) {
			statusP1 = Values.Result.lose;
			statusP2 = Values.Result.lose;
			isStopped = true;
			return true;
		}
		if ((player1.getAction().equals("forfeit"))
				&& (!player2.getAction().equals("forfeit"))) {
			statusP1 = Values.Result.lose;
			statusP2 = Values.Result.win;
			isStopped = true;
			return true;
		}

		if ((!player1.getAction().equals("forfeit"))
				&& (player2.getAction().equals("forfeit"))) {
			statusP1 = Values.Result.win;
			statusP2 = Values.Result.lose;
			isStopped = true;
			return true;
		}

		return false;
		
	} // end isForfeit
 
	
	/*
	 * This method will decide upon the match results if the turn counter reaches 
	 * 20.  The match will still be decided if the turn maximum is reached
	 * 
	 *  no input or output
	 */
	
	private void tooManyTurns() {
		if (player1.getRobot().getHealth() > player2.getRobot().getHealth()) {
			statusP1 = Values.Result.win;
			statusP2 = Values.Result.lose;
		}

		if (player1.getRobot().getHealth() < player2.getRobot().getHealth()) {
			statusP1 = Values.Result.lose;
			statusP2 = Values.Result.win;
		}

		if (player1.getRobot().getHealth() == player2.getRobot().getHealth()) {
			statusP1 = Values.Result.draw;
			statusP2 = Values.Result.draw;
		}
	} // end tooManyTurns

	
	/*
	 * All references are cleaned up so to aid in garbage collection when
	 * the match is concluded.
	 * 
	 * no input or output
	 */
	private void cleanUp() {
		
		statusP1 = null;
		statusP2 = null;
		player1 = null;
		player2 = null;
		current = null;
		sessionID = null;
		db = null;
		
	} // end cleanUp

	
	/*
	 * This method is used for synchronization of the 3 threads involved
	 * in a battle.  This function will cause the battle thread to sleep for
	 * 100 ms if it is waiting on one of the players.  Similar to a spin-lock
	 * but without the run time overhead since sleep is invoked.  The synchronize
	 * variable is reset after each use of hold().
	 * 
	 *   no input or output
	 */
	
	private void hold() throws Exception {
		while (synchronizer < 2) {
			Thread.sleep(100L);
		}

		synchronizer = 0;
	}
	
	// Getter Setter -------------------------------------------------------
	
	public Client getPlayer1() {
		return player1;
	}

	public Client getPlayer2() {
		return player2;
	}

	public String toString() {
		return player1.getUserID() + "," + player1.getAction() + ","
				+ player2.getUserID() + "," + player2.getAction();
	}

	public void synchronize() {
		synchronizer += 1;
	}

	public void stop() {
		isStopped = true;
	}
}

