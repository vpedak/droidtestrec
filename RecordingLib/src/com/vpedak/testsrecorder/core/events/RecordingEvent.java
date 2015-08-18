package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class RecordingEvent {
    private Subject subject;
    private Action action;
    private String description;

    public RecordingEvent(Subject subject, Action action, String description) {
        this.subject = subject;
        this.action = action;
        this.description = description;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateEvent(sb, this);
    }

    @Override
    public String toString() {
        return "event=["+subject.toString()+","+action.toString()+","+description+"]";
    }

    public static RecordingEvent fromString(String str) {
        int pos = str.indexOf(',');
        int pos2 = str.lastIndexOf(',');
        String subjectStr = str.substring(7, pos);
        String actionStr = str.substring(pos+1, pos2);
        String description = str.substring(pos2+1, str.length()-1);
       return new RecordingEvent(Subject.fromString(subjectStr), Action.fromString(actionStr), description);
    }
}
