package com.fdossena.speedtest.core.worker;

import org.json.JSONObject;

import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.download.DownloadStream;
import com.fdossena.speedtest.core.getIP.GetIP;
import com.fdossena.speedtest.core.log.Logger;
import com.fdossena.speedtest.core.ping.PingStream;
import com.fdossena.speedtest.core.serverSelector.TestPoint;
import com.fdossena.speedtest.core.telemetry.Telemetry;
import com.fdossena.speedtest.core.upload.UploadStream;

import java.util.Locale;

public abstract class SpeedtestWorker extends Thread{
    private TestPoint backend;
    private SpeedtestConfig config;
    private TelemetryConfig telemetryConfig;
    private boolean stopASAP=false;
    private double dl=-1, ul=-1, ping=-1, jitter=-1;
    private String ipIsp="";
    private Logger log=new Logger();

    public SpeedtestWorker(TestPoint backend, SpeedtestConfig config, TelemetryConfig telemetryConfig){
        this.backend=backend;
        this.config=config==null?new SpeedtestConfig():config;
        this.telemetryConfig=telemetryConfig==null?new TelemetryConfig():telemetryConfig;
        start();
    }

    public void run(){
        log.l("Test started");
        try {
            for (char t : config.getTest_order().toCharArray()) {
                if(stopASAP) break;
                if (t == '_') Utils.sleep(1000);
                if (t == 'I') getIP();
                if (t == 'D') dlTest();
                if (t == 'U') ulTest();
                if (t == 'P') pingTest();
            }
        }catch (Throwable t){
            onCriticalFailure(t.toString());
        }
        try{
            sendTelemetry();
        }catch (Throwable t){}
        onEnd();
    }

    private boolean getIPCalled=false;
    private void getIP(){
        if(getIPCalled) return; else getIPCalled=true;
        final long start=System.currentTimeMillis();
        Connection c = null;
        try {
            c = new Connection(backend.getServer(), config.getPing_connectTimeout(), config.getPing_soTimeout(), -1, -1);
        } catch (Throwable t) {
            if (config.getErrorHandlingMode().equals(SpeedtestConfig.ONERROR_FAIL)){
                abort();
                onCriticalFailure(t.toString());
            }
            return;
        }
        GetIP g = new GetIP(c, backend.getGetIpURL(), config.getGetIP_isp(), config.getGetIP_distance()) {
            @Override
            public void onDataReceived(String data) {
                ipIsp=data;
                try{
                    data=new JSONObject(data).getString("processedString");
                }catch (Throwable t){}
                log.l("GetIP: "+ data+ " (took "+(System.currentTimeMillis()-start)+"ms)");
                onIPInfoUpdate(data);
            }

            @Override
            public void onError(String err) {
                log.l("GetIP: FAILED (took "+(System.currentTimeMillis()-start)+"ms)");
                abort();
                onCriticalFailure(err);
            }
        };
        while (g.isAlive()) Utils.sleep(0, 100);
    }

    private boolean dlCalled=false;
    private void dlTest(){
        if(dlCalled) return; else dlCalled=true;
        final long start=System.currentTimeMillis();
        onDownloadUpdate(0,0);
        DownloadStream[] streams=new DownloadStream[config.getDl_parallelStreams()];
        for(int i=0;i<streams.length;i++){
            streams[i]=new DownloadStream(backend.getServer(),backend.getDlURL(),config.getDl_ckSize(),config.getErrorHandlingMode(),config.getDl_connectTimeout(),config.getDl_soTimeout(),config.getDl_recvBuffer(),config.getDl_sendBuffer(),log) {
                @Override
                public void onError(String err) {
                    log.l("Download: FAILED (took "+(System.currentTimeMillis()-start)+"ms)");
                    abort();
                    onCriticalFailure(err);
                }
            };
            Utils.sleep(config.getDl_streamDelay());
        }
        boolean graceTimeDone=false;
        long startT=System.currentTimeMillis(), bonusT=0;
        for(;;){
            double t=System.currentTimeMillis()-startT;
            if(!graceTimeDone&&t>=config.getDl_graceTime()*1000){
                graceTimeDone=true;
                for(DownloadStream d:streams) d.resetDownloadCounter();
                startT=System.currentTimeMillis();
                continue;
            }
            if(stopASAP||t+bonusT>=config.getTime_dl_max()*1000){
                for(DownloadStream d:streams) d.stopASAP();
                for(DownloadStream d:streams) d.join();
                break;
            }
            if(graceTimeDone) {
                long totDownloaded = 0;
                for (DownloadStream d : streams) totDownloaded += d.getTotalDownloaded();
                double speed = totDownloaded / ((t<100?100:t) / 1000.0);
                if (config.getTime_auto()) {
                    double b = (2.5 * speed) / 100000.0;
                    bonusT += b > 200 ? 200 : b;
                }
                double progress = (t + bonusT) / (double) (config.getTime_dl_max() * 1000);
                speed = (speed * 8 * config.getOverheadCompensationFactor()) / (config.getUseMebibits() ? 1048576.0 : 1000000.0);
                dl = speed;
                onDownloadUpdate(dl, progress>1?1:progress);
            }
            Utils.sleep(100);
        }
        if(stopASAP) return;
        log.l("Download: "+ dl+ " (took "+(System.currentTimeMillis()-start)+"ms)");
        onDownloadUpdate(dl,1);
    }

