package demo;


import java.util.ArrayList;
import java.util.Random;
import demo.Process.CrashMSG;
import demo.Process.LaunchMSG;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
/**
 * @author Romane THOIREY
 * @description
 */
public class Implementation {

    public static int N_PROCESS = 3;  // number of process
    public static int M_OPERATIONS = 3; // number of get and put each process will make
    public static int FAULTY; //number of crash allowed

    public static ListofProcess list = new ListofProcess(new ArrayList<>()); //list of the process


    public static void main(String[] args) {

        if(N_PROCESS%2==0)
            FAULTY=N_PROCESS/2-1; //if the number of process is even F is N/2-1 (for 50 actors it will be 24)
        else
            FAULTY=N_PROCESS/2; //if the number of process is odd F will be N/2

        final ActorSystem system = ActorSystem.create("system");

        // Instantiate N_PROCESS
        for(int i = 0; i < N_PROCESS; i++){
            list.add(system.actorOf(Process.createActor(i), "Process"+Integer.toString(i)));
        }

        sleepFor(1);

        // We shuffle the list and split it in two lists : crash and launch

        Random rand = new Random();
        //making randomly [0,n/2 process crash by sending them a CrashMSG
        for(int i=0; i<FAULTY;i++) {
            list.getList().get(rand.nextInt(N_PROCESS)).tell(new CrashMSG(),ActorRef.noSender());
            //list.getList().get(i).tell(new CrashMSG(),ActorRef.noSender());
        }


        //launch every remaining process by sending them a LaunchMSG
        for(int i=0; i<N_PROCESS;i++) {
            list.getList().get(i).tell(new LaunchMSG(),ActorRef.noSender());
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
