package com.fdossena.speedtest.httpverybasicspeedtest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

public class TestPoint
{
    private final String name;
    private final String server; // include the port, if needed, after a ":"
    private final String downloadPath;
    private final String uploadPath;
    private final String pingPath;
    private final String getIPPath;

    protected float ping=-1;

    public TestPoint(String name, String server, String downloadPath, String uploadPath, String pingPath, String getIPPath)
    {
        this.name=name;
        this.server=server;
        this.downloadPath = downloadPath;
        this.uploadPath = uploadPath;
        this.pingPath = pingPath;
        this.getIPPath = getIPPath;
    }

    public int getPort()
    {
        try
        {
            URL u=new URL(server);
            return u.getPort();
        }
        catch(Throwable t)
        {
            throw new IllegalArgumentException("Malformed URL (HTTP)");
        }
    }
    public String getHost()
    {
        try
        {
            URL u=new URL(server);
            return u.getHost();
        }
        catch(Throwable t)
        {
            throw new IllegalArgumentException("Malformed URL (HTTP)");
        }
    }

    public TestPoint(JSONObject json){
        try {
            name = json.getString("name");
            if (name == null) throw new IllegalArgumentException("Missing name field");
            server = json.getString("server");
            if (server == null) throw new IllegalArgumentException("Missing server field");
            downloadPath = json.getString("dlURL");
            if (downloadPath == null) throw new IllegalArgumentException("Missing dlURL field");
            uploadPath = json.getString("ulURL");
            if (uploadPath == null) throw new IllegalArgumentException("Missing ulURL field");
            pingPath = json.getString("pingURL");
            if (pingPath == null) throw new IllegalArgumentException("Missing pingURL field");
            getIPPath = json.getString("getIpURL");
            if (getIPPath == null) throw new IllegalArgumentException("Missing getIpURL field");
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

    public String getDownloadPath() {
        return downloadPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public String getPingPath() {
        return pingPath;
    }

    public String getGetIPPath() {
        return getIPPath;
    }

    public float getPing() {
        return ping;
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put("name", name);
            json.put("server", server);
            json.put("dlURL", downloadPath);
            json.put("ulURL", uploadPath);
            json.put("pingURL", pingPath);
            json.put("getIpURL", getIPPath);
            return json;
        }
        catch (JSONException e)
        {
            return json;
        }
    }
}
