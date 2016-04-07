package com.vpedak.testsrecorder.core;

import android.util.Log;

import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventWriter {
    private long uniqueId;
    public static final String ANDRIOD_TEST_RECORDER = "AndriodTestRecorder";
    public String tag;
    private Map<String, RecordingEvent> delayedEvents = new HashMap<String, RecordingEvent>();
    private EventWriterListener listener;
    private long lastTime;

    public EventWriter(long uniqueId, EventWriterListener listener) {
        this.uniqueId = uniqueId;
        this.listener = listener;
        tag = ANDRIOD_TEST_RECORDER+uniqueId;
        lastTime = System.currentTimeMillis();
    }

    public synchronized void writeEvent(RecordingEvent event) {
        long diff = System.currentTimeMillis() - lastTime;
        lastTime = System.currentTimeMillis();

        if (delayedEvents.size() > 0) {
            for(RecordingEvent delayedEvent : delayedEvents.values()) {
                delayedEvent.setTime(diff);
                Log.d(tag, delayedEvent.toString());
            }
            delayedEvents.clear();
        }
        event.setTime(diff);
        Log.d(tag, event.toString());
        listener.onEventWritten();
    }

    public synchronized void addDelayedEvent(String id, RecordingEvent delayedEvent) {
        delayedEvents.put(id, delayedEvent);
    }

    public interface EventWriterListener {
        void onEventWritten();
    }
}
