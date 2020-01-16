package com.fdossena.speedtest.core;

import com.fdossena.speedtest.core.config.SpeedtestConfig;

abstract class PingStream {
    private String server, path;
    private int remainingPings=10;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Pinger pinger;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private boolean stopASAP=false;
    private Logger log;

    PingStream(String server, String path, int pings, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log){
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
                if(remainingPings<=0) return;
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer);
                    if(stopASAP){
                        try{c.close();}catch (Throwable t){}
                        return;
                    }
                    pinger =new Pinger(c,path) {
                        @Override
                        boolean onPong(long ns) {
                            boolean r=PingStream.this.onPong(ns);
                            if(--remainingPings<=0||!r){
                                onDone();
                                return false;
                            } else return true;
                        }

                        @Override
                        void onError(String err) {
                            log("A pinger died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
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
                    }else onError(t.toString());
                }
            }
        }.start();
    }

    abstract void onError(String err);
    abstract boolean onPong(long ns);
    abstract void onDone();

    void stopASAP(){
        stopASAP=true;
        if(pinger !=null) pinger.stopASAP();
    }

    void join(){
        while(pinger==null) Utils.sleep(0,100);
        try{pinger.join();}catch (Throwable t){}
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
