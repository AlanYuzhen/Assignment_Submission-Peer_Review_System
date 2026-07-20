package com.yuzhen.assignment;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

class TeacherFrame extends JFrame implements DataChangeListener {
    protected final DataStore store;
    protected final RoleSession session;
    protected final DefaultTableModel similarityModel = UiKit.model("提交A", "提交B", "学生A", "学生B", "相似度", "风险");

    private final DefaultTableModel assignmentModel = UiKit.model("ID", "标题", "截止日期", "编程作业", "互评开放");
    private final DefaultTableModel submissionOverviewModel = UiKit.model("作业", "提交人数", "未提交人数", "总人数");
    private final DefaultTableModel judgeModel = UiKit.model("作业", "通过", "失败", "编译错误", "其他");
    private final DefaultTableModel gradeModel = UiKit.model("提交ID", "作业", "学生", "自动评测", "互评均分", "最终分", "查重");
    private final DefaultTableModel reviewModel = UiKit.model("互评ID", "提交ID", "评审人", "分数", "评语", "争议", "已仲裁");
    private final JComboBox<Assignment> monitorAssignment = new JComboBox<>();

    TeacherFrame(DataStore store) {
        this(store, RoleSession.teacher());
    }

    TeacherFrame(DataStore store, RoleSession session) {
        super("教师端 - 作业管理与成绩评定");
        this.store = store;
        session.requireTeacher();
        this.session = session;
        UiKit.installGlobalStyle();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        UiKit.fitWindow(this, 1180, 760);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        store.addListener(this);
        refreshAll();
    }

