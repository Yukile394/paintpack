package com.yukilex.paintpack.client.paint;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Piksel dizilerinin (int[] ARGB) anlik goruntulerini (snapshot) tutarak
 * Geri Al / Ileri Al islevini saglar. Bellek kullanimini sinirlamak icin
 * gecmis derinligi MAX_HISTORY ile sinirlandirilmistir.
 */
public final class PaintHistory {

    private static final int MAX_HISTORY = 60;

    private final Deque<int[]> undoStack = new ArrayDeque<>();
    private final Deque<int[]> redoStack = new ArrayDeque<>();

    /** Bir firca darbesinden (stroke) once cagrilir; mevcut durumu gecmise kaydeder. */
    public void pushUndoState(int[] currentPixels) {
        undoStack.push(currentPixels.clone());
        redoStack.clear();
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Bir onceki duruma doner. Simdiki piksel dizisi redo yigini icin saklanir.
     * Geri alinan piksel dizisini dondurur, gecmis bossa null doner.
     */
    public int[] undo(int[] currentPixels) {
        if (undoStack.isEmpty()) {
            return null;
        }
        redoStack.push(currentPixels.clone());
        return undoStack.pop();
    }

    /**
     * Ileri alinan (redo) duruma gecer. Simdiki piksel dizisini undo yigina geri koyar.
     */
    public int[] redo(int[] currentPixels) {
        if (redoStack.isEmpty()) {
            return null;
        }
        undoStack.push(currentPixels.clone());
        return redoStack.pop();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
