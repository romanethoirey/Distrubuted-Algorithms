package com.lightbend.akka.sample;

import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import akka.event.Logging;
import akka.event.LoggingAdapter;


import java.util.ArrayList;


//#greeter-messages
public class Process extends UntypedAbstractActor {

	private LoggingAdapter log;

	static public Props props(int id) {
		return Props.create(Process.class, () -> new Process(id));
	}




	//#system-members
	static public class Members {
		//public final int num;
		public final ArrayList<ActorRef> members;

		public Members(ArrayList<ActorRef> members) {
			//    this.num = members.size();
			this.members = members;
		}
	}




	/*
	State,
	1 - Correct
	2 - Crashed
	3 - Wait_Read
	4 - Wait_Write
	5 - Correct_wait
	*/
	static public class State {
		public final String state;

		public State(String state) {
			this.state = state;
		}
	}

	// local value with (seqnum, process id) timestamp
	static public class StampedValue {
		public int value;
		public int seqnum;
		public int pid;

		public StampedValue(int value, int seqnum, int pid) {
			this.value = value;
			this.seqnum = seqnum;
			this.pid = pid;
		}
	}

	// the "write-request" message type with a timestamped value and the local seq number
	static public class WriteRequest{
		public StampedValue val;
		public int localseqnum;

		public WriteRequest(StampedValue val, int localseqnum) {
			this.val = val;
			this.localseqnum = localseqnum;
		}
	}

	// the "write-response" message type with a local seq number
	static public class WriteResponse{
		public int localseqnum;

		public WriteResponse(int localseqnum) {
			this.localseqnum = localseqnum;
		}
	}


	// the "read-request" message type with the local seq number
	static public class ReadRequest{
		public int localseqnum;

		public ReadRequest(int localseqnum) {
			this.localseqnum = localseqnum;
		}
	}

	// the "read-response" message type with a timestamped value and the local seq number
	static public class ReadResponse{
		public StampedValue val;
		public int localseqnum;

		public ReadResponse(StampedValue val,int localseqnum) {
			this.val = val;
			this.localseqnum = localseqnum;
		}
	}


	// process identifier
	private final int id;

	// system members known to the process
	private  Members mem;

	// process state
	private String state;

	/* currently executed operation:
	0 - No operation is active,
	1 - get,
	2- put
	*/
	private int activeop;

	// local copy of the register value
	private StampedValue val;

	// local sequence number: the number of operations performed so far
	private int localseqnum;

	// local message buffer (to store messages received in a write or read phase)
	private ArrayList msgs = new ArrayList();

	// number of operations to perform
	private final int M=10;

	//number of operations performed so far
	private int ops=1;

	// counter of put operation
	private int putCounter = 0;

	// counter of get operation
	private int getCounter = 0;

