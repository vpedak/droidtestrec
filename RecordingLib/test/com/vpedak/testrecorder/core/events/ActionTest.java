package com.vpedak.testrecorder.core.events;


import com.vpedak.testsrecorder.core.events.*;
import org.junit.Assert;
import org.junit.Test;

public class ActionTest {

    @Test
    public void testSerialization() {
        ClickAction clickAction = new ClickAction();
        Assert.assertEquals("action=click", clickAction.toString());

        LongClickAction longClickAction = new LongClickAction();
        Assert.assertEquals("action=longclick", longClickAction.toString());

        ReplaceTextAction replaceTextAction = new ReplaceTextAction("text");
        Assert.assertEquals("action=replaceText#text", replaceTextAction.toString());

        ScrollToPositionAction scrollToPositionAction = new ScrollToPositionAction(5);
        Assert.assertEquals("action=scrollToPosition#5", scrollToPositionAction.toString());

        SelectViewPagerPageAction selectViewPagerPageAction = new SelectViewPagerPageAction(5);
        Assert.assertEquals("action=selectViewPagerPage#5", selectViewPagerPageAction.toString());

        SwipeUpAction swipeUpAction = new SwipeUpAction();
        Assert.assertEquals("action=swipeup", swipeUpAction.toString());

        SwipeDownAction swipeDownAction = new SwipeDownAction();
        Assert.assertEquals("action=swipedown", swipeDownAction.toString());

        SwipeLeftAction swipeLeftAction = new SwipeLeftAction();
        Assert.assertEquals("action=swipeleft", swipeLeftAction.toString());

        SwipeRightAction swipeRightAction = new SwipeRightAction();
        Assert.assertEquals("action=swiperight", swipeRightAction.toString());

        PressBackAction pressBackAction = new PressBackAction();
        Assert.assertEquals("action=pressback", pressBackAction.toString());

        ScrollToAction scrollToAction = new ScrollToAction();
        Assert.assertEquals("action=scrollTo", scrollToAction.toString());
    }

    @Test
    public void testDeserialization() {
        ClickAction clickAction = (ClickAction) Action.fromString("action=click");

        LongClickAction longClickAction = (LongClickAction) Action.fromString("action=longclick");

        ReplaceTextAction replaceTextAction = (ReplaceTextAction) Action.fromString("action=replaceText#text");
        Assert.assertEquals("text", replaceTextAction.getText());

        ScrollToPositionAction scrollToPositionAction = (ScrollToPositionAction) Action.fromString("action=scrollToPosition#5");
        Assert.assertEquals(5, scrollToPositionAction.getPosition());

        SelectViewPagerPageAction selectViewPagerPageAction = (SelectViewPagerPageAction) Action.fromString("action=selectViewPagerPage#5");
        Assert.assertEquals(5, selectViewPagerPageAction.getPosition());

        SwipeUpAction swipeUpAction = (SwipeUpAction) Action.fromString("action=swipeup");

        SwipeDownAction swipeDownAction = (SwipeDownAction) Action.fromString("action=swipedown");

        SwipeLeftAction swipeLeftAction = (SwipeLeftAction) Action.fromString("action=swipeleft");

        SwipeRightAction swipeRightAction = (SwipeRightAction) Action.fromString("action=swiperight");

        PressBackAction pressBackAction = (PressBackAction) Action.fromString("action=pressback");

        ScrollToAction scrollToAction = (ScrollToAction) Action.fromString("action=scrollTo");
    }

}