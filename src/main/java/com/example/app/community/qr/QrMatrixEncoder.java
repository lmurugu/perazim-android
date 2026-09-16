package com.example.app.community.qr;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-Java standards-compliant implementation of the ISO/IEC 18004 QR Code matrix generator.
 * Produces genuine, standards-compliant 2D QR matrices readable by any smartphone camera
 * (Google Lens, iPhone Camera, ZXing, Barcode Scanners).
 * <p>
 * Zero external dependencies.
 */
public final class QrMatrixEncoder {

    public enum ErrorCorrectionLevel {
        L(1), // 01 in binary
        M(0), // 00 in binary
        Q(3), // 11 in binary
        H(2); // 10 in binary

        public final int bits;

        ErrorCorrectionLevel(int bits) {
            this.bits = bits;
        }
    }

    // --- Galois Field GF(256) Tables (P(x) = x^8 + x^4 + x^3 + x^2 + 1 = 0x11D / 285) ---
    private static final int[] EXP_TABLE = new int[512];
    private static final int[] LOG_TABLE = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP_TABLE[i] = x;
            EXP_TABLE[i + 255] = x;
            LOG_TABLE[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= 0x11D;
            }
        }
    }

    private static int gfMultiply(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP_TABLE[LOG_TABLE[a] + LOG_TABLE[b]];
    }

    // --- Version Specifications for Versions 1 to 10 (ISO/IEC 18004 Tables 1 & 7) ---
    private static class VersionSpec {
        final int version;
        final int dimension;
        final int[] alignmentCoords;
        final int remainderBits;

        // Level L
        final int totalDataCodewordsL;
        final int ecPerBlockL;
        final int numBlocksG1L;
        final int dataPerBlockG1L;
        final int numBlocksG2L;
        final int dataPerBlockG2L;

        // Level M
        final int totalDataCodewordsM;
        final int ecPerBlockM;
        final int numBlocksG1M;
        final int dataPerBlockG1M;
        final int numBlocksG2M;
        final int dataPerBlockG2M;

        VersionSpec(int version, int remainderBits, int[] alignmentCoords,
                    int totalDataCodewordsL, int ecPerBlockL, int numBlocksG1L, int dataPerBlockG1L, int numBlocksG2L, int dataPerBlockG2L,
                    int totalDataCodewordsM, int ecPerBlockM, int numBlocksG1M, int dataPerBlockG1M, int numBlocksG2M, int dataPerBlockG2M) {
            this.version = version;
            this.dimension = 17 + (4 * version);
            this.remainderBits = remainderBits;
            this.alignmentCoords = alignmentCoords;

            this.totalDataCodewordsL = totalDataCodewordsL;
            this.ecPerBlockL = ecPerBlockL;
            this.numBlocksG1L = numBlocksG1L;
            this.dataPerBlockG1L = dataPerBlockG1L;
            this.numBlocksG2L = numBlocksG2L;
            this.dataPerBlockG2L = dataPerBlockG2L;

            this.totalDataCodewordsM = totalDataCodewordsM;
            this.ecPerBlockM = ecPerBlockM;
            this.numBlocksG1M = numBlocksG1M;
            this.dataPerBlockG1M = dataPerBlockG1M;
            this.numBlocksG2M = numBlocksG2M;
            this.dataPerBlockG2M = dataPerBlockG2M;
        }
    }

    private static final VersionSpec[] VERSION_SPECS = {
            null, // 1-indexed
            new VersionSpec(1, 0, new int[]{},
                    19, 7, 1, 19, 0, 0,
                    16, 10, 1, 16, 0, 0),
            new VersionSpec(2, 7, new int[]{6, 18},
                    34, 10, 1, 34, 0, 0,
                    28, 16, 1, 28, 0, 0),
            new VersionSpec(3, 7, new int[]{6, 22},
                    55, 15, 1, 55, 0, 0,
                    44, 26, 1, 44, 0, 0),
            new VersionSpec(4, 7, new int[]{6, 26},
                    80, 20, 1, 80, 0, 0,
                    64, 18, 2, 32, 0, 0),
            new VersionSpec(5, 7, new int[]{6, 30},
                    108, 26, 1, 108, 0, 0,
                    86, 24, 2, 43, 0, 0),
            new VersionSpec(6, 7, new int[]{6, 34},
                    136, 18, 2, 68, 0, 0,
                    108, 16, 4, 27, 0, 0),
            new VersionSpec(7, 0, new int[]{6, 22, 38},
                    156, 20, 2, 78, 0, 0,
                    124, 18, 4, 31, 0, 0),
            new VersionSpec(8, 0, new int[]{6, 24, 42},
                    194, 24, 2, 97, 0, 0,
                    154, 22, 2, 38, 2, 39),
            new VersionSpec(9, 0, new int[]{6, 26, 46},
                    232, 30, 2, 116, 0, 0,
                    182, 22, 3, 36, 2, 37),
            new VersionSpec(10, 0, new int[]{6, 28, 50},
                    274, 18, 2, 68, 2, 69,
                    216, 26, 4, 43, 1, 44)
    };

    private QrMatrixEncoder() {}

    /**
     * Encodes text using ISO/IEC 18004 standard QR code generation.
     *
     * @param text The text payload to encode.
     * @return 2D boolean array where true = dark module, false = light module.
     */
    @NonNull
    public static boolean[][] encode(@NonNull String text) {
        return encode(text, 1, ErrorCorrectionLevel.L);
    }

    /**
     * Encodes text with a minimum version constraint.
     *
     * @param text       The text payload to encode.
     * @param minVersion Minimum QR version (1 to 10).
     * @return 2D boolean array where true = dark module, false = light module.
     */
    @NonNull
    public static boolean[][] encode(@NonNull String text, int minVersion) {
        return encode(text, minVersion, ErrorCorrectionLevel.L);
    }

    /**
     * Encodes text with specified minimum version and error correction level.
     *
     * @param text       The text payload to encode.
     * @param minVersion Minimum QR version (1 to 10).
     * @param ecLevel    Error correction level (L, M).
     * @return 2D boolean array where true = dark module, false = light module.
     */
    @NonNull
    public static boolean[][] encode(@NonNull String text, int minVersion, @NonNull ErrorCorrectionLevel ecLevel) {
        byte[] dataBytes = (text != null ? text : "").getBytes(StandardCharsets.UTF_8);

        // 1. Select optimal version (1 to 10) that can accommodate the data
        VersionSpec spec = selectVersion(dataBytes.length, Math.max(1, minVersion), ecLevel);

        // 2. Build data bit stream with Mode Indicator (0100), Character Count, and Data Bits
        int totalDataCodewords = (ecLevel == ErrorCorrectionLevel.L)
                ? spec.totalDataCodewordsL
                : spec.totalDataCodewordsM;

        byte[] encodedDataCodewords = encodeDataCodewords(dataBytes, spec.version, totalDataCodewords);

        // 3. Generate Reed-Solomon Error Correction Codewords & Interleave
        byte[] finalCodewords = generateAndInterleave(encodedDataCodewords, spec, ecLevel);

        // 4. Populate Matrix Function Patterns (Finder, Separators, Timing, Alignment)
        int dim = spec.dimension;
        boolean[][] matrix = new boolean[dim][dim];
        boolean[][] reserved = new boolean[dim][dim];

        setupFinderPatterns(matrix, reserved, dim);
        setupAlignmentPatterns(matrix, reserved, spec.alignmentCoords, dim);
        setupTimingPatterns(matrix, reserved, dim);
        setupDarkModule(matrix, reserved, dim);
        reserveFormatInfo(reserved, dim);

        if (spec.version >= 7) {
            reserveVersionInfo(reserved, dim);
        }

        // 5. Place Data and Error Correction Codewords into Matrix (Zig-Zag order)
        placeCodewordsInMatrix(matrix, reserved, finalCodewords, spec.remainderBits, dim);

        // 6. Evaluate all 8 Mask Patterns and apply the best mask
        int bestMask = findBestMaskPattern(matrix, reserved, dim);
        applyMask(matrix, reserved, bestMask, dim);

        // 7. Write Format Information Bits
        writeFormatInfo(matrix, ecLevel, bestMask, dim);

        // 8. Write Version Information Bits (for Version 7+)
        if (spec.version >= 7) {
            writeVersionInfo(matrix, spec.version, dim);
        }

        return matrix;
    }

    private static VersionSpec selectVersion(int byteLen, int minVersion, ErrorCorrectionLevel ecLevel) {
        for (int v = Math.min(minVersion, 10); v <= 10; v++) {
            VersionSpec s = VERSION_SPECS[v];
            int totalData = (ecLevel == ErrorCorrectionLevel.L) ? s.totalDataCodewordsL : s.totalDataCodewordsM;
            int countBits = (v < 10) ? 8 : 16;
            int maxBytes = (totalData * 8 - 4 - countBits) / 8;
            if (byteLen <= maxBytes) {
                return s;
            }
        }
        // If exceeds Version 10, fallback to Version 10
        return VERSION_SPECS[10];
    }

    private static byte[] encodeDataCodewords(byte[] data, int version, int totalCodewords) {
        BitBuffer buffer = new BitBuffer();

        // 1. Mode Indicator: 0100 for Byte Mode (4 bits)
        buffer.appendBits(0b0100, 4);

        // 2. Character Count Indicator (8 bits for V1-9, 16 bits for V10+)
        int countBits = (version < 10) ? 8 : 16;
        buffer.appendBits(data.length, countBits);

        // 3. Data bytes (8 bits each)
        for (byte b : data) {
            buffer.appendBits(b & 0xFF, 8);
        }

        // 4. Terminator (up to 4 zero bits)
        int totalBits = totalCodewords * 8;
        int remaining = totalBits - buffer.getBitLength();
        int termLen = Math.min(4, Math.max(0, remaining));
        if (termLen > 0) {
            buffer.appendBits(0, termLen);
        }

        // 5. Pad to byte boundary
        int padBits = (8 - (buffer.getBitLength() % 8)) % 8;
        if (padBits > 0) {
            buffer.appendBits(0, padBits);
        }

        // 6. Pad with alternating 0xEC and 0x11
        byte[] result = new byte[totalCodewords];
        byte[] src = buffer.toByteArray();
        System.arraycopy(src, 0, result, 0, Math.min(src.length, totalCodewords));

        boolean toggle = true;
        for (int i = src.length; i < totalCodewords; i++) {
            result[i] = (byte) (toggle ? 0xEC : 0x11);
            toggle = !toggle;
        }

        return result;
    }

    private static byte[] generateAndInterleave(byte[] data, VersionSpec spec, ErrorCorrectionLevel ecLevel) {
        int ecPerBlock = (ecLevel == ErrorCorrectionLevel.L) ? spec.ecPerBlockL : spec.ecPerBlockM;
        int nBlocksG1 = (ecLevel == ErrorCorrectionLevel.L) ? spec.numBlocksG1L : spec.numBlocksG1M;
        int dataPerBlockG1 = (ecLevel == ErrorCorrectionLevel.L) ? spec.dataPerBlockG1L : spec.dataPerBlockG1M;
        int nBlocksG2 = (ecLevel == ErrorCorrectionLevel.L) ? spec.numBlocksG2L : spec.numBlocksG2M;
        int dataPerBlockG2 = (ecLevel == ErrorCorrectionLevel.L) ? spec.dataPerBlockG2L : spec.dataPerBlockG2M;

        int totalBlocks = nBlocksG1 + nBlocksG2;
        byte[][] dataBlocks = new byte[totalBlocks][];
        byte[][] ecBlocks = new byte[totalBlocks][];

        int offset = 0;
        int blockIdx = 0;

        for (int b = 0; b < nBlocksG1; b++) {
            dataBlocks[blockIdx] = new byte[dataPerBlockG1];
            System.arraycopy(data, offset, dataBlocks[blockIdx], 0, dataPerBlockG1);
            offset += dataPerBlockG1;
            ecBlocks[blockIdx] = computeReedSolomon(dataBlocks[blockIdx], ecPerBlock);
            blockIdx++;
        }

        for (int b = 0; b < nBlocksG2; b++) {
            dataBlocks[blockIdx] = new byte[dataPerBlockG2];
            System.arraycopy(data, offset, dataBlocks[blockIdx], 0, dataPerBlockG2);
            offset += dataPerBlockG2;
            ecBlocks[blockIdx] = computeReedSolomon(dataBlocks[blockIdx], ecPerBlock);
            blockIdx++;
        }

        // Interleave data codewords across all blocks
        int maxDataLen = Math.max(dataPerBlockG1, dataPerBlockG2);
        int totalCodewords = data.length + (totalBlocks * ecPerBlock);
        byte[] interleaved = new byte[totalCodewords];
        int outIdx = 0;

        for (int col = 0; col < maxDataLen; col++) {
            for (int b = 0; b < totalBlocks; b++) {
                if (col < dataBlocks[b].length) {
                    interleaved[outIdx++] = dataBlocks[b][col];
                }
            }
        }

        // Interleave EC codewords across all blocks
        for (int col = 0; col < ecPerBlock; col++) {
            for (int b = 0; b < totalBlocks; b++) {
                interleaved[outIdx++] = ecBlocks[b][col];
            }
        }

        return interleaved;
    }

    private static byte[] computeReedSolomon(byte[] data, int ecCount) {
        int[] gen = buildGeneratorPolynomial(ecCount);
        int[] remainder = new int[ecCount];

        for (byte b : data) {
            int factor = (b & 0xFF) ^ remainder[0];
            System.arraycopy(remainder, 1, remainder, 0, ecCount - 1);
            remainder[ecCount - 1] = 0;

            if (factor != 0) {
                for (int j = 0; j < ecCount; j++) {
                    remainder[j] ^= gfMultiply(gen[j + 1], factor);
                }
            }
        }

        byte[] ec = new byte[ecCount];
        for (int i = 0; i < ecCount; i++) {
            ec[i] = (byte) remainder[i];
        }
        return ec;
    }

    private static int[] buildGeneratorPolynomial(int degree) {
        int[] g = new int[degree + 1];
        g[0] = 1;

        for (int i = 0; i < degree; i++) {
            int alpha = EXP_TABLE[i];
            for (int j = i; j >= 0; j--) {
                g[j + 1] ^= gfMultiply(g[j], alpha);
            }
        }
        return g;
    }

    // --- MATRIX PATTERN SETUP (Finder, Alignment, Timing, Dark Module) ---

    private static void setupFinderPatterns(boolean[][] matrix, boolean[][] reserved, int dim) {
        applyFinder(matrix, reserved, 0, 0); // Top-Left
        applyFinder(matrix, reserved, dim - 7, 0); // Bottom-Left
        applyFinder(matrix, reserved, 0, dim - 7); // Top-Right
    }

    private static void applyFinder(boolean[][] matrix, boolean[][] reserved, int startR, int startC) {
        // Standard 7x7 Finder with 1-module Quiet Separator Border (8x8)
        for (int dr = -1; dr <= 7; dr++) {
            for (int dc = -1; dc <= 7; dc++) {
                int r = startR + dr;
                int c = startC + dc;
                if (r >= 0 && r < matrix.length && c >= 0 && c < matrix.length) {
                    reserved[r][c] = true;
                    if (dr >= 0 && dr < 7 && dc >= 0 && dc < 7) {
                        if (dr == 0 || dr == 6 || dc == 0 || dc == 6) {
                            matrix[r][c] = true; // outer dark ring
                        } else if (dr == 1 || dr == 5 || dc == 1 || dc == 5) {
                            matrix[r][c] = false; // inner light ring
                        } else {
                            matrix[r][c] = true; // 3x3 dark core
                        }
                    } else {
                        matrix[r][c] = false; // separator
                    }
                }
            }
        }
    }

    private static void setupAlignmentPatterns(boolean[][] matrix, boolean[][] reserved, int[] coords, int dim) {
        if (coords == null || coords.length == 0) return;

        for (int r : coords) {
            for (int c : coords) {
                // Skip if overlaps with any of the 3 finder patterns
                if ((r <= 8 && c <= 8) || (r <= 8 && c >= dim - 9) || (r >= dim - 9 && c <= 8)) {
                    continue;
                }
                applyAlignment(matrix, reserved, r, c);
            }
        }
    }

    private static void applyAlignment(boolean[][] matrix, boolean[][] reserved, int centerR, int centerC) {
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int r = centerR + dr;
                int c = centerC + dc;
                reserved[r][c] = true;
                if (Math.abs(dr) == 2 || Math.abs(dc) == 2) {
                    matrix[r][c] = true; // outer dark box
                } else if (Math.abs(dr) == 1 || Math.abs(dc) == 1) {
                    matrix[r][c] = false; // inner light box
                } else {
                    matrix[r][c] = true; // center dark point
                }
            }
        }
    }

    private static void setupTimingPatterns(boolean[][] matrix, boolean[][] reserved, int dim) {
        // Horizontal timing on row 6
        for (int c = 8; c < dim - 8; c++) {
            if (!reserved[6][c]) {
                matrix[6][c] = (c % 2 == 0);
                reserved[6][c] = true;
            }
        }
        // Vertical timing on column 6
        for (int r = 8; r < dim - 8; r++) {
            if (!reserved[r][6]) {
                matrix[r][6] = (r % 2 == 0);
                reserved[r][6] = true;
            }
        }
    }

    private static void setupDarkModule(boolean[][] matrix, boolean[][] reserved, int dim) {
        // Always dark module at (dim - 8, 8)
        matrix[dim - 8][8] = true;
        reserved[dim - 8][8] = true;
    }

    private static void reserveFormatInfo(boolean[][] reserved, int dim) {
        // Around Top-Left
        for (int c = 0; c <= 8; c++) reserved[8][c] = true;
        for (int r = 0; r <= 8; r++) reserved[r][8] = true;
        // Around Top-Right
        for (int c = dim - 8; c < dim; c++) reserved[8][c] = true;
        // Around Bottom-Left
        for (int r = dim - 8; r < dim; r++) reserved[r][8] = true;
    }

    private static void reserveVersionInfo(boolean[][] reserved, int dim) {
        // Bottom-Left (3 rows x 6 cols)
        for (int r = dim - 11; r < dim - 8; r++) {
            for (int c = 0; c < 6; c++) {
                reserved[r][c] = true;
            }
        }
        // Top-Right (6 rows x 3 cols)
        for (int r = 0; r < 6; r++) {
            for (int c = dim - 11; c < dim - 8; c++) {
                reserved[r][c] = true;
            }
        }
    }

    // --- ZIG-ZAG CODEWORD PLACEMENT ---

    private static void placeCodewordsInMatrix(boolean[][] matrix, boolean[][] reserved,
                                              byte[] codewords, int remainderBits, int dim) {
        BitBuffer buffer = new BitBuffer();
        for (byte b : codewords) {
            buffer.appendBits(b & 0xFF, 8);
        }
        if (remainderBits > 0) {
            buffer.appendBits(0, remainderBits);
        }

        int totalBits = buffer.getBitLength();
        int bitIdx = 0;

        int col = dim - 1;
        boolean goingUp = true;

        while (col > 0) {
            // Skip vertical timing line at column 6
            if (col == 6) {
                col--;
            }

            int right = col;
            int left = col - 1;

            int rowStart = goingUp ? dim - 1 : 0;
            int rowEnd = goingUp ? -1 : dim;
            int rowStep = goingUp ? -1 : 1;

            for (int r = rowStart; r != rowEnd; r += rowStep) {
                // Right module
                if (!reserved[r][right]) {
                    boolean bit = (bitIdx < totalBits) && buffer.getBit(bitIdx++);
                    matrix[r][right] = bit;
                }
                // Left module
                if (!reserved[r][left]) {
                    boolean bit = (bitIdx < totalBits) && buffer.getBit(bitIdx++);
                    matrix[r][left] = bit;
                }
            }

            goingUp = !goingUp;
            col -= 2;
        }
    }

    // --- MASK PATTERN EVALUATION (ISO/IEC 18004 §6.8.2) ---

    private static int findBestMaskPattern(boolean[][] matrix, boolean[][] reserved, int dim) {
        int bestMask = 0;
        int minPenalty = Integer.MAX_VALUE;

        for (int mask = 0; mask < 8; mask++) {
            boolean[][] candidate = new boolean[dim][dim];
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c < dim; c++) {
                    if (reserved[r][c]) {
                        candidate[r][c] = matrix[r][c];
                    } else {
                        candidate[r][c] = matrix[r][c] ^ isMaskBit(mask, r, c);
                    }
                }
            }
            int penalty = evaluatePenalty(candidate, dim);
            if (penalty < minPenalty) {
                minPenalty = penalty;
                bestMask = mask;
            }
        }
        return bestMask;
    }

    private static void applyMask(boolean[][] matrix, boolean[][] reserved, int mask, int dim) {
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim; c++) {
                if (!reserved[r][c]) {
                    matrix[r][c] ^= isMaskBit(mask, r, c);
                }
            }
        }
    }

    private static boolean isMaskBit(int mask, int row, int col) {
        switch (mask) {
            case 0: return (row + col) % 2 == 0;
            case 1: return row % 2 == 0;
            case 2: return col % 3 == 0;
            case 3: return (row + col) % 3 == 0;
            case 4: return ((row / 2) + (col / 3)) % 2 == 0;
            case 5: return ((row * col) % 2) + ((row * col) % 3) == 0;
            case 6: return (((row * col) % 2) + ((row * col) % 3)) % 2 == 0;
            case 7: return (((row + col) % 2) + ((row * col) % 3)) % 2 == 0;
            default: return false;
        }
    }

    private static int evaluatePenalty(boolean[][] m, int dim) {
        int penalty = 0;

        // Condition 1: Horizontal & Vertical runs of 5 or more
        for (int r = 0; r < dim; r++) {
            int run = 1;
            for (int c = 1; c < dim; c++) {
                if (m[r][c] == m[r][c - 1]) {
                    run++;
                } else {
                    if (run >= 5) penalty += 3 + (run - 5);
                    run = 1;
                }
            }
            if (run >= 5) penalty += 3 + (run - 5);
        }

        for (int c = 0; c < dim; c++) {
            int run = 1;
            for (int r = 1; r < dim; r++) {
                if (m[r][c] == m[r - 1][c]) {
                    run++;
                } else {
                    if (run >= 5) penalty += 3 + (run - 5);
                    run = 1;
                }
            }
            if (run >= 5) penalty += 3 + (run - 5);
        }

        // Condition 2: 2x2 blocks of same color
        for (int r = 0; r < dim - 1; r++) {
            for (int c = 0; c < dim - 1; c++) {
                boolean bit = m[r][c];
                if (m[r + 1][c] == bit && m[r][c + 1] == bit && m[r + 1][c + 1] == bit) {
                    penalty += 3;
                }
            }
        }

        // Condition 3: Finder-like 1:1:3:1:1 patterns
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim - 10; c++) {
                if (isFinderPattern11311(m[r][c], m[r][c + 1], m[r][c + 2], m[r][c + 3], m[r][c + 4],
                        m[r][c + 5], m[r][c + 6], m[r][c + 7], m[r][c + 8], m[r][c + 9], m[r][c + 10])) {
                    penalty += 40;
                }
            }
        }

        for (int c = 0; c < dim; c++) {
            for (int r = 0; r < dim - 10; r++) {
                if (isFinderPattern11311(m[r][c], m[r + 1][c], m[r + 2][c], m[r + 3][c], m[r + 4][c],
                        m[r + 5][c], m[r + 6][c], m[r + 7][c], m[r + 8][c], m[r + 9][c], m[r + 10][c])) {
                    penalty += 40;
                }
            }
        }

        // Condition 4: Dark to light ratio deviation from 50%
        int dark = 0;
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim; c++) {
                if (m[r][c]) dark++;
            }
        }
        int total = dim * dim;
        int percent = (dark * 100) / total;
        int rating = Math.abs(percent - 50) / 5;
        penalty += rating * 10;

        return penalty;
    }

    private static boolean isFinderPattern11311(boolean b0, boolean b1, boolean b2, boolean b3, boolean b4,
                                               boolean b5, boolean b6, boolean b7, boolean b8, boolean b9, boolean b10) {
        // 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0
        boolean p1 = b0 && !b1 && b2 && b3 && b4 && !b5 && b6 && !b7 && !b8 && !b9 && !b10;
        if (p1) return true;
        // 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1
        return !b0 && !b1 && !b2 && !b3 && b4 && !b5 && b6 && b7 && b8 && !b9 && b10;
    }

    // --- FORMAT & VERSION INFORMATION WRITING ---

    private static void writeFormatInfo(boolean[][] matrix, ErrorCorrectionLevel ecLevel, int mask, int dim) {
        int data = (ecLevel.bits << 3) | mask; // 5 bits
        int bch = data << 10;
        int gen = 0x537;

        for (int i = 4; i >= 0; i--) {
            if (((bch >> (i + 10)) & 1) == 1) {
                bch ^= (gen << i);
            }
        }

        int formatBits = ((data << 10) | bch) ^ 0x5412;

        // Copy 1 (Around Top-Left)
        // Bits 0..5 on row 8, cols 0..5
        for (int i = 0; i <= 5; i++) {
            matrix[8][i] = ((formatBits >> i) & 1) == 1;
        }
        // Bit 6 on row 8, col 7
        matrix[8][7] = ((formatBits >> 6) & 1) == 1;
        // Bit 7 on row 8, col 8
        matrix[8][8] = ((formatBits >> 7) & 1) == 1;
        // Bit 8 on row 7, col 8
        matrix[7][8] = ((formatBits >> 8) & 1) == 1;
        // Bits 9..14 on rows 5..0, col 8
        for (int i = 9; i <= 14; i++) {
            matrix[14 - i][8] = ((formatBits >> i) & 1) == 1;
        }

        // Copy 2 (Bottom-Left and Top-Right)
        // Bits 0..6 on rows dim-1..dim-7, col 8
        for (int i = 0; i <= 6; i++) {
            matrix[dim - 1 - i][8] = ((formatBits >> i) & 1) == 1;
        }
        // Bits 7..14 on row 8, cols dim-8..dim-1
        for (int i = 7; i <= 14; i++) {
            matrix[8][dim - 8 + (i - 7)] = ((formatBits >> i) & 1) == 1;
        }
    }

    private static void writeVersionInfo(boolean[][] matrix, int version, int dim) {
        int bch = version << 12;
        int gen = 0x1F25;

        for (int i = 5; i >= 0; i--) {
            if (((bch >> (i + 12)) & 1) == 1) {
                bch ^= (gen << i);
            }
        }

        int versionInfo = (version << 12) | bch;

        for (int i = 0; i < 18; i++) {
            boolean bit = ((versionInfo >> i) & 1) == 1;
            // Bottom-Left
            int blRow = dim - 11 + (i % 3);
            int blCol = i / 3;
            matrix[blRow][blCol] = bit;

            // Top-Right
            int trRow = i / 3;
            int trCol = dim - 11 + (i % 3);
            matrix[trRow][trCol] = bit;
        }
    }

    // --- HELPER BIT BUFFER ---
    private static class BitBuffer {
        private final List<Boolean> bits = new ArrayList<>();

        void appendBits(int value, int numBits) {
            for (int i = numBits - 1; i >= 0; i--) {
                bits.add(((value >> i) & 1) == 1);
            }
        }

        int getBitLength() {
            return bits.size();
        }

        boolean getBit(int index) {
            return bits.get(index);
        }

        byte[] toByteArray() {
            int numBytes = (bits.size() + 7) / 8;
            byte[] bytes = new byte[numBytes];
            for (int i = 0; i < bits.size(); i++) {
                if (bits.get(i)) {
                    bytes[i / 8] |= (1 << (7 - (i % 8)));
                }
            }
            return bytes;
        }
    }
}
