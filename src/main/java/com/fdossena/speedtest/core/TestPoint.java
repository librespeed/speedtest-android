/*
 *     This file is part of the LibreSpeed speedtest library.
 *
 *     The LibreSpeed speedtest library is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     The LibreSpeed speedtest library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with The LibreSpeed speedtest library.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fdossena.speedtest.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The representation of a LibreSpeed server
 *
 * @see <a href="https://github.com/librespeed/speedtest">LibreSpeed server</a>
 */
public class TestPoint {
    private final String name, server, dlURL, ulURL, pingURL, getIpURL;
    protected float ping=-1;

    /**
     * Create a new instance of {@link TestPoint}.
     *
     * @param name A user friendly name for the server
     * @param server The server's root URL (e.g. https://speedtest.example.com)
     * @param dlURL The path to the download address (e.g. backend/garbage.php)
     * @param ulURL The path to the upload address (e.g. backend/empty.php)
     * @param pingURL The path to the ping address (e.g. backend/empty.php)
     * @param getIpURL The path to the address to get the IP info (e.g. backend/getIP.php)
     */
    public TestPoint(String name, String server, String dlURL, String ulURL, String pingURL, String getIpURL){
        this.name=name;
        this.server=server;
        this.dlURL=dlURL;
        this.ulURL=ulURL;
        this.pingURL=pingURL;
        this.getIpURL=getIpURL;
    }

    /**
     * Create a new instance of {@link TestPoint} from a JSON object.
     * @param json the json object to use for this object's creation
     */
    public TestPoint(JSONObject json){
        try {
            name = json.getString("name");
            if (name == null) throw new IllegalArgumentException("Missing name field");
            server = json.getString("server");
            if (server == null) throw new IllegalArgumentException("Missing server field");
            dlURL = json.getString("dlURL");
            if (dlURL == null) throw new IllegalArgumentException("Missing dlURL field");
            ulURL = json.getString("ulURL");
            if (ulURL == null) throw new IllegalArgumentException("Missing ulURL field");
            pingURL = json.getString("pingURL");
            if (pingURL == null) throw new IllegalArgumentException("Missing pingURL field");
            getIpURL = json.getString("getIpURL");
            if (getIpURL == null) throw new IllegalArgumentException("Missing getIpURL field");
        }catch (JSONException t){
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public String getDlURL() {
        return dlURL;
    }

    public String getUlURL() {
        return ulURL;
    }

    public String getPingURL() {
        return pingURL;
    }

    public String getGetIpURL() {
        return getIpURL;
    }

    public float getPing() {
        return ping;
    }
}
