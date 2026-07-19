package destiny.null_ouroboros.client.render.entity.steel_leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.model.steel_leviathan.SteelLeviathanPartGeoModel;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanSinew;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class SteelLeviathanPartGeoRenderer extends GeoEntityRenderer<SteelLeviathanPartEntity> {
    public SteelLeviathanPartGeoRenderer(EntityRendererProvider.Context context, String partName) {
        super(context, new SteelLeviathanPartGeoModel(partName));
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(SteelLeviathanPartEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(SteelLeviathanPartEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        SteelLeviathanForceRenderer.markRendered(entity);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, SteelLeviathanPartEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {

        float yaw = Mth.rotLerp(partialTick, animatable.getBodyYawO(), animatable.getBodyYaw());
        float pitch = Mth.rotLerp(partialTick, animatable.getBodyPitchO(), animatable.getBodyPitch());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        applyBonePoses(animatable, model, yaw, pitch, partialTick);
        float heat = animatable.getArmorShedHeat(partialTick);
        float tintR = Mth.lerp(heat, red, 1.0F);
        float tintG = Mth.lerp(heat, green, 0.42F);
        float tintB = Mth.lerp(heat, blue, 0.08F);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, tintR, tintG, tintB, alpha);
        if (!isReRender && animatable instanceof SteelLeviathanHeadEntity head) {
            renderHologram(head, partialTick, poseStack, bufferSource);
        }
    }

    private void renderHologram(SteelLeviathanHeadEntity head, float partialTick, PoseStack poseStack,
                                MultiBufferSource bufferSource) {
        ItemStack stack = head.getHologramStack();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.75D, 10.0D);
        float spin = (head.tickCount + partialTick) * 4.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, head.level(), head.getId());
        poseStack.popPose();
    }

    private void applyBonePoses(SteelLeviathanPartEntity entity, BakedGeoModel model,
                                float lerpedYaw, float lerpedPitch, float partialTick) {
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            GeoBone bone = model.getBone(SteelLeviathanBones.heatsinkBone(i)).orElse(null);
            if (bone != null) {

                boolean show = entity.isHeatsinkPresent(i)
                        && !(entity.isHeatsinkDestroyed(i) && entity.isVulnerable());
                bone.setHidden(!show);
            }
        }

        GeoBone sinew = model.getBone(SteelLeviathanBones.SINEW).orElse(null);
        if (sinew != null) {

            SteelLeviathanPartEntity neighbor = entity.resolveNext();
            if (neighbor == null) {
                neighbor = entity.resolvePrev();
            }
            if (neighbor != null) {
                float nYaw = Mth.rotLerp(partialTick, neighbor.getBodyYawO(), neighbor.getBodyYaw());
                float nPitch = Mth.rotLerp(partialTick, neighbor.getBodyPitchO(), neighbor.getBodyPitch());
                float[] euler = SteelLeviathanSinew.averageBridgeLocalDegrees(
                        lerpedYaw, lerpedPitch, nYaw, nPitch);
                sinew.setRotX(euler[0] * Mth.DEG_TO_RAD);
                sinew.setRotY(-euler[1] * Mth.DEG_TO_RAD);
                sinew.setRotZ(euler[2] * Mth.DEG_TO_RAD);
            } else {
                sinew.setRotX(0);
                sinew.setRotY(0);
                sinew.setRotZ(0);
            }
        }

        boolean thrustersOn = entity.areThrustersActive();
        for (GeoBone top : model.topLevelBones()) {
            applyPlumeVisibility(top, thrustersOn);
        }
    }

    private void applyPlumeVisibility(GeoBone bone, boolean thrustersOn) {
        if (SteelLeviathanBones.isPlumeBone(bone.getName())) {
            bone.setHidden(!thrustersOn);
        }
        for (GeoBone child : bone.getChildBones()) {
            applyPlumeVisibility(child, thrustersOn);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SteelLeviathanPartEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (bone.isHidden()) {
            return;
        }

        ResourceLocation texture = resolveBoneTexture(animatable, bone.getName());
        RenderType boneRenderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        float heat = animatable.getArmorShedHeat(partialTick);
        float tintR = Mth.lerp(heat, red, 1.0F);
        float tintG = Mth.lerp(heat, green, 0.42F);
        float tintB = Mth.lerp(heat, blue, 0.08F);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, tintR, tintG, tintB, alpha);

        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, boneRenderType, bufferSource,
                    boneBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private ResourceLocation resolveBoneTexture(SteelLeviathanPartEntity entity, String boneName) {
        if (!entity.isVulnerable()) {
            int heatsink = SteelLeviathanBones.heatsinkIndexForBone(boneName);
            if (heatsink >= 0 && entity.isHeatsinkDestroyed(heatsink)) {
                return SteelLeviathanConstants.TEXTURE_BROKEN_HEATSINK;
            }
        }
        if (entity.areThrustersActive()
                && (SteelLeviathanBones.isEngineBone(boneName) || SteelLeviathanBones.isPlumeBone(boneName))) {
            return SteelLeviathanConstants.engineTexture(entity.tickCount / SteelLeviathanConstants.ENGINE_FRAME_TICKS);
        }
        return entity.isVulnerable() ? SteelLeviathanConstants.TEXTURE_VULNERABLE : SteelLeviathanConstants.TEXTURE;
    }

    @Override
    public ResourceLocation getTextureLocation(SteelLeviathanPartEntity entity) {
        return entity.isVulnerable() ? SteelLeviathanConstants.TEXTURE_VULNERABLE : SteelLeviathanConstants.TEXTURE;
    }
}

