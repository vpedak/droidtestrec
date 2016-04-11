package com.vpedak.testsrecorder.plugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;
import java.awt.*;

public class PluginConfiguration implements Configurable {
    public static final String LEAVE_RUN_CONFIGURATION = "android.test.recorder.leave.run.configuration";

    private JPanel rootPanel;
    private JCheckBox leaveRunConfigurationsCB;

    @Override
    public String getDisplayName() {
        return "Android Test Recorder";
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        rootPanel = new JPanel(new BorderLayout());

        leaveRunConfigurationsCB = new JCheckBox("leave Run Configuration used for recording (for debug purpose)");
        rootPanel.add(leaveRunConfigurationsCB, BorderLayout.NORTH);

        return rootPanel;
    }

    @Override
    public boolean isModified() {
        return leaveRunConfigurationsCB.isSelected() != isLeaveRunConfiguration();
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent.getInstance().setValue(LEAVE_RUN_CONFIGURATION, leaveRunConfigurationsCB.isSelected()?"1":"0");
    }

    @Override
    public void reset() {
        leaveRunConfigurationsCB.setSelected(isLeaveRunConfiguration());
    }

    @Override
    public void disposeUIResources() {
    }

    public static boolean isLeaveRunConfiguration() {
        int tst = PropertiesComponent.getInstance().getOrInitInt(LEAVE_RUN_CONFIGURATION, 0);
        return tst == 1;
    }
}
