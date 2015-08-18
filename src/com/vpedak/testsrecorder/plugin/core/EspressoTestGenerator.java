package com.vpedak.testsrecorder.plugin.core;

import com.vpedak.testsrecorder.core.TestGenerator;
import com.vpedak.testsrecorder.core.events.*;

import java.util.List;

public class EspressoTestGenerator implements TestGenerator {
    @Override
    public String generate(String activityClassName, String testClassName, String packageName, List<RecordingEvent> events) {
        Templates templates = Templates.getInstance();
        StringBuilder sb = new StringBuilder(templates.getTemplate("test_main"));
        replace(sb, "{PACKAGE}", packageName);
        replace(sb, "{TEST_CLASS}", testClassName);
        replace(sb, "{ACTIVITY_CLASS}", activityClassName);
        replace(sb, "{BODY}", generateBody(events));
        return sb.toString();
    }

    @Override
    public void generateSubject(StringBuilder sb, Subject subject) {
        // do nothing
    }

    @Override
    public void generateActon(StringBuilder sb, Action action, Subject subject) {
        // do nothing
    }


    @Override
    public void generateSubject(StringBuilder sb, View subject) {
        sb.append("onView(withId(").append(subject.getId()).append(")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, OptionsMenu subject) {
        sb.append("openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())");
    }

    @Override
    public void generateSubject(StringBuilder sb, MenuItem subject) {
        sb.append("onView(withText(\"").append(subject.getTitle()).append("\")).");
    }

    @Override
    public void generateActon(StringBuilder sb, ClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(click())");
        }
    }

    @Override
    public void generateActon(StringBuilder sb, LongClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(longClick())");
        }
    }

    private String generateBody(List<RecordingEvent> events) {
        StringBuilder sb = new StringBuilder();
        for(RecordingEvent event : events) {
            event.accept(sb, this);
            sb.append(";\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    public void generateEvent(StringBuilder sb, RecordingEvent event) {
        sb.append("\t// ").append(event.getDescription()).append('\n');
        event.getSubject().accept(sb, this);
        event.getAction().accept(sb, this, event.getSubject());
    }

    private StringBuilder replace(StringBuilder sb, String from, String to) {
        int index = -1;
        while ((index = sb.lastIndexOf(from)) != -1) {
            sb.replace(index, index + from.length(), to);
        }
        return sb;
    }
}
