package com.vpedak.testsrecorder.core;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ActionMenuView;

import com.vpedak.testsrecorder.core.events.*;

import java.lang.reflect.Field;


public class MenuProcessor {
    private ActivityProcessor activityProcessor;

    public MenuProcessor(ActivityProcessor activityProcessor) {
        this.activityProcessor = activityProcessor;
    }


    public void processView(View view) {
        View.OnCreateContextMenuListener listener = null;
        try {
            Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(view);

            if (li != null) {
                Field f2 = li.getClass().getDeclaredField("mOnCreateContextMenuListener");
                f2.setAccessible(true);
                listener = (View.OnCreateContextMenuListener) f2.get(li);
            }
        } catch (IllegalAccessException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
        } catch (NoSuchFieldException e) {
            Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
        }

        if (listener != null) {
            final View.OnCreateContextMenuListener finalListener = listener;
            view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    finalListener.onCreateContextMenu(menu, v, menuInfo);
                    processMenu(menu);
                }
            });
        }
    }

    public void processActivity(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            processActionBar(actionBar);
        } else if (activity instanceof AppCompatActivity) {
            android.support.v7.app.ActionBar supportActionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (supportActionBar != null) {
                processSupportActionBar(supportActionBar);
            }
        }

        ViewGroup menuView = findActionBar(activity);
        if (menuView != null) {
            processMenuView(menuView);
        }
    }

    private void processMenuView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            try {
                if (child instanceof ActionMenuView) {
                    processActionMenuView((ActionMenuView) child);
                } else if (child instanceof android.support.v7.widget.ActionMenuView) {
                    processSupportActionMenuView((android.support.v7.widget.ActionMenuView) child);
                }
            } catch (NoClassDefFoundError e) {
                // not a error com.android.support:appcompat-v7 is not used in project
            }
        }
    }

    private void processSupportActionMenuView(android.support.v7.widget.ActionMenuView menuView) {
        Menu menu = menuView.getMenu();
        processMenu(menu);
    }

    @TargetApi(21)
    private void processActionMenuView(ActionMenuView menuView) {
        Menu menu = menuView.getMenu();
        processMenu(menu);
    }

    // see android.support.v7.internal.view.menu.MenuItemImpl.invoke()
    private void processMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            try {
                Field f = item.getClass().getDeclaredField("mClickListener");
                f.setAccessible(true);
                Object obj = f.get(item);
                final MenuItem.OnMenuItemClickListener menuItemClickListener = (MenuItem.OnMenuItemClickListener) obj;
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String menuId = activityProcessor.resolveId(item.getItemId());

                        if (menuId != null) {
                            String descr = "Click at menu item with text '" + item.getTitle() + "' and id " + menuId;
                            activityProcessor.getEventWriter().writeEvent(new RecordingEvent(new com.vpedak.testsrecorder.core.events.MenuItem(item.getTitle(), menuId), new ClickAction(), descr));
                            if (menuItemClickListener == null) {
                                return false;
                            } else {
                                return menuItemClickListener.onMenuItemClick(item);
                            }
                        } else {
                            return false;
                        }
                    }
                });
            } catch (NoSuchFieldException e) {
                Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "NoSuchFieldException", e);
            } catch (IllegalAccessException e) {
                Log.e(ActivityProcessor.ANDRIOD_TEST_RECORDER, "IllegalAccessException", e);
            }
        }
    }

    private void processSupportActionBar(android.support.v7.app.ActionBar supportActionBar) {
        supportActionBar.addOnMenuVisibilityListener(new android.support.v7.app.ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                if (isVisible) {
                    String descr = "Open options menu";
                    activityProcessor.getEventWriter().writeEvent(new RecordingEvent(new OptionsMenu(), new ClickAction(), descr));
                }
            }
        });
    }

    private void processActionBar(ActionBar actionBar) {
        actionBar.addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                if (isVisible) {
                    String descr = "Open options menu";
                    activityProcessor.getEventWriter().writeEvent(new RecordingEvent(new OptionsMenu(), new ClickAction(), descr));
                }
            }
        });
    }

    public ViewGroup findActionBar(Activity activity) {
        int id = activity.getResources().getIdentifier("action_bar", "id", "android");
        ViewGroup actionBar = null;
        if (id != 0) {
            actionBar = (ViewGroup) activity.findViewById(id);
        }
        if (actionBar == null) {
            actionBar = findToolbar((ViewGroup) activity.findViewById(android.R.id.content)
                    .getRootView());
        }
        return actionBar;
    }

    private ViewGroup findToolbar(ViewGroup viewGroup) {
        ViewGroup toolbar = null;
        for (int i = 0, len = viewGroup.getChildCount(); i < len; i++) {
            View view = viewGroup.getChildAt(i);
            if (view.getClass().getName().equals("android.support.v7.widget.Toolbar")
                    || view.getClass().getName().equals("android.widget.Toolbar")) {
                toolbar = (ViewGroup) view;
            } else if (view instanceof ViewGroup) {
                toolbar = findToolbar((ViewGroup) view);
            }
            if (toolbar != null) {
                break;
            }
        }
        return toolbar;
    }

}
