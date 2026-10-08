package dev.buck.block;

import dev.buck.block.entity.MixedSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MixedSlabBreak {
    private static final double RAY_LENGTH = 8.0D;

    private MixedSlabBreak() {
    }

    public static Target target(BlockGetter level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof MixedSlabBlockEntity mixed) || !mixed.isReady()) {
            return null;
        }

        BlockState first = mixed.firstComponentState();
        BlockState second = mixed.secondComponentState();
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RAY_LENGTH));

        double firstDistance = hitDistance(first, level, pos, start, end);
        double secondDistance = hitDistance(second, level, pos, start, end);

        if (firstDistance == Double.POSITIVE_INFINITY && secondDistance == Double.POSITIVE_INFINITY) {
            boolean positive = eyeOnPositiveSide(start, pos, mixed.getBlockState().getValue(MixedSlabBlock.AXIS));
            boolean firstPositive = mixed.getBlockState().getValue(MixedSlabBlock.FIRST_POSITIVE);
            return positive == firstPositive
                    ? new Target(first, second)
                    : new Target(second, first);
        }

        return firstDistance <= secondDistance
                ? new Target(first, second)
                : new Target(second, first);
    }

    public static boolean canDestroy(Player player, Level level, BlockPos pos, BlockState state) {
        ItemStack stack = player.getMainHandItem();
        //? if >=26.3 {
        /*return stack.canDestroyBlock(state, level, pos, player);
        *///?} else {
        return stack.getItem().canAttackBlock(state, level, pos, player);
        //?}
    }

    private static double hitDistance(BlockState state,
                                      BlockGetter level,
                                      BlockPos pos,
                                      Vec3 start,
                                      Vec3 end) {
        VoxelShape shape = state.getShape(level, pos);
        BlockHitResult hit = shape.clip(start, end, pos);
        return hit == null ? Double.POSITIVE_INFINITY : start.distanceToSqr(hit.getLocation());
    }

    private static boolean eyeOnPositiveSide(Vec3 eye, BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> eye.x >= pos.getX() + 0.5D;
            case Y -> eye.y >= pos.getY() + 0.5D;
            case Z -> eye.z >= pos.getZ() + 0.5D;
        };
    }

    public record Target(BlockState broken, BlockState remaining) {
    }
}
