package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.DusterbikeGeoModel;
import destiny.null_ouroboros.common.DusterbikePistonShakeConstants;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.event.ClientForgeEvents;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.*;

public class DusterbikeGeoRenderer extends GeoEntityRenderer<DusterbikeEntity> {
    private static final ResourceLocation COLORED_ON  = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored.png");
    private static final ResourceLocation COLORED_OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored_off.png");
    private static final ResourceLocation DEFAULT_ON  = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");
    private static final ResourceLocation DEFAULT_OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_off.png");

    private final Map<String, float[]> pistonRestPositions = new HashMap<>();

    private static final Map<DusterbikePartType, List<String>> PART_BONES = Map.ofEntries(
            Map.entry(DusterbikePartType.FRAME, List.of(
                    "Body",
                    "SuspensionFront",
                    "SuspensionRear",
                    "CoverFront",
                    "CoverRear",
                    "CoverChain",
                    "Exhaust",
                    "ExhaustUpper",
                    "ExhaustLower",
                    "Piping",
                    "Support",
                    "HandleRight",
                    "HandleLeft",
                    "SpeedGauge",
                    "FuelGauge",

                    "SpeedGaugeEmissive",
                    "FuelGaugeEmissive",
                    "HandleRightEmissive",
                    "HandleLeftEmissive",
                    "SuspensionFrontEmissive",
                    "SuspensionRearEmissive",
                    "CoverChainEmissive",
                    "SupportEmissive",
                    "SpeedGaugeArrowEmissive",
                    "FuelGaugeArrowEmissive")),
            Map.entry(DusterbikePartType.FRONT_WHEEL, List.of(
                    "WheelFront",
                    "WheelFrontEmissive"
            )),
            Map.entry(DusterbikePartType.REAR_WHEEL, List.of(
                    "WheelRear",
                    "WheelRearEmissive"
            )),
            Map.entry(DusterbikePartType.FRONT_LIGHT, List.of(
                    "Headlight",
                    "HeadlightEmissive",
                    "FrontBlinkerLeft",
                    "FrontBlinkerLeftEmissive",
                    "FrontBlinkerRight",
                    "FrontBlinkerRightEmissive"
            )),
            Map.entry(DusterbikePartType.REAR_LIGHT, List.of(
                    "RearStopLight",
                    "RearStopLightEmissive",
                    "RearBlinkerLeft",
                    "RearBlinkerLeftEmissive",
                    "RearBlinkerRight",
                    "RearBlinkerRightEmissive"
            )),
            Map.entry(DusterbikePartType.BATTERY, List.of(
                    "Battery",
                    "BatteryEmissive"
            )),
            Map.entry(DusterbikePartType.ENGINE, List.of(
                    "Engine"
            )),
            Map.entry(DusterbikePartType.PISTON_FRONT, List.of(
                    "PistonFront",
                    "PistonFrontEmissive"
            )),
            Map.entry(DusterbikePartType.PISTON_REAR, List.of(
                    "PistonRear",
                    "PistonRearEmissive"
            )),
            Map.entry(DusterbikePartType.KEY, List.of(
                    "Key",
                    "KeyEmissive"
            ))
    );

