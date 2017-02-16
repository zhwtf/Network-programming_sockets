/**
 * @author: hao zheng
 login name: hza89
 email: hza89@sfu.ca
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestClient4 {

	/**
	 *
	 */
	public TestClient4() {

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

	     RDT rdt = new RDT(hostname, dst_port, local_port, 1, 1);
	     RDT.setLossRate(0.8);
		 RDT.setProtocol(1);
		 RDT.setMSS(100);
		 RDT.setRTO(500);
	     byte[] buf = new byte[RDT.MSS];
	     byte[] data = new byte[10];
		 for (int i=0; i<10; i++)
			data[i] = 0;
		 rdt.send(data, 10);

		 for (int i=0; i<10; i++)
			data[i] = 1;
		 rdt.send(data, 10);

		 for (int i=0; i<10; i++)
			data[i] = 2;
		 rdt.send(data, 10);

		 for (int i=0; i<10; i++)
			data[i] = 3;
		 rdt.send(data, 10);

		 for (int i=0; i<10; i++)
			data[i] = 4;
		 rdt.send(data, 10);



	     System.out.println(System.currentTimeMillis() + ":Client has sent all data " );
	     System.out.flush();

	     int size = rdt.receive(buf, RDT.MSS);
		 for (int i=0; i<size; i++)
			 System.out.print(buf[i]);
	     rdt.close();

	     System.out.println("Client is done " );
	}

}
