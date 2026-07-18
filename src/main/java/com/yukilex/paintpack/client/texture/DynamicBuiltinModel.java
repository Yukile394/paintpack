package com.yukilex.paintpack.client.texture;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.Item;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * PaintPack tarafindan boyanmis bir esyanin, kendi ozel (builtin) render
 * kodumuzla cizilebilmesi icin orijinal modelini saran ince bir katman.
 *
 * Tek farki: isBuiltin() metodu, o an bu esya turu boyanmissa true doner.
 * Bu sayede oyun motoru (Fabric API'nin BuiltinModelItemRenderer yamasi
 * uzerinden) PaintedTextureManager.applyTexture() icinde kaydedilen ozel
 * cizim fonksiyonumuzu cagirir. Esya boyanmamissa, orijinal model
 * degismeden normal sekilde cizilmeye devam eder (delegate'e yonlendirilir).
 *
 * Boylece boyama, "resource pack degistirmis gibi" bir gorunum saglar:
 * elde, yerde, envanterde ve item frame'de esya artik boyanmis haliyle
 * gorunur; ama hicbir orijinal dosya degistirilmez.
 */
final class DynamicBuiltinModel implements BakedModel {

    private final BakedModel delegate;
    private final Item item;

    DynamicBuiltinModel(BakedModel delegate, Item item) {
        this.delegate = delegate;
        this.item = item;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return delegate.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return delegate.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return delegate.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return PaintedTextureManager.getInstance().isPainted(item);
    }

    @Override
    public Sprite getParticleSprite() {
        return delegate.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return delegate.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return delegate.getOverrides();
    }
}
