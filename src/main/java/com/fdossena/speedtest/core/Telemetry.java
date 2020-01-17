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

import java.io.PrintStream;
import java.util.HashMap;

import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.Connection;
import com.fdossena.speedtest.core.Utils;

abstract class Telemetry extends Thread{
    private Connection c;
    private String path;
    private String level, ispinfo, extra, dl, ul, ping, jitter, log;

    Telemetry(Connection c, String path, String level, String ispinfo, String extra, String dl, String ul, String ping, String jitter, String log){
        if(level.equals(TelemetryConfig.LEVEL_DISABLED)){
            onDataReceived(null);
            return;
        }
        this.c=c;
        this.path=path;
        this.level=level;
        this.ispinfo=ispinfo;
        this.extra=extra;
        this.dl=dl;
        this.ul=ul;
        this.ping=ping;
        this.jitter=jitter;
        this.log=log;
        start();
    }

    public void run(){
        try{
            String s=path;
            StringBuilder sb=new StringBuilder();
            sb.append("ispinfo=");
            sb.append(Utils.urlEncode(ispinfo));
            sb.append("&dl=");
            sb.append(Utils.urlEncode(dl));
            sb.append("&ul=");
            sb.append(Utils.urlEncode(ul));
            sb.append("&ping=");
            sb.append(Utils.urlEncode(ping));
            sb.append("&jitter=");
            sb.append(Utils.urlEncode(jitter));
            if(level.equals(TelemetryConfig.LEVEL_FULL)) {
                sb.append("&log=");
                sb.append(Utils.urlEncode(log));
            }
            sb.append("&extra=");
            sb.append(Utils.urlEncode(extra));
            c.POST(s,false, "application/x-www-form-urlencoded",sb.length());
            PrintStream ps=c.getPrintStream();
            ps.print(sb.toString());
            ps.flush();
            HashMap<String,String> h=c.parseResponseHeaders();
            String data="";
            String transferEncoding=h.get("transfer-encoding");
            if(transferEncoding!=null&&transferEncoding.equalsIgnoreCase("chunked")){
                c.readLineUnbuffered();
            }
            data=c.readLineUnbuffered();
            onDataReceived(data);
            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    abstract void onDataReceived(String data);
    abstract void onError(String err);
}
