package com.yukilex.paintpack.client.texture;

import com.yukilex.paintpack.client.paint.PaintCanvas;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PaintPack modunun texture yonetimi.
 *
 * Tasarim karari: Boyama, "elde tutulan tek bir esya ornegi" degil,
 * "esya TURU" bazinda uygulanir (orn. bir Elmas Kilic boyandiginda,
 * o anki ve sonraki tum Elmas Kiliclar boyanmis gorunur). Bunun sebebi,
 * Minecraft'ta bir esya turunun normalde tek bir paylasilan texture'i
 * olmasidir; boylece "resource pack" mantigina benzer, ancak
 * orijinal dosyalar hicbir sekilde degistirilmeden calisir.
 *
 * Boyanan texture'lar yalnizca mod'un kendi klasorune
 * (.minecraft/config/paintpack/textures) PNG olarak yazilir. Minecraft'in
 * kendi resource/texture dosyalarina asla dokunulmaz.
 */
public final class PaintedTextureManager {

    private static final PaintedTextureManager INSTANCE = new PaintedTextureManager();

    private final Path storageDir;
    /** Boyanmis esya turlerini takip eder (tekrar renderer kaydetmemek icin). */
    private final Set<Item> paintedItems = new HashSet<>();
    /** Item -> uygulanan ozel texture kimligi. */
    private final Map<Item, Identifier> appliedTextures = new HashMap<>();

    private PaintedTextureManager() {
        this.storageDir = FabricLoader.getInstance().getConfigDir().resolve("paintpack").resolve("textures");
    }

    public static PaintedTextureManager getInstance() {
        return INSTANCE;
    }

    /** Bu esya turu daha once boyanmis mi (DynamicBuiltinModel bunu her render'da kontrol eder). */
    public boolean isPainted(Item item) {
        return paintedItems.contains(item);
    }

    /** Bu esya turunu boyamadan once nasilsa oyle geri getirir: kayitli PNG silinir, ozel render kapanir. */
    public void resetToOriginal(Item item) {
        paintedItems.remove(item);
        appliedTextures.remove(item);
        try {
            Path saved = pathFor(item);
            Files.deleteIfExists(saved);
        } catch (IOException e) {
            System.err.println("[PaintPack] Kayitli texture silinemedi: " + e.getMessage());
        }
    }

