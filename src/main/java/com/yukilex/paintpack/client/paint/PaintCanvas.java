package com.yukilex.paintpack.client.paint;

import net.minecraft.client.texture.NativeImage;

/**
 * Bir esyanin texture'ini piksel duzeyinde tutan ve duzenleyen sinif.
 * Renkler bu sinif icinde standart ARGB (0xAARRGGBB) formatinda saklanir.
 * Minecraft'in NativeImage sinifi renkleri ABGR olarak paketledigi icin
 * NativeImage ile veri alisverisinde donusum yapilir (bkz. {@link #toNativeColor}
 * ve {@link #fromNativeColor}).
 */
public final class PaintCanvas {

    private final int width;
    private final int height;
    /** Duzenlenebilir, guncel piksel verisi (ARGB). */
    private final int[] pixels;
    /** Orijinal (ilk yuklenen) piksel verisi; silgi bu veriye geri doner. */
    private final int[] originalPixels;

    private final PaintHistory history = new PaintHistory();
    private boolean dirty = false;

    public PaintCanvas(NativeImage source) {
        this.width = source.getWidth();
        this.height = source.getHeight();
        this.pixels = new int[width * height];
        this.originalPixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = fromNativeColor(source.getColor(x, y));
                pixels[index(x, y)] = color;
                originalPixels[index(x, y)] = color;
            }
        }
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public int getPixel(int x, int y) {
        if (!inBounds(x, y)) {
            return 0;
        }
        return pixels[index(x, y)];
    }

    /** Yeni bir firca darbesi baslamadan once cagrilmalidir (Undo icin anlik goruntu alir). */
    public void beginStroke() {
        history.pushUndoState(pixels);
    }

    /** Belirtilen merkez etrafinda, verilen yaricapta (kare firca) boyama yapar. */
    public void paint(int centerX, int centerY, int brushSize, int argbColor) {
        applyBrush(centerX, centerY, brushSize, argbColor);
        dirty = true;
    }

    /** Belirtilen merkez etrafinda silgi uygular; piksel(ler)i orijinal haline dondurur. */
    public void erase(int centerX, int centerY, int brushSize) {
        int half = brushSize / 2;
        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (inBounds(x, y)) {
                    pixels[index(x, y)] = originalPixels[index(x, y)];
                }
            }
        }
        dirty = true;
    }

    private void applyBrush(int centerX, int centerY, int brushSize, int argbColor) {
        int half = brushSize / 2;
        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (inBounds(x, y)) {
                    pixels[index(x, y)] = argbColor;
                }
            }
        }
    }

    /** Orta tik ile renk alma (eyedropper). Belirtilen pikselin rengini dondurur. */
    public int pickColor(int x, int y) {
        return getPixel(x, y);
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    public void undo() {
        int[] restored = history.undo(pixels);
        if (restored != null) {
            System.arraycopy(restored, 0, pixels, 0, pixels.length);
            dirty = true;
        }
    }

    public void redo() {
        int[] restored = history.redo(pixels);
        if (restored != null) {
            System.arraycopy(restored, 0, pixels, 0, pixels.length);
            dirty = true;
        }
    }

    /** Guncel piksel verisini yeni bir NativeImage'a yazar (kaydetme/uygulama icin). */
    public NativeImage toNativeImage() {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setColor(x, y, toNativeColor(pixels[index(x, y)]));
            }
        }
        return image;
    }

    public void markSaved() {
        this.dirty = false;
    }

    /** ARGB -> Minecraft NativeImage ABGR donusumu (R ve B kanallari yer degistirir). */
    private static int toNativeColor(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** Minecraft NativeImage ABGR -> ARGB donusumu. */
    private static int fromNativeColor(int abgr) {
        int a = (abgr >>> 24) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