    private JComponent buildContent() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.addTab("作业管理", assignmentPanel());
        tabs.addTab("数据监控", monitorPanel());
        tabs.addTab("成绩管理", gradePanel());
        return tabs;
    }

    private JComponent assignmentPanel() {
        JTable table = new JTable(assignmentModel);
        JButton add = UiKit.primaryButton("发布作业");
        JButton edit = new JButton("编辑选中作业");
        JButton delete = new JButton("删除选中作业");
        add.addActionListener(e -> editAssignment(null));
        edit.addActionListener(e -> {
            Assignment selected = selectedAssignment(table);
            if (selected != null) {
                editAssignment(selected);
            }
        });
        delete.addActionListener(e -> deleteAssignment(table));
        return UiKit.withActions(new JScrollPane(table), UiKit.actions(add, edit, delete));
    }

    private void deleteAssignment(JTable table) {
        Assignment selected = selectedAssignment(table);
        if (selected == null) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "删除作业会级联删除提交与互评数据，是否继续？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            store.deleteAssignment(session, selected.id);
        } catch (IOException ex) {
            UiKit.error(this, ex);
        }
    }

    private JComponent monitorPanel() {
        UiKit.renderAssignmentCombo(monitorAssignment);
        JButton plagiarism = new JButton("一键查重并标记高风险");
        plagiarism.addActionListener(e -> {
            Assignment a = (Assignment) monitorAssignment.getSelectedItem();
            if (a == null) {
                return;
            }
            try {
                store.flagPlagiarism(session, a.id, 0.80);
                fillSimilarity(a.id);
            } catch (IOException ex) {
                UiKit.error(this, ex);
            }
        });
        monitorAssignment.addActionListener(e -> {
            Assignment a = (Assignment) monitorAssignment.getSelectedItem();
            if (a != null) {
                fillSimilarity(a.id);
            }
        });

        JPanel top = UiKit.actions(new JLabel("监控作业："), monitorAssignment, plagiarism);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(new JTable(submissionOverviewModel)),
                new JScrollPane(new JTable(similarityModel)));
        split.setResizeWeight(0.45);

        JPanel judge = new JPanel(new BorderLayout(8, 8));
        judge.add(new JLabel("评测概览"), BorderLayout.NORTH);
        judge.add(new JScrollPane(new JTable(judgeModel)), BorderLayout.CENTER);

        JSplitPane full = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, split, judge);
        full.setResizeWeight(0.66);
        return UiKit.withActions(full, top);
    }

    private JComponent gradePanel() {
        JTable grades = new JTable(gradeModel);
        JTable reviews = new JTable(reviewModel);
        JButton resolve = new JButton("仲裁选中互评");
        resolve.addActionListener(e -> resolveReview(reviews));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(grades), new JScrollPane(reviews));
        split.setResizeWeight(0.55);
        return UiKit.withActions(split, UiKit.actions(resolve));
    }

    private void resolveReview(JTable reviews) {
        int row = reviews.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = (Integer) reviewModel.getValueAt(reviews.convertRowIndexToModel(row), 0);
        PeerReview review = store.reviews.stream().filter(r -> r.id == id).findFirst().orElse(null);
        if (review == null) {
            return;
        }
        ReviewArbitrationDialog dialog = new ReviewArbitrationDialog(this, review);
        dialog.setVisible(true);
        if (!dialog.saved) {
            return;
        }
        try {
            store.arbitrateReview(session, review.id, dialog.score(), dialog.comment());
        } catch (IOException ex) {
            UiKit.error(this, ex);
        }
    }

    private void editAssignment(Assignment existing) {
        AssignmentDialog dialog = new AssignmentDialog(this, existing);
        dialog.setVisible(true);
        Assignment result = dialog.result;
        if (result == null) {
            return;
        }
        try {
            store.saveAssignment(session, result);
        } catch (IOException ex) {
            UiKit.error(this, ex);
        }
    }

    private Assignment selectedAssignment(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int id = (Integer) assignmentModel.getValueAt(table.convertRowIndexToModel(row), 0);
        return store.findAssignment(id);
    }

    protected void refreshAll() {
        assignmentModel.setRowCount(0);
        monitorAssignment.removeAllItems();
        for (Assignment a : store.assignments) {
            assignmentModel.addRow(new Object[]{
                    a.id, a.title, a.dueDate, a.programming ? "是" : "否", a.peerReviewOpen ? "开放" : "关闭"
            });
            monitorAssignment.addItem(a);
        }
        fillSubmissionOverview();
        fillJudgeOverview();
        fillGradesAndReviews();
        Assignment selected = (Assignment) monitorAssignment.getSelectedItem();
        if (selected != null) {
            fillSimilarity(selected.id);
        } else {
            similarityModel.setRowCount(0);
        }
    }

    private void fillSubmissionOverview() {
        submissionOverviewModel.setRowCount(0);
        int total = store.getStudents().size();
        for (Assignment a : store.assignments) {
            long submitted = store.submissionsOf(a.id).stream().map(s -> s.studentNo).distinct().count();
            submissionOverviewModel.addRow(new Object[]{a.title, submitted, total - submitted, total});
        }
    }

    private void fillSimilarity(int assignmentId) {
        similarityModel.setRowCount(0);
        List<SimilarityResult> results = PlagiarismService.compare(store.submissionsOf(assignmentId));
        for (SimilarityResult r : results) {
            String risk = r.score >= 0.80 ? "高风险" : r.score >= 0.50 ? "需复核" : "正常";
            similarityModel.addRow(new Object[]{r.left.id, r.right.id, r.left.studentName, r.right.studentName,
                    String.format(Locale.ROOT, "%.2f%%", r.score * 100), risk});
        }
    }

    private void fillJudgeOverview() {
        judgeModel.setRowCount(0);
        for (Assignment a : store.assignments) {
            int pass = 0;
            int fail = 0;
            int compile = 0;
            int other = 0;
            for (Submission s : store.submissionsOf(a.id)) {
                if ("通过".equals(s.judgeStatus)) {
                    pass++;
                } else if ("失败".equals(s.judgeStatus)) {
                    fail++;
                } else if ("编译错误".equals(s.judgeStatus)) {
                    compile++;
                } else {
                    other++;
                }
            }
            judgeModel.addRow(new Object[]{a.title, pass, fail, compile, other});
        }
    }

    private void fillGradesAndReviews() {
        gradeModel.setRowCount(0);
        reviewModel.setRowCount(0);
        for (Submission s : store.submissions) {
            Assignment a = store.findAssignment(s.assignmentId);
            gradeModel.addRow(new Object[]{s.id, a == null ? "已删除" : a.title, s.studentName, s.autoScore,
                    String.format(Locale.ROOT, "%.1f", store.averagePeerScore(s.id)),
                    String.format(Locale.ROOT, "%.1f", store.finalScore(s)), s.plagiarismFlag ? "高风险" : "正常"});
        }
        for (PeerReview r : store.reviews) {
            reviewModel.addRow(new Object[]{r.id, r.submissionId, r.reviewerNo, r.score, r.comment,
                    r.disputed ? "是" : "否", r.resolvedByTeacher ? "是" : "否"});
        }
    }

    @Override
    public void onDataChanged() {
        SwingUtilities.invokeLater(this::refreshAll);
    }
}
