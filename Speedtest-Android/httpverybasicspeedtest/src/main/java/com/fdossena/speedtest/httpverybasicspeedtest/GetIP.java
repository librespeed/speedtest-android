package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

import javax.net.SocketFactory;

public class GetIP
{
    String ipData;
    Throwable error;
    boolean running;
    boolean ended;

    synchronized void update(String ipData)
    {
        this.ipData = ipData;
    }
    synchronized void setError(Throwable error)
    {
        this.error = error;
    }

    static class IPResult
    {
        boolean ended;
        double percentage;
        String ipData;
        Throwable error;
        public IPResult(boolean ended, double percentage, String ipData, Throwable error)
        {
            this.ended = ended;
            this.percentage = percentage;
            this.ipData = ipData;
            this.error = error;
        }
        public String toString()
        {
            return "ended = " + ended +
                   " percentage = " + percentage +
                   " idData = " + ipData;
        }
    }
    public synchronized IPResult getResult()
    {
        return new IPResult(ended, ended?100:0, ipData, error);
    }

    public synchronized void clear()
    {
        ipData = null;
        error = null;
        ended = false;
    }
    public void test(String host, int port, String path, SpeedTestListener log, SocketFactory clientSocketFactory)
    {
        synchronized (this)
        {
            if (running)
                return;  // does not not start again if still running
            running = true;
        }
        clear();
        try
        {
            SocketFactory factory = clientSocketFactory==null ? SocketFactory.getDefault() : clientSocketFactory;
            Socket socket = factory.createSocket();
            socket.connect(new InetSocketAddress(host, port));
            testIntern(socket, host, port, path);
        }
        catch(Throwable th)
        {
            setError(th);
        }
        finally
        {
            synchronized (this)
            {
                running = false;
                ended = true;
            }
            log.speedTestEnded();
        }
    }

    void testIntern(Socket socket, String host, int port, String path) throws IOException
    {
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        try
        {
            path += "?isp=true&distance=km";
            HTTPHelper.putGetHeadersInStream(out, host, path);
            HTTPHelper.HTTPLineReader lineReader = new HTTPHelper.HTTPLineReader(in);
            boolean chunked=false;
            boolean ok=false;
            int content_length = 0;
            for(;;)  // reads HTTP headers sent by the server
            {
                String l = lineReader.readLine();
                if(l==null) break;
                l = l.trim().toLowerCase();
                if (l.equals("transfer-encoding: chunked")) chunked=true;
                if (l.contains("200 ok")) ok=true;
                if (l.contains("content-length"))
                    content_length = Integer.parseInt(l.split(":")[1].trim());
                if (l.trim().isEmpty())
                    break;
            }
            if(!chunked)
            {
                // read all at once
                BufferedReader br=new BufferedReader(new InputStreamReader(in));
                char[] buf=new char[content_length];
                br.read(buf);
                String data=new String(buf);
                update(data);
            }
            else
            {
                // the server does not follow the official protocol for chunked content
                lineReader.readLine();
                String l = lineReader.readLine();
                update(l);
                lineReader.readLine();
            }
        }
        catch(Throwable th)
        {
            setError(th);
        }
        finally
        {
            try { socket.close(); } catch(Throwable th) {}
        }

    }
}
