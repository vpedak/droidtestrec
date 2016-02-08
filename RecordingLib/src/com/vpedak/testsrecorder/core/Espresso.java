package com.vpedak.testsrecorder.core;

import com.vpedak.testsrecorder.core.events.Subject;

public class Espresso extends Subject {

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
