package com.vpedak.testsrecorder.core;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.vpedak.testsrecorder.core.events.ClickAction;
import com.vpedak.testsrecorder.core.events.RecordingEvent;

import java.lang.reflect.Field;

public class CheckableProcessor {
    private ActivityProcessor activityProcessor;

    public CheckableProcessor(ActivityProcessor activityProcessor) {
        this.activityProcessor = activityProcessor;
    }

    public void processClick(final CompoundButton checkable) {
        CompoundButton.OnCheckedChangeListener listener = null;
        try {
            Field f = View.class.getDeclaredField("mOnCheckedChangeListener");
            f.setAccessible(true);
            listener = (CompoundButton.OnCheckedChangeListener) f.get(checkable);
        } catch (IllegalAccessException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }

        final CompoundButton.OnCheckedChangeListener finalListener = listener;
        checkable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String id = activityProcessor.resolveId(buttonView.getId());
                if (id != null) {
                    String descr = (isChecked ? "Check " : "Uncheck ") + activityProcessor.getWidgetName(buttonView) + " with id "+id;
                    activityProcessor.getEventWriter().writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(id), new ClickAction(), descr));
                }

                if (finalListener != null) {
                    finalListener.onCheckedChanged(buttonView, isChecked);
                }
            }
        });
    }

}
