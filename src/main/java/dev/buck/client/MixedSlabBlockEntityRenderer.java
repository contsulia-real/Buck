package dev.buck.client;

import dev.buck.block.entity.MixedSlabBlockEntity;

//? if >=26.3 {
/*public final class MixedSlabBlockEntityRenderer implements
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<MixedSlabBlockEntity, MixedSlabRenderState> {

    public MixedSlabBlockEntityRenderer(
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
    }

    @Override
    public MixedSlabRenderState createRenderState() {
        return new MixedSlabRenderState();
    }

    @Override
    public void extractRenderState(
            MixedSlabBlockEntity blockEntity,
            MixedSlabRenderState state,
            float partialTick,
            net.minecraft.world.phys.Vec3 cameraPos,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTick, cameraPos, breakProgress
        );
        if (!blockEntity.isReady()) {
            state.first.blockState = null;
            state.second.blockState = null;
            return;
        }
        if (blockEntity.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            state.capture(clientLevel, blockEntity.getBlockPos());
        }
        fill(state.first, blockEntity.firstComponentState(), blockEntity);
        fill(state.second, blockEntity.secondComponentState(), blockEntity);
    }

    private static void fill(
            net.minecraft.client.renderer.block.MovingBlockRenderState target,
            net.minecraft.world.level.block.state.BlockState blockState,
            MixedSlabBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        target.randomSeedPos = pos;
        target.blockPos = pos;
        target.blockState = blockState;

        if (blockEntity.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            target.biome = clientLevel.getBiome(pos);
            target.cardinalLighting = clientLevel.cardinalLighting();
            target.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(
            MixedSlabRenderState state,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState cameraState) {
        if (state.first.blockState == null || state.second.blockState == null) {
            return;
        }
        poseStack.pushPose();
        collector.submitMovingBlock(poseStack, state.first, 0);
        collector.submitMovingBlock(poseStack, state.second, 0);
        poseStack.popPose();
    }
}
*///?} else {
public final class MixedSlabBlockEntityRenderer implements
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<MixedSlabBlockEntity> {

    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;

    public MixedSlabBlockEntityRenderer(
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            MixedSlabBlockEntity blockEntity,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        if (!blockEntity.isReady()) {
            return;
        }

        blockRenderer.renderSingleBlock(
                blockEntity.firstComponentState(), poseStack, buffers, packedLight, packedOverlay
        );
        blockRenderer.renderSingleBlock(
                blockEntity.secondComponentState(), poseStack, buffers, packedLight, packedOverlay
        );
    }
}
//?}
