package destiny.null_ouroboros.common;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class DusterbikeRiderAnimation {
    public static final float MAX_HEAD_YAW_DEGREES = 90.0F;
    public static final float TURN_SENSITIVITY = 0.15F;

    private static final Map<LivingEntity, Float> HEAD_OFFSETS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DusterbikeRiderAnimation() {}

    public static float getHeadOffset(LivingEntity rider) {
        return HEAD_OFFSETS.getOrDefault(rider, 0.0F);
    }

    public static void clearRider(LivingEntity rider) {
        HEAD_OFFSETS.remove(rider);
    }

    public static float clampHeadOffset(float bodyYaw, float lookYaw) {
        return Mth.clamp(
                Mth.wrapDegrees(lookYaw - bodyYaw),
                -MAX_HEAD_YAW_DEGREES,
                MAX_HEAD_YAW_DEGREES);
    }

    public static float viewYaw(LivingEntity rider, DusterbikeEntity bike, float partialTick) {
        return bike.getRenderYaw(partialTick) + getHeadOffset(rider);
    }

    public static void applyLookFromBody(LivingEntity rider, float bodyYaw, float bodyYawO) {
        float offset = getHeadOffset(rider);
        float lookYaw = bodyYaw + offset;
        rider.yBodyRotO = bodyYawO;
        rider.yBodyRot = bodyYaw;
        rider.yHeadRotO = bodyYawO + offset;
        rider.setYRot(lookYaw);
        rider.setYHeadRot(lookYaw);
    }

    public static void syncRiderToBike(LivingEntity rider, DusterbikeEntity bike) {
        if (!HEAD_OFFSETS.containsKey(rider)) {
            HEAD_OFFSETS.put(rider, clampHeadOffset(bike.getYRot(), rider.getYRot()));
        }
        applyLookFromBody(rider, bike.getYRot(), bike.yRotO);
    }

    public static double clampTurnYawDelta(double yawDelta, float headOffset) {
        float proposedOffset = headOffset + (float) yawDelta * TURN_SENSITIVITY;
        float clampedOffset = Mth.clamp(proposedOffset, -MAX_HEAD_YAW_DEGREES, MAX_HEAD_YAW_DEGREES);
        return (clampedOffset - headOffset) / TURN_SENSITIVITY;
    }

    public static void reconcileLookAfterTurn(LivingEntity rider, DusterbikeEntity bike) {
        float offset = clampHeadOffset(bike.getYRot(), rider.getYRot());
        HEAD_OFFSETS.put(rider, offset);
        applyLookFromBody(rider, bike.getYRot(), bike.yRotO);
    }
}