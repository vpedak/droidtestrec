package com.vpedak.testsrecorder.plugin.core;

import com.vpedak.testsrecorder.core.Espresso;
import com.vpedak.testsrecorder.core.events.Data;
import com.vpedak.testsrecorder.core.TestGenerator;
import com.vpedak.testsrecorder.core.events.*;

import java.util.List;


public class EspressoTestGenerator implements TestGenerator {
    private boolean wasParentView = false;
    private boolean wasScrollToPosition = false;
    private boolean wasSelectViewPagerPage = false;
    private boolean generateDelays = false;

    public EspressoTestGenerator(boolean generateDelays) {
        this.generateDelays = generateDelays;
    }

    @Override
    public String generate(String activityClassName, String testClassName, String packageName, List<RecordingEvent> events) {
        Templates templates = Templates.getInstance();
        StringBuilder sb = new StringBuilder(templates.getTemplate("test_main"));
        replace(sb, "{PACKAGE}", packageName);
        replace(sb, "{TEST_CLASS}", testClassName);
        replace(sb, "{ACTIVITY_CLASS}", activityClassName);
        replace(sb, "{BODY}", generateBody(events));

        StringBuilder additions = new StringBuilder();

        if (wasParentView) {
            additions.append("\n\n").append(nthChildOfTemplate);
        }
        if (wasScrollToPosition) {
            additions.append("\n\n").append(scrollToPositionTemplate);
        }
        if (wasSelectViewPagerPage) {
            additions.append("\n\n").append(selectViewPagerPageTemplate);
        }
        if (generateDelays) {
            additions.append("\n\n").append(idlingResourceTemplate);
        }

        if (additions.length() > 0) {
            replace(sb, "{ADDITION}", additions.toString());
        } else {
            replace(sb, "{ADDITION}", "");
        }

        return sb.toString();
    }

    @Override
    public void generateSubject(StringBuilder sb, Subject subject) {
        // do nothing
    }

    @Override
    public void generateActon(StringBuilder sb, Action action, Subject subject) {
        // do nothing
    }


