package org.perazimchurch.app.ai.local;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Arrays;

/** Verification fallback for Android releases whose JCA provider lacks Ed25519. */
final class Ed25519Verifier {
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger P = TWO.pow(255).subtract(BigInteger.valueOf(19));
    private static final BigInteger L = TWO.pow(252).add(new BigInteger("27742317777372353535851937790883648493"));
    private static final BigInteger D = P.subtract(BigInteger.valueOf(121665))
            .multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P);
    private static final BigInteger SQRT_M1 = TWO.modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P);
    private static final Point IDENTITY = new Point(BigInteger.ZERO, BigInteger.ONE);
    private static final Point BASE = decodeBase();

    private Ed25519Verifier() {}

    static boolean verify(byte[] publicBytes, byte[] message, byte[] signature) throws Exception {
        if (publicBytes.length != 32 || signature.length != 64) return false;
        byte[] rBytes = Arrays.copyOfRange(signature, 0, 32);
        byte[] sBytes = Arrays.copyOfRange(signature, 32, 64);
        BigInteger s = littleInteger(sBytes);
        if (s.compareTo(L) >= 0) return false;
        Point a = decode(publicBytes);
        Point r = decode(rBytes);
        if (a == null || r == null || !multiply(a, L).equals(IDENTITY) || !multiply(r, L).equals(IDENTITY)) return false;
        byte[] hashInput = new byte[64 + message.length];
        System.arraycopy(rBytes, 0, hashInput, 0, 32);
        System.arraycopy(publicBytes, 0, hashInput, 32, 32);
        System.arraycopy(message, 0, hashInput, 64, message.length);
        BigInteger h = littleInteger(MessageDigest.getInstance("SHA-512").digest(hashInput)).mod(L);
        byte[] lhs = encode(multiply(BASE, s));
        byte[] rhs = encode(add(r, multiply(a, h)));
        return MessageDigest.isEqual(lhs, rhs);
    }

    private static Point decodeBase() {
        byte[] encoded = new byte[32];
        encoded[0] = 0x58;
        Arrays.fill(encoded, 1, 32, (byte) 0x66);
        return decode(encoded);
    }

    private static Point decode(byte[] encoded) {
        byte[] yBytes = encoded.clone();
        int sign = (yBytes[31] >>> 7) & 1;
        yBytes[31] &= 0x7f;
        BigInteger y = littleInteger(yBytes);
        if (y.compareTo(P) >= 0) return null;
        BigInteger y2 = y.multiply(y).mod(P);
        BigInteger u = y2.subtract(BigInteger.ONE).mod(P);
        BigInteger v = D.multiply(y2).add(BigInteger.ONE).mod(P);
        BigInteger x2 = u.multiply(v.modInverse(P)).mod(P);
        BigInteger x = x2.modPow(P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), P);
        if (!x.multiply(x).subtract(x2).mod(P).equals(BigInteger.ZERO)) x = x.multiply(SQRT_M1).mod(P);
        if (!x.multiply(x).subtract(x2).mod(P).equals(BigInteger.ZERO)) return null;
        if (x.signum() == 0 && sign == 1) return null;
        if (x.testBit(0) != (sign == 1)) x = P.subtract(x);
        return new Point(x, y);
    }

    private static Point add(Point a, Point b) {
        BigInteger product = D.multiply(a.x).multiply(b.x).multiply(a.y).multiply(b.y).mod(P);
        BigInteger x = a.x.multiply(b.y).add(a.y.multiply(b.x)).mod(P)
                .multiply(BigInteger.ONE.add(product).mod(P).modInverse(P)).mod(P);
        BigInteger y = a.y.multiply(b.y).add(a.x.multiply(b.x)).mod(P)
                .multiply(BigInteger.ONE.subtract(product).mod(P).modInverse(P)).mod(P);
        return new Point(x, y);
    }

    private static Point multiply(Point point, BigInteger scalar) {
        Point result = IDENTITY;
        Point addend = point;
        for (int bit = 0; bit < scalar.bitLength(); bit++) {
            if (scalar.testBit(bit)) result = add(result, addend);
            addend = add(addend, addend);
        }
        return result;
    }

    private static byte[] encode(Point point) {
        byte[] y = littleBytes(point.y, 32);
        if (point.x.testBit(0)) y[31] = (byte) (y[31] | 0x80);
        return y;
    }

    private static BigInteger littleInteger(byte[] bytes) {
        byte[] reversed = bytes.clone();
        for (int i = 0; i < reversed.length / 2; i++) {
            byte swap = reversed[i]; reversed[i] = reversed[reversed.length - 1 - i]; reversed[reversed.length - 1 - i] = swap;
        }
        return new BigInteger(1, reversed);
    }

    private static byte[] littleBytes(BigInteger value, int size) {
        byte[] big = value.toByteArray();
        byte[] result = new byte[size];
        for (int i = 0; i < size && i < big.length; i++) result[i] = big[big.length - 1 - i];
        return result;
    }

    private static final class Point {
        final BigInteger x, y;
        Point(BigInteger x, BigInteger y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object other) {
            return other instanceof Point && x.equals(((Point) other).x) && y.equals(((Point) other).y);
        }
        @Override public int hashCode() { return 31 * x.hashCode() + y.hashCode(); }
    }
}
