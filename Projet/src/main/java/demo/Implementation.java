package demo;

import java.util.ArrayList;
import java.util.Collections;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
/**
 * @author Romane THOIREY
 * @description
 */
public class Implementation {

    public static void main(String[] args) {

        final ActorSystem system = ActorSystem.create("system");
        final LoggingAdapter log = Logging.getLogger(system, "main");

        ArrayList<ActorRef> actorRefs = new ArrayList<>();

        // Instantiate N actors
        int N = 5;
        for(int i = 0; i < N; i++){
            actorRefs.add(system.actorOf(Process.createActor(), "a"+i));
        }

        // We send the list of actors to each actor
        for(int i = 0; i < actorRefs.size(); i++ ){
            actorRefs.get(i).tell(actorRefs, actorRefs.get(i));
        }

        sleepFor(1);

        // We shuffle the list and split it in two lists : crash and launch
        int faulty = N / 2 ;

        Collections.shuffle(actorRefs);
        ArrayList<ActorRef> crashList = new ArrayList<>();
        for(int i = 0; i < faulty; i++) {
            crashList.add(actorRefs.get(i));
            actorRefs.remove(i);
        }

        // We say to remains process to Launch
        Process.MyMessage launch = new Process.MyMessage("Launch");
        for(int i = 0; i < actorRefs.size();  i++) {
            actorRefs.get(i).tell(launch, actorRefs.get(i));
        }

        // We say to faulty process to do nothing with crash
        Process.MyMessage crash = new Process.MyMessage("Crash");
        for(int i = 0; i < crashList.size(); i++) {
            actorRefs.get(i).tell(crash, actorRefs.get(i));
        }


        // We wait 5 seconds before ending system (by default)
        // But this is not the best solution.
        try {
            waitBeforeTerminate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }

    public static void waitBeforeTerminate() throws InterruptedException {
        Thread.sleep(5000);
    }

    public static void sleepFor(int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
