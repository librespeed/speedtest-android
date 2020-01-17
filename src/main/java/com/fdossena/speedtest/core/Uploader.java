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

import java.io.OutputStream;
import java.util.Random;

import com.fdossena.speedtest.core.Connection;

abstract class Uploader extends Thread{
    private Connection c;
    private String path;
    private boolean stopASAP=false, resetASAP=false;
    private long totUploaded=0;
    private byte[] garbage;

    Uploader(Connection c, String path, int ckSize){
        this.c=c;
        this.path=path;
        garbage=new byte[ckSize*1048576];
        Random r=new Random(System.nanoTime());
        r.nextBytes(garbage);
        start();
    }

    private static final int BUFFER_SIZE=16384;
    public void run(){
        try{
            String s=path;
            long lastProgressEvent=System.currentTimeMillis();
            OutputStream out=c.getOutputStream();
            byte[] buf=new byte[BUFFER_SIZE];
            for(;;){
                if(stopASAP) break;
                c.POST(s,true,"application/octet-stream",garbage.length);
                for(int offset=0;offset<garbage.length;offset+=BUFFER_SIZE){
                    if(stopASAP) break;
                    int l=(offset+BUFFER_SIZE>=garbage.length)?(garbage.length-offset):BUFFER_SIZE;
                    out.write(garbage,offset,l);
                    if(stopASAP) break;
                    if(resetASAP){
                        totUploaded=0;
                        resetASAP=false;
                    }
                    totUploaded+=l;
                    if(System.currentTimeMillis()-lastProgressEvent>200){
                        lastProgressEvent=System.currentTimeMillis();
                        onProgress(totUploaded);
                    }
                }
                if(stopASAP) break;
                while(!c.readLineUnbuffered().trim().isEmpty());
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

    abstract void onProgress(long uploaded);
    abstract void onError(String err);

    void resetUploadCounter(){
        resetASAP=true;
    }

    long getUploaded() {
        return resetASAP?0:totUploaded;
    }
}
