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
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Connection {
    private Socket socket;

    public boolean isIPv6(){
        return socket!=null&&socket.getInetAddress() instanceof java.net.Inet6Address;
    }
    private String host; private int port;
    private String basePath="";
    private int mode=MODE_NOT_SET;
    private static final int MODE_NOT_SET=0, MODE_HTTP=1, MODE_HTTPS=2;

    // Replaced at startup with ClientInfo.userAgent, which can name the app
    // version; this default is what the engine sends if that never runs.
    private static String userAgent="librespeed-android (android "+Build.VERSION.RELEASE+"; "
            +(Build.SUPPORTED_ABIS!=null&&Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"unknown")+"; "+Build.PRODUCT+")";

    public static void setUserAgent(String ua){
        userAgent=ua;
    }

    private static final String
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
                basePath=stripTrailingSlashes(u.getPath());
            }catch(Throwable t){
                throw new IllegalArgumentException("Malformed URL (HTTP)");
            }
        }else if(url.startsWith("https://")){
            tryHTTPS=true;
            try{
                URL u=new URL(url);
                host=u.getHost();
                port=u.getPort();
                basePath=stripTrailingSlashes(u.getPath());
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
                basePath=stripTrailingSlashes(u.getPath());
            }catch(Throwable t){
                throw new IllegalArgumentException("Malformed URL (HTTP/HTTPS)");
            }
        }else{
            throw new IllegalArgumentException("Malformed URL (Unknown or unspecified protocol)");
        }
        if(mode == MODE_NOT_SET && tryHTTPS){
            Socket s=new Socket();
            try{
                if(connectTimeout>0){
                    s.connect(new InetSocketAddress(host, port==-1?443:port),connectTimeout);
                    s.setSoTimeout(connectTimeout); //bounds the handshake reads too
                }else{
                    s.connect(new InetSocketAddress(host, port==-1?443:port));
                }
                SSLSocketFactory factory=(SSLSocketFactory)SSLSocketFactory.getDefault();
                //wrapping with the hostname sets SNI; endpoint identification makes the
                //handshake reject certificates that were not issued for this host
                SSLSocket ssl=(SSLSocket)factory.createSocket(s,host,port==-1?443:port,true);
                SSLParameters params=ssl.getSSLParameters();
                params.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(params);
                //the handshake must succeed before HTTPS is committed, otherwise a plain
                //HTTP server would pass the TCP connect and block the HTTP fallback below
                ssl.startHandshake();
                if(connectTimeout>0) s.setSoTimeout(0);
                socket=ssl;
                mode=MODE_HTTPS;
            }catch(Throwable t){
                try{s.close();}catch(Throwable t1){}
            }
        }
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

    //RFC 7230 section 5.4: the Host header carries the port unless it is the scheme default
    private String hostHeader(){
        if(port!=-1&&port!=(mode==MODE_HTTPS?443:80)) return host+":"+port;
        return host;
    }

    //endpoint paths from the server list are relative to the server URL's path, if it has one
    private String resolvePath(String path){
        if(path.startsWith("/")) return path;
        return basePath+"/"+path;
    }

    private static String stripTrailingSlashes(String path){
        if(path==null) return "";
        while(path.endsWith("/")) path=path.substring(0,path.length()-1);
        return path;
    }

    public void GET(String path, boolean keepAlive) throws Exception{
        try{
            path=resolvePath(path);
            //one write per request: with Nagle's algorithm every further small
            //write waits for the ACK of the previous one, which added a whole
            //round trip to every ping
            StringBuilder r=new StringBuilder();
            r.append("GET ").append(path).append(" HTTP/1.1\r\n");
            r.append("Host: ").append(hostHeader()).append("\r\n");
            r.append("User-Agent: ").append(userAgent).append("\r\n");
            r.append("Connection: ").append(keepAlive?"keep-alive":"close").append("\r\n");
            r.append("Accept-Encoding: identity\r\n");
            if(LOCALE!=null) r.append("Accept-Language: ").append(LOCALE).append("\r\n");
            r.append("\r\n");
            PrintStream ps=getPrintStream();
            ps.print(r.toString());
            ps.flush();
        }catch (Throwable t){
            throw new Exception("Failed to send GET request");
        }
    }

    public void POST(String path, boolean keepAlive, String contentType, long contentLength) throws Exception{
        try{
            path=resolvePath(path);
            StringBuilder r=new StringBuilder();
            r.append("POST ").append(path).append(" HTTP/1.1\r\n");
            r.append("Host: ").append(hostHeader()).append("\r\n");
            r.append("User-Agent: ").append(userAgent).append("\r\n");
            r.append("Connection: ").append(keepAlive?"keep-alive":"close").append("\r\n");
            r.append("Accept-Encoding: identity\r\n");
            if(LOCALE!=null) r.append("Accept-Language: ").append(LOCALE).append("\r\n");
            if(contentType!=null) r.append("Content-Type: ").append(contentType).append("\r\n");
            r.append("Content-Encoding: identity\r\n");
            if(contentLength>=0) r.append("Content-Length: ").append(contentLength).append("\r\n");
            r.append("\r\n");
            PrintStream ps=getPrintStream();
            ps.print(r.toString());
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
            String[] statusParts=s.trim().split(" ");
            if(statusParts.length<2||!statusParts[1].startsWith("2")) throw new Exception("Did not receive an HTTP 2xx ("+s.trim()+")");
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
