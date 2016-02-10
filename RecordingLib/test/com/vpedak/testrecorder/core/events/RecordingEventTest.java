package com.vpedak.testrecorder.core.events;

import com.vpedak.testsrecorder.core.events.ClickAction;
import com.vpedak.testsrecorder.core.events.Data;
import com.vpedak.testsrecorder.core.events.RecordingEvent;
import com.vpedak.testsrecorder.core.events.View;
import org.junit.Assert;
import org.junit.Test;

public class RecordingEventTest {

    @Test
    public void testSerialization() {
        RecordingEvent event1 = new RecordingEvent(new Data("adapterId", "className", "value"), new ClickAction(), "description text");
        Assert.assertEquals("event=[,data=adapterId#className#value,action=click,description text]", event1.toString());

        RecordingEvent event2 = new RecordingEvent(new View("viewId"), new ClickAction(), "description text");
        event2.setGroup("grp");
        Assert.assertEquals("event=[grp,view=viewId,action=click,description text]", event2.toString());

        RecordingEvent event3 = new RecordingEvent("grp", new View("viewId"), new ClickAction());
        Assert.assertEquals("event=[grp,view=viewId,action=click,]", event3.toString());
    }

    @Test
    public void testDeserialization() {
        RecordingEvent event1 = RecordingEvent.fromString("event=[,data=adapterId#className#value,action=click,description text]");
        Assert.assertEquals("description text", event1.getDescription());
        Assert.assertTrue(event1.getSubject() instanceof Data);
        Assert.assertEquals("adapterId", ((Data)event1.getSubject()).getAdapterId());
        Assert.assertEquals("className", ((Data)event1.getSubject()).getClassName());
        Assert.assertEquals("value", ((Data)event1.getSubject()).getValue());
        Assert.assertTrue(event1.getAction() instanceof ClickAction);

        RecordingEvent event2 = RecordingEvent.fromString("event=[grp,view=viewId,action=click,description text]");
        Assert.assertEquals("grp", event2.getGroup());
        Assert.assertEquals("description text", event2.getDescription());
        Assert.assertTrue(event2.getSubject() instanceof View);
        Assert.assertEquals("viewId", ((View)event2.getSubject()).getId());
        Assert.assertTrue(event2.getAction() instanceof ClickAction);

        RecordingEvent event3 = RecordingEvent.fromString("event=[grp,view=viewId,action=click,]");
        Assert.assertNull(event3.getDescription());
        Assert.assertEquals("grp", event3.getGroup());
        Assert.assertTrue(event3.getSubject() instanceof View);
        Assert.assertEquals("viewId", ((View)event3.getSubject()).getId());
        Assert.assertTrue(event3.getAction() instanceof ClickAction);

        RecordingEvent event4 = RecordingEvent.fromString("event=[,view=viewId,action=click,]");
        Assert.assertNull(event4.getDescription());
        Assert.assertNull(event4.getGroup());
        Assert.assertTrue(event4.getSubject() instanceof View);
        Assert.assertEquals("viewId", ((View)event4.getSubject()).getId());
        Assert.assertTrue(event4.getAction() instanceof ClickAction);
    }
}
