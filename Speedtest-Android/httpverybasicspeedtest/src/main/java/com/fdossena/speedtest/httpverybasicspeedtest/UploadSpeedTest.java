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
import java.net.Socket;

import javax.net.SocketFactory;

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

    class Uploader extends SocketTestRunnable
    {
        public Uploader(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException
        {
            super(log, host, port, path, socket);
        }
        public void run()
        {
            testUploadIntern(socket, host, port, path);
        }
    }
    SocketTestRunnable getSocketTestRunnable(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException
    {
         return new Uploader(log, host, port, path, socket);
    }

    int n = 3;
    int lenData = 2*1048576;

    private void testUploadIntern(Socket socket, String host, int port, String path)
    {
        try
        {
            byte[] data = dataField;
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            for (;;)
            {
                HTTPHelper.putPostHeadersInStream(out, host, path, data.length * n);
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
                HTTPHelper.HTTPLineReader lineReader = new HTTPHelper.HTTPLineReader(in);
                for (;;) // reads and ignores the server answer, which consists of http headers
                {
                    String lin = lineReader.readLine();
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
