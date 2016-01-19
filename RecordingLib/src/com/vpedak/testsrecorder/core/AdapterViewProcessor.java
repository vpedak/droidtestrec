package com.vpedak.testsrecorder.core;

import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import com.vpedak.testsrecorder.core.events.ClickAction;
import com.vpedak.testsrecorder.core.events.LongClickAction;
import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.lang.reflect.Field;

public class AdapterViewProcessor {
    private ActivityProcessor activityProcessor;

    public AdapterViewProcessor(ActivityProcessor activityProcessor) {
        this.activityProcessor = activityProcessor;
    }

    public void processView(final AdapterView adapterView) {
        final AdapterView.OnItemClickListener listener = adapterView.getOnItemClickListener();
        final AdapterView.OnItemLongClickListener longListener = adapterView.getOnItemLongClickListener();

        if (listener != null) {
            adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    generateClickEvent(activityProcessor, position, adapterView);

                    if (listener != null) {
                        listener.onItemClick(parent, view, position, id);
                    }
                }
            });
        }

        if (longListener != null) {
            adapterView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    generateLongClickEvent(activityProcessor, position, adapterView);

                    if (longListener != null) {
                        return longListener.onItemLongClick(parent, view, position, id);
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    public static void generateClickEvent(ActivityProcessor activityProcessor, int position, AdapterView adapterView) {
        Object tmp = adapterView.getItemAtPosition(position);

        Data data = null;
        String descr = "Click at item with value '"+tmp.toString()+"' in " + activityProcessor.getWidgetName(adapterView);
        if (tmp instanceof String) {
            data = new Data(String.class.getName(), tmp.toString());
        } else if (tmp instanceof Cursor) { // CursorAdapter support
            data = new Data(Cursor.class.getName());
        } else {
            data = new Data(tmp.getClass().getName());
        }
        activityProcessor.getEventWriter().writeEvent(new RecordingEvent(data, new ClickAction(), descr));
    }

    public static void generateLongClickEvent(ActivityProcessor activityProcessor, int position, AdapterView adapterView) {
        Object tmp = adapterView.getItemAtPosition(position);

        String descr = "Long click at item with value '" + tmp.toString() + "' in " + activityProcessor.getWidgetName(adapterView);
        Data data;
        if (tmp instanceof String) {
            data = new Data(String.class.getName(), tmp.toString());
        } else if (tmp instanceof Cursor) { // CursorAdapter support
            data = new Data(Cursor.class.getName());
        } else {
            data = new Data(tmp.getClass().getName());
        }
        activityProcessor.getEventWriter().writeEvent(new RecordingEvent(data, new LongClickAction(), descr));
    }

}
