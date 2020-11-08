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
        public IPResult(boolean ended, double percentage, String ipData)
        {
            this.ended = ended;
            this.percentage = percentage;
            this.ipData = ipData;
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
        return new IPResult(!running, running?0:100, ipData);
    }

    public void test(String host, int port, String path, int nt, SpeedTestListener log, SocketFactory clientSocketFactory)
    {
        if (running)
            return;  // does not not start again if still running
        running =  true;
        ipData = null;
        error = null;
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
            running =  false;
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
                System.out.println(l);
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

                String l = lineReader.readLine();
                l = lineReader.readLine();
                update(l);
                l = lineReader.readLine();

                /*
                String l = lineReader.readLine();
                content_length = Integer.parseInt(l.trim(),10); // the server sends the length in decimal not hexadecimal as specified fro chunked data
                StringBuilder sb = new StringBuilder();
                while (content_length!=0)
                {
                    char[] buf=new char[content_length];
                    BufferedReader br=new BufferedReader(new InputStreamReader(in));
                    br.read(buf);
                    String data=new String(buf);
                    sb.append(data);
                    content_length = Integer.parseInt(l.trim());
                } */
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
