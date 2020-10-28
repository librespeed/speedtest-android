package com.fdossena.speedtest.core.serverSelector;

import org.json.JSONException;
import org.json.JSONObject;

public class TestPoint {
    private final String name, server, dlURL, ulURL, pingURL, getIpURL;
    protected float ping=-1;

    public TestPoint(String name, String server, String dlURL, String ulURL, String pingURL, String getIpURL){
        this.name=name;
        this.server=server;
        this.dlURL=dlURL;
        this.ulURL=ulURL;
        this.pingURL=pingURL;
        this.getIpURL=getIpURL;
    }

    public TestPoint(JSONObject json){
        try {
            name = json.getString("name");
            if (name == null) throw new IllegalArgumentException("Missing name field");
            server = json.getString("server");
            if (server == null) throw new IllegalArgumentException("Missing server field");
            dlURL = json.getString("dlURL");
            if (dlURL == null) throw new IllegalArgumentException("Missing dlURL field");
            ulURL = json.getString("ulURL");
            if (ulURL == null) throw new IllegalArgumentException("Missing ulURL field");
            pingURL = json.getString("pingURL");
            if (pingURL == null) throw new IllegalArgumentException("Missing pingURL field");
            getIpURL = json.getString("getIpURL");
            if (getIpURL == null) throw new IllegalArgumentException("Missing getIpURL field");
        }catch (JSONException t){
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public String getDlURL() {
        return dlURL;
    }

    public String getUlURL() {
        return ulURL;
    }

    public String getPingURL() {
        return pingURL;
    }

    public String getGetIpURL() {
        return getIpURL;
    }

    public float getPing() {
        return ping;
    }
}
