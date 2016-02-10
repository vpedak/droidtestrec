package com.vpedak.testrecorder.core.events;

import com.vpedak.testsrecorder.core.Espresso;
import com.vpedak.testsrecorder.core.events.*;
import org.junit.Assert;
import org.junit.Test;

public class SubjectTest {

    @Test
    public void testSerialization() {
        View view = new View("viewId");
        Assert.assertEquals("view=viewId", view.toString());

        MenuItem menuItem = new MenuItem("title", "menuId");
        Assert.assertEquals("menuItem=menuId#title", menuItem.toString());

        ParentView parentView = new ParentView("parentId", 1);
        Assert.assertEquals("parentView=parentId#1", parentView.toString());

        OptionsMenu optionsMenu = new OptionsMenu();
        Assert.assertEquals("optionsMenu", optionsMenu.toString());

        Espresso espresso = new Espresso();
        Assert.assertEquals("espresso", espresso.toString());

        Data data1 = new Data("adapterId", "className");
        Assert.assertEquals("data=adapterId#className", data1.toString());

        Data data2 = new Data("adapterId", "className", "value");
        Assert.assertEquals("data=adapterId#className#value", data2.toString());
    }

    @Test
    public void testDeserialization() {
        View view = (View) Subject.fromString("view=viewId");
        Assert.assertEquals("viewId", view.getId());

        MenuItem menuItem = (MenuItem) Subject.fromString("menuItem=menuId#title");
        Assert.assertEquals("menuId", menuItem.getId());
        Assert.assertEquals("title", menuItem.getTitle());

        ParentView parentView = (ParentView) Subject.fromString("parentView=parentId#1");
        Assert.assertEquals("parentId", parentView.getParentId());
        Assert.assertEquals(1, parentView.getChildIndex());

        OptionsMenu optionsMenu = (OptionsMenu) Subject.fromString("optionsMenu");

        Espresso espresso = (Espresso) Subject.fromString("espresso");

        Data data1 = (Data) Subject.fromString("data=adapterId#className");
        Assert.assertEquals("adapterId", data1.getAdapterId());
        Assert.assertEquals("className", data1.getClassName());

        Data data2 = (Data) Subject.fromString("data=adapterId#className#value");
        Assert.assertEquals("adapterId", data2.getAdapterId());
        Assert.assertEquals("className", data2.getClassName());
        Assert.assertEquals("value", data2.getValue());
    }
}
