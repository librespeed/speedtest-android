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
    //written by stream/getIP callback threads and read by this worker thread
    private volatile boolean stopASAP=false;
    private volatile double dl=-1, ul=-1, ping=-1, jitter=-1, loss=-1;
    private volatile int pongsReceived=0;
    private volatile String ipIsp="";
    //telemetry runs after the test proper; a stuck server must not wedge the worker
    private static final long TELEMETRY_JOIN_TIMEOUT=10000;
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
    //kept so that abort() can cut the IP lookup short instead of waiting out its timeouts
    private volatile Connection ipConnection=null;
    private void getIP(){
        if(getIPCalled) return; else getIPCalled=true;
        final long start=System.currentTimeMillis();
        Connection c = null;
        try {
            c = new Connection(backend.getServer(), config.getPing_connectTimeout(), config.getPing_soTimeout(), -1, -1);
            ipConnection=c;
        } catch (Throwable t) {
            if (config.getErrorHandlingMode().equals(SpeedtestConfig.ONERROR_FAIL)){
                abort();
                onCriticalFailure(t.toString());
            }
            return;
        }
        GetIP g;
        try {
            g = new GetIP(c, backend.getGetIpURL(), config.getGetIP_isp(), config.getGetIP_distance()) {
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
        } catch (Throwable t) {
            //GetIP throws before its thread starts, so nothing else closes the socket
            try { c.close(); } catch (Throwable t1) {}
            throw t;
        }
        try { g.join(); } catch (InterruptedException ignored) {}
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
                    //truncating to whole milliseconds is intended; the cast keeps it explicit
                    bonusT += (long) (b > 200 ? 200 : b);
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
                    //truncating to whole milliseconds is intended; the cast keeps it explicit
                    bonusT += (long) (b > 200 ? 200 : b);
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
                pongsReceived++;
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
        //wait for the stream's terminal event rather than for whichever pinger
        //thread is current: error recovery replaces that thread, and joining a
        //replaced thread used to report loss for pings that were still being
        //retried. the budget bounds a server that keeps erroring; past it, the
        //unanswered pings count as lost
        long connT=config.getPing_connectTimeout(), soT=config.getPing_soTimeout();
        long deadline=System.currentTimeMillis()+config.getCount_ping()*((connT>0?connT:2000)+(soT>0?soT:5000)+200);
        while(!stopASAP&&!ps.hasEnded()&&System.currentTimeMillis()<deadline) Utils.sleep(100);
        ps.stopASAP();
        ps.join();
        if(stopASAP) return;
        log.l("Ping: "+ ping+" "+jitter+ " (took "+(System.currentTimeMillis()-start)+"ms)");
        onPingJitterUpdate(ping,jitter,1);
        //approximate packet loss from HTTP pings that never returned
        int expected=config.getCount_ping();
        if(expected>0){
            loss=pongsReceived>=expected?0:100.0*(expected-pongsReceived)/expected;
            onLossUpdate(loss);
        }
    }

    private void sendTelemetry(){
        if(telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) return;
        //an aborted test transmits nothing, regardless of level: results are
        //only submitted for tests that ran to completion
        if(stopASAP) return;
        //the tested server may run its own results backend (this is what the web client uses);
        //try it first, then fall back to the centrally configured endpoint
        String base=testServerTelemetryBase();
        String[] localId=new String[1];
        boolean localDelivered=submitTelemetry(backend.getServer(),base.isEmpty()?"results/telemetry.php":base+"/results/telemetry.php",localId);
        if(localId[0]!=null){
            onTestIDReceived(localId[0],shareUrlTemplate(backend.getServer(),base.isEmpty()?"results/?id=%s":base+"/results/?id=%s"));
            return;
        }
        //a server that accepted the POST but returned no usable id may still have
        //stored the run; do not record it a second time at the central server
        if(localDelivered) return;
        String[] centralId=new String[1];
        submitTelemetry(telemetryConfig.getServer(),telemetryConfig.getPath(),centralId);
        if(centralId[0]!=null){
            onTestIDReceived(centralId[0],shareUrlTemplate(telemetryConfig.getServer(),telemetryConfig.getShareURL()));
        }
    }

    //endpoints usually live in <base>/backend/, the results backend in <base>/results/
    private String testServerTelemetryBase(){
        String pingURL=backend.getPingURL()==null?"":backend.getPingURL();
        int slash=pingURL.lastIndexOf('/');
        String dir=slash==-1?"":pingURL.substring(0,slash);
        if(dir.endsWith("backend")) dir=dir.substring(0,dir.length()-"backend".length());
        //an absolute pingURL must yield an absolute base, so the telemetry path
        //is not resolved against the server URL's own path a second time
        boolean absolute=dir.startsWith("/");
        while(dir.startsWith("/")) dir=dir.substring(1);
        while(dir.endsWith("/")) dir=dir.substring(0,dir.length()-1);
        return absolute&&!dir.isEmpty()?"/"+dir:dir;
    }

    //returns true when the server answered the POST with a 2xx, even if no share
    //id could be parsed from the response; the id, if any, is left in idOut[0]
    private boolean submitTelemetry(String server, String path, String[] idOut){
        if(server==null||server.isEmpty()||path==null||path.isEmpty()) return false;
        try{
            Connection c=new Connection(server,config.getPing_connectTimeout(),config.getPing_soTimeout(),-1,-1);
            final boolean[] delivered=new boolean[1];
            Telemetry t=new Telemetry(c,path,telemetryConfig.getTelemetryLevel(),ipIsp,config.getTelemetry_extra(),dl==-1?"":String.format(Locale.ENGLISH,"%.2f",dl),ul==-1?"":String.format(Locale.ENGLISH,"%.2f",ul),ping==-1?"":String.format(Locale.ENGLISH,"%.2f",ping),jitter==-1?"":String.format(Locale.ENGLISH,"%.2f",jitter),log.getLog()) {
                @Override
                public void onDataReceived(String data) {
                    delivered[0]=true;
                    if(data!=null&&data.startsWith("id")){
                        String[] parts=data.split(" ");
                        if(parts.length>1) idOut[0]=parts[1];
                    }
                }

                @Override
                public void onError(String err) {
                    System.err.println("Telemetry error ("+server+"): "+err);
                }
            };
            t.join(TELEMETRY_JOIN_TIMEOUT);
            //if the join timed out, closing the connection unblocks the thread
            try{c.close();}catch (Throwable t1){}
            return delivered[0];
        }catch (Throwable t){
            System.err.println("Failed to send telemetry to "+server+": "+t);
            return false;
        }
    }

    private String shareUrlTemplate(String serverBase, String sharePath){
        if(serverBase==null||serverBase.isEmpty()||sharePath==null||sharePath.isEmpty()) return null;
        String server=serverBase;
        if(!server.endsWith("/")) server=server+"/";
        String path=sharePath;
        while(path.startsWith("/")) path=path.substring(1);
        if(server.startsWith("//")) server="https:"+server;
        return server+path;
    }

    public void abort(){
        if(stopASAP) return;
        log.l("Manually aborted");
        stopASAP=true;
        Connection ic=ipConnection;
        if(ic!=null){ try{ic.close();}catch(Throwable t){} }
    }

    public abstract void onDownloadUpdate(double dl, double progress);
    public abstract void onUploadUpdate(double ul, double progress);
    public abstract void onPingJitterUpdate(double ping, double jitter, double progress);
    public abstract void onLossUpdate(double loss);
    public abstract void onIPInfoUpdate(String ipInfo);
    public abstract void onTestIDReceived(String id, String shareURLTemplate);
    public abstract void onEnd();

    public abstract void onCriticalFailure(String err);

}
