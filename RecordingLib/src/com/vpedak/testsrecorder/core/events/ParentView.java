package com.vpedak.testsrecorder.core.events;

import com.vpedak.testsrecorder.core.TestGenerator;

public class ParentView extends Subject {
    private String parentId;
    private int childIdx;

    public ParentView(String parentId, int childIdx) {
        this.parentId = parentId;
        this.childIdx = childIdx;
    }

    public String getParentId() {
        return parentId;
    }

    public int getChildIndex() {
        return childIdx;
    }

    public void accept(StringBuilder sb, TestGenerator generator){
        generator.generateSubject(sb, this);
    }
}
