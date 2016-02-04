package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class ParentView extends Subject {
    private String parentId;
    private int childIdx;
    private boolean generatetScrollToPosition = false;

    public ParentView(String parentId, int childIdx, boolean supportScrollToPosition) {
        this.parentId = parentId;
        this.childIdx = childIdx;
        this.generatetScrollToPosition = supportScrollToPosition;
    }

    public String getParentId() {
        return parentId;
    }

    public int getChildIndex() {
        return childIdx;
    }

    public boolean isGeneratetScrollToPosition() {
        return generatetScrollToPosition;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
