import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

public class DHTServer extends Thread 
{
	
	private static Thread thread;
	private static int initialize;
	
	private DataInputStream nextInput = null; // Used for server at the front
	private DataOutputStream nextOutput = null;
	private DataInputStream prevInput = null; // Used for server at the back
	private DataOutputStream prevOutput = null;
	

	public int serverID;
	public int port;
	public InetAddress serverIP; // For the same machine
	//public InetAddress serverIP; // For different machines
	
	public int nextServerID;
	public int prevServerID;
	public String nextServerIP;
	public String prevServerIP;
	
	public ServerSocket welcomeSocket; // The TCP handshake socket
	public Socket nextSocket; // Used for server at the back
	public Socket prevSocket; // Used for server at the front
	
	DatagramSocket forP2PClient;

	public String whatToDo = "";
	HashMap<String, Integer> cache = new HashMap<String, Integer>();
	
	DHTServer(int newPort, int ID) throws IOException
	{
		serverID = ID;
		port = newPort;
		welcomeSocket = new ServerSocket(port);
		welcomeSocket.setSoTimeout(9999999);
	}
	
	public int stringToInt(byte[] IP)
	{
		int returnInt = 0;
		for (int i = 0; i < IP.length; i++) 
		{
			returnInt <<= 8;
			returnInt |= IP[i] & 0xff;
		}
		return returnInt;
	}
	
	public byte[] intToIP(int IP)
	{
		 return new byte[] {
				    (byte)((IP >>> 24) & 0xff),
				    (byte)((IP >>> 16) & 0xff),
				    (byte)((IP >>>  8) & 0xff),
				    (byte)((IP       ) & 0xff)
				  };
	}

