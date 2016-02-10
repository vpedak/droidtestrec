package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class ScrollToPositionAction extends Action {
    private int position;

    public ScrollToPositionAction(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void accept(StringBuilder sb, TestGenerator generator, Subject subject){
        generator.generateActon(sb, this, subject);
    }
}
