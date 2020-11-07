/**
 *
 * This example is released under the MIT license, so it can be copied, modified and used in
 * proprietary software.
 *
 * Note the the speed test core library is released under LGLP. PAY ATTENTION.
 *
 */
package com.fdossena.speedtest.httpverybasicspeedtest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;

import javax.net.SocketFactory;

public class VeryBasicSpeedTest
{
    byte[] data;
    public interface SpeedTestLog
    {
        void speedTestlog(String s);

    }
    public  VeryBasicSpeedTest()
    {
        data = new byte[10 * 1024 * 1024];
        int v = 13;
        data[0] = (byte)(v % 256);
        for (int t=0; t<data.length; t++)
        {
            v = v * 17 + t * 31 + 7;
            data[t] = (byte)(v % 256);
        }
    }
    public void test(SpeedTestLog log)
    {
        try
        {
            String httpUrl = "http://192.168.0.102:8080/backend/empty.php";
            URL url = new URL(httpUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            //ps.print("Host: "+host+"\r\n");
            //ps.print("User-Agent: "+USER_AGENT+"\r\n");
            conn.setRequestProperty("Connection", "close"); //keep-alive
            //ps.print("Accept-Encoding: identity\r\n");
            //if(LOCALE!=null) ps.print("Accept-Language: "+LOCALE+"\r\n");
            //if(contentType!=null) ps.print("Content-Type: "+contentType+"\r\n");
            //if(contentLength>=0) ps.print("Content-Length: "+contentLength+"\r\n");

            conn.setReadTimeout(1000 * 1000);
            conn.setConnectTimeout(1000 * 1000);

            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestMethod("POST");

            long time1 = System.currentTimeMillis();
            OutputStream os = conn.getOutputStream();
            int n = 100;
            for (int t=0; t<n; t++)
            {
                os.write(data);
                os.flush();
            }
            os.close();

            final int responseCode = conn.getResponseCode();
            long time2 = System.currentTimeMillis();

            long delta = time2 - time1;
            double speed = data.length * n * 1000.0 / delta;

            log.speedTestlog(""+responseCode+" "+delta+" "+speed);
        }
        catch (SocketException se)
        {
            se.printStackTrace();
            log.speedTestlog(se.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.speedTestlog(e.getMessage());
        }
    }
}