    /** Mod baslatilirken cagrilir: daha once kaydedilmis tum texture'lari yukler ve uygular. */
    public void loadSavedTextures() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            System.err.println("[PaintPack] Depolama klasoru olusturulamadi: " + e.getMessage());
            return;
        }

        try (var stream = Files.list(storageDir)) {
            stream.filter(path -> path.toString().endsWith(".png")).forEach(this::loadTextureFile);
        } catch (IOException e) {
            System.err.println("[PaintPack] Kayitli texture'lar yuklenemedi: " + e.getMessage());
        }
    }

    private void loadTextureFile(Path file) {
        String fileName = file.getFileName().toString();
        String itemIdString = fileName.substring(0, fileName.length() - 4).replaceFirst("_", ":");
        Identifier itemId = Identifier.tryParse(itemIdString);
        if (itemId == null) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.item.Items.AIR) {
            return;
        }

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            applyTexture(item, image);
        } catch (IOException e) {
            System.err.println("[PaintPack] Texture okunamadi (" + fileName + "): " + e.getMessage());
        }
    }

    /**
     * Elde tutulan esyanin duzenlenebilecek bir tuval (PaintCanvas) olarak
     * yuklenmesini saglar. Onceden boyanmis bir texture varsa onun uzerinden,
     * yoksa oyunun orijinal texture'i uzerinden baslar.
     */
    public PaintCanvas loadCanvasFor(ItemStack stack) {
        Item item = stack.getItem();
        NativeImage source = null;

        Path savedFile = pathFor(item);
        if (Files.exists(savedFile)) {
            try (InputStream in = Files.newInputStream(savedFile)) {
                source = NativeImage.read(in);
            } catch (IOException e) {
                System.err.println("[PaintPack] Kayitli texture yuklenemedi, orijinal kullanilacak: " + e.getMessage());
            }
        }

        if (source == null) {
            source = readOriginalTexture(stack);
        }

        if (source == null) {
            // Son care: bos 16x16 tuval
            source = new NativeImage(NativeImage.Format.RGBA, 16, 16, true);
        }

        return new PaintCanvas(source);
    }

    /** Oyunun orijinal (vanilla / resource pack) texture dosyasini okur. Bu dosyaya asla yazilmaz. */
    private NativeImage readOriginalTexture(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            BakedModel model = client.getItemRenderer().getModel(stack, client.world, null, 0);
            if (model == null) {
                return null;
            }
            Identifier spriteId = model.getParticleSprite().getContents().getId();
            Identifier textureFile = Identifier.of(spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png");

            Optional<Resource> resource = client.getResourceManager().getResource(textureFile);
            if (resource.isEmpty()) {
                return null;
            }
            try (InputStream in = resource.get().getInputStream()) {
                return NativeImage.read(in);
            }
        } catch (IOException e) {
            System.err.println("[PaintPack] Orijinal texture okunamadi: " + e.getMessage());
            return null;
        }
    }

    /** Duzenlenen tuvali diske kaydeder ve oyunda hemen uygular. */
    public void saveAndApply(ItemStack stack, PaintCanvas canvas) {
        Item item = stack.getItem();
        try {
            Files.createDirectories(storageDir);
            Path target = pathFor(item);

            // Diske PNG olarak yaz (yalnizca mod'un kendi klasorune).
            NativeImage toSave = canvas.toNativeImage();
            try {
                toSave.writeTo(target);
            } finally {
                toSave.close();
            }

            // Oyunda goruntuyu hemen guncellemek icin ayri bir kopya kullanilir
            // (TextureManager bu NativeImage'in sahipligini devralir).
            NativeImage forApply = canvas.toNativeImage();
            applyTexture(item, forApply);
            canvas.markSaved();
        } catch (IOException e) {
            System.err.println("[PaintPack] Texture kaydedilemedi: " + e.getMessage());
        }
    }

    private Path pathFor(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        String fileName = id.getNamespace() + "_" + id.getPath().replace('/', '.') + ".png";
        return storageDir.resolve(fileName);
    }

    /**
     * Verilen esya turu icin ozel bir texture'i oyun icinde etkinlestirir:
     * bir NativeImageBackedTexture kaydeder ve BuiltinItemRendererRegistry
     * uzerinden, o esya turunun elde/yerde/envanterde bu ozel texture ile
     * cizilmesini saglar. Bu islem, hicbir orijinal dosyaya dokunmaz;
     * yalnizca bellek icinde (RAM/GPU) bir gorunum degisikligi yapar.
     */
    private void applyTexture(Item item, NativeImage image) {
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier id = Registries.ITEM.getId(item);
        Identifier textureId = Identifier.of("paintpack", "painted/" + id.getNamespace() + "_" + id.getPath().replace('/', '_'));

        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        client.getTextureManager().registerTexture(textureId, texture);
        appliedTextures.put(item, textureId);

        if (paintedItems.add(item)) {
            BuiltinItemRendererRegistry.INSTANCE.register(item, (stack, mode, matrices, vertexConsumers, light, overlay) ->
                    renderPaintedItem(item, mode, matrices, vertexConsumers, light, overlay));
        }
    }

    /** Boyanmis bir esyayi cizer: bloklar icin kup, digerleri icin kalinlikli (derinlikli) kart. */
    private void renderPaintedItem(Item item, ModelTransformationMode mode, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light, int overlay) {
        Identifier textureId = appliedTextures.get(item);
        if (textureId == null) {
            return;
        }

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(textureId));
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        if (item instanceof net.minecraft.item.BlockItem) {
            drawCube(buffer, posMatrix, light, overlay);
        } else {
            drawFlatCard(buffer, posMatrix, light, overlay);
        }

        matrices.pop();
    }

    private static final float HALF = 0.5f;
    /** Kart kalinligi (yamulma/gecirgen kenar sorununu onlemek icin). */
    private static final float CARD_DEPTH = 1f / 16f;

    /** Araclar, silahlar vb. duz esyalar icin: on, arka ve 4 ince kenar (gercek kalinlik verir). */
    private void drawFlatCard(VertexConsumer buffer, Matrix4f m, int light, int overlay) {
        float d = CARD_DEPTH / 2f;

        // On yuz (+Z)
        face(buffer, m, -HALF, -HALF, d, HALF, -HALF, d, HALF, HALF, d, -HALF, HALF, d, 0, 0, 1, 255, light, overlay, true);
        // Arka yuz (-Z)
        face(buffer, m, HALF, -HALF, -d, -HALF, -HALF, -d, -HALF, HALF, -d, HALF, HALF, -d, 0, 0, -1, 255, light, overlay, true);
        // Alt kenar
        face(buffer, m, -HALF, -HALF, -d, HALF, -HALF, -d, HALF, -HALF, d, -HALF, -HALF, d, 0, -1, 0, 170, light, overlay, false);
        // Ust kenar
        face(buffer, m, -HALF, HALF, d, HALF, HALF, d, HALF, HALF, -d, -HALF, HALF, -d, 0, 1, 0, 170, light, overlay, false);
        // Sol kenar
        face(buffer, m, -HALF, -HALF, -d, -HALF, -HALF, d, -HALF, HALF, d, -HALF, HALF, -d, -1, 0, 0, 170, light, overlay, false);
        // Sag kenar
        face(buffer, m, HALF, -HALF, d, HALF, -HALF, -d, HALF, HALF, -d, HALF, HALF, d, 1, 0, 0, 170, light, overlay, false);
    }

    /** Bloklar icin tam bir kup: her yuzeyde ayni (boyanmis) texture, vanilla'ya benzer golgelendirme. */
    private void drawCube(VertexConsumer buffer, Matrix4f m, int light, int overlay) {
        float h = HALF;
        // Ust (+Y) - en parlak
        face(buffer, m, -h, h, -h, -h, h, h, h, h, h, h, h, -h, 0, 1, 0, 255, light, overlay, true);
        // Alt (-Y) - en koyu
        face(buffer, m, -h, -h, h, -h, -h, -h, h, -h, -h, h, -h, h, 0, -1, 0, 140, light, overlay, true);
        // On (+Z)
        face(buffer, m, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1, 220, light, overlay, true);
        // Arka (-Z)
        face(buffer, m, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1, 220, light, overlay, true);
        // Sol (-X)
        face(buffer, m, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1, 0, 0, 190, light, overlay, true);
        // Sag (+X)
        face(buffer, m, h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1, 0, 0, 190, light, overlay, true);
    }

    /** Dort koseli bir yuzey (quad) cizer; verilen kose sirasi UV (0,0)-(1,0)-(1,1)-(0,1) ile eslesir. */
    private void face(VertexConsumer buffer, Matrix4f m,
                       float x1, float y1, float z1, float x2, float y2, float z2,
                       float x3, float y3, float z3, float x4, float y4, float z4,
                       float nx, float ny, float nz, int brightness, int light, int overlay, boolean useFullTexture) {
        if (useFullTexture) {
            vertex(buffer, m, x1, y1, z1, 0, 1, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x2, y2, z2, 1, 1, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x3, y3, z3, 1, 0, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x4, y4, z4, 0, 0, brightness, light, overlay, nx, ny, nz);
        } else {
            // Ince kenar seritleri icin tekstur kenar pikselinden alinir (ince cizgi gibi gorunmesin diye).
            vertex(buffer, m, x1, y1, z1, 0, 0, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x2, y2, z2, 1, 0, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x3, y3, z3, 1, 0.02f, brightness, light, overlay, nx, ny, nz);
            vertex(buffer, m, x4, y4, z4, 0, 0.02f, brightness, light, overlay, nx, ny, nz);
        }
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z,
                         float u, float v, int brightness, int light, int overlay,
                         float nx, float ny, float nz) {
        buffer.vertex(matrix, x, y, z)
                .color(brightness, brightness, brightness, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(nx, ny, nz);
    }
}
