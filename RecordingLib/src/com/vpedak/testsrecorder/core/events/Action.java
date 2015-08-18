package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public abstract class Action {

    public void accept(StringBuilder sb, TestGenerator generator, Subject subject){
        generator.generateActon(sb, this, subject);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static Action fromString(String str) {
        String action = str.substring(str.indexOf('=')+1);

        if (action.equals("click")) {
            return new ClickAction();
        } else if (action.equals("longclick")) {
            return  new LongClickAction();
        }

        throw new RuntimeException("Unknown action - " + str);
    }
}
