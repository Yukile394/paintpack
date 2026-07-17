package com.yukilex.paintpack.client.gui;

import com.yukilex.paintpack.client.paint.PaintCanvas;
import com.yukilex.paintpack.client.texture.PaintedTextureManager;
import com.yukilex.paintpack.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Elde tutulan esyanin texture'ini duzenlemeye yarayan ana editor ekrani.
 *
 * Sol taraf: buyutulmus piksel tuvali (canli onizleme burasidir; her firca
 * darbesi aninda gorunur). Sag taraf: renk secici (HSV kutusu + ton
 * seridi), HEX girisi, son kullanilan renkler, firca boyutu / yakinlastirma
 * bilgisi ve arac dugmeleri.
 *
 * Fare kontrolleri:
 *  - Sol tik: Firca ile boya
 *  - Sag tik: Silgi
 *  - Orta tik: Renk alma (eyedropper)
 *  - Tekerlek: Firca boyutunu degistir
 *
 * Kisayollar: CTRL+Z (Geri Al), CTRL+Y (Ileri Al)
 */
public final class PaintEditorScreen extends Screen {

    private enum Tool { BRUSH, ERASER }

    private static final int MIN_BRUSH = 1;
    private static final int MAX_BRUSH = 8;
    private static final int MIN_ZOOM = 6;
    private static final int MAX_ZOOM = 32;
    private static final int MAX_RECENT_COLORS = 8;

    private final ItemStack stack;
    private PaintCanvas canvas;

    private Tool currentTool = Tool.BRUSH;
    private int brushSize = 1;
    private int zoom = 16;
    private boolean showGrid = true;

    private int currentColor = 0xFF000000;
    private float hue = 0f;
    private float saturation = 0f;
    private float value = 0f;

    private final Deque<Integer> recentColors = new ArrayDeque<>();

    private int canvasOriginX;
    private int canvasOriginY;

    private TextFieldWidget hexField;
    private ButtonWidget brushButton;
    private ButtonWidget eraserButton;
    private ButtonWidget undoButton;
    private ButtonWidget redoButton;
    private ButtonWidget saveButton;

    private boolean strokeActive = false;
    private String statusMessage = "";
    private int statusTicks = 0;

