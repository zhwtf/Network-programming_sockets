/**
 * @author mhefeeda
 *
 */

package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	RDTBuffer sndBuf;
	RDTSegment seg;
	DatagramSocket socket;
	InetAddress ip;
	int port;

	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock,
			InetAddress ip_addr, int p) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		ip = ip_addr;
		port = p;
	}

	public void run() {

		System.out.println(System.currentTimeMillis()+ ":Timeout for seg: " + seg.seqNum);
		System.out.flush();

		// complete
		switch(RDT.protocol){
			case RDT.GBN:
				//resend segments in sndBuf from base to next-1
				//and start a new TimerTask
				//start the TimerTask for the seg
				seg.timeoutHandler = new TimeoutHandler(sndBuf, seg, socket, ip, port);
				RDT.timer.schedule(seg.timeoutHandler, RDT.RTO);
				int j = sndBuf.next;
				for (int i = sndBuf.base; i < j; i++) {
					Utility.udp_send(sndBuf.getNext(), socket, ip, port);
				}
				break;
			case RDT.SR:

				break;
			default:
				System.out.println("Error in TimeoutHandler:run(): unknown protocol");
		}

	}
} // end TimeoutHandler class
