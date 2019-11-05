package demo;

import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.actor.ActorRef;

import java.util.ArrayList;

public class Process extends UntypedAbstractActor{

    // Logger attached to actor
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ArrayList<ActorRef> actorRefs = new ArrayList<>();
    private StampValue register;
    private boolean active = true;


    public Process() {}

    // Static function creating actor
    public static Props createActor() {
        return Props.create(Process.class, () -> {
            return new Process();
        });
    }

    static public class MyMessage {
        public final String data;

        public MyMessage(String data) {
            this.data = data;
        }
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof MyMessage){
            MyMessage m = (MyMessage) message;

            if(m.data.equals("Launch")) {
                active = true;
                // start put op send message to other

                log.info("["+getSelf().path().name()+"] received message from ["+ getSender().path().name() +"] with data: ["+m.data+"]");
            }

            if(m.data.equals("Crash")) {
                active = false;
                log.info("["+getSelf().path().name()+"] received message from ["+ getSender().path().name() +"] with data: ["+m.data+"]");
            }

        }

        if (message instanceof ArrayList){
            actorRefs = (ArrayList<ActorRef>) message;
            log.info("["+getSelf().path().name()+"] received message from ["+ getSender().path().name() +"] with data: "+actorRefs);
        }
    }

    public Object get(int k) {
        return null;
    }

    public void put(int k, Object v) {

    }

}
