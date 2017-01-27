/*
 * This class contains the main method.  It starts 2 threads: GameManger and Listener.
 * MultiThreadedServerTester will also provide some debugging options which are read in
 * from the console/stdin and printed back out to console/stdout.  This thread sleeps
 * for 1 second before reading any input.  The sleep is this length of time because less 
 * running time overhead will be incurred by this thread which is not responsible for 
 * doing a lot.
 * 
 * The options are ...
 * 
 * kill 	- hard stop of the server.  All threads stop when exit(0) is called.
 * stop 	- soft stop of the server.  The GameManager and Listener threads are stopped but
 * 		  	  the battle threads are allowed to complete before exiting.
 * flush 	- removes all clients by stopping their service threads and clearing the logged in
 * 			  map.
 * poll		- outputs the number of active battles
 * threads	- outputs the number of active threads
 * map		- lists all the active users
 * debug 	- toggles debug mode on/off.  Debug mode will allow printing to the console from various
 * 			  parts of the application.  Example: each move of a battle are written to the console, or
 * 			  certain error messages are printed.
 *  
 */

package servers;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class MultiThreadedServerTester {
	public static boolean isStopped = false;

	public static void main(String[] args) {
		java.util.concurrent.BlockingQueue<Client> requests = new ArrayBlockingQueue<Client>(
				1028);
		GameManager gameManager = new GameManager(requests);
		Listener server = new Listener(6789, requests);
		Scanner kbd = new Scanner(System.in);

		Lists lists = Lists.getLists();

		new Thread(server).start();
		new Thread(gameManager).start();
		ThreadGroup tg = gameManager.getThreadGroup();

		String inputString = "";

		showPrompt();

		do {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (kbd.hasNext()) {
				inputString = kbd.nextLine();

				if (inputString.equals("kill")) {
					isStopped = true;
					System.out.println("Stopping Server...");
					System.out.println("Stopping Game Manager...");
					gameManager.stop();
					server.stop();
					showPrompt();
				}

				if (inputString.equals("stop")) {

					isStopped = true;
					System.out.println("Stopping Server...");
					System.out.println("Stopping Game Manager...");
					server.stop();
					gameManager.stop();
					lists.flush();

					while (tg.activeCount() > 0) {
						try {
							Thread.sleep(1000L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					showPrompt();
				}

				if (inputString.equals("flush")) {
					try {
						lists.flush();
						System.out.println("Flushed");
					} catch (Exception e) {
						System.out.println("Could not flush client list");
					}
					showPrompt();
				}

				if (inputString.equals("poll")) {
					System.out.println("Map: " + lists.map.size());
					System.out.println(" Idle: " + lists.idle.size());
					System.out.println(" Waiting Quick: "
							+ lists.waitingQuick.size());
					System.out.println(" Active Battles: "
							+ lists.activeBattles.size());
					showPrompt();
				}

				if (inputString.equals("map")) {
					lists.showMap();
					showPrompt();
				}

				if (inputString.equals("threads")) {
					System.out.println("Number of running threads: "
							+ Thread.activeCount());
					showPrompt();
				}

				if (inputString.equals("debug")) {
					if (Values.debug) {
						Values.debug = false;
						System.out.println("Debug Off");
						showPrompt();
					} else {
						Values.debug = true;
						System.out.println("Debug On");
					}

				}

			}

		} while (!isStopped);

		kbd.close();
		System.exit(0);
	}

	public static void showPrompt() {
		if (!Values.debug) {
			System.out.print(">");
		}
	}
}
