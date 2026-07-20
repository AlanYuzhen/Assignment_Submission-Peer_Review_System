package com.yuzhen.assignment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class TextExtractor {
    private static final Pattern XML_TAG = Pattern.compile("<[^>]+>");

    private TextExtractor() {
    }

    public static String extract(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".java") || name.endsWith(".txt")) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        if (name.endsWith(".docx")) {
            return readDocx(file);
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static String readDocx(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zip.transferTo(out);
                    String xml = out.toString(StandardCharsets.UTF_8);
                    return XML_TAG.matcher(xml).replaceAll(" ").replaceAll("\\s+", " ").trim();
                }
            }
        }
        return "";
    }
}
