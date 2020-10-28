package com.fdossena.speedtest.core.config;

import org.json.JSONException;
import org.json.JSONObject;

public class SpeedtestConfig {
    private int dl_ckSize=100, ul_ckSize=20;
    private int dl_parallelStreams=3, ul_parallelStreams=3;
    private int dl_streamDelay=300, ul_streamDelay=300;
    private double dl_graceTime=1.5, ul_graceTime=1.5;
    private int dl_connectTimeout=5000, dl_soTimeout=10000, ul_connectTimeout=5000, ul_soTimeout=10000, ping_connectTimeout=2000, ping_soTimeout=5000;
    private int dl_recvBuffer=-1, dl_sendBuffer=-1, ul_recvBuffer=-1, ul_sendBuffer=16384, ping_recvBuffer=-1, ping_sendBuffer=-1;
    private String errorHandlingMode=ONERROR_ATTEMPT_RESTART;
    public static final String ONERROR_FAIL="fail", ONERROR_ATTEMPT_RESTART="attempt-restart", ONERROR_MUST_RESTART="must-restart";
    private int time_dl_max=15, time_ul_max=15;
    private boolean time_auto=true;
    private int count_ping=10;
    private String telemetry_extra="";
    private double overheadCompensationFactor=1.06;
    private boolean getIP_isp=true;
    private String getIP_distance=DISTANCE_KM;
    public static final String DISTANCE_NO="no", DISTANCE_MILES="mi", DISTANCE_KM="km";
    private boolean useMebibits=false;
    private String test_order="IP_D_U";