    public DusterbikeGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DusterbikeGeoModel());
        this.shadowRadius = 0.75F;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, DusterbikeEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        float yaw = animatable.getRenderYaw(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        float pitch = animatable.getRenderPitch(partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        float roll = animatable.getRenderRoll(partialTick);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));

        applyDynamicBonePoses(animatable, partialTick);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(0.0F, 0.01F, 0.0F);
        renderActiveEmissives(poseStack, animatable, model, bufferSource, partialTick, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, DusterbikeEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        String boneName = bone.getName();
        DusterbikePartType partType = getPartTypeForBone(boneName);
        if (partType != null && partType.isRemovable() && !animatable.isPartInstalled(partType)) {
            return;
        }

        boolean isEmissive = boneName.endsWith("Emissive");

        ResourceLocation texture;
        float r = red, g = green, b = blue;

        if (partType != null) {
            if (isEmissive) {
                Integer glowColor = animatable.getPartGlowColor(partType);

                r = 1.0f; g = 1.0f; b = 1.0f;

                texture = animatable.isEngineRunning() ? (glowColor != null ? COLORED_ON  : DEFAULT_ON)
                        : (glowColor != null ? COLORED_OFF : DEFAULT_OFF);
                if (glowColor != null) {
                    r *= ((glowColor >> 16) & 0xFF) / 255f;
                    g *= ((glowColor >> 8) & 0xFF) / 255f;
                    b *= (glowColor & 0xFF) / 255f;
                }
            } else {
                Integer mainColor = animatable.getPartMainColor(partType);
                texture = animatable.isEngineRunning() ? (mainColor != null ? COLORED_ON  : DEFAULT_ON)
                        : (mainColor != null ? COLORED_OFF : DEFAULT_OFF);
                if (mainColor != null) {
                    r *= ((mainColor >> 16) & 0xFF) / 255f;
                    g *= ((mainColor >> 8) & 0xFF) / 255f;
                    b *= (mainColor & 0xFF) / 255f;
                }
            }
        } else {
            texture = animatable.isEngineRunning() ? DEFAULT_ON : DEFAULT_OFF;
        }

        RenderType boneRenderType = RenderType.entityTranslucent(texture);
        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);


        super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, r, g, b, alpha);

        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, boneRenderType, bufferSource,
                    boneBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    1.0f, 1.0f, 1.0f, alpha);
        }
        poseStack.popPose();
    }

    private void renderActiveEmissives(PoseStack poseStack, DusterbikeEntity entity, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedOverlay) {
        for (GeoBone topBone : bakedModel.topLevelBones()) {
            renderActiveEmissiveFromBone(poseStack, entity, topBone, bufferSource, partialTick, packedOverlay);
        }
    }

    private void renderActiveEmissiveFromBone(PoseStack poseStack, DusterbikeEntity entity, GeoBone bone, MultiBufferSource bufferSource, float partialTick, int packedOverlay) {
        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        if (bone.getName().endsWith("Emissive")) {
            DusterbikePartType partType = getPartTypeForBone(bone.getName());
            if (partType == null || !partType.isRemovable() || entity.isPartInstalled(partType)) {
                if (shouldRenderActiveEmissive(entity, bone.getName())) {
                    boolean isLight = (partType == DusterbikePartType.FRONT_LIGHT || partType == DusterbikePartType.REAR_LIGHT);
                    boolean useOnTexture = isLight || entity.isEngineRunning();

                    ResourceLocation tex = useOnTexture ? DEFAULT_ON : DEFAULT_OFF;
                    float r = 1f, g = 1f, b = 1f;

                    if (partType != null) {
                        Integer glowColor = entity.getPartGlowColor(partType);
                        if (glowColor != null) {
                            tex = useOnTexture ? COLORED_ON : COLORED_OFF;
                            r = ((glowColor >> 16) & 0xFF) / 255f;
                            g = ((glowColor >> 8) & 0xFF) / 255f;
                            b = (glowColor & 0xFF) / 255f;
                        }
                    }

                    RenderType glowType = RenderTypeRegistry.entityTranslucentEmissive(tex);
                    VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
                    super.renderCubesOfBone(poseStack, bone, glowBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
                }
            }
        }

        for (GeoBone child : bone.getChildBones()) {
            renderActiveEmissiveFromBone(poseStack, entity, child, bufferSource, partialTick, packedOverlay);
        }
        poseStack.popPose();
    }

    private static boolean shouldRenderActiveEmissive(DusterbikeEntity entity, String boneName) {
        if (boneName.equals("HeadlightEmissive")) {
            return entity.areHeadlightsOn();
        }
        if (boneName.equals("FrontBlinkerLeftEmissive") || boneName.equals("RearBlinkerLeftEmissive")) {
            return entity.isLeftBlinkerLit();
        }
        if (boneName.equals("FrontBlinkerRightEmissive") || boneName.equals("RearBlinkerRightEmissive")) {
            return entity.isRightBlinkerLit();
        }
        if (boneName.equals("RearStopLightEmissive")) {
            return entity.isStopLightLit();
        }

        return entity.isEngineRunning();
    }

    private static DusterbikePartType getPartTypeForBone(String boneName) {
        for (var entry : PART_BONES.entrySet())
            if (entry.getValue().contains(boneName)) return entry.getKey();
        return null;
    }

    private void applyDynamicBonePoses(DusterbikeEntity entity, float partialTick) {
        setBoneRotation("Front", 0, -entity.getRenderSteer(partialTick) * (float)Math.PI / 180f, 0);
        float frontRot = entity.getFrontWheelRotation(partialTick);
        float rearRot  = entity.getRearWheelRotation(partialTick);
        setBoneRotation("WheelFront", -frontRot, 0, 0);
        setBoneRotation("WheelRear",  -rearRot,  0, 0);
        boolean deployed = entity.getPassengers().isEmpty();
        setBoneRotation("Support", deployed ? 0 : -90 * (float)Math.PI / 180f, 0, 0);

        if (ClientForgeEvents.isKeyCrankVisualActive(entity)) {
            setBoneRotation("Key", -DusterbikeTransforms.KEY_CRANK_ANGLE_DEGREES * (float)Math.PI / 180f, 0, 0);
        } else {
            setBoneRotation("Key", 0, 0, 0);
        }

        float intensity = DusterbikePistonShakeManager.getShakeIntensity(entity);
        if (intensity > 0) {
            applyPistonShake("PistonRear", entity, intensity, 0, partialTick);
            applyPistonShake("PistonFront", entity, intensity, 1, partialTick);
        }

        float speedDeg = DusterbikePistonShakeManager.getSpeedGaugeArrowDegrees(entity, partialTick);
        setBoneRotation("SpeedGaugeArrow", 0, -speedDeg * (float)Math.PI / 180f, 0);
        setBoneRotation("FuelGaugeArrow", 0, -(entity.getFuelRatio() * 160f - 80f) * (float)Math.PI / 180f, 0);
    }

    private void setBoneRotation(String name, float x, float y, float z) {
        getGeoModel().getBone(name).ifPresent(b -> { b.setRotX(x); b.setRotY(y); b.setRotZ(z); });
    }

    private void applyPistonShake(String boneName, DusterbikeEntity entity, float intensity, int index, float partialTick) {
        getGeoModel().getBone(boneName).ifPresent(bone -> {
            pistonRestPositions.computeIfAbsent(boneName, k -> new float[]{
                    bone.getPosX(), bone.getPosY(), bone.getPosZ()
            });

            float[] rest = pistonRestPositions.get(boneName);
            float amp = DusterbikePistonShakeConstants.MAX_OFFSET_PIXELS * intensity;
            bone.setPosX(rest[0] + shakeOffset(entity.getId(), index, 0, partialTick, amp));
            bone.setPosY(rest[1] + shakeOffset(entity.getId(), index, 1, partialTick, amp));
            bone.setPosZ(rest[2] + shakeOffset(entity.getId(), index, 2, partialTick, amp));
        });
    }

    private static float shakeOffset(int entityId, int pistonIndex, int axis, float ageInTicks, float amplitude) {
        int tick = (int)Math.floor(ageInTicks);
        float partial = ageInTicks - tick;
        float current = randomSigned(entityId, pistonIndex, axis, tick);
        float next = randomSigned(entityId, pistonIndex, axis, tick + 1);
        float blended = current + (next - current) * partial;
        float wave = (float)Math.sin(ageInTicks * (1.9f + axis * 0.41f + pistonIndex * 0.67f)) * 0.35f;
        return (blended * 0.65f + wave) * amplitude;
    }

    private static float randomSigned(int entityId, int pistonIndex, int axis, int tick) {
        int hash = entityId * 31 + pistonIndex * 17 + axis * 13 + tick * 1013;
        hash ^= hash << 13;
        hash ^= hash >>> 17;
        hash ^= hash << 5;
        return (Math.floorMod(hash, 1000) / 500.0f) - 1.0f;
    }
}