package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class RecordingEvent {
    private String group = null;
    private Subject subject;
    private Action action;
    private String description;

    public RecordingEvent(String group, Subject subject, Action action) {
        this.group = group;
        this.subject = subject;
        this.action = action;
    }

    public RecordingEvent(Subject subject, Action action, String description) {
        this.subject = subject;
        this.action = action;
        this.description = description;
    }

    public Subject getSubject() {
        return subject;
    }

    public Action getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateEvent(sb, this);
    }

    @Override
    public String toString() {
        return "event=["+(getGroup()==null?"":getGroup())+","+subject.toString()+","+action.toString()+"," +(description==null?"":description)+"]";
    }

    public static RecordingEvent fromString(String str) {
        String tmp = str.substring(7, str.length()-1);
        String arr[] = tmp.split("[,]");

        String group = arr[0];
        String subjectStr = arr[1];
        String actionStr = arr[2];
        String description = arr.length == 4 ? arr[3] : null;
        RecordingEvent event = new RecordingEvent(Subject.fromString(subjectStr), Action.fromString(actionStr), description);

        if (group.length() > 0) {
            event.setGroup(group);
        }

        return event;
    }
}
