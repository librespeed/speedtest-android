![LibreSpeed-Android Logo](https://github.com/adolfintel/speedtest-android/blob/master/.github/Readme-Logo.png?raw=true)
 
# LibreSpeed Java Library
The LibreSpeed Java Library allows you to performs a speedtest using your existing [LibreSpeed](https://github.com/librespeed/speedtest) server(s) from any java application.
 
 
## Features
* Download
* Upload
* Ping
* Jitter
* IP Address, ISP, distance from server (optional)
* Telemetry (optional)
* Results sharing (optional)
* Multiple Points of Test (optional)


## Server requirements
One or more servers with [LibreSpeed](https://github.com/librespeed/speedtest) installed.

## Getting Started

Create a `Speedtest` instance:

```java
Speedtest speedtest = new Speedtest();
```

Add at least one `TestPoint` to your instance
```java
TestPoint testPoint = new TestPoint(
        "Testpoint name",
        "https://librespeed.org/",
        "backend/garbage.php",
        "backend/empty.php",
        "backend/empty.php",
        "backend/getIP.php"
);
speedtest.addTestPoint(testPoint);
```

Customize configuration (Optional)
```java
SpeedtestConfig config = new SpeedtestConfig();
config.setDl_ckSize(200);
// ...
speedtest.setSpeedtestConfig(config);
```

Add a telemetry config (Optional)
```java
TelemetryConfig telemetryConfig = new TelemetryConfig(
        "full",
        "https://librespeed.org",
        "results/telemetry.php",
        "results/?id=%s"
);
speedtest.setTelemetryConfig(telemetryConfig);
```

Create a `SpeedtestHandler`
```java
Speedtest.SpeedtestHandler speedtestHandler = new Speedtest.SpeedtestHandler() {
    @Override
    public void onTestIDReceived(String id, String shareURL) {
        log.info("Id: {} url: {}", id, shareURL);
    }

    @Override
    public void onDownloadUpdate(double speed, double progress) {
        log.info("Download: {} - {}", speed, progress);
    }

    @Override
    public void onUploadUpdate(double speed, double progress) {
        log.info("Upload: {} - {}", speed, progress);
    }

    @Override
    public void onPingJitterUpdate(double ping, double jitter, double progress) {
        log.info("Ping: {}:{} - {}", ping, jitter, progress);
    }

    @Override
    public void onIPInfoUpdate(String ipInfo) {
        log.info("IP Info: {}", ipInfo);
    }

    @Override
    public void onEnd() {
        log.info("Test finished");
    }

    @Override
    public void onCriticalFailure(String err) {
        log.error("Critical failure: {}", err);
    }
};
```

Start the test
```java
speedtest.start(speedtestHandler);
```


## Donate
[![Donate with Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/fdossena/donate)  
[Donate with PayPal](https://www.paypal.me/sineisochronic)  

## License
Copyright (C) 2023 Federico Dossena

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/lgpl>.
