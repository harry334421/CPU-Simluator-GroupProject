package pipeliningpreImpl.stage;

import java.util.concurrent.SynchronousQueue;

public class Fetch implements Runnable{

    SynchronousQueue<Integer> IF_ID;

    public Fetch (SynchronousQueue<Integer> IF_ID){
        this.IF_ID = IF_ID;
    }

    @Override
    public void run() {
        System.out.println("Thread Id: " + Thread.currentThread().getId() + " - fetch start");
        try {
            this.IF_ID.put(99);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Thread Id: " + Thread.currentThread().getId() + " - fetch end");
    }

}