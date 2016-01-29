package com.vpedak.testsrecorder.core;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.IntentFilter;
import android.os.Looper;
import android.util.Log;
import android.util.Printer;

import java.util.Timer;
import java.util.TimerTask;

public class ActivityListener {
    private Instrumentation instr;
    private Instrumentation.ActivityMonitor monitor;
    private Activity activity = null;
    private ActivityProcessor activityProcessor;
    private long uniqueId;

    public ActivityListener(Instrumentation instr, Activity activity, long uniqueId) {
        this.instr = instr;
        IntentFilter filter = null;
        this.monitor = instr.addMonitor(filter, null, false);
        this.activity = activity;
        this.uniqueId = uniqueId;

        activityProcessor = new ActivityProcessor(uniqueId, instr);

        activityProcessor.processActivity(activity);
    }

    public void start() {
        new Timer().schedule(new ActivityTask(this), 200L, 200L);
    }

    public void check() {
        Activity test = monitor.getLastActivity();
        if (test != null && test != activity) {
            activity = test;

            activityProcessor.processActivity(activity);
        }
    }

    public static class ActivityTask extends TimerTask {
        private ActivityListener checker;

        public ActivityTask(ActivityListener checker) {
            this.checker = checker;
        }

        @Override
        public void run() {
            checker.check();
        }
    }
}
