/*
 * This class is used to manage the tcp communication between this server application
 * and the various clients.  Comms is to be used by the Client class for the abovementioned
 * use.  There key methods are read and write which wrap the input and output stream methods
 * of the same name.  This means that inputs and returns are typed as string arrays instead
 * of byte arrays making parsing and error checking more clear when used by other classes.
 */

package servers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Comms {
	
	private InputStream 	input = null;
	private OutputStream 	output = null;
	private Socket	 		clientSocket = null;
	

	/*
	 * Constructor.  If there is an error establishing the input/output
	 * streams for a socket connection with the client an exception is
	 * thrown.
	 * 
	 * Input: a socket object for a given client
	 * Output: none 
	 */
	
	public Comms( Socket s ) throws Exception {
		
		clientSocket = s;
		input = this.clientSocket.getInputStream();
		output = this.clientSocket.getOutputStream();
		clientSocket.setSoTimeout(60000);
	}

	
	/*
	 * This method is used to close the socket connection and the
	 * input output streams.
	 * 
	 * No input or output
	 */
	
	public void closeConnection() throws Exception {
		
		input.close();
		output.close();
		clientSocket.close();
	}

	
	/*
	 * The write method wraps the OutputStream method of the same name
	 * so that a string can be passed in instead of a byte array.  The string 
	 * passed in will be written to the stream.  If there is a failure to write
	 * an Exception is thrown.
	 * 
	 * Input: the string to be written to the stream 
	 * 
	 * Output: none
	 */
	
	public void write(String response) throws Exception {
		
		output.write( response.getBytes() );
	}

	
	/*
	 * This method wraps the OutputStream method of the same name.  A byte array
	 * is read in from the stream and then converted to a string array for parsing 
	 * by the calling method.  If there is a failure to read from the stream an 
	 * exception is thrown.
	 * 
	 * Input: none
	 * Output: a string array is returned
	 */
	public String[] read() throws IOException {
		
		byte[] request = new byte[100];
		input.read( request, 0, 100 );
		String msg = new String( request );

		return msg.split( "," );
	}

	//Utilities ------------------------------------------------------------------
	
	public void setTimeOut( int s ) throws Exception {
		try {
			this.clientSocket.setSoTimeout(s * 1000);
		} catch (Exception e) {
			closeConnection();

			if (!Values.debug) {
				System.out.println("Could not set time out, Comms");
			}
		}
	}
}