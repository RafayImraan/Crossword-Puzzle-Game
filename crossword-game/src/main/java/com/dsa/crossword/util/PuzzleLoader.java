package com.dsa.crossword.util;

import com.dsa.crossword.model.Word;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads puzzle word lists from .txt files or built-in resources.
 *
 * File format (one entry per line):
 *   WORD = Clue text
 * Lines starting with '#' are comments. Blank lines are ignored.
 */
public final class PuzzleLoader {

    private PuzzleLoader() {}

    public static List<Word> fromResource(String resourcePath) throws IOException {
        InputStream is = PuzzleLoader.class.getResourceAsStream(resourcePath);
        if (is == null) throw new IOException("Resource not found: " + resourcePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return parse(br);
        }
    }

    public static List<Word> fromFile(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(br);
        }
    }

    private static List<Word> parse(BufferedReader br) throws IOException {
        List<Word> words = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String w = line.substring(0, eq).trim();
            String clue = line.substring(eq + 1).trim();
            if (w.isEmpty()) continue;
            words.add(new Word(w, clue));
        }
        return words;
    }
}
