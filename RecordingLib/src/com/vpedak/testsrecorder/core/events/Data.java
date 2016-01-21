package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class Data extends Subject {
    private String adapterId;
    private String className;
    private String value;

    public Data(String adapterId, String className) {
        this.adapterId = adapterId;
        this.className = className;
    }
    public Data(String adapterId, String className, String value) {
        this.adapterId = adapterId;
        this.className = className;
        this.value = value;
    }

    public String getAdapterId() {
        return adapterId;
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
