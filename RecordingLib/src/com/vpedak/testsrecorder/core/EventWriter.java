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
    private List<RecordingEvent> delayedEvents = new ArrayList<RecordingEvent>();

    public EventWriter(long uniqueId) {
        this.uniqueId = uniqueId;
        tag = ANDRIOD_TEST_RECORDER+uniqueId;
    }

    public synchronized void writeEvent(RecordingEvent event) {
        if (delayedEvents.size() > 0) {
            for(RecordingEvent delayedEvent : delayedEvents) {
                Log.d(tag, delayedEvent.toString());
            }
            delayedEvents.clear();
        }
        Log.d(tag, event.toString());
    }

    public synchronized void addDelayedEvent(RecordingEvent delayedEvent) {
        delayedEvents.add(delayedEvent);
    }
}
