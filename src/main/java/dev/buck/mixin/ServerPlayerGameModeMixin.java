package dev.buck.mixin;

import dev.buck.block.MixedSlabBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void buck$destroyMixedHalf(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        MixedSlabBreak.Target target = MixedSlabBreak.target(level, pos, player);
        if (target == null) {
            return;
        }

        ServerPlayerGameMode self = (ServerPlayerGameMode) (Object) this;
        if (!MixedSlabBreak.canDestroy(player, level, pos, target.broken())
                || player.blockActionRestricted(level, pos, self.getGameModeForPlayer())) {
            callback.setReturnValue(false);
            return;
        }

        ItemStack hand = player.getMainHandItem();
        ItemStack tool = hand.copy();
        boolean correctTool = player.hasCorrectToolForDrops(target.broken());

        if (!level.setBlock(pos, target.remaining(), Block.UPDATE_ALL)) {
            callback.setReturnValue(false);
            return;
        }

        level.levelEvent(player, 2001, pos, Block.getId(target.broken()));

        if (!self.isCreative()) {
            hand.mineBlock(level, target.broken(), pos, player);
            if (correctTool) {
                target.broken().getBlock().playerDestroy(
                        level, player, pos, target.broken(), null, tool
                );
            }
        }

        callback.setReturnValue(true);
    }

}
