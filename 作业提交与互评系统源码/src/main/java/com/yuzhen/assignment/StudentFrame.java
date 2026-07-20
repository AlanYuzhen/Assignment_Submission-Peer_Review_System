package com.yuzhen.assignment;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

class StudentFrame extends JFrame implements DataChangeListener {
    private final DataStore store;
    private final RoleSession session;
    private final StudentProfile student;
    private final DefaultTableModel openAssignmentModel = UiKit.model("ID", "标题", "截止日期", "类型", "互评");
    private final DefaultTableModel submitHistoryModel = UiKit.model("提交ID", "作业", "文件", "提交时间", "状态");
    private final DefaultTableModel judgeModel = UiKit.model("提交ID", "作业", "评测状态", "评测信息", "自动分");
    private final DefaultTableModel scoreModel = UiKit.model("作业", "最终分", "自动评测", "互评均分", "同学评语");
    private final JComboBox<Assignment> submitAssignment = new JComboBox<>();
    private final JTextArea assignmentDetail = new JTextArea();
    private final JTextArea reviewTarget = new JTextArea();
    private final JSpinner reviewScore = new JSpinner(new SpinnerNumberModel(85, 0, 100, 1));
    private final JTextArea reviewComment = new JTextArea(4, 30);
    private Submission currentReviewTarget;

    StudentFrame(DataStore store, StudentProfile student) {
        this(store, RoleSession.student(student));
    }

    StudentFrame(DataStore store, RoleSession session) {
        super(studentTitle(session));
        this.store = store;
        this.session = session;
        this.student = session.requireStudent();
        UiKit.installGlobalStyle();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        UiKit.fitWindow(this, 1120, 740);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        store.addListener(this);
        refreshAll();
    }

    private static String studentTitle(RoleSession session) {
        StudentProfile student = session.requireStudent();
        return "学生端 - " + student.name + "（" + student.studentNo + "）";
    }

