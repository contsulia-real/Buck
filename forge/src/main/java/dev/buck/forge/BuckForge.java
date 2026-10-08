package dev.buck.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.buck.Buck;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Buck.MOD_ID)
public final class BuckForge {
    public BuckForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Buck.MOD_ID, modBus);
        Buck.init();
    }
}
