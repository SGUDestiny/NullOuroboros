package destiny.null_ouroboros.common.player_anim;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.resources.ResourceLocation;

public final class HeavyRevolverPlayerAnims {
    public static final ResourceLocation HOLD_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_hold");
    public static final ResourceLocation SHOOT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_shoot");

    public static final float HOLD_LENGTH_SECONDS = 0.625F;
    public static final float SHOOT_LENGTH_SECONDS = 0.5F;

    private HeavyRevolverPlayerAnims() {}

    public static void registerMeta() {
        PlayerAnimationMeta.register(HOLD_ID, HOLD_LENGTH_SECONDS);
        PlayerAnimationMeta.register(SHOOT_ID, SHOOT_LENGTH_SECONDS);
    }

    public static PlayOptions holdOptions() {
        return baseOptions().loopMode(LoopMode.HOLD_LAST).build();
    }

    public static PlayOptions shootOptions() {
        return baseOptions().loopMode(LoopMode.HOLD_LAST).build();
    }

    private static PlayOptions.Builder baseOptions() {
        return PlayOptions.builder()
                .override(true)
                .renderFirstPerson(true)
                .renderFirstPersonBody(true)
                .renderFirstPersonHead(false)
                .aimFollowArms(true)
                .aimFollowArmsX(true)
                .aimFollowMode(AimFollowMode.PARENT_ROTATION)
                .mirrorForLeftHanded(true);
    }

    public static boolean isRevolverAnim(ResourceLocation id) {
        return HOLD_ID.equals(id) || SHOOT_ID.equals(id);
    }
}
