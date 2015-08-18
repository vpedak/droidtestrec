package com.vpedak.testsrecorder.plugin.ui;

import com.intellij.openapi.module.Module;
import com.intellij.ui.AbstractCollectionComboBoxModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ModulesComboBoxModel extends AbstractCollectionComboBoxModel<ModulesComboBoxModel.ModuleWrapper> {
    private List<ModuleWrapper> list;

    public ModulesComboBoxModel(Module[] modules, Module selected) {
        super(selected == null ? null : new ModuleWrapper(selected));
        this.list = new ArrayList(modules.length);
        for (int i = 0; i < modules.length; i++) {
            this.list.add(new ModuleWrapper(modules[i]));
        }
    }

    @NotNull
    protected List<ModuleWrapper> getItems() {
        return list;
    }

    public static class ModuleWrapper {
        private Module module;

        public ModuleWrapper(Module module) {
            this.module = module;
        }

        public Module getModule() {
            return this.module;
        }

        public String toString() {
            return this.module.getName();
        }
    }
}
