package com.dujiao.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * 与 Go {@code parseCSVSecrets} / {@code normalizeSecrets} 行为对齐。
 */
public final class CardSecretCsvParser {

    private CardSecretCsvParser() {}

    public static List<String> parse(InputStream in) throws IOException {
        CSVFormat format =
                CSVFormat.DEFAULT.builder()
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build();
        try (CSVParser parser =
                format.parse(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            boolean headerRead = false;
            int secretIdx = 0;
            List<String> raw = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() == 0) {
                    continue;
                }
                if (!headerRead) {
                    headerRead = true;
                    boolean skipRow = false;
                    for (int i = 0; i < record.size(); i++) {
                        String col = stripBom(record.get(i)).trim();
                        if ("secret".equalsIgnoreCase(col)) {
                            secretIdx = i;
                            skipRow = true;
                            break;
                        }
                    }
                    if (skipRow) {
                        continue;
                    }
                }
                if (secretIdx >= record.size()) {
                    continue;
                }
                String secret = stripBom(record.get(secretIdx)).trim();
                if (!secret.isEmpty()) {
                    raw.add(secret);
                }
            }
            return normalizeSecrets(raw);
        }
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\ufeff') {
            return s.substring(1);
        }
        return s == null ? "" : s;
    }

    static List<String> normalizeSecrets(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String val : values) {
            if (val == null) {
                continue;
            }
            for (String line : val.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || seen.contains(trimmed)) {
                    continue;
                }
                seen.add(trimmed);
                result.add(trimmed);
            }
        }
        return result;
    }
}
