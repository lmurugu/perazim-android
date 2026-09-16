package com.example.app.community.qr;

import androidx.annotation.NonNull;
import com.example.app.community.qr.qrcodegen.QrCode;
import com.example.app.community.qr.qrcodegen.QrSegment;

import java.util.List;

/**
 * Standards-compliant implementation of the ISO/IEC 18004 QR Code matrix generator.
 * Produces genuine, standards-compliant 2D QR matrices readable by any smartphone camera
 * (Google Lens, iPhone Camera, ZXing, Barcode Scanners).
 */
public final class QrMatrixEncoder {

    public enum ErrorCorrectionLevel {
        L, M, Q, H
    }

    private QrMatrixEncoder() {}

    @NonNull
    public static boolean[][] encode(@NonNull String text) {
        return encode(text, 1, ErrorCorrectionLevel.M);
    }

    @NonNull
    public static boolean[][] encode(@NonNull String text, int minVersion) {
        return encode(text, minVersion, ErrorCorrectionLevel.M);
    }

    @NonNull
    public static boolean[][] encode(@NonNull String text, int minVersion, @NonNull ErrorCorrectionLevel ecLevel) {
        QrCode.Ecc ecc;
        switch (ecLevel) {
            case L: ecc = QrCode.Ecc.LOW; break;
            case M: ecc = QrCode.Ecc.MEDIUM; break;
            case Q: ecc = QrCode.Ecc.QUARTILE; break;
            case H: ecc = QrCode.Ecc.HIGH; break;
            default: ecc = QrCode.Ecc.MEDIUM; break;
        }

        List<QrSegment> segments = QrSegment.makeSegments(text != null ? text : "");
        QrCode qr = QrCode.encodeSegments(segments, ecc, Math.max(1, minVersion), 40, -1, true);

        int size = qr.size;
        boolean[][] matrix = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                matrix[y][x] = qr.getModule(x, y);
            }
        }
        return matrix;
    }
}
