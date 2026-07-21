package destiny.null_ouroboros.client.render.entity.steel_leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.steel_leviathan.SteelLeviathanPartGeoModel;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanSinew;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanBehaviorState;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanMove;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public class SteelLeviathanPartGeoRenderer extends GeoEntityRenderer<SteelLeviathanPartEntity> {
    private final Map<String, float[]> deathShakeRestPositions = new HashMap<>();

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
            renderScanBeam(head, model, partialTick, poseStack, bufferSource);
        }
    }

    private void renderHologram(SteelLeviathanHeadEntity head, float partialTick, PoseStack poseStack,
                                MultiBufferSource bufferSource) {
        ItemStack stack = head.getHologramStack();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, 10.0D);
        float spin = (head.tickCount + partialTick) * 4.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(1.5F, 1.5F, 1.5F);
        MultiBufferSource eyesBuffer = type -> bufferSource.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, eyesBuffer, head.level(), head.getId());
        poseStack.popPose();
    }

    private void renderScanBeam(SteelLeviathanHeadEntity head, BakedGeoModel model, float partialTick,
                                PoseStack poseStack, MultiBufferSource bufferSource) {
        if (head.getBehaviorState() != SteelLeviathanBehaviorState.INTEREST_SCAN) {
            return;
        }
        int targetId = head.getInterestTargetId();
        if (targetId < 0) {
            return;
        }
        Entity target = head.level().getEntity(targetId);
        if (!(target instanceof Player player)) {
            return;
        }
        GeoBone bone = model.getBone(SteelLeviathanBones.SCAN_ORIGIN).orElse(null);
        if (bone == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);

        Matrix4f localMatrix = poseStack.last().pose();
        Matrix4f invLocal = new Matrix4f(localMatrix);
        invLocal.invert();

        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f originCam = localMatrix.transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));
        Vec3 originWorld = new Vec3(originCam.x, originCam.y, originCam.z).add(camPos);

        float sweep = Mth.sin((head.tickCount + partialTick) * SteelLeviathanConstants.SCAN_BEAM_SWEEP_SPEED)
                * SteelLeviathanConstants.SCAN_BEAM_SWEEP_AMP;
        Vec3 aim = player.getEyePosition(partialTick).add(0.0D, sweep, 0.0D);
        Vec3 toAim = aim.subtract(originWorld);
        double aimDist = toAim.length();
        if (aimDist < 1.0E-3D) {
            poseStack.popPose();
            return;
        }
        Vec3 dir = toAim.scale(1.0D / aimDist);
        Vec3 endWorld = aim.add(dir.scale(SteelLeviathanConstants.SCAN_BEAM_OVERSHOOT));
        Vec3 endCam = endWorld.subtract(camPos);
        Vector3f endLocal = invLocal.transformPosition(new Vector3f(
                (float) endCam.x, (float) endCam.y, (float) endCam.z));

        Vector3f beamDir = new Vector3f(endLocal);
        float length = beamDir.length();
        if (length < 1.0E-3F) {
            poseStack.popPose();
            return;
        }
        beamDir.mul(1.0F / length);

        Vector3f viewWorld = new Vector3f(
                (float) (camPos.x - originWorld.x),
                (float) (camPos.y - originWorld.y),
                (float) (camPos.z - originWorld.z));
        Vector3f localView = invLocal.transformDirection(viewWorld);
        if (localView.lengthSquared() > 1.0E-6F) {
            localView.normalize();
        } else {
            localView.set(0.0F, 1.0F, 0.0F);
        }

        Vector3f perp = new Vector3f();
        beamDir.cross(localView, perp);
        if (perp.lengthSquared() < 1.0E-4F) {
            perp.set(0.0F, 1.0F, 0.0F);
            beamDir.cross(perp, perp);
        }
        perp.normalize();

        float halfStart = SteelLeviathanConstants.SCAN_BEAM_ORIGIN_WIDTH * 0.5F;
        float halfEnd = SteelLeviathanConstants.SCAN_BEAM_END_WIDTH * 0.5F;
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypeRegistry.SCAN_BEAM_RENDER_TYPE);
        drawTaperedBeamQuad(consumer, poseStack, endLocal, perp, halfStart, halfEnd);
        poseStack.popPose();
    }

    private void drawTaperedBeamQuad(VertexConsumer consumer, PoseStack poseStack, Vector3f end,
                                     Vector3f perp, float halfStart, float halfEnd) {
        Matrix4f matrix = poseStack.last().pose();
        float sx = perp.x() * halfStart;
        float sy = perp.y() * halfStart;
        float sz = perp.z() * halfStart;
        float ex = perp.x() * halfEnd;
        float ey = perp.y() * halfEnd;
        float ez = perp.z() * halfEnd;

        int startR = 255, startG = 40, startB = 40, startA = 255;
        int endR = 255, endG = 40, endB = 40, endA = 0;

        consumer.vertex(matrix, -sx, -sy, -sz)
                .color(startR, startG, startB, startA)
                .endVertex();
        consumer.vertex(matrix, sx, sy, sz)
                .color(startR, startG, startB, startA)
                .endVertex();
        consumer.vertex(matrix, end.x + ex, end.y + ey, end.z + ez)
                .color(endR, endG, endB, endA)
                .endVertex();
        consumer.vertex(matrix, end.x - ex, end.y - ey, end.z - ez)
                .color(endR, endG, endB, endA)
                .endVertex();
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
        float bodyGearAngle = entity.getBodyGearSpinAngle();
        float mawGearAngle = entity.getMawGearSpinAngle();
        float drillAngle = entity.getDrillSpinAngle();
        boolean headThrusters = entity instanceof SteelLeviathanHeadEntity;
        float drillFlare = 0.0F;
        if (headThrusters) {
            drillFlare = ((SteelLeviathanHeadEntity) entity).getClientDrillFlare()
                    * SteelLeviathanConstants.SCAN_DRILL_FLARE_DEG * Mth.DEG_TO_RAD;
        }
        for (GeoBone top : model.topLevelBones()) {
            applyMissileThrusterVisibility(top, entity);
            applyPlumeVisibility(top, thrustersOn);
            applySpinBones(top, bodyGearAngle, mawGearAngle, drillAngle, headThrusters, drillFlare);
        }
        applyDeathShake(entity, model, partialTick);
    }

    private void applyDeathShake(SteelLeviathanPartEntity entity, BakedGeoModel model, float partialTick) {
        String rootName = switch (entity.getPartKind()) {
            case HEAD -> "head";
            case SEGMENT -> "segment";
            case TAIL -> "tail";
        };
        GeoBone bone = model.getBone(rootName).orElse(null);
        if (bone == null) {
            return;
        }
        float[] rest = deathShakeRestPositions.computeIfAbsent(rootName, k -> new float[]{
                bone.getPosX(), bone.getPosY(), bone.getPosZ()
        });
        SteelLeviathanHeadEntity head = entity instanceof SteelLeviathanHeadEntity self
                ? self
                : entity.resolveHead();
        boolean dying = head != null && head.isDying();
        if (!dying) {
            bone.setPosX(rest[0]);
            bone.setPosY(rest[1]);
            bone.setPosZ(rest[2]);
            return;
        }
        float amp = SteelLeviathanConstants.DEATH_SHAKE_MAX_OFFSET_PIXELS
                * SteelLeviathanConstants.DEATH_SHAKE_INTENSITY;
        float age = entity.tickCount + partialTick;
        int index = entity.getChainIndex();
        bone.setPosX(rest[0] + deathShakeOffset(entity.getId(), index, 0, age, amp));
        bone.setPosY(rest[1] + deathShakeOffset(entity.getId(), index, 1, age, amp));
        bone.setPosZ(rest[2] + deathShakeOffset(entity.getId(), index, 2, age, amp));
    }

    private static float deathShakeOffset(int entityId, int partIndex, int axis, float ageInTicks, float amplitude) {
        int tick = (int) Math.floor(ageInTicks);
        float partial = ageInTicks - tick;
        float current = deathShakeRandomSigned(entityId, partIndex, axis, tick);
        float next = deathShakeRandomSigned(entityId, partIndex, axis, tick + 1);
        float blended = current + (next - current) * partial;
        float wave = (float) Math.sin(ageInTicks * (1.9f + axis * 0.41f + partIndex * 0.67f)) * 0.35f;
        return (blended * 0.65f + wave) * amplitude;
    }

    private static float deathShakeRandomSigned(int entityId, int partIndex, int axis, int tick) {
        int hash = entityId * 31 + partIndex * 17 + axis * 13 + tick * 1013;
        hash ^= hash << 13;
        hash ^= hash >>> 17;
        hash ^= hash << 5;
        return (Math.floorMod(hash, 1000) / 500.0f) - 1.0f;
    }

    private void applyMissileThrusterVisibility(GeoBone bone, SteelLeviathanPartEntity entity) {
        String name = bone.getName();
        int slot = -1;
        if (entity.getPartKind() == SteelLeviathanPartEntity.PartKind.TAIL) {
            slot = SteelLeviathanBones.tailMissileIndexForBone(name);
        } else if (entity instanceof SteelLeviathanHeadEntity) {
            slot = SteelLeviathanBones.mawMissileIndexForBone(name);
        }
        if (slot >= 0) {
            if (entity.isMissileSlotReleased(slot)) {
                setBoneTreeHidden(bone, true);
            } else {
                setBoneTreeHidden(bone, false);
            }
            return;
        }
        for (GeoBone child : bone.getChildBones()) {
            applyMissileThrusterVisibility(child, entity);
        }
    }

    private void setBoneTreeHidden(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setBoneTreeHidden(child, hidden);
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

    private void applySpinBones(GeoBone bone, float bodyGearAngle, float mawGearAngle, float drillAngle,
                                boolean headThrusters, float drillFlare) {
        String name = bone.getName();
        if (SteelLeviathanBones.isBodyGearBone(name)) {
            bone.setRotX(bodyGearAngle);
        } else if (SteelLeviathanBones.isMawInternalGearBone(name)
                || SteelLeviathanBones.isMawExternalGearBone(name)) {
            bone.setRotX(mawGearAngle * SteelLeviathanBones.mawGearSpinSign(name));
        } else if (SteelLeviathanBones.isDrillBone(name)) {
            bone.setRotZ(drillAngle);
        } else if (headThrusters && SteelLeviathanBones.HEAD_THRUSTERS.contains(name)) {
            float restY = SteelLeviathanConstants.THRUSTER_REST_Y_DEG * Mth.DEG_TO_RAD;
            bone.setRotY(-restY + drillFlare);
        }
        for (GeoBone child : bone.getChildBones()) {
            applySpinBones(child, bodyGearAngle, mawGearAngle, drillAngle, headThrusters, drillFlare);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SteelLeviathanPartEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        renderBonePass(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha, false, -1);
    }

    private void renderBonePass(PoseStack poseStack, SteelLeviathanPartEntity animatable, GeoBone bone,
                                MultiBufferSource bufferSource, boolean isReRender, float partialTick,
                                int packedLight, int packedOverlay, float red, float green, float blue,
                                float alpha, boolean underEngine, int heatsinkIndex) {
        if (bone.isHidden()) {
            return;
        }

        String name = bone.getName();
        boolean nextEngine = underEngine
                || SteelLeviathanBones.isEngineBone(name)
                || SteelLeviathanBones.isPlumeBone(name);
        int boneHeatsink = SteelLeviathanBones.heatsinkIndexForBone(name);
        int nextHeatsink = boneHeatsink >= 0 ? boneHeatsink : heatsinkIndex;

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        boolean emissive = SteelLeviathanBones.isEmissiveBone(name);
        boolean blinker = SteelLeviathanBones.isBlinkerBone(name);
        if (emissive) {
            int heatsinkEmissive = SteelLeviathanBones.heatsinkIndexForEmissiveBone(name);
            int gateHeatsink = heatsinkEmissive >= 0 ? heatsinkEmissive : nextHeatsink;
            boolean heatsinkOk = gateHeatsink < 0
                    || (animatable.isHeatsinkPresent(gateHeatsink)
                    && !animatable.isHeatsinkDestroyed(gateHeatsink)
                    && !animatable.isVulnerable());
            boolean engineOk = !nextEngine || animatable.areThrustersActive();
            if (heatsinkOk && engineOk) {
                ResourceLocation texture = resolveBoneTexture(animatable, name, nextEngine);
                RenderType glowType = animatable.isVulnerable()
                        ? RenderTypeRegistry.getOpaqueEmissiveRenderType(texture)
                        : RenderType.entityTranslucentEmissive(texture);
                VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
                super.renderCubesOfBone(poseStack, bone, glowBuffer,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        } else if (blinker) {
            ResourceLocation texture = resolveBoneTexture(animatable, name, nextEngine);
            RenderType offType = RenderType.entityTranslucent(texture);
            VertexConsumer offBuffer = bufferSource.getBuffer(offType);
            super.renderCubesOfBone(poseStack, bone, offBuffer, packedLight, packedOverlay, 0.0F, 0.0F, 0.0F, 1.0F);

            float intensity = blinkerIntensity(animatable, name, partialTick);
            if (intensity > 0.01F) {
                RenderType glowType = RenderType.entityTranslucentEmissive(texture);
                VertexConsumer glowBuffer = bufferSource.getBuffer(glowType);
                super.renderCubesOfBone(poseStack, bone, glowBuffer,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, intensity, intensity, intensity, 1.0F);
            }
        } else {
            ResourceLocation texture = resolveBoneTexture(animatable, name, nextEngine);
            RenderType boneRenderType = RenderType.entityCutoutNoCull(texture);
            VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);
            float heat = animatable.getArmorShedHeat(partialTick);
            float tintR = Mth.lerp(heat, red, 1.0F);
            float tintG = Mth.lerp(heat, green, 0.42F);
            float tintB = Mth.lerp(heat, blue, 0.08F);
            super.renderCubesOfBone(poseStack, bone, boneBuffer, packedLight, packedOverlay, tintR, tintG, tintB, alpha);
        }

        for (GeoBone child : bone.getChildBones()) {
            renderBonePass(poseStack, animatable, child, bufferSource, isReRender, partialTick, packedLight,
                    packedOverlay, red, green, blue, alpha, nextEngine, nextHeatsink);
        }
        poseStack.popPose();
    }

    private float blinkerIntensity(SteelLeviathanPartEntity entity, String boneName, float partialTick) {
        SteelLeviathanHeadEntity head = entity instanceof SteelLeviathanHeadEntity self
                ? self
                : entity.resolveHead();
        if (head != null) {
            float telegraph = head.getBlinkerTelegraph();
            SteelLeviathanMove move = head.getCurrentMove();
            SteelLeviathanBehaviorState behavior = head.getBehaviorState();
            if (behavior == SteelLeviathanBehaviorState.INTEREST_SCAN) {
                return Math.abs(Mth.sin((head.tickCount + partialTick)
                        * SteelLeviathanConstants.BLINKER_SCAN_FLICKER_RATE));
            }
            if (telegraph > 0.01F
                    || move == SteelLeviathanMove.LUNGE
                    || behavior == SteelLeviathanBehaviorState.INTEREST_WAIT) {
                return telegraph;
            }
            if (move == SteelLeviathanMove.STUCK) {
                return Math.abs(Mth.sin((entity.tickCount + partialTick)
                        * SteelLeviathanConstants.BLINKER_STUCK_FLICKER_RATE));
            }
            boolean boss = behavior == SteelLeviathanBehaviorState.BOSSFIGHT;
            float waveLength = boss
                    ? SteelLeviathanConstants.BLINKER_WAVE_LENGTH_BOSS
                    : SteelLeviathanConstants.BLINKER_WAVE_LENGTH;
            float waveSpeed = boss
                    ? SteelLeviathanConstants.BLINKER_WAVE_SPEED_BOSS
                    : SteelLeviathanConstants.BLINKER_WAVE_SPEED;
            float along = entity.getChainIndex()
                    + SteelLeviathanBones.blinkerIndex(boneName) * SteelLeviathanConstants.BLINKER_SUB_INDEX_SCALE;
            float phase = along / waveLength - (entity.tickCount + partialTick) * waveSpeed;
            float wave = Mth.sin(phase * Mth.TWO_PI);
            if (wave <= 0.0F) {
                return 0.0F;
            }
            return (float) Math.pow(wave, SteelLeviathanConstants.BLINKER_WAVE_POWER);
        }

        float along = entity.getChainIndex()
                + SteelLeviathanBones.blinkerIndex(boneName) * SteelLeviathanConstants.BLINKER_SUB_INDEX_SCALE;
        float phase = along / SteelLeviathanConstants.BLINKER_WAVE_LENGTH
                - (entity.tickCount + partialTick) * SteelLeviathanConstants.BLINKER_WAVE_SPEED;
        float wave = Mth.sin(phase * Mth.TWO_PI);
        if (wave <= 0.0F) {
            return 0.0F;
        }
        return (float) Math.pow(wave, SteelLeviathanConstants.BLINKER_WAVE_POWER);
    }

    private ResourceLocation resolveBoneTexture(SteelLeviathanPartEntity entity, String boneName,
                                                boolean underEngine) {
        if (!entity.isVulnerable()) {
            int heatsink = SteelLeviathanBones.heatsinkIndexForBone(boneName);
            if (heatsink < 0) {
                heatsink = SteelLeviathanBones.heatsinkIndexForEmissiveBone(boneName);
            }
            if (heatsink >= 0 && entity.isHeatsinkDestroyed(heatsink)) {
                return SteelLeviathanConstants.TEXTURE_BROKEN_HEATSINK;
            }
        }
        if (entity.areThrustersActive()
                && (underEngine
                || SteelLeviathanBones.isEngineBone(boneName)
                || SteelLeviathanBones.isPlumeBone(boneName))) {
            return SteelLeviathanConstants.engineTexture(entity.tickCount / SteelLeviathanConstants.ENGINE_FRAME_TICKS);
        }
        return entity.isVulnerable() ? SteelLeviathanConstants.TEXTURE_VULNERABLE : SteelLeviathanConstants.TEXTURE;
    }

    @Override
    public ResourceLocation getTextureLocation(SteelLeviathanPartEntity entity) {
        return entity.isVulnerable() ? SteelLeviathanConstants.TEXTURE_VULNERABLE : SteelLeviathanConstants.TEXTURE;
    }
}
