package com.yuzhen.assignment;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Arrays;

public class RoleSelectionFrame extends JFrame {
    private static final String TEACHER_CREATE_CODE = "#YUzhen123";

    private final DataStore store;
    private final JComboBox<RoleChoice> loginRole = new JComboBox<>(RoleChoice.values());
    private final JTextField loginUsername = new JTextField(22);
    private final JPasswordField loginPassword = new JPasswordField(22);
    private final JComboBox<RoleChoice> registerRole = new JComboBox<>(RoleChoice.values());
    private final JTextField registerUsername = new JTextField(22);
    private final JPasswordField registerPassword = new JPasswordField(22);
    private final JPasswordField registerConfirm = new JPasswordField(22);
    private final JPasswordField teacherCreateCode = new JPasswordField(22);
    private final JLabel teacherCreateCodeLabel = new JLabel("教师注册码");
    private final JTextField registerStudentNo = new JTextField(22);
    private final JLabel registerStudentNoLabel = new JLabel("学号");
    private final JTextField registerName = new JTextField(22);
    private final JLabel registerNameLabel = new JLabel("姓名");

    public RoleSelectionFrame(DataStore store) {
        super("作业提交与互评系统 - 登录");
        this.store = store;
        UiKit.installGlobalStyle();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        UiKit.fitWindow(this, 820, 560);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout(18, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(226, 236, 248),
                        getWidth(), getHeight(), new Color(248, 250, 252)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(new EmptyBorder(26, 34, 26, 34));
        root.setOpaque(false);

        JLabel title = new JLabel("作业提交与互评系统", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 30));
        title.setForeground(new Color(15, 23, 42));
        JLabel subtitle = new JLabel("教师端和学生端使用独立账号密码登录，也可以创建新账号", SwingConstants.CENTER);
        subtitle.setForeground(new Color(71, 85, 105));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 8));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("登录", loginPanel());
        tabs.addTab("创建账户", registerPanel());

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(sidePanel(), BorderLayout.WEST);
        body.add(UiKit.card(tabs), BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JComponent sidePanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 10));
        panel.setBorder(new EmptyBorder(22, 24, 22, 24));
        panel.setBackground(new Color(30, 64, 175));
        panel.setPreferredSize(new java.awt.Dimension(210, 0));
        panel.add(sideLabel("教师端"));
        panel.add(sideLabel("作业  数据  成绩"));
        panel.add(sideLabel("学生端"));
        panel.add(sideLabel("提交  互评  查询"));
        return panel;
    }

    private JLabel sideLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        return label;
    }

    private JComponent loginPanel() {
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(18, 18, 12, 18));
        GridBagConstraints c = UiKit.formConstraints();
        int row = 0;
        row = UiKit.formRow(form, c, row, "登录端", loginRole);
        row = UiKit.formRow(form, c, row, "账号", loginUsername);
        row = UiKit.formRow(form, c, row, "密码", loginPassword);

        javax.swing.JButton login = UiKit.primaryButton("登录");
        login.addActionListener(e -> login());
        UiKit.formRow(form, c, row, "", UiKit.actions(login));
        return form;
    }

    private JComponent registerPanel() {
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(18, 18, 12, 18));
        GridBagConstraints c = UiKit.formConstraints();
        int row = 0;
        row = UiKit.formRow(form, c, row, "账户类型", registerRole);
        row = UiKit.formRow(form, c, row, "账号", registerUsername);
        row = UiKit.formRow(form, c, row, "密码", registerPassword);
        row = UiKit.formRow(form, c, row, "确认密码", registerConfirm);
        row = UiKit.formRow(form, c, row, teacherCreateCodeLabel, teacherCreateCode);
        row = UiKit.formRow(form, c, row, registerStudentNoLabel, registerStudentNo);
        row = UiKit.formRow(form, c, row, registerNameLabel, registerName);

        registerRole.addActionListener(e -> updateRegisterFields());
        updateRegisterFields();

        javax.swing.JButton register = UiKit.primaryButton("创建账户");
        register.addActionListener(e -> register());
        UiKit.formRow(form, c, row, "", UiKit.actions(register));
        return form;
    }

    private void login() {
        try {
            RoleChoice role = (RoleChoice) loginRole.getSelectedItem();
            RoleSession session = store.login(role.role, loginUsername.getText(), loginPassword.getPassword());
            if (session.role() == RoleSession.Role.TEACHER) {
                new TeacherFrame(store, session).setVisible(true);
            } else {
                new StudentFrame(store, session).setVisible(true);
            }
            loginPassword.setText("");
        } catch (IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void register() {
        try {
            char[] password = registerPassword.getPassword();
            char[] confirm = registerConfirm.getPassword();
            if (!Arrays.equals(password, confirm)) {
                throw new IllegalArgumentException("两次输入的密码不一致");
            }
            RoleChoice role = (RoleChoice) registerRole.getSelectedItem();
            if (role != null && role.role == RoleSession.Role.TEACHER &&
                    !TEACHER_CREATE_CODE.equals(new String(teacherCreateCode.getPassword()))) {
                throw new IllegalArgumentException("教师注册码错误");
            }
            store.registerAccount(role.role, registerUsername.getText(), password,
                    registerStudentNo.getText(), registerName.getText());
            JOptionPane.showMessageDialog(this, "账户创建成功，可以返回登录页登录。");
            registerPassword.setText("");
            registerConfirm.setText("");
            teacherCreateCode.setText("");
            registerUsername.setText("");
            registerStudentNo.setText("");
            registerName.setText("");
        } catch (IOException ex) {
            UiKit.error(this, ex);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "创建账户失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRegisterFields() {
        RoleChoice role = (RoleChoice) registerRole.getSelectedItem();
        boolean student = role != null && role.role == RoleSession.Role.STUDENT;
        boolean teacher = role != null && role.role == RoleSession.Role.TEACHER;
        teacherCreateCodeLabel.setVisible(teacher);
        teacherCreateCode.setVisible(teacher);
        registerStudentNoLabel.setVisible(student);
        registerStudentNo.setVisible(student);
        registerNameLabel.setVisible(student);
        registerName.setVisible(student);
        registerName.setToolTipText(student ? "学生姓名" : "教师姓名");
        revalidate();
        repaint();
    }

    private enum RoleChoice {
        TEACHER("教师端", RoleSession.Role.TEACHER),
        STUDENT("学生端", RoleSession.Role.STUDENT);

        final String label;
        final RoleSession.Role role;

        RoleChoice(String label, RoleSession.Role role) {
            this.label = label;
            this.role = role;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
