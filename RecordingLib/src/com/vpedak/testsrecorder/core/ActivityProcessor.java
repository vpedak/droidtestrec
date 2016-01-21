package com.vpedak.testsrecorder.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import com.vpedak.testsrecorder.core.events.Action;
import com.vpedak.testsrecorder.core.events.ClickAction;
import com.vpedak.testsrecorder.core.events.LongClickAction;
import com.vpedak.testsrecorder.core.events.RecordingEvent;
import com.vpedak.testsrecorder.core.events.ReplaceTextAction;
import com.vpedak.testsrecorder.core.events.SwipeDownAction;
import com.vpedak.testsrecorder.core.events.SwipeLeftAction;
import com.vpedak.testsrecorder.core.events.SwipeRightAction;
import com.vpedak.testsrecorder.core.events.SwipeUpAction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ActivityProcessor {
    private long uniqueId;
    private final Instrumentation instrumentation;
    public static final String ANDRIOD_TEST_RECORDER = "Andriod Test Recorder";
    private List<View> allViews = new ArrayList<View>();
    private EventWriter eventWriter;
    private Activity activity;
    private MenuProcessor menuProcessor;
    private CheckableProcessor checkableProcessor;
    private AdapterViewProcessor adapterViewProcessor;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public ActivityProcessor(long uniqueId, Instrumentation instrumentation) {
        this.uniqueId = uniqueId;
        this.instrumentation = instrumentation;
        eventWriter = new EventWriter(uniqueId);
        menuProcessor = new MenuProcessor(this);
        checkableProcessor = new CheckableProcessor(this);
        adapterViewProcessor = new AdapterViewProcessor(this);
    }

    public EventWriter getEventWriter() {
        return eventWriter;
    }

    public void processActivity(Activity activity) {
        this.activity = activity;

        menuProcessor.processActivity(activity);

        View view = activity.getWindow().getDecorView();
        processView(view);
    }

    private void processViews(View[] views) {
        for (int i = 0; i < views.length; i++) {
            processView(views[i]);
        }
    }

    private void processView(View view) {

        if (allViews.contains(view)) {
            return;
        }

        menuProcessor.processView(view);

        processClick(view);

        processTouch(view);

        if (view instanceof EditText) {
            processTextView((TextView) view);
        } else if (view instanceof AdapterView) {
            adapterViewProcessor.processView((AdapterView) view);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);

                processView(child);
            }
        }

        allViews.add(view);
    }

    private void processTouch(final View view) {
        View.OnTouchListener listener = null;
        try {
            Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(view);

            if (li != null) {
                Field f2 = li.getClass().getDeclaredField("mOnTouchListener");
                f2.setAccessible(true);
                listener = (View.OnTouchListener) f2.get(li);

            }
        } catch (IllegalAccessException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }

        final GestureDetectorRunnable runnable = new GestureDetectorRunnable(view);
        activity.runOnUiThread(runnable);

        final View.OnTouchListener finalListener = listener;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = false;

                if (finalListener != null) {
                    result = finalListener.onTouch(v, event);
                } else {
                    result = view.onTouchEvent(event);
                }

                if (result) {
                    runnable.getGestureDetector().onTouchEvent(event);
                }

                return result;
            }
        });
    }

    private class GestureDetectorRunnable implements Runnable {
        private View view;
        private GestureDetector gestureDetector;

        public GestureDetectorRunnable(View view) {
            this.view = view;
        }

        public GestureDetector getGestureDetector() {
            return gestureDetector;
        }

        @Override
        public void run() {
            gestureDetector = new GestureDetector(instrumentation.getTargetContext(), new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    Action action = null;
                    String str = null;

                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // swipe right
                                action = new SwipeRightAction();
                                str = "Swipe right at ";
                            } else {
                                // swipe left
                                action = new SwipeLeftAction();
                                str = "Swipe left at ";
                            }
                        }
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // swipe down
                            action = new SwipeDownAction();
                            str = "Swipe down at ";
                        } else {
                            // swipe up
                            action = new SwipeUpAction();
                            str = "Swipe up at ";
                        }
                    }

                    if (action != null) {
                        String viewId = resolveId(view.getId());
                        if (viewId != null) {
                            AdapterView adapterView = getAdaptedView(view);

                            if (adapterView != null) {
                                // view is inside adapter view
                                int pos = adapterView.getPositionForView(view);
                                AdapterViewProcessor.generateEvent(ActivityProcessor.this, pos, adapterView, str+"item ", action);
                            } else {
                                String descr = str + getWidgetName(view) + " with id " + viewId;
                                eventWriter.writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(viewId), action, descr));
                            }
                        }
                    }

                    return false;
                }
            });
        }
    };

    private void processTextView(final TextView view) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String id = resolveId(view.getId());
                if (id != null) {
                    String text = view.getText().toString();
                    String descr = "Set text to '" + text + "' in " + getWidgetName(view) + " with id " + id;
                    eventWriter.addDelayedEvent(id, new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(id), new ReplaceTextAction(text), descr));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void processClick(final View view) {
        View.OnClickListener listener = null;
        View.OnLongClickListener longListener = null;
        try {
            Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(view);

            if (li != null) {
                Field f2 = li.getClass().getDeclaredField("mOnClickListener");
                f2.setAccessible(true);
                listener = (View.OnClickListener) f2.get(li);

                Field f3 = li.getClass().getDeclaredField("mOnLongClickListener");
                f3.setAccessible(true);
                longListener = (View.OnLongClickListener) f3.get(li);
            }
        } catch (IllegalAccessException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            Log.e(ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }

        if (view.isClickable() && listener != null) {
            final View.OnClickListener finalListener = listener;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String viewId = resolveId(view.getId());
                    if (viewId != null) {

                        AdapterView adapterView = getAdaptedView(view);

                        if (adapterView != null) {
                            // view is inside adapter view
                            int pos = adapterView.getPositionForView(view);
                            AdapterViewProcessor.generateClickEvent(ActivityProcessor.this, pos, adapterView);
                        } else {
                            String descr = "Click at " + getWidgetName(view) + " with id " + viewId;
                            eventWriter.writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(viewId), new ClickAction(), descr));
                        }

                        finalListener.onClick(v);
                        processViews(getWMViews());
                    }
                }
            });
        }

        if (view.isLongClickable()) {
            final View.OnLongClickListener finalLongListener = longListener;
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String viewId = resolveId(view.getId());
                    if (viewId != null) {

                        AdapterView adapterView = getAdaptedView(view);

                        if (adapterView != null) {
                            // view is inside adapter view
                            int pos = adapterView.getPositionForView(view);
                            AdapterViewProcessor.generateLongClickEvent(ActivityProcessor.this, pos, adapterView);
                        } else {
                            String descr = "Long click at " + getWidgetName(view) + " with id " + viewId;
                            eventWriter.writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.View(viewId), new LongClickAction(), descr));
                        }

                        if (finalLongListener != null) {
                            return finalLongListener.onLongClick(v);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            });
        }

        if (view instanceof CompoundButton) {
            checkableProcessor.processClick((CompoundButton) view);
        }
    }

    private AdapterView getAdaptedView(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof AdapterView) {
                return (AdapterView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    public String resolveId(int id) {
        if (id < 0) {
            return null;
        }

        try {
            String viewId = activity.getResources().getResourceEntryName(id);
            String pkg = activity.getResources().getResourcePackageName(id);
            String type = activity.getResources().getResourceTypeName(id);
            if (pkg.equals("android")) {
                viewId = "android.R." + type + "." + viewId;
            } else {
                viewId = "R." + type + "." + viewId;
            }
            return viewId;
        } catch (Resources.NotFoundException e) {
            return String.valueOf(id);
        }
    }

    public String getWidgetName(View view) {
        return view.getClass().getSimpleName();
    }

    static String wmFieldName;
    static Class wmClass;

    static {
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                wmFieldName = "sDefaultWindowManager";
                wmClass = Class.forName("android.view.WindowManagerGlobal");
            } else if (Build.VERSION.SDK_INT >= 13) {
                wmFieldName = "sWindowManager";
                wmClass = Class.forName("android.view.WindowManagerImpl");
            } else {
                wmFieldName = "mWindowManager";
                wmClass = Class.forName("android.view.WindowManagerImpl");
            }
        } catch (ClassNotFoundException localClassNotFoundException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't find android.view.WindowManagerImpl - fatal!", localClassNotFoundException);
        } catch (SecurityException localSecurityException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get android.view.WindowManagerImpl", localSecurityException);
        }
    }

    private static View[] getWMViews() {
        try {
            Field localField = wmClass.getDeclaredField("mViews");
            Object localObject = wmClass.getDeclaredField(wmFieldName);
            localField.setAccessible(true);
            ((Field) localObject).setAccessible(true);
            localObject = ((Field) localObject).get(null);
            if (Build.VERSION.SDK_INT < 19) {
                return (View[]) localField.get(localObject);
            }
            return (View[]) ((ArrayList) localField.get(localObject)).toArray(new View[0]);
        } catch (SecurityException localSecurityException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localSecurityException);
        } catch (NoSuchFieldException localNoSuchFieldException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localNoSuchFieldException);
        } catch (IllegalArgumentException localIllegalArgumentException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localIllegalArgumentException);
        } catch (IllegalAccessException localIllegalAccessException) {
            Log.e(ANDRIOD_TEST_RECORDER, "Couldn't get decor views!", localIllegalAccessException);
        }

        return null;
    }
}
