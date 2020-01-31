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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.config.TelemetryConfig;

/**
 * A utility class to measure network speed to one or more LibreSpeed servers.
 *
 * @see <a href="https://github.com/librespeed/speedtest">LibreSpeed server</a>
 */
@SuppressWarnings("unused") // Public API
public class Speedtest {
    private ArrayList<TestPoint> servers=new ArrayList<>();
    private TestPoint selectedServer=null;
    private SpeedtestConfig config=new SpeedtestConfig();
    private TelemetryConfig telemetryConfig=new TelemetryConfig();
    private int state=0; //0=configs, 1=test points, 2=server selection, 3=ready, 4=testing, 5=finished

    private final Object mutex=new Object();

    private String originalExtra="";

    /**
     * Initialize an instance of a {@link Speedtest} with the default configuration and no servers.
     * <p>
     * Configuration can be added to this object by using {@link #setSpeedtestConfig(SpeedtestConfig)} and {@link #setTelemetryConfig(TelemetryConfig)}
     * <p>
     * Servers can be added with {@link #addTestPoint(TestPoint)}, {@link #addTestPoints(TestPoint[])}, {@link #addTestPoint(JSONObject)}
     * or {@link #addTestPoints(JSONArray)} and a server can be selected for the test with {@link #selectServer(ServerSelectedHandler)} or
     * {@link #setSelectedServer(TestPoint)}
     */
    public Speedtest(){

    }

    /**
     * Set the configuration for this speed test
     * @param speedtestConfig the configuration to set
     */
    public void setSpeedtestConfig(SpeedtestConfig speedtestConfig){
        synchronized (mutex){
            if(state!=0) throw new IllegalStateException("Cannot change config at this moment");
            config=speedtestConfig.clone();
            String extra=config.getTelemetry_extra();
            if(extra!=null&&!extra.isEmpty()) originalExtra=extra;
        }
    }

    /**
     * Set a telemetry configuration for this speed test
     * @param telemetryConfig the configuration to set
     */
    public void setTelemetryConfig(TelemetryConfig telemetryConfig){
        synchronized (mutex) {
            if (state != 0) throw new IllegalStateException("Cannot change config at this moment");
            this.telemetryConfig = telemetryConfig.clone();
        }
    }

    /**
     * Add one server to this speed test.
     * <p>
     * Note that the server is not automatically selected for the speed test and that either {@link Speedtest#selectServer(ServerSelectedHandler)}
     * or {@link Speedtest#setSelectedServer(TestPoint)} still needs to be called
     *
     * @param testPoint The server to add
     */
    public void addTestPoint(TestPoint testPoint){
        synchronized (mutex) {
            if (state == 0) state = 1;
            if (state > 1) throw new IllegalStateException("Cannot add test points at this moment");
            servers.add(testPoint);
        }
    }

    /**
     * Add servers to this speed test
     * <p>
     * Note that the server is not automatically selected for the speed test and that either {@link Speedtest#selectServer(ServerSelectedHandler)}
     * or {@link Speedtest#setSelectedServer(TestPoint)} still needs to be called
     *
     * @param testPoints The servers to add
     */
    public void addTestPoints(TestPoint[] testPoints){
        synchronized (mutex) {
            for (TestPoint t : testPoints) addTestPoint(t);
        }
    }

    /**
     * Add one server to this speed test.
     * <p>
     * The server is given as a JSON object. This methods parses the JSON object and adds the corresponding server to the list of available
     * servers
     * <p>
     * Note that the server is not automatically selected for the speed test and that either {@link Speedtest#selectServer(ServerSelectedHandler)}
     * or {@link Speedtest#setSelectedServer(TestPoint)} still needs to be called
     *
     * @param jsonTestPoint The server to add
     */
    public void addTestPoint(JSONObject jsonTestPoint){
        synchronized (mutex) {
            addTestPoint(new TestPoint(jsonTestPoint));
        }
    }

    /**
     * Add servers to this speed test.
     * <p>
     * The servers are given as a JSON array. This methods parses the JSON array and adds the corresponding servers to the list of available
     * servers
     * <p>
     * Note that the server is not automatically selected for the speed test and that either {@link Speedtest#selectServer(ServerSelectedHandler)}
     * or {@link Speedtest#setSelectedServer(TestPoint)} still needs to be called
     *
     * @param jsonTestPoints The servers to add
     */
    public void addTestPoints(JSONArray jsonTestPoints){
        synchronized (mutex) {
            for (int i = 0; i < jsonTestPoints.length(); i++)
                try {
                    addTestPoint(jsonTestPoints.getJSONObject(i));
                } catch (JSONException t) {
                }
        }
    }

    /**
     * Get the servers available for this speed test
     *
     * @return The available servers
     */
    public TestPoint[] getTestPoints(){
        synchronized (mutex) {
            return servers.toArray(new TestPoint[0]);
        }
    }

    private ServerSelector ss=null;

    /**
     * Automatically search for the closest server (by ping) and select it for use in the speed test.
     *
     * @param callback A handler defining the action to perform when a server has been selected
     */
    public void selectServer(final ServerSelectedHandler callback){
        synchronized (mutex) {
            if (state == 0) throw new IllegalStateException("No test points added");
            if (state == 2) throw new IllegalStateException("Server selection is in progress");
            if (state > 2) throw new IllegalStateException("Server already selected");
            state = 2;
            ss = new ServerSelector(getTestPoints(), config.getPing_connectTimeout()) {
                @Override
                void onServerSelected(TestPoint server) {
                    selectedServer = server;
                    synchronized (mutex) {
                        if (server != null) state = 3; else state = 1;
                    }
                    callback.onServerSelected(server);
                }
            };
            ss.start();
        }
    }

