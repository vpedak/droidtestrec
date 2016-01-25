package com.vpedak.testsrecorder.plugin.ui;

import com.intellij.ide.util.PackageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.IncorrectOperationException;
import com.vpedak.testsrecorder.core.events.RecordingEvent;
import com.vpedak.testsrecorder.plugin.core.EspressoTestGenerator;
import com.vpedak.testsrecorder.plugin.core.EventReader;
import com.vpedak.testsrecorder.core.TestGenerator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class EventsList extends JPanel implements EventReader.EventListener, ComponentListener {
    private JBList list;
    private DefaultListModel listModel;
    private final JBScrollPane pane;
    private Project project;
    private Module module;
    private PsiFile activityFile;
    private JTextField testClassField = new JTextField(20);
    private String activityClassName;

    public EventsList() {
        super(new VerticalLayout(5));
        listModel = new DefaultListModel();
        list = new JBList(listModel);
        list.setCellRenderer(new MyRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pane = new JBScrollPane(list);
        add(pane);
        final JButton generate = new JButton("Generate Code", IconLoader.getIcon("icons/gen.png"));
        generate.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generate();
            }
        });
        JPanel tmp = new JPanel();
        JLabel testLabel = new JLabel("Test class: ", SwingConstants.RIGHT);
        testLabel.setMaximumSize(new Dimension(testLabel.getPreferredSize().width + 20, testLabel.getPreferredSize().height));
        tmp.add(testLabel);
        testClassField = new JTextField(20);
        testClassField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                generate.setEnabled(testClassField.getText().trim().length() > 0);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                generate.setEnabled(testClassField.getText().trim().length() > 0);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                generate.setEnabled(testClassField.getText().trim().length() > 0);
            }
        });
        tmp.add(testClassField);
        tmp.add(generate);
        add(tmp);
        JPanel buttonsPanel = new JPanel();
        JButton save = new JButton("Save List", IconLoader.getIcon("icons/save.png"));
        save.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        JButton load = new JButton("Load List", IconLoader.getIcon("icons/load.png"));
        load.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
            }
        });
        buttonsPanel.add(save);
        buttonsPanel.add(load);
        add(buttonsPanel);
        addComponentListener(this);
    }

    public void newRecording(Project project) {
        this.project = project;
    }

    public void clear(Project project, Module module, PsiFile activityFile) {
        listModel.removeAllElements();

        this.project = project;
        this.module = module;

        this.activityFile = activityFile;
        activityClassName = activityFile.getName();
        int pos = activityClassName.indexOf('.');
        if (pos > 0) {
            activityClassName = activityClassName.substring(0, pos);
        }
        testClassField.setText(activityClassName +"Test");
    }

    private void generate() {
        final String packageName = ((com.intellij.psi.PsiJavaFile) activityFile).getPackageName();
        PsiDirectory dir = selectTargetDirectory(packageName);

        if (dir == null) {
            dir = activityFile.getContainingDirectory();
        }

        final PsiDirectory finalDir = dir;
        final String testClassName = testClassField.getText();
        final String testName = testClassName+".java";

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    PsiFile testFile = finalDir.findFile(testName);
                    if (testFile == null) {
                        testFile = finalDir.createFile(testName);
                    }
                    VirtualFile testVirtualFile = testFile.getVirtualFile();

                    TestGenerator testGenerator = new EspressoTestGenerator();

                    List<RecordingEvent> list = new ArrayList<RecordingEvent>(listModel.size());
                    for (int i=0; i<listModel.getSize(); i++) {
                        RecordingEvent event = (RecordingEvent) listModel.get(i);
                        list.add(event);
                    }

                    String code = testGenerator.generate(activityClassName, testClassName, packageName, list);
                    com.intellij.openapi.vfs.VfsUtil.saveText(testVirtualFile, code);

                    final PsiFile psiFile = testFile;

                    WriteCommandAction.Simple<String> command = new WriteCommandAction.Simple<String>(project, psiFile) {
                        @Override
                        protected void run() throws Throwable {
                            CodeStyleManager.getInstance(project).reformat(psiFile, false);
                        }
                    };
                    command.execute();

                    FileEditorManagerEx.getInstanceEx(project).openFile(testVirtualFile, true);
                } catch (IOException e) {
                    Messages.showErrorDialog(project, e.getMessage(), "Error");
                }
            }
        });


    }

    @Nullable
    private PsiDirectory selectTargetDirectory(final String packageName) throws IncorrectOperationException {
        final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(project), packageName);

        final VirtualFile selectedRoot = new ReadAction<VirtualFile>() {
            protected void run(Result<VirtualFile> result) throws Throwable {
                final HashSet<VirtualFile> testFolders = new HashSet<VirtualFile>();
                checkForTestRoots(module, testFolders);
                List<VirtualFile> roots;
                if (testFolders.isEmpty()) {
                    roots = ModuleRootManager.getInstance(module).getSourceRoots(JavaModuleSourceRootTypes.SOURCES);
                    removeGenerated(roots);

                    if (roots.isEmpty()) return;
                } else {
                    roots = new ArrayList<VirtualFile>(testFolders);
                }

                if (roots.size() == 1) {
                    result.setResult(roots.get(0));
                }
                else {
                    PsiDirectory defaultDir = chooseDefaultDirectory(packageName);
                    result.setResult(MoveClassesOrPackagesUtil.chooseSourceRoot(targetPackage, roots, defaultDir));
                }
            }
        }.execute().getResultObject();

        if (selectedRoot == null) return null;

        return new WriteCommandAction<PsiDirectory>(project, "") {
            protected void run(Result<PsiDirectory> result) throws Throwable {
                result.setResult(RefactoringUtil.createPackageDirectoryInSourceRoot(targetPackage, selectedRoot));
            }
        }.execute().getResultObject();
    }

    protected static void checkForTestRoots(Module srcModule, Set<VirtualFile> testFolders) {
        testFolders.addAll(ModuleRootManager.getInstance(srcModule).getSourceRoots(JavaSourceRootType.TEST_SOURCE));

        removeGenerated(testFolders);
        //create test in the same module
        if (!testFolders.isEmpty()) return;

        //suggest to choose from all dependencies modules
        final HashSet<Module> modules = new HashSet<Module>();
        ModuleUtilCore.collectModulesDependsOn(srcModule, modules);
        for (Module module : modules) {
            testFolders.addAll(ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE));
        }

        removeGenerated(testFolders);
    }

    private static void removeGenerated(Collection<VirtualFile> testFolders) {
        // remove generated directories
        for (Iterator<VirtualFile> iter = testFolders.iterator(); iter.hasNext();) {
            VirtualFile tst = iter.next();

            String url = tst.getPresentableUrl();

            if (url.contains("build/generated") || url.contains("build\\generated")) {
                iter.remove();
            }
        }
    }

    @Nullable
    private PsiDirectory chooseDefaultDirectory(String packageName) {
        List<PsiDirectory> dirs = new ArrayList<PsiDirectory>();
        for (VirtualFile file : ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE)) {
            final PsiDirectory dir = PsiManager.getInstance(project).findDirectory(file);
            if (dir != null) {
                dirs.add(dir);
            }
        }
        if (!dirs.isEmpty()) {
            for (PsiDirectory dir : dirs) {
                final String dirName = dir.getVirtualFile().getPath();
                if (dirName.contains("generated")) continue;
                return dir;
            }
            return dirs.get(0);
        }
        return PackageUtil.findPossiblePackageDirectoryInModule(module, packageName);
    }
    private void load() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Recording Files", "atr"));
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            listModel.removeAllElements();

            File file = fc.getSelectedFile();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));

                String str = reader.readLine();
                while (str != null) {
                    RecordingEvent event = RecordingEvent.fromString(str);
                    listModel.addElement(event);
                    str = reader.readLine();
                }
            } catch (IOException e) {
                Messages.showErrorDialog(project, "Failed to load file " + file.getAbsolutePath(), "Error");
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void save() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Recording Files", "atr"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!file.getAbsolutePath().contains(".")) {
                file = new File(file.getAbsolutePath()+".atr");
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(file, false);

                for (int i=0; i<listModel.getSize(); i++) {
                    RecordingEvent event = (RecordingEvent) listModel.get(i);
                    writer.write(event.toString()+"\n");
                }
            } catch (IOException e) {
                Messages.showErrorDialog(project, "Failed to save to file " + file.getAbsolutePath(), "Error");
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRecordingEvent(final RecordingEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.addElement(event);
            }
        });
    }

    @Override
    public void componentResized(ComponentEvent e) {
        pane.setPreferredSize(new Dimension(getWidth(), getHeight() - 110));
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    class MyRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RecordingEvent event = (RecordingEvent) value;
            setText(" "+event.getDescription());
            return c;
        }
    }
}
