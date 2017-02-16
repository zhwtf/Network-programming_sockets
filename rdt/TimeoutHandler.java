/**
 * @author: hao zheng
 login name: hza89
 email: hza89@sfu.ca
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
				//and start a new TimerTask fot the segment

				seg.timeoutHandler = new TimeoutHandler(sndBuf, seg, socket, ip, port);
				RDT.timer.schedule(seg.timeoutHandler, RDT.RTO);
				int j = sndBuf.next;
				for (int i = sndBuf.base; i < j; i++) {
					System.out.println("resending segment" + sndBuf.buf[i%sndBuf.size].seqNum);
					Utility.udp_send(sndBuf.buf[i%sndBuf.size], socket, ip, port);

				}
				break;
			case RDT.SR:
				//resend only the timeout segment
				//and start a new timeoutHandler for the segment
				seg.timeoutHandler = new TimeoutHandler(sndBuf, seg, socket, ip, port);
				RDT.timer.schedule(seg.timeoutHandler, RDT.RTO);
				System.out.println("resending the timeout segment" + seg.seqNum);
				Utility.udp_send(seg, socket, ip, port);
				break;
			default:
				System.out.println("Error in TimeoutHandler:run(): unknown protocol");
		}

	}
} // end TimeoutHandler class
