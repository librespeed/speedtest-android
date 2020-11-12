package com.fdossena.speedtest.httpverybasicspeedtest;

import org.json.JSONException;
import org.json.JSONObject;

class SpeedTestResult
{
    double ping;
    double jitter;
    double downloadSpeed;
    double uploadSpeed;
    String ipInfo;
    Throwable error;
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
            if (error!=null)
                res.put("error",error.getMessage());
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
}
