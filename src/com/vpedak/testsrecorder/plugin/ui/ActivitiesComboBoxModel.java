package com.vpedak.testsrecorder.plugin.ui;

import com.intellij.ui.AbstractCollectionComboBoxModel;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ActivitiesComboBoxModel extends AbstractCollectionComboBoxModel<ActivitiesComboBoxModel.ActivityWrapper> {
    private List<ActivityWrapper> list;

    public ActivitiesComboBoxModel(List<Activity> activities, Activity selected) {
        super(selected == null ? null : new ActivityWrapper(selected));
        this.list = new ArrayList(activities.size());
        for (Activity activity : activities) {
            this.list.add(new ActivityWrapper(activity));
        }
    }

    @NotNull
    protected List<ActivityWrapper> getItems() {
        return list;
    }

    public static class ActivityWrapper {
        private Activity activity;

        public ActivityWrapper(Activity activity) {
            this.activity = activity;
        }

        public Activity getActivity() {
            return this.activity;
        }

        public String toString() {
            return this.activity.getActivityClass().toString();
        }
    }
}
