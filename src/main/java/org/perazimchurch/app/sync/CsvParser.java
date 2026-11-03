package org.perazimchurch.app.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight, zero-dependency, RFC 4180-compliant CSV parser.
 * Handles:
 * - Multiline quoted fields (e.g. lyrics and chords with newlines)
 * - Escaped double quotes ("" -> ")
 * - UTF-8 Byte Order Mark (BOM: \uFEFF) stripping
 * - Swahili diacritics and international UTF-8 characters
 * - Trailing empty fields and CRLF / LF line endings
 */
public final class CsvParser {

    private CsvParser() {}

    /**
     * Parses a raw CSV string into a list of rows, where each row is a list of string tokens.
     *
     * @param csvContent The raw CSV content
     * @return List of rows with parsed cells
     */
    public static List<List<String>> parse(String csvContent) throws IOException {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Strip UTF-8 BOM if present
        if (csvContent.startsWith("\uFEFF")) {
            csvContent = csvContent.substring(1);
        }
        return parse(new StringReader(csvContent));
    }

    /**
     * Parses CSV data from a Reader.
     *
     * @param reader Source character stream
     * @return List of rows with parsed cells
     */
    public static List<List<String>> parse(Reader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        BufferedReader in = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);

        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int prevChar = -1;

        int ch;
        while ((ch = in.read()) != -1) {
            char c = (char) ch;

            if (inQuotes) {
                if (c == '"') {
                    // Peek or check if escaped quote
                    in.mark(1);
                    int next = in.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        in.reset();
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentField.toString().trim());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    // Handle CRLF: peek next char
                    in.mark(1);
                    int next = in.read();
                    if (next != '\n') {
                        in.reset();
                    }
                    currentRow.add(currentField.toString().trim());
                    currentField.setLength(0);
                    if (!isEmptyRow(currentRow)) {
                        rows.add(new ArrayList<>(currentRow));
                    }
                    currentRow.clear();
                } else if (c == '\n') {
                    currentRow.add(currentField.toString().trim());
                    currentField.setLength(0);
                    if (!isEmptyRow(currentRow)) {
                        rows.add(new ArrayList<>(currentRow));
                    }
                    currentRow.clear();
                } else {
                    currentField.append(c);
                }
            }
            prevChar = ch;
        }

        // Add last field and row if any
        if (currentField.length() > 0 || prevChar == ',') {
            currentRow.add(currentField.toString().trim());
        }
        if (!isEmptyRow(currentRow)) {
            rows.add(new ArrayList<>(currentRow));
        }

        return rows;
    }

    private static boolean isEmptyRow(List<String> row) {
        if (row == null || row.isEmpty()) return true;
        for (String cell : row) {
            if (!cell.isEmpty()) return false;
        }
        return true;
    }
}
