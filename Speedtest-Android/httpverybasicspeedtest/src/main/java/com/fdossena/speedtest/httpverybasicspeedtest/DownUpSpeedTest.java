package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

abstract public class DownUpSpeedTest
{
    long numBytesMoved;
    boolean stop;
    boolean running;
    long timeIni;
    long timeEnd;
    Throwable error;
    DownUpSpeedTest()
    {
    }
    synchronized boolean moved(long numBytes)
    {
        numBytesMoved += numBytes;
        return stop;
    }
    synchronized void setError(Throwable error)
    {
        this.error = error;
    }
    class DownUpSpeedResult
    {
        double speed;
        double percentage;
        boolean ended;
        Throwable error;
        public DownUpSpeedResult(double speed, Throwable error,double percentage, boolean ended)
        {
            this.speed = speed;
            this.error = error;
            this.ended = ended;
            this.percentage = percentage;
        }
        public String toString()
        {
            return  "Ended = " + ended +
                    " Percentage = " + percentage +
                    " Speed  = " + speed +
                    " Error = " + (error==null?"None":error.getMessage());
        }
    }
    public synchronized DownUpSpeedResult getResult()  // the client code can keep calling this method to get partial results
    {
        long time2 = running ? timeEnd : System.currentTimeMillis();
        long delta = (time2 - timeIni) / 1000;
        double speed = numBytesMoved * 8.0 / delta / 1024 / 1024;
        int overhead = 1;
        double perc = (running ? Math.min(delta * 1.0 / (beginDelay + testLength + overhead), 1.0) : 1.0) * 100;
        return new DownUpSpeedResult(speed, error, perc, !running);
    }
    abstract class SocketTestRunnable implements Runnable
    {
        SpeedTestListener log;
        Socket socket;
        String host;
        int port;
        String path;
        SocketTestRunnable(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException
        {
            this.log = log;
            this.socket = socket;
            this.host = host;
            this.port = port;
            this.path = path;
        }
    }

    abstract SocketTestRunnable getSocketTestRunnable(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException;

    int beginDelay = 10;
    int testLength = 30;

    public void test(String host, int port, String path, int nt, SpeedTestListener log, SocketFactory clientSocketFactory)
    {
        if (running)
            return;  // does not not start again if still running
        stop = false;
        running =  true;
        error = null;
        try
        {
            Thread[] th = new Thread[nt];
            for (int t = 0; t < nt; t++)
            {
                SocketFactory factory = clientSocketFactory==null ? SocketFactory.getDefault() : clientSocketFactory;
                Socket socket = factory.createSocket();
                socket.connect(new InetSocketAddress(host, port));
                th[t] = new Thread(getSocketTestRunnable(log,host, port, path, socket));
            }
            timeIni = System.currentTimeMillis();
            for (int t = 0; t < nt; t++)
                th[t].start();
            try
            {
                Thread.sleep(1000 * beginDelay);
            }
            catch (InterruptedException e)
            {
            }
            synchronized (this)
            {
                numBytesMoved = 0;  // to ignore initial speed
            }
            timeIni = System.currentTimeMillis(); // to ignore initial speed
            try
            {
                Thread.sleep(1000 * testLength);
            }
            catch (InterruptedException e)
            {
            }
            synchronized (this)
            {
                stop = true;
            }
            for (int t = 0; t < nt; t++)
            {
                try
                {
                    th[t].join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            timeEnd = System.currentTimeMillis();
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
}
