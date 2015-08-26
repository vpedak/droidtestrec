package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class View extends Subject {
    private String id;

    public View(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
