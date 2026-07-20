package com.yuzhen.assignment;

import javax.swing.table.DefaultTableModel;

class MainFrame extends TeacherFrame {
    public final DefaultTableModel similarityModel;

    MainFrame(DataStore store) {
        super(store);
        this.similarityModel = super.similarityModel;
    }

    static void installGlobalStyle() {
        UiKit.installGlobalStyle();
    }
}
