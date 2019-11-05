package demo;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;

public class MyCorrectMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public final ArrayList<ActorRef> list;
    public final int i;
    public final String s;

    public MyCorrectMessage(ArrayList<ActorRef> list, int i, String s) {
        this.list = org.apache.commons.lang3.SerializationUtils.clone(list);
        this.i = i;
        this.s = s;
    }

  }