package dev.buck.fabric;

import dev.buck.Buck;
import net.fabricmc.api.ModInitializer;

public final class BuckFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Buck.init();
    }
}
