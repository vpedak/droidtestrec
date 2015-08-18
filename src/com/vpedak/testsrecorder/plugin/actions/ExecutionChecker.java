package com.vpedak.testsrecorder.plugin.actions;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;

import java.util.List;
import java.util.TimerTask;

public class ExecutionChecker extends TimerTask {
    private ExecutionManager executionManager;
    private String runConfigName;
    private TestListener testListener;
    private RunContentDescriptor descriptor;

    public ExecutionChecker(ExecutionManager executionManager, String runConfigName, TestListener testListener) {
        this.executionManager = executionManager;
        this.runConfigName = runConfigName;
        this.testListener = testListener;
    }

    public RunContentDescriptor getDescriptor() {
        return this.descriptor;
    }

    public void run() {
        if (this.descriptor == null) {
            ProcessHandler[] processHandlers = this.executionManager.getRunningProcesses();
            if (processHandlers.length > 0) {
                List<RunContentDescriptor> descriptors = this.executionManager.getContentManager().getAllDescriptors();
                for (RunContentDescriptor tmp : descriptors) {
                    if (tmp.getDisplayName().equals(this.runConfigName)) {
                        this.descriptor = tmp;
                        this.testListener.testStarted();
                        break;
                    }
                }
            }
        }
    }
}
