package com.yuzhen.assignment;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;

final class UiKit {
    private UiKit() {
    }

    static void installGlobalStyle() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Font base = new Font("Microsoft YaHei UI", Font.PLAIN, 16);
        Font table = new Font("Microsoft YaHei UI", Font.PLAIN, 15);
        Font tab = new Font("Microsoft YaHei UI", Font.BOLD, 16);
        UIManager.put("Button.font", base);
        UIManager.put("CheckBox.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Label.font", base);
        UIManager.put("PasswordField.font", base);
        UIManager.put("Spinner.font", base);
        UIManager.put("Table.font", table);
        UIManager.put("TableHeader.font", new Font("Microsoft YaHei UI", Font.BOLD, 15));
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TabbedPane.font", tab);
        UIManager.put("TextArea.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
    }

    static void fitWindow(Window window, int preferredWidth, int preferredHeight) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(preferredWidth, Math.max(520, screen.width - 96));
        int height = Math.min(preferredHeight, Math.max(420, screen.height - 96));
        window.setSize(width, height);
        if (window instanceof JFrame) {
            ((JFrame) window).setMinimumSize(new Dimension(Math.min(width, 720), Math.min(height, 520)));
        } else if (window instanceof JDialog) {
            ((JDialog) window).setMinimumSize(new Dimension(Math.min(width, 520), Math.min(height, 420)));
        }
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = getModel().isPressed()
                        ? new Color(15, 50, 120)
                        : getModel().isRollover() ? new Color(24, 94, 190) : new Color(22, 78, 175);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setBackground(new Color(22, 78, 175));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        return button;
    }

    static JPanel card(Component child) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 205, 224)),
                new EmptyBorder(20, 20, 20, 20)));
        panel.setBackground(Color.WHITE);
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    static JPanel actions(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    static GridBagConstraints formConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        return c;
    }

    static JPanel withActions(Component center, Component actions) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(actions, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    static void renderAssignmentCombo(JComboBox<Assignment> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Assignment) {
                    Assignment a = (Assignment) value;
                    setText(a.id + " - " + a.title);
                }
                return this;
            }
        });
    }

    static int formRow(JPanel form, GridBagConstraints c, int row, String label, Component field) {
        return formRow(form, c, row, new JLabel(label), field);
    }

    static int formRow(JPanel form, GridBagConstraints c, int row, JLabel label, Component field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.weighty = 0;
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.weighty = field instanceof JScrollPane ? 1 : 0;
        form.add(field, c);
        return row + 1;
    }

    static String limit(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "\n......";
    }

    static void error(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
    }
}