	public Process(int id) {
		this.state = "";
		this.val = new StampedValue(0,0,0);
		this.id = id;
		this.activeop = 0;
		this.localseqnum = 0;
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tF %1$tT] [%4$-7s] %5$s %n");
		log = Logging.getLogger(getContext().getSystem(), this);
	}

	public void putOperation() {// the process is active
		// start a put operation
		this.localseqnum++;
		log.info("P"+this.id+": uses put operation "+this.localseqnum+" with value "+this.id);
		this.activeop = 2; // active operation is put
		this.msgs.clear();
		for(int i = 0; i < this.mem.members.size(); i++) { // get the current value/timestamp
			this.mem.members.get(i).tell(new ReadRequest(localseqnum),getSelf());
		}
		this.state="WAIT_READ"; // Enter the waiting for read state

	}

	public void getOperation() {// the process is active
		// start a get operation
		this.localseqnum++;
		log.info("P"+this.id+": uses get operation "+this.localseqnum+" with value "+this.id);
		this.activeop = 3; // active operation is get
		this.msgs.clear();
		for(int i = 0; i < this.mem.members.size(); i++) { // get the current value/timestamp
			this.mem.members.get(i).tell(new ReadRequest(localseqnum),getSelf());
		}
		this.state = "WAIT_READ"; // Enter the waiting for read state

	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (this.state != "CRASHED") { // If not faulty
			ActorRef actorRef = getSender();
			if (msg instanceof Members) {
				this.mem = (Members) msg; // determine the system members
			}
			else
			if (msg instanceof State) { // failure indication
				this.state = ((State) msg).state;
				if (this.state=="CORRECT") {//active
					putOperation();
				}
				else if (this.state=="CORRECT_WAIT") {//active and put already done
					getOperation();
				}
				else { // the process is faulty
					log.info("P"+this.id+" is faulty");
				}
			}
			else
			if (msg instanceof ReadRequest) {  // upon receive, send to read response his own timestamp
				log.info("P"+this.id+": received a read request "+this.localseqnum+" from "+actorRef);
				actorRef.tell(new ReadResponse(this.val,((ReadRequest)msg).localseqnum),getSelf());
			}

			// stampedValue = timestamps + v
			// r = localseqnum
			else
			if (msg instanceof WriteRequest) {
				if (((WriteRequest)msg).val.seqnum>this.val.seqnum ||
						((WriteRequest)msg).val.seqnum==this.val.seqnum &&
								((WriteRequest)msg).val.pid>this.val.pid) {   // highest value
					this.val = ((WriteRequest)msg).val;
					log.info("P"+this.id+": updated the local value with value ("+this.val.value+","+this.val.seqnum+","+this.val.pid+")");
				}
				actorRef.tell(new WriteResponse(((WriteRequest)msg).localseqnum),getSelf());
			}
			else
			if (msg instanceof ReadResponse && ((ReadResponse)msg).localseqnum==this.localseqnum
					&& this.state=="WAIT_READ") {/// we look if P1 value is equal to P0 value
				// the expected response is received
				log.info("P"+this.id+": received a read response "+this.localseqnum+" from "+actorRef);
				msgs.add(msg); // this.val + this.localsequnum
				// If "enough" responses received:  change the state and invoke a new request
				if (msgs.size()>=this.mem.members.size()/2+1) { // we look if the majority of messages (N/2)+1 has been reached

					log.info("P"+this.id+": received a quorum of read responses "+this.localseqnum);
					this.state="CORRECT"; // not waiting any longer
					for (int i = 0; i<msgs.size(); i++) { // taking the highest value
						if (((ReadResponse)msgs.get(i)).val.seqnum>this.val.seqnum ||
								((ReadResponse)msgs.get(i)).val.seqnum==this.val.seqnum &&
										((ReadResponse)msgs.get(i)).val.pid>this.val.pid) {
							this.val = ((ReadResponse)msgs.get(i)).val;
						}
					}
					log.info("P"+this.id+": new timestamp = ("+this.val.seqnum+","+this.val.pid+")");


					if (this.activeop==2) { // this is a put operation
						this.state="WAIT_WRITE"; //the waiting state
						this.msgs.clear();

						for(int i = 0; i < this.mem.members.size(); i++) {
							// write a new value (process identifier) with the incremented seq number

							this.mem.members.get(i).tell(new WriteRequest(new StampedValue(this.id,this.val.seqnum+1,this.id),this.localseqnum),getSelf());
						}

					}
					if (this.activeop==3) { // this is a get operation
						this.state="WAIT_WRITE"; //the waiting state
						this.msgs.clear();

						for(int i = 0; i < this.mem.members.size(); i++) {
							// write a new value (process identifier) with the incremented seq number

							this.mem.members.get(i).tell(new WriteRequest(new StampedValue(this.id,this.val.seqnum,this.id),this.localseqnum),getSelf());
						}

					}

				}
			}

			else
			if (msg instanceof WriteResponse && ((WriteResponse)msg).localseqnum==this.localseqnum
					&& this.state=="WAIT_WRITE") {
				// the expected write response is received
				log.info("P"+this.id+": received a write response "+this.localseqnum+" from "+actorRef);
				msgs.add(msg);
				// If "enough" responses received:  change the state and complete the put operation
				if (msgs.size()>=this.mem.members.size()/2+1) {// we look if the majority of messages (N/2)+1 has been reached
					if (this.activeop == 2) {
						log.info("P"+this.id+": received a quorum of write responses "+this.localseqnum);
						this.state="CORRECT";	// not waiting any longer, go to get statement
						log.info("P"+this.id+": completes put operation "+this.localseqnum);
						if(putCounter < M-1) {
							putCounter++;//increment the counter
							this.activeop = 0;
							getSelf().tell(new State("CORRECT"),  ActorRef.noSender());
						}
						else{
							this.activeop = 0;
							getSelf().tell(new State("CORRECT_WAIT"),  ActorRef.noSender());

						}
						// Finished a put operation
					}
					if (this.activeop == 3) {
						log.info("P"+this.id+": received a quorum of write responses "+this.localseqnum);
						this.state="CORRECT";	// not waiting any longer
						log.info("P"+this.id+": completes get operation " +this.localseqnum+ ", the value of this process is : "+this.val.value);
						this.activeop = 0;
						this.state = "";
						if(getCounter < M-1) {
							getCounter++;//increment the counter
							this.activeop = 0;
							getSelf().tell(new State("CORRECT_WAIT"),  ActorRef.noSender());
						}
						else{
							this.activeop = 0;
							this.state = "";

						}
					}
				}

			}



			else
				unhandled(msg);
		}
	}



}

