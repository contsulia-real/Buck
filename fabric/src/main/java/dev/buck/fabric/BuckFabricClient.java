package dev.buck.fabric;

import dev.buck.client.MixedSlabBlockEntityRenderer;
import dev.buck.registry.BuckContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class BuckFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(
                BuckContent.MIXED_SLAB_ENTITY.get(),
                MixedSlabBlockEntityRenderer::new
        );
    }
}
