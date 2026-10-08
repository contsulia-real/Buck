package dev.buck.neoforge;

import dev.buck.Buck;
import net.neoforged.fml.common.Mod;

@Mod(Buck.MOD_ID)
public final class BuckNeoForge {
    public BuckNeoForge() {
        Buck.init();
    }
}
