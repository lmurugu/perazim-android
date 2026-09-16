package com.example.app.community.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.Decoder;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class QrInteroperabilityTest {

    @Test
    public void testShortPayloadDecode() throws Exception {
        String payload = "HELLO PERAZIM";
        boolean[][] matrix = QrMatrixEncoder.encode(payload);
        Assert.assertNotNull(matrix);

        String decoded = decodeWithZxing(matrix);
        System.out.println("Expected: " + payload + " | Decoded: " + decoded);
        Assert.assertEquals(payload, decoded);
    }

    @Test
    public void testPerazimConnectionPayloadDecode() throws Exception {
        String payload = "perazim://connect?uid=user_12345&name=Pastor+Samuel&campus=Embu+Headquarters&ts=1773660000000";
        boolean[][] matrix = QrMatrixEncoder.encode(payload);
        Assert.assertNotNull(matrix);

        String decoded = decodeWithZxing(matrix);
        System.out.println("Expected: " + payload + " | Decoded: " + decoded);
        Assert.assertEquals(payload, decoded);
    }

    private String decodeWithZxing(boolean[][] matrix) throws Exception {
        int height = matrix.length;
        int width = matrix[0].length;

        // 1. Direct BitMatrix decode via ZXing Decoder
        BitMatrix bitMatrix = new BitMatrix(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x]) {
                    bitMatrix.set(x, y);
                }
            }
        }

        Decoder decoder = new Decoder();
        DecoderResult decoderResult = decoder.decode(bitMatrix);
        Assert.assertNotNull(decoderResult);
        Assert.assertNotNull(decoderResult.getText());

        // 2. Optical decode via ZXing QRCodeReader with scaled image (scale=8, quietZone=4)
        int scale = 8;
        int quietZone = 4;
        int fullModulesW = width + (quietZone * 2);
        int fullModulesH = height + (quietZone * 2);
        int pixelWidth = fullModulesW * scale;
        int pixelHeight = fullModulesH * scale;

        byte[] luminances = new byte[pixelWidth * pixelHeight];
        Arrays.fill(luminances, (byte) 0xFF); // White background

        for (int my = 0; my < height; my++) {
            for (int mx = 0; mx < width; mx++) {
                if (matrix[my][mx]) {
                    int startPy = (my + quietZone) * scale;
                    int startPx = (mx + quietZone) * scale;
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            int idx = ((startPy + dy) * pixelWidth) + (startPx + dx);
                            luminances[idx] = 0x00; // Black module
                        }
                    }
                }
            }
        }

        LuminanceSource source = new TestLuminanceSource(pixelWidth, pixelHeight, luminances);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);
        Assert.assertNotNull(result);

        return result.getText();
    }

    private static class TestLuminanceSource extends LuminanceSource {
        private final byte[] luminances;

        protected TestLuminanceSource(int width, int height, byte[] luminances) {
            super(width, height);
            this.luminances = luminances;
        }

        @Override
        public byte[] getRow(int y, byte[] row) {
            if (row == null || row.length < getWidth()) {
                row = new byte[getWidth()];
            }
            System.arraycopy(luminances, y * getWidth(), row, 0, getWidth());
            return row;
        }

        @Override
        public byte[] getMatrix() {
            return luminances;
        }
    }
}
