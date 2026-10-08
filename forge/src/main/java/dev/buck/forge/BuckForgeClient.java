package dev.buck.forge;

import dev.buck.Buck;
import dev.buck.client.MixedSlabBlockEntityRenderer;
import dev.buck.registry.BuckContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Buck.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BuckForgeClient {
    private BuckForgeClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BuckContent.MIXED_SLAB_ENTITY.get(),
                MixedSlabBlockEntityRenderer::new
        );
    }
}
