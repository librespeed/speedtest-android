package com.fdossena.speedtest.core.ping;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.log.Logger;

public abstract class PingStream {
    private String server, path;
    private volatile int remainingPings=10;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private volatile Connection c=null;

    public Boolean usedIPv6(){
        Connection conn=c;
        return conn==null?null:conn.isIPv6();
    }
    private volatile Pinger pinger;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    //ended means the stream reached its terminal event (onDone or onError) and
    //no pinger will ever (re)appear; error recovery replaces pinger threads, so
    //waiting on one of them says nothing about the stream as a whole
    private volatile boolean stopASAP=false, ended=false;
    private Logger log;

    public PingStream(String server, String path, int pings, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log){
        this.server=server;
        this.path=path;
        remainingPings=pings<1?1:pings;
        this.errorHandlingMode=errorHandlingMode;
        this.connectTimeout=connectTimeout;
        this.soTimeout=soTimeout;
        this.recvBuffer=recvBuffer;
        this.sendBuffer=sendBuffer;
        this.log=log;
        init();
    }

    private void init(){
        if(stopASAP) return;
        if(c!=null){
            try{c.close();}catch (Throwable t){}
        }
        new Thread(){
            public void run(){
                if(pinger !=null) pinger.stopASAP();
                if(remainingPings<=0){
                    ended=true;
                    return;
                }
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer);
                    if(stopASAP){
                        ended=true;
                        try{c.close();}catch (Throwable t){}
                        return;
                    }
                    pinger =new Pinger(c,path) {
                        @Override
                        public boolean onPong(long ns) {
                            boolean r=PingStream.this.onPong(ns);
                            if(--remainingPings<=0||!r){
                                ended=true;
                                onDone();
                                return false;
                            } else return true;
                        }

                        @Override
                        public void onError(String err) {
                            if(stopASAP) return;
                            log("A pinger died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                ended=true;
                                PingStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                Utils.sleep(100);
                                init();
                            }
                        }
                    };
                }catch (Throwable t){
                    log("A pinger failed hard");
                    try{c.close();}catch (Throwable t1){}
                    if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                        Utils.sleep(100);
                        init();
                    }else{
                        ended=true;
                        onError(t.toString());
                    }
                }
            }
        }.start();
    }

    public abstract void onError(String err);
    public abstract boolean onPong(long ns);
    public abstract void onDone();

    public void stopASAP(){
        stopASAP=true;
        if(pinger !=null) pinger.stopASAP();
        //closing the connection unblocks a thread parked in a read or write,
        //making the stop prompt instead of waiting out the socket timeout
        Connection conn=c;
        if(conn!=null){
            try{conn.close();}catch (Throwable t){}
        }
    }

    public boolean hasEnded(){
        return ended;
    }

    public void join(){
        while(pinger==null&&!ended&&!stopASAP) Utils.sleep(1);
        Pinger p=pinger;
        if(p!=null){
            try{p.join();}catch (Throwable t){}
        }
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
