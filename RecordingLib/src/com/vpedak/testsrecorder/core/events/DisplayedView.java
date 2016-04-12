package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class DisplayedView extends Subject {
    private String id;

    public DisplayedView(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
