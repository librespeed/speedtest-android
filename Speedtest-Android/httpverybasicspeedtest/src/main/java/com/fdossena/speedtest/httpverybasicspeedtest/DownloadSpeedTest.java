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
import java.io.OutputStream;
import java.net.Socket;

public class DownloadSpeedTest extends DownUpSpeedTest
{

    public DownloadSpeedTest()
    {
        super();
    }

    class Downloader extends SocketTestRunnable
    {
        public Downloader(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException
        {
            super(log, host, port, path, socket);
        }
        public void run()
        {
            testDownloadIntern(socket, host, port, path);
        }
    }
    SocketTestRunnable getSocketTestRunnable(SpeedTestListener log, String host, int port, String path, Socket socket) throws IOException
    {
         return new Downloader(log, host, port, path, socket);
    }

    int n = 100;
    int sizeChunks = n*1048576;

    private void testDownloadIntern(Socket socket, String host, int port, String path)
    {
        try
        {
            int bufSize = 64 * 1024;
            byte[] buf=new byte[bufSize];
            path+= "?ckSize="+n; // This is to tell the server the number of chunks it should send. Each chunk has always 1048576 bytes
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            int newRequestThreshold=sizeChunks/4;
            long bytesLeft=0;
            for(;;)
            {
                if(bytesLeft<=newRequestThreshold)  // send a new request before having read all bytes from the previous, in order to keep the flow
                {
                    HTTPHelper.putGetHeadersInStream(out, host, path);
                    bytesLeft+=sizeChunks;
                }
                int numRead=in.read(buf);
                bytesLeft-=numRead;
                if (moved(numRead))
                {
                    socket.close();
                    return;
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
