package com.vpedak.testsrecorder.core;

import com.vpedak.testsrecorder.core.events.Subject;

public class Data extends Subject {
    private String className;
    private String value;

    public Data(String className) {
        this.className = className;
    }
    public Data(String className, String value) {
        this.className = className;
        this.value = value;
    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