    public PaintEditorScreen(ItemStack stack) {
        super(Text.translatable("paintpack.editor.title"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        this.canvas = PaintedTextureManager.getInstance().loadCanvasFor(stack);

        canvasOriginX = 24;
        canvasOriginY = 40;

        int panelX = this.width - 190;
        int y = 40;

        this.hexField = new TextFieldWidget(this.textRenderer, panelX, y, 100, 18, Text.translatable("paintpack.editor.hex"));
        this.hexField.setMaxLength(7);
        this.hexField.setText(ColorUtil.toHex(currentColor));
        this.hexField.setChangedListener(this::onHexChanged);
        this.addDrawableChild(this.hexField);

        y += 120;

        this.brushButton = ButtonWidget.builder(Text.translatable("paintpack.editor.brush"), b -> selectTool(Tool.BRUSH))
                .dimensions(panelX, y, 85, 20).build();
        this.addDrawableChild(this.brushButton);

        this.eraserButton = ButtonWidget.builder(Text.translatable("paintpack.editor.eraser"), b -> selectTool(Tool.ERASER))
                .dimensions(panelX + 90, y, 85, 20).build();
        this.addDrawableChild(this.eraserButton);

        y += 26;

        this.undoButton = ButtonWidget.builder(Text.translatable("paintpack.editor.undo"), b -> doUndo())
                .dimensions(panelX, y, 85, 20).build();
        this.addDrawableChild(this.undoButton);

        this.redoButton = ButtonWidget.builder(Text.translatable("paintpack.editor.redo"), b -> doRedo())
                .dimensions(panelX + 90, y, 85, 20).build();
        this.addDrawableChild(this.redoButton);

        y += 26;

        this.saveButton = ButtonWidget.builder(Text.translatable("paintpack.editor.save"), b -> doSave())
                .dimensions(panelX, y, 175, 20).build();
        this.addDrawableChild(this.saveButton);

        updateHsvFromColor();
    }

    // ----------------------------------------------------------------------------------
    // Yardimci islemler
    // ----------------------------------------------------------------------------------

    private void selectTool(Tool tool) {
        this.currentTool = tool;
    }

    private void doUndo() {
        if (canvas.canUndo()) {
            canvas.undo();
        }
    }

    private void doRedo() {
        if (canvas.canRedo()) {
            canvas.redo();
        }
    }

    private void doSave() {
        PaintedTextureManager.getInstance().saveAndApply(stack, canvas);
        statusMessage = Text.translatable("paintpack.editor.saved").getString();
        statusTicks = 60;
    }

    private void addRecentColor(int color) {
        recentColors.remove(color);
        recentColors.addFirst(color);
        while (recentColors.size() > MAX_RECENT_COLORS) {
            recentColors.removeLast();
        }
    }

    private void setCurrentColor(int argb) {
        this.currentColor = argb;
        this.hexField.setText(ColorUtil.toHex(argb));
    }

    private void updateHsvFromColor() {
        float[] hsv = ColorUtil.toHsv(currentColor);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    private void onHexChanged(String text) {
        Integer parsed = ColorUtil.fromHex(text);
        if (parsed != null) {
            this.currentColor = parsed;
            updateHsvFromColor();
        }
    }

    // ----------------------------------------------------------------------------------
    // Tuval (canvas) alan hesaplari
    // ----------------------------------------------------------------------------------

    private int pixelToScreenX(int px) {
        return canvasOriginX + px * zoom;
    }

    private int pixelToScreenY(int py) {
        return canvasOriginY + py * zoom;
    }

    private int screenToPixelX(double sx) {
        return (int) Math.floor((sx - canvasOriginX) / zoom);
    }

    private int screenToPixelY(double sy) {
        return (int) Math.floor((sy - canvasOriginY) / zoom);
    }

    private boolean isOverCanvas(double mouseX, double mouseY) {
        int px = screenToPixelX(mouseX);
        int py = screenToPixelY(mouseY);
        return canvas.inBounds(px, py);
    }

    // ----------------------------------------------------------------------------------
    // HSV renk kutusu / ton seridi alanlari (sag panel ustunde)
    // ----------------------------------------------------------------------------------

    private int svBoxX() {
        return this.width - 190;
    }

    private int svBoxY() {
        return 62;
    }

    private static final int SV_BOX_SIZE = 100;
    private static final int HUE_STRIP_WIDTH = 16;

    private int hueStripX() {
        return svBoxX() + SV_BOX_SIZE + 8;
    }

    private boolean isOverSvBox(double mx, double my) {
        return mx >= svBoxX() && mx < svBoxX() + SV_BOX_SIZE && my >= svBoxY() && my < svBoxY() + SV_BOX_SIZE;
    }

    private boolean isOverHueStrip(double mx, double my) {
        return mx >= hueStripX() && mx < hueStripX() + HUE_STRIP_WIDTH && my >= svBoxY() && my < svBoxY() + SV_BOX_SIZE;
    }

    private void updateFromSvBox(double mx, double my) {
        double sx = Math.max(0, Math.min(SV_BOX_SIZE - 1, mx - svBoxX()));
        double sy = Math.max(0, Math.min(SV_BOX_SIZE - 1, my - svBoxY()));
        this.saturation = (float) (sx / (SV_BOX_SIZE - 1));
        this.value = (float) (1.0 - sy / (SV_BOX_SIZE - 1));
        setCurrentColor(ColorUtil.fromHsv(hue, saturation, value));
    }

    private void updateFromHueStrip(double my) {
        double sy = Math.max(0, Math.min(SV_BOX_SIZE - 1, my - svBoxY()));
        this.hue = (float) (sy / (SV_BOX_SIZE - 1) * 360.0);
        setCurrentColor(ColorUtil.fromHsv(hue, saturation, value));
    }

    // ----------------------------------------------------------------------------------
    // Son kullanilan renkler satiri
    // ----------------------------------------------------------------------------------

    private int recentColorsY() {
        return svBoxY() + SV_BOX_SIZE + 14;
    }

    private int recentSwatchAt(double mx, double my) {
        int rowY = recentColorsY();
        int size = 16;
        int startX = svBoxX();
        if (my < rowY || my >= rowY + size) {
            return -1;
        }
        int index = 0;
        for (Integer ignored : recentColors) {
            int sx = startX + index * (size + 2);
            if (mx >= sx && mx < sx + size) {
                return index;
            }
            index++;
        }
        return -1;
    }

    // ----------------------------------------------------------------------------------
    // Fare olaylari
    // ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverSvBox(mouseX, mouseY)) {
            updateFromSvBox(mouseX, mouseY);
            return true;
        }
        if (isOverHueStrip(mouseX, mouseY)) {
            updateFromHueStrip(mouseY);
            return true;
        }
        int recentIndex = recentSwatchAt(mouseX, mouseY);
        if (recentIndex >= 0) {
            setCurrentColor((Integer) recentColors.toArray()[recentIndex]);
            updateHsvFromColor();
            return true;
        }

        if (isOverCanvas(mouseX, mouseY)) {
            int px = screenToPixelX(mouseX);
            int py = screenToPixelY(mouseY);
            strokeActive = true;
            canvas.beginStroke();
            applyToolAt(px, py, button);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isOverSvBox(mouseX, mouseY)) {
            updateFromSvBox(mouseX, mouseY);
            return true;
        }
        if (isOverHueStrip(mouseX, mouseY)) {
            updateFromHueStrip(mouseY);
            return true;
        }
        if (strokeActive && isOverCanvas(mouseX, mouseY)) {
            int px = screenToPixelX(mouseX);
            int py = screenToPixelY(mouseY);
            applyToolAt(px, py, button);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        strokeActive = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverCanvas(mouseX, mouseY)) {
            int delta = verticalAmount > 0 ? 1 : -1;
            brushSize = Math.max(MIN_BRUSH, Math.min(MAX_BRUSH, brushSize + delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void applyToolAt(int px, int py, int button) {
        if (button == 1) {
            // Sag tik: her zaman silgi
            canvas.erase(px, py, brushSize);
        } else if (button == 2) {
            // Orta tik: her zaman renk alma (eyedropper)
            int picked = canvas.pickColor(px, py);
            if (ColorUtil.getAlpha(picked) > 0) {
                setCurrentColor(picked);
                updateHsvFromColor();
                addRecentColor(picked);
            }
        } else if (button == 0) {
            // Sol tik: secili arac (Firca veya Silgi) uygulanir
            if (currentTool == Tool.ERASER) {
                canvas.erase(px, py, brushSize);
            } else {
                canvas.paint(px, py, brushSize, currentColor);
                addRecentColor(currentColor);
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // Klavye kisayollari
    // ----------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrl = Screen.hasControlDown();
        if (ctrl && keyCode == 90) { // Z
            doUndo();
            return true;
        }
        if (ctrl && keyCode == 89) { // Y
            doRedo();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ----------------------------------------------------------------------------------
    // Cizim
    // ----------------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        context.drawTextWithShadow(this.textRenderer, this.title, canvasOriginX, 16, 0xFFFFFF);

        drawCanvas(context);
        drawCrosshair(context, mouseX, mouseY);
        drawColorPicker(context);
        drawRecentColors(context);
        drawInfoPanel(context);

        super.render(context, mouseX, mouseY, delta);

        if (statusTicks > 0) {
            context.drawTextWithShadow(this.textRenderer, statusMessage, canvasOriginX, this.height - 20, 0x55FF55);
            statusTicks--;
        }
    }

    private void drawCanvas(DrawContext context) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        // Saydamligi belirginlestirmek icin dama tahtasi arka plan.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = pixelToScreenX(x);
                int sy = pixelToScreenY(y);
                boolean checker = ((x + y) & 1) == 0;
                int bg = checker ? 0xFF9E9E9E : 0xFF7A7A7A;
                context.fill(sx, sy, sx + zoom, sy + zoom, bg);

                int pixel = canvas.getPixel(x, y);
                if (ColorUtil.getAlpha(pixel) > 0) {
                    context.fill(sx, sy, sx + zoom, sy + zoom, pixel);
                }

                if (showGrid && zoom >= 8) {
                    context.fill(sx, sy, sx + zoom, sy + 1, 0x33000000);
                    context.fill(sx, sy, sx + 1, sy + zoom, 0x33000000);
                }
            }
        }

        int borderColor = 0xFFFFFFFF;
        int x0 = pixelToScreenX(0);
        int y0 = pixelToScreenY(0);
        int x1 = pixelToScreenX(w);
        int y1 = pixelToScreenY(h);
        context.fill(x0, y0, x1, y0 + 1, borderColor);
        context.fill(x0, y1, x1, y1 + 1, borderColor);
        context.fill(x0, y0, x0 + 1, y1, borderColor);
        context.fill(x1, y0, x1 + 1, y1, borderColor);
    }

    private void drawCrosshair(DrawContext context, int mouseX, int mouseY) {
        if (!isOverCanvas(mouseX, mouseY)) {
            return;
        }
        int px = screenToPixelX(mouseX);
        int py = screenToPixelY(mouseY);
        int half = brushSize / 2;

        int sx = pixelToScreenX(px - half);
        int sy = pixelToScreenY(py - half);
        int size = Math.max(1, brushSize) * zoom;

        context.fill(sx, sy, sx + size, sy + 1, 0xFFFFFFFF);
        context.fill(sx, sy + size - 1, sx + size, sy + size, 0xFFFFFFFF);
        context.fill(sx, sy, sx + 1, sy + size, 0xFFFFFFFF);
        context.fill(sx + size - 1, sy, sx + size, sy + size, 0xFFFFFFFF);
    }

    private void drawColorPicker(DrawContext context) {
        int boxX = svBoxX();
        int boxY = svBoxY();

        // SV kutusu: yatay eksen doygunluk (saturation), dikey eksen parlaklik (value)
        for (int y = 0; y < SV_BOX_SIZE; y += 2) {
            for (int x = 0; x < SV_BOX_SIZE; x += 2) {
                float s = x / (float) (SV_BOX_SIZE - 1);
                float v = 1.0f - y / (float) (SV_BOX_SIZE - 1);
                int color = ColorUtil.fromHsv(hue, s, v);
                context.fill(boxX + x, boxY + y, boxX + x + 2, boxY + y + 2, color);
            }
        }

        // Ton (hue) seridi
        int hueX = hueStripX();
        for (int y = 0; y < SV_BOX_SIZE; y++) {
            float h = y / (float) (SV_BOX_SIZE - 1) * 360f;
            int color = ColorUtil.fromHsv(h, 1.0f, 1.0f);
            context.fill(hueX, boxY + y, hueX + HUE_STRIP_WIDTH, boxY + y + 1, color);
        }

        // Secili konum gostergeleri
        int selX = boxX + Math.round(saturation * (SV_BOX_SIZE - 1));
        int selY = boxY + Math.round((1.0f - value) * (SV_BOX_SIZE - 1));
        context.fill(selX - 1, selY - 1, selX + 2, selY + 2, 0xFFFFFFFF);

        int selHueY = boxY + Math.round(hue / 360f * (SV_BOX_SIZE - 1));
        context.fill(hueX - 2, selHueY, hueX + HUE_STRIP_WIDTH + 2, selHueY + 1, 0xFFFFFFFF);

        // Mevcut renk onizlemesi + HEX etiketi
        int previewY = boxY + SV_BOX_SIZE + 20;
        context.fill(boxX, previewY, boxX + 20, previewY + 18, currentColor);
        context.drawTextWithShadow(this.textRenderer, ColorUtil.toHex(currentColor), boxX + 24, previewY + 5, 0xFFFFFF);
    }

    private void drawRecentColors(DrawContext context) {
        int y = recentColorsY();
        context.drawTextWithShadow(this.textRenderer, Text.translatable("paintpack.editor.recent"), svBoxX(), y - 12, 0xAAAAAA);
        int size = 16;
        int index = 0;
        for (int color : recentColors) {
            int sx = svBoxX() + index * (size + 2);
            context.fill(sx, y, sx + size, y + size, 0xFF000000 | (color & 0xFFFFFF));
            index++;
        }
    }

    private void drawInfoPanel(DrawContext context) {
        int panelX = this.width - 190;
        int y = svBoxY() + SV_BOX_SIZE + 45;

        String tool = currentTool == Tool.BRUSH
                ? Text.translatable("paintpack.editor.brush").getString()
                : Text.translatable("paintpack.editor.eraser").getString();

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("paintpack.editor.brush_size").getString() + ": " + brushSize + "  (" + tool + ")",
                panelX, y, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("paintpack.editor.zoom").getString() + ": " + zoom + "x",
                panelX, y + 12, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
