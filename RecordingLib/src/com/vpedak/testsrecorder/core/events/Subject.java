package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.Espresso;
import com.vpedak.testsrecorder.core.TestGenerator;

public abstract class Subject {
    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }

    @Override
    public String toString() {
        return Subject.toString(this);
    }

    public static String toString(Subject subject) {
        if (subject instanceof View) {
            View view = (View) subject;
            return "view=" + view.getId();
        } else if (subject instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) subject;
            return "menuItem=" + menuItem.getId() + "#" + menuItem.getTitle();
        } else if (subject instanceof ParentView) {
            ParentView parentView = (ParentView) subject;
            return "parentView=" + parentView.getParentId() + "#" + parentView.getChildIndex()+"#"+parentView.isGeneratetScrollToPosition();
        } else if (subject instanceof OptionsMenu) {
            return "optionsMenu";
        } else if (subject instanceof Espresso) {
            return "espresso";
        } else if (subject instanceof Data) {
            Data data = (Data) subject;
            return "data="+data.getAdapterId()+"#"+data.getClassName()+(data.getValue()==null?"":"#"+data.getValue());
        } else {
            throw new RuntimeException("Unknown subject - " + subject.getClass());
        }
    }

    public static Subject fromString(String str) {
        if (str.startsWith("view")) {
            String id = str.substring(str.indexOf("=") + 1);
            return new View(id);
        } else if (str.startsWith("menuItem")) {
            String tmp = str.substring(str.indexOf("=") + 1);
            int pos = tmp.indexOf("#");
            String id = tmp.substring(0, pos);
            String title = tmp.substring(pos+1, tmp.length());
            return new MenuItem(title, id);
        } else if (str.startsWith("parentView")) {
            String tmp = str.substring(str.indexOf("=") + 1);
            String[] arr = tmp.split("[#]");
            String parentId = arr[0];
            String idxStr = arr[1];
            int idx = Integer.parseInt(idxStr);
            String scrollSupportedStr = arr[2];
            boolean scrollSupported = Boolean.parseBoolean(scrollSupportedStr);
            return new ParentView(parentId, idx, scrollSupported);
        } else if (str.startsWith("optionsMenu")) {
            return new OptionsMenu();
        } else if (str.startsWith("espresso")) {
            return new Espresso();
        } else if (str.startsWith("data")) {
            String tmp = str.substring(str.indexOf("=") + 1);
            int pos1 = tmp.indexOf("#");
            String adapterId = tmp.substring(0, pos1);
            int pos2 = tmp.indexOf("#", pos1+1);
            if (pos2 != -1) {
                String className = tmp.substring(pos1+1, pos2);
                String value = tmp.substring(pos2 + 1, tmp.length());
                return new Data(adapterId, className, value);
            } else {
                return new Data(adapterId, tmp.substring(pos1+1, tmp.length()));
            }
        } else {
            throw new RuntimeException("Unknown subject - " + str);
        }
    }
}
