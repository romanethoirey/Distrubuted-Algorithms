package demo;

import java.util.ArrayList;
import akka.actor.ActorRef;

public class ListofProcess {
    private ArrayList<ActorRef> list = new ArrayList<>();
    public ListofProcess(ArrayList<ActorRef> list){
        this.list=org.apache.commons.lang3.SerializationUtils.clone(list);
    }
    public ArrayList<ActorRef> getList() {
        return list;
    }
    public void add(ActorRef ref) {
        list.add(ref);
    }
}