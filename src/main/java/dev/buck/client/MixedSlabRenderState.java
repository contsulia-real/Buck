package dev.buck.client;

//? if >=26.3 {
/*public final class MixedSlabRenderState
        extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
    public final NeighborAwareMovingBlockRenderState first = new NeighborAwareMovingBlockRenderState();
    public final NeighborAwareMovingBlockRenderState second = new NeighborAwareMovingBlockRenderState();

    // Snapshot the real neighborhood on the extraction thread. A moving-block
    // state normally reports air everywhere except its own position, which
    // breaks terrain-slab neighbor geometry and ambient-occlusion sampling.
    public void capture(net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.core.BlockPos pos) {
        java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> neighbors =
                new java.util.HashMap<>();
        for (net.minecraft.core.BlockPos sample : net.minecraft.core.BlockPos.betweenClosed(
                pos.offset(-1, -2, -1), pos.offset(1, 1, 1))) {
            neighbors.put(sample.immutable(), level.getBlockState(sample));
        }
        var snapshot = java.util.Map.copyOf(neighbors);
        first.neighbors = snapshot;
        second.neighbors = snapshot;

        java.util.Map<net.minecraft.world.level.ColorResolver, Integer> tints =
                new java.util.IdentityHashMap<>();
        for (net.minecraft.world.level.ColorResolver resolver : new net.minecraft.world.level.ColorResolver[]{
                net.minecraft.client.renderer.BiomeColors.GRASS_COLOR_RESOLVER,
                net.minecraft.client.renderer.BiomeColors.FOLIAGE_COLOR_RESOLVER,
                net.minecraft.client.renderer.BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER,
                net.minecraft.client.renderer.BiomeColors.WATER_COLOR_RESOLVER}) {
            tints.put(resolver, level.getBlockTint(pos, resolver));
        }
        var colors = java.util.Map.copyOf(tints);
        first.tints = colors;
        second.tints = colors;
        first.worldHeight = second.worldHeight = level.getHeight();
        first.worldMinY = second.worldMinY = level.getMinY();
    }

    public static final class NeighborAwareMovingBlockRenderState
            extends net.minecraft.client.renderer.block.MovingBlockRenderState {
        private java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> neighbors =
                java.util.Map.of();
        private java.util.Map<net.minecraft.world.level.ColorResolver, Integer> tints = java.util.Map.of();
        private int worldHeight = 1;
        private int worldMinY;

        @Override
        public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
            // Each of the two renders sees its own slab state at the shared position.
            if (pos.equals(blockPos)) {
                return blockState;
            }
            return neighbors.getOrDefault(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }

        @Override
        public net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.core.BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getBlockTint(net.minecraft.core.BlockPos pos, net.minecraft.world.level.ColorResolver resolver) {
            if (pos.equals(blockPos)) {
                Integer blended = tints.get(resolver);
                if (blended != null) {
                    return blended;
                }
            }
            return super.getBlockTint(pos, resolver);
        }

        @Override
        public int getHeight() {
            return worldHeight;
        }

        @Override
        public int getMinY() {
            return worldMinY;
        }
    }
}
*///?}
