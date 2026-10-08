package dev.buck;

import dev.buck.registry.BuckContent;

public final class Buck {
    public static final String MOD_ID = "buck";

    private Buck() {
    }

    public static void init() {
        BuckContent.init();
    }
}
