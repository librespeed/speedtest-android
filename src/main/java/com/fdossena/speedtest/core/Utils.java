package com.fdossena.speedtest.core;

import java.net.URLEncoder;

class Utils {
    static String urlEncode(String s){
        try{return URLEncoder.encode(s, "utf-8");}catch(Throwable t){return null;}
    }
    static void sleep(long ms){
        try{Thread.sleep(ms);}catch (Throwable t){}
    }
    static void sleep(long ms, int ns){
        try{Thread.sleep(ms,ns);}catch (Throwable t){}
    }
    static String url_sep(String url){
        if(url.contains("?")) return "&"; else return "?";
    }
}
