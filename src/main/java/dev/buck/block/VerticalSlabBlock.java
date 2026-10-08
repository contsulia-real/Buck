package dev.buck.block;

import dev.buck.registry.BuckContent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
//? if =1.21.1 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


public class VerticalSlabBlock extends SlabBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape WEST_SHAPE = Block.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(8, 0, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 8, 16, 16, 16);
    private final SlabBlock source;

    public VerticalSlabBlock(SlabBlock source, BlockBehaviour.Properties properties) {
        super(properties);
        this.source = source;
        registerDefaultState(defaultBlockState()
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    public SlabBlock source() {
        return source;
    }

    //? if =1.20.1 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return transformCopper(state, level, pos, player, hand, player.getItemInHand(hand))
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : super.use(state, level, pos, player, hand, hit);
    }
    *///?}

    //? if =1.21.1 {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return transformCopper(state, level, pos, player, hand, stack)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    //?}

    //? if >=26.3 {
    /*@Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return transformCopper(state, level, pos, player, hand, stack)
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    *///?}

    private static boolean isClientLevel(Level level) {
        //? if >=26.3 {
        /*return level.isClientSide();
        *///?} else {
        return level.isClientSide;
        //?}
    }

    private boolean transformCopper(BlockState state, Level level, BlockPos pos,
                                    Player player, InteractionHand hand, ItemStack stack) {
        Block nextSource;
        boolean waxOn = stack.is(Items.HONEYCOMB);
        boolean waxOff = false;

        if (waxOn) {
            nextSource = HoneycombItem.WAXABLES.get().get(source);
        } else if (stack.is(ItemTags.AXES)) {
            nextSource = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(source);
            waxOff = nextSource != null;
            if (nextSource == null) {
                nextSource = WeatheringCopper.getPrevious(source).orElse(null);
            }
        } else {
            return false;
        }

        if (!(nextSource instanceof SlabBlock slab)) {
            return false;
        }

        VerticalSlabBlock replacement = BuckContent.getVerticalSlabBlock(slab);
        if (replacement == null) {
            return false;
        }
        if (!isClientLevel(level)) {
            BlockState next = replacement.defaultBlockState()
                    .setValue(TYPE, state.getValue(TYPE))
                    .setValue(FACING, state.getValue(FACING))
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            if (!level.setBlock(pos, next, Block.UPDATE_ALL)) {
                return false;
            }
            if (waxOn) {
                level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.levelEvent(null, 3003, pos, 0);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else {
                //? if >=26.3 {
                /*level.playSound(null, pos, waxOff ? SoundEvents.AXE_WAX_OFF.value() : SoundEvents.AXE_SCRAPE.value(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                *///?} else {
                level.playSound(null, pos, waxOff ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                //?}
                level.levelEvent(null, waxOff ? 3004 : 3005, pos, 0);
                if (!player.getAbilities().instabuild) {
                    //? if =1.20.1 {
                    /*stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
                    *///?} else {
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    //?}
                }
            }
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == SlabType.DOUBLE) {
            return Shapes.block();
        }

        return switch (state.getValue(FACING)) {
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(pos);

        if (existing.is(this)) {
            return existing
                    .setValue(TYPE, SlabType.DOUBLE)
                    .setValue(WATERLOGGED, false);
        }

        boolean waterlogged = context.getLevel().getFluidState(pos).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(FACING, placementSide(context))
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (state.getValue(TYPE) == SlabType.DOUBLE || !context.getItemInHand().is(asItem())) {
            return false;
        }

        if (!context.replacingClickedOnBlock()) {
            return true;
        }

        return placementSide(context) == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    public static Direction placementSide(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal() && !context.replacingClickedOnBlock()) {
            return face.getOpposite();
        }
        return placementSide(face, context.getClickLocation(), context.getClickedPos());
    }

    public static Direction placementSide(Direction face, Vec3 hit, BlockPos pos) {
        if (face.getAxis().isHorizontal()) {
            return face;
        }

        double x = hit.x - pos.getX();
        double z = hit.z - pos.getZ();

        double xDistance = Math.abs(x - 0.5);
        double zDistance = Math.abs(z - 0.5);

        if (xDistance > zDistance) {
            return x < 0.5 ? Direction.WEST : Direction.EAST;
        }
        return z < 0.5 ? Direction.NORTH : Direction.SOUTH;
    }
}