    private void check(){
        if(dl_ckSize<1) throw new IllegalArgumentException("dl_ckSize must be at least 1");
        if(ul_ckSize<1) throw new IllegalArgumentException("ul_ckSize must be at least 1");
        if(dl_parallelStreams<1) throw new IllegalArgumentException("dl_parallelStreams must be at least 1");
        if(ul_parallelStreams<1) throw new IllegalArgumentException("ul_parallelStreams must be at least 1");
        if(dl_streamDelay<0) throw new IllegalArgumentException("dl_streamDelay must be at least 0");
        if(ul_streamDelay<0) throw new IllegalArgumentException("ul_streamDelay must be at least 0");
        if(dl_graceTime<0) throw new IllegalArgumentException("dl_graceTime must be at least 0");
        if(ul_graceTime<0) throw new IllegalArgumentException("ul_graceTime must be at least 0");
        if(!(errorHandlingMode.equals(ONERROR_FAIL)||errorHandlingMode.equals(ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(ONERROR_MUST_RESTART))) throw new IllegalArgumentException("errorHandlingMode must be fail, attempt-restart, or must-restart");
        if(time_dl_max<1) throw new IllegalArgumentException("time_dl_max must be at least 1");
        if(time_ul_max<1) throw new IllegalArgumentException("time_ul_max must be at least 1");
        if(count_ping<1) throw new IllegalArgumentException("count_ping must be at least 1");
        if(overheadCompensationFactor<1) throw new IllegalArgumentException("overheadCompensationFactor must be at least 1");
        if(!(getIP_distance.equals(DISTANCE_NO)||getIP_distance.equals(DISTANCE_KM)||getIP_distance.equals(DISTANCE_MILES))) throw new IllegalArgumentException("getIP_distance must be no, km or miles");
        for(char c:test_order.toCharArray()){
            if(!(c=='I'||c=='P'||c=='D'||c=='U'||c=='_')) throw new IllegalArgumentException("test_order can only contain characters I, P, D, U, _");
        }
    }

    public SpeedtestConfig(){
        check();
    }

    public SpeedtestConfig(int dl_ckSize, int ul_ckSize, int dl_parallelStreams, int ul_parallelStreams, int dl_streamDelay, int ul_streamDelay, double dl_graceTime, double ul_graceTime, int dl_connectTimeout, int dl_soTimeout, int ul_connectTimeout, int ul_soTimeout, int ping_connectTimeout, int ping_soTimeout, int dl_recvBuffer, int dl_sendBuffer, int ul_recvBuffer, int ul_sendBuffer, int ping_recvBuffer, int ping_sendBuffer, String errorHandlingMode, int time_dl_max, int time_ul_max, boolean time_auto, int count_ping, String telemetry_extra, double overheadCompensationFactor, boolean getIP_isp, String getIP_distance, boolean useMebibits, String test_order) {
        this.dl_ckSize = dl_ckSize;
        this.ul_ckSize = ul_ckSize;
        this.dl_parallelStreams = dl_parallelStreams;
        this.ul_parallelStreams = ul_parallelStreams;
        this.dl_streamDelay = dl_streamDelay;
        this.ul_streamDelay = ul_streamDelay;
        this.dl_graceTime = dl_graceTime;
        this.ul_graceTime = ul_graceTime;
        this.dl_connectTimeout = dl_connectTimeout;
        this.dl_soTimeout = dl_soTimeout;
        this.ul_connectTimeout = ul_connectTimeout;
        this.ul_soTimeout = ul_soTimeout;
        this.ping_connectTimeout = ping_connectTimeout;
        this.ping_soTimeout = ping_soTimeout;
        this.dl_recvBuffer = dl_recvBuffer;
        this.dl_sendBuffer = dl_sendBuffer;
        this.ul_recvBuffer = ul_recvBuffer;
        this.ul_sendBuffer = ul_sendBuffer;
        this.ping_recvBuffer = ping_recvBuffer;
        this.ping_sendBuffer = ping_sendBuffer;
        this.errorHandlingMode = errorHandlingMode;
        this.time_dl_max = time_dl_max;
        this.time_ul_max = time_ul_max;
        this.time_auto = time_auto;
        this.count_ping = count_ping;
        this.telemetry_extra = telemetry_extra;
        this.overheadCompensationFactor = overheadCompensationFactor;
        this.getIP_isp = getIP_isp;
        this.getIP_distance = getIP_distance;
        this.useMebibits = useMebibits;
        this.test_order = test_order;
        check();
    }

    public SpeedtestConfig(JSONObject json){
        try {
            if (json.has("dl_ckSize")) this.dl_ckSize = json.getInt("dl_ckSize");
            if (json.has("ul_ckSize")) this.ul_ckSize = json.getInt("ul_ckSize");
            if (json.has("dl_parallelStreams"))
                this.dl_parallelStreams = json.getInt("dl_parallelStreams");
            if (json.has("ul_parallelStreams"))
                this.ul_parallelStreams = json.getInt("ul_parallelStreams");
            if (json.has("dl_streamDelay")) this.dl_streamDelay = json.getInt("dl_streamDelay");
            if (json.has("ul_streamDelay")) this.ul_streamDelay = json.getInt("ul_streamDelay");
            if (json.has("dl_graceTime")) this.dl_graceTime = json.getDouble("dl_graceTime");
            if (json.has("ul_graceTime")) this.ul_graceTime = json.getDouble("ul_graceTime");
            if (json.has("dl_connectTimeout"))
                this.dl_connectTimeout = json.getInt("dl_connectTimeout");
            if (json.has("ul_connectTimeout"))
                this.ul_connectTimeout = json.getInt("ul_connectTimeout");
            if (json.has("ping_connectTimeout"))
                this.ping_connectTimeout = json.getInt("ping_connectTimeout");
            if (json.has("dl_soTimeout")) this.dl_soTimeout = json.getInt("dl_soTimeout");
            if (json.has("ul_soTimeout")) this.ul_soTimeout = json.getInt("ul_soTimeout");
            if (json.has("ping_soTimeout")) this.ping_soTimeout = json.getInt("ping_soTimeout");
            if (json.has("dl_recvBuffer")) this.dl_recvBuffer = json.getInt("dl_recvBuffer");
            if (json.has("ul_recvBuffer")) this.ul_recvBuffer = json.getInt("ul_recvBuffer");
            if (json.has("ping_recvBuffer")) this.ping_recvBuffer = json.getInt("ping_recvBuffer");
            if (json.has("dl_sendBuffer")) this.dl_sendBuffer = json.getInt("dl_sendBuffer");
            if (json.has("ul_sendBuffer")) this.ul_sendBuffer = json.getInt("ul_sendBuffer");
            if (json.has("ping_sendBuffer")) this.ping_sendBuffer = json.getInt("ping_sendBuffer");
            if (json.has("errorHandlingMode"))
                this.errorHandlingMode = json.getString("errorHandlingMode");
            if (json.has("time_dl_max")) this.time_dl_max = json.getInt("time_dl_max");
            if (json.has("time_ul_max")) this.time_ul_max = json.getInt("time_ul_max");
            if (json.has("count_ping")) this.count_ping = json.getInt("count_ping");
            if (json.has("telemetry_extra"))
                this.telemetry_extra = json.getString("telemetry_extra");
            if (json.has("overheadCompensationFactor"))
                this.overheadCompensationFactor = json.getDouble("overheadCompensationFactor");
            if (json.has("getIP_isp")) this.getIP_isp = json.getBoolean("getIP_isp");
            if (json.has("getIP_distance")) this.getIP_distance = json.getString("getIP_distance");
            if (json.has("test_order")) this.test_order = json.getString("test_order");
            if (json.has("useMebibits")) this.useMebibits = json.getBoolean("useMebibits");
            check();
        }catch(JSONException t){
            throw new IllegalArgumentException("Invalid JSON ("+t.toString()+")");
        }
    }

    public int getDl_ckSize() {
        return dl_ckSize;
    }

    public int getUl_ckSize() {
        return ul_ckSize;
    }

    public int getDl_parallelStreams() {
        return dl_parallelStreams;
    }

    public int getUl_parallelStreams() {
        return ul_parallelStreams;
    }

    public int getDl_streamDelay() {
        return dl_streamDelay;
    }

    public int getUl_streamDelay() {
        return ul_streamDelay;
    }

    public double getDl_graceTime() {
        return dl_graceTime;
    }

    public double getUl_graceTime() {
        return ul_graceTime;
    }

    public int getDl_connectTimeout() {
        return dl_connectTimeout;
    }

    public int getDl_soTimeout() {
        return dl_soTimeout;
    }

    public int getUl_connectTimeout() {
        return ul_connectTimeout;
    }

    public int getUl_soTimeout() {
        return ul_soTimeout;
    }

    public int getPing_connectTimeout() {
        return ping_connectTimeout;
    }

    public int getPing_soTimeout() {
        return ping_soTimeout;
    }

    public int getDl_recvBuffer() {
        return dl_recvBuffer;
    }

    public int getDl_sendBuffer() {
        return dl_sendBuffer;
    }

    public int getUl_recvBuffer() {
        return ul_recvBuffer;
    }

    public int getUl_sendBuffer() {
        return ul_sendBuffer;
    }

    public int getPing_recvBuffer() {
        return ping_recvBuffer;
    }

    public int getPing_sendBuffer() {
        return ping_sendBuffer;
    }

    public String getErrorHandlingMode() {
        return errorHandlingMode;
    }

    public int getTime_dl_max() {
        return time_dl_max;
    }

    public int getTime_ul_max() {
        return time_ul_max;
    }

    public boolean getTime_auto() {
        return time_auto;
    }

    public int getCount_ping() {
        return count_ping;
    }

    public String getTelemetry_extra() {
        return telemetry_extra;
    }

    public double getOverheadCompensationFactor() {
        return overheadCompensationFactor;
    }

    public boolean getGetIP_isp() {
        return getIP_isp;
    }

    public String getGetIP_distance() {
        return getIP_distance;
    }

    public boolean getUseMebibits() {
        return useMebibits;
    }

    public String getTest_order() {
        return test_order;
    }
    
    public void setDl_ckSize(int dl_ckSize) {
        if(dl_ckSize<1) throw new IllegalArgumentException("dl_ckSize must be at least 1");
        this.dl_ckSize = dl_ckSize;
    }

    public void setUl_ckSize(int ul_ckSize) {
        if(ul_ckSize<1) throw new IllegalArgumentException("ul_ckSize must be at least 1");
        this.ul_ckSize = ul_ckSize;
    }

    public void setDl_parallelStreams(int dl_parallelStreams) {
        if(dl_parallelStreams<1) throw new IllegalArgumentException("dl_parallelStreams must be at least 1");
        this.dl_parallelStreams = dl_parallelStreams;
    }

    public void setUl_parallelStreams(int ul_parallelStreams) {
        if(ul_parallelStreams<1) throw new IllegalArgumentException("ul_parallelStreams must be at least 1");
        this.ul_parallelStreams = ul_parallelStreams;
    }

    public void setDl_streamDelay(int dl_streamDelay) {
        if(dl_streamDelay<0) throw new IllegalArgumentException("dl_streamDelay must be at least 0");
        this.dl_streamDelay = dl_streamDelay;
    }

    public void setUl_streamDelay(int ul_streamDelay) {
        if(ul_streamDelay<0) throw new IllegalArgumentException("ul_streamDelay must be at least 0");
        this.ul_streamDelay = ul_streamDelay;
    }

    public void setDl_graceTime(double dl_graceTime) {
        if(dl_graceTime<0) throw new IllegalArgumentException("dl_graceTime must be at least 0");
        this.dl_graceTime = dl_graceTime;
    }

    public void setUl_graceTime(double ul_graceTime) {
        if(ul_graceTime<0) throw new IllegalArgumentException("ul_graceTime must be at least 0");
        this.ul_graceTime = ul_graceTime;
    }

    public void setDl_connectTimeout(int dl_connectTimeout) {
        
        this.dl_connectTimeout = dl_connectTimeout;
    }

    public void setDl_soTimeout(int dl_soTimeout) {
        
        this.dl_soTimeout = dl_soTimeout;
    }

    public void setUl_connectTimeout(int ul_connectTimeout) {
        
        this.ul_connectTimeout = ul_connectTimeout;
    }

    public void setUl_soTimeout(int ul_soTimeout) {
        
        this.ul_soTimeout = ul_soTimeout;
    }

    public void setPing_connectTimeout(int ping_connectTimeout) {
        
        this.ping_connectTimeout = ping_connectTimeout;
    }

    public void setPing_soTimeout(int ping_soTimeout) {
        
        this.ping_soTimeout = ping_soTimeout;
    }

    public void setDl_recvBuffer(int dl_recvBuffer) {
        
        this.dl_recvBuffer = dl_recvBuffer;
    }

    public void setDl_sendBuffer(int dl_sendBuffer) {
        
        this.dl_sendBuffer = dl_sendBuffer;
    }

    public void setUl_recvBuffer(int ul_recvBuffer) {
        
        this.ul_recvBuffer = ul_recvBuffer;
    }

    public void setUl_sendBuffer(int ul_sendBuffer) {
        
        this.ul_sendBuffer = ul_sendBuffer;
    }

    public void setPing_recvBuffer(int ping_recvBuffer) {
        
        this.ping_recvBuffer = ping_recvBuffer;
    }

    public void setPing_sendBuffer(int ping_sendBuffer) {
        
        this.ping_sendBuffer = ping_sendBuffer;
    }

    public void setErrorHandlingMode(String errorHandlingMode) {
        if(!(errorHandlingMode.equals(ONERROR_FAIL)||errorHandlingMode.equals(ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(ONERROR_MUST_RESTART))) throw new IllegalArgumentException("errorHandlingMode must be fail, attempt-restart, or must-restart");
        this.errorHandlingMode = errorHandlingMode;
    }

    public void setTime_dl_max(int time_dl_max) {
        if(time_dl_max<1) throw new IllegalArgumentException("time_dl_max must be at least 1");
        this.time_dl_max = time_dl_max;
    }

    public void setTime_ul_max(int time_ul_max) {
        if(time_ul_max<1) throw new IllegalArgumentException("time_ul_max must be at least 1");
        this.time_ul_max = time_ul_max;
    }

    public void setTime_auto(boolean time_auto) {
        
        this.time_auto = time_auto;
    }

    public void setCount_ping(int count_ping) {
        if(count_ping<1) throw new IllegalArgumentException("count_ping must be at least 1");
        this.count_ping = count_ping;
    }

    public void setTelemetry_extra(String telemetry_extra) {
        
        this.telemetry_extra = telemetry_extra;
    }

    public void setOverheadCompensationFactor(double overheadCompensationFactor) {
        if(overheadCompensationFactor<1) throw new IllegalArgumentException("overheadCompensationFactor must be at least 1");
        this.overheadCompensationFactor = overheadCompensationFactor;
    }

    public void setGetIP_isp(boolean getIP_isp) {
        
        this.getIP_isp = getIP_isp;
    }

    public void setGetIP_distance(String getIP_distance) {
        if(!(getIP_distance.equals(DISTANCE_NO)||getIP_distance.equals(DISTANCE_KM)||getIP_distance.equals(DISTANCE_MILES))) throw new IllegalArgumentException("getIP_distance must be no, km or miles");
        this.getIP_distance = getIP_distance;
    }

    public void setUseMebibits(boolean useMebibits) {
        
        this.useMebibits = useMebibits;
    }

    public void setTest_order(String test_order) {
        for(char c:test_order.toCharArray()){
            if(!(c=='I'||c=='P'||c=='D'||c=='U'||c=='_')) throw new IllegalArgumentException("test_order can only contain characters I, P, D, U, _");
        }
        this.test_order = test_order;
    }

    public SpeedtestConfig clone(){
        return new SpeedtestConfig(dl_ckSize, ul_ckSize, dl_parallelStreams, ul_parallelStreams, dl_streamDelay, ul_streamDelay, dl_graceTime, ul_graceTime, dl_connectTimeout, dl_soTimeout, ul_connectTimeout, ul_soTimeout, ping_connectTimeout, ping_soTimeout, dl_recvBuffer, dl_sendBuffer, ul_recvBuffer, ul_sendBuffer, ping_recvBuffer, ping_sendBuffer, errorHandlingMode, time_dl_max, time_ul_max, time_auto, count_ping, telemetry_extra, overheadCompensationFactor, getIP_isp, getIP_distance, useMebibits, test_order);
    }
}
