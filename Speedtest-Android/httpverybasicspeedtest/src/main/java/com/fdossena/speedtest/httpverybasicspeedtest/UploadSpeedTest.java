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
import java.net.Socket;

public class UploadSpeedTest extends DownUpSpeedTest
{
    byte[] dataField;


    public UploadSpeedTest()
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

    class Uploader extends SocketHolder
    {
        public Uploader(SpeedTestListener log, String host, int port, String path) throws IOException
        {
            super(log, host, port, path);
        }
        public void run()
        {
            testUploadIntern(log, socket, host, port, path);
        }
    }
    SocketHolder getSocketTestRunnable(SpeedTestListener log, String host, int port, String path) throws IOException
    {
         return new Uploader(log, host, port, path);
    }

    int n = 3;
    int lenData = 2*1048576;

    private void testUploadIntern(SpeedTestListener log, Socket socket, String host, int port, String path)
    {
        try
        {
            byte[] data = dataField;
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            for (;;)
            {
                PrintStream ps = new PrintStream(out, false, "utf-8");
                ps.print("POST "+path+" HTTP/1.1\r\n"); // /backend/empty.php
                ps.print("Host: "+host+"\r\n"); // 192.168.0.102:8080
                ps.print("Connection: keep-alive\r\n");
                ps.print("Accept-Encoding: identity\r\n");
                ps.print("Content-Type: application/octet-stream\r\n");
                ps.print("Content-Length: " + data.length * n + "\r\n");
                ps.print("\r\n");
                ps.flush();
                int bufSize = 64 * 1024;
                for (int tt = 0; tt < n; tt++)
                {
                    for (int i=0; i < lenData / bufSize; i++)
                    {
                        out.write(data, i * bufSize, bufSize);
                        if (moved(bufSize))
                            return;
                    }
                }
                InputStreamReader isr = new InputStreamReader(in, "utf-8");

                for (;;) // reads and ignores the server answer, which consists of http headers
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
                    if (lin == null || lin.trim().isEmpty())
                        break;
                }
            }
        }
        catch (Throwable th)
        {
            setError(th);
        }
        finally
        {
            try { socket.close(); } catch(Throwable th) {}
        }
    }
}
