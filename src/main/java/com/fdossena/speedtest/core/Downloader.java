/*
 *     This file is part of the LibreSpeed speedtest library.
 *
 *     The LibreSpeed speedtest library is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fdossena.speedtest.core;

import java.io.InputStream;

import com.fdossena.speedtest.core.Connection;
import com.fdossena.speedtest.core.Utils;

abstract class Downloader extends Thread{
    private Connection c;
    private String path;
    private int ckSize;
    private boolean stopASAP=false, resetASAP=false;
    private long totDownloaded=0;

    Downloader(Connection c, String path, int ckSize){
        this.c=c;
        this.path=path;
        this.ckSize=ckSize<1?1:ckSize;
        start();
    }

    private static final int BUFFER_SIZE=16384;
    public void run(){
        try{
            String s=path;
            s+= Utils.url_sep(s)+"ckSize="+ckSize;
            long lastProgressEvent=System.currentTimeMillis();
            long ckBytes=ckSize*1048576, newRequestThreshold=ckBytes/4;
            long bytesLeft=0;
            InputStream in=c.getInputStream();
            byte[] buf=new byte[BUFFER_SIZE];
            for(;;){
                if(stopASAP) break;
                if(bytesLeft<=newRequestThreshold){
                    c.GET(s, true);
                    bytesLeft+=ckBytes;
                }
                if(stopASAP) break;
                int l=in.read(buf);
                if(stopASAP) break;
                bytesLeft-=l;
                if(resetASAP){
                    totDownloaded=0;
                    resetASAP=false;
                }
                totDownloaded+=l;
                if(System.currentTimeMillis()-lastProgressEvent>200){
                    lastProgressEvent=System.currentTimeMillis();
                    onProgress(totDownloaded);
                }
            }
            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    void stopASAP(){
        this.stopASAP=true;
    }

    abstract void onProgress(long downloaded);
    abstract void onError(String err);

    void resetDownloadCounter(){
        resetASAP=true;
    }

    long getDownloaded() {
        return resetASAP?0:totDownloaded;
    }
}