    private JComponent buildContent() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.addTab("作业查看", assignmentViewPanel());
        tabs.addTab("作业提交", submitPanel());
        tabs.addTab("评测与互评", reviewPanel());
        tabs.addTab("成绩查询", scorePanel());
        return tabs;
    }

    private JComponent assignmentViewPanel() {
        JTable table = new JTable(openAssignmentModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (Integer) openAssignmentModel.getValueAt(table.convertRowIndexToModel(row), 0);
                assignmentDetail.setText(formatAssignment(store.findAssignment(id)));
            }
        });
        assignmentDetail.setEditable(false);
        assignmentDetail.setLineWrap(true);
        assignmentDetail.setWrapStyleWord(true);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), new JScrollPane(assignmentDetail));
        split.setResizeWeight(0.55);
        return split;
    }

    private JComponent submitPanel() {
        UiKit.renderAssignmentCombo(submitAssignment);
        JButton upload = UiKit.primaryButton("上传 .java 或 .docx");
        upload.addActionListener(e -> uploadSubmission());
        JPanel actions = UiKit.actions(new JLabel("选择作业："), submitAssignment, upload);
        return UiKit.withActions(new JScrollPane(new JTable(submitHistoryModel)), actions);
    }

    private JComponent reviewPanel() {
        JTable table = new JTable(judgeModel);
        JButton next = new JButton("随机领取匿名互评");
        JButton submit = UiKit.primaryButton("提交互评");
        next.addActionListener(e -> loadReviewTarget());
        submit.addActionListener(e -> submitReview());

        reviewTarget.setEditable(false);
        reviewTarget.setLineWrap(true);
        reviewTarget.setWrapStyleWord(true);
        reviewComment.setLineWrap(true);
        reviewComment.setWrapStyleWord(true);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(new JScrollPane(reviewTarget), BorderLayout.CENTER);
        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(UiKit.actions(new JLabel("分数："), reviewScore, next, submit), BorderLayout.NORTH);
        lower.add(new JScrollPane(reviewComment), BorderLayout.CENTER);
        form.add(lower, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), form);
        split.setResizeWeight(0.55);
        return split;
    }

    private JComponent scorePanel() {
        return new JScrollPane(new JTable(scoreModel));
    }

    private void uploadSubmission() {
        Assignment a = (Assignment) submitAssignment.getSelectedItem();
        if (a == null) {
            return;
        }
        if (!a.isOpen(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "该作业已截止，不能提交。");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Java 源文件或 Word 文档", "java", "docx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            store.saveSubmission(session, a.id, chooser.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "提交成功，评测结果已更新。");
        } catch (IOException ex) {
            UiKit.error(this, ex);
        }
    }

    private void loadReviewTarget() {
        currentReviewTarget = store.randomReviewTarget(session);
        if (currentReviewTarget == null) {
            reviewTarget.setText("当前没有可互评的匿名作业。");
            return;
        }
        Assignment a = store.findAssignment(currentReviewTarget.assignmentId);
        reviewTarget.setText("匿名作业：" + (a == null ? "未知作业" : a.title) +
                "\n\n提交内容摘录：\n" + UiKit.limit(currentReviewTarget.textContent, 1200));
    }

    private void submitReview() {
        if (currentReviewTarget == null) {
            JOptionPane.showMessageDialog(this, "请先领取一个互评作业。");
            return;
        }
        PeerReview review = new PeerReview();
        review.assignmentId = currentReviewTarget.assignmentId;
        review.submissionId = currentReviewTarget.id;
        review.reviewerNo = student.studentNo;
        review.score = (Integer) reviewScore.getValue();
        review.comment = reviewComment.getText().trim();
        review.disputed = review.score < 60 || review.score > 98;
        try {
            store.saveReview(session, review);
            currentReviewTarget = null;
            reviewTarget.setText("互评已提交，可继续领取下一份。");
            reviewComment.setText("");
        } catch (IOException ex) {
            UiKit.error(this, ex);
        }
    }

    private void refreshAll() {
        openAssignmentModel.setRowCount(0);
        submitAssignment.removeAllItems();
        LocalDate today = LocalDate.now();
        for (Assignment a : store.assignments) {
            if (a.isOpen(today)) {
                openAssignmentModel.addRow(new Object[]{
                        a.id, a.title, a.dueDate, a.programming ? "编程" : "文档",
                        a.peerReviewOpen ? "开放" : "关闭"
                });
                submitAssignment.addItem(a);
            }
        }
        fillHistory();
        fillScores();
    }

    private void fillHistory() {
        submitHistoryModel.setRowCount(0);
        judgeModel.setRowCount(0);
        for (Submission s : store.submissionsOfStudent(student.studentNo)) {
            Assignment a = store.findAssignment(s.assignmentId);
            String title = a == null ? "已删除" : a.title;
            submitHistoryModel.addRow(new Object[]{s.id, title, s.fileName, s.submittedAt, s.judgeStatus});
            judgeModel.addRow(new Object[]{s.id, title, s.judgeStatus, s.judgeMessage, s.autoScore});
        }
    }

    private void fillScores() {
        scoreModel.setRowCount(0);
        for (Submission s : store.submissionsOfStudent(student.studentNo)) {
            Assignment a = store.findAssignment(s.assignmentId);
            List<PeerReview> reviews = store.reviewsOfSubmission(s.id);
            String comments = reviews.stream()
                    .map(r -> r.comment)
                    .filter(c -> c != null && !c.isBlank())
                    .reduce((x, y) -> x + "；" + y)
                    .orElse("");
            scoreModel.addRow(new Object[]{a == null ? "已删除" : a.title,
                    String.format(Locale.ROOT, "%.1f", store.finalScore(s)), s.judgeStatus,
                    String.format(Locale.ROOT, "%.1f", store.averagePeerScore(s.id)), comments});
        }
    }

    private String formatAssignment(Assignment a) {
        if (a == null) {
            return "";
        }
        return "标题：" + a.title +
                "\n截止日期：" + a.dueDate +
                "\n类型：" + (a.programming ? "编程作业" : "文档作业") +
                "\n互评状态：" + (a.peerReviewOpen ? "开放" : "关闭") +
                "\n\n题目描述：\n" + a.description +
                "\n\n样例输入：\n" + a.sampleInput +
                "\n\n期望输出：\n" + a.expectedOutput;
    }

    @Override
    public void onDataChanged() {
        SwingUtilities.invokeLater(this::refreshAll);
    }
}
