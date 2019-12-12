package com.lightbend.akka.sample;

import java.io.IOException;

import com.lightbend.akka.sample.Process.Members;
import com.lightbend.akka.sample.Process.State;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

//To print time 
import java.text.SimpleDateFormat;
import java.util.Date;
//

import java.util.ArrayList;

import java.util.Collections;

public class Main {
    
    public static void main(String[] args) {
	
	final int N_PROCESS=100;  // The system size
	final int N_FAULTY = N_PROCESS%2 == 0 ? (N_PROCESS/2)-1 : N_PROCESS/2;
	final int N_LAUNCH = N_PROCESS - N_FAULTY;

	final ActorSystem system = ActorSystem.create("system");

	Date now = new Date();
	
    final ArrayList<ActorRef> members = new ArrayList<>();
    
    try {
      
    	
    	//#create-actors
 
	for(int i = 0; i <= N_PROCESS-1; i++) {
	     members.add(system.actorOf(Process.props(i), "Process"+Integer.toString(i)));
	}    

	System.out.println("System creation "+ now);

    // inform others process the list of process
    for(int x = 0; x < N_PROCESS; x = x + 1) {
        members.get(x).tell(new Members(members), ActorRef.noSender());
    }

    // shuffle and choose N_FAULTY random processes to fail
    Collections.shuffle(members);
  
    for(int i = 0; i < N_LAUNCH; i++) { // first N_LAUNCH are active
    	   members.get(i).tell(new State("CORRECT"), ActorRef.noSender());
    	} 
    for(int i = N_LAUNCH; i < N_PROCESS; i++) { // last N_FAULTY processes are faulty
       members.get(i).tell(new State("CRASHED"), ActorRef.noSender());
    }
       
	
      //#main-send-messages

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ioe) {
    } finally {
      system.terminate();
    }
  }
}
