package demo;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.actor.ActorRef;


public class Process extends UntypedAbstractActor{

    private static int kREQ=0; //counter of the request
    private int kPut=1; //counter of the put step when kPut=M_OPERATIONS the program stops
    private int id; //Identity of the process
    private int quorum=0; //counter for the Quorum
    private boolean active= true; //boolean if active=false that means process crashed
    //private int key=1; //The key of the stored value in our case there is only one value so only one key
    private int val=0; //Value stored by the process

    //The couple (seq,iMAX) is the timestamp
    private int seq=0; //Sequence associate to the value
    private int seqMAX=0; //max seq collected from the ACK
    private int iMAX=1; //identity of the last writer of the value
    private int valMAX=0; //val associate to the max timestamp collected during collect phase


    // Logger attached to actor
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Process(int id) {
        this.id = id;
    }

    // Static function creating actor
    public static Props createActor(int id) {
        return Props.create(Process.class, () -> {
            return new Process(id);
        });
    }

    //The put function take a key and a value as argument and return OK when operation finish
    public void put(int k, int v) {
        kREQ++;//we ask for a new REQ identifier
        int numREQ = kREQ;
        //Display the invocation of the put REQ
        log.info("[" + getSelf().path().name() + "] [PutREQ#" + numREQ + "]" + " [" + v + "]");

        //send to all process the put REQ
        for (ActorRef a : Implementation.list.getList()) {
            a.tell(new PutREQ(numREQ, k, v), getSelf());
        }

    }

    //Get function takes only a key as argument and return the value
    public void get(int k) {
        kREQ++;//we ask for a new REQ identifier
        int numREQ=kREQ;
        //Display the invocation of the put REQ
        log.info("["+getSelf().path().name()+"] [GetREQ#"+numREQ+"]");

        //send to all process the put REQ
        for(ActorRef a:Implementation.list.getList()) {
            a.tell(new GetREQ(numREQ,k), getSelf());
        }
    }

    //function to make a process crash
    public void setCrash() {
        active=false;
    }

    public void nextPut() {

        if(kPut<Implementation.M_OPERATIONS) {
            kPut=kPut+1;
            put(1,this.id*kPut);
        }

    }
    public void nextGet() {
        get(1);
    }

    static public class CrashMSG {
    }

    static public class PutREQ {
        public final int numREQ;
        public final int key;
        public final int val;

        public PutREQ(int numREQ,int key,int val) {
            this.numREQ=numREQ;
            this.key = key;
            this.val=val;
        }
    }

    static public class PutMSG {
        public final int numREQ;
        public final int val;
        public final int seq;
        public final int i;

        public PutMSG(int numREQ,int val,int seq, int i) {
            this.numREQ=numREQ;
            this.val=val;
            this.seq=seq;
            this.i=i;
        }
    }

    static public class GetMSG {
        public final int numREQ;
        public final int val;
        public final int seq;
        public final int i;

        public GetMSG(int numREQ,int val,int seq, int i) {
            this.numREQ=numREQ;
            this.val=val;
            this.seq=seq;
            this.i=i;
        }
    }

    static public class ACKGetMSG {
        public final int numREQ;
        public final int val;


        public ACKGetMSG(int numREQ,int val) {
            this.numREQ=numREQ;
            this.val=val;
        }
    }
    static public class ACKPutMSG {
        public final int numREQ;
        public final int val;


        public ACKPutMSG(int numREQ,int val) {
            this.numREQ=numREQ;
            this.val=val;
        }
    }
    static public class GetREQ {
        public final int numREQ;
        public final int key;

        public GetREQ(int numREQ,int key) {
            this.numREQ=numREQ;
            this.key = key;
        }
    }
    static public class ACKGetREQ {
        public final int numREQ;
        public final int val;
        public final int seq;
        public final int i;

