package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.client.render.model.BulkheadGeoModel;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.server.block.BulkheadBlock;
import destiny.null_ouroboros.server.block.BulkheadPart;
import destiny.null_ouroboros.server.block.entity.BulkheadBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class BulkheadGeoBlockEntityRenderer extends GeoBlockRenderer<BulkheadBlockEntity> {
    public BulkheadGeoBlockEntityRenderer() {
        super(new BulkheadGeoModel());
    }

    @Override
    public boolean shouldRenderOffScreen(BulkheadBlockEntity animatable) {
        return true;
    }

    @Override
    public boolean shouldRender(BulkheadBlockEntity animatable, Vec3 cameraPos) {
        return true;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, BulkheadBlockEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, sidePackedLight(animatable, packedLight), packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BulkheadBlockEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        String name = bone.getName();
        ResourceLocation mainTexture = getTextureLocation(animatable);
        RenderType boneRenderType = RenderType.entityCutout(mainTexture);
        int light = packedLight;
        int overlay = packedOverlay;

        if (isEmissiveBone(name)) {
            boneRenderType = RenderTypeRegistry.getOpaqueEmissiveRenderType(mainTexture);
            light = LightTexture.FULL_BRIGHT;
            overlay = OverlayTexture.NO_OVERLAY;
        }

        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        super.renderCubesOfBone(poseStack, bone, boneBuffer, light, overlay, red, green, blue, alpha);

        RenderType childType = RenderType.entityCutout(mainTexture);
        VertexConsumer childBuffer = bufferSource.getBuffer(childType);
        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, childType, bufferSource,
                    childBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private static int sidePackedLight(BulkheadBlockEntity animatable, int fallback) {
        Level level = animatable.getLevel();
        if (level == null) {
            return fallback;
        }

        BlockState state = animatable.getBlockState();
        if (BulkheadBlock.isPassable(state)) {
            return fallback;
        }

        Direction facing = state.getValue(BulkheadBlock.FACING);
        BlockPos controller = animatable.getBlockPos();
        BlockPos center = BulkheadBlock.partPos(controller, facing, BulkheadPart.CENTER_MIDDLE);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double side = camera.subtract(Vec3.atCenterOf(center)).dot(Vec3.atLowerCornerOf(facing.getNormal()));
        BlockPos sample = center.relative(side >= 0.0D ? facing : facing.getOpposite());
        return LevelRenderer.getLightColor(level, sample);
    }

    private static boolean isEmissiveBone(String name) {
        return name.equals("emissive") || name.startsWith("emissive");
    }
}
