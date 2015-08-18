package com.vpedak.testsrecorder.core;

import android.util.Log;

import com.vpedak.testsrecorder.core.events.RecordingEvent;

public class EventWriter {
    private long uniqueId;
    public static final String ANDRIOD_TEST_RECORDER = "AndriodTestRecorder";
    public String tag;

    public EventWriter(long uniqueId) {
        this.uniqueId = uniqueId;
        tag = ANDRIOD_TEST_RECORDER+uniqueId;
    }

    public void writeEvent(RecordingEvent event) {
        Log.d(tag, event.toString());
    }
}
