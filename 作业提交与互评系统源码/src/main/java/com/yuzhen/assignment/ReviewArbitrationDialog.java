package com.yuzhen.assignment;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

class ReviewArbitrationDialog extends JDialog {
    boolean saved;

    private final JSpinner score;
    private final JTextArea commentText = new JTextArea(5, 32);

    ReviewArbitrationDialog(JFrame owner, PeerReview review) {
        super(owner, "仲裁互评 #" + review.id, true);
        this.score = new JSpinner(new SpinnerNumberModel(review.score, 0, 100, 1));
        this.commentText.setText(review.comment == null ? "" : review.comment);
        this.commentText.setLineWrap(true);
        this.commentText.setWrapStyleWord(true);
        UiKit.fitWindow(this, 560, 460);
        setLocationRelativeTo(owner);
        setContentPane(buildContent(review));
    }

    int score() {
        return (Integer) score.getValue();
    }

    String comment() {
        return commentText.getText().trim();
    }

    private JComponent buildContent(PeerReview review) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        int row = 0;
        row = UiKit.formRow(form, c, row, "提交ID", new JLabel(String.valueOf(review.submissionId)));
        row = UiKit.formRow(form, c, row, "评审人", new JLabel(review.reviewerNo));
        row = UiKit.formRow(form, c, row, "当前争议", new JLabel(review.disputed ? "是" : "否"));
        row = UiKit.formRow(form, c, row, "仲裁分数", score);
        row = UiKit.formRow(form, c, row, "仲裁评语", new JScrollPane(commentText));
        UiKit.formRow(form, c, row, "", new JLabel("保存后将标记为已仲裁，并清除争议状态。"));

        JButton save = UiKit.primaryButton("保存仲裁");
        JButton cancel = new JButton("取消");
        save.addActionListener(e -> {
            saved = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        return UiKit.withActions(new JScrollPane(form), UiKit.actions(save, cancel));
    }
}