    private boolean ulCalled=false;
    private void ulTest(){
        if(ulCalled) return; else ulCalled=true;
        final long start=System.currentTimeMillis();
        onUploadUpdate(0,0);
        UploadStream[] streams=new UploadStream[config.getUl_parallelStreams()];
        for(int i=0;i<streams.length;i++){
            streams[i]=new UploadStream(backend.getServer(),backend.getUlURL(),config.getUl_ckSize(),config.getErrorHandlingMode(),config.getUl_connectTimeout(),config.getUl_soTimeout(),config.getUl_recvBuffer(),config.getUl_sendBuffer(),log) {
                @Override
                public void onError(String err) {
                    log.l("Upload: FAILED (took "+(System.currentTimeMillis()-start)+"ms)");
                    abort();
                    onCriticalFailure(err);
                }
            };
            Utils.sleep(config.getUl_streamDelay());
        }
        boolean graceTimeDone=false;
        long startT=System.currentTimeMillis(), bonusT=0;
        for(;;){
            double t=System.currentTimeMillis()-startT;
            if(!graceTimeDone&&t>=config.getUl_graceTime()*1000){
                graceTimeDone=true;
                for(UploadStream u:streams) u.resetUploadCounter();
                startT=System.currentTimeMillis();
                continue;
            }
            if(stopASAP||t+bonusT>=config.getTime_ul_max()*1000){
                for(UploadStream u:streams) u.stopASAP();
                for(UploadStream u:streams) u.join();
                break;
            }
            if(graceTimeDone) {
                long totUploaded = 0;
                for (UploadStream u : streams) totUploaded += u.getTotalUploaded();
                double speed = totUploaded / ((t<100?100:t) / 1000.0);
                if (config.getTime_auto()) {
                    double b = (2.5 * speed) / 100000.0;
                    bonusT += b > 200 ? 200 : b;
                }
                double progress = (t + bonusT) / (double) (config.getTime_ul_max() * 1000);
                speed = (speed * 8 * config.getOverheadCompensationFactor()) / (config.getUseMebibits() ? 1048576.0 : 1000000.0);
                ul = speed;
                onUploadUpdate(ul, progress>1?1:progress);
            }
            Utils.sleep(100);
        }
        if(stopASAP) return;
        log.l("Upload: "+ ul+ " (took "+(System.currentTimeMillis()-start)+"ms)");
        onUploadUpdate(ul,1);
    }

    private boolean pingCalled=false;
    private void pingTest(){
        if(pingCalled) return; else pingCalled=true;
        final long start=System.currentTimeMillis();
        onPingJitterUpdate(0,0,0);
        PingStream ps=new PingStream(backend.getServer(),backend.getPingURL(),config.getCount_ping(),config.getErrorHandlingMode(),config.getPing_connectTimeout(),config.getPing_soTimeout(),config.getPing_recvBuffer(),config.getPing_sendBuffer(),log) {
            private double minPing=Double.MAX_VALUE, prevPing=-1;
            private int counter=0;
            @Override
            public void onError(String err) {
                log.l("Ping: FAILED (took "+(System.currentTimeMillis()-start)+"ms)");
                abort();
                onCriticalFailure(err);
            }

            @Override
            public boolean onPong(long ns) {
                counter++;
                double ms = ns / 1000000.0;
                if (ms < minPing) minPing = ms;
                ping = minPing;
                if (prevPing == -1) {
                    jitter=0;
                }else {
                    double j = Math.abs(ms - prevPing);
                    jitter=j>jitter?(jitter*0.3+j*0.7):(jitter*0.8+j*0.2);
                }
                prevPing = ms;
                double progress = counter / (double) config.getCount_ping();
                onPingJitterUpdate(ping, jitter, progress>1?1:progress);
                return !stopASAP;
            }

            @Override
            public void onDone() {
            }
        };
        ps.join();
        if(stopASAP) return;
        log.l("Ping: "+ ping+" "+jitter+ " (took "+(System.currentTimeMillis()-start)+"ms)");
        onPingJitterUpdate(ping,jitter,1);
    }

    private void sendTelemetry(){
        if(telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) return;
        if(stopASAP&&telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_BASIC)) return;
        try{
            Connection c=new Connection(telemetryConfig.getServer(),-1,-1,-1,-1);
            Telemetry t=new Telemetry(c,telemetryConfig.getPath(),telemetryConfig.getTelemetryLevel(),ipIsp,config.getTelemetry_extra(),dl==-1?"":String.format(Locale.ENGLISH,"%.2f",dl),ul==-1?"":String.format(Locale.ENGLISH,"%.2f",ul),ping==-1?"":String.format(Locale.ENGLISH,"%.2f",ping),jitter==-1?"":String.format(Locale.ENGLISH,"%.2f",jitter),log.getLog()) {
                @Override
                public void onDataReceived(String data) {
                    if(data.startsWith("id")){
                        onTestIDReceived(data.split(" ")[1]);
                    }
                }

                @Override
                public void onError(String err) {
                    System.err.println("Telemetry error: "+err);
                }
            };
            t.join();
        }catch (Throwable t){
            System.err.println("Failed to send telemetry: "+t.toString());
            t.printStackTrace(System.err);
        }
    }

    public void abort(){
        if(stopASAP) return;
        log.l("Manually aborted");
        stopASAP=true;
    }

    public abstract void onDownloadUpdate(double dl, double progress);
    public abstract void onUploadUpdate(double ul, double progress);
    public abstract void onPingJitterUpdate(double ping, double jitter, double progress);
    public abstract void onIPInfoUpdate(String ipInfo);
    public abstract void onTestIDReceived(String id);
    public abstract void onEnd();

    public abstract void onCriticalFailure(String err);

}
