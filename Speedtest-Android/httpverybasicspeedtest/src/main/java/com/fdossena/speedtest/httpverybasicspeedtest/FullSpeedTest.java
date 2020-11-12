/**
 *
 * This example is released under the MIT license, so it can be copied, modified and used in
 * proprietary software.
 *
 * Note the the speed test core library is released under LGLP. PAY ATTENTION.
 *
 */
package com.fdossena.speedtest.httpverybasicspeedtest;


import javax.net.SocketFactory;

public class FullSpeedTest
{
    UploadSpeedTest uploadSpeedTest;
    DownloadSpeedTest downloadSpeedTest;
    PingTest pingTest;
    GetIP getIP;
    TestPoint tp;

    boolean running = false;
    boolean ended;
    SpeedTestResult res;
    public FullSpeedTest()
    {
        uploadSpeedTest = new UploadSpeedTest();
        downloadSpeedTest = new DownloadSpeedTest();
        pingTest = new PingTest();
        getIP = new GetIP();
    }
    class DeathListener implements SpeedTestListener
    {
        @Override
        public void speedTestEnded()
        {
        }
    }
    public SpeedTestResult getResult()
    {
        GetIP.IPResult ipRes = getIP.getResult();
        PingTest.PingResult pingRes = pingTest.getResult();
        DownUpSpeedTest.DownUpSpeedResult downRes = downloadSpeedTest.getResult();
        DownUpSpeedTest.DownUpSpeedResult upRes = uploadSpeedTest.getResult();
        SpeedTestResult res =new SpeedTestResult(tp);
        res.ping = pingRes.ping;
        res.jitter = pingRes.jitter;
        res.downloadSpeed = downRes.speed;
        res.uploadSpeed = upRes.speed;
        res.ipInfo = ipRes.ipData;
        if (res.error==null) res.error = ipRes.error;
        if (res.error==null) res.error = pingRes.error;
        if (res.error==null) res.error = downRes.error;
        if (res.error==null) res.error = upRes.error;
        res.progress_ping = pingRes.percentage;
        res.progress_jitter = pingRes.percentage;
        res.progress_downloadSpeed = downRes.percentage;
        res.progress_uploadSpeed = upRes.percentage;
        synchronized (this)
        {
            res.ended = ended;
        }
        return res;
    }
    public  void test(TestPoint tp, SpeedTestListener listener,SocketFactory clientSocketFactory)
    {
        DeathListener deathListener = new DeathListener();
        synchronized(this)
        {
            if (running)
                return;
            running = true;
            ended = false;
            this.tp = tp;
            res = new SpeedTestResult(tp);
        }
        try
        {
            getIP.clear();
            pingTest.clear();
            downloadSpeedTest.clear();
            uploadSpeedTest.clear();
            getIP.test(tp.getHost(), tp.getPort(),  tp.getGetIPPath(), deathListener, clientSocketFactory);
            pingTest.test(tp.getHost(), tp.getPort(),  tp.getPingPath(), deathListener, clientSocketFactory);
            downloadSpeedTest.test(tp.getHost(), tp.getPort(),  tp.getDownloadPath(), 10, deathListener, clientSocketFactory);
            uploadSpeedTest.test(tp.getHost(), tp.getPort(),  tp.getUploadPath(), 10, deathListener, clientSocketFactory);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        finally
        {
            synchronized (this)
            {
                running = false;
                ended = true;
            }
        }
        listener.speedTestEnded();
    }
}
