package com.vpedak.testsrecorder.core;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.IntentFilter;
import android.os.Looper;
import android.util.Log;
import android.util.Printer;
import com.vpedak.testsrecorder.core.events.PressBackAction;
import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.lang.reflect.Field;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityListener implements EventWriter.EventWriterListener {
    private Instrumentation instr;
    private volatile Instrumentation.ActivityMonitor monitor;
    private Activity activity = null;
    private ActivityProcessor activityProcessor;
    private long uniqueId;
    private boolean wasEvent = false;
    private EventWriter eventWriter;
    private Stack<Activity> stack = new Stack<Activity>();

    private Field fResumed;
    private Field fStopped;

    public ActivityListener(Instrumentation instr, Activity activity, long uniqueId) {
        this.instr = instr;
        IntentFilter filter = null;
        this.monitor = instr.addMonitor(filter, null, false);
        this.activity = activity;
        this.uniqueId = uniqueId;

        eventWriter = new EventWriter(uniqueId, this);
        activityProcessor = new ActivityProcessor(uniqueId, instr, eventWriter);

        activityProcessor.processActivity(activity);

        try {
            fResumed = Activity.class.getDeclaredField("mResumed");
            fResumed.setAccessible(true);

            fStopped = Activity.class.getDeclaredField("mStopped");
            fStopped.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }
    }

    public void start() {
        new Timer().schedule(new ActivityTask(this), 300L, 300L);
    }

    public synchronized void check() {
        Activity test = monitor.getLastActivity();
        if (test != null && test != activity && !isStopped(test)) {
            boolean push = true;
            if (isResumed(test)) {
                try {
                    if (stack.peek().equals(test) ) {
                        stack.pop();
                        push = false;

                        if (!wasEvent) {
                            eventWriter.writeEvent(new RecordingEvent(new Espresso(), new PressBackAction(), "Press on the back button"));
                        }
                    }
                } catch (EmptyStackException e) {
                    //no op;
                }
            }

            if (push) {
                stack.push(activity);
            }
            activity = test;

            //Log.i("12345", activity.getLocalClassName());

            activityProcessor.processActivity(activity);
        }
        wasEvent = false;

        activityProcessor.processAllViews();
    }

    @Override
    public synchronized void onEventWritten() {
        wasEvent = true;
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


    private boolean isResumed(Activity activity) {
        try {
            return (Boolean) fResumed.get(activity);
        } catch (IllegalAccessException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
            return false;
        }
    }

    private boolean isStopped(Activity activity) {
        try {
            return (Boolean) fStopped.get(activity);
        } catch (IllegalAccessException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
            return false;
        }
    }
}
