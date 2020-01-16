package com.fdossena.speedtest.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.fdossena.speedtest.core.config.SpeedtestConfig;

abstract class ServerSelector {
    private ArrayList<TestPoint> servers=new ArrayList<>();
    private static final int PARALLELISM=6;
    private TestPoint selectedTestPoint=null;
    private int state=NOT_STARTED;
    private static final int NOT_STARTED=0, WORKING=1, DONE=2;
    private int timeout;
    private static final int PINGS=3, SLOW_THRESHOLD=500;
    private boolean stopASAP=false;

    ServerSelector(TestPoint[] servers, int timeout){
        addTestPoints(servers);
        this.timeout=timeout;
    }
    void addTestPoint(TestPoint t){
        if(state!=NOT_STARTED) throw new IllegalStateException("Cannot add test points at this time");
        if(t==null) return;
        servers.add(t);
    }
    void addTestPoint(JSONObject t){
        if(state!=NOT_STARTED) throw new IllegalStateException("Cannot add test points at this time");
        servers.add(new TestPoint(t));
    }
    void addTestPoints(JSONArray a){
        if(state!=NOT_STARTED) throw new IllegalStateException("Cannot add test points at this time");
        for(int i=0;i<a.length();i++){
            try {
                servers.add(new TestPoint(a.getJSONObject(i)));
            }catch (JSONException e){}
        }
    }
    void addTestPoints(TestPoint[] servers){
        if(state!=NOT_STARTED) throw new IllegalStateException("Cannot add test points at this time");
        for(TestPoint t:servers) addTestPoint(t);
    }

    TestPoint getSelectedTestPoint() {
        if(state!=DONE) throw new IllegalStateException("Test point hasn't been selected yet");
        return selectedTestPoint;
    }

    TestPoint[] getTestPoints(){
        return servers.toArray(new TestPoint[0]);
    }

    private final Object mutex=new Object();
    private int tpPointer=0;
    private int activeStreams=0;
    private void next(){
        if(stopASAP) return;
        synchronized (mutex) {
            if (tpPointer >= servers.size()){
                if(activeStreams<=0){
                    selectedTestPoint=null;
                    for(TestPoint t:servers){
                        if(t.ping==-1) continue;
                        if(selectedTestPoint==null||t.ping<selectedTestPoint.ping) selectedTestPoint=t;
                    }
                    if(state==DONE) return;
                    state=DONE;
                    onServerSelected(selectedTestPoint);
                }
                return;
            }
            final TestPoint tp=servers.get(tpPointer++);
            PingStream ps=new PingStream(tp.getServer(),tp.getPingURL(),PINGS, SpeedtestConfig.ONERROR_FAIL,timeout,timeout,-1,-1,null) {
                @Override
                void onError(String err) {
                    tp.ping=-1;
                    synchronized (mutex){activeStreams--;}
                    next();
                }

                @Override
                boolean onPong(long ns) {
                    float p=ns/1000000f;
                    if(tp.ping==-1||p<tp.ping) tp.ping=p;
                    if(stopASAP) return false;
                    return p<SLOW_THRESHOLD;
                }

                @Override
                void onDone() {
                    synchronized (mutex){activeStreams--;}
                    next();
                }
            };
            activeStreams++;
        }
    }

    void start(){
        if(state!=NOT_STARTED) throw new IllegalStateException("Already started");
        state=WORKING;
        for(TestPoint t:servers) t.ping=-1;
        for(int i=0;i<PARALLELISM;i++) next();
    }

    void stopASAP(){
        stopASAP=true;
    }

    abstract void onServerSelected(TestPoint server);
}
