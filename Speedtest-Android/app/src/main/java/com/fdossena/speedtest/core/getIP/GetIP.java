package com.fdossena.speedtest.core.getIP;

import java.io.BufferedReader;
import java.util.HashMap;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;

public abstract class GetIP extends Thread{
    private Connection c;
    private String path;
    private boolean isp;
    private String distance;
    public GetIP(Connection c, String path, boolean isp, String distance){
        this.c=c;
        this.path=path;
        this.isp=isp;
        if(!(distance==null||distance.equals(SpeedtestConfig.DISTANCE_KM)||distance.equals(SpeedtestConfig.DISTANCE_MILES))) throw new IllegalArgumentException("Distance must be null, mi or km");
        this.distance=distance;
        start();
    }

    public void run(){
        try{
            String s=path;
            if(isp){
                s+= Utils.url_sep(s)+"isp=true";
                if(!distance.equals(SpeedtestConfig.DISTANCE_NO)){
                    s+=Utils.url_sep(s)+"distance="+distance;
                }
            }
            c.GET(s,true);
            HashMap<String,String> h=c.parseResponseHeaders();
            BufferedReader br=new BufferedReader(c.getInputStreamReader());
            if(h.get("content-length")!=null){
                //standard encoding
                char[] buf=new char[Integer.parseInt(h.get("content-length"))];
                br.read(buf);
                String data=new String(buf);
                onDataReceived(data);
            }else{
                //chunked encoding hack. TODO: improve this garbage with proper chunked support
                c.readLineUnbuffered(); //ignore first line
                String data=c.readLineUnbuffered(); //actual info we want
                c.readLineUnbuffered(); //ignore last line (0)
                onDataReceived(data);
            }

            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    public abstract void onDataReceived(String data);
    public abstract void onError(String err);
}
