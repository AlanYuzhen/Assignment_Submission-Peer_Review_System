package com.yuzhen.assignment;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DataStore store = new DataStore(Paths.get("data"));
                RoleSelectionFrame frame = new RoleSelectionFrame(store);
                frame.setVisible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
