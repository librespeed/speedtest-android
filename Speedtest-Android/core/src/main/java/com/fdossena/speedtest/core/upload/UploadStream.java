package com.fdossena.speedtest.core.upload;

import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.log.Logger;
import com.fdossena.speedtest.core.ping.PingStream;

import javax.net.SocketFactory;

public abstract class UploadStream {
    private String server, path;
    private int ckSize;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Uploader uploader;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private long currentUploaded=0, previouslyUploaded=0;
    private boolean stopASAP=false;
    private Logger log;
    private int numEnded;
    private int numStarted;
    private int max_number_of_restarts;

    static int nid = 0;
    int id;
    SocketFactory clientSocketFactory;

    public UploadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, int max_number_of_restarts, Logger log, SocketFactory clientSocketFactory) {
        this.server=server;
        this.path=path;
        this.ckSize=ckSize;
        this.errorHandlingMode=errorHandlingMode;
        this.connectTimeout=connectTimeout;
        this.soTimeout=soTimeout;
        this.recvBuffer=recvBuffer;
        this.sendBuffer=sendBuffer;
        this.log=log;
        this.max_number_of_restarts = max_number_of_restarts;
        numEnded = 0;
        numStarted = 0;
        id = nid++;
        this.clientSocketFactory = clientSocketFactory;
        init();
    }

    private void init(){
        synchronized (this)
        {
            numStarted++;
            // If this method was called from the onError method of the uploader, a new uploader will be created to replace that downloader.
            // In this case, numStarted was incremented right before numEnded was incremented by the call to onEnd() of the uploader.
            // The difference between numStarted and numEnded went to 2, so numEnded cannot become equal to numStart before the new uploader ends.
            // If this method was called from the constructor, then numStarted was incremented in the creator thread and stopASAP() and join()
            // can only be called after this increment.
        }
        new Thread("UploadStream"){
            public void run(){
                synchronized (UploadStream.this)  {
                    currentUploaded = 0;
                }
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer, clientSocketFactory);
                    Uploader newUploader =new Uploader(c,path,ckSize) {
                        @Override
                        public void onProgress(long uploaded) {
                            synchronized (UploadStream.this) {
                                currentUploaded = uploaded;
                                System.out.println("Uploadstream update");
                            }
                        }
                        @Override
                        public void onError(String err) {
                            log("An uploader died");
                            UploadStream.this.onError(err);
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                UploadStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                synchronized (UploadStream.this) {
                                    previouslyUploaded+=currentUploaded;
                                }
                                if (max_number_of_restarts > numStarted)
                                {
                                    UploadStream.this.onWarning(err);
                                    init();
                                }
                                else
                                {
                                    UploadStream.this.onError(err);
                                    return;
                                }
                            }
                        }
                        @Override
                        public void onWarning(String err)
                        {
                            UploadStream.this.onWarning(err);
                        }
                        @Override
                        public void onEnd() {
                            synchronized(UploadStream.this) {
                                numEnded++; // if the difference between numStarted and numEnded goes to zero, the upload test is over, so notify the waiting thread (see join() method)
                                System.out.println("up onEnd "+id+" "+numStarted+" "+numEnded);
                                if (numEnded==numStarted) // the difference only goes to zero if an uploader ends without calling onError() and creating a new uploader
                                    UploadStream.this.notify();  // notifies the waiting thread, because the upload test over
                            }
                        }
                    };
                    synchronized(UploadStream.this)
                    {
                        if (!stopASAP) {
                            uploader = newUploader; // from ths point on, any calls to stopASAP will stop the new uploader
                        }
                        else
                            // UpStream was stopped (by a call to stopASAP()) during the creation of the new Uploader or
                            // right before and either told the old uploader to stop (by a call to uploader's stopASAP())
                            // or told no uploader to stop because there was no old uploader. Anyway, the new uploader was
                            // not told to stop.
                            // Nobody will tell the upstream to stop again, so stop the new uploader immediately.
                            newUploader.stopASAP();
                    }
                } catch (Throwable t){
                    log("An uploader failed hard");
                    try{c.close();}catch (Throwable t1){} // If the Uploader failed to be created, it may not close the connection
                    if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                        synchronized (UploadStream.this) {
                            if (max_number_of_restarts > numStarted)
                                init();
                            numEnded++; // only marks the end after init() has marked the start of the new uploader
                            if (numEnded == numStarted)
                                UploadStream.this.notify(); // notifies the waiting thread, because the upload test over
                        }
                    } else {
                        onError(t.toString());
                        synchronized (UploadStream.this) {
                            numEnded++;
                            if (numEnded == numStarted)
                                UploadStream.this.notify(); // notifies the waiting thread, because the upload test may be over
                        }
                    }
                }
            }
        }.start();
    }

    public abstract void onError(String err);

    public abstract void onWarning(String err);

    public synchronized void stopASAP() {
        stopASAP=true;
        if(uploader !=null)
            uploader.stopASAP();
    }

    public synchronized long getTotalUploaded() {
        return previouslyUploaded+currentUploaded;
    }

    public synchronized void resetUploadCounter(){
        previouslyUploaded=0;
        currentUploaded=0;
        if(uploader !=null)
            uploader.resetUploadCounter();
    }

    public void join(){
        synchronized(this) {
            while (numStarted > numEnded) // if this test fails, all created uploaders have ended
            {
                System.out.println("up join "+id+" "+numStarted+" "+numEnded);
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                }
            }
            System.out.println("up join "+id+" "+numStarted+" "+numEnded);
        }
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
