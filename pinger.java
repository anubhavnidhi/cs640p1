/*
PING
	
Run client : java pinger -l <local port> -h <remote hostname> -r <remote port> -n <number of packets>
Run server : java pinger -l <local port>
*/

import java.io.*; 
import java.net.*; 
import java.util.*;  
class pinger
{
   // Convert bytearray to int and vice verse and in proper network byte order
	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a)
	{
	    return new byte[] {
	        (byte) ((a >> 24) & 0xFF),
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) (a & 0xFF)
	    };
	}

	public static void main(String[] args) throws Exception
	{
		try
		{
			byte[] num=new byte[4];
			byte[] timest=new byte[12];
			byte[] sentdata = new byte[16]; 
			byte[] recvdata = new byte[16]; 
			Random g = new Random();
		    int seqno=g.nextInt(1000000000); // Sequence number is intialised to random value
			num= intToByteArray(seqno);  // Need to know size of sequence number in packet
			//Decision on whether client or server based on if there is 6th (-n) & 7th argument
			//CLIENT CODE 0=-l 1=port name 2=-h 3=remote hostname 4=-r 5=remote port 6=-n 7=numpack
			if(args.length>6&&args[6].equalsIgnoreCase("-n"))
			{
				if(args.length!=8 || (!args[0].equalsIgnoreCase("-l"))||(!args[2].equalsIgnoreCase("-h"))||(!args[4].equalsIgnoreCase("-r")))
				{
				     System.out.println("Missing, incorrect or extra parameters for pinger client");
					 System.out.println("Usage: java pinger -l <local port>");
					 System.exit(1);
				}
				int unreceived=0;  // keep track of lost packets
				long start=0,end=0;
				long max=0,min=1000000000,avg=0;
				int localport=Integer.parseInt(args[1]);
				String remotehostname =args[3];
				int remoteport=Integer.parseInt(args[5]);
				DatagramSocket clientsocket = new DatagramSocket(localport); 
				DatagramPacket sendpacket=null,recvpacket=null; 
				InetAddress IPAddress = InetAddress.getByName(remotehostname); 
				int num_packets=Integer.parseInt(args[7]);
				long rtt[]=new long[num_packets];  // store the RTTs of each packet in microseconds
				for(int i=0;i<num_packets;i++)
				{
					num= intToByteArray(seqno++);		
					start = System.nanoTime()/1000;  // Start time in microsecond
					String y=Long.toString(start);
					timest=y.getBytes();
					// Put sequence number and timestamp into packet to be sent
					sentdata = new byte[num.length + timest.length];
					System.arraycopy(num, 0, sentdata, 0, num.length);
					System.arraycopy(timest, 0, sentdata, num.length, timest.length);
					sendpacket = new DatagramPacket(sentdata, sentdata.length, IPAddress, remoteport); 
					// sending packet
					clientsocket.send(sendpacket); 
					// receiving packet
					recvpacket = new DatagramPacket(recvdata, recvdata.length); 
					clientsocket.setSoTimeout(10000);
					try 
					{
						clientsocket.receive(recvpacket); 
						num=Arrays.copyOfRange(recvpacket.getData(),0,num.length);	// get sequence number
						timest=Arrays.copyOfRange(recvpacket.getData(),num.length,num.length+timest.length); // get timestamp
						end = System.nanoTime()/1000; // End time in microsecond
						rtt[i] = ((end - Long.valueOf(new String(timest)).longValue())); // find RTT
						System.out.println("size=" + recvpacket.getData().length +" from=" +recvpacket.getAddress()+" seq="+byteArrayToInt(num)+" rtt="+ rtt[i]/(float)1000 +" ms"); 
						// find max, min and sum of rtt's for finding average
						if(rtt[i]>max)
						{
							max=rtt[i];
						}
						if(rtt[i]<min)
						{
							min=rtt[i];
						}
						avg+=rtt[i];
					}
					catch (SocketTimeoutException sockerr)
					{
						System.out.println ("Timeout for packet");
						unreceived++;
					}
				}
				// Print Output
				System.out.print("sent="+num_packets+" received="+ (num_packets-unreceived)+" lost="+(unreceived/(float)num_packets)+"%");
				System.out.print(" rtt min/avg/max="+min/(float)1000+"/"+(((float)avg)/num_packets)/(float)1000+max+"/"+max/(float)1000+" ms");
				System.out.println();
				clientsocket.close(); 
			}
			else
			{
				if(args.length!=2|| (!args[0].equalsIgnoreCase("-l")))
				{
				     System.out.println("Missing, incorrect or extra parameters for pinger server");
					 System.out.println("Usage: java pinger -l <local port>");
					 System.out.println("IF YOU WANT TO RUN CLIENT: java pinger -l <local port> -h <remote hostname> -r <remote port> -n <number of packets>");		 
					 System.exit(1);
				}
				long time=0;
				int localport=Integer.parseInt(args[1]);
				DatagramSocket serversocket = new DatagramSocket(localport); 
				while(true) 
				{ 
					DatagramPacket recvpacket = new DatagramPacket(recvdata, recvdata.length); 
					// receive packet
					serversocket.receive(recvpacket); 
					time=System.currentTimeMillis(); // time in millisecond
					num=Arrays.copyOfRange(recvpacket.getData(),0,num.length); // get sequence number
					System.out.println ("time="+ time +" from=" + recvpacket.getAddress() + " seq=" + byteArrayToInt(num));
					// send packet
					sentdata = recvpacket.getData(); 
					DatagramPacket sendpacket = new DatagramPacket(sentdata, sentdata.length, recvpacket.getAddress(), recvpacket.getPort()); 
					serversocket.send(sendpacket); 
				}
			}
		}
		catch(IllegalArgumentException il)
		{
			System.out.println("Illegal argument exception ");
			System.out.println("CLIENT:Usage: java pinger -l <local port> -h <remote hostname> -r <remote port> -n <number of packets>");
			System.out.println("SERVER:Usage: java pinger -l <local port> -h <remote hostname> -r <remote port>");
		}
	}
} 
