package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class OptionsMenu extends Subject {

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
