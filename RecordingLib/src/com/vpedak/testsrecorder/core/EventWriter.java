package com.vpedak.testsrecorder.core;

import android.util.Log;

import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.util.HashMap;
import java.util.Map;

public class EventWriter {
    private long uniqueId;
    public static final String ANDRIOD_TEST_RECORDER = "AndriodTestRecorder";
    public String tag;
    private Map<String, RecordingEvent> delayedEvents = new HashMap<String, RecordingEvent>();

    public EventWriter(long uniqueId) {
        this.uniqueId = uniqueId;
        tag = ANDRIOD_TEST_RECORDER+uniqueId;
    }

    public void writeEvent(RecordingEvent event) {
        if (delayedEvents.size() > 0) {
            for(RecordingEvent delayedEvent : delayedEvents.values()) {
                Log.d(tag, delayedEvent.toString());
            }
            delayedEvents.clear();
        }
        Log.d(tag, event.toString());
    }

    public void addDelayedEvent(String id, RecordingEvent delayedEvent) {
        delayedEvents.put(id, delayedEvent);
    }
}
