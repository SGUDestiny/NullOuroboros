package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.client.render.model.FuseBoxGeoModel;
import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class FuseBoxGeoBlockEntityRenderer extends GeoBlockRenderer<FuseBoxBlockEntity> {
    public FuseBoxGeoBlockEntityRenderer() {
        super(new FuseBoxGeoModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, FuseBoxBlockEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (animatable.isOpen()) {
            for (int i = 0; i < FuseBoxBlockEntity.SLOT_COUNT; i++) {
                int slot = i;
                boolean missing = !animatable.hasFuse(slot);
                boolean hideEmissive = missing || !animatable.isSwitchOn(slot);
                model.getBone(fuseBoneName(slot)).ifPresent(bone -> bone.setHidden(missing));
                model.getBone(emissiveBoneName(slot)).ifPresent(bone -> bone.setHidden(hideEmissive));
            }
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FuseBoxBlockEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (bone.isHidden()) {
            return;
        }

        if (!animatable.isOpen()) {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        String name = bone.getName();
        Integer slot = slotIndex(name);
        if (slot != null && !animatable.hasFuse(slot)) {
            return;
        }

        ResourceLocation mainTexture = getTextureLocation(animatable);
        RenderType boneRenderType = RenderType.entityCutout(mainTexture);
        int light = packedLight;
        int overlay = packedOverlay;

        if (slot != null) {
            boolean on = animatable.isSwitchOn(slot);
            if (isEmissiveBone(name)) {
                if (!on) {
                    return;
                }
                boneRenderType = RenderType.entityTranslucentEmissive(mainTexture);
                light = LightTexture.FULL_BRIGHT;
                overlay = OverlayTexture.NO_OVERLAY;
            } else if (!on) {
                boneRenderType = RenderType.entityCutout(FuseBoxGeoModel.OPEN_FUSE_OFF_TEXTURE);
            }
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

    private static String fuseBoneName(int slot) {
        return slot == 0 ? "fuse" : "fuse" + (slot + 1);
    }

    private static String emissiveBoneName(int slot) {
        return slot == 0 ? "emissive" : "emissive" + (slot + 1);
    }

    private static boolean isEmissiveBone(String name) {
        return name.equals("emissive") || name.startsWith("emissive");
    }

    private static Integer slotIndex(String name) {
        if (name.equals("fuse") || name.equals("emissive")) {
            return 0;
        }
        if (name.startsWith("fuse")) {
            try {
                return Integer.parseInt(name.substring(4)) - 1;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (name.startsWith("emissive")) {
            try {
                return Integer.parseInt(name.substring(8)) - 1;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