    @Override
    public void generateSubject(StringBuilder sb, View subject) {
        sb.append("onView(withId(").append(subject.getId()).append(")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, OptionsMenu subject) {
        sb.append("openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())");
    }

    @Override
    public void generateSubject(StringBuilder sb, MenuItem subject) {
        sb.append("onView(withText(\"").append(subject.getTitle()).append("\")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, ParentView subject) {
        wasParentView = true;
        sb.append("onView(nthChildOf(withId(").append(subject.getParentId()).append("), ").append(subject.getChildIndex()).append(")).");
    }

    @Override
    public void generateSubject(StringBuilder sb, Data subject) {
        if (subject.getValue() != null) {
            sb.append("onData(allOf(is(instanceOf(").append(subject.getClassName()).append(".class)), is(\"").
                    append(subject.getValue()).append("\"))).inAdapterView(withId(").append(subject.getAdapterId()).
                    append(")).");
        } else {
            StringBuilder tmp = new StringBuilder(dataTemplate);
            replace( tmp, "CLASS", subject.getClassName());
            replace( tmp, "ADAPTER_ID", subject.getAdapterId());
            sb.append(tmp);
        }
    }

    @Override
    public void generateSubject(StringBuilder sb, Espresso subject) {
    }



    @Override
    public void generateActon(StringBuilder sb, ClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(click())");
        }
    }

    @Override
    public void generateActon(StringBuilder sb, LongClickAction action, Subject subject) {
        if (!(subject instanceof OptionsMenu)) {
            sb.append("perform(longClick())");
        }
    }

    @Override
    public void generateActon(StringBuilder sb, ReplaceTextAction action, Subject subject) {
        sb.append("perform(replaceText(\""+action.getText()+"\"))");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeUpAction action, Subject subject) {
        sb.append("perform(swipeUp())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeDownAction action, Subject subject) {
        sb.append("perform(swipeDown())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeLeftAction action, Subject subject) {
        sb.append("perform(swipeLeft())");
    }

    @Override
    public void generateActon(StringBuilder sb, SwipeRightAction action, Subject subject) {
        sb.append("perform(swipeRight())");
    }

    @Override
    public void generateActon(StringBuilder sb, PressBackAction action, Subject subject) {
        sb.append("pressBack()");
    }

    @Override
    public void generateActon(StringBuilder sb, ScrollToPositionAction action, Subject subject) {
        wasScrollToPosition = true;
        sb.append("perform(scrollToPosition("+action.getPosition()+"))");
    }

    @Override
    public void generateActon(StringBuilder sb, SelectViewPagerPageAction action, Subject subject) {
        wasSelectViewPagerPage = true;
        sb.append("perform(selectViewPagerPage("+action.getPosition()+"))");
    }

    @Override
    public void generateActon(StringBuilder sb, ScrollToAction action, Subject subject) {
        sb.append("perform(scrollTo())");
    }

    private String generateBody(List<RecordingEvent> events) {
        StringBuilder sb = new StringBuilder();

        if (generateDelays) {
            sb.append("\t// Used to provide time delays between actions, see details at http://droidtestlab.com/delay.html \n");
            sb.append("IdlingResource idlingResource;\n");
        }

        for(RecordingEvent event : events) {
            event.accept(sb, this);
        }

        if (generateDelays) {
            sb.append("stopTiming(idlingResource);\n");
        }
        return sb.toString();
    }

    private String currentGroup = null;
    boolean firstEvent = true;

    public void generateEvent(StringBuilder sb, RecordingEvent event) {
        if (event.getGroup() != null) {
            if (!event.getGroup().equals(currentGroup)) {
                if (generateDelays) {
                    if (!firstEvent) {
                        sb.append("stopTiming(idlingResource);\n");
                    }
                    sb.append("\n");
                    sb.append("idlingResource = startTiming(").append(calculateIdleTime(event)).append(");\n");
                } else {
                    sb.append("\n");
                }
                currentGroup = event.getGroup();
            }
        } else {
            if (generateDelays) {
                if (!firstEvent) {
                    sb.append("stopTiming(idlingResource);\n");
                }
                sb.append("\n");
                sb.append("idlingResource = startTiming(").append(calculateIdleTime(event)).append(");\n");
            } else {
                sb.append("\n");
            }

            currentGroup = null;
        }

        if (event.getDescription() != null) {
            sb.append("\t// ").append(event.getDescription()).append('\n');
        }

        event.getSubject().accept(sb, this);
        event.getAction().accept(sb, this, event.getSubject());
        sb.append(";\n");

        firstEvent = false;
    }

    private StringBuilder replace(StringBuilder sb, String from, String to) {
        int index = -1;
        while ((index = sb.lastIndexOf(from)) != -1) {
            sb.replace(index, index + from.length(), to);
        }
        return sb;
    }

    private long calculateIdleTime(RecordingEvent event) {
        return (long) (Math.ceil(event.getTime()/100) * 100);
    }

    private String dataTemplate =
            "\t// see details at http://droidtestlab.com/adapterView.html\n" +
            "onData(allOf(is(new BoundedMatcher<Object, CLASS>(CLASS.class) {" +
            "    @Override" +
            "    public void describeTo(Description description) {" +
            "    }" +
            "    @Override" +
            "    protected boolean matchesSafely(CLASS obj) {\n" +
            "        /* TODO Implement comparison logic, see details at http://droidtestlab.com/adapterView.html  */\n" +
            "        return false;" +
            "    }" +
            "}))).inAdapterView(withId(ADAPTER_ID)).";

    /* TODO: If need it is possible to access List or Grids by position:
    onData(allOf(is(instanceOf(<DATA>.class))))
            .inAdapterView(withId(<LIST_GRID_ID>))
            .atPosition(0)
    .perform(click());
    */

    private String nthChildOfTemplate =
            "    public static Matcher<View> nthChildOf(final Matcher<View> parentMatcher, final int childPosition) {\n" +
            "        return new TypeSafeMatcher<View>() {\n" +
            "            @Override\n" +
            "            public void describeTo(Description description) {\n" +
            "            }\n" +
            "            @Override\n" +
            "            public boolean matchesSafely(View view) {\n" +
            "                if (!(view.getParent() instanceof ViewGroup)) {\n" +
            "                    return false;\n" +
            "                }\n" +
            "                ViewGroup group = (ViewGroup) view.getParent();\n" +
            "                return parentMatcher.matches(group) && view.equals(group.getChildAt(childPosition));\n" +
            "            }\n" +
            "        };\n" +
            "    }\n";

    private String scrollToPositionTemplate =
            "    public static ViewAction scrollToPosition(final int pos) {\n" +
                    "        return new ViewAction() {\n" +
                    "            @Override\n" +
                    "            public Matcher<View> getConstraints() {\n" +
                    "                return isAssignableFrom(android.support.v7.widget.RecyclerView.class);\n" +
                    "            }\n" +
                    "            @Override\n" +
                    "            public String getDescription() {\n" +
                    "                return \"scroll to position\";\n" +
                    "            }\n" +
                    "            @Override\n" +
                    "            public void perform(UiController uiController, View view) {\n" +
                    "                ((android.support.v7.widget.RecyclerView)view).scrollToPosition(pos);\n" +
                    "            }\n" +
                    "        };\n" +
                    "    }";

    private String selectViewPagerPageTemplate =
            "    public static ViewAction selectViewPagerPage(final int pos) {\n" +
                    "        return new ViewAction() {\n" +
                    "            @Override\n" +
                    "            public Matcher<View> getConstraints() {\n" +
                    "                return isAssignableFrom(android.support.v4.view.ViewPager.class);\n" +
                    "            }\n" +
                    "            @Override\n" +
                    "            public String getDescription() {\n" +
                    "                return \"select page in ViewPager\";\n" +
                    "            }\n" +
                    "            @Override\n" +
                    "            public void perform(UiController uiController, View view) {\n" +
                    "                ((android.support.v4.view.ViewPager)view).setCurrentItem(pos);\n" +
                    "            }\n" +
                    "        };\n" +
                    "    }\n";

    private String idlingResourceTemplate =
            "\t // See details at http://droidtestlab.com/delay.html \n" +
            "    public IdlingResource startTiming(long time) {\n" +
                    "        IdlingResource idlingResource = new ElapsedTimeIdlingResource(time);\n" +
                    "        Espresso.registerIdlingResources(idlingResource);\n" +
                    "        return idlingResource;\n" +
                    "    }\n" +
                    "    public void stopTiming(IdlingResource idlingResource) {\n" +
                    "        Espresso.unregisterIdlingResources(idlingResource);\n" +
                    "    }\n" +
                    "    public class ElapsedTimeIdlingResource implements IdlingResource {\n" +
                    "        private long startTime;\n" +
                    "        private final long waitingTime;\n" +
                    "        private ResourceCallback resourceCallback;\n" +
                    "\n" +
                    "        public ElapsedTimeIdlingResource(long waitingTime) {\n" +
                    "            this.startTime = System.currentTimeMillis();\n" +
                    "            this.waitingTime = waitingTime;\n" +
                    "        }\n" +
                    "\n" +
                    "        @Override\n" +
                    "        public String getName() {\n" +
                    "            return ElapsedTimeIdlingResource.class.getName() + \":\" + waitingTime;\n" +
                    "        }\n" +
                    "\n" +
                    "        @Override\n" +
                    "        public boolean isIdleNow() {\n" +
                    "            long elapsed = System.currentTimeMillis() - startTime;\n" +
                    "            boolean idle = (elapsed >= waitingTime);\n" +
                    "            if (idle) {\n" +
                    "                resourceCallback.onTransitionToIdle();\n" +
                    "            }\n" +
                    "            return idle;\n" +
                    "        }\n" +
                    "\n" +
                    "        @Override\n" +
                    "        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {\n" +
                    "            this.resourceCallback = resourceCallback;\n" +
                    "        }\n" +
                    "    }";
}
