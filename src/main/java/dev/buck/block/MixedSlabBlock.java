package dev.buck.block;

import com.mojang.serialization.MapCodec;
import dev.buck.block.entity.MixedSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

public final class MixedSlabBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty FIRST_POSITIVE = BooleanProperty.create("first_positive");

    public MixedSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(FIRST_POSITIVE, false));
    }

    //? if =1.21.1 {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MixedSlabBlock::new);
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, FIRST_POSITIVE);
    }

    @Override
    public MixedSlabBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixedSlabBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof MixedSlabBlockEntity mixed) || !mixed.isReady()) {
            return;
        }
        SlabBlock first = nextOxidizedSlab(mixed.firstSource(), level, pos, random);
        SlabBlock second = nextOxidizedSlab(mixed.secondSource(), level, pos, random);
        if (first != mixed.firstSource() || second != mixed.secondSource()) {
            mixed.setSources(first, second);
        }
    }

    private static SlabBlock nextOxidizedSlab(SlabBlock source, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(source instanceof WeatheringCopper copper)) {
            return source;
        }
        Block next = WeatheringCopper.getNext(source).orElse(null);
        if (!(next instanceof SlabBlock nextSlab) || random.nextFloat() >= 0.05688889F) {
            return source;
        }

        int age = copper.getAge().ordinal();
        int sameAge = 0;
        int olderAge = 0;
        //? if >=26.3 {
        /*Iterable<BlockPos> neighbors = BlockPos.withinManhattan(pos, 4);
        *///?} else {
        Iterable<BlockPos> neighbors = BlockPos.withinManhattan(pos, 4, 4, 4);
        //?}
        for (BlockPos neighbor : neighbors) {
            if (neighbor.equals(pos) || neighbor.distManhattan(pos) > 4) {
                continue;
            }
            BlockState neighborState = level.getBlockState(neighbor);
            Block[] components;
            if (neighborState.getBlock() instanceof MixedSlabBlock
                    && level.getBlockEntity(neighbor) instanceof MixedSlabBlockEntity other
                    && other.isReady()) {
                components = new Block[]{other.firstSource(), other.secondSource()};
            } else {
                components = new Block[]{neighborState.getBlock()};
            }
            for (Block component : components) {
                if (!(component instanceof WeatheringCopper otherCopper)) {
                    continue;
                }
                int neighborAge = otherCopper.getAge().ordinal();
                if (neighborAge < age) {
                    return source;
                }
                if (neighborAge == age) {
                    sameAge++;
                } else {
                    olderAge++;
                }
            }
        }
        float chance = (olderAge + 1.0F) / (olderAge + sameAge + 1.0F);
        return random.nextFloat() < chance * chance * copper.getChanceModifier() ? nextSlab : source;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        MixedSlabBreak.Target target = MixedSlabBreak.target(level, pos, player);
        return target == null
                ? super.getDestroyProgress(state, player, level, pos)
                : target.broken().getDestroyProgress(player, level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MixedSlabBlockEntity mixed)
                || !mixed.isReady()) {
            return List.of();
        }

        BlockState original = builder.getParameter(LootContextParams.BLOCK_STATE);
        List<ItemStack> drops = new ArrayList<>();
        try {
            BlockState first = mixed.firstComponentState();
            builder.withParameter(LootContextParams.BLOCK_STATE, first);
            drops.addAll(first.getDrops(builder));

            BlockState second = mixed.secondComponentState();
            builder.withParameter(LootContextParams.BLOCK_STATE, second);
            drops.addAll(second.getDrops(builder));
        } finally {
            builder.withParameter(LootContextParams.BLOCK_STATE, original);
        }
        return drops;
    }
}
