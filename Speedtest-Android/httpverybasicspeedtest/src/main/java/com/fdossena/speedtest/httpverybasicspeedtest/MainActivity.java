package com.fdossena.speedtest.httpverybasicspeedtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements DonwUpSpeedTest.SpeedTestLog
{
    Button button_run;
    TextView tvReport;
    DonwUpSpeedTest exampleSpeedTest;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        exampleSpeedTest = new DonwUpSpeedTest();
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
    @Override
    public void speedTestlog(final String s)
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
                exampleSpeedTest.test(MainActivity.this);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

}
