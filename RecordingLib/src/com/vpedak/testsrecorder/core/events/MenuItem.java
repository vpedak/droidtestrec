package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class MenuItem extends Subject {
    private CharSequence title;
    private String id;

    public MenuItem(CharSequence title, String id) {
        this.title = title;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
