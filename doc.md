# LibreSpeed Android template

The LibreSpeed Android template allows you to configure and distribute an Android app that performs a speedtest using your existing [LibreSpeed](https://github.com/librespeed/speedtest) server(s).

## Configuring the template
Here's an overview of the structure of the template, configured with the default settings. Click to expand.

[![Overview of the template](https://github.com/librespeed/speedtest-android/blob/master/.github/template_fsm.png?raw=true)](https://github.com/librespeed/speedtest-android/blob/master/.github/template_fsm.png?raw=true)

### Download the template
The first thing to do is download latest version of the template:
```
git clone https://github.com/librespeed/speedtest-android
```

Now you will have a directory containing the template project and some documentation. Open the Speedtest-Android project in Android Studio.

### Setting the package name
The template's default package name is `your.name.here.speedtest`; this must be changed to allow distribution. Typically it's going to be something like `com.yourdomain.speedtest`.

* Right click app and click Open Module settings
* Go to Modules > app > Default config, change Application ID to your new package name and press Ok
* Expand app and double click manifests
* Update `package="your.name.here.speedtest"` with your new package name
* Expand app > java > com > fdossena > speedtest > ui
* Open MainActivity.java and update `import your.name.here.speedtest.R` with your new package name
* Do the same thing for GaugeView.java
* Build the application

### Adding your test points
Expand app > assets and open ServerList.json. Here you will find a list of test servers expressed as a JSON array.

* Select all and delete
* Add your test points following this syntax:  
    ```json
    [
        {
            "name":"My Test Server 1",
            "server":"//example.yourdomain.com",
            "dlURL":"garbage.php",
            "ulURL":"empty.php",
            "pingURL":"empty.php",
            "getIpURL":"getIP.php"
        },
        {
            ...
        }
        ...
    ]
    ```
    If you want the test to load this list from an URL instead of a file, just put the URL that you want to be loaded in the file in quotes, like this:
    ```
    "//mydomain.com/ServerList.json"
    ```
    The syntax of the remote JSON file is the same as the local one.
* Save the file

Here's a more in-depth explanation of what the individual fields for each servers are:
* __`"name"`__: User friendly name (eg. `"Milan, Italy"`)
* __`"server"`__: URL to the server where LibreSpeed is installed. If it only supports HTTP or HTTPS, specify it; if it supports both, simply use // followed by the address
* __`"dlURL"`__: Path on your server where the download test can be performed (typically `"garbage.php"` or `"backend/garbage.php"`)
* __`"urURL"`__: Path on your server where the upload test can be performed (typically `"empty.php"` or `"backend/empty.php"`)
* __`"pingURL"`__: Path on your server where the ping/jitter test can be performed (typically `"empty.php"` or `"backend/empty.php"`)
* __`"getIpURL"`__: Path on your server where the IP address and ISP info can be fetched (typically `"getIP.php"` or `"backend/getIP.php"`)

__Important__: This app DOES NOT handle HTTP redirects (3xx codes)!

### Telemetry and results sharing
If you want to enable telemetry and results sharing, edit TelemetryConfig.json. Here you need to add the information about your telemetry server (LibreSpeed frontend server). This feature is disabled by default.

* Follow this syntax:  
```json
{
    "telemetryLevel":"full",
    "server":"//speedtest.yourdomain.com",
    "path":"results/telemetry.php",
    "shareURL":"results/?id=%s"
}
```
* Save the file

Here's a more in-depth explanation of what the individual fields are:

* __`"telemetryLevel"`__:
    * `"disabled"`: No telemetry sent at all. If you choose this, you don't need to specify anything else
    * `"basic"`: Stores results for completed tests without log
    * `"full"`: Stores results and log for all tests, even if they are aborted or if they fail
    * Default: `"disabled"`
* __`"server"`__: URL to the server where the LibreSpeed telemetry is installed (the frontend server). If it only supports HTTP or HTTPS, specify it; if it supports both, simply use // followed by the address
* __`"path"`__: Path on your server where telemetry can be stored (typically `results/telemetry.php`)
* __`"shareURL"`__: Path on your server where the results sharing links point to (typically `results/?id=%s` or an empty string if you don't want to enable this feature). %s will be replaced by the test ID. Omit this if you don't want to use results sharing

#### Privacy policy
Telemetry contains personal information (according to GDPR defintion), therefore it is important to treat this data respectfully of national and international laws, especially if you plan to offer the service in the European Union.

A template for a privacy policy is included in `privacy_en.html`. You MUST read it, change it if necessary, and att your email address for data deletion requests. __Failure to comply with GDPR regulations can get you in serious trouble__.

If you plan to support other languages, see the chapter on localization on how to localize the privacy policy.

### Optional: Advanced configuration
The default speedtest configuration is fine for most use cases, but you can customize it if you want.

* Edit `SpeedtestConfig.json`. Inside you will find an empty JSON object. This indicates that the test is uses the default configuration. Delete it.
* Follow this syntax:  
```json
{
    "param1":"value",
    "param2":"other value",
    ...
}
```

Here's a list of all the test parameters. All parameters are optional. Do not change them unless you have a good reason to!

* __`"dl_ckSize"`__: Size in megabytes of the garbage data generated by garbage.php and downloaded by the test.
    * Default: `100`,
    * Recommended: `1`-`400`
    * Important: if you're not using garbage.php, make sure that your replaement file or whatever sends at least ckSize megabytes of data or the download test might fail!
* __`"ul_ckSize"`__: Size in megabytes of the garbage data sent by the upload test.
    * Default: `20`,
    * Recommended: `1`-`100`
* __`"dl_parallelStreams"`__: Number of concurrent streams used by the download test.
    * Default: `3`
    * Recommended: `1`-`6`.
* __`"ul_parallelStreams"`__: Number of concurrent streams used by the upload test.
    * Default: `3`
    * Recommended: `1`-`6`.
* __`"dl_streamDelay"`__: Delay in milliseconds between starts of concurrent streams in the download test
    * Default: `300`
    * Recommended: `0`-`1000`
* __`"ul_streamDelay"`__: Delay in milliseconds between starts of concurrent streams in the upload test
    * Default: `300`
    * Recommended: `0`-`1000`
* __`"time_dl_max"`__: Maximum duration in settings of the download test (if time_auto is set to false, this will be the fixed duration)
    * Default: `15`
    * Recommended: `5`-`30`
* __`"time_ul_max"`__: Maximum duration in settings of the upload test (if time_auto is set to false, this will be the fixed duration)
    * Default: `15`
    * Recommended: `5`-`30`
* __`"time_auto"`__: If set to true, the duration of the upload and download tests will be automatically decided according to the measured speed.
    * Default: `true`
* __`"count_ping"`__: Number of pings to be done during the ping+jitter test
    * Default: `10`
    * Recommended: `3`-`50`
* __`"dl_graceTime"`__: Seconds at the beginning of the download test where data is discarded (allows TCP window adjustment)
    * Default: `1.5`
    * Recommended: >`0`
* __`"ul_graceTime"`__: Seconds at the beginning of the upload test where data is discarded (prevents initial spike due to buffering)
    * Default: `1.5`
    * Recommended: >`0`
* __`"errorHandlingMode"`__: What the test should do in case an error is encountered
    * `"fail"`: Fail the test immediately
    * `"attempt-restart"`: If a stream dies, it tries to restart it. If reopening fails, the test fails
    * `"must-restart"`: If a stream dies, keep trying to restart it indefinitely
    * Default: `"attempt-restart"`
* __`"getIP_isp"`__: If set to true, the test will request ISP information along with the IP address
    * Default: `true`
* __`"getIP_distance"`__: If `getIP_isp` is set to true, this tells whether to calculate approximate distance or not, and the unit
    * `"no"`: Do not calculate distance
    * `"km"`: Calculate distance in kilometers
    * `"mi"`: Calculate distance in miles
    * Default: `"km"`
* __`"overheadCompensationFactor"`__: How much the download and upload values should be multipled by to compensate for transport overhead
    * Default: `1.06`
    * Recommended: `1` to ignore overhead, or `1.06` for a decent estimate
* __`"useMebibits"`__: If set to true, the download and upload speeds will be measured in mebibits instead of megabits
    * Default: `false`
* __`"test_order"`__: The order in which tests are performed. Each character in this string represents a test. Tests can only be run once
    * Default: `"IP_D_U"`
    * `I`: IP address and ISP information
    * `P`: Ping+jitter test
    * `D`: Download test
    * `U`: Upload test
    * `_`: 1 second delay
* __`"dl_connectTimeout"`__, __`"dl_soTimeout"`__: Timeouts in milliseconds for the sockets used in the download test.
    * Default: `5000`, `10000`
    * `-1` means System default
* __`"ul_connectTimeout"`__, __`"ul_soTimeout"`__: Timeouts in milliseconds for the sockets used in the upload test.
    * Default: `5000`, `10000`
    * `-1` means System default
* __`"ping_connectTimeout"`__, __`"ping_soTimeout"`__: Timeouts in milliseconds for the sockets used in the ping+jitter test, the server selector, IP/ISP fetching, and telemetry.
    * Default: `2000`, `5000`
    * `-1` means System default
* __`"dl_recvBuffer"`__, __`"dl_sendBuffer"`__: Sizes of socket buffers used in the download test
    * Default: `-1`
    * `-1` means System default
* __`"ul_recvBuffer"`__, __`"ul_sendBuffer"`__: Sizes of socket buffers used in the upload test
    * Default: `-1` and `16384` respectively
    * `-1` means System default
* __`"ping_recvBuffer"`__, __`"ping_sendBuffer"`__: Sizes of socket buffers used in the ping+jitter test
    * Default: `-1`
    * `-1` means System default

### Branding

#### Logos and background
Expand app > res > drawable. Replace the following files (make sure that they actually go in the drawable folder!):

* __`ic_launcher.png`__: App icon (256x256 recommended)
* __`logo.png`__: Large logo file used in the splash screen (1000px width recommended)
* __`logo_inapp.png`__: Small logo file shown above the test (800-1000px width recommended)
* __`testbackground.png`__: Background image shown under the test (Large but not too large or it won't be loaded to prevent crashing low end devices)

#### App name and link
Expand app > res > values and edit strings.xml.

* Replace the value of `app_name` with the name of your app
* Replace the value of `logo_inapp_link` with a link to your website that users can open by clicking on the logo. Leave empty if you don't want the logo to be clickable.

#### Colors
Expand app > res > values and edit colors.xml.

Here's a list of where each color is used:

* __`splashBackground`__: Displayed on the splash screen, under the logo
* __`appBackground`__: Background color in the rest of the app
* __`textColor`__: Color used by most of the text in the application
* __`loadingColor`__: Color of the spinning progress bar displayed during server selection
* __`gaugesBackground`__: Background color of the gauges
* __`progressBackground`__: Background color of the progress bars below the download and upload indicators (ignored in older versions of Android)
* __`progressColor`__: Color of the progress bars below the download and upload indicators (ignored in older versions of Android)
* __`dlGauge`__: Color of the download gauge
* __`dlText`__: Color of the text indicating the download speed
* __`ulGauge`__: Color of the upload gauge
* __`ulText`__: Color of the text indicating the upload speed
* __`pingText`__: Color of the text indicating the ping time
* __`jitterText`__: Color of the text indicating the jitter time
* __`serverText`__: Color of the text indicating the server name
* __`startButton_background`__: Color of the start button
* __`startButton_test`__: Color of the text in the start button
* __`failButton_background`__: Color of the fail button (the one that says Retry)
* __`failButton_test`__: Color of the text in the fail button (the one that says Retry)
* __`shareButton_background`__: Color of the share button
* __`shareButton_test`__: Color of the text in the share button
* __`restartButton_background`__: Color of the button to start a new test
* __`restartButton_test`__: Color of the text in the button to start a new test
* __`privacyLinks`__: Color of the link to open/close the privacy policy

__Important__: Make sure your text is always legible!

### Optional: localization
The template only contains an English localization. To replace the English text or add other languages, expand app > res > values and double click strings.xml. From here you can change all the strings in the application. To add other languages, use Android Studio's localization editor.

Here's a description of each string:

* __`app_name`__: Name of the app as it appears in the app drawer
* __`logo_inapp_link`__: Link to open if the user clicks on the logo. Leave this empty if you don't want this
* __`privacy_policy`__: URL to the privacy policy for this specific locale. You can have multiple privacy policies for different languages in the assets folder
* __`init_init`__: Displayed while the app is loading
* __`initFail_configError`__: Error that appears if the configuration couldn't be loaded
* __`initFail_retry`__: Text of the Retry button when initialization or server selection fails
* __`init_selecting`__: Displayed while the app is selecting a server
* __`initFail_noServers`__: Displayed if no servers are available
* __`serverSelect_message`__: Displayed above the server selection spinner
* __`start`__: Text of the Start button
* __`test_dl`__: Label above the download gauge
* __`test_ul`__: Label above the upload gauge
* __`test_speedMeasure`__: Label below the download and upload gauges
* __`test_ping`__: Label above the ping time indicator
* __`test_jitter`__: Label above the jitter time indicator
* __`test_timeMeasure`__: Label next to the ping and jitter indicators
* __`test_share`__: Text of the share button
* __`test_restart`__: Text of the new test button
* __`testFail_err`__: Displayed if an error occurs during the test
* __`testFail_retry`__: Text of the Retry button when the test fails
* __`privacy_open`__: Text of the link that opens the privacy policy
* __`privacy_close`__: Text of the link that closes the privacy policy

### The end
Your template is now fully configured for your LibreSpeed server(s). Enjoy!

This template is licensed under the [GNU LGPLv3 License](https://www.gnu.org/licenses/lgpl-3.0.en.html). In short, you are free to use, study, modify, and distribute modified copies of this application, but they must remain under the same GNU LGPLv3 license. This means that if you make any modification to the application, the source code must be made publicly available. Merely changing the configuration of this template as we did in this chapter does NOT count as a modified version, so you are free to distribute it as an APK, but any other change made to the app does count as a modification and you MUST provide the source code.

## Making a custom UI
If you don't like the UI provided by the template or the customization is too limited, you might want to make your own UI. This is possible and APIs are provided to develop a custom UI, however it requires pretty good knowledge of Android and Java.

To make a custom UI:

* Make a new project in Android Studio. The minimum SDK supported by the speedtest is API 15.
* Copy the `com.fdossena.speedtest.core` package from the template into the new project
* Add the `android.permission.INTERNET` permission to the manifest
* Start developing

Before we continue, we need to discuss the license: the `com.fdossena.speeedtest.core` package is licensed under a GNU LGPLv3 license; you are now using it as a library to develop your project so your project can use whatever license you want (even a proprietary one if you want), but any modification made to the files inside the `com.fdossena.speedtest.core` package MUST be made publicly available in source code form. You must also credit the original author in your app (Federico Dossena, https://fdossena.com).

### The `Speedtest` class
The `com.fdossena.speedtest.core` provides a handy `Speedtest` class that you can use to implement your test.

##### Constructor
The first thing to do is create an instance of the `Speedtest` class:

```java
Speedtest st = new Speedtest();
```

You might want to store this object in a variable in your activity.

##### Configuration
If you don't want to use the default settings, you need to load your configuration:

```java
SpeedtestConfig config=new SpeedtestConfig();
... change some of the settings ...
st.setSpeedtestConfig(config);
```

The `config` class provides getters and setters for all the test settings mentioned in the Advanced configuration section. Anything that you don't explicitly change will be set to the default value.

The `SpeedtestConfig` class can also be instantiated from a JSON object, which is very convenient if you want to load the settings from a file.

##### Telemetry
If you want to use telemetry, you need to load your configuration:

```java
TelemetryConfig telemetryConfig=new TelemetryConfig(
    telemetryLevel,
    server,
    path,
    shareURL
);
st.setTelemetryConfig(telemetryConfig);
```

These are the same settings mentioned in the Telemetry and results sharing section.

The `TelemetryConfig` class can also be instantiated from a JSON object, which is very convenient if you want to load the settings from a file.

##### Adding your test points
To run the test you need at least 1 test point. To add a test point, use 
```java
TestPoint p=new TestPoint(name,server,dlURL,ulURL,pingURL,getIpURL);
st.addTestPoint(p);
```

You can also add test points from an array, or from a JSON array, which is very convenient if you want to load the settings from a file:

```java
TestPoint[] servers=new TestPoint[]{
    new TestPoint(name,server,dlURL,ulURL,pingURL,getIpURL),
    ...
};
st.addTestPoints(servers);
```

If you want to load the servers from an URL instead, use:
```java
st.loadServerList(url)
```
__Important__: This function is blocking and must not be called from the UI thread.

These are the same settings mentioned in the Adding your test points section.

##### Server selection
To run the server selection process, use

```java
st.selectServer(new Speedtest.ServerSelectedHandler(){
    @Override
    public void onServerSelected(TestPoint server){
        //do something
    }
});
```

The selection process is asynchronous. The `onServerSelected` method will be called at the end of process: `server` will either be the server with the lowest ping, or `null` if none of them were reachable.

To manually choose one of the servers, use
```java
TestPoint p=new TestPoint(...);
st.setSelectedServer(p);
```

Note: `selectServer` can only be called once, and you cannot call it after using `setSelectedServer`!

Note: You cannot change the configuration, or add other test points after selecting a server!

##### Running the test
Finally, we can run the test:

```java
    st.start(new Speedtest.SpeedtestHandler(){
        @Override
        public void onDownloadUpdate(double dl, double progress){
            //update your UI
        }
        public void onUploadUpdate(double ul, double progress){
            //update your UI
        }
        public void onPingJitterUpdate(double ping, double jitter, double progress){
            //update your UI
        }
        public void onIPInfoUpdate(String ipInfo){
            //update your UI
        }
        public void onTestIDReceived(String id){
            //update your UI
        }
        public void onEnd(){
            //test finished
        }
        public void onCriticalFailure(String err){
            //do something
        }
    });
```

The test is done asynchronously. During the test, the following callbacks will be called:

* `onDownloadUpdate` is called periodically during the download test to report the download speed. `dl` is the current speed in Mbps, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onUploadUpdate` is called periodically during the upload test to report the upload speed. `ul` is the current speed in Mbps, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onPingJitterUpdate` is called periodically during the ping+jitter test to report ping and jitter times. `ping` is the current ping, `jitter` is the current jitter, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onIPInfoUpdate` is called when the IP address and ISP information is received. `ipInfo` is a string combining this information
* `onTestIDReceived` is called at the end of the test when we receive an ID from the telemetry. `id` is a string containing the test ID that can be used to generate a share link
* `onEnd` is called at the end of the test
* `onCriticalFailure` is called if the test fails. `err` is a string containing details about the error

Note: if some functions are disabled (for instance, telemetry), the corresponding event will not be called.

After the test is over, the test can be ran again, with a different server if you want.

##### Aborting the test
The test can be aborted at any time using

```java
st.abort();
```

Note: aborting the test is not instantaneous. This is done asynchronously. If the test is running, the `onEnd` event will be called.

## Implementation details
This chapter is dedicated to the inner workings of the `com.fdossena.speedtest.core` package. You don't need to read this section if you're just customizing the template or making a custom UI, it's only here if you need to make changes to how the speedtest client works.

If you also need to know how the speedtest server works, see [here](https://github.com/librespeed/speedtest/wiki/Implementation-details).

__Remember that both the server and this client are under a GNU LGPLv3 license, so any modification you make to it MUST be publicly available in source form! No exceptions.__

The core is basically an HTTP client implemented using only Sockets, Threads and Java streams, so very good knowledge of these topics is mandatory if you want to make changes here.

This chapter will be divided into the following sections:

* __`base` package__: handles HTTP/HTTPS connection creation and provides functions to interact with an HTTP server
* __`getIP` package__: implements IP and ISP info fetching
* __`download` package__: implementation of the streams used for the download test
* __`upload` package__: implementation of the streams used for the upload test
* __`ping` package__: implementation of a "ping stream" used for the ping+jitter test and the server selector
* __`telemetry` package__: implements telemetry sender
* __`serverSelector` package__: implements the automatic server selection process
* __`log` package__: a simple logger used to collect telemetry
* __`config` package__: configuration for the speedtest and the telemetry
* __`worker` package__: implements the speedtest
* __`Speedtest` class__: wraps everything up

### `base` package
##### `Utils` class
This class provides some static functions used throughout the test. These functions are:

* `urlEncode(String s)`: URL-encodes a string (UTF-8)
* `sleep(long ms)` and `sleep(long ms, int ns)`: simple sleep functions
* `url_sep(String url)`: Returns the proper separator to use for GET parameters for a given string (eg. for "https://example.com/index.php" it will return "?", for "https://example.com/?id=whatever" it will return "&")

##### `Connection` class
This is the foundation on which relies most of the test.

It uses a Socket to connect to the HTTP server, using either HTTP or HTTPS, and provides several methods to interact with it.

__Constructors:__

```java
public Connection(String url, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer)
```

* `url` is the URL of the host that you want to connect to (eg. "https://speedtest.fdossena.com")
* `connectTimeout` and `soTimeout` are the timeouts for the socket in milliseconds. Set them to `-1` to let the system determine them
* `recvBuffer` and `sendBuffer` are sizes of the buffers used by the socket in bytes. Set them to `-1` to let the system determine them

```java
public Connection(String url)
```

* `url` is the URL of the host that you want to connect to (eg. "https://speedtest.fdossena.com")

If you use this constructor, it will use a `connectTimeout` of `2000`, a `soTimeout` of `5000` and default buffer sizes.

Note: URLs can be in one of these formats:
* `http://address`: if the server only supports HTTP
* `https://address`: if the server only supports HTTPS
* `//address`: if the server supports both HTTP and HTTPS (HTTPS preferred)

__Important__: This class DOES NOT handle HTTP redirects (3xx codes)!

__Methods:__

* `getInputStream()`: returns the socket's InputStream or null if the socket is closed/dead
* `getOutputStream()`: returns the socket's OutputStream or null if the socket is closed/dead
* `getPrintStream()`: similar to `getOutputStream` but it returns a `PrintStream` instead, which is more convenient for writing UTF-8 strings
* `getInputStreamReader()`: similar to `getInputStream` but it returns an `InputStreamReader` instead, which is more convenient for reading UTF-8 strings
* `GET(String path, boolean keepAlive)`: writes a GET request on the socket to fetch the resource at the requested `path`. If `keepAlive` is set to true, it also sends a `Connection: keep-alive` header so that the Connection can be reused. An Exception is thrown if something goes wrong.
* `POST(String path, boolean keepAlive, String contentType, long contentLength):`: writes a POST request on the socket to fetch the requested `path` after sending some data. An Exception is thrown if something goes wrong.
    * `keepAlive`: if set to true, it will send the `Connection: keep-alive` header and the Connection can be reused
    * `contentType`: if not null, it will send the `Content-Type` header with the specified content type
    * `contentLength`: if >=`0`, it will send the `Content-Length` header, and more data can be written to the socket's OutputStream
* `readLineUnbuffered()`: reads a line from the socket's InputStream until `\n` is encountered
* `parseResponseHeaders()`: reads an entire HTTP response from the socket's InputStream. The headers are returned in a `Hashmap<String,String>`. Throws an exception if something goes wrong or if the response was not a `200 OK`
* `close()`: closes the socket

### `getIP` package
##### `GetIP` class
Implements IP and ISP info fetching over an existing Connection. Creates a new Thread to do it. Starts immediately (no need to call `start()`).

__Constructor:__

```java
public GetIP(Connection c, String path, boolean isp, String distance)
```

* `c`: the instance of Connection to be used
* `path`: path on the server where the IP and ISP info can be fetched
* `isp`: if set to true, it will fetch ISP info, otherwise it will only fetch the IP address
* `distance`: if `isp` is set to true, this specifies how the distance should be measured
    * `SpeedtestConfig.DISTANCE_NO`: do not calculate distance
    * `SpeedtestConfig.DISTANCE_KM`: kilometers
    * `SpeedtestConfig.DISTANCE_MILES`: miles

__Callbacks:__

* `onDataReceived(String data)`: if the operation succedes, this is called with a JSON string representing the result (see `getIP.php` implementation detalis for more information)
* `onError(String err)`: if the operation fails, this is called with an error message

### `download` package
##### `Downloader` class
A Thread that uses a Connection to download an endless stream of garbage data from the server. The download starts immediately (no need to call `start()`).

__Constructor:__

```java
public Downloader(Connection c, String path, int ckSize)
```

* `c`: the instance of Connection to be used
* `path`: path on the server where the garbage data can be fetched
* `ckSize`: size in megabytes of the garbage data (also see documentation for garbage.php)

__Important:__ If you're not using `garbage.php`, your replacement must accept the `ckSize` parameter; if you're using a large file of garbage data, it needs to be at least `ckSize` megabytes for this downloader to work properly.

This thread repeatedly sends a GET request to the speified `path` and download all the data coming to it. When the amount of downloaded data is nearing ckSize (75%), a new request is made to ensure a steady flow of data.

__Callbacks:__

* `onProgress(long downloaded)`: periodically called to inform of how much data was downloaded. (Max once every 200ms)
* `onError(String err)`: called if something goes wrong. `err` contains the error message

__Methods:__

* `stopASAP()`: asks to stop the download as soon as possible. The Connection will also be closed. Use `join()` to wait for the thread to die
* `resetDownloadCounter()`: resets the counter of the amount of downloaded data
* `getDownloaded()`: returns the amount of downloaded data since the beginning or last reset

##### `DownloadStream` class
A stream for the download test. Manages a Connection and a Downloader, and handles errors according to the specified policy. Starts immediately.

__Constructor:__

```java
public DownloadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log)
```

* `server`: URL to the server where we need to connect
* `path`: path on the server where the garbage data can be fetched
* `ckSize`: size in megabytes of the garbage data
* `errorHandlingMode`: what we should do in case of errors:
    * SpeedtestConfig.ONERROR_FAIL: Fail immediately
    * SpeedtestConfig.ONERROR_ATTEMPT_RESTART: Try to reconnect. If that fails, the stream fails
    * SpeedtestConfig.ONERROR_MUST_RESTART: Keep trying to reconnect until we succeed (or stopped)
* `connectTimeout`, `soTimeout`, `recvBuffer`, `sendBuffer`: settings for the socket created by Connection
* `log`: instance of Logger that is used to report errors such as dead connections

__Callbacks:__

* `onError(String err)`: called if the stream fails. `err` contains the error message

__Methods:__

* `stopASAP()`: asks to stop the download as soon as possible. The Connection will also be closed. Use `join()` to wait for the connection to be actually closed
* `resetDownloadCounter()`: resets the counter of the amount of downloaded data
* `getTotalDownloaded()`: returns the amount of downloaded data since the beginning or last reset
* `join()`: waits for the instance of Downloader to die

### `upload` package
##### `Uploader` class
A Thread that uses a Connection to upload an endless stream of POST request containing garbage data to the server. The upload starts immediately (no need to call `start()`).

This is the upload equivalent of the `Downloader` class.

__Constructor:__

```java
public Uploader(Connection c, String path, int ckSize)
```

* `c`: the instance of Connection to be used
* `path`: path on the server where the garbage data will be sent
* `ckSize`: size in megabytes of the garbage data sent with each POST request

__Important__: when an instance of Uploader is created, a blob of `ckSize` megabytes is generated and stays in RAM. Don't instantiate too many!

This thread repeatedly sends a POST request containing garbage data to the server. Responses coming from the server are ignored and discarded.

__Callbacks:__

* `onProgress(long uploaded)`: periodically called to inform of how much data was uploaded. (Max once every 200ms)
* `onError(String err)`: called if something goes wrong. `err` contains the error message

__Methods:__

* `stopASAP()`: asks to stop the upload as soon as possible. The Connection will also be closed. Use `join()` to wait for the thread to die
* `resetUploadCounter()`: resets the counter of the amount of upload data
* `getUploaded()`: returns the amount of uploaded data since the beginning or last reset

##### `UploadStream` class
A stream for the upload test. Manages a Connection and an Uploader, and handles errors according to the specified policy.

This is the upload equivalent of the `DownloadStream` class.

__Constructor:__

```java
public DownloadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log)
```

* `server`: URL to the server where we need to connect
* `path`: path on the server where the garbage data can be fetched
* `ckSize`: size in megabytes of the garbage data
* `errorHandlingMode`: what we should do in case of errors:
    * `SpeedtestConfig.ONERROR_FAIL`: Fail immediately
    * `SpeedtestConfig.ONERROR_ATTEMPT_RESTART`: Try to reconnect. If that fails, the stream fails
    * `SpeedtestConfig.ONERROR_MUST_RESTART`: Keep trying to reconnect until we succeed (or stopped)
* `connectTimeout`, `soTimeout`, `recvBuffer`, `sendBuffer`: settings for the socket created by Connection
* `log`: instance of Logger that is used to report errors such as dead connections

__Callbacks:__

* `onError(String err)`: called if the stream fails. `err` contains the error message

__Methods:__

* `stopASAP()`: asks to stop the upload as soon as possible. The Connection will also be closed. Use `join()` to wait for the connection to be actually closed
* `resetUploadCounter()`: resets the counter of the amount of uploaded data
* `getTotalUploaded()`: returns the amount of uploaded data since the beginning or last reset
* `join()`: waits for the instance of Uploader to die

### `ping` package
##### `Pinger` class
A Thread that uses a Connection to repeatedly ping a specified path.

A ping is defined as the time difference between the moment in which we finish sending the HTTP request, and the moment in which we start receiving the response, over a persistent HTTP connection.

__Constructor:__

```java
public Pinger(Connection c, String path)
```

* `c`: the instance of Connection to be used
* `path`: path on the server where the ping should be done

__Callbacks:__

* `boolean onPong(long ns)`: called after a ping. `ns` is the ping time in nanoseconds. Return true to perform another ping, false to stop.
* `onError(String err)`: called if something goes wrong. `err` contains the error message

__Methods:__

* `stopASAP()`: stops the pinging as soon as possible. Use `join()` to wait for the thread to die

##### `PingStream` class
A stream for the ping+jitter test. Manages a Connection and a Pinger, and handles errors according to the specified policy.

__Constructor:__

```java
public DownloadStream(String server, String path, int pings, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log)
```

* `server`: URL to the server where we need to connect
* `path`: path on the server where the ping can be done
* `pings`: number of pings to be performed
* `errorHandlingMode`: what we should do in case of errors:
    * `SpeedtestConfig.ONERROR_FAIL`: Fail immediately
    * `SpeedtestConfig.ONERROR_ATTEMPT_RESTART`: Try to reconnect. If that fails, the stream fails
    * `SpeedtestConfig.ONERROR_MUST_RESTART`: Keep trying to reconnect until we succeed (or stopped)
* `connectTimeout`, `soTimeout`, `recvBuffer`, `sendBuffer`: settings for the socket created by Connection
* `log`: instance of Logger that is used to report errors such as dead connections

__Callbacks:__

* `boolean onPong(long ns)`: called after a ping. `ns` is the ping time in nanoseconds. Return true to perform another ping, false to stop.
* `onError(String err)`: called if the stream fails. `err` contains the error message
* `onDone()`: called after all the pings are done

__Methods:__

* `stopASAP()`: asks to stop the pinging as soon as possible. The Connection will also be closed. Use `join()` to wait for the connection to be actually closed
* `join()`: waits for the instance of Pinger to die

### `telemetry` package
##### `Telemetry` class
Sends telemetry to the server over an existing Connection. Creates a new Thread to do it. Starts immediately (no need to call `start()`).

__Constructor:__

```java
public Telemetry(Connection c, String path, String level, String ispinfo, String extra, String dl, String ul, String ping, String jitter, String log)
```

* `c`: the instance of Connection to be used
* `path`: path on the server where the IP and ISP info can be fetched
* `level`: telemetry level
    * `TelemetryConfig.DISABLED`: sends nothing
    * `TelemetryConfig.BASIC`: sends only the results
    * `TelemetryConfig.FULL`: sends results and log
* `ispinfo`, `extra`, `dl`, `ul`, `ping`, `jitter`, `log`: data to be sent. No null values.

__Callbacks:__

* `onDataReceived(String data)`: if the operation succedes, this is called with the response from the server (1 line)
* `onError(String err)`: if the operation fails, this is called with an error message

### `serverSelector` package
##### `TestPoint` class
Defines a test point where the speedtest can be performed.

__Constructors:__

```java
public TestPoint(String name, String server, String dlURL, String ulURL, String pingURL, String getIpURL)
```

* `name`: User friendly name (eg. `"Milan, Italy"`)
* `server`: URL to the server where LibreSpeed is installed. If it only supports HTTP or HTTPS, specify it; if it supports both, simply use // followed by the address
* `dlURL`: Path on your server where the download test can be performed (typically `"garbage.php"` or `"backend/garbage.php"`)
* `urURL`: Path on your server where the upload test can be performed (typically `"empty.php"` or `"backend/empty.php"`)
* `pingURL`: Path on your server where the ping/jitter test can be performed (typically `"empty.php"` or `"backend/empty.php"`)
* `getIpURL`: Path on your server where the IP address and ISP info can be fetched (typically `"getIP.php"` or `"backend/getIP.php"`)

```java
public TestPoint(JSONObject json)
```

* `json`: JSON object with the same fields as above

The class also has a protected `ping` field, which is used by `ServerSelector`. It is initialized at `-1`, and after running the server selector, it will contain the ping in milliseconds to this test point.

The class provides getters for all fields.

##### `ServerSelector` class
Uses several parallel PingStream instances to ping a list of test points and find out which ones are online, and which one has the lowest ping.

This class uses 6 parallel streams (see `PARALLELISM`), and pings each server up to 3 times (see `PINGS`); if a ping takes more than 500ms (see `SLOW_THRESHOLD`), no more pings are done. If a server is offline, its `ping` field is set to `-1`, otherwise it will be set to the lowest measured ping for that server. Once all test points have been pinged, the server with the lowest ping is determined and a callback is called.

__Constructor:__

```java
public ServerSelector(TestPoint[] servers, int timeout)
```

* `servers`: list of servers. More can be added later. If you want to start with an empty list, use `new TestPoint[0]`, not null.
* `timeout`: timeout for a ping

__Callbacks:__

* `onServerSelected(TestPoint server)`: called at the end of server selection. `server` is the server with the lowest ping, or null if all test points are offline.

__Methods:__

* `addTestPoint(TestPoint t)` and `addTestPoint(JSONObject o)`: adds a TestPoint to the list of servers
* `addTestPoints(TestPoint[] servers)` and `addTestPoints(JSONArray a)`: adds a list of TestPoints to the list of servers
* `getTestPoints()`: returns the list of servers as a TestPoint[]
* `start()`: starts the server selection process
* `stopASAP()`: stops the server selection process as soon as possible

### `log` package
##### `Logger` class
Implements a very simple logger. Used to store the telemetry generated during the test.

__Constructor:__

```java
public Logger()
```

__Methods:__

* `getLog()`: returns the current log
* `l(String s)`: appends a timestamp in milliseconds and the string `s` to the log`

### `config` package
##### `SpeedtestConfig` class
Stores the configuration for the speedtest, and provides some constants used throughout the application.

__Constructors:__

To instantiate with the default configuration (recommended):
```java
public SpeedtestConfig()
```

To instantiate with custom settings:
```java
public SpeedtestConfig(int dl_ckSize, int ul_ckSize, int dl_parallelStreams, int ul_parallelStreams, int dl_streamDelay, int ul_streamDelay, double dl_graceTime, double ul_graceTime, int dl_connectTimeout, int dl_soTimeout, int ul_connectTimeout, int ul_soTimeout, int ping_connectTimeout, int ping_soTimeout, int dl_recvBuffer, int dl_sendBuffer, int ul_recvBuffer, int ul_sendBuffer, int ping_recvBuffer, int ping_sendBuffer, String errorHandlingMode, int time_dl_max, int time_ul_max, boolean time_auto, int count_ping, String telemetry_extra, double overheadCompensationFactor, boolean getIP_isp, String getIP_distance, boolean useMebibits, String test_order)
```

A description for each of these arguments is provided in the Advanced configuration section.

SpeedtestConfig can also be instantiated from a JSON object with the same fields.

Getters and setters for all of the settings are provided. Validity checks are performed every time a setting is changed.

__Public constants:__

* Erorr handling:
    * `ONERROR_FAIL`: `"fail"`
    * `ONERROR_ATTEMPT_RESTART`: `"attempt-restart"`
    * `ONERROR_MUST_RESTART`: `"must-restart"`
* Distance measurement
    * `DISTANCE_NO`: `"no"`
    * `DISTANCE_KM`: `"km"`
    * `DISTANCE_MILES`: `"mi"`

__Methods:__

* `clone()`: generates a clone of this object

##### `TelemetryConfig` class
Stores the configuration for the telemetry, and provides some constants used throughout the application.

__Constructors:__

To instantiate with the default settings (telemetry disabled):
```java
public TelemetryConfig()
```

To instantiate with custom settings:
```java
public TelemetryConfig(String telemetryLevel, String server, String path, String shareURL)
```

A description for each of these arguments is provided in the Telemetry and results sharing section.

TelemetryConfig can also be instantiated from a JSON object with the same fields.

Getters are provided for all of the settings.

__Methods:__

* `clone()`: generates a clone of this object

### `worker` package
##### `SpeedtestWorker` class
Performs the speedtest using a given TestPoint, SpeedtestConfig and TelemetryConfig. This is the Java equivalent of `speedtest_worker.js`. Creates a Thread. Starts immediately (no need to call `start()`).

__Constructor:__

```java
public SpeedtestWorker(TestPoint backend, SpeedtestConfig config, TelemetryConfig telemetryConfig)
```

* `backend`: the TestPoint to use to perform the speedtest
* `config`: speedtest settings. Use null to use the default settings
* `telemetryConfig`: telemetry settings. Use null to use the default settings (telemetry disabled)

The GetIP class is used to fetch IP and ISP info.

The download test uses multiple instances of DownloadStream to perform the test. After an initial grace time in which speed data is discarded, speed is measured as the amount of downloaded data over the amount of time it took to download it.

The upload test uses multiple instances of UploadStream to perform the test. After an initial grace time in which speed data is discarded, speed is measured as the amount of uploaded data over the amount of time it took to send it.

The ping test is performed by a single PingStream. The ping reported by the test is the lowest ping measured. Jitter is also calculated as the variance between consecutive pings.

At the end of the test, the Telemetry class is used to send telemetry (if enabled).

Each test can only be ran once.

__Callbacks:__

* `onDownloadUpdate` is called periodically during the download test to report the download speed. `dl` is the current speed in Mbps, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onUploadUpdate` is called periodically during the upload test to report the upload speed. `ul` is the current speed in Mbps, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onPingJitterUpdate` is called periodically during the ping+jitter test to report ping and jitter times. `ping` is the current ping, `jitter` is the current jitter, `progress` is a number between 0 and 1 representing how close we are to the time limit
* `onIPInfoUpdate` is called when the IP address and ISP information is received. `ipInfo` is a string combining this information
* `onTestIDReceived` is called at the end of the test when we receive an ID from the telemetry. `id` is a string containing the test ID that can be used to generate a share link
* `onEnd` is called at the end of the test
* `onCriticalFailure` is called if the test fails. `err` is a string containing details about the error

__Methods:__

* `abort()`: aborts the test as soon as possible. Use `join()` to wait for the test to die.

### `Speedtest` class
This class wraps up everything into a convenient interface that can be used to develop your custom UI.

It is described extensively in the The `Speedtest` class section in the chapter on making a custom UI.

## Contributing

Since this is an open source project, you can modify it.

If you made some changes that you think should make it into the main project, send a Pull Request on GitHub, or contact me at [info@fdossena.com](mailto:info@fdossena.com).  
We don't require you to use a specific coding convention, write the code however you want and we'll change the formatting if necessary.

Donations are also appreciated: you can donate with [PayPal](https://www.paypal.me/sineisochronic) or [Liberapay](https://liberapay.com/fdossena/donate).

## License
This software is under the GNU LGPL license, Version 3 or newer.

To put it short: you are free to use, study, modify, and redistribute this software and modified versions of it, for free or for money.
You can also use it in proprietary software but all changes to this software must remain under the same GNU LGPL license.

Contact me at [info@fdossena.com](mailto:info@fdossena.com) for other licensing models.
