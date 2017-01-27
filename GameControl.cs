/*
	This is the client side of the project.  This script is meant to 
	work with the Unity game engine.  It is meant to provide basic 
	connectivity and to keep it during the changing of scenes.  Since
	this script is meant to stay alive for the duration of the application
	a singleton pattern is used here
*/

using UnityEngine;
using System.Collections;
using System.Net.Sockets;
using System.Net;
using System.Text;
using System.Threading;
using UnityEngine.SceneManagement;

public class GameControl : MonoBehaviour {
	//Network and Authentication
	public static GameControl control;
	private TcpClient client = null;
	private NetworkStream connection;
	private string ip = "192.168.2.33";
	private int port = 6789;
	public string userID = "user1";
	private string password;
	private string sessionID;
	public string otherID;
	private int timeOut = 1; //minutes
	public static bool isSignedIn = false;
	public static string Scene = "Menu";
	//public float timeLeft = 180;


	public static string[] UpdatedPartArray;

	// Robot details
	private string[] myParts= new string[6];       //H,T,LA,RA,L
	public static string[] otherParts = new string[6];    //H,T,LA,RA,L


	void Awake () {

		//pseudo-singleton pattern

		if (control == null)
		{
			DontDestroyOnLoad(gameObject);
			control = this;
		}
		else if (control != this)
		{
			Destroy(gameObject);
		}

	}


	// Game Operations -------------------------------------


	public string[] ReadClosestPlayers(){
		string[] ss = Read ();
		foreach (string s in ss) {
			s.Replace (';', ' ');
		}
		PlayerListController.PlayerList = ss;
			
		return ss;
	}

	public void MakeMove(string m)
	{
		Write(sessionID + "," + m);
	}

	public string[] GetResult()
	{
		string[] s;
		s = Read();

		return s;
	}

	public void RequestMatch()
	{
		string output = userID + ","  + myParts[0] + "," + myParts[1] + "," + myParts[2] + "," + myParts[3] + "," + myParts[4] + "," + myParts[5];
		print(output);
		Write(output);
	}

	public void StartMatch()
	{
		string[] input = Read(); // read other players stats, do something with them...
		sessionID = input[0];
		otherID = input [1];

		for (int i = 0 ; i < otherParts.Length; i++)
			otherParts[i] = input[i + 2];


		print("Other's Parts: " + otherParts [0] + "," + otherParts [1] + "," + otherParts [2] + "," + otherParts [3] + "," + otherParts [4] + "," + otherParts [5]);
	}


	//Program Control --------------------------------------

	public bool IsDataAvailable()
	{
		return connection.DataAvailable;
	}

	public void EndMatch()
	{
		otherParts = null;
	}
	public void Confirm()
	{
		Write("confirm,");
	}

	public bool ConfirmLogin()
	{
		print ("Confirming login.");
		string[] ss = Read();
		print ("Setting ss to UpdatedPartList.");
		UpdatedPartArray = ss;
		for (int i = 0; i < UpdatedPartArray.Length; i++) {
		}

		return ss[0].Equals("logged in");
	}

	// Utilities -------------------------------------------

	public void SignIn(string id, string pwd)
	{ 
		userID = id;
		password = pwd;
		Connect();
		Write( id + "," + pwd + "," + "" );
		isSignedIn = true;
	}

	public void NewSession()
	{
		KillSession();// kill the old session
		SignIn(userID,password);// start a new session
		isSignedIn = true;
	}

	public void KillSession()
	{
		GameControl.control.Write ("signout");
		//close the socket, and nwetwork stream
		connection.Close();
		client.Close();
	}
	public void SignOut()
	{
		KillSession();
		password = null;
		userID = null;
		isSignedIn = false;
	}

	public void Connect()
	{
		try
		{
			client = new TcpClient(ip, port);
			connection = client.GetStream();
			connection.ReadTimeout = timeOut * 60 * 1000; // in millis
		}
		catch { }

	}


	// IO Methods ---------------------------------------------

	public string[] Read()
	{
		byte[] b = new byte[100];
		string msg;
		connection.Read(b, 0, b.Length);

		msg = System.Text.Encoding.UTF8.GetString(b, 0, b.Length);
		//print(msg);
		return msg.Split(',');
	}

	public void Write(string message)
	{
		byte[] b = new byte[100];
		b = Encoding.ASCII.GetBytes(message);
		connection.Write(b, 0, b.Length);

	}

	public void OnApplicationQuit(){
		if(client != null){
			if (client.Connected) {
				Write ("signout");
			}
		}
	}

	// Getter / Setter -------------------------------

	public AutoResetEvent Handle
	{
		get
		{
			return (AutoResetEvent)handle;
		}

	}

	public string[] OtherParts
	{
		set
		{
			otherParts = value;
		}
		get
		{
			return otherParts;
		}
	}

	public string[] MyParts
	{

		get
		{
			return myParts;
		}
	}

	public void SetParts(string a, string b, string c, string d, string e, string f)
	{
		myParts[0] = a;
		myParts[1] = b;
		myParts[2] = c;
		myParts[3] = d;
		myParts[4] = e;
		myParts[5] = f;
		print ("My Parts: " + myParts [0] + "," + myParts [1] + "," + myParts [2] + "," + myParts [3] + "," + myParts [4]);
	}
}
