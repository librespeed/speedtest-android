package com.fdossena.speedtest.core.download;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.log.Logger;
import com.fdossena.speedtest.core.upload.UploadStream;

import javax.net.SocketFactory;

public abstract class DownloadStream {
    private String server, path;
    private int ckSize;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Downloader downloader;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private long currentDownloaded=0, previouslyDownloaded=0;
    private boolean stopASAP=false;
    private Logger log;
    private int max_number_of_restarts;
    private int numEnded;
    private int numStarted;
    SocketFactory clientSocketFactory;

    public DownloadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, int max_number_of_restarts, Logger log, SocketFactory clientSocketFactory) {
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
        this.clientSocketFactory = clientSocketFactory;
        init();
    }

    private void init(){
        synchronized (this)
        {
            numStarted++;
            // If this method was called from the onError method of the downloader, a new downloader will be created to replace that downloader.
            // In this case, numStarted was incremented right before numEnded was incremented by the call to onEnd() of the downloader.
            // The difference between numStarted and numEnded went to 2, so numEnded cannot become equal to numStart before the new downloader ends.
            // If this method was called from the constructor, then numStarted was incremented in the creator thread and stopASAP() and join()
            // can only be called after this increment.
        }
        new Thread("DownloadStream"){
            public void run(){
                synchronized (DownloadStream.this)  {
                    currentDownloaded=0;
                }
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer, clientSocketFactory);
                    Downloader newDownloader =new Downloader(c,path,ckSize) {
                        @Override
                        public void onProgress(long downloaded) {
                            synchronized (DownloadStream.this) {
                                currentDownloaded = downloaded;
                            }
                        }
                        @Override
                        public void onError(String err) {
                            log("A downloader died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                DownloadStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                previouslyDownloaded+=currentDownloaded;
                                synchronized (DownloadStream.this) {
                                    previouslyDownloaded+=currentDownloaded;
                                }
                                if (max_number_of_restarts > numStarted)
                                    init();
                            }
                        }
                        @Override
                        public void onEnd() {
                            synchronized(DownloadStream.this) {
                                DownloadStream.this.numEnded++; // if the difference between numStarted and numEnded goes to zero, the download test is over, so notify the waiting thread (see join() method)
                                DownloadStream.this.notify();   // the difference only goes to zero if an downloader ends without calling onError() and creating a new downloader
                            }
                        }
                    };
                    synchronized(DownloadStream.this)
                    {
                        if (!stopASAP) {
                            downloader = newDownloader; // from ths point on, any calls to stopASAP will stop the new downloader
                        }
                        else
                            // DownStream was stopped (by a call to stopASAP()) during the creation of the new Downloader or
                            // right before and either told the old downloader to stop (by a call to downloader's stopASAP())
                            // or told no downloader to stop because there was no old downloader. Anyway, the new downloader was
                            // not told to stop.
                            // Nobody will tell the downstream to stop again, so stop the new downloader immediately.
                            newDownloader.stopASAP();
                    }
                }catch (Throwable t){
                    log("A Downloader failed hard");
                    try{c.close();} catch (Throwable t1){} // If the Downloader failed to be created, it may not close the connection
                    if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                        synchronized (DownloadStream.this) {
                            if (max_number_of_restarts > numStarted)
                                init();
                            numEnded++; // only marks the end after init() has marked the start of the new downloader
                            if (numEnded == numStarted)
                                DownloadStream.this.notify(); // notifies the waiting thread, because the download test over
                        }
                    } else {
                        onError(t.toString());
                        synchronized (DownloadStream.this) {
                            numEnded++;
                            if (numEnded == numStarted)
                                DownloadStream.this.notify(); // notifies the waiting thread, because the downloader test is over
                        }
                    }
                }
            }
        }.start();
    }

    public abstract void onError(String err);

    public synchronized void stopASAP() {
        stopASAP=true;
        if(downloader !=null)
            downloader.stopASAP();
    }

    public synchronized long getTotalDownloaded() {
        return previouslyDownloaded+currentDownloaded;
    }

    public synchronized void resetDownloadCounter() {
        previouslyDownloaded=0;
        currentDownloaded=0;
        if(downloader !=null)
            downloader.resetDownloadCounter();
    }

    public void join(){
        synchronized(this) {
            while (numStarted > numEnded) // if this test fails, all created uploaders have ended
                try { wait(); } catch(InterruptedException e) {};
        }
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
