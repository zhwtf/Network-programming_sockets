/**
 * @author: hao zheng
hza89@sfu.ca
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestClient {

	/**
	 *
	 */
	public TestClient() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 if (args.length != 3) {
	         System.out.println("Required arguments: dst_hostname dst_port local_port");
	         return;
	      }
		 String hostname = args[0];
	     int dst_port = Integer.parseInt(args[1]);
	     int local_port = Integer.parseInt(args[2]);

	     RDT rdt = new RDT(hostname, dst_port, local_port, 6, 6);
	     RDT.setLossRate(0.4);

	     byte[] buf = new byte[RDT.MSS];
	     byte[] data = new byte[45];
		 for (int i=0; i<45; i++)
			data[i] = 0;
		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
			data[i] = 1;
		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
			data[i] = 2;
		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
			data[i] = 3;
		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
			data[i] = 4;
		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
 		   data[i] = 5;
 		 rdt.send(data, 45);

		 for (int i=0; i<45; i++)
			data[i] = 6;
		 rdt.send(data, 45);

		for (int i=0; i<45; i++)
			data[i] = 7;
		rdt.send(data, 45);

		for (int i=0; i<45; i++)
			data[i] = 8;
		rdt.send(data, 45);

		for (int i=0; i<45; i++)
			data[i] = 9;
		rdt.send(data, 45);

	     System.out.println(System.currentTimeMillis() + ":Client has sent all data " );
	     System.out.flush();

	     int size = rdt.receive(buf, RDT.MSS);
		 for (int i=0; i<size; i++)
			 System.out.print(buf[i]);
	     rdt.close();
		 
	     System.out.println("Client is done " );
	}

}
