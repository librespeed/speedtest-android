package com.fdossena.speedtest.core.base;

import android.os.Build;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class Connection {
    private Socket socket;
    private String host; private int port;
    private int mode=MODE_NOT_SET;
    private static final int MODE_NOT_SET=0, MODE_HTTP=1, MODE_HTTPS=2;

    private static final String USER_AGENT="Speedtest-Android/1.2.3 (SDK "+Build.VERSION.SDK_INT+"; "+Build.PRODUCT+"; Android "+Build.VERSION.RELEASE+")",
                                LOCALE= Build.VERSION.SDK_INT>=21?Locale.getDefault().toLanguageTag():null;

    public Connection(String url, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer){
        boolean tryHTTP=false, tryHTTPS=false;
        Locale.getDefault().toString();
        if(url.startsWith("http://")){
            tryHTTP=true;
            try{
                URL u=new URL(url);
                host=u.getHost();
                port=u.getPort();
            }catch(Throwable t){
                throw new IllegalArgumentException("Malformed URL (HTTP)");
            }
        }else if(url.startsWith("https://")){
            tryHTTPS=true;
            try{
                URL u=new URL(url);
                host=u.getHost();
                port=u.getPort();
            }catch(Throwable t){
                throw new IllegalArgumentException("Malformed URL (HTTPS)");
            }
        }else if(url.startsWith("//")){
            tryHTTP=true;
            tryHTTPS=true;
            try{
                URL u=new URL("http:"+url);
                host=u.getHost();
                port=u.getPort();
            }catch(Throwable t){
                throw new IllegalArgumentException("Malformed URL (HTTP/HTTPS)");
            }
        }else{
            throw new IllegalArgumentException("Malformed URL (Unknown or unspecified protocol)");
        }
        try{
            if(mode == MODE_NOT_SET && tryHTTPS){
                SocketFactory factory = SSLSocketFactory.getDefault();
                socket=factory.createSocket();
                if(connectTimeout>0){
                    socket.connect(new InetSocketAddress(host, port==-1?443:port),connectTimeout);
                }else{
                    socket.connect(new InetSocketAddress(host, port==-1?443:port));
                }
                mode=MODE_HTTPS;
            }
        }catch(Throwable t){}
        try{
            if(mode == MODE_NOT_SET && tryHTTP){
                SocketFactory factory = SocketFactory.getDefault();
                socket=factory.createSocket();
                if(connectTimeout>0) {
                    socket.connect(new InetSocketAddress(host, port == -1 ? 80 : port), connectTimeout);
                }else{
                    socket.connect(new InetSocketAddress(host, port == -1 ? 80 : port));
                }
                mode=MODE_HTTP;
            }
        }catch(Throwable t){}
        if(mode==MODE_NOT_SET) throw new IllegalStateException("Failed to connect");
        if(soTimeout>0) {
            try {
                socket.setSoTimeout(soTimeout);
            } catch(Throwable t){}
        }
        if(recvBuffer>0){
            try{
                socket.setReceiveBufferSize(recvBuffer);
            }catch(Throwable t){}
        }
        if(sendBuffer>0){
            try{
                socket.setSendBufferSize(sendBuffer);
            }catch(Throwable t){}
        }
    }

    private static final int DEFAULT_CONNECT_TIMEOUT=2000, DEFAULT_SO_TIMEOUT=5000;
    public Connection(String url){
        this(url,DEFAULT_CONNECT_TIMEOUT,DEFAULT_SO_TIMEOUT,-1,-1);
    }

    public InputStream getInputStream(){
        try{
            return socket.getInputStream();
        }catch (Throwable t){
            return null;
        }
    }

    public OutputStream getOutputStream(){
        try{
            return socket.getOutputStream();
        }catch (Throwable t){
            return null;
        }
    }

    private PrintStream ps=null;
    public PrintStream getPrintStream(){
        if(ps==null){
            try{
                ps=new PrintStream(getOutputStream(),false,"utf-8");
            }catch(Throwable t){
                ps=null;
            }
        }
        return ps;
    }
    private InputStreamReader isr=null;
    public InputStreamReader getInputStreamReader(){
        if(isr==null){
            try{
                isr=new InputStreamReader(getInputStream(),"utf-8");
            }catch(Throwable t){
                isr=null;
            }
        }
        return isr;
    }

    public void GET(String path, boolean keepAlive) throws Exception{
        try{
            if(!path.startsWith("/")) path="/"+path;
            PrintStream ps=getPrintStream();
            ps.print("GET "+path+" HTTP/1.1\r\n");
            ps.print("Host: "+host+"\r\n");
            ps.print("User-Agent: "+USER_AGENT);
            ps.print("Connection: "+(keepAlive?"keep-alive":"close")+"\r\n");
            ps.print("Accept-Encoding: identity\r\n");
            if(LOCALE!=null) ps.print("Accept-Language: "+LOCALE+"\r\n");
            ps.print("\r\n");
            ps.flush();
        }catch (Throwable t){
            throw new Exception("Failed to send GET request");
        }
    }

    public void POST(String path, boolean keepAlive, String contentType, long contentLength) throws Exception{
        try{
            if(!path.startsWith("/")) path="/"+path;
            PrintStream ps=getPrintStream();
            ps.print("POST "+path+" HTTP/1.1\r\n");
            ps.print("Host: "+host+"\r\n");
            ps.print("User-Agent: "+USER_AGENT+"\r\n");
            ps.print("Connection: "+(keepAlive?"keep-alive":"close")+"\r\n");
            ps.print("Accept-Encoding: identity\r\n");
            if(LOCALE!=null) ps.print("Accept-Language: "+LOCALE+"\r\n");
            if(contentType!=null) ps.print("Content-Type: "+contentType+"\r\n");
            ps.print("Content-Encoding: identity\r\n");
            if(contentLength>=0) ps.print("Content-Length: "+contentLength+"\r\n");
            ps.print("\r\n");
            ps.flush();
        }catch (Throwable t){
            throw new Exception("Failed to send POST request");
        }
    }

    public String readLineUnbuffered(){
        try {
            InputStreamReader in = getInputStreamReader();
            StringBuilder sb=new StringBuilder();
            while(true){
                int c=in.read();
                if(c==-1) break;
                sb.append((char)c);
                if(c=='\n') break;
            }
            return sb.toString();
        }catch(Throwable t){
            return null;
        }
    }

    public HashMap<String, String> parseResponseHeaders() throws Exception{
        try{
            HashMap<String,String> ret=new HashMap<>();
            String s=readLineUnbuffered();
            if(!s.contains("200 OK")) throw new Exception("Did not receive an HTTP 200 ("+s.trim()+")");
            while(true){
                s=readLineUnbuffered();
                if(s.trim().isEmpty()) break;
                if(s.contains(":")){
                    ret.put(s.substring(0,s.indexOf(":")).trim().toLowerCase(),s.substring(s.indexOf(":")+1).trim());
                }
            }
            return ret;
        }catch(Throwable t){
            throw new Exception("Failed to get response headers ("+t+")");
        }
    }

    public void close(){
        try{
            socket.close();
        }catch(Throwable t){}
        socket=null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getMode() {
        return mode;
    }

}
