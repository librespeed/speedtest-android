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
        if(!(distance==null||distance.equals(SpeedtestConfig.DISTANCE_NO)||distance.equals(SpeedtestConfig.DISTANCE_KM)||distance.equals(SpeedtestConfig.DISTANCE_MILES))) throw new IllegalArgumentException("Distance must be null, no, mi or km");
        this.distance=distance;
        start();
    }

    public void run(){
        try{
            String s=path;
            if(isp){
                s+= Utils.url_sep(s)+"isp=true";
                if(distance!=null&&!distance.equals(SpeedtestConfig.DISTANCE_NO)){
                    s+=Utils.url_sep(s)+"distance="+distance;
                }
            }
            c.GET(s,true);
            HashMap<String,String> h=c.parseResponseHeaders();
            BufferedReader br=new BufferedReader(c.getInputStreamReader());
            if(h.get("content-length")!=null){
                //standard encoding. content-length counts UTF-8 bytes but the shared reader
                //(which may have buffered past the headers) yields chars, so read until the
                //decoded chars account for the whole body instead of trusting a single read
                int bytesExpected=Integer.parseInt(h.get("content-length"));
                StringBuilder sb=new StringBuilder();
                int bytesReceived=0;
                while(bytesReceived<bytesExpected){
                    int ch=br.read();
                    if(ch==-1) break;
                    sb.append((char)ch);
                    bytesReceived+=utf8Length((char)ch);
                }
                onDataReceived(sb.toString());
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

    private static int utf8Length(char c){
        if(c<0x80) return 1;
        if(c<0x800) return 2;
        if(Character.isSurrogate(c)) return 2; //half of a 4-byte sequence
        return 3;
    }

    public abstract void onDataReceived(String data);
    public abstract void onError(String err);
}
