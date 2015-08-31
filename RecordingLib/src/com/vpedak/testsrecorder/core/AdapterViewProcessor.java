package com.vpedak.testsrecorder.core;

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
                    Object tmp = parent.getItemAtPosition(position);

                    String descr = "Click at item in " + activityProcessor.getWidgetName(adapterView);
                    Data data = null;
                    if (tmp instanceof String) {
                        data = new Data(String.class.getName(), tmp.toString());
                        descr += " with value '"+tmp.toString()+"'";
                    } else {
                        data = new Data(tmp.getClass().getName());
                    }
                    activityProcessor.getEventWriter().writeEvent(new RecordingEvent(data, new ClickAction(), descr));

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
                    Object tmp = parent.getItemAtPosition(position);

                    String descr = "Long click at item in " + activityProcessor.getWidgetName(adapterView);
                    Data data;
                    if (tmp instanceof String) {
                        data = new Data(String.class.getName(), tmp.toString());
                        descr += " with value '"+tmp.toString()+"'";
                    } else {
                        data = new Data(tmp.getClass().getName());
                    }
                    activityProcessor.getEventWriter().writeEvent(new RecordingEvent(data, new LongClickAction(), descr));

                    if (longListener != null) {
                        return longListener.onItemLongClick(parent, view, position, id);
                    } else {
                        return false;
                    }
                }
            });
        }
    }

}
