package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;

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
    public synchronized DownUpSpeedResult getResult()  // the client code can keep calling this method to get partial results
    {
        long time2 = running ? timeEnd : System.currentTimeMillis();
        long delta = (time2 - timeIni) / 1000;
        double speed = numBytesMoved * 8.0 / delta / 1024 / 1024;
        int overhead = 1;
        double perc = (running ? Math.min(delta * 1.0 / (beginDelay + testLength + overhead), 1.0) : 1.0) * 100;
        return new DownUpSpeedResult(speed, error, perc, !running);
    }

    abstract SocketHolder getSocketTestRunnable(SpeedTestListener log, String host, int port, String path) throws IOException;

    int beginDelay = 10;
    int testLength = 30;

    public void test(String host, int port, String path, int nt, SpeedTestListener log)
    {
        if (running)
            return;  // does not not start again if still running
        stop = false;
        running =  true;
        try
        {
            Thread[] th = new Thread[nt];
            for (int t = 0; t < nt; t++)
            {
                th[t] = new Thread(getSocketTestRunnable(log,host, port, path));
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
