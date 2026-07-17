package com.yukilex.paintpack.client.util;

/**
 * RGB, HSV ve HEX renk formatlari arasinda donusum saglayan yardimci sinif.
 * Tum renkler ARGB int formatinda tutulur (0xAARRGGBB).
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    /** 0-255 araligindaki r,g,b degerlerinden opak (alpha=255) bir ARGB int uretir. */
    public static int rgb(int r, int g, int b) {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int getAlpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int getRed(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    public static int getGreen(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** ARGB rengi HEX metnine cevirir. Ornek: #FF00A2 */
    public static String toHex(int argb) {
        return String.format("#%02X%02X%02X", getRed(argb), getGreen(argb), getBlue(argb));
    }

    /**
     * HEX metnini ARGB rengine cevirir. "#" isaretsiz ve isaretli girisleri kabul eder.
     * Gecersiz girislerde null doner.
     */
    public static Integer fromHex(String hex) {
        if (hex == null) {
            return null;
        }
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.length() != 6) {
            return null;
        }
        try {
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return rgb(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** ARGB rengi float[]{hue(0-360), sat(0-1), value(0-1)} dizisine cevirir. */
    public static float[] toHsv(int argb) {
        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB(getRed(argb), getGreen(argb), getBlue(argb), hsv);
        hsv[0] *= 360f;
        return hsv;
    }

    /** HSV degerlerinden (hue 0-360, sat 0-1, value 0-1) opak ARGB renk uretir. */
    public static int fromHsv(float hue, float saturation, float value) {
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
