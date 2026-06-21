package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.TemporalSurgeDetectorBlockModel;
import destiny.null_ouroboros.server.block.entity.TemporalSurgeDetectorBlockEntity;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import static destiny.null_ouroboros.server.block.TemporalSurgeDetectorBlock.POWERED;

public class TemporalSurgeDetectorBlockEntityRenderer implements BlockEntityRenderer<TemporalSurgeDetectorBlockEntity> {
    private final TemporalSurgeDetectorBlockModel model;

    public TemporalSurgeDetectorBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new TemporalSurgeDetectorBlockModel(ctx.bakeLayer(TemporalSurgeDetectorBlockModel.LAYER_LOCATION));
    }

    @Override
    public void render(TemporalSurgeDetectorBlockEntity temporalEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean powered = temporalEntity.getBlockState().getValue(POWERED);
        ResourceLocation TEXTURE = powered ? ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/temporal_surge_detector.png")
                : ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/temporal_surge_detector_off.png");

        ModelPart bone = this.model.bone;
        float pivotX = bone.x / 16f;
        float pivotY = bone.y / 16f;
        float pivotZ = bone.z / 16f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Direction.UP.getOpposite().getRotation());
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);
        poseStack.translate(-0.5, 0, 0.5);

        float speed = getCurrentSpeed(temporalEntity);
        float ring1Angle = temporalEntity.getRing1Angle() + partialTick * speed;
        float ring2Angle = temporalEntity.getRing2Angle() + partialTick * speed;

        this.model.ring1.yRot = ring1Angle * (float) Math.PI / 180f;
        this.model.ring2.xRot = ring2Angle * (float) Math.PI / 180f;

        VertexConsumer mainConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, mainConsumer, packedLight, packedOverlay, 1f, 1f, 1f, 1f);

        if (powered) {
            VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            poseStack.pushPose();
            model.bone.translateAndRotate(poseStack);
            model.emissive.render(poseStack, glow, LightTexture.FULL_BRIGHT, packedOverlay);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private float getCurrentSpeed(TemporalSurgeDetectorBlockEntity be) {
        if (be.getLevel() == null) return 0f;

        ManifoldingPhase phase = ClientManifoldingHolder.getPhase();
        long startTime = ClientManifoldingHolder.getPhaseStartTime();
        long now = be.getLevel().getGameTime();
        long elapsed = now - startTime;

        float baseSpeed = 0f;
        switch (phase) {
            case PRE_EVENT -> {
                int preDur = ClientManifoldingHolder.getPreDuration();
                if (preDur > 0) {
                    float progress = Math.min(1f, (float) elapsed / preDur);
                    baseSpeed = progress * 3f;
                }
            }
            case ACTIVE -> baseSpeed = 6f;
            case POST_EVENT -> {
                int postDur = ClientManifoldingHolder.getPostDuration();
                if (postDur > 0) {
                    float postProgress = Math.min(1f, (float) elapsed / postDur);
                    baseSpeed = (1f - postProgress) * 6f;
                }
            }
            default -> baseSpeed = 0f;
        }

        float burstBoost = be.getBurstFactor();
        return baseSpeed + burstBoost * TemporalSurgeDetectorBlockEntity.BURST_SPEED;
    }

    @Override
    public boolean shouldRenderOffScreen(TemporalSurgeDetectorBlockEntity entity) {
        return true;
    }

    @Override
    public boolean shouldRender(TemporalSurgeDetectorBlockEntity entity, Vec3 vec3) {
        return true;
    }
}