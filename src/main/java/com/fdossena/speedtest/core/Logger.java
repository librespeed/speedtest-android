package com.fdossena.speedtest.core;

class Logger {
    private String log="";
    Logger(){}

    String getLog(){
        synchronized (this){
            return log;
        }
    }

    void l(String s){
        synchronized (this){
            log+=System.currentTimeMillis()+" "+s+"\n";
        }
    }
}
