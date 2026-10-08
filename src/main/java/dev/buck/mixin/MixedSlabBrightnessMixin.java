package dev.buck.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

// Match AO to the complete block occupied by two joined slabs.
//? if >=26.3 {
/*@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixedSlabBrightnessMixin {
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "getShadeBrightness",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            cancellable = true)
    private void buck$shadeJoinedSlabsAsFullBlock(
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> result) {
        if (level instanceof dev.buck.client.MixedSlabRenderState.NeighborAwareMovingBlockRenderState render
                && pos.equals(render.blockPos)
                && (Object) this == render.blockState) {
            net.minecraft.world.level.block.state.BlockState slab =
                    (net.minecraft.world.level.block.state.BlockState) (Object) this;
            if (slab.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
                result.setReturnValue(slab.setValue(
                        net.minecraft.world.level.block.SlabBlock.TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                ).getShadeBrightness(level, pos));
            }
        }
    }
}
*///?} else {
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixedSlabBrightnessMixin {}
//?}
