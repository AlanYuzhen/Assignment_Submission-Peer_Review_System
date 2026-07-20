package com.yuzhen.assignment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore {
    public final Path root;
    public final Path submittedDir;
    public final List<Assignment> assignments = new ArrayList<>();
    public final List<Submission> submissions = new ArrayList<>();
    public final List<PeerReview> reviews = new ArrayList<>();
    private final List<UserAccount> accounts = new ArrayList<>();
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<StudentProfile> DEMO_STUDENTS = List.of(
            new StudentProfile("202503113099", "于震"),
            new StudentProfile("202503113100", "李明"),
            new StudentProfile("202503113101", "王芳"),
            new StudentProfile("202503113102", "张敏")
    );

    public DataStore(Path root) throws IOException {
        this.root = root;
        this.submittedDir = root.resolve("submitted");
        Files.createDirectories(root);
        Files.createDirectories(submittedDir);
        load();
        boolean changed = false;
        if (assignments.isEmpty()) {
            seedDemoData();
            changed = true;
        }
        if (accounts.isEmpty()) {
            seedDefaultAccounts();
            changed = true;
        }
        if (changed) {
            saveAll();
        }
    }

    public List<StudentProfile> getStudents() {
        Map<String, StudentProfile> merged = new LinkedHashMap<>();
        for (StudentProfile student : DEMO_STUDENTS) {
            merged.put(student.studentNo, student);
        }
        for (UserAccount account : accounts) {
            if (account.role == RoleSession.Role.STUDENT && account.studentNo != null && !account.studentNo.isBlank()) {
                merged.putIfAbsent(account.studentNo, new StudentProfile(account.studentNo, account.name));
            }
        }
        return List.copyOf(merged.values());
    }

    public RoleSession login(RoleSession.Role role, String username, char[] password) {
        String normalized = normalizeUsername(username);
        UserAccount account = accounts.stream()
                .filter(a -> a.role == role && a.username.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
        if (account == null || !verifyPassword(account, password)) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (role == RoleSession.Role.TEACHER) {
            return RoleSession.teacher();
        }
        return RoleSession.student(new StudentProfile(account.studentNo, account.name));
    }

    public void registerAccount(RoleSession.Role role, String username, char[] password, String studentNo, String name) throws IOException {
        String normalized = normalizeUsername(username);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (password == null || password.length < 6) {
            throw new IllegalArgumentException("密码至少需要 6 位");
        }
        boolean exists = accounts.stream().anyMatch(a -> a.username.equalsIgnoreCase(normalized));
        if (exists) {
            throw new IllegalArgumentException("账号已存在");
        }

        UserAccount account = new UserAccount();
        account.role = role;
        account.username = normalized;
        account.passwordSalt = randomSalt();
        account.passwordHash = hashPassword(account.passwordSalt, password);
        if (role == RoleSession.Role.STUDENT) {
            account.studentNo = studentNo == null ? "" : studentNo.trim();
            account.name = name == null ? "" : name.trim();
            if (account.studentNo.isBlank() || account.name.isBlank()) {
                throw new IllegalArgumentException("学生账号需要填写学号和姓名");
            }
        } else {
            account.studentNo = "";
            account.name = name == null ? "教师" : name.trim();
            if (account.name.isBlank()) {
                account.name = "教师";
            }
        }
        accounts.add(account);
        saveAccounts();
        notifyChanged();
    }

    public void saveAssignment(RoleSession session, Assignment assignment) throws IOException {
        session.requireTeacher();
        saveAssignment(assignment);
    }

    public void deleteAssignment(RoleSession session, int assignmentId) throws IOException {
        session.requireTeacher();
        deleteAssignment(assignmentId);
    }

    public Submission saveSubmission(RoleSession session, int assignmentId, Path source) throws IOException {
        StudentProfile student = session.requireStudent();
        return saveSubmission(assignmentId, student, source);
    }

    public void saveReview(RoleSession session, PeerReview review) throws IOException {
        session.requireStudent(review.reviewerNo);
        Submission target = findSubmission(review.submissionId);
        if (target == null) {
            throw new IOException("提交记录不存在");
        }
        if (target.studentNo.equals(review.reviewerNo)) {
            throw new IOException("不能互评自己的作业");
        }
        Assignment assignment = findAssignment(target.assignmentId);
        if (assignment == null) {
            throw new IOException("作业不存在");
        }
        if (!assignment.peerReviewOpen) {
            throw new IOException("该作业尚未开放互评");
        }
        review.assignmentId = assignment.id;
        saveReview(review);
    }

    public void arbitrateReview(RoleSession session, int reviewId, int score, String comment) throws IOException {
        session.requireTeacher();
        if (score < 0 || score > 100) {
            throw new IOException("仲裁分数必须在 0 到 100 之间");
        }
        PeerReview review = reviews.stream().filter(r -> r.id == reviewId).findFirst().orElse(null);
        if (review == null) {
            throw new IOException("互评记录不存在");
        }
        review.score = score;
        review.comment = comment == null ? "" : comment.trim();
        review.disputed = false;
        review.resolvedByTeacher = true;
        saveReviews();
        notifyChanged();
    }

    public void flagPlagiarism(RoleSession session, int assignmentId, double threshold) throws IOException {
        session.requireTeacher();
        flagPlagiarism(assignmentId, threshold);
    }

    public Submission randomReviewTarget(RoleSession session) {
        StudentProfile student = session.requireStudent();
        return randomReviewTarget(student.studentNo);
    }

    public void addListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    public void notifyChanged() {
        for (DataChangeListener listener : listeners) {
            listener.onDataChanged();
        }
    }

    public Assignment findAssignment(int assignmentId) {
        return assignments.stream().filter(a -> a.id == assignmentId).findFirst().orElse(null);
    }

    public Submission findSubmission(int submissionId) {
        return submissions.stream().filter(s -> s.id == submissionId).findFirst().orElse(null);
    }

    public StudentProfile findStudent(String studentNo) {
        return getStudents().stream().filter(s -> s.studentNo.equals(studentNo)).findFirst().orElse(null);
    }

    public List<Submission> submissionsOf(int assignmentId) {
        return submissions.stream().filter(s -> s.assignmentId == assignmentId).sorted(Comparator.comparing(s -> s.submittedAt)).toList();
    }

    public List<Submission> submissionsOfStudent(String studentNo) {
        return submissions.stream().filter(s -> s.studentNo.equals(studentNo)).sorted(Comparator.comparing((Submission s) -> s.submittedAt).reversed()).toList();
    }

    public List<PeerReview> reviewsOfSubmission(int submissionId) {
        return reviews.stream().filter(r -> r.submissionId == submissionId).sorted(Comparator.comparing(r -> r.createdAt)).toList();
    }

    public void saveAssignment(Assignment assignment) throws IOException {
        if (assignment.id == 0) {
            assignment.id = nextAssignmentId();
            assignments.add(assignment);
        }
        saveAssignments();
        notifyChanged();
    }

    public void deleteAssignment(int assignmentId) throws IOException {
        assignments.removeIf(a -> a.id == assignmentId);
        List<Integer> submissionIds = submissions.stream().filter(s -> s.assignmentId == assignmentId).map(s -> s.id).toList();
        submissions.removeIf(s -> s.assignmentId == assignmentId);
        reviews.removeIf(r -> r.assignmentId == assignmentId || submissionIds.contains(r.submissionId));
        saveAll();
        notifyChanged();
    }

    public Submission saveSubmission(int assignmentId, StudentProfile student, Path source) throws IOException {
        Assignment assignment = findAssignment(assignmentId);
        if (assignment == null) {
            throw new IOException("作业不存在");
        }
        if (findStudent(student.studentNo) == null) {
            throw new IOException("学生身份不存在");
        }
        if (!assignment.isOpen(LocalDate.now())) {
            throw new IOException("该作业已截止，不能提交");
        }
        String lowerName = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".java") && !lowerName.endsWith(".docx")) {
            throw new IOException("仅支持上传 .java 源文件或 .docx 文档");
        }
        if (assignment.programming && !lowerName.endsWith(".java")) {
            throw new IOException("编程作业仅支持上传 .java 源文件");
        }
        Submission submission = new Submission();
        submission.id = nextSubmissionId();
        submission.assignmentId = assignmentId;
        submission.studentNo = student.studentNo;
        submission.studentName = student.name;
        submission.fileName = source.getFileName().toString();
        Path saved = submittedDir.resolve(submission.id + "_" + student.studentNo + "_" + source.getFileName());
        Files.copy(source, saved, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        submission.filePath = saved.toString();
        submission.textContent = TextExtractor.extract(saved);
        submission.submittedAt = LocalDateTime.now();
        JudgeResult judge = AutoJudgeService.judge(assignment, saved);
        submission.judgeStatus = judge.status;
        submission.judgeMessage = judge.message;
        submission.autoScore = judge.score;
        submissions.add(submission);
        saveSubmissions();
        notifyChanged();
        return submission;
    }

    public void saveReview(PeerReview review) throws IOException {
        PeerReview existing = reviews.stream()
                .filter(r -> r.submissionId == review.submissionId && r.reviewerNo.equals(review.reviewerNo))
                .findFirst().orElse(null);
        if (existing == null) {
            review.id = nextReviewId();
            review.createdAt = LocalDateTime.now();
            reviews.add(review);
        } else {
            existing.score = review.score;
            existing.comment = review.comment;
            existing.disputed = review.disputed;
            existing.resolvedByTeacher = review.resolvedByTeacher;
        }
        saveReviews();
        notifyChanged();
    }

    public double averagePeerScore(int submissionId) {
        List<PeerReview> list = reviewsOfSubmission(submissionId);
        if (list.isEmpty()) {
            return 0;
        }
        return list.stream().mapToInt(r -> r.score).average().orElse(0);
    }

    public double finalScore(Submission submission) {
        double peer = averagePeerScore(submission.id);
        return Math.round((submission.autoScore * 0.6 + peer * 0.4) * 10.0) / 10.0;
    }

    public Submission randomReviewTarget(String reviewerNo) {
        List<Submission> candidates = new ArrayList<>();
        for (Submission submission : submissions) {
            Assignment assignment = findAssignment(submission.assignmentId);
            boolean alreadyReviewed = reviews.stream()
                    .anyMatch(r -> r.submissionId == submission.id && r.reviewerNo.equals(reviewerNo));
            if (assignment != null && assignment.peerReviewOpen &&
                    !submission.studentNo.equals(reviewerNo) && !alreadyReviewed) {
                candidates.add(submission);
            }
        }
        Collections.shuffle(candidates);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public void flagPlagiarism(int assignmentId, double threshold) throws IOException {
        List<SimilarityResult> results = PlagiarismService.compare(submissionsOf(assignmentId));
        for (Submission submission : submissions) {
            if (submission.assignmentId == assignmentId) {
                submission.plagiarismFlag = false;
            }
        }
        for (SimilarityResult result : results) {
            if (result.score >= threshold) {
                result.left.plagiarismFlag = true;
                result.right.plagiarismFlag = true;
            }
        }
        saveSubmissions();
        notifyChanged();
    }

    private void load() throws IOException {
        loadAssignments();
        loadSubmissions();
        loadReviews();
        loadAccounts();
    }

    private void saveAll() throws IOException {
        saveAssignments();
        saveSubmissions();
        saveReviews();
        saveAccounts();
    }

    private void seedDemoData() throws IOException {
        Assignment a1 = new Assignment();
        a1.id = 1;
        a1.title = "Java基础加法练习";
        a1.description = "提交一个 Java 程序，从标准输入读取两个整数并输出它们的和。";
        a1.sampleInput = "1 2\n";
        a1.expectedOutput = "3\n";
        a1.dueDate = LocalDate.now().plusDays(7);
        a1.programming = true;
        a1.peerReviewOpen = true;
        assignments.add(a1);

        Assignment a2 = new Assignment();
        a2.id = 2;
        a2.title = "需求分析文档";
        a2.description = "提交课程设计需求分析说明文档，要求结构完整。";
        a2.sampleInput = "";
        a2.expectedOutput = "";
        a2.dueDate = LocalDate.now().plusDays(4);
        a2.programming = false;
        a2.peerReviewOpen = true;
        assignments.add(a2);
    }

    private void seedDefaultAccounts() {
        addSeedAccount(RoleSession.Role.TEACHER, "teacher", "teacher123", "", "教师");
        for (StudentProfile student : DEMO_STUDENTS) {
            addSeedAccount(RoleSession.Role.STUDENT, student.studentNo, "123456", student.studentNo, student.name);
        }
    }

    private void addSeedAccount(RoleSession.Role role, String username, String password, String studentNo, String name) {
        UserAccount account = new UserAccount();
        account.role = role;
        account.username = username;
        account.passwordSalt = randomSalt();
        account.passwordHash = hashPassword(account.passwordSalt, password.toCharArray());
        account.studentNo = studentNo;
        account.name = name;
        accounts.add(account);
    }

    private void loadAssignments() throws IOException {
        assignments.clear();
        Path file = root.resolve("assignments.tsv");
        if (!Files.exists(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] p = line.split("\t", -1);
            Assignment a = new Assignment();
            a.id = Integer.parseInt(p[0]);
            a.title = decode(p[1]);
            a.description = decode(p[2]);
            a.sampleInput = decode(p[3]);
            a.expectedOutput = decode(p[4]);
            a.dueDate = LocalDate.parse(p[5]);
            a.programming = p.length > 6 && Boolean.parseBoolean(p[6]);
            a.peerReviewOpen = p.length > 7 && Boolean.parseBoolean(p[7]);
            assignments.add(a);
        }
    }

    private void saveAssignments() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Assignment a : assignments) {
            lines.add(a.id + "\t" + encode(a.title) + "\t" + encode(a.description) + "\t" +
                    encode(a.sampleInput) + "\t" + encode(a.expectedOutput) + "\t" + a.dueDate + "\t" +
                    a.programming + "\t" + a.peerReviewOpen);
        }
        Files.write(root.resolve("assignments.tsv"), lines, StandardCharsets.UTF_8);
    }

    private void loadSubmissions() throws IOException {
        submissions.clear();
        Path file = root.resolve("submissions.tsv");
        if (!Files.exists(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] p = line.split("\t", -1);
            Submission s = new Submission();
            s.id = Integer.parseInt(p[0]);
            s.assignmentId = Integer.parseInt(p[1]);
            s.studentNo = p[2];
            s.studentName = decode(p[3]);
            s.fileName = decode(p[4]);
            s.filePath = decode(p[5]);
            s.textContent = decode(p[6]);
            s.submittedAt = LocalDateTime.parse(p[7]);
            s.judgeStatus = decode(p[8]);
            s.judgeMessage = decode(p[9]);
            s.autoScore = Double.parseDouble(p[10]);
            s.plagiarismFlag = p.length > 11 && Boolean.parseBoolean(p[11]);
            submissions.add(s);
        }
    }

    private void saveSubmissions() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Submission s : submissions) {
            lines.add(s.id + "\t" + s.assignmentId + "\t" + s.studentNo + "\t" + encode(s.studentName) + "\t" +
                    encode(s.fileName) + "\t" + encode(s.filePath) + "\t" + encode(s.textContent) + "\t" +
                    s.submittedAt + "\t" + encode(s.judgeStatus) + "\t" + encode(s.judgeMessage) + "\t" +
                    String.format(Locale.ROOT, "%.1f", s.autoScore) + "\t" + s.plagiarismFlag);
        }
        Files.write(root.resolve("submissions.tsv"), lines, StandardCharsets.UTF_8);
    }

    private void loadReviews() throws IOException {
        reviews.clear();
        Path file = root.resolve("reviews.tsv");
        if (!Files.exists(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] p = line.split("\t", -1);
            PeerReview r = new PeerReview();
            r.id = Integer.parseInt(p[0]);
            r.assignmentId = Integer.parseInt(p[1]);
            r.submissionId = Integer.parseInt(p[2]);
            r.reviewerNo = p[3];
            r.score = Integer.parseInt(p[4]);
            r.comment = decode(p[5]);
            r.createdAt = LocalDateTime.parse(p[6]);
            r.disputed = p.length > 7 && Boolean.parseBoolean(p[7]);
            r.resolvedByTeacher = p.length > 8 && Boolean.parseBoolean(p[8]);
            reviews.add(r);
        }
    }

    private void saveReviews() throws IOException {
        List<String> lines = new ArrayList<>();
        for (PeerReview r : reviews) {
            lines.add(r.id + "\t" + r.assignmentId + "\t" + r.submissionId + "\t" + r.reviewerNo + "\t" + r.score + "\t" +
                    encode(r.comment) + "\t" + r.createdAt + "\t" + r.disputed + "\t" + r.resolvedByTeacher);
        }
        Files.write(root.resolve("reviews.tsv"), lines, StandardCharsets.UTF_8);
    }

    private void loadAccounts() throws IOException {
        accounts.clear();
        Path file = root.resolve("accounts.tsv");
        if (!Files.exists(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] p = line.split("\t", -1);
            UserAccount account = new UserAccount();
            account.role = RoleSession.Role.valueOf(p[0]);
            account.username = decode(p[1]);
            account.passwordSalt = p[2];
            account.passwordHash = p[3];
            account.studentNo = p.length > 4 ? decode(p[4]) : "";
            account.name = p.length > 5 ? decode(p[5]) : "";
            accounts.add(account);
        }
    }

    private void saveAccounts() throws IOException {
        List<String> lines = new ArrayList<>();
        for (UserAccount account : accounts) {
            lines.add(account.role + "\t" + encode(account.username) + "\t" + account.passwordSalt + "\t" +
                    account.passwordHash + "\t" + encode(account.studentNo) + "\t" + encode(account.name));
        }
        Files.write(root.resolve("accounts.tsv"), lines, StandardCharsets.UTF_8);
    }

    private int nextAssignmentId() {
        return assignments.stream().mapToInt(a -> a.id).max().orElse(0) + 1;
    }

    private int nextSubmissionId() {
        return submissions.stream().mapToInt(s -> s.id).max().orElse(0) + 1;
    }

    private int nextReviewId() {
        return reviews.stream().mapToInt(r -> r.id).max().orElse(0) + 1;
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private static String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static boolean verifyPassword(UserAccount account, char[] password) {
        return MessageDigest.isEqual(
                account.passwordHash.getBytes(StandardCharsets.UTF_8),
                hashPassword(account.passwordSalt, password).getBytes(StandardCharsets.UTF_8));
    }

    private static String hashPassword(String salt, char[] password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            digest.update(new String(password == null ? new char[0] : password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", ex);
        }
    }
}
