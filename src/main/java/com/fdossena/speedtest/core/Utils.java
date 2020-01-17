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

import java.net.URLEncoder;

class Utils {
    static String urlEncode(String s){
        try{return URLEncoder.encode(s, "utf-8");}catch(Throwable t){return null;}
    }
    static void sleep(long ms){
        try{Thread.sleep(ms);}catch (Throwable t){}
    }
    static void sleep(long ms, int ns){
        try{Thread.sleep(ms,ns);}catch (Throwable t){}
    }
    static String url_sep(String url){
        if(url.contains("?")) return "&"; else return "?";
    }
}
