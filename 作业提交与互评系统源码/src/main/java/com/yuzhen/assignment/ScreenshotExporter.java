package com.yuzhen.assignment;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class ScreenshotExporter {
    public static void main(String[] args) throws Exception {
        Path out = Paths.get(args.length > 0 ? args[0] : "screenshots");
        Files.createDirectories(out);

        SwingUtilities.invokeAndWait(() -> {
            try {
                MainFrame.installGlobalStyle();
                DataStore store = new DataStore(Paths.get("screenshot-data"));
                prepareDemoData(store);
                MainFrame frame = new MainFrame(store);
                try {
                    frame.setSize(1180, 780);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);

                    JTabbedPane tabs = findTabs(frame);
                    fillSimilarity(frame, store);
                    capture(frame, out.resolve("00_system_overview.png"));
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        tabs.setSelectedIndex(i);
                        frame.validate();
                        frame.repaint();
                        String name = String.format(Locale.ROOT, "%02d_%s.png", i + 1, safe(tabs.getTitleAt(i)));
                        capture(frame, out.resolve(name));
                    }
                } finally {
                    frame.dispose();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void prepareDemoData(DataStore store) throws Exception {
        Files.createDirectories(store.root);
        Files.createDirectories(store.submittedDir);
        store.assignments.clear();
        store.submissions.clear();
        store.reviews.clear();

        Assignment add = assignment(1, "Java基础加法练习",
                "提交 Java 程序，从标准输入读取两个整数并输出它们的和。",
                "1 2\n", "3\n", LocalDate.now().plusDays(7).toString());
        Assignment array = assignment(2, "数组最大值统计",
                "读取一组整数，输出其中最大值，用于练习数组遍历。",
                "5\n1 8 3 6 2\n", "8\n", LocalDate.now().plusDays(10).toString());
        store.assignments.add(add);
        store.assignments.add(array);

        Path s1Code = store.submittedDir.resolve("1_202503113099.java");
        Path s2Code = store.submittedDir.resolve("2_202503113100.java");
        Path s3Code = store.submittedDir.resolve("3_202503113101.java");
        Files.writeString(s1Code, "import java.util.*; class Main { public static void main(String[] args){ Scanner sc=new Scanner(System.in); System.out.println(sc.nextInt()+sc.nextInt()); } }", StandardCharsets.UTF_8);
        Files.writeString(s2Code, "import java.util.*; class Main { public static void main(String[] args){ Scanner input=new Scanner(System.in); int a=input.nextInt(); int b=input.nextInt(); System.out.println(a+b); } }", StandardCharsets.UTF_8);
        Files.writeString(s3Code, "import java.util.*; class Main { public static void main(String[] args){ Scanner sc=new Scanner(System.in); int n=sc.nextInt(); int max=Integer.MIN_VALUE; for(int i=0;i<n;i++){ max=Math.max(max, sc.nextInt()); } System.out.println(max); } }", StandardCharsets.UTF_8);
        Files.writeString(store.submittedDir.resolve("1_202503113099.txt"), Files.readString(s1Code, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Files.writeString(store.submittedDir.resolve("2_202503113100.txt"), Files.readString(s2Code, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Files.writeString(store.submittedDir.resolve("3_202503113101.txt"), Files.readString(s3Code, StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        store.submissions.add(submission(1, 1, "202503113099", "于震", s1Code, "1_202503113099.txt", "通过", "输出结果与标准答案一致。"));
        store.submissions.add(submission(2, 1, "202503113100", "李明", s2Code, "2_202503113100.txt", "通过", "输出结果与标准答案一致。"));
        store.submissions.add(submission(3, 2, "202503113101", "王芳", s3Code, "3_202503113101.txt", "未评测", ""));

        store.reviews.add(review(1, 1, "202503113100", 92, "思路清晰，变量命名规范，能够通过样例。"));
        store.reviews.add(review(2, 1, "202503113101", 88, "代码较简洁，建议补充边界输入处理。"));
        store.reviews.add(review(3, 2, "202503113099", 90, "实现正确，输入输出格式符合要求。"));
    }

    private static Assignment assignment(int id, String title, String description, String input, String output, String due) {
        Assignment a = new Assignment();
        a.id = id;
        a.title = title;
        a.description = description;
        a.sampleInput = input;
        a.expectedOutput = output;
        a.dueDate = LocalDate.parse(due);
        a.programming = true;
        a.peerReviewOpen = true;
        return a;
    }

    private static Submission submission(int id, int assignmentId, String no, String name, Path source, String textFile, String status, String output) {
        Submission s = new Submission();
        s.id = id;
        s.assignmentId = assignmentId;
        s.studentNo = no;
        s.studentName = name;
        s.fileName = source.getFileName().toString();
        s.filePath = source.toString();
        try {
            s.textContent = Files.readString(source.getParent().resolve(textFile), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            s.textContent = "";
        }
        s.submittedAt = LocalDateTime.now().minusHours(3L - id);
        s.judgeStatus = status;
        s.judgeMessage = output;
        s.autoScore = "閫氳繃".equals(status) || "通过".equals(status) ? 100 : 0;
        return s;
    }

    private static PeerReview review(int id, int submissionId, String reviewerNo, int score, String comment) {
        PeerReview r = new PeerReview();
        r.id = id;
        r.submissionId = submissionId;
        r.reviewerNo = reviewerNo;
        r.score = score;
        r.comment = comment;
        r.createdAt = LocalDateTime.now().minusMinutes(20L * id);
        return r;
    }

    private static void fillSimilarity(MainFrame frame, DataStore store) throws Exception {
        java.lang.reflect.Field field = MainFrame.class.getDeclaredField("similarityModel");
        field.setAccessible(true);
        DefaultTableModel model = (DefaultTableModel) field.get(frame);
        model.setRowCount(0);
        List<SimilarityResult> results = PlagiarismService.compare(store.submissionsOf(1));
        for (SimilarityResult r : results) {
            model.addRow(new Object[]{
                    r.left.id, r.right.id, r.left.studentName, r.right.studentName,
                    String.format(Locale.ROOT, "%.2f%%", r.score * 100),
                    r.score >= 0.8 ? "高风险" : r.score >= 0.5 ? "需复核" : "正常"
            });
        }
    }

    private static JTabbedPane findTabs(Container container) {
        JTabbedPane found = findTabsOrNull(container);
        if (found == null) {
            throw new IllegalStateException("Cannot find tabbed pane in:\n" + tree(container, 0));
        }
        return found;
    }

    private static JTabbedPane findTabsOrNull(Container container) {
        if (container instanceof JTabbedPane) {
            return (JTabbedPane) container;
        }
        for (Component component : container.getComponents()) {
            if (component instanceof JTabbedPane) {
                return (JTabbedPane) component;
            }
            if (component instanceof Container) {
                JTabbedPane found = findTabsOrNull((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String tree(Component component, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append(component.getClass().getName()).append('\n');
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                sb.append(tree(child, level + 1));
            }
        }
        return sb.toString();
    }

    private static void capture(JFrame frame, Path file) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        frame.paint(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", file.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String safe(String text) {
        return text.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
