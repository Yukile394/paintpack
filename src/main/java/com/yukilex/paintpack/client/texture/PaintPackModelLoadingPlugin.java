package com.yukilex.paintpack.client.texture;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

public final class PaintPackModelLoadingPlugin implements ModelLoadingPlugin {

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (model == null) {
                return null;
            }
            ModelIdentifier id = context.topLevelId();
            if (id == null || !"inventory".equals(id.getVariant())) {
                return model;
            }

            Item item = Registries.ITEM.get(id.id());
            if (item == null || item == Items.AIR) {
                return model;
            }

            return (BakedModel) new DynamicBuiltinModel(model, item);
        });
    }
}
