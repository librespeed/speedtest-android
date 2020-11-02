/**
 *
 * This example is released under the MIT license, so it can be copied, modified and used in
 * proprietary software.
 *
 * Note the the speed test core library is released under LGLP. PAY ATTENTION.
 *
 */
package com.playip.speedtest.examplespeedtest;

import com.fdossena.speedtest.core.Speedtest;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.serverSelector.TestPoint;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.SocketFactory;

public class ExampleSpeedTest
{
    public interface SpeedTestLog
    {
        void speedTestlog(String s);
    }

    Speedtest st = null;
    boolean testGoingOn = false;
    SpeedTestResult res;
    ExampleSpeedTest()
    {
        SpeedtestConfig stConfig = new SpeedtestConfig();
        TelemetryConfig telemetryConfig = new TelemetryConfig();
        st=new Speedtest();
        st.setSpeedtestConfig(stConfig);
        st.setTelemetryConfig(telemetryConfig);
    }
    static class SpeedTestResult
    {
        double ping;
        double jitter;
        double downloadSpeed;
        double uploadSpeed;
        String ipInfo;
        String error;
        double progress_ping;
        double progress_jitter;
        double progress_downloadSpeed;
        double progress_uploadSpeed;
        boolean ended;
        TestPoint tp;

        SpeedTestResult(TestPoint tp)
        {
            this.tp = tp;
        }
        public synchronized String toString()
        {
            return toJSON().toString();
        }
        public synchronized JSONObject toJSON()
        {
            try
            {
                JSONObject res = new JSONObject();
                res.put("ping", ping);
                res.put("jitter", jitter);
                res.put("downloadSpeed",downloadSpeed);
                res.put("uploadSpeed",uploadSpeed);
                res.put("ipInfo",ipInfo);
                res.put("error",error);
                res.put("progress_ping", progress_ping);
                res.put("progress_jitter", progress_jitter);
                res.put("progress_downloadSpeed",progress_downloadSpeed);
                res.put("progress_uploadSpeed",progress_uploadSpeed);
		        res.put("ended",ended);
                res.put("server",tp.toJSON());
                return res;
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }
        public synchronized double getPing() {  return ping; }
        public synchronized void setPing(double ping) { this.ping = ping; }
        public synchronized double getJitter() { return jitter; }
        public synchronized void setJitter(double jitter) { this.jitter = jitter; }
        public synchronized double getDownloadSpeed() { return downloadSpeed; }
        public synchronized void setDownloadSpeed(double downloadSpeed) { this.downloadSpeed = downloadSpeed; }
        public synchronized double getUploadSpeed() { return uploadSpeed; }
        public synchronized void setUploadSpeed(double uploadSpeed) { this.uploadSpeed = uploadSpeed; }
        public synchronized String getIpInfo() { return ipInfo; }
        public synchronized void setIpInfo(String ipInfo) { this.ipInfo = ipInfo; }
        public synchronized String getError() { return error; }
        public synchronized void setError(String error) { this.error = error; }
        public synchronized double getProgress_ping() { return progress_ping; }
        public synchronized void setProgress_ping(double progress_ping) { this.progress_ping = progress_ping; }
        public synchronized double getProgress_jitter() { return progress_jitter; }
        public synchronized void setProgress_jitter(double progress_jitter) { this.progress_jitter = progress_jitter; }
        public synchronized double getProgress_downloadSpeed() { return progress_downloadSpeed; }
        public synchronized void setProgress_downloadSpeed(double progress_downloadSpeed) { this.progress_downloadSpeed = progress_downloadSpeed; }
        public synchronized double getProgress_uploadSpeed() { return progress_uploadSpeed; }
        public synchronized void setProgress_uploadSpeed(double progress_uploadSpeed) { this.progress_uploadSpeed = progress_uploadSpeed; }
        public synchronized boolean isEnded() { return ended; }
        public synchronized void setEnded(boolean ended) { this.ended = ended; }
    }
    public SpeedTestResult test(TestPoint tp, final SpeedTestLog logger)
    {

        synchronized(this)
        {
            if (testGoingOn)
            {
                logger.speedTestlog("Test in progress");
                logger.speedTestlog(res.toString());
                return res;
            }
            testGoingOn = true;
            res = new SpeedTestResult(tp);
        }
        Speedtest.SpeedtestHandler speedTestHandler = new Speedtest.SpeedtestHandler()
        {
            @Override
            public void onDownloadUpdate(final double dl, final double progress)
            {
                res.setDownloadSpeed(dl);
                res.setProgress_downloadSpeed(progress);
                //logger.speedTestlog(res.toString());
            }
            @Override
            public void onUploadUpdate(final double ul, final double progress)
            {
                res.setUploadSpeed(ul);
                res.setProgress_uploadSpeed(progress);
                //logger.speedTestlog(res.toString());
            }
            @Override
            public void onPingJitterUpdate(final double ping, final double jitter, final double progress)
            {
                res.setPing(ping);
                res.setJitter(jitter);
                res.setProgress_jitter(progress);
                res.setProgress_ping(progress);
                //logger.speedTestlog(res.toString());
            }

            @Override
            public void onIPInfoUpdate(final String ipInfo)
            {
                res.setIpInfo(ipInfo);
                logger.speedTestlog(res.toString());
            }

            @Override
            public void onTestIDReceived(final String id, final String shareURL)
            {
            }

            @Override
            public void onEnd()
            {
                logger.speedTestlog("Speed test ended");
                res.setEnded(true);
                logger.speedTestlog(res.toString());
                synchronized(res)
                {
                    res.notify();
                }
            }

            @Override
            public void onCriticalFailure(String err)
            {
                res.setError(err);
                logger.speedTestlog(res.toString());
            }
        };
        try
        {
            st.setSpeedtestConfig(new SpeedtestConfig());  // you can change some parameters here
            st.setSpeedtestSocketfactory(SocketFactory.getDefault()); // you can choose a different SocketFactory, for example to test mobile speed even if the default connection is wifi
            st.setSelectedServer(tp);
            st.start(speedTestHandler);
        }
        catch(Throwable th)
        {
            res.setEnded(true);
            res.setError(th.getMessage());
            logger.speedTestlog(res.toString());
            System.out.println("Speed test could not start");
        }
        synchronized(res)
        {
            while (!res.isEnded())
                try { res.wait();} catch(InterruptedException ignored) {}
        }
        synchronized(this)
        {
            testGoingOn = false;
        }
        logger.speedTestlog("Speed test exited");
        return res;
    }
}
