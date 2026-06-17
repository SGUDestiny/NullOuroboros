package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.StrobelightBlockModel;
import destiny.null_ouroboros.server.block.StrobelightBlock;
import destiny.null_ouroboros.server.block.entity.StrobelightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class StrobelightBlockEntityRenderer implements BlockEntityRenderer<StrobelightBlockEntity> {
    private final StrobelightBlockModel model;

    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");

    public StrobelightBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StrobelightBlockModel(ctx.getModelSet().bakeLayer(StrobelightBlockModel.LAYER_LOCATION));
    }

    @Override
    public void render(StrobelightBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        boolean isOn = state.getValue(StrobelightBlock.LIT);
        Direction facing = state.getValue(StrobelightBlock.FACING);

        ResourceLocation texture = isOn ? ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/strobelight.png")
                : ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/strobelight_off.png");

        ModelPart bone = this.model.bone;
        float pivotX = bone.x / 16.0f;
        float pivotY = bone.y / 16.0f;
        float pivotZ = bone.z / 16.0f;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(facing.getOpposite().getRotation());
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);
        poseStack.translate(pivotX, pivotY, pivotZ);

        float angle = be.getRotationAngle() + be.getRotationSpeed() * partialTick;

        VertexConsumer mainConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        this.model.cube2.render(poseStack, mainConsumer, packedLight, packedOverlay);
        this.model.cube3.render(poseStack, mainConsumer, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-0.5, -0.625, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle % 360f));

        this.model.cube1.render(poseStack, mainConsumer, packedLight, packedOverlay);

        if (isOn) {
            VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
            this.model.siren_emissive.render(poseStack, glow, packedLight, packedOverlay);

            renderBeams(poseStack, buffer, be, partialTick, packedLight, packedOverlay);
        } else {
            this.model.siren_emissive.render(poseStack, mainConsumer, packedLight, packedOverlay);
        }

        poseStack.popPose();

        if (isOn) {
            VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
            this.model.emissive.render(poseStack, glow, packedLight, packedOverlay);
        } else {
            this.model.emissive.render(poseStack, mainConsumer, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private void renderBeams(PoseStack poseStack, MultiBufferSource buffer, StrobelightBlockEntity be, float partialTick, int packedLight, int packedOverlay) {
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 blockCenter = Vec3.atCenterOf(be.getBlockPos());
        Vec3 worldView = camPos.subtract(blockCenter);

        Matrix4f modelMatrix = poseStack.last().pose();
        Matrix4f invModel = new Matrix4f(modelMatrix);
        invModel.invert();

        Vector3f localView = new Vector3f((float) worldView.x, (float) worldView.y, (float) worldView.z);
        invModel.transformDirection(localView);
        localView.normalize();

        Vector3f beamDir = new Vector3f(1, 0, 0);
        Vector3f perp = new Vector3f();
        beamDir.cross(localView, perp);
        if (perp.lengthSquared() < 1.0E-4) {
            perp.set(0, 0, 1);
            beamDir.cross(perp, perp);
        }
        perp.normalize();

        float beamLength = 1.25f;
        float halfWidth = 0.1f;

        VertexConsumer beamConsumer = buffer.getBuffer(RenderTypeRegistry.BEAM_RENDER_TYPE);

        drawColouredBeamQuad(beamConsumer, poseStack, beamLength,  halfWidth, perp, localView);
        drawColouredBeamQuad(beamConsumer, poseStack, -beamLength, halfWidth, perp, localView);
    }

    private void drawColouredBeamQuad(VertexConsumer consumer, PoseStack poseStack, float length, float halfWidth, Vector3f perp, Vector3f normal) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        float wx = perp.x() * halfWidth;
        float wy = perp.y() * halfWidth;
        float wz = perp.z() * halfWidth;

        int startR = 255, startG = 40, startB = 40, startA = 255;
        int endR = 255, endG = 40, endB = 40, endA = 0;

        consumer.vertex(matrix, 0 - wx, 0 - wy, 0 - wz)
                .color(startR, startG, startB, startA)
                .endVertex();
        consumer.vertex(matrix, 0 + wx, 0 + wy, 0 + wz)
                .color(startR, startG, startB, startA)
                .endVertex();

        consumer.vertex(matrix, length + wx, 0 + wy, 0 + wz)
                .color(endR, endG, endB, endA)
                .endVertex();
        consumer.vertex(matrix, length - wx, 0 - wy, 0 - wz)
                .color(endR, endG, endB, endA)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(StrobelightBlockEntity entity) {
        return true;
    }

    @Override
    public boolean shouldRender(StrobelightBlockEntity entity, Vec3 vec3) {
        return true;
    }
}