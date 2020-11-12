package com.fdossena.speedtest.core.base;

import java.net.URLEncoder;

public class Utils {
    public static String urlEncode(String s){
        try{return URLEncoder.encode(s, "utf-8");}catch(Throwable t){return null;}
    }
    public static void sleep(long ms){
        try{Thread.sleep(ms);}catch (Throwable t){}
    }
    public static void sleep(long ms, int ns){
        try{Thread.sleep(ms,ns);}catch (Throwable t){}
    }
    public static String url_sep(String url){
        if(url.contains("?")) return "&"; else return "?";
    }
}
