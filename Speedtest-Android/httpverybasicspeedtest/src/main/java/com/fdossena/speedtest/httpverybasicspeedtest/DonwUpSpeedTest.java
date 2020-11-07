/**
 *
 * This example is released under the MIT license, so it can be copied, modified and used in
 * proprietary software.
 *
 * Note the the speed test core library is released under LGLP. PAY ATTENTION.
 *
 */
package com.fdossena.speedtest.httpverybasicspeedtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.SocketFactory;

public class DonwUpSpeedTest
{
    byte[] dataField;

    public interface SpeedTestLog
    {
        void speedTestlog(String s);

    }

    long numBytesMoved;
    boolean stop;
    boolean running;
    long timeIni;
    long timeEnd;
    String error;

    public DonwUpSpeedTest()
    {

        dataField = new byte[lenData]; //
        int v = 13;
        dataField[0] = (byte) (v % 256);
        for (int t = 0; t < dataField.length; t++)
        {
            v = v * 17 + t * 31 + 7;
            dataField[t] = (byte) (v % 256);
        }

    }
    synchronized boolean shouldStop()
    {
        return stop;
    }
    synchronized boolean moved(long numBytes)
    {
        numBytesMoved += numBytes;
        return stop;
    }

    class Uploader implements Runnable
    {
        SpeedTestLog log;
        Socket socket;
        Uploader(SpeedTestLog log) throws IOException
        {
            this.log = log;
            SocketFactory factory = SocketFactory.getDefault();
            socket = factory.createSocket();
            socket.connect(new InetSocketAddress("170.238.84.8", 8080));
        }
        public void run()
        {
            testUploadIntern(log, socket);
        }
    }

    class DownUpSpeedResult
    {
        double speed;
        String error;
        public DownUpSpeedResult(double speed, String error)
        {
            this.speed = speed;
            this.error = error;
        }
        public String toString()
        {
            if (error!=null)
                return "  Error "+error;
            else
                return "Speed "+speed;
        }
    }

    public void test(final SpeedTestLog log)
    {
        if (running)
            return;
        stop = false;
        running =  true;
        try
        {
            Thread[] th = new Thread[nt];
            for (int t = 0; t < nt; t++)
            {
                th[t] = new Thread(new Uploader(log));
            }
            timeIni = System.currentTimeMillis();
            for (int t = 0; t < nt; t++)
                th[t].start();
            try
            {
                Thread.sleep(10000);
            }
            catch (InterruptedException e)
            {
            }
            synchronized (this)
            {
                numBytesMoved = 0;  // to ignore initial speed
            }
            timeIni = System.currentTimeMillis(); // to ignore initial speed
            try
            {
                Thread.sleep(30000);
            }
            catch (InterruptedException e)
            {
            }
            synchronized (this)
            {
                stop = true;
            }
            for (int t = 0; t < nt; t++)
            {
                try
                {
                    th[t].join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            timeEnd = System.currentTimeMillis();
            DownUpSpeedResult res = getResult();

            log.speedTestlog(res.toString());
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            log.speedTestlog("Critical Error");
        }
        finally
        {
            running =  false;
        }
    }

    public synchronized DownUpSpeedResult getResult()
    {
        long time2 = running ? timeEnd : System.currentTimeMillis();
        long delta = (time2 - timeIni) / 1000;
        double speed = numBytesMoved * 8.0 / delta / 1024 / 1024;
        return new DownUpSpeedResult(speed, error);
    }

    int N = 25;
    int n = 3; // * 1024;
    int nt = 10;
    int lenData = 2*1048576;

    private void testUploadIntern(SpeedTestLog log, Socket socket)
    {
        try
        {
            byte[] data = dataField;
            log.speedTestlog("Start 6");
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            long time1 = System.currentTimeMillis();
            for (int t = 0; t < N; t++)
            {
                PrintStream ps = new PrintStream(out, false, "utf-8");
                ps.print("POST /backend/empty.php HTTP/1.1\r\n");
                ps.print("Host: 192.168.0.102:8080\r\n");
                //ps.print("User-Agent: "+USER_AGENT+"\r\n");
                ps.print("Connection: keep-alive\r\n");
                ps.print("Accept-Encoding: identity\r\n");
                //if(LOCALE!=null) ps.print("Accept-Language: "+LOCALE+"\r\n");
                ps.print("Content-Type: application/octet-stream\r\n");
                ps.print("Content-Length: " + data.length * n + "\r\n");
                ps.print("\r\n");
                ps.flush();
                int chunk = 64 * 1024;
                for (int tt = 0; tt < n; tt++)
                {
                    for (int i=0; i < lenData / chunk; i++)
                    {
                        out.write(data, i * chunk, chunk);
                        if (moved(chunk))
                        {
                            socket.close();
                            return;
                        }
                    }
                }
                InputStreamReader isr = new InputStreamReader(in, "utf-8");

                for (; ; )
                {
                    StringBuilder sb = new StringBuilder();
                    while (true)
                    {
                        int c = isr.read();
                        if (c == -1) break;
                        sb.append((char) c);
                        if (c == '\n') break;
                    }
                    String lin = sb.toString();
                    System.out.println(lin);
                    if (lin == null || lin.trim().isEmpty())
                        break;
                }
                log.speedTestlog("Sent chunk");
            }
            long time2 = System.currentTimeMillis();
            long delta = time2 - time1;
            double speed = (long)data.length * 8 * N * n * 1000.0 / delta;

            log.speedTestlog("" + delta + " " + speed);

            out.close();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
            log.speedTestlog(e.getMessage());

        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            log.speedTestlog(e.getMessage());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            log.speedTestlog(e.getMessage());
        }
    }
}
