package com.yuzhen.assignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PlagiarismService {
    private PlagiarismService() {
    }

    public static List<SimilarityResult> compare(List<Submission> submissions) {
        List<SimilarityResult> results = new ArrayList<>();
        for (int i = 0; i < submissions.size(); i++) {
            for (int j = i + 1; j < submissions.size(); j++) {
                SimilarityResult result = new SimilarityResult();
                result.left = submissions.get(i);
                result.right = submissions.get(j);
                result.score = similarity(tokens(result.left.textContent), tokens(result.right.textContent));
                results.add(result);
            }
        }
        return results;
    }

    private static Set<String> tokens(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) {
            return set;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (!token.isBlank()) {
                set.add(token);
            }
        }
        return set;
    }

    private static double similarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1;
        }
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        Set<String> intersect = new HashSet<>(a);
        intersect.retainAll(b);
        return union.isEmpty() ? 0 : (double) intersect.size() / union.size();
    }
}