        public ACKGetREQ(int numREQ,int val, int seq, int i) {
            this.numREQ=numREQ;
            this.val = val;
            this.seq = seq;
            this.i = i;
        }
    }
    static public class ACKPutREQ {
        public final int numREQ;
        public final int val;
        public final int seq;
        public ACKPutREQ(int numREQ,int val,int seq) {
            this.numREQ=numREQ;
            this.val=val;
            this.seq = seq;
        }
    }
    static public class LaunchMSG {
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if(active)
            if(message instanceof CrashMSG) {
                setCrash();
                log.info("["+getSelf().path().name()+"] received message CRASH from ["+ getSender().path().name()+"]");
            }
            if(message instanceof LaunchMSG) {
                put(1,kPut*id);
            }
            //when a put REQ is received by i send an ACK put REQ with the seq of i to the sender
            if(message instanceof PutREQ) {
                PutREQ p= (PutREQ)message;
                getSender().tell(new ACKPutREQ(p.numREQ,p.val,seq), getSelf());
                log.info("["+getSelf().path().name()+"] received put request from ["+ getSender().path().name() +"] with data: ["+p.key+p.val+"]");
            }
            //the sender collect all the seq from ACKs msg and keep the max
            if(message instanceof ACKPutREQ) {
                ACKPutREQ p=(ACKPutREQ)message;
                if(p.seq>seqMAX) {
                    this.seqMAX=p.seq;
                }
                quorum++; // when a ACK is received we add 1 to the quorum counter
                //when we have n/2+1 ACK we can ask to all process the write the maxSeq+1 and the value associate to the REQ
                if (quorum==Implementation.N_PROCESS/2+1) {
                    quorum=0; // we reinitialize the quorum counter
                    //log.info("["+getSelf().path().name()+"] ECRIVEZ :"+"CLE :1 "+" VAL="+p.val+" SEQ="+(seq+1));

                    for(ActorRef a: Implementation.list.getList()) {
                        a.tell(new PutMSG(p.numREQ,p.val,(this.seqMAX)+1,this.id), getSelf());
                    }
                }
            }
            // when a PutMSG is received the process has to write the value and the seq
            if(message instanceof PutMSG) {

                PutMSG p= (PutMSG)message;
                //if the Put(seq,iMAX) >>Process(seq,iMax) then we can write
                if(p.seq>=seq) {
                    if(p.seq==seq) {
                        if(p.i>iMAX) {
                            this.seq=p.seq;
                            this.val=p.val;
                            this.iMAX=p.i;
                        }
                    }
                    else {
                        this.seq=p.seq;
                        this.val=p.val;
                        this.iMAX=p.i;
                    }
                }
                //log.info("["+getSelf().path().name()+"] JAI ECRIS :"+val+seq);
                //when the process has finished its write we send an ACKPutMSG to notify the sender
                getSender().tell(new ACKPutMSG(p.numREQ,p.val), getSelf());
            }
            //collect every ACKPutMSG
            if(message instanceof ACKPutMSG) {
                quorum++;
                ACKPutMSG p= (ACKPutMSG)message;
                //when we have n/2+1 ACK we can validate the put
                if (quorum==Implementation.N_PROCESS/2+1) {
                    log.info("["+getSelf().path().name()+"] [Put#"+p.numREQ+"]"+" [OK]"+ "["+p.val+"]");
                    quorum=0; //we reinitialize the quorum counter
                    nextGet(); //we can go to the next operation
                }
            }
            //[FIRST PHASE]
            //the process who wnats to read send a GetREQ to every process
            if(message instanceof GetREQ) {
                GetREQ g= (GetREQ)message;
                //when a put REQ is received by i send an ACKGetREQ with the (val,seq,iMAX) of i to the sender
                getSender().tell(new ACKGetREQ(g.numREQ,this.val,this.seq,this.iMAX), getSelf());
                //log.info("["+getSelf().path().name()+"] received get request from ["+ getSender().path().name() +"] with data: ["+g.key+"]");

            }
            //collecting  all the ACKgetREQ and keep the max (seq,iMAX) couple and the v associate to this max couple
            if(message instanceof ACKGetREQ) {
                ACKGetREQ g= (ACKGetREQ)message;
                if(g.seq>=seqMAX) {
                    if(g.seq==seqMAX) {
                        if(g.i>iMAX) {
                            this.seqMAX=g.seq;
                            this.valMAX=g.val;
                            this.iMAX=g.i;
                        }
                    }
                    else {
                        this.valMAX=g.val;
                        this.seqMAX=g.seq;
                        this.iMAX=g.i;
                    }
                }

                quorum++;
                //log.info("["+getSelf().path().name()+"] received ACKget request from ["+ getSender().path().name() +"] with data: ["+g.val+g.seq+"]");
                //[SECOND PHASE]
                if (quorum==Implementation.N_PROCESS/2+1) {
                    quorum=0;
                    //when we have collecting enough ACKGetREQ we ask to all process to write the (v,seq,iMAX)
                    for(ActorRef a: Implementation.list.getList()) {
                        a.tell(new GetMSG(g.numREQ,this.valMAX,this.seqMAX,this.iMAX), getSelf());
                    }
                }

            }
            //similar than the PutMSG
            if(message instanceof GetMSG) {

                GetMSG p= (GetMSG)message;
                if(p.seq>this.seq) {
                    this.seq=p.seq;
                    this.val=p.val;
                    this.iMAX=p.i;
                }

                if(p.seq==this.seq) {
                    if(p.i>this.iMAX) {
                        this.seq=p.seq;
                        this.val=p.val;
                        this.iMAX=p.i;
                    }
                }


                //log.info("["+getSelf().path().name()+"] JAI ECRIS :"+val+seq);
                //when the process has finished its write we send an ACKGetMSG to notify the sender
                getSender().tell(new ACKGetMSG(p.numREQ,p.val), getSelf());
            }

            if(message instanceof ACKGetMSG) {
                quorum++;
                ACKGetMSG p= (ACKGetMSG)message;
                //when we have n/2+1 ACK we can validate the get and display the v value
                if (quorum==Implementation.N_PROCESS/2+1) {
                    log.info("["+getSelf().path().name()+"] [Get#"+p.numREQ+"]"+" ["+p.val+"]");
                    quorum=0;//we reinitialize the quorum counter
                    nextPut();//we can go to the next operation
                }
            }
    }
}

