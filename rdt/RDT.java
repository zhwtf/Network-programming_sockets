
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
import java.util.concurrent.*;

public class RDT {


	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat


	public static double lossRate = 0.0;
	public static Random random = new Random();
	public static Timer timer = new Timer();
	public static int MSS = 100; // Max segement size in bytes
	public static int RTO = 500; // Retransmission Timeout in msec
	public static  int protocol = SR;

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
	public static void setMSS(int mss) {MSS = mss;}
	public static void setProtocol(int protocol_) {protocol = protocol_;}
	public static void setRTO(int rto) {RTO = rto;}



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
		System.out.println("need to send " + num_s2 + " segments");
		int i = 0; // the inital index of the segment
		System.out.println("one send");
		while(i != num_s2){
			//int i = 0;
			// put each segment into sndBuf
			//System.out.println("segment out... ");
			//((sndBuf.next - sndBuf.base) < MAX_BUF_SIZE)
			while(i < num_s2){
				System.out.println("segment in...");
				//create the segment and put it into sndBuf
				RDTSegment sg1 = new RDTSegment();
				sg1.seqNum = sndBuf.next; //set the seqNum
				sg1.ackNum = sndBuf.next; //set the ackNum
				//set the data
				int len = 0;
				for (int j = 0; j < MSS && i*MSS + j < size; j++) {
					sg1.data[j] = data[i*MSS + j];
					len++;
				}
				System.out.println("the segment length is: " + len);
				sg1.length = len; //set the length
				sg1.flags = (i == num_s2-1) ? 0 : 1;
				sg1.rcvWin = (protocol == GBN) ? 1 : MAX_BUF_SIZE; //set the rcvwin
				//need to get the 1's complement of the computeChecksum()
				sg1.checksum = sg1.computeChecksum() ^ 0xff; //set the checksum
				//put the segment into sndBuf
				System.out.println("the segment " + sg1.seqNum + " is created successfully");

				sndBuf.putNext(sg1); //sndBuf's next increased by 1
				System.out.println("the segment " + sg1.seqNum + " is put into the sndBuf successfully");
				// send using udp_send()
				Utility.udp_send(sg1, socket, dst_ip, dst_port);

				// schedule timeout for segment(s)
				//start the timer only when base == 0
				i = i + 1;
				if(sg1.seqNum == 0 && (protocol == GBN)){//if the protocol is GBN
					//create the TimerTask first
					//TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, InetAddress ip_addr, int p)
					sg1.timeoutHandler = new TimeoutHandler(sndBuf, sg1, socket, dst_ip, dst_port);
					//schedule timeout for the segment at base position in the sndBuf
					timer.schedule(sg1.timeoutHandler, RTO);
				}
				if(protocol == SR){ //then need to schedule a TimerTask for every segment
					sg1.timeoutHandler = new TimeoutHandler(sndBuf, sg1, socket, dst_ip, dst_port);

					timer.schedule(sg1.timeoutHandler, RTO);
				}

			}
		}
		return size;
	}


	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive (byte[] buf, int size)
	{
		int len2 = 0;
		//*****  complete
		if(protocol == GBN){
			System.out.println("Receive For go back N protocol!!");
			RDTSegment rseg = rcvBuf.getNext();
			len2 = rseg.length;
			for (int i = 0; i < len2; i++) {
				buf[i] = rseg.data[i];

			}


		}
		else{ //protocol is SR
			//need to deliver from rcv_base to previously buffered and consecutively numbered (beginning with rcv_base) packets
			//System.out.println("Receive --- SR");

			//while(rcvBuf.base == 0 || rcvBuf.buf[rcvBuf.base % rcvBuf.size] != null)
				if(rcvBuf.base == 0 && rcvBuf.buf[rcvBuf.base % rcvBuf.size] != null){ //at start, the base is 0 and has segment
					RDTSegment rseg = rcvBuf.getNext();
					len2 = rseg.length;
					for (int i = 0; i < len2; i++) {
						buf[i] = rseg.data[i];

					}
					return len2;
				}
				//need to keep track of the base and the seqNum of the base(should be updated, not the previous segment)
				if(rcvBuf.buf[rcvBuf.base % rcvBuf.size] != null && rcvBuf.buf[rcvBuf.base % rcvBuf.size].seqNum == rcvBuf.base){
					RDTSegment rseg = rcvBuf.getNext();
					len2 = rseg.length;
					for (int i = 0; i < len2; i++) {
						buf[i] = rseg.data[i];

					}
					return len2;
				}

		}
		return len2;
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
		RDTSegment nextSeg = new RDTSegment();
		try {
			//RDTSegment nextSeg = new RDTSegment();
			semFull.acquire(); // decrease the full slots
			semMutex.acquire(); // wait for mutex
				nextSeg = buf[base%size];
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
		try {
			semEmpty.acquire(); // wait for an empty slot
			semMutex.acquire(); // wait for mutex
				buf[seg.seqNum%size] = seg;
				next++;
			semMutex.release();
			semFull.release(); // increase #of full slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}
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
		// Essentially:  while(cond==true)  // may loop for ever if you will not implement RDT::close()
		//                socket.receive(pkt)
		//                seg = make a segment from the pkt
		//                verify checksum of seg
		//	              if seg contains ACK, process it potentailly removing segments from sndBuf
		//                if seg contains data, put the data in rcvBuf and do any necessary
		//                             stuff (e.g, send ACK)
		//
		//System.out.print("can not  get in?!\n");
		while(socket != null){
			System.out.print("ReceiverThread is Waiting fot incoming segments!!!!\n");
			//need to add the header size to the buf1
			byte[] buf1 = new byte[RDT.MSS + RDTSegment.HDR_SIZE];
			DatagramPacket pkt = new DatagramPacket(buf1, RDT.MSS + RDTSegment.HDR_SIZE); //declare need arguments
			//receive the packet
			try {
				// have problems here??????? Exception....
				//DatagramPacket pkt = new DatagramPacket(buf1, RDT.MSS); //declare need arguments
				socket.receive(pkt);
				System.out.print("packet received!!\n");
				//RDTSegment seg1 = new RDTSegment();
				//makeSegment(seg1, pkt.getData());

			} catch (IOException e)   {   e.printStackTrace();    }

			RDTSegment seg1 = new RDTSegment();
			makeSegment(seg1, pkt.getData());
			//System.out.println("seg1 is not valid??!!!!");
			//seg1.isValid() == true
			if(seg1.isValid() == true){ // if the segment is valid
				//if seg contains ACK, process it potentailly removing segments from sndBuf
				System.out.println("valid segment number: " + seg1.seqNum);
				System.out.println("length of seg: " + seg1.length);

				if(RDT.protocol == RDT.GBN){
					if(seg1.containsAck() == true){ //not duplicate ack
						System.out.print("this is ACK segment!\n");
						//for go back N protocol
						System.out.println("for go back N protocol!");
						//if the ackNum is equal to the sndBuf base and seqNum, meaning the order is right
						//then stop the timer using timeoutHandler.cancel()
						//what if the segment timeout first then the ack arrives
						RDTSegment baseseg = sndBuf.buf[sndBuf.base % sndBuf.size];
						if (seg1.ackNum == baseseg.ackNum && seg1.seqNum == baseseg.seqNum){
							//getNext used to get the in order segment and cancel its TimerTask
							//RDTSegment seg2 = sndBuf.getNext();
							//seg2.timeoutHandler.cancel();
							if (sndBuf.getNext().timeoutHandler.cancel() == true){//meaning the timeoutHandler hasn't started
								// if sndBuf.base == sndBuf.next meaning no segment in the buffer
								//sliding window, base increased by 1 when called sndBuf.getNext()
								//if the next in-order segment exists, then prepare its timeoutHandler and start it
								System.out.println("Received the expected ACK: " + seg1.ackNum);
								System.out.println("cancel the timeoutHandler for segment " + seg1.ackNum + " before it starts!!!!");
								System.out.println("the sndBuf base is: " + sndBuf.base);
								System.out.println("the sndBuf next is: " + sndBuf.next);
								if (sndBuf.base != sndBuf.next) { //there are still unacked segment in the sndBuf
									//TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, InetAddress ip_addr, int p)
									System.out.println("Start a new TimerTask for next in-order segment!");
									sndBuf.buf[sndBuf.base % sndBuf.size].timeoutHandler = new TimeoutHandler(sndBuf, sndBuf.buf[sndBuf.base % sndBuf.size], socket, dst_ip, dst_port);
									//schedule timeout for the segment at base position in the sndBuf

									RDT.timer.schedule(sndBuf.buf[sndBuf.base % sndBuf.size].timeoutHandler, RDT.RTO);
									System.out.println("schedule the new timeoutHandler successfully!!!!!");
								}
								else{
									System.out.println("there are no more unacked segment in the sender buffer!");

								}
							}
						}


						//if the ack is not equal to the seg at base postion, meaning lost occurs
						else {
							//if the ack seqNum > base, meaning the order is not right, the server discarded the segment

							System.out.println("The ACK's order is not right!!! So ignore it!!!");
							//if the ack seqNum <
						}

					}
					//if sender has buffer size 3, server has buffer size 1, and both segment and ACK can be lost


					//if seg contains data, put the data in rcvBuf and do any necessary  stuff (e.g, send ACK)
					//expectedsequent = base?? the reveiver
					if(seg1.containsData() == true){
						System.out.println("this is a data segment!!!!");
						//for go back N protocol
						//first need to check if it's the expectedsequent
						//the expectedsequent = next for revbuff(size = 1)
						if (seg1.seqNum == rcvBuf.next){ //the order is right
							//put the data in rcvBuf
							System.out.println("the order is right!!!! put into buffer");
							rcvBuf.putNext(seg1); //next incresed by 1
							//increase base by 1
							//rcvBuf.base = rcvBuf.base + 1;
							//create an ACK and send ACK to Client
							RDTSegment ackseg = new RDTSegment();
							ackseg.ackNum = seg1.ackNum;
							ackseg.seqNum = seg1.seqNum;
							ackseg.length = 0;
							ackseg.checksum = ackseg.computeChecksum() ^ 0xff;
							Utility.udp_send(ackseg, socket, dst_ip, dst_port); //send the ACK to Client
							//System.out.println("ACK sent to client successfully!!!");
						}
						else{//seg1.seqNum != rcvBuf.next the order is not right
							//meaning there is an ack lost occured
							//for go back N protocol
							//return an ACK with the next-1 ackNum and discard the received segment
							//create an ACK and send ACK to Client
							RDTSegment ackseg = new RDTSegment();
							//ackseg.ackNum = rcvBuf.next - 1;
							ackseg.ackNum = seg1.ackNum;
							ackseg.seqNum = seg1.seqNum;
							//ackseg.ackReceived = true;
							ackseg.length = 0;
							ackseg.checksum = ackseg.computeChecksum() ^ 0xff;

							Utility.udp_send(ackseg, socket, dst_ip, dst_port); //send the ACK to Client
						}

					}
				}





				//the protocol is selective repeat
				else{
					//if the segment received is an ACK
					//means the sender is handling the ACK
					if(seg1.containsAck() == true){
						System.out.print("this is ACK segment!!!!\n");

						System.out.println("for selective repeat protocol!!!!!");
						//if the ACK_NUM equals to the sndBuf base, then move the window
						//increase the base by one, and cancel the timeoutHandler of this segment
						RDTSegment baseseg = sndBuf.buf[sndBuf.base % sndBuf.size];
						if (seg1.ackNum == baseseg.ackNum){//received the right ordered ack
							if (sndBuf.getNext().timeoutHandler.cancel() == true){
								//sliding window, base increased by 1 when called sndBuf.getNext()
								System.out.println("Received the in-order ACK: " + baseseg.ackNum);
								System.out.println("canceled the timeoutHandler for segment " + seg1.ackNum + " before it starts!!!!");
								System.out.println("the sndBuf base is: " + sndBuf.base);
								System.out.println("the sndBuf next is: " + sndBuf.next);
								//need to move the window to the unacked segment with the smallest seqNum
								while(sndBuf.base < sndBuf.next && sndBuf.buf[sndBuf.base % sndBuf.size].ackReceived == true){
									//slide the window---increase base by 1 and create an empty slot in the buffer
									sndBuf.getNext();

								}

							}
							else{ //timeoutHandler has started, ignore the ACK
								System.out.println("TimeoutHandler has started......Ignore the ACK");
							}

						}
						else{//received other ACK ---not in-order
							System.out.println("The ack order is not right, but need to mark it");
							int mark = seg1.ackNum;
							//if the segment timeout is not occured, then cancel the timeoutHandler
							if(sndBuf.buf[mark % sndBuf.size].timeoutHandler.cancel() == true){
								System.out.println("timeoutHandler canceled for segment " + mark);
								//set the segment as received segment
								sndBuf.buf[mark % sndBuf.size].ackReceived = true;
							}
							else{//the timeoutHandler has occured, then just ignore the ack
								System.out.println("timeoutHandler started for segment: " + mark);
								System.out.println("Just ignore this ACK: " + mark);
							}

						}

					} // end of the ACK received


				//if the seg contains data, put the data in rcvBuf and do any necessary  stuff (e.g, send ACK)

				if(seg1.containsData() == true){
					System.out.println("this is a data segment!!!!");

					//and need to avoid same semgent put into recerver buffer twice
					//this would cause deadlock
					if ((seg1.seqNum == rcvBuf.base) && (rcvBuf.buf[rcvBuf.base%rcvBuf.size] == null || (rcvBuf.buf[rcvBuf.base%rcvBuf.size].seqNum != seg1.seqNum))){ //the order is right
						System.out.println("the order is right!!!! put into buffer and slide the window");
						rcvBuf.putSeqNum(seg1);
						//then this packet,
						//and any previously buffered and consecutively numbered (beginning with
						//rcv_base) packets are delivered to the upper layer. The receive window is
						//then moved forward by the number of packets delivered to the upper layer.
						//base++; //increase the base by 1, sliding the window??? need to increase base??
						//then send ack to the sender
						System.out.println("sending the ack: " + seg1.seqNum);
						RDTSegment ackseg = new RDTSegment();
						ackseg.ackNum = seg1.ackNum;
						ackseg.seqNum = seg1.seqNum;
						ackseg.checksum = ackseg.computeChecksum() ^ 0xff;
						ackseg.length = 0;
						Utility.udp_send(ackseg, socket, dst_ip, dst_port); //send the ACK to Client
						//System.out.println("ACK sent to client successfully!!!");
					}
					else{// the order is not right
						System.out.println("The order is not the same as base");
						//need to avoid the same segment put into the buffer multiple times, which would cause deadlock---(overlay)
						if(((seg1.seqNum > rcvBuf.base) && (seg1.seqNum <= (rcvBuf.base+rcvBuf.size-1))) && (rcvBuf.buf[seg1.seqNum%rcvBuf.size] == null || (rcvBuf.buf[seg1.seqNum%rcvBuf.size].seqNum != seg1.seqNum))){
							rcvBuf.putSeqNum(seg1); //put into the revbuff
							System.out.println("segment is put into rcvbuffer: " + seg1.seqNum);

						}
						//else if the seqNum is in [rcv_base-N, rcv_base-1] also need to send ack
						System.out.println("sending the ack: " + seg1.seqNum);
						RDTSegment ackseg = new RDTSegment();
						ackseg.ackNum = seg1.ackNum;
						ackseg.seqNum = seg1.seqNum;
						ackseg.checksum = ackseg.computeChecksum() ^ 0xff;
						ackseg.length = 0;
						Utility.udp_send(ackseg, socket, dst_ip, dst_port);

					}
					System.out.println("the rcvBuf base is: " + rcvBuf.base);
					System.out.println("the rcvBuf next is: " + rcvBuf.next);

				}//end of contain data

			} //end of SR protocol

		} //end of valid
	} //end of while
}//end of run
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
