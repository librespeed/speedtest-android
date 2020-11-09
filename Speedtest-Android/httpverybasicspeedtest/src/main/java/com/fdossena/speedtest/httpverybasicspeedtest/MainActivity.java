package com.fdossena.speedtest.httpverybasicspeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SpeedTestListener
{
    Button button_run;
    TextView tvReport;
    UploadSpeedTest uploadSpeedTest;
    DownloadSpeedTest downloadSpeedTest;
    PingTest pingTest;
    GetIP getIP;
    FullSpeedTest fullSpeedTest;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        uploadSpeedTest = new UploadSpeedTest();
        downloadSpeedTest = new DownloadSpeedTest();
        pingTest = new PingTest();
        getIP = new GetIP();
        fullSpeedTest = new FullSpeedTest();
        setContentView(R.layout.activity_main);
        button_run = ((Button)findViewById(R.id.button_run));
        tvReport = ((TextView)findViewById(R.id.tv_report));
        tvReport.setMovementMethod(new ScrollingMovementMethod());
        button_run.setOnClickListener
                (
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                new Thread(new TaskSpeed()).start();
                            }
                        }
                );

    }
    public void speedTestLog(final String s)
    {

        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler .post
                (
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                tvReport.append(s+"\n");
                                System.out.println(s);
                            }
                        }
                );
    }

    @Override
    public void speedTestEnded()
    {
        SpeedTestResult res = fullSpeedTest.getResult();
        speedTestLog(res.toString());
    }

    private class TaskSpeed implements Runnable
    {
        @Override
        public void run()
        {
/*
                String  name = "Helsinki, Finland";
                String server = "//fi.openspeed.org";
                String downLoadPath = "garbage.php";
                String uploadPath = "empty.php";
                String pingPath = "empty.php";
                String getpath = "getIP.php";
*/
            String  name = "PlayIP";
            String server = "http://170.238.84.8:8080/";
            String downLoadPath = "/backend/garbage.php";
            String uploadPath = "/backend/empty.php";
            String pingPath = "/backend/empty.php";
            String getpath = "/backend/getIP.php";

/*
                String  name = "Local";
                String server = "http://192.168.12.43:8080";
                String downLoadPath = "backend/garbage.php";
                String uploadPath = "backend/empty.php";
                String pingPath = "backend/empty.php";
                String getpath = "backend/getIP.php";
*/


            TestPoint tp = new TestPoint(name, server, downLoadPath, uploadPath, pingPath, getpath);
/*
            downloadSpeedTest.test(tp.getHost(),  tp.getPort(), tp.getDownloadPath(), 10,
                    new SpeedTestListener()
                    {
                        @Override
                        public void speedTestEnded()
                        {
                              speedTestLog(downloadSpeedTest.getResult().toString());
                        }
                    },
                    null
            );
*/
            fullSpeedTest.test(tp, MainActivity.this, null);
            SpeedTestResult res = fullSpeedTest.getResult();
            speedTestLog(res.toString());

        }
    }

}
