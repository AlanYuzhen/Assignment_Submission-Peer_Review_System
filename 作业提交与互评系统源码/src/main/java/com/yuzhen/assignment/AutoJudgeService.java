package com.yuzhen.assignment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class AutoJudgeService {
    private AutoJudgeService() {
    }

    public static JudgeResult judge(Assignment assignment, Path sourceFile) {
        JudgeResult result = new JudgeResult();
        if (!assignment.programming) {
            result.status = "待教师批阅";
            result.message = "文档作业不参与自动评测";
            result.score = 0;
            return result;
        }
        if (!sourceFile.getFileName().toString().toLowerCase().endsWith(".java")) {
            result.status = "格式错误";
            result.message = "编程作业仅支持 .java 源文件";
            result.score = 0;
            return result;
        }

        Path workDir = sourceFile.getParent().resolve("judge_" + System.nanoTime());
        try {
            Files.createDirectories(workDir);
            Path mainFile = workDir.resolve("Main.java");
            Files.writeString(mainFile, Files.readString(sourceFile, StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            String compileOut = new String(compile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int compileCode = compile.waitFor();
            if (compileCode != 0) {
                result.status = "编译错误";
                result.message = trim(compileOut);
                result.score = 0;
                return result;
            }

            Process run = new ProcessBuilder("java", "Main")
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            run.getOutputStream().write(assignment.sampleInput.getBytes(StandardCharsets.UTF_8));
            run.getOutputStream().close();
            boolean finished = run.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                run.destroyForcibly();
                result.status = "超时";
                result.message = "程序运行超时";
                result.score = 0;
                return result;
            }
            String actual = new String(run.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String expected = assignment.expectedOutput.trim();
            boolean pass = actual.equals(expected);
            result.status = pass ? "通过" : "失败";
            result.message = pass ? "输出与样例一致" : "期望输出: " + expected + "；实际输出: " + actual;
            result.score = pass ? 100 : 50;
            return result;
        } catch (IOException e) {
            result.status = "环境错误";
            result.message = "无法执行 javac/java: " + e.getMessage();
            result.score = 0;
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.status = "中断";
            result.message = "评测被中断";
            result.score = 0;
            return result;
        } finally {
            deleteTree(workDir);
        }
    }

    private static String trim(String text) {
        text = text == null ? "" : text.trim();
        return text.length() > 240 ? text.substring(0, 240) + "..." : text;
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            List<Path> paths = new ArrayList<>();
            Files.walk(root).forEach(paths::add);
            for (int i = paths.size() - 1; i >= 0; i--) {
                Files.deleteIfExists(paths.get(i));
            }
        } catch (IOException ignored) {
        }
    }
}
