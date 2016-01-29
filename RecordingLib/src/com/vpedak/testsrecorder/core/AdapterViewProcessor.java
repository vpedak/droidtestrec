package com.vpedak.testsrecorder.core;

import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;

import com.vpedak.testsrecorder.core.events.*;

public class AdapterViewProcessor {
    private ActivityProcessor activityProcessor;

    public AdapterViewProcessor(ActivityProcessor activityProcessor) {
        this.activityProcessor = activityProcessor;
    }

    public void processView(final AdapterView adapterView) {
        final AdapterView.OnItemClickListener listener = adapterView.getOnItemClickListener();
        final AdapterView.OnItemLongClickListener longListener = adapterView.getOnItemLongClickListener();

        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                generateClickEvent(activityProcessor, position, adapterView);

                if (listener != null) {
                    listener.onItemClick(parent, view, position, id);
                }
            }
        });

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

    public static void generateClickEvent(ActivityProcessor activityProcessor, int position, AdapterView adapterView) {
        generateEvent(activityProcessor, position, adapterView, "Click at item", new ClickAction());
    }

    public static void generateLongClickEvent(ActivityProcessor activityProcessor, int position, AdapterView adapterView) {
        generateEvent(activityProcessor, position, adapterView, "Long click at item", new LongClickAction());
    }

    public static void generateEvent(ActivityProcessor activityProcessor, int position, AdapterView adapterView, String str, Action action) {
        String adapterId = activityProcessor.resolveId(adapterView.getId());

        if (adapterId != null) {
            Object tmp = adapterView.getItemAtPosition(position);

            String descr = str + " with value '" + tmp.toString() + "' in " + activityProcessor.getWidgetName(adapterView);
            Data data;
            if (tmp instanceof String) {
                data = new Data(adapterId, String.class.getName(), tmp.toString());
            } else if (tmp instanceof Cursor) { // CursorAdapter support
                data = new Data(adapterId, Cursor.class.getName());
            } else {
                data = new Data(adapterId, tmp.getClass().getName());
            }
            activityProcessor.getEventWriter().writeEvent(new RecordingEvent(data, action, descr));
        }
    }
}
