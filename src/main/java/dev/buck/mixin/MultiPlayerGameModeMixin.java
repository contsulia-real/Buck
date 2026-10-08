package dev.buck.mixin;

import dev.buck.block.MixedSlabBreak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void buck$predictMixedHalfBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        MixedSlabBreak.Target target = MixedSlabBreak.target(level, pos, player);
        if (target == null) {
            return;
        }

        MultiPlayerGameMode self = (MultiPlayerGameMode) (Object) this;
        if (player.blockActionRestricted(level, pos, self.getPlayerMode())
                || !MixedSlabBreak.canDestroy(player, level, pos, target.broken())) {
            callback.setReturnValue(false);
            return;
        }

        level.addDestroyBlockEffect(pos, target.broken());
        SoundType sound = target.broken().getSoundType();
        level.playLocalSound(
                pos,
                sound.getBreakSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F,
                false
        );

        callback.setReturnValue(level.setBlock(
                pos,
                target.remaining(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
        ));
    }
}
