package com.vpedak.testsrecorder.core;

import com.vpedak.testsrecorder.core.events.*;

import java.util.List;

public interface TestGenerator {
    String generate(String activityClassName, String testClassName, String packageName, List<RecordingEvent> events);

    void generateEvent(StringBuilder sb, RecordingEvent event);

    void generateSubject(StringBuilder sb, Subject subject);
    void generateSubject(StringBuilder sb, View subject);
    void generateSubject(StringBuilder sb, OptionsMenu subject);
    void generateSubject(StringBuilder sb, MenuItem subject);
    void generateSubject(StringBuilder sb, ParentView subject);
    void generateSubject(StringBuilder sb, Data subject);
    void generateSubject(StringBuilder sb, Espresso subject);

    void generateActon(StringBuilder sb, Action action, Subject subject);
    void generateActon(StringBuilder sb, ClickAction action, Subject subject);
    void generateActon(StringBuilder sb, LongClickAction action, Subject subject);
    void generateActon(StringBuilder sb, ReplaceTextAction action, Subject subject);
    void generateActon(StringBuilder sb, SwipeUpAction action, Subject subject);
    void generateActon(StringBuilder sb, SwipeDownAction action, Subject subject);
    void generateActon(StringBuilder sb, SwipeLeftAction action, Subject subject);
    void generateActon(StringBuilder sb, SwipeRightAction action, Subject subject);
    void generateActon(StringBuilder sb, PressBackAction action, Subject subject);
    void generateActon(StringBuilder sb, ScrollToPositionAction action, Subject subject);
    void generateActon(StringBuilder sb, SelectViewPagerPageAction action, Subject subject);
}
