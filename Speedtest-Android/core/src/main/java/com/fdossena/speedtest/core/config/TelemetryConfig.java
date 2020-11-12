package com.fdossena.speedtest.core.config;

import org.json.JSONException;
import org.json.JSONObject;

public class TelemetryConfig {
    private String telemetryLevel=LEVEL_DISABLED, server=null, path=null, shareURL=null;
    public static final String LEVEL_DISABLED="disabled", LEVEL_BASIC="basic", LEVEL_FULL="full";

    private void check(){
        if(!(telemetryLevel.equals(LEVEL_DISABLED)||telemetryLevel.equals(LEVEL_BASIC)||telemetryLevel.equals(LEVEL_FULL))) throw new IllegalArgumentException("Telemetry level must be disabled, basic or full");
    }

    public TelemetryConfig(){}

    public TelemetryConfig(String telemetryLevel, String server, String path, String shareURL){
        this.telemetryLevel=telemetryLevel;
        this.server=server;
        this.path=path;
        this.shareURL=shareURL;
        check();
    }

    public TelemetryConfig(JSONObject json){
        try{
            if(json.has("telemetryLevel")) telemetryLevel=json.getString("telemetryLevel");
            if(json.has("server")) server=json.getString("server");
            if(json.has("path")) path=json.getString("path");
            if(json.has("shareURL")) shareURL=json.getString("shareURL");
            check();
        }catch(JSONException t){
            throw new IllegalArgumentException("Invalid JSON ("+t.toString()+")");
        }
    }

    public String getTelemetryLevel() {
        return telemetryLevel;
    }

    public String getServer() {
        return server;
    }

    public String getPath() {
        return path;
    }

    public String getShareURL() {
        return shareURL;
    }

    public TelemetryConfig clone(){
        return new TelemetryConfig(telemetryLevel,server,path,shareURL);
    }
}
