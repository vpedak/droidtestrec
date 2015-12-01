package com.vpedak.testsrecorder.plugin.ui;

import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

public abstract class AbstractCollectionComboBoxModel<T> extends AbstractListModel implements ComboBoxModel {
    private T mySelection;

    public AbstractCollectionComboBoxModel(T selection) {
        this.mySelection = selection;
    }

    public int getSize() {
        return this.getItems().size();
    }

    public T getElementAt(int index) {
        return this.getItems().get(index);
    }

    public void setSelectedItem(Object anItem) {
        this.mySelection = (T) anItem;
    }

    public Object getSelectedItem() {
        return this.mySelection;
    }

    public T getSelected() {
        return this.mySelection;
    }

    public void update() {
        super.fireContentsChanged(this, -1, -1);
    }

    public boolean contains(T item) {
        return this.getItems().contains(item);
    }

    protected abstract List<T> getItems();
}