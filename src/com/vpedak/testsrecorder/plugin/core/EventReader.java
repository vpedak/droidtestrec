package com.vpedak.testsrecorder.plugin.core;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatListener;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.logcat.LogCatReceiverTask;
import com.vpedak.testsrecorder.core.EventWriter;
import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventReader {
    private EventListener eventListener;
    private LogCatReceiverTask receiverTask;
    private AndroidDebugBridge bridge;
    private Thread thread;

    public EventReader(EventListener eventListener) {
        this.eventListener = eventListener;
        AndroidDebugBridge.init(false);
    }

    public void start(long uniqueId) {

        bridge = AndroidDebugBridge.createBridge();
        int count = 0;
        while (bridge.hasInitialDeviceList() == false) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException e) {
            }
            if (count > 100) {
                throw new RuntimeException("Timeout connecting to the adb!");
            }
        }

        IDevice[] devices = bridge.getDevices();
        if (devices.length == 0) {
            throw new RuntimeException("Failed to find device");
        }

        IDevice device = devices[0];

        receiverTask = new LogCatReceiverTask(device);

        final String tag = EventWriter.ANDRIOD_TEST_RECORDER+uniqueId;

        receiverTask.addLogCatListener(new LogCatListener() {
            @Override
            public void log(List<LogCatMessage> list) {
                for (LogCatMessage message : list) {
                    if (tag.equals(message.getTag())) {
                        String str = message.getMessage();
                        try {
                            RecordingEvent event = RecordingEvent.fromString(str);
                            eventListener.onRecordingEvent(event);
                        } catch (Exception e) {
                            System.out.println("Failed to parse event - " + str);
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread = new Thread(receiverTask);
        thread.start();
    }

    public void stop() {
        if (receiverTask != null) {
            receiverTask.stop();
        }
    }

    public static interface EventListener {
        void onRecordingEvent(RecordingEvent event);
    }
}
