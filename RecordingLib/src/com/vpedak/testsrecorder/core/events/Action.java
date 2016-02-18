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
        } else if (action instanceof ScrollToPositionAction) {
            ScrollToPositionAction positionAction = (ScrollToPositionAction) action;
            return "action=scrollToPosition#"+positionAction.getPosition();
        } else if (action instanceof SelectViewPagerPageAction) {
            SelectViewPagerPageAction positionAction = (SelectViewPagerPageAction) action;
            return "action=selectViewPagerPage#"+positionAction.getPosition();
        } else if (action instanceof SwipeUpAction) {
            return "action=swipeup";
        } else if (action instanceof SwipeDownAction) {
            return "action=swipedown";
        } else if (action instanceof SwipeLeftAction) {
            return "action=swipeleft";
        } else if (action instanceof SwipeRightAction) {
            return "action=swiperight";
        } else if (action instanceof PressBackAction) {
            return "action=pressback";
        } else if (action instanceof ScrollToAction) {
            return "action=scrollTo";
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
        } else if (action.startsWith("scrollToPosition")) {
            int pos = action.indexOf("#");
            String posStr = action.substring(pos+1);
            return new ScrollToPositionAction(Integer.parseInt(posStr));
        } else if (action.startsWith("selectViewPagerPage")) {
            int pos = action.indexOf("#");
            String posStr = action.substring(pos+1);
            return new SelectViewPagerPageAction(Integer.parseInt(posStr));
        } else if (action.equals("swipeup")) {
            return  new SwipeUpAction();
        } else if (action.equals("swipedown")) {
            return  new SwipeDownAction();
        } else if (action.equals("swipeleft")) {
            return  new SwipeLeftAction();
        } else if (action.equals("swiperight")) {
            return  new SwipeRightAction();
        } else if (action.equals("pressback")) {
            return  new PressBackAction();
        } else if (action.equals("scrollTo")) {
            return  new ScrollToAction();
        } else {
            throw new RuntimeException("Unknown action - " + str);
        }
    }
}