	public void run()
	{
	
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		while(true) // While the process is still alive
		{
			try
			{
				if(initialize == 0)
				{				
					System.out.println("Waiting for instructions . . .");
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // Get Packet from P2PClient
					forP2PClient.setSoTimeout(10000);
					try 
					{
						forP2PClient.receive(receivePacket);
						String whatToDo = new String(receiveData, 0, receivePacket.getLength()); // Turn packet to string
						System.out.println("Instructions Obtained: " + whatToDo);		
						InetAddress p2pIPAddress = receivePacket.getAddress();
						int p2pPort = receivePacket.getPort();
							if(whatToDo.equals("1")) // init: Send all the IP address of the DHT to P2PClient
							{
								//Obtain the IPs
								InetAddress thisIP = serverIP;
								System.out.println("Obtained this IP Address");
								InetAddress prevIP = Inet4Address.getByName(prevSocket.getInetAddress().getHostAddress());
								System.out.println("Obtained prevServer IP Address");
								InetAddress nextIP = Inet4Address.getByName(nextSocket.getInetAddress().getHostAddress());
								System.out.println("Obtained nextServer IP Address");
								InetAddress unknownIP = requestUnknownIP();
								System.out.println("Obtained unknown IP Address");
								
								
								/* Not used at the moment, might need it later
								 * String server1ip = getRequestedIP(1);
								System.out.println("IP of Server 1: " + server1ip);
								String server2ip = getRequestedIP(2);
								System.out.println("IP of Server 2: " + server2ip);
								String server3ip = getRequestedIP(3);
								System.out.println("IP of Server 3: " + server3ip);
								String server4ip = getRequestedIP(4);
								System.out.println("IP of Server 4: " + server4ip);*/
								
								// Send all the server IPs
								String allServers = thisIP + " " + nextIP + " " + unknownIP + " " + prevIP;
								sendData = allServers.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, p2pIPAddress, p2pPort);
								forP2PClient.send(sendPacket);
								System.out.println("Sent IP Addresses");
								whatToDo = "";
							}
							else if(whatToDo.equals("2"))
							{
								//Tell the client we got the instruction, and we will wait for the content name
								System.out.println("Sending a response . . .");
								String response = "Server " + serverID + " ready to recieve content name . . .";
								sendData = response.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, p2pIPAddress, p2pPort);
								forP2PClient.send(sendPacket);
								
								//Wait for the content
								System.out.println("Waiting for client . . .");
								forP2PClient.setSoTimeout(0); // Set timer to infinite to do a wait and response
								forP2PClient.receive(receivePacket);
								String storeThis = new String(receiveData, 0, receivePacket.getLength());
								
								//Once we got it, we have to turn the IP to int because hashMap's put works with String and int as parameters
								//String will be the content name, int will be the IP address
								//ipAddressStore will contain the int
								//ipAddressStoreName will contain the String
								int ipAddressStore = stringToInt(p2pIPAddress.getAddress());
								String ipAddressStoreName = InetAddress.getByAddress(intToIP(ipAddressStore)).getHostAddress();
								
								System.out.println("Storing: " + storeThis);
								//cache.put(storeThis,ipAddressStore); // This is where we store it
								//System.out.println("Contents Stored ("+storeThis+") from "+ ipAddressStoreName);
								
								
								cache.put(storeThis,p2pPort);  // FOR 1 MACHINE******************
								System.out.println("Contents Stored ("+storeThis+") from "+ p2pPort);
								
								
								forP2PClient.setSoTimeout(5000); // Reset the timer
								whatToDo = "";
							}
							else if(whatToDo.equals("3"))
							{
								//Tell the client we got the instruction, and we will wait for the content name
								System.out.println("Sending a response . . .");
								String response = "Server " + serverID + " is asking what to look for . . .";
								sendData = response.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, p2pIPAddress, p2pPort);
								forP2PClient.send(sendPacket);
								
								//Wait for the content
								System.out.println("Waiting for client . . .");
								forP2PClient.setSoTimeout(0); // Set timer to infinite to do a wait and response
								forP2PClient.receive(receivePacket);
								String storeThis = new String(receiveData, 0, receivePacket.getLength());
								System.out.println("Recieved response from client: "+storeThis);
								
								//Check the hashMap if it contains the content
								String ipAddressStoreName = "404 content not found";
								if(cache.containsKey(storeThis) == true)
								{
									System.out.println("I have the contents");
									
									
									int ipToSend = cache.get(storeThis);  // FOR 1 MACHINE******************
									ipAddressStoreName = ipToSend + "";
									
									
									//int ipToSend = cache.get(storeThis);
									//ipAddressStoreName = InetAddress.getByAddress(intToIP(ipToSend)).getHostAddress();
								}
								else
									System.out.println("I do not have that record");
								
								//Send the ipAddressStoreName which can be either a 404 or the IP address of the content
								System.out.println("Informing the client . . . ");
								sendData = ipAddressStoreName.getBytes();
								sendPacket = new DatagramPacket(sendData, sendData.length, p2pIPAddress, p2pPort);
								forP2PClient.send(sendPacket);
								System.out.println("Packet sent");
								
								forP2PClient.setSoTimeout(5000); // Reset the timer
								whatToDo = "";
							}
							else if (whatToDo.equals("5"))
							{
								//int ipAddressStore = stringToInt(p2pIPAddress.getAddress());
								int ipAddressStore = p2pPort; // FOR 1 MACHINE******************
								
								
								boolean deleted = false;
								int counter = 0;
								String reply = "";
								Iterator<Map.Entry<String,Integer>> iter = cache.entrySet().iterator();
								while (iter.hasNext()) 
								{
								    Map.Entry<String,Integer> entry = iter.next();
								    if(ipAddressStore == entry.getValue())
								    {
								    	{
								    		System.out.println("Deleting content: "+entry.getKey());
								    		iter.remove();
								    		deleted = true;
								    		counter++;
								    	}
								    }
								}
								if(deleted == true)
								{
									System.out.println("Done.");
									reply = "Server "+serverID+" deleted: "+counter+" content(s)";
								}
								else
								{
									System.out.println("Nothing to delete");
									reply = "Server "+serverID+" does not have any records of your contents";
								}
								
								System.out.println("Informing the client . . . ");
								sendData = reply.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, p2pIPAddress, p2pPort);
								forP2PClient.send(sendPacket);
								System.out.println("Packet sent");
							}
					} catch (SocketTimeoutException e) {
						prevOutput.writeUTF(nextSocket.getInetAddress().getHostAddress()); // Send each prevServer the current's server's nextServer IP
						//prevOutput.writeUTF(nextSocket.getInetAddress().getHostAddress()); // Send each prevServer the current's server's nextServer IP
						prevOutput.flush();
						continue;
					}

					
				}
				else if(initialize == 1) // Initialization, connect to respective IPs and ports **********************************************
				{
					if(serverID == 1)
					{
						connectToNextServer("localHost", port+1);
						//connectToNextServer("141.117.232.42",port); // IP Address of serverID == 2
						serverIP = InetAddress.getByName("localHost"); // IP Address of serverID == 1
						waitForPrevServer();
						initialize = 0;
						forP2PClient = new DatagramSocket(40231); // Create a socket to wait for UDP requests

					}
					else if(serverID == 2)
					{
						waitForPrevServer();
						connectToNextServer("localHost", port+1);
						//connectToNextServer("141.117.232.43",port); // IP Address of serverID == 3
						serverIP = InetAddress.getByName("localHost"); // IP Address of serverID == 2
						initialize = 0;
						forP2PClient = new DatagramSocket(40232);
					}
					else if(serverID == 3)
					{
						waitForPrevServer();
						connectToNextServer("localHost", port+1);
						//connectToNextServer("141.117.232.44",port); // IP Address of serverID == 4
						serverIP = InetAddress.getByName("localHost"); // IP Address of serverID == 3
						initialize = 0;
						forP2PClient = new DatagramSocket(40233);
					}
					else if(serverID == 4)
					{
						waitForPrevServer();
						connectToNextServer("localHost", port-3);
						//connectToNextServer("141.117.232.41",port); // IP Address of serverID == 1
						serverIP = InetAddress.getByName("localHost"); // IP Address of serverID == 4
						initialize = 0;
						forP2PClient = new DatagramSocket(40234);
					}
				}
			}
			catch(SocketTimeoutException s) 
			{
	            System.out.println("Socket timed out!");
	            break;
			}
			catch(IOException e)
			{
				e.printStackTrace();
            	break;
			}
			 
		}
	}

	public InetAddress requestUnknownIP() throws IOException // This is called when the current server can't find the right ID from prev or next server
	{
		String returnIP = "";
		returnIP = nextInput.readUTF();
		InetAddress returnThis = Inet4Address.getByName(returnIP);
		return returnThis;
	}
	
	public void waitForPrevServer() throws IOException // Wait for incoming requests
	{
		System.out.println("Waiting for request on socket: " + welcomeSocket.getLocalPort());
		prevSocket = welcomeSocket.accept();
		prevInput = new DataInputStream(prevSocket.getInputStream()); // Create Input and Output stream for DHT server at the back
		prevOutput = new DataOutputStream(prevSocket.getOutputStream());
		System.out.println("Recieved a request from another server's port " + prevSocket);
	}
	
	public void connectToNextServer(String ip, int port) throws UnknownHostException, IOException // Connect to next DHT server
	{
		System.out.println("Trying to connect to port: "+port);
		nextSocket = new Socket(ip, port);
		nextInput = new DataInputStream(nextSocket.getInputStream()); // Create Input and Output stream for DHT at the front
		nextOutput = new DataOutputStream(nextSocket.getOutputStream());
		System.out.println("Connected!");	
	}
	
	public static void main(String [] args)
	{
		int newPort = 40230; // The port used
		int newID = Integer.parseInt(args[0]); // Get the server ID from user
		
		if(newID == 1 || newID == 2 || newID == 3 || newID == 4)
		{
			newPort += newID;
			
			initialize = 1; // Used for connecting to ports and IPs
			System.out.println("This is Server "+newID);
			try
			{
				if(thread == null)
				{
					thread = new DHTServer(newPort, newID); 
					thread.start();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Wrong Server ID (Acceptable IDs: 1, 2, 3 and 4)");
		}
	}
	
}
