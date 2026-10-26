package vn.camerahub.marketplace.util;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Canonical pixels catch format changes; dHash + colour checks catch modest resizing/recompression.
 */
public record ImageFingerprint(String sha256, long dhash, String signature) {
    public static ImageFingerprint of(BufferedImage image) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (int value : new int[]{image.getWidth(), image.getHeight()})
                for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
            for (int y = 0; y < image.getHeight(); y++)
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (rgb >>> shift));
                }
            BufferedImage small = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
            var graphics = small.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, 9, 8, null);
            graphics.dispose();
            long hash = 0;
            byte[] colours = new byte[192];
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) {
                    hash = (hash << 1) | (gray(small.getRGB(x, y)) > gray(small.getRGB(x + 1, y)) ? 1 : 0);
                    int rgb = small.getRGB(x, y), offset = (y * 8 + x) * 3;
                    colours[offset] = (byte) (rgb >>> 16);
                    colours[offset + 1] = (byte) (rgb >>> 8);
                    colours[offset + 2] = (byte) rgb;
                }
            return new ImageFingerprint(HexFormat.of().formatHex(digest.digest()), hash, Base64.getEncoder().encodeToString(colours));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot fingerprint image", e);
        }
    }

    private static int gray(int rgb) {
        return ((rgb >>> 16 & 255) * 299 + (rgb >>> 8 & 255) * 587 + (rgb & 255) * 114) / 1000;
    }

    public boolean similar(ImageFingerprint other) {
        if (sha256.equals(other.sha256)) return true;
        if (Long.bitCount(dhash ^ other.dhash) > 4) return false;
        byte[] a = Base64.getDecoder().decode(signature), b = Base64.getDecoder().decode(other.signature);
        double sum = 0, squares = 0, error = 0;
        for (int i = 0; i < a.length; i++) {
            int value = Byte.toUnsignedInt(a[i]);
            error += Math.pow(value - Byte.toUnsignedInt(b[i]), 2);
        }
        for (int i = 0; i < a.length; i += 3) {
            double value = (Byte.toUnsignedInt(a[i]) * 299 + Byte.toUnsignedInt(a[i + 1]) * 587 + Byte.toUnsignedInt(a[i + 2]) * 114) / 1000.0;
            sum += value;
            squares += value * value;
        }
        // Featureless backgrounds are insufficient evidence of image reuse.
        return squares / 64 - Math.pow(sum / 64, 2) > 225 && error / a.length < 100;
    }
}

