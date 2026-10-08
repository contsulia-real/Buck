package dev.buck.block;

import dev.buck.registry.BuckContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class WeatheringVerticalSlabBlock extends VerticalSlabBlock implements WeatheringCopper {
    public WeatheringVerticalSlabBlock(SlabBlock source, BlockBehaviour.Properties properties) {
        super(source, properties);
    }

    @Override
    public Optional<BlockState> getNext(BlockState state) {
        // Prefer the loader's oxidation map (NeoForge data map), while
        // retaining the vanilla source-slab mapping for Fabric and Forge.
        Block mapped = WeatheringCopper.getNext(this).orElse(null);
        VerticalSlabBlock next;
        if (mapped instanceof VerticalSlabBlock vertical) {
            next = vertical;
        } else {
            Block nextSource = WeatheringCopper.getNext(source()).orElse(null);
            next = nextSource instanceof SlabBlock nextSlab
                    ? BuckContent.getVerticalSlabBlock(nextSlab)
                    : null;
        }
        if (next == null) {
            return Optional.empty();
        }

        return Optional.of(next.defaultBlockState()
                .setValue(TYPE, state.getValue(TYPE))
                .setValue(FACING, state.getValue(FACING))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED)));
    }

    @Override
    public WeatherState getAge() {
        return ((WeatheringCopper) source()).getAge();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        //? if =1.20.1 {
        /*onRandomTick(state, level, pos, random);
        *///?} else {
        changeOverTime(state, level, pos, random);
        //?}
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // The vanilla next-block map may not be populated when block state caches initialize.
        return getAge() != WeatherState.OXIDIZED;
    }
}
