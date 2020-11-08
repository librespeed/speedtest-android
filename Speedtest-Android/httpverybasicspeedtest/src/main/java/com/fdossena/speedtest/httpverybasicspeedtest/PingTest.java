package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

public class PingTest
{
    double jitter;
    int counter;
    double minPing;
    double prevPing;
    boolean running;
    Throwable error;

    int numToPing = 10;

    synchronized boolean update(long ns)
    {
        counter++;
        double ms = ns / 1000000.0;
        if (ms < minPing) minPing = ms;
        //ping = minPing;
        if (prevPing == -1)
            jitter=0;
        else
        {
            double j = Math.abs(ms - prevPing);
            jitter=j>jitter?(jitter*0.3+j*0.7):(jitter*0.8+j*0.2);
        }
        prevPing = ms;
        return counter > numToPing;
    }
    synchronized void setError(Throwable error)
    {
        this.error = error;
    }

    static class PingResult
    {
        boolean ended;
        double percentage;
        double ping;
        double jitter;
        public PingResult(boolean ended, double percentage, double ping, double jitter)
        {
            this.ended = ended;
            this.percentage = percentage;
            this.ping = ping;
            this.jitter = jitter;
        }
        public String toString()
        {
            return "ended = " + ended +
                   " percentage = " + percentage +
                   " ping = " + ping +
                   " jitter = " + jitter;
        }
    }
    public synchronized PingResult getResult()
    {
        return new PingResult(!running, counter * 100.0 / numToPing, minPing, jitter);
    }

    public void test(String host, int port, String path, int nt, SpeedTestListener log, SocketFactory clientSocketFactory)
    {
        if (running)
            return;  // does not not start again if still running
        running =  true;
        prevPing = -1;
        minPing = Double.MAX_VALUE;
        counter = 0;
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
            String s=path;
            for(;;)
            {
                HTTPHelper.putGetHeadersInStream(out, host, path);
                long t=System.nanoTime();
                boolean chunked=false;
                boolean ok=false;
                HTTPHelper.HTTPLineReader lineReader = new HTTPHelper.HTTPLineReader(in);
                for(;;)  // reads HTTP headers sent by the server
                {
                    String l = lineReader.readLine();
                    if(l==null) break;
                    l = l.trim().toLowerCase();
                    if (l.equals("transfer-encoding: chunked")) chunked=true;
                    if (l.contains("200 ok")) ok=true;
                    if (l.trim().isEmpty())
                    {
                        if(chunked)
                        {
                            lineReader.readLine();
                            lineReader.readLine();
                        }
                        break;
                    }
                }
                if(!ok)
                    throw new Exception("Did not get a 200");
                t=System.nanoTime()-t;
                if(update(t/2))
                    break;
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
