/*
 * Listener is used to listen for tcp clients.  When there is a new client request the socket
 * is used to create an instance of the class Client which is added to the blocking queue. 
 * Listener implements Runnable so that it can run as its own thread.  Threading is useful 
 * here because serverSocket.accept is blocking.  Matches can be made and played with existing
 * clients while this thread sleeps.  
 * 
 * Note: the port 6789 is used arbitrarily for the testing of this application.
 */

package servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class Listener implements Runnable {
	
	private BlockingQueue<Client> 	clients = null;
	private int 					serverPort = 6789;
	private boolean 				isStopped = false;
	private ServerSocket			serverSocket = null;
	private Client 					c = null;	
	private Thread 					runningThread = null;

	public Listener(int port, BlockingQueue<Client> blockingQueue) {
		serverPort = port;
		clients = blockingQueue;
	}

	
	/*
	 * run is the implementation of the Runnable interface.  This methods waits for socket
	 * clients and awakes when there is a request made.  When a request is made a client
	 * object is instantiated and placed in the blocking queue if there is less than the 
	 * limit.  The limit is imposed so the server does not have memory overflow issues.  If
	 * the limit is reached the socket is closed.
	 */
	
	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}

		System.out.println("Server Thread Running...");
		openServerSocket();

		while (!isStopped()) {
			
			Socket clientSocket = null;
			try {
				
				clientSocket = serverSocket.accept();
			
			} catch (IOException e) {
			
				if (isStopped()) {
					System.out.println("Server Stopped.");
					return;
				}
				
				throw new RuntimeException("Error accepting client connection", e);
			}

			try {
				
				if (clients.size() < 101){
					c = new Client(clientSocket);
					clients.add(c);
				}else{
					clientSocket.close();
				}
			
			} catch (Exception e) {
				
				if (Values.debug) {
					System.out.println("Failed to accept client");
				}
			}
		} // end while

		System.out.println("Server Stopped: " + runningThread.getId());
		
	} //end run


	/*
	 * This method is used to stop the main loop in the run method.
	 * 
	 * no input/output
	 */
	
	public synchronized void stop() {
		isStopped = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}
	
	
	/*
	 * openServerSocket is used to instantiate a socket object.  If there is a 
	 * failure to create the socket object an exception is thron
	 * 
	 * no input/output
	 */
	
	private void openServerSocket() {
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port 6789", e);
		}
	}
	
	
	// Getter - Setter ----------------------------------------------------------------
	
	private synchronized boolean isStopped() {
		return isStopped;
	}
}
