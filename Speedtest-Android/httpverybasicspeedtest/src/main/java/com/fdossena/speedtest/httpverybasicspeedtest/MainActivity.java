package com.fdossena.speedtest.httpverybasicspeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    Button button_run;
    TextView tvReport;
    UploadSpeedTest uploadSpeedTest;
    DownloadSpeedTest downloadSpeedTest;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        uploadSpeedTest = new UploadSpeedTest();
        downloadSpeedTest = new DownloadSpeedTest();
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
    private class TaskSpeed implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                uploadSpeedTest.test
                ("170.238.84.8", 8080, "/backend/empty.php", 10,
                        new SpeedTestListener()
                        {
                            @Override
                            public void speedTestEnded()
                            {
                                MainActivity.this.speedTestLog("Upload "+uploadSpeedTest.getResult().toString());
                            }
                        }
                );
                downloadSpeedTest.test
                ("170.238.84.8", 8080, "/backend/garbage.php", 10,
                        new SpeedTestListener()
                        {
                            @Override
                            public void speedTestEnded()
                            {
                                MainActivity.this.speedTestLog("Download "+downloadSpeedTest.getResult().toString());
                            }
                        }
                );
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

}
