package dev.buck.block.entity;

import dev.buck.block.MixedSlabBlock;
import dev.buck.block.VerticalSlabBlock;
import dev.buck.registry.BuckContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class MixedSlabBlockEntity extends BlockEntity {
    private SlabBlock first;
    private SlabBlock second;
    // Keep unresolved IDs intact when another mod is temporarily unavailable.
    private String savedFirstId = "";
    private String savedSecondId = "";

    public MixedSlabBlockEntity(BlockPos pos, BlockState state) {
        super(BuckContent.MIXED_SLAB_ENTITY.get(), pos, state);
    }

    public boolean isReady() {
        return first != null && second != null;
    }

    public SlabBlock firstSource() {
        return first;
    }

    public SlabBlock secondSource() {
        return second;
    }

    public void setSources(SlabBlock first, SlabBlock second) {
        this.first = first;
        this.second = second;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public BlockState firstComponentState() {
        return componentState(first, getBlockState().getValue(MixedSlabBlock.FIRST_POSITIVE));
    }

    public BlockState secondComponentState() {
        return componentState(second, !getBlockState().getValue(MixedSlabBlock.FIRST_POSITIVE));
    }

    private BlockState componentState(SlabBlock source, boolean positive) {
        Direction.Axis axis = getBlockState().getValue(MixedSlabBlock.AXIS);
        if (axis == Direction.Axis.Y) {
            return source.defaultBlockState()
                    .setValue(SlabBlock.TYPE, positive ? SlabType.TOP : SlabType.BOTTOM)
                    .setValue(SlabBlock.WATERLOGGED, false);
        }

        VerticalSlabBlock vertical = BuckContent.getVerticalSlabBlock(source);
        Direction side = axis == Direction.Axis.X
                ? (positive ? Direction.EAST : Direction.WEST)
                : (positive ? Direction.SOUTH : Direction.NORTH);
        return vertical.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(VerticalSlabBlock.FACING, side)
                .setValue(SlabBlock.WATERLOGGED, false);
    }

    private void loadIds(String firstId, String secondId) {
        savedFirstId = firstId;
        savedSecondId = secondId;
        first = BuckContent.getSourceSlab(firstId);
        second = BuckContent.getSourceSlab(secondId);
    }

    private String firstId() {
        return first == null ? savedFirstId : BuckContent.getSourceId(first);
    }

    private String secondId() {
        return second == null ? savedSecondId : BuckContent.getSourceId(second);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if =1.20.1 {
    /*@Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadIds(tag.getString("first"), tag.getString("second"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("first", firstId());
        tag.putString("second", secondId());
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
    *///?} elif <26.3 {
    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        loadIds(tag.getString("first"), tag.getString("second"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("first", firstId());
        tag.putString("second", secondId());
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        loadIds(input.getStringOr("first", ""), input.getStringOr("second", ""));
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putString("first", firstId());
        output.putString("second", secondId());
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    *///?}
}
