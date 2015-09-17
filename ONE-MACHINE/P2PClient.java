import java.net.*;
import java.util.*;
import java.io.*;

public class P2PClient {
	
	//For testing
	public static int[] serverPorts = new int[4];
	//public static int[] serverPort = some numbers;
	public static String[] serverIPs = new String[3]; // 4
	public static String[] contents = new String[10]; // 11
	private static boolean listening;

	
	public static void sender(String msg, DatagramSocket socket, InetAddress address, int port) throws IOException
	{
		byte[] sendData = msg.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		socket.send(sendPacket);
		System.out.println("Sent");
	}
	
	public static String waitAndGet(DatagramSocket socket) throws IOException
	{
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);
		System.out.println("Received response");
		String message = new String(receiveData, 0, receivePacket.getLength());
		return message;
	}
	
	public static int hash(String content)
	{
		System.out.println("Hashing the content name . . .");
		int returnInt = 0;
		char[] charArray = content.toCharArray();
		int xValue = 0; // The x value, for y = x mod 4 where y + 1 is the server
		for(int i = 0; i < content.length(); i++)
		{
			xValue += (int)charArray[i];
		}
		returnInt = xValue % 4;
		int servID = returnInt + 1;
		System.out.println("Content will be sent to Server ID: "+servID);
		return returnInt;// sendTo is our y value
	}
	
	public static void main(String [] args) throws IOException
	{
		Scanner scanner = new Scanner(System.in);
		DatagramSocket socketToServer = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localHost"); // Change this to IP Address of serverID == 1
		
		//For testing
		//serverPorts[0] = 59999;
		//serverPorts[1] = 59998;
		//serverPorts[2] = 59997;
		//serverPorts[3] = 59996;
		
		//THE PORTS USED
		serverPorts[0] = 40231;
		serverPorts[1] = 40232;
		serverPorts[2] = 40233;
		serverPorts[3] = 40234;
		listening = false;
		while(true)
		{
			System.out.println("What to do? (1 = INIT, 2 = INFORM AND UPDATE, 3 = QUERY FOR CONTENT, 5 = EXIT)");
			String sentence = scanner.nextLine();
			
			if(sentence.equals("1")) // Initialize
			{
				//Sending
				System.out.println("Sending Instruction "+sentence+" (Ask for IPs)");
				sender(sentence, socketToServer, IPAddress, serverPorts[0]);
				
				//Waiting
				System.out.println("Waiting for response . . .");			
				//Reading and storing them
				String message = waitAndGet(socketToServer); // Turn packet to string
				serverIPs = message.split("\\s+/");
				serverIPs[0] = serverIPs[0].substring(1);
				for(int i = 0; i <= 3; i++)
				{
					int realNum = i + 1;
					System.out.println("Server "+ realNum +" "+serverIPs[i]); 	
				}
			}
			else if(sentence.equals("2")) // Inform and Update
			{
				
				int sendTo = 0;
				String sendThis = "";
				
				//Hashing
				System.out.println("Send what? (Content name)");
				sendThis = scanner.nextLine();
				sendTo = hash(sendThis); // sendTo is our y value
				
				//Storing the stuff
				int serverID = sendTo + 1; // Grabbing the proper server ID
				//String serverIp = serverIP[sendTo]; // The server IP of server ID
				//Storing what this client has
				for(int i = 0; i < 10; i++) 
				{
					if(contents[i] == null)
					{
						contents[i] = sendThis;
						i = 11;
					}
				}
				

				//Sending instructions
				System.out.println("Sending Instruction "+sentence+" (Inform and Update) to Server "+serverID);
				sender(sentence, socketToServer, InetAddress.getByName(serverIPs[sendTo]), serverPorts[sendTo]);
				
				//Waiting
				System.out.println("Waiting for response . . .");
				String message = waitAndGet(socketToServer); // Turn packet to string
				System.out.println(message);
				
				//Sending content name
				System.out.println("Sending content name . . .");
				sender(sendThis, socketToServer, InetAddress.getByName(serverIPs[sendTo]), serverPorts[sendTo]);
				
				if(listening == false)
				{
					(new P2PServer(40235)).start();
					listening = true;
				}
					
			}
			
			else if (sentence.equals("3"))
			{
				int sendTo = 0;
				String getThis = "";
				
				System.out.println("Get what? (Content Name)");
				getThis = scanner.nextLine();
				sendTo = hash(getThis);
				int serverID = sendTo + 1; // Grabbing the proper server ID
				
				//Sending Instruction
				System.out.println("Sending Instruction "+sentence+" (Query for Content) to Server "+serverID);
				sender(sentence, socketToServer, InetAddress.getByName(serverIPs[sendTo]), serverPorts[sendTo]);
				
				//Waiting for approval
				String message = waitAndGet(socketToServer); // Turn packet to string
				System.out.println(message);
				
				//Sending content name
				System.out.println("Sending content name . . .");
				sender(getThis, socketToServer, InetAddress.getByName(serverIPs[sendTo]), serverPorts[sendTo]);
				
				//Waiting for IP of Client
				message = waitAndGet(socketToServer); // Turn packet to string
				//message = "localHost";
				if(message.equals("404 content not found"))
					System.out.println("Server: "+message);
				else
				{
					System.out.println("Client who has it has the IP Address: "+message);

					// THIS IS WHERE WE START FILE TRANSFER
					// Send FTP request and receive the new TCP
					 System.out.println("Sending request to: "+message);
					 Socket clientSocket = new Socket(InetAddress.getByName("localHost"),40236);
					 System.out.println("Sent");
					 DataInputStream inFromServer =  new DataInputStream(clientSocket.getInputStream());
					 System.out.println("Waiting for new TCP from: " + InetAddress.getByName(message));
					 String serverSentence = inFromServer.readUTF();
					 
					 //Send new request to the new TCP
					 System.out.println("Sending a request to new TCP");
					 int newSocket = Integer.parseInt(serverSentence);
					 Socket clientTransferSocket = new Socket(InetAddress.getByName(message), newSocket);
					 
					 //Send the file name
					 DataInputStream inFromServer2 =  new DataInputStream(clientTransferSocket.getInputStream()); // for the file bytes
					 DataOutputStream outToServer2 = new DataOutputStream(clientTransferSocket.getOutputStream()); // for file name send
					 System.out.println("Sending file name "+getThis+" . . .");
					 outToServer2.writeUTF(getThis);
					 outToServer2.flush();
					 System.out.println("Waiting for response . . . ");
					 
					 //Receive the file
					 FileOutputStream desktop = null;
			         BufferedOutputStream toDesktop = null;
					 ByteArrayOutputStream baos = new ByteArrayOutputStream();
					 //InputStream is = clientSocket.getInputStream(); // waiting
					 desktop = new FileOutputStream(getThis+".jpg");
		             toDesktop = new BufferedOutputStream(desktop);
		             byte[] aByte = new byte[1];
		             System.out.println("Recieved response, writing file . . .");
		             int bytesRead = inFromServer2.read(aByte, 0, aByte.length);
		              do {
	                        baos.write(aByte);
	                        bytesRead = inFromServer2.read(aByte);
	                } while (bytesRead != -1);
		            
		            // File received, close TCP
	                toDesktop.write(baos.toByteArray());
	                toDesktop.flush();
	                toDesktop.close();
	                clientSocket.close();
	                clientTransferSocket.close();
	                System.out.println("File recieved!");
				 }
			}
			else if(sentence.equals("5"))
			{
				String message = "";
				int realNum = 0;
				//Iterate to all the servers, ask them to delete contents related to this IP
				for(int i = 0; i <= 3;i++)
				{
					realNum = i + 1;
					System.out.println("Sending Instruction "+sentence+" (Exit) to Server "+realNum);
					sender(sentence, socketToServer, InetAddress.getByName(serverIPs[i]), serverPorts[i]);
					//Waiting for approval
					message = waitAndGet(socketToServer); // Turn packet to string
					System.out.println(message);
				}
				System.out.println("Goodbye");
				break;
			}
		}
		
		
	}
}
