package destiny.null_ouroboros.client.render.entity.steel_leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.model.steel_leviathan.BurrowMissileGeoModel;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.steel_leviathan.BurrowMissileEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class BurrowMissileGeoRenderer extends GeoEntityRenderer<BurrowMissileEntity> {
    public BurrowMissileGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new BurrowMissileGeoModel());
        this.shadowRadius = 0.25F;
    }

    @Override
    public boolean shouldRender(BurrowMissileEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, BurrowMissileEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        float drillAngle = animatable.getDrillSpinAngle();
        for (GeoBone top : model.topLevelBones()) {
            applyDrillSpin(top, drillAngle);
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BurrowMissileEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        renderBonePass(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha, false);
    }

    private void renderBonePass(PoseStack poseStack, BurrowMissileEntity animatable, GeoBone bone,
                                MultiBufferSource bufferSource, boolean isReRender, float partialTick,
                                int packedLight, int packedOverlay, float red, float green, float blue,
                                float alpha, boolean underEngine) {
        if (bone.isHidden()) {
            return;
        }

        String name = bone.getName();
        boolean nextEngine = underEngine
                || SteelLeviathanBones.isEngineBone(name)
                || SteelLeviathanBones.isPlumeBone(name);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        ResourceLocation texture = nextEngine
                ? SteelLeviathanConstants.engineTexture(animatable.tickCount / SteelLeviathanConstants.ENGINE_FRAME_TICKS)
                : SteelLeviathanConstants.TEXTURE;

        if (SteelLeviathanBones.isEmissiveBone(name)) {
            RenderType glowType = RenderType.entityTranslucentEmissive(texture);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
            super.renderCubesOfBone(poseStack, bone, glowBuffer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else if (SteelLeviathanBones.isBlinkerBone(name)) {
            RenderType offType = RenderType.entityTranslucent(texture);
            VertexConsumer offBuffer = bufferSource.getBuffer(offType);
            super.renderCubesOfBone(poseStack, bone, offBuffer, packedLight, packedOverlay, 0.0F, 0.0F, 0.0F, 1.0F);
            RenderType glowType = RenderType.entityTranslucentEmissive(texture);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
            super.renderCubesOfBone(poseStack, bone, glowBuffer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            RenderType boneRenderType = RenderType.entityCutoutNoCull(texture);
            VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);
            super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        for (GeoBone child : bone.getChildBones()) {
            renderBonePass(poseStack, animatable, child, bufferSource, isReRender, partialTick, packedLight,
                    packedOverlay, red, green, blue, alpha, nextEngine);
        }
        poseStack.popPose();
    }

    private void applyDrillSpin(GeoBone bone, float drillAngle) {
        if (SteelLeviathanBones.isDrillBone(bone.getName())) {
            bone.setRotZ(drillAngle);
        }
        for (GeoBone child : bone.getChildBones()) {
            applyDrillSpin(child, drillAngle);
        }
    }
}
