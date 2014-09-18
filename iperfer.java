/*
IPERF

Run client : java iperfer -c -h <server hostname> -p <server port> -t <time>
Run server : java iperfer -s -p <listen port>
*/

import java.io.*; 
import java.net.*; 
class iperfer
{
	static int x=0;  // to keep count of number of packets sent
	public static void main(String[] args) throws Exception
	{
	    // Decision on whether client or server based on presence of argument -c or -s
		try
		{
		if(args[0].equalsIgnoreCase("-c")) // CLIENT CODE parameters 0=-c 1=-h 2=server hostname 3=-p 4=server port 5=-t 6=time
		{
				if(args.length!=7||(!args[1].equalsIgnoreCase("-h"))||(!args[3].equalsIgnoreCase("-p"))||(!args[5].equalsIgnoreCase("-t")))
				{
				     System.out.println("Missing or extra parameters for iperfer client");
					 System.out.println("Usage: java iperfer -c -h <server hostname> -p <server port> -t <time>");
					 System.exit(1);
				}
				long time=Long.valueOf(args[6]).longValue();
				final String serverhostname = args[2];                  // set to final because running in a thread, to use it in Runnable class
				final int serverport=Integer.parseInt(args[4]);
				final Socket clientsocket = new Socket();
				final SocketAddress sockaddr = new InetSocketAddress(serverhostname, serverport);
				long start=0,end=0;
				clientsocket.connect(sockaddr);
					// Using thread so that sending packets stop when time-out occurs
				start=System.currentTimeMillis();
				Thread t1 = new Thread(new Runnable() {
				public void run() 
				{
					try
					{
						byte[] packet=new byte[1024];   // packets size is 1KB
						while (true) 
						{					
							try 
							{
								//packet=("0").getBytes();  // set packet data to 0
								OutputStream out = clientsocket.getOutputStream();
								out.write(packet); // send packet
								x++; // keep track of packets sent
							} 
							catch (Exception e1) 
							{
								clientsocket.close();
							}
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				});
				t1.start();
				t1.join(time);  // run thread for specified amount of time
				if(t1.isAlive()) t1.stop();
				end=System.currentTimeMillis();
				long diff=end-start;
				System.out.println("sent="+x+" kb rate="+((float)x)/diff+" mbps");   //print output as specified
		}
		else if(args[0].equalsIgnoreCase("-s"))  // SERVER CODE Parameters 0=-s 1=-p 2=listen port
		{
				if(args.length!=3||(!args[1].equalsIgnoreCase("-p")))
				{
				     System.out.println("Missing or extra parameters for iperfer server");
					 System.out.println("Usage: java iperfer -s -p <listen port>");
					 System.exit(1);
				}
				int listenport = Integer.parseInt(args[2]);   // listen port
				ServerSocket serversocket = new ServerSocket(listenport);
				Socket clientsocket = serversocket.accept();
				byte[] packet=new byte[1024]; // packets size is 1KB
				long start=0,end=0,time=0;
				try
				{
					start=System.currentTimeMillis();	// start timer for reading packets
					while(true) 
					{ 
						InputStream in = clientsocket.getInputStream();
						in.read(packet);   //read packet
						x++; // keep track of packets received
					}
				}
				catch(Exception e)
				{
					end=System.currentTimeMillis();  // stop timer
					time=end-start; // rate in milliseconds i.e 1 KB per ms
					System.out.println("received="+x+" kb rate="+((float)x)/time+" mbps"); //print output as specified
					clientsocket.close();
				}
		}
		else
		{
			System.out.println("Invalid: Command is neither for server nor client");
			System.out.println("CLIENT:Usage: java iperfer -c -h <server hostname> -p <server port> -t <time>");
			System.out.println("SERVER:Usage: java iperfer -s -p <listen port>");
		}
		}
		catch(IllegalArgumentException il)
		{
			System.out.println("Illegal argument exception ");
			System.out.println("CLIENT:Usage: java iperfer -c -h <server hostname> -p <server port> -t <time>");
			System.out.println("SERVER:Usage: java iperfer -s -p <listen port>");
		}
	}
} 
