package com.vpedak.testsrecorder.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.*;

import com.vpedak.testsrecorder.core.events.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityProcessor {
    private long uniqueId;
    private final Instrumentation instrumentation;
    public static final String ANDRIOD_TEST_RECORDER = "Android Test Recorder";
    private ConcurrentHashMap<View, View> allViews = new ConcurrentHashMap<View, View>();
    private EventWriter eventWriter;
    private Activity activity;
    private MenuProcessor menuProcessor;
    private CheckableProcessor checkableProcessor;
    private AdapterViewProcessor adapterViewProcessor;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public ActivityProcessor(long uniqueId, Instrumentation instrumentation, EventWriter eventWriter) {
        this.uniqueId = uniqueId;
        this.instrumentation = instrumentation;
        this.eventWriter = eventWriter;
        menuProcessor = new MenuProcessor(this);
        checkableProcessor = new CheckableProcessor(this);
        adapterViewProcessor = new AdapterViewProcessor(this);
    }

    public EventWriter getEventWriter() {
        return eventWriter;
    }

    public void processActivity(final Activity activity) {
        this.activity = activity;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menuProcessor.processActivity(activity);

                View view = activity.getWindow().getDecorView();
                processView(view);
            }
        });
    }

    private void processViews(View[] views) {
        for (int i = 0; i < views.length; i++) {
            processView(views[i]);
        }
    }

    private void processView(View view) {
        View tst = allViews.putIfAbsent(view, view);

        if (tst != null) {
            return;
        }

        menuProcessor.processView(view);

        processClick(view);

        processTouch(view);


        if (view instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) view;
            processViewPager(viewPager);
        }

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

            ViewGroup.OnHierarchyChangeListener listener = null;
            try {
                Field f = ViewGroup.class.getDeclaredField("mOnHierarchyChangeListener");
                f.setAccessible(true);
                listener = (ViewGroup.OnHierarchyChangeListener) f.get(view);
            } catch (IllegalAccessException e) {
                Log.e(ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
            } catch (NoSuchFieldException e) {
                Log.e(ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
            }
            final ViewGroup.OnHierarchyChangeListener finalListener = listener;
            viewGroup.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, final View child) {

                    // make a delay in new view processing to make sure that all listeners will be already attached during Activity.OnCreate, etc
                    // not sure how to make this better (to wait for listeners attachment)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    processView(child);
                                }
                            }, 200);
                        }
                    });

                    if (finalListener != null) {
                        finalListener.onChildViewAdded(parent, child);
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                    if (finalListener != null) {
                        finalListener.onChildViewRemoved(parent, child);
                    }
                }
            });
        }
    }

    private void processViewPager(final ViewPager viewPager) {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                ResolveSubjectResult result = resolveSubject(viewPager);
                if (result != null) {
                    eventWriter.writeEvent(new RecordingEvent(result.getSubject(), new SelectViewPagerPageAction(position),
                            "Select page "+position+" in " + getWidgetName(viewPager)+generateSubjectDescription(result.getSubject())));
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
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

        final GestureDetector gestureDetector = new GestureDetector(instrumentation.getTargetContext(), new GestureDetector.OnGestureListener() {
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
                if (e1 == null || e2 == null) {
                    return false;
                }

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
                    AdapterView adapterView = getAdaptedView(view);

                    if (adapterView != null) {
                        // view is inside adapter view
                        int pos = adapterView.getPositionForView(view);
                        AdapterViewProcessor.generateEvent(ActivityProcessor.this, pos, adapterView, str + "item ", action);
                    } else {
                        ResolveSubjectResult result = resolveSubject(view);
                        if (result != null) {
                            String descr = str + getWidgetName(view) + generateSubjectDescription(result.getSubject());
                            if (result.getScrollToEvent() != null) {
                                result.getScrollToEvent().setDescription(descr);
                                eventWriter.writeEvent(result.getScrollToEvent());
                                eventWriter.writeEvent(new RecordingEvent(result.getScrollToEvent().getGroup(),result.getSubject(), action));
                            } else {
                                eventWriter.writeEvent(new RecordingEvent(result.getSubject(), action, descr));
                            }
                        }
                    }
                }

                return false;
            }
        });

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
                    gestureDetector.onTouchEvent(event);
                }

                return result;
            }
        });
    }

    private String generateSubjectDescription(Subject subject) {
        if (subject instanceof com.vpedak.testsrecorder.core.events.View) {
            com.vpedak.testsrecorder.core.events.View view = (com.vpedak.testsrecorder.core.events.View) subject;
            return " with id "+view.getId();
        } else if (subject instanceof  ParentView) {
            ParentView view = (ParentView) subject;
            return " with child index "+view.getChildIndex()+" of parent with id "+view.getParentId();
        }
        return "";
    }

    private void processTextView(final TextView view) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ResolveSubjectResult result = resolveSubject(view);
                if (result != null) {
                    String text = view.getText().toString();
                    String descr = "Set text to '" + text + "' in " + getWidgetName(view) + generateSubjectDescription(result.getSubject());
                    if (result.getScrollToEvent() != null) {
                        result.getScrollToEvent().setDescription(descr);
                        eventWriter.addDelayedEvent(result.getScrollToEvent());
                        eventWriter.addDelayedEvent(new RecordingEvent(result.getScrollToEvent().getGroup(),
                                result.getSubject(), new ReplaceTextAction(text)));
                    } else {
                        eventWriter.addDelayedEvent(new RecordingEvent(result.getSubject(), new ReplaceTextAction(text), descr));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void processClick(final View view) {
        if (view instanceof Spinner) {
            return;
        }

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

            Log.d("123", "view - " + view + " listener - " + listener);

            final View.OnClickListener finalListener = listener;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdapterView adapterView = getAdaptedView(view);

                    if (adapterView != null) {
                        // view is inside adapter view
                        int pos = adapterView.getPositionForView(view);
                        AdapterViewProcessor.generateClickEvent(ActivityProcessor.this, pos, adapterView);
                    } else {
                        ResolveSubjectResult result = resolveSubject(view);
                        if (result != null) {
                            String descr = "Click at " + getWidgetName(view) + generateSubjectDescription(result.getSubject());
                            if (result.getScrollToEvent() != null) {
                                result.getScrollToEvent().setDescription(descr);
                                eventWriter.writeEvent(result.getScrollToEvent());
                                eventWriter.writeEvent(new RecordingEvent(
                                        result.getScrollToEvent().getGroup(),
                                        result.getSubject(),
                                        new ClickAction()));
                            } else {
                                eventWriter.writeEvent(new RecordingEvent(result.getSubject(), new ClickAction(), descr));
                            }
                        }
                    }
                    finalListener.onClick(v);
                    processViews(getWMViews());
                }
            });
        }

        if (view.isLongClickable()) {
            final View.OnLongClickListener finalLongListener = longListener;
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AdapterView adapterView = getAdaptedView(view);

                    if (adapterView != null) {
                        // view is inside adapter view
                        int pos = adapterView.getPositionForView(view);
                        AdapterViewProcessor.generateLongClickEvent(ActivityProcessor.this, pos, adapterView);
                    } else {
                        ResolveSubjectResult result = resolveSubject(view);
                        if (result != null) {
                            String descr = "Long click at " + getWidgetName(view) + generateSubjectDescription(result.getSubject());
                            if (result.getScrollToEvent() != null) {
                                result.getScrollToEvent().setDescription(descr);
                                eventWriter.writeEvent(result.getScrollToEvent());
                                eventWriter.writeEvent(new RecordingEvent(
                                        result.getScrollToEvent().getGroup(),
                                        result.getSubject(), new LongClickAction()));
                            } else {
                                eventWriter.writeEvent(new RecordingEvent(result.getSubject(), new LongClickAction(), descr));
                            }
                        }

                    }

                    if (finalLongListener != null) {
                        return finalLongListener.onLongClick(v);
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

    private boolean isInsideScrollView(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof ScrollView || parent instanceof HorizontalScrollView) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    @Nullable
    public ResolveSubjectResult resolveSubject(View view) {
        String viewId = resolveId(view.getId());
        if (viewId != null) {
            try {
                if (view.getParent() instanceof RecyclerView) {
                    RecyclerView parentView = (RecyclerView) view.getParent();

                    String parentId = resolveId(parentView.getId());
                    if (parentId != null) {
                        for (int i = 0; i < parentView.getChildCount(); i++) {
                            if (view.equals(parentView.getChildAt(i))) {
                                RecordingEvent scrollToEvent =
                                        new RecordingEvent(String.valueOf(System.currentTimeMillis()),
                                                new com.vpedak.testsrecorder.core.events.View(parentId),
                                                new ScrollToPositionAction(i));
                                return new ResolveSubjectResult(new ParentView(parentId, i), scrollToEvent);
                            }
                        }
                    }
                }
            } catch (NoClassDefFoundError e) {
                // RecyclerView is not used in this project, ignore this
            }

            if (isInsideScrollView(view)) {
                RecordingEvent scrollToEvent = new RecordingEvent(String.valueOf(System.currentTimeMillis()),
                        new com.vpedak.testsrecorder.core.events.View(viewId),
                        new ScrollToAction());
                return new ResolveSubjectResult(new com.vpedak.testsrecorder.core.events.View(viewId), scrollToEvent);
            } else {
                return new ResolveSubjectResult(new com.vpedak.testsrecorder.core.events.View(viewId));
            }
        } else if (view.getParent() != null && view.getParent() instanceof ViewGroup) {
            ViewGroup parentView = (ViewGroup) view.getParent();
            String parentId = resolveId(parentView.getId());

            if (parentId != null) {
                for (int i = 0; i < parentView.getChildCount(); i++) {
                    if (view.equals(parentView.getChildAt(i))) {
                        RecordingEvent scrollToEvent = null;
                        try {
                            if (parentView instanceof RecyclerView) {
                                scrollToEvent =
                                        new RecordingEvent(String.valueOf(System.currentTimeMillis()),
                                                new com.vpedak.testsrecorder.core.events.View(parentId),
                                                new ScrollToPositionAction(i));
                            }
                        } catch (NoClassDefFoundError e) {
                            // RecyclerView is not used in this project, ignore this
                        }
                        return new ResolveSubjectResult(new ParentView(parentId, i), scrollToEvent);
                    }
                }
            }
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

    public static class ResolveSubjectResult {
        private Subject subject;
        private RecordingEvent scrollToEvent;

        public ResolveSubjectResult(Subject subject) {
            this.subject = subject;
        }

        public ResolveSubjectResult(Subject subject, RecordingEvent scrollToEvent) {
            this.subject = subject;
            this.scrollToEvent = scrollToEvent;
        }

        public Subject getSubject() {
            return subject;
        }

        public RecordingEvent getScrollToEvent() {
            return scrollToEvent;
        }
    }
}