    /**
     * Manually set the server to user for the speed test
     *
     * @param testPoint The server to use
     */
    public void setSelectedServer(TestPoint testPoint){
        synchronized (mutex) {
            if (state == 2) throw new IllegalStateException("Server selection is in progress");
            if (testPoint == null) throw new IllegalArgumentException("t is null");
            selectedServer = testPoint;
            state = 3;
        }
    }

    private SpeedtestWorker st=null;

    /**
     * Start the speed test.
     *
     * @param callback The handler defining the actions to perform when the speed test makes progress
     */
    public void start(final SpeedtestHandler callback){
        synchronized (mutex) {
            if (state < 3) throw new IllegalStateException("Server hasn't been selected yet");
            if (state == 4) throw new IllegalStateException("Test already running");
            state = 4;
            try {
                JSONObject extra = new JSONObject();
                if (originalExtra != null && !originalExtra.isEmpty())
                    extra.put("extra", originalExtra);
                extra.put("server", selectedServer.getName());
                config.setTelemetry_extra(extra.toString());
            } catch (Throwable t) {
            }
            st = new SpeedtestWorker(selectedServer, config, telemetryConfig) {
                @Override
                void onDownloadUpdate(double dl, double progress) {
                    callback.onDownloadUpdate(dl, progress);
                }

                @Override
                void onUploadUpdate(double ul, double progress) {
                    callback.onUploadUpdate(ul, progress);
                }

                @Override
                void onPingJitterUpdate(double ping, double jitter, double progress) {
                    callback.onPingJitterUpdate(ping, jitter, progress);
                }

                @Override
                void onIPInfoUpdate(String ipInfo) {
                    callback.onIPInfoUpdate(ipInfo);
                }

                @Override
                void onTestIDReceived(String id) {
                    String shareURL=prepareShareURL(telemetryConfig);
                    if(shareURL!=null) shareURL=String.format(shareURL,id);
                    callback.onTestIDReceived(id,shareURL);
                }

                @Override
                void onEnd() {
                    synchronized (mutex) {
                        state = 5;
                    }
                    callback.onEnd();
                }

                @Override
                void onCriticalFailure(String err) {
                    synchronized (mutex) {
                        state = 5;
                    }
                    callback.onCriticalFailure(err);
                }
            };
        }
    }

    private String prepareShareURL(TelemetryConfig c){
        if(c==null) return null;
        String server=c.getServer(), shareURL=c.getShareURL();
        if(server==null||server.isEmpty()||shareURL==null||shareURL.isEmpty()) return null;
        if(!server.endsWith("/")) server=server+"/";
        while(shareURL.startsWith("/")) shareURL=shareURL.substring(1);
        if(server.startsWith("//")) server="https:"+server;
        return server+shareURL;
    }

    /**
     * Stop the test whenever possible.
     * <p>
     * Note that some tasks might take some time to finish after calling this method.
     */
    public void abort(){
        synchronized (mutex) {
            if (state == 2) ss.stopASAP();
            if (state == 4) st.abort();
            state = 5;
        }
    }

    /**
     * A handler to define action to perform when a server has been selected
     */
    public static abstract class ServerSelectedHandler{

        /**
         * Called when a server has been selected
         *
         * @param server The selected server
         */
        public abstract void onServerSelected(TestPoint server);
    }

    /**
     * A handler to define actions to perform when the speed test makes progress.
     */
    public static abstract class SpeedtestHandler{
        /**
         * Called when download measurement is actualized.
         *
         * @param speed The download speed in Mbps
         * @param progress The current progress of the download test between 0 and 1 where 0 means that the test hasn't started yet and 1 means that the test is finished
         */
        public abstract void onDownloadUpdate(double speed, double progress);

        /**
         * Called when upload measurement is actualized.
         *
         * @param speed The upload speed in Mbps
         * @param progress The current progress of the upload test between 0 and 1 where 0 means that the test hasn't started yet and 1 means that the test is finished
         */
        public abstract void onUploadUpdate(double speed, double progress);

        /**
         * Called when ping measurement is actualized.
         *
         * @param ping The lowest ping measured in milliseconds
         * @param jitter The ping measurements' jitter in milliseconds
         * @param progress The current progress of the ping test between 0 and 1 where 0 means that the test hasn't started yet and 1 means that the test is finished
         */
        public abstract void onPingJitterUpdate(double ping, double jitter, double progress);

        /**
         * Called when the client's IP and information about this IP is updated.
         *
         * @param ipInfo the IP address and its info
         */
        public abstract void onIPInfoUpdate(String ipInfo);

        /**
         * Called when the test result id has been received from the server.
         *
         * @param id The id of the result
         * @param shareURL The url to the image representing the result
         */
        public abstract void onTestIDReceived(String id, String shareURL);


        /**
         * Called when the test finishes.
         */
        public abstract void onEnd();

        /**
         * Called if an error occurs during the test
         *
         * @param err Information about the error
         */
        public abstract void onCriticalFailure(String err);
    }
}
