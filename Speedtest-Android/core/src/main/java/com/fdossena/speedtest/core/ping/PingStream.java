package com.fdossena.speedtest.core.ping;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.log.Logger;
import com.fdossena.speedtest.core.upload.UploadStream;

public abstract class PingStream {
    private String server, path;
    private int remainingPings=10;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Pinger pinger;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private boolean stopASAP=false;
    private Logger log;
    private int max_number_of_restarts;
    private int numEnded;
    private int numStarted;

    public PingStream(String server, String path, int pings, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, int max_number_of_restarts, Logger log) {
        this.server=server;
        this.path=path;
        remainingPings=pings<1?1:pings;
        this.errorHandlingMode=errorHandlingMode;
        this.connectTimeout=connectTimeout;
        this.soTimeout=soTimeout;
        this.recvBuffer=recvBuffer;
        this.sendBuffer=sendBuffer;
        this.log=log;
        this.max_number_of_restarts = max_number_of_restarts;
        numEnded = 0;
        numStarted = 0;
        init();
    }

    private void init(){
        synchronized (this)
        {
            numStarted++;
            // If this method was called from the onError method of the pinger, a new pinger will be created to replace that pinger.
            // In this case, numStarted was incremented right before numEnded was incremented by the call to onEnd() of the pinger.
            // The difference between numStarted and numEnded went to 2, so numEnded cannot become equal to numStart before the new pinger ends.
            // If this method was called from the constructor, then numStarted was incremented in the creator thread and stopASAP() and join()
            // can only be called after this increment.
        }
        new Thread("PingStream"){
            public void run(){
                if(remainingPings<=0) return;
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer);
                    Pinger newPinger =new Pinger(c,path) {
                        @Override
                        public boolean onPong(long ns) {
                            boolean r=PingStream.this.onPong(ns);
                            if(--remainingPings<=0||!r){
                                onDone();
                                return false;
                            } else return true;
                        }
                        @Override
                        public void onError(String err) {
                            log("A pinger died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                PingStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                if (max_number_of_restarts > numStarted)
                                    init();
                            }
                        }
                        @Override
                        public void onEnd()
                        {
                            synchronized(PingStream.this) {
                                numEnded++; // if the difference between numStarted and numEnded goes to zero, the ping test is over, so notify the waiting thread (see join() method)
                                if (numEnded == numStarted) // the difference only goes to zero if a pinger ends without calling onError() and creating a new pinger
                                    PingStream.this.notify();  // notifies the waiting thread, because the ping test is over
                            }
                        }
                    };
                    synchronized(PingStream.this)
                    {
                        if (!stopASAP) {
                            pinger = newPinger; // from ths point on, any calls to stopASAP will stop the new pinger
                        }
                        else
                            // PingStream was stopped (by a call to stopASAP()) during the creation of the new Pinger or
                            // right before and either told the old pinger to stop (by a call to pinger's stopASAP())
                            // or told no pinger to stop because there was no old pinger. Anyway, the new pinger was
                            // not told to stop.
                            // Nobody will tell the pingstream to stop again, so stop the new pinger immediately.
                            newPinger.stopASAP();
                    }
                }catch (Throwable t){
                    log("A pinger failed hard");
                    try{c.close();}catch (Throwable t1){} // If the Pinger failed to be created, it may not close the connection
                    if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                        synchronized (PingStream.this) {
                            if (max_number_of_restarts > numStarted)
                                init();
                            numEnded++; // only marks the end after init() has marked the start of the new pinger
                            if (numEnded == numStarted)
                                PingStream.this.notify(); // notifies the waiting thread, because the ping test over
                        }
                    } else {
                        onError(t.toString());
                        synchronized (PingStream.this) {
                            numEnded++;
                            if (numEnded == numStarted)
                                PingStream.this.notify(); // notifies the waiting thread, because the ping test is over
                        }
                    }
                }
            }
        }.start();
    }

    public abstract void onError(String err);
    public abstract boolean onPong(long ns);
    public abstract void onDone();

    public synchronized void stopASAP(){
        stopASAP=true;
        if(pinger !=null) pinger.stopASAP();
    }

    public void join() {
        synchronized(this) {
            while (numStarted > numEnded) // if this test fails, all created pingers have ended
                try { wait(); } catch(InterruptedException e) {};
        }
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
