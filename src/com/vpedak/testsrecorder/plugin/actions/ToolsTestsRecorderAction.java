package com.vpedak.testsrecorder.plugin.actions;

import com.android.tools.idea.gradle.parser.BuildFileKey;
import com.android.tools.idea.gradle.parser.BuildFileStatement;
import com.android.tools.idea.gradle.parser.Dependency;
import com.android.tools.idea.gradle.parser.GradleBuildFile;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.vpedak.testsrecorder.plugin.core.EventReader;
import com.vpedak.testsrecorder.plugin.core.Templates;
import com.vpedak.testsrecorder.plugin.ui.ActivitiesComboBoxModel;
import com.vpedak.testsrecorder.plugin.ui.EventsList;
import com.vpedak.testsrecorder.plugin.ui.ModulesComboBoxModel;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.run.testing.AndroidTestRunConfiguration;
import org.jetbrains.android.run.testing.AndroidTestRunConfigurationType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ToolsTestsRecorderAction extends com.intellij.openapi.actionSystem.AnAction implements TestListener {
    public static final String TOOL_WINDOW_ID = "vpedak.tests.recorder,id";
    public static final String TEST_FILE_NAME = "AndrTestRec.java";
    public static final String ANDR_TEST_CLASSNAME = "AndrTestRec";
    public static final String RUN_CONFIG_NAME = "TestRecorderTemporary";
    public static final String RECORD = "Record";
    public static final String STOP = "Stop";
    public static final String TOOLWINDOW_TITLE = "Android Tests Recorder";
    private ModulesComboBoxModel moduleBoxModel;
    private ActivitiesComboBoxModel activitiesBoxModel;
    private ComboBox activitiesList;
    private JButton recButton;
    private EventsList eventsList;
    private ExecutionChecker executionChecker;
    private volatile ToolWindow toolWindow;
    private GradleBuildFile buildFile;
    private String jarPath;
    private Project project;
    private VirtualFile testVirtualFile;
    private static String template;
    private EventReader eventReader;
    private JLabel label;
    private SimpleToolWindowPanel panel;
    private long uniqueId;

    public ToolsTestsRecorderAction() {
        super("Android Tests _Recorder");
        this.jarPath = getJarPath();
        if (template == null) {
            template = Templates.getInstance().getTemplate("start_record");
        }
        eventsList = new EventsList();
        eventReader = new EventReader(eventsList);
    }

    public void actionPerformed(final AnActionEvent event) {
        Project project = (Project) event.getData(PlatformDataKeys.PROJECT);

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        this.toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        if (this.toolWindow == null) {
            this.toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, true, com.intellij.openapi.wm.ToolWindowAnchor.RIGHT, false);
            this.toolWindow.setTitle(TOOLWINDOW_TITLE);
            this.toolWindow.setStripeTitle(TOOLWINDOW_TITLE);
            this.toolWindow.setIcon(IconLoader.getIcon("icons/main.png"));
            this.toolWindow.setAutoHide(false);

            panel = new SimpleToolWindowPanel(true);

            JToolBar toolBar = new JToolBar();
            this.recButton = new JButton(RECORD, IconLoader.getIcon("icons/rec.png"));
            this.recButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (ToolsTestsRecorderAction.this.recButton.getText().equals(RECORD)) {
                        ToolsTestsRecorderAction.this.recButton.setText(STOP);
                        ToolsTestsRecorderAction.this.recButton.setIcon(IconLoader.getIcon("icons/stop.png"));
                        ToolsTestsRecorderAction.this.record(event);

                        panel.remove(label);
                        panel.add(eventsList);
                        panel.repaint();
                    } else {
                        ToolsTestsRecorderAction.this.recButton.setText(RECORD);
                        ToolsTestsRecorderAction.this.recButton.setIcon(IconLoader.getIcon("icons/rec.png"));
                        ToolsTestsRecorderAction.this.stop(event);
                    }
                }
            });
            toolBar.add(this.recButton);

            ModuleManager moduleManager = ModuleManager.getInstance(project);
            Module[] modules = moduleManager.getModules();
            Module currentModule = null;
            VirtualFile virtualFile = (VirtualFile) event.getData(PlatformDataKeys.VIRTUAL_FILE);
            if (virtualFile != null) {
                currentModule = com.intellij.openapi.roots.ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
            }
            this.recButton.setEnabled(false);

            this.moduleBoxModel = new ModulesComboBoxModel(modules, currentModule);
            ComboBox modList = new ComboBox(this.moduleBoxModel);
            modList.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    ToolsTestsRecorderAction.this.fillActivities((ModulesComboBoxModel.ModuleWrapper) ToolsTestsRecorderAction.this.moduleBoxModel.getSelected());
                }
            });
            modList.setMaximumSize(modules.length == 0 ? new Dimension(150, modList.getPreferredSize().height) : modList.getPreferredSize());
            JLabel modLabel = new JLabel("Module: ", SwingConstants.RIGHT);
            modLabel.setMaximumSize(new Dimension(modLabel.getPreferredSize().width + 20, modLabel.getPreferredSize().height));
            toolBar.add(modLabel);
            toolBar.add(modList);

            this.activitiesBoxModel = new ActivitiesComboBoxModel(Collections.<Activity>emptyList(), null);
            this.activitiesList = new ComboBox(this.activitiesBoxModel);
            this.activitiesList.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    ToolsTestsRecorderAction.this.recButton.setEnabled(ToolsTestsRecorderAction.this.activitiesBoxModel.getSelected() != null);
                }
            });
            this.activitiesList.setMaximumSize(new Dimension(150, this.activitiesList.getPreferredSize().height));
            JLabel activityLabel = new JLabel("Activity: ", SwingConstants.RIGHT);
            activityLabel.setMaximumSize(new Dimension(activityLabel.getPreferredSize().width + 20, activityLabel.getPreferredSize().height));
            toolBar.add(activityLabel);
            toolBar.add(this.activitiesList);

            fillActivities(currentModule == null ? null : new ModulesComboBoxModel.ModuleWrapper(currentModule));

            panel.setToolbar(toolBar);

            label = new JLabel("Select Module and Activity to start recording.");
            label.setHorizontalAlignment(JLabel.CENTER);
            panel.add(label);

            com.intellij.ui.content.Content toolContent = this.toolWindow.getContentManager().getFactory().createContent(panel, "", false);
            this.toolWindow.getContentManager().addContent(toolContent);
        }
        this.toolWindow.activate(null, true, true);
    }

    private void fillActivities(ModulesComboBoxModel.ModuleWrapper module) {
        List<Activity> activities = Collections.emptyList();
        Activity selected = null;

        if (module != null) {
            FacetManager facetManager = FacetManager.getInstance(module.getModule());
            com.intellij.facet.Facet[] facets = facetManager.getAllFacets();
            Iterator i$;
            Activity activity;
            for (int i = 0; i < facets.length; i++) {
                com.intellij.facet.Facet facet = facets[i];
                if ((facet instanceof AndroidFacet)) {
                    AndroidFacet androidFacet = (AndroidFacet) facet;
                    org.jetbrains.android.dom.manifest.Manifest manifest = androidFacet.getManifest();
                    activities = manifest.getApplication().getActivities();
                    for (i$ = activities.iterator(); i$.hasNext(); ) {
                        activity = (Activity) i$.next();
                        for (org.jetbrains.android.dom.manifest.IntentFilter filter : activity.getIntentFilters()) {
                            for (org.jetbrains.android.dom.manifest.Action action : filter.getActions()) {
                                if (action.getName().getValue().equals("android.intent.action.MAIN")) {
                                    selected = activity;
                                    this.activitiesBoxModel = new ActivitiesComboBoxModel(activities, selected);
                                    this.activitiesList.setModel(this.activitiesBoxModel);
                                    this.recButton.setEnabled(this.activitiesBoxModel.getSelected() != null);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        this.activitiesBoxModel = new ActivitiesComboBoxModel(activities, selected);
        this.activitiesList.setModel(this.activitiesBoxModel);
        this.recButton.setEnabled(false);
    }

    private void stop(AnActionEvent event) {
        if (this.executionChecker != null && this.executionChecker.getDescriptor() != null && this.executionChecker.getDescriptor().getProcessHandler() != null) {
            this.executionChecker.getDescriptor().getProcessHandler().destroyProcess();
        }

        if (this.buildFile != null) {
            final List<BuildFileStatement> dependencies = this.buildFile.getDependencies();
            final Dependency dependency = findDepRecord(dependencies);
            if (dependency != null) {
                CommandProcessor.getInstance().executeCommand(this.project, new Runnable() {
                    public void run() {
                        dependencies.remove(dependency);
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            public void run() {
                                ToolsTestsRecorderAction.this.buildFile.setValue(BuildFileKey.DEPENDENCIES, dependencies);
                            }
                        });
                    }
                }, null, null);
            }
        }
        if (this.testVirtualFile != null) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    try {
                        ToolsTestsRecorderAction.this.testVirtualFile.delete(null);
                    } catch (IOException e) {
                        Messages.showErrorDialog(ToolsTestsRecorderAction.this.project, "Failed to delete file " + ToolsTestsRecorderAction.this.testVirtualFile.getCanonicalPath(), "Error");
                        e.printStackTrace();
                    }
                }
            });
        }

        if (eventReader != null) {
            eventReader.stop();
        }
    }

    private void record(AnActionEvent event) {
        project = ((Project) event.getData(PlatformDataKeys.PROJECT));

        Module module = ((ModulesComboBoxModel.ModuleWrapper) this.moduleBoxModel.getSelected()).getModule();

        this.buildFile = GradleBuildFile.get(module);
        final List<BuildFileStatement> dependencies = this.buildFile.getDependencies();

        /*boolean espressoFound = false;
        for (BuildFileStatement statement : dependencies) {
            if ((statement instanceof Dependency)) {
                Dependency dependency = (Dependency) statement;
                if ((dependency.type == Dependency.Type.EXTERNAL) && (dependency.scope == com.android.tools.idea.gradle.parser.Dependency.Scope.ANDROID_TEST_COMPILE) && (dependency.data != null) && (dependency.data.toString().startsWith("com.android.support.test.espresso:espresso-core"))) {
                    espressoFound = true;
                    break;
                }
            }
        }
        if (!espressoFound) {
            Messages.showErrorDialog(this.project, "<html>Failed to find dependencies for Espresso. You must set up Espresso as defined at <a href='http://developer.android.com/training/testing/ui-testing/espresso-testing.html#setup'>http://developer.android.com/training/testing/ui-testing/espresso-testing.html#setup</a></html>", "Error");
            this.recButton.setText(RECORD);
            this.recButton.setIcon(IconLoader.getIcon("icons/rec.png"));
            return;
        }*/

        Dependency dependency = findDepRecord(dependencies);
        if (dependency == null) {
            CommandProcessor.getInstance().executeCommand(this.project, new Runnable() {
                public void run() {
                    dependencies.add(new Dependency(com.android.tools.idea.gradle.parser.Dependency.Scope.COMPILE, Dependency.Type.FILES, ToolsTestsRecorderAction.this.jarPath));
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run() {
                            ToolsTestsRecorderAction.this.buildFile.setValue(BuildFileKey.DEPENDENCIES, dependencies);
                        }
                    });
                }
            }, null, null);
        }

        uniqueId = System.currentTimeMillis();

        Activity activity = ((ActivitiesComboBoxModel.ActivityWrapper) this.activitiesBoxModel.getSelected()).getActivity();
        final PsiClass activityClass = (PsiClass) activity.getActivityClass().getValue();
        com.intellij.psi.PsiManager manager = com.intellij.psi.PsiManager.getInstance(this.project);
        PsiFile psiFile = activityClass.getContainingFile();
        final PsiDirectory psiDirectory = psiFile.getContainingDirectory();
        final String packageName = ((com.intellij.psi.PsiJavaFile) activityClass.getContainingFile()).getPackageName();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    PsiFile testFile = psiDirectory.findFile(TEST_FILE_NAME);
                    if (testFile == null) {
                        testFile = psiDirectory.createFile(TEST_FILE_NAME);
                    }
                    ToolsTestsRecorderAction.this.testVirtualFile = testFile.getVirtualFile();
                    com.intellij.openapi.vfs.VfsUtil.saveText(ToolsTestsRecorderAction.this.testVirtualFile,
                            ToolsTestsRecorderAction.template.replace("{ACTIVITY}", activityClass.getName()).replace("{PACKAGE}", packageName).
                                    replace("{CLASSNAME}", "AndrTestRec").replace("{ID}", String.valueOf(uniqueId)));
                } catch (IOException e) {
                    Messages.showErrorDialog(ToolsTestsRecorderAction.this.project, e.getMessage(), "Error");
                }
            }
        });
        RunManager runManager = RunManager.getInstance(this.project);
        AndroidTestRunConfigurationType configurationType = new AndroidTestRunConfigurationType();
        AndroidTestRunConfiguration runConfiguration = new AndroidTestRunConfiguration(this.project, configurationType.getFactory());
        String runConfigName = RUN_CONFIG_NAME + System.currentTimeMillis();
        runConfiguration.setName(runConfigName);
        runConfiguration.setModule(module);
        runConfiguration.setTargetSelectionMode(org.jetbrains.android.run.TargetSelectionMode.SHOW_DIALOG);
        runConfiguration.TESTING_TYPE = 2;
        runConfiguration.CLASS_NAME = (packageName + "." + ANDR_TEST_CLASSNAME);
        com.intellij.execution.RunnerAndConfigurationSettings rcs = runManager.createConfiguration(runConfiguration, configurationType.getFactory());
        rcs.setTemporary(true);
        ExecutionManager executionManager = ExecutionManager.getInstance(this.project);
        executionManager.restartRunProfile(this.project, com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance(), com.intellij.execution.DefaultExecutionTarget.INSTANCE, rcs, (ProcessHandler) null);
        this.executionChecker = new ExecutionChecker(executionManager, runConfigName, this);
        new java.util.Timer().schedule(this.executionChecker, 200L, 200L);

        eventsList.clear(project, module, psiFile);
    }

    private Dependency findDepRecord(List<BuildFileStatement> dependencies) {
        for (BuildFileStatement statement : dependencies) {
            if ((statement instanceof Dependency)) {
                Dependency dependency = (Dependency) statement;
                if ((dependency.type == Dependency.Type.FILES) && (dependency.data != null) && (dependency.data.toString().equals(this.jarPath))) {
                    return dependency;
                }
            }
        }
        return null;
    }

    private String getJarPath() {
        /*
        String name = this.getClass().getName().replace('.', '/');
        String s = this.getClass().getResource("/" + name + ".class").toString();
        s = s.substring(0, s.indexOf(".jar")+4);
        s = s.substring(s.lastIndexOf(':')-1);
        return s;*/
        // temporary because we are starting plugin from Idea and it is not packaged in ZIP
        return "C:/Users/vpedak/IdeaProjects/droidtestrec/AndroidTestsRecorder.jar";
    }

    public void testStarted() {
        ApplicationManager.getApplication().invokeLater(new Runnable()
/*     */ {
            /*     */
            public void run() {
                ToolsTestsRecorderAction.this.toolWindow.activate(null, true, true);
            }
/* 388 */
        });

        eventReader.start(uniqueId);
    }
}
