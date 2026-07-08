package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.EngineHoistGeoModel;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.*;

public class EngineHoistGeoRenderer extends GeoEntityRenderer<EngineHoistEntity> {
    private static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/engine_hoist.png");
    private static final ResourceLocation OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/engine_hoist_off.png");
    private static final ResourceLocation COLORED = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/engine_hoist_colored.png");

    private static final Map<DusterbikePartType, List<String>> PART_BONES = Map.ofEntries(
            Map.entry(DusterbikePartType.ENGINE, List.of("Engine")),
            Map.entry(DusterbikePartType.PISTON_FRONT, List.of("PistonFront", "PistonFrontEmissive")),
            Map.entry(DusterbikePartType.PISTON_REAR, List.of("PistonRear", "PistonRearEmissive")),
            Map.entry(DusterbikePartType.SPARK_PLUG_FRONT, List.of("SparkPlugFront", "SparkPlugFrontEmissive")),
            Map.entry(DusterbikePartType.SPARK_PLUG_REAR, List.of("SparkPlugRear", "SparkPlugRearEmissive")),
            Map.entry(DusterbikePartType.KEY, List.of("Key", "KeyEmissive"))
    );

    public EngineHoistGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new EngineHoistGeoModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, EngineHoistEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        long gameTime = animatable.level().getGameTime();
        float timeSinceHit = (gameTime - animatable.getLastDamageTick()) + partialTick;
        if (timeSinceHit < 5.0F) {
            int health = animatable.getHoistHealth();
            float damageRatio = 1.0F - (health / 3.0F);
            float amplitude = damageRatio * 11.25F;
            float wobble = Mth.sin(timeSinceHit / 1.5F * Mth.PI) * amplitude;
            poseStack.mulPose(Axis.YP.rotationDegrees(wobble));
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        renderEmissivePass(poseStack, animatable, model, bufferSource, packedOverlay);
    }

    private void renderEmissivePass(PoseStack poseStack, EngineHoistEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, int packedOverlay) {
        poseStack.translate(0.0F, 0.01F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        for (GeoBone topBone : model.topLevelBones()) {
            renderEmissiveBone(poseStack, animatable, topBone, bufferSource, packedOverlay);
        }
    }

    private void renderEmissiveBone(PoseStack poseStack, EngineHoistEntity animatable, GeoBone bone, MultiBufferSource bufferSource, int packedOverlay) {
        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        if (bone.getName().endsWith("Emissive")) {
            DusterbikePartType partType = getPartTypeForBone(bone.getName());
            boolean visible = (partType == null) || animatable.isPartInstalled(partType);

            if (visible) {
                float r = 1f, g = 1f, b = 1f;
                boolean hasColor = false;
                if (partType != null) {
                    Integer color;
                    if (partType == DusterbikePartType.KEY) {
                        color = animatable.getPartMainColor(partType);
                    } else {
                        color = animatable.getPartGlowColor(partType);
                    }
                    if (color != null) {
                        r = ((color >> 16) & 0xFF) / 255f;
                        g = ((color >> 8) & 0xFF) / 255f;
                        b = (color & 0xFF) / 255f;
                        hasColor = true;
                    }
                }

                ResourceLocation tex;
                if (hasColor) {
                    tex = COLORED;
                } else if (animatable.hasEngine()) {
                    tex = DEFAULT;
                } else {
                    tex = OFF;
                }

                RenderType glowType = RenderTypeRegistry.getEmissiveRenderType(tex);
                VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
                super.renderCubesOfBone(poseStack, bone, glowBuffer,
                        LightTexture.FULL_BRIGHT, packedOverlay, r, g, b, 1f);
            }
        }

        for (GeoBone child : bone.getChildBones()) {
            renderEmissiveBone(poseStack, animatable, child, bufferSource, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EngineHoistEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String boneName = bone.getName();
        DusterbikePartType partType = getPartTypeForBone(boneName);

        if (partType != null && !animatable.isPartInstalled(partType)) {
            return;
        }

        boolean isEmissive = boneName.endsWith("Emissive");
        if (isEmissive) {
            for (GeoBone child : bone.getChildBones()) {
                this.renderRecursively(poseStack, animatable, child, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, alpha);
            }
            return;
        }

        float r = red, g = green, b = blue;
        if (partType != null && animatable.isPartInstalled(partType)) {
            Integer mainColor = animatable.getPartMainColor(partType);
            if (mainColor != null) {
                r *= ((mainColor >> 16) & 0xFF) / 255f;
                g *= ((mainColor >> 8) & 0xFF) / 255f;
                b *= (mainColor & 0xFF) / 255f;
            }
        }

        RenderType boneRenderType = RenderType.entityTranslucent(DEFAULT);
        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, r, g, b, alpha);

        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, boneRenderType, bufferSource, boneBuffer, isReRender, partialTick, packedLight, packedOverlay, 1f, 1f, 1f, alpha);
        }
        poseStack.popPose();
    }

    private static DusterbikePartType getPartTypeForBone(String boneName) {
        for (var entry : PART_BONES.entrySet()) {
            if (entry.getValue().contains(boneName)) return entry.getKey();
        }
        return null;
    }

    @Override
    public boolean shouldRender(EngineHoistEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}