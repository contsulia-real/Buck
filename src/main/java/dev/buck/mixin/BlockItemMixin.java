package dev.buck.mixin;

import dev.buck.block.MixedSlabPlacement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @ModifyVariable(method = "place", at = @At("HEAD"), argsOnly = true)
    private BlockPlaceContext buck$keepMixedSlabTarget(BlockPlaceContext context) {
        BlockItem self = (BlockItem) (Object) this;
        return MixedSlabPlacement.prepareContext(self.getBlock(), context);
    }

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void buck$placeMixedSlab(BlockPlaceContext context,
                                     CallbackInfoReturnable<BlockState> callback) {
        BlockState mixed = MixedSlabPlacement.getPlacementState(context);
        if (mixed != null) {
            callback.setReturnValue(mixed);
        }
    }

    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void buck$finishPlacement(BlockPlaceContext context,
                                      BlockState state,
                                      CallbackInfoReturnable<Boolean> callback) {
        if (!Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }

        MixedSlabPlacement.finishPlacement(context);
    }
}
