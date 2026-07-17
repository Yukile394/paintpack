package com.yukilex.paintpack.client;

import com.yukilex.paintpack.client.gui.PaintEditorScreen;
import com.yukilex.paintpack.client.texture.PaintedTextureManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;

/**
 * PaintPack modunun istemci (client) giris noktasi.
 * Bu mod tamamen client-side'dir; sunucu tarafinda hicbir sey degistirmez.
 */
public final class PaintPackClient implements ClientModInitializer {

    /** Varsayilan tus: P. Kullanici, Kontroller menusunden bu tusu degistirebilir. */
    private static KeyBinding openEditorKey;

    @Override
    public void onInitializeClient() {
        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paintpack.open_editor",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_P,
                "key.category.paintpack"
        ));

        // Daha once boyanmis texture'lari oyun acilir acilmaz yukle ve uygula.
        PaintedTextureManager.getInstance().loadSavedTextures();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        while (openEditorKey.wasPressed()) {
            if (client.player == null || client.currentScreen != null) {
                continue;
            }
            ItemStack heldStack = client.player.getMainHandStack();
            if (heldStack.isEmpty()) {
                continue;
            }
            client.setScreen(new PaintEditorScreen(heldStack));
        }
    }
}
