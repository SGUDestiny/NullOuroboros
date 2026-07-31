package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.DusterbikeGeoModel;
import destiny.null_ouroboros.common.dusterbike.DusterbikePistonShakeConstants;
import destiny.null_ouroboros.common.dusterbike.DusterbikeTransforms;
import destiny.null_ouroboros.common.dusterbike.DusterbikeEngineState;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.client.event.DusterbikeClientEvents;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
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
    public void render(DusterbikeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        Entity holder = entity.getLeashHolder();
        if (holder != null) {
            renderLeash(entity, partialTick, poseStack, bufferSource, holder);
        }
    }

    private void renderLeash(DusterbikeEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, Entity holder) {
        poseStack.pushPose();
        Vec3 holderPos = holder.getRopeHoldPosition(partialTick);
        double yawRad = (Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) * Mth.DEG_TO_RAD) + (Math.PI / 2.0D);
        Vec3 attach = entity.getLeashAttachOffset();
        double attachX = Math.cos(yawRad) * attach.z + Math.sin(yawRad) * attach.x;
        double attachZ = Math.sin(yawRad) * attach.z - Math.cos(yawRad) * attach.x;
        double startX = Mth.lerp(partialTick, entity.xo, entity.getX()) + attachX;
        double startY = Mth.lerp(partialTick, entity.yo, entity.getY()) + attach.y;
        double startZ = Mth.lerp(partialTick, entity.zo, entity.getZ()) + attachZ;
        poseStack.translate(attachX, attach.y, attachZ);
        float dx = (float) (holderPos.x - startX);
        float dy = (float) (holderPos.y - startY);
        float dz = (float) (holderPos.z - startZ);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        float leashWidth = Mth.invSqrt(dx * dx + dz * dz) * 0.025F / 2.0F;
        float widthZ = dz * leashWidth;
        float widthX = dx * leashWidth;
        BlockPos bikeLightPos = BlockPos.containing(entity.getEyePosition(partialTick));
        BlockPos holderLightPos = BlockPos.containing(holder.getEyePosition(partialTick));
        int blockLightBike = getBlockLightLevel(entity, bikeLightPos);
        int blockLightHolder = holder.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, holderLightPos);
        int skyLightBike = entity.level().getBrightness(LightLayer.SKY, bikeLightPos);
        int skyLightHolder = entity.level().getBrightness(LightLayer.SKY, holderLightPos);
        for (int i = 0; i <= 24; ++i) {
            addLeashVertexPair(consumer, matrix, dx, dy, dz, blockLightBike, blockLightHolder, skyLightBike, skyLightHolder, 0.025F, 0.025F, widthZ, widthX, i, false);
        }
        for (int i = 24; i >= 0; --i) {
            addLeashVertexPair(consumer, matrix, dx, dy, dz, blockLightBike, blockLightHolder, skyLightBike, skyLightHolder, 0.025F, 0.0F, widthZ, widthX, i, true);
        }
        poseStack.popPose();
    }

    private static void addLeashVertexPair(VertexConsumer consumer, Matrix4f matrix, float dx, float dy, float dz,
                                           int blockLightStart, int blockLightEnd, int skyLightStart, int skyLightEnd,
                                           float leashWidth, float leashWidthOffset, float widthZ, float widthX, int segment, boolean reverse) {
        float t = (float) segment / 24.0F;
        int blockLight = (int) Mth.lerp(t, (float) blockLightStart, (float) blockLightEnd);
        int skyLight = (int) Mth.lerp(t, (float) skyLightStart, (float) skyLightEnd);
        int light = LightTexture.pack(blockLight, skyLight);
        float shade = segment % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.5F * shade;
        float g = 0.4F * shade;
        float b = 0.3F * shade;
        float x = dx * t;
        float y = dy > 0.0F ? dy * t * t : dy - dy * (1.0F - t) * (1.0F - t);
        float z = dz * t;
        consumer.vertex(matrix, x - widthZ, y + leashWidthOffset, z + widthX).color(r, g, b, 1.0F).uv2(light).endVertex();
        consumer.vertex(matrix, x + widthZ, y + leashWidth - leashWidthOffset, z - widthX).color(r, g, b, 1.0F).uv2(light).endVertex();
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

        long gameTime = animatable.level().getGameTime();
        float timeSinceHit = (gameTime - animatable.getLastDamageTick()) + partialTick;
        if (timeSinceHit >= 0.0F && timeSinceHit < 5.0F) {
            float damageRatio = 1.0F - (animatable.getFrameHealth() / (float) DusterbikeEngineState.FRAME_MAX_HEALTH);
            float amplitude = 4.0F + damageRatio * 8.0F;
            float wobble = Mth.sin(timeSinceHit / 1.5F * Mth.PI) * amplitude;
            poseStack.mulPose(Axis.YP.rotationDegrees(wobble));
        }

        applyDynamicBonePoses(animatable, partialTick);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(0.0F, 0.01F, 0.0F);
        renderActiveEmissives(poseStack, animatable, model, bufferSource, partialTick, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, DusterbikeEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        String boneName = bone.getName();
        DusterbikePartType partType = getPartTypeForBone(boneName);
        boolean isEmissive = boneName.endsWith("Emissive");

        boolean isLightSolid = !isEmissive &&
                (partType == DusterbikePartType.FRONT_LIGHT || partType == DusterbikePartType.REAR_LIGHT);

        boolean partInstalled = partType == null || animatable.isPartInstalled(partType);

        if (partType != null && partType.isRemovable() && !partInstalled && isEmissive &&
                (partType == DusterbikePartType.FRONT_LIGHT || partType == DusterbikePartType.REAR_LIGHT)) {
            ResourceLocation tex = DEFAULT_OFF;
            float r = 0f, g = 0f, b = 0f;

            RenderType boneRenderType = RenderType.entityTranslucent(tex);
            VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

            poseStack.pushPose();
            RenderUtils.translateMatrixToBone(poseStack, bone);
            RenderUtils.translateToPivotPoint(poseStack, bone);
            RenderUtils.rotateMatrixAroundBone(poseStack, bone);
            RenderUtils.scaleMatrixForBone(poseStack, bone);
            RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

            super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, r, g, b, alpha);
            poseStack.popPose();
            return;
        }

        if (partType != null && !partInstalled && !isLightSolid) {
            return;
        }

        ResourceLocation texture;
        float r = red, g = green, b = blue;

        if (partType != null && (partInstalled || isLightSolid)) {
            boolean lit = (partType == DusterbikePartType.FRONT_LIGHT || partType == DusterbikePartType.REAR_LIGHT)
                    && isLightBoneLit(animatable, boneName);
            boolean useOnTexture = (partType == DusterbikePartType.FRONT_LIGHT || partType == DusterbikePartType.REAR_LIGHT)
                    ? lit
                    : animatable.isEngineRunning();
            if (isEmissive) {
                Integer glowColor = animatable.getPartGlowColor(partType);
                if (partType == DusterbikePartType.KEY) {
                    glowColor = animatable.getPartMainColor(partType);
                }

                r = 1.0f; g = 1.0f; b = 1.0f;
                texture = useOnTexture ? (glowColor != null ? COLORED_ON  : DEFAULT_ON)
                        : (glowColor != null ? COLORED_OFF : DEFAULT_OFF);
                if (glowColor != null) {
                    r *= ((glowColor >> 16) & 0xFF) / 255f;
                    g *= ((glowColor >> 8) & 0xFF) / 255f;
                    b *= (glowColor & 0xFF) / 255f;
                }
            } else {
                Integer mainColor = animatable.getPartMainColor(partType);
                texture = useOnTexture ? (mainColor != null ? COLORED_ON  : DEFAULT_ON)
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

                        if (partType == DusterbikePartType.KEY) {
                            glowColor = entity.getPartMainColor(partType);
                        }

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

    private static boolean isLightBoneLit(DusterbikeEntity entity, String boneName) {
        if (boneName.equals("Headlight") || boneName.equals("HeadlightEmissive")) {
            return entity.areHeadlightsOn();
        }
        if (boneName.startsWith("FrontBlinkerLeft") || boneName.startsWith("RearBlinkerLeft")) {
            return entity.isLeftBlinkerLit();
        }
        if (boneName.startsWith("FrontBlinkerRight") || boneName.startsWith("RearBlinkerRight")) {
            return entity.isRightBlinkerLit();
        }
        if (boneName.startsWith("RearStopLight")) {
            return entity.isStopLightLit();
        }
        return false;
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

        if (DusterbikeClientEvents.isKeyCrankVisualActive(entity)) {
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