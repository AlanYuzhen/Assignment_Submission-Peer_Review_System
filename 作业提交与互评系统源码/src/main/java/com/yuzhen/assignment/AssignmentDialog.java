package com.yuzhen.assignment;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

class AssignmentDialog extends JDialog {
    Assignment result;

    private final JTextField title = new JTextField(28);
    private final JTextField dueDate = new JTextField(10);
    private final JTextArea description = new JTextArea(5, 28);
    private final JTextArea sampleInput = new JTextArea(4, 28);
    private final JTextArea expectedOutput = new JTextArea(4, 28);
    private final JCheckBox programming = new JCheckBox("编程作业，需要自动评测");
    private final JCheckBox peerReviewOpen = new JCheckBox("开放学生互评");
    private final Assignment source;

    AssignmentDialog(JFrame owner, Assignment source) {
        super(owner, source == null ? "发布作业" : "编辑作业", true);
        this.source = source;
        UiKit.fitWindow(this, 620, 680);
        setLocationRelativeTo(owner);
        setContentPane(buildContent());
        if (source != null) {
            fill(source);
        } else {
            dueDate.setText(LocalDate.now().plusDays(7).toString());
            programming.setSelected(true);
            peerReviewOpen.setSelected(true);
        }
    }

    private JComponent buildContent() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        int row = 0;
        row = UiKit.formRow(form, c, row, "标题", title);
        row = UiKit.formRow(form, c, row, "截止日期(yyyy-MM-dd)", dueDate);
        row = UiKit.formRow(form, c, row, "描述", new JScrollPane(description));
        row = UiKit.formRow(form, c, row, "样例输入", new JScrollPane(sampleInput));
        row = UiKit.formRow(form, c, row, "期望输出", new JScrollPane(expectedOutput));
        row = UiKit.formRow(form, c, row, "", programming);
        UiKit.formRow(form, c, row, "", peerReviewOpen);

        JButton save = UiKit.primaryButton("保存");
        JButton cancel = new JButton("取消");
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        return UiKit.withActions(new JScrollPane(form), UiKit.actions(save, cancel));
    }

    private void fill(Assignment a) {
        title.setText(a.title);
        dueDate.setText(a.dueDate.toString());
        description.setText(a.description);
        sampleInput.setText(a.sampleInput);
        expectedOutput.setText(a.expectedOutput);
        programming.setSelected(a.programming);
        peerReviewOpen.setSelected(a.peerReviewOpen);
    }

    private void save() {
        try {
            Assignment a = source == null ? new Assignment() : source;
            a.title = title.getText().trim();
            a.dueDate = LocalDate.parse(dueDate.getText().trim());
            a.description = description.getText();
            a.sampleInput = sampleInput.getText();
            a.expectedOutput = expectedOutput.getText();
            a.programming = programming.isSelected();
            a.peerReviewOpen = peerReviewOpen.isSelected();
            if (a.title.isBlank()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            result = a;
            dispose();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "截止日期格式应为 yyyy-MM-dd");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }
}
