
/**
 * @author mohamed
 *
 */
package rdt;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RDT {

	public static final int MSS = 100; // Max segement size in bytes
	public static final int RTO = 500; // Retransmission Timeout in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	public static final int protocol = GBN;

	public static double lossRate = 0.0;
	public static Random random = new Random();
	public static Timer timer = new Timer();

	private DatagramSocket socket;
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port;

	//why it has two buffers??
	private RDTBuffer sndBuf;
	private RDTBuffer rcvBuf;

	private ReceiverThread rcvThread;


	RDT (String dst_hostname_, int dst_port_, int local_port_)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(MAX_BUF_SIZE);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else
			rcvBuf = new RDTBuffer(MAX_BUF_SIZE);
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}

	RDT (String dst_hostname_, int dst_port_, int local_port_, int sndBufSize, int rcvBufSize)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		 try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(sndBufSize);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else
			rcvBuf = new RDTBuffer(rcvBufSize);

		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}

	public static void setLossRate(double rate) {lossRate = rate;}

	// called by app
	// returns total number of sent bytes
	public int send(byte[] data, int size) {

		//****** complete

		// divide data into segments
		//calculate how many segments we need to send data[]
		int num_s1 = size % MSS;
		int num_s2 = size / MSS;
		if(num_s1 != 0){
			num_s2 = num_s2 + 1;
		}

		int i = 0; // the inital index of the segment
		while(true){
			//int i = 0;
			// put each segment into sndBuf
			while(i < num_s2 && ((sndBuf.next - sndBuf.base) < 3)){
				//create the segment and put it into sndBuf
				RDTSegment sg1;
				sg1.seqNum = i; //set the seqNum
				sg1.ackNum = i; //set the ackNum
				//set the data
				int len = 0;
				for (int j = 0; i*MSS + j < size; j++) {
					sg1.data[j] = data[i*MSS + j];
					len++;
				}
				sg1.length = len; //set the length
				sg1.flags = (i == num_s2-1) ? 0 : 1;
				sg1.rcvWin = (protocol == GBN) ? 1 : MAX_BUF_SIZE; //set the rcvwin
				sg1.checksum = sg1.computeChecksum(); //set the checksum
				//put the segment into sndBuf
				sndBuf.putNext(sg1);
				// send using udp_send()
				Utility.udp_send(sg1, socket, dst_ip, dst_port);

				//create the TimerTask first
				//TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, InetAddress ip_addr, int p)
				sg1.timeoutHandler = new TimeoutHandler(sndBuf, sg1, socket, dst_ip, dst_port);
				// schedule timeout for segment(s)
				timer.schedule(sg1.timeoutHandler, RTO);
				i++;
				//num_s2--;
			}
			if(i == num_s2){
				break;
			}


			// schedule timeout for segment(s)

		}




		return size;
	}


	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive (byte[] buf, int size)
	{
		//*****  complete

		return 0;   // fix
	}

	// called by app
	public void close() {
		// OPTIONAL: close the connection gracefully
		// you can use TCP-style connection termination process
	}

}  // end RDT class


class RDTBuffer {
	public RDTSegment[] buf;
	public int size;
	public int base; // the front of the sliding window
	public int next;
	public Semaphore semMutex; // for mutual execlusion
	public Semaphore semFull; // #of full slots
	public Semaphore semEmpty; // #of Empty slots

	RDTBuffer (int bufSize) {
		buf = new RDTSegment[bufSize];
		for (int i=0; i<bufSize; i++)
			buf[i] = null;
		size = bufSize;
		base = next = 0;
		semMutex = new Semaphore(1, true);
		semFull =  new Semaphore(0, true);
		semEmpty = new Semaphore(bufSize, true);
	}



	// Put a segment in the next available slot in the buffer
	// similar to producer
	//write to the buffer
	public void putNext(RDTSegment seg) {
		try {
			semEmpty.acquire(); // wait for an empty slot
			semMutex.acquire(); // wait for mutex
				buf[next%size] = seg;
				next++;
			semMutex.release();
			semFull.release(); // increase #of full slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}
	}

	// return the next in-order segment
	// similar to consumer
	//read from the buffer
	public RDTSegment getNext() {
		// **** Complete
		try {
			semFULL.acquire(); // decrease the full slots
			semMutex.acquire(); // wait for mutex
				RDTSegment nextSeg = buf[base%size];
				base++;
			semMutex.release();
			semEmpty.release(); // increase #of empty slots
		} catch(InterruptedException e) {
			System.out.println("Buffer getNext(): " + e);
		}


		return nextSeg;  // fix
	}

	// Put a segment in the *right* slot based on seg.seqNum
	// used by receiver in Selective Repeat
	public void putSeqNum (RDTSegment seg) {
		// ***** compelte

	}

	// for debugging
	public void dump() {
		System.out.println("Dumping the receiver buffer ...");
		// Complete, if you want to

	}
} // end RDTBuffer class



class ReceiverThread extends Thread {
	RDTBuffer rcvBuf, sndBuf;
	DatagramSocket socket;
	InetAddress dst_ip;
	int dst_port;

	ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s,
			InetAddress dst_ip_, int dst_port_) {
		rcvBuf = rcv_buf;
		sndBuf = snd_buf;
		socket = s;
		dst_ip = dst_ip_;
		dst_port = dst_port_;
	}
	public void run() {

		// *** complete
		// Essentially:  while(cond==true){  // may loop for ever if you will not implement RDT::close()
		//                socket.receive(pkt)
		//                seg = make a segment from the pkt
		//                verify checksum of seg
		//	              if seg contains ACK, process it potentailly removing segments from sndBuf
		//                if seg contains data, put the data in rcvBuf and do any necessary
		//                             stuff (e.g, send ACK)
		//
		while(true){
			byte[] buf1 = new byte[MSS];
			DatagramPacket pkt = new DatagramPacket(buf1, MSS); //declare need arguments
			socket.receive(pkt);
			RDTSegment seg1 = new RDTSegment();
			makeSegment(seg1, pkt.getData());
			if(seg1.isValid() == true){ // if the segment is valid
				//if seg contains ACK, process it potentailly removing segments from sndBuf
				if(sef1.containsAck == true){

				}
			}
		}
	}


//	 create a segment from received bytes
	void makeSegment(RDTSegment seg, byte[] payload) {

		seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
		seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
		seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
		seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
		seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
		seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);
		//Note: Unlike C/C++, Java does not support explicit use of pointers!
		// we have to make another copy of the data
		// This is not effecient in protocol implementation
		for (int i=0; i< seg.length; i++)
			seg.data[i] = payload[i + RDTSegment.HDR_SIZE];
	}

} // end ReceiverThread class
