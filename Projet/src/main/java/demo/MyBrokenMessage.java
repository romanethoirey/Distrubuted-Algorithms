package demo;

import akka.actor.ActorRef;

import java.util.ArrayList;

public class MyBrokenMessage {
    public final ArrayList<ActorRef> list;
    public final int i;
    public final String s;

    public MyBrokenMessage(ArrayList<ActorRef> list, int i, String s) {
        this.list = list;
        this.i=i;
        this.s=s;
    }
  }