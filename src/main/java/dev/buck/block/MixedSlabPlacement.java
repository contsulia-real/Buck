package dev.buck.block;

import dev.buck.block.entity.MixedSlabBlockEntity;
import dev.buck.registry.BuckContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

public final class MixedSlabPlacement {
    private MixedSlabPlacement() {
    }

    public static BlockPlaceContext prepareContext(Block heldBlock, BlockPlaceContext context) {
        BlockPos target = context.getClickedPos();
        Placement placement = createPlacement(heldBlock, context, target, !context.replacingClickedOnBlock());

        if (placement == null && !context.replacingClickedOnBlock()) {
            BlockPos clicked = target.relative(context.getClickedFace().getOpposite());
            placement = createPlacement(heldBlock, context, clicked, false);
        }

        return placement == null ? context : new ForcedBlockPlaceContext(context, placement);
    }

    public static BlockState getPlacementState(BlockPlaceContext context) {
        return context instanceof ForcedBlockPlaceContext forced ? forced.placement.state() : null;
    }

    public static void finishPlacement(BlockPlaceContext context) {
        if (!(context instanceof ForcedBlockPlaceContext forced)) {
            return;
        }
        BlockPos pos = forced.placement.pos();
        if (context.getLevel().getBlockEntity(pos) instanceof MixedSlabBlockEntity blockEntity) {
            blockEntity.setSources(forced.placement.first(), forced.placement.second());
        }
    }

    private static Placement createPlacement(Block heldBlock,
                                             BlockPlaceContext context,
                                             BlockPos pos,
                                             boolean adjacentTarget) {
        Half existing = halfFromState(context.getLevel().getBlockState(pos));
        SlabBlock heldSource = sourceSlab(heldBlock);
        if (existing == null || heldSource == null || heldSource == existing.source()) {
            return null;
        }

        boolean heldVertical = heldBlock instanceof VerticalSlabBlock;
        boolean existingVertical = existing.axis() != Direction.Axis.Y;
        if (heldVertical != existingVertical) {
            return null;
        }

        Half intended = intendedHalf(heldBlock, heldSource, context, pos, adjacentTarget);
        if (intended.axis() != existing.axis() || intended.positive() == existing.positive()) {
            return null;
        }

        BlockState state = BuckContent.MIXED_SLAB.get().defaultBlockState()
                .setValue(MixedSlabBlock.AXIS, existing.axis())
                .setValue(MixedSlabBlock.FIRST_POSITIVE, existing.positive());
        return new Placement(pos, state, existing.source(), heldSource);
    }

    private static Half intendedHalf(Block heldBlock,
                                     SlabBlock heldSource,
                                     BlockPlaceContext context,
                                     BlockPos pos,
                                     boolean adjacentTarget) {
        if (heldBlock instanceof VerticalSlabBlock) {
            Direction side = adjacentTarget
                    ? VerticalSlabBlock.placementSide(context)
                    : VerticalSlabBlock.placementSide(
                            context.getClickedFace(),
                            context.getClickLocation(),
                            pos
                    );
            return new Half(heldSource, side.getAxis(), isPositive(side));
        }

        return new Half(
                heldSource,
                Direction.Axis.Y,
                horizontalPositive(
                        context.getClickedFace(),
                        context.getClickLocation(),
                        pos,
                        adjacentTarget
                )
        );
    }

    private static boolean horizontalPositive(Direction face,
                                              Vec3 hit,
                                              BlockPos pos,
                                              boolean adjacentTarget) {
        if (face == Direction.UP) {
            return !adjacentTarget;
        }
        if (face == Direction.DOWN) {
            return adjacentTarget;
        }
        return hit.y - pos.getY() > 0.5D;
    }

    private static Half halfFromState(BlockState state) {
        if (!(state.getBlock() instanceof SlabBlock slab)
                || state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            return null;
        }

        if (slab instanceof VerticalSlabBlock vertical) {
            Direction side = state.getValue(VerticalSlabBlock.FACING);
            return new Half(vertical.source(), side.getAxis(), isPositive(side));
        }

        return new Half(
                slab,
                Direction.Axis.Y,
                state.getValue(SlabBlock.TYPE) == SlabType.TOP
        );
    }

    private static SlabBlock sourceSlab(Block block) {
        if (block instanceof VerticalSlabBlock vertical) {
            return vertical.source();
        }
        return block instanceof SlabBlock slab ? slab : null;
    }

    private static boolean isPositive(Direction direction) {
        return direction == Direction.EAST
                || direction == Direction.SOUTH
                || direction == Direction.UP;
    }

    private record Half(SlabBlock source, Direction.Axis axis, boolean positive) {
    }

    private record Placement(BlockPos pos, BlockState state, SlabBlock first, SlabBlock second) {
    }

    private static final class ForcedBlockPlaceContext extends BlockPlaceContext {
        private final Placement placement;

        private ForcedBlockPlaceContext(BlockPlaceContext original, Placement placement) {
            super(original);
            this.placement = placement;
            replaceClicked = true;
        }

        @Override
        public BlockPos getClickedPos() {
            return placement.pos();
        }
    }
}
