package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public abstract class Action {

    public void accept(StringBuilder sb, TestGenerator generator, Subject subject){
        generator.generateActon(sb, this, subject);
    }

    @Override
    public String toString() {
        return Action.toString(this);
    }

    public static String toString(Action action) {
        if (action instanceof ClickAction) {
            return "action=click";
        } else if (action instanceof LongClickAction) {
            return "action=longclick";
        } else if (action instanceof ReplaceTextAction) {
            ReplaceTextAction replaceTextAction = (ReplaceTextAction) action;
            return "action=replaceText#"+replaceTextAction.getText().replace(",", "#comma#");
        } else {
            throw new RuntimeException("Unknown action - " + action.getClass());
        }
    }

    public static Action fromString(String str) {
        String action = str.substring(str.indexOf('=')+1);

        if (action.equals("click")) {
            return new ClickAction();
        } else if (action.equals("longclick")) {
            return  new LongClickAction();
        } else if (action.startsWith("replaceText")) {
            int pos = action.indexOf("#");
            String text = action.substring(pos+1).replace("#comma#", ",");
            return new ReplaceTextAction(text);
        } else {
            throw new RuntimeException("Unknown action - " + str);
        }
    }
}
