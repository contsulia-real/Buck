package dev.buck.neoforge;

import dev.buck.Buck;
import dev.buck.client.MixedSlabBlockEntityRenderer;
import dev.buck.registry.BuckContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = Buck.MOD_ID, dist = Dist.CLIENT)
public final class BuckNeoForgeClient {
    public BuckNeoForgeClient(IEventBus modEventBus) {
        modEventBus.addListener(BuckNeoForgeClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BuckContent.MIXED_SLAB_ENTITY.get(),
                MixedSlabBlockEntityRenderer::new
        );
    }
}
