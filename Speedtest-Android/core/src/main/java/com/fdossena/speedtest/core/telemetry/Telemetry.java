package com.fdossena.speedtest.core.telemetry;

import java.io.PrintStream;
import java.util.HashMap;

import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;

public abstract class Telemetry extends Thread{
    private Connection c;
    private String path;
    private String level, ispinfo, extra, dl, ul, ping, jitter, log;

    public Telemetry(Connection c, String path, String level, String ispinfo, String extra, String dl, String ul, String ping, String jitter, String log){
        if(level.equals(TelemetryConfig.LEVEL_DISABLED)){
            onDataReceived(null);
            return;
        }
        this.c=c;
        this.path=path;
        this.level=level;
        this.ispinfo=ispinfo;
        this.extra=extra;
        this.dl=dl;
        this.ul=ul;
        this.ping=ping;
        this.jitter=jitter;
        this.log=log;
        start();
    }

    public void run(){
        try{
            String s=path;
            StringBuilder sb=new StringBuilder();
            sb.append("ispinfo=");
            sb.append(Utils.urlEncode(ispinfo));
            sb.append("&dl=");
            sb.append(Utils.urlEncode(dl));
            sb.append("&ul=");
            sb.append(Utils.urlEncode(ul));
            sb.append("&ping=");
            sb.append(Utils.urlEncode(ping));
            sb.append("&jitter=");
            sb.append(Utils.urlEncode(jitter));
            if(level.equals(TelemetryConfig.LEVEL_FULL)) {
                sb.append("&log=");
                sb.append(Utils.urlEncode(log));
            }
            sb.append("&extra=");
            sb.append(Utils.urlEncode(extra));
            c.POST(s,false, "application/x-www-form-urlencoded",sb.length());
            PrintStream ps=c.getPrintStream();
            ps.print(sb.toString());
            ps.flush();
            HashMap<String,String> h=c.parseResponseHeaders();
            String data="";
            String transferEncoding=h.get("transfer-encoding");
            if(transferEncoding!=null&&transferEncoding.equalsIgnoreCase("chunked")){
                c.readLineUnbuffered();
            }
            data=c.readLineUnbuffered();
            onDataReceived(data);
            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    public abstract void onDataReceived(String data);
    public abstract void onError(String err);
}
