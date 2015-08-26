package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class ReplaceTextAction extends Action {
    private String text;

    public ReplaceTextAction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void accept(StringBuilder sb, TestGenerator generator, Subject subject){
        generator.generateActon(sb, this, subject);
    }
}
