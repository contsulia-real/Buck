"""Regression guard for 26.3 mixed-slab lighting and tint context."""
from pathlib import Path

state = Path("src/main/java/dev/buck/client/MixedSlabRenderState.java").read_text(encoding="utf-8")
renderer = Path("src/main/java/dev/buck/client/MixedSlabBlockEntityRenderer.java").read_text(encoding="utf-8")

assert "extends net.minecraft.client.renderer.block.MovingBlockRenderState" in state, "moving state must be adapted"
assert "getBlockState(net.minecraft.core.BlockPos" in state, "neighbors must be visible to model/AO"
assert "getFluidState(net.minecraft.core.BlockPos" in state, "fluid neighbors must agree with states"
assert "level.getBlockState(sample)" in state, "capture real world neighbor states"
assert "sample.immutable()" in state, "avoid mutable block position keys"
assert "level.getBlockTint(pos, resolver)" in state, "capture world-computed blended biome tints"
assert "BiomeColors.GRASS_COLOR_RESOLVER" in state, "grass tint missing"
assert "BiomeColors.FOLIAGE_COLOR_RESOLVER" in state, "foliage tint missing"
assert "BiomeColors.WATER_COLOR_RESOLVER" in state, "water tint missing"
assert "state.capture(clientLevel, blockEntity.getBlockPos());" in renderer, "render context must be captured on extraction"
assert "collector.submitMovingBlock" in renderer, "existing rendering pipeline must remain"
assert "FULL_BRIGHT" not in renderer and "FULL_BRIGHT" not in state, "don't mask lighting bugs with full brightness"

print("mixed render-context regression: 12 checks passed")
