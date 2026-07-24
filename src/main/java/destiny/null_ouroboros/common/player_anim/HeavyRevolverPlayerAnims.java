package destiny.null_ouroboros.common.player_anim;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.resources.ResourceLocation;

public final class HeavyRevolverPlayerAnims {
    public static final ResourceLocation HOLD_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_hold");
    public static final ResourceLocation SHOOT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_shoot");
    public static final ResourceLocation COCK_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cock");
    public static final ResourceLocation DECOCK_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_decock");
    public static final ResourceLocation CYLINDER_OUT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_out");
    public static final ResourceLocation CYLINDER_IN_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_in");
    public static final ResourceLocation CYLINDER_EJECT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_eject");
    public static final ResourceLocation CYLINDER_TAKE_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_take");
    public static final ResourceLocation CYLINDER_PUT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_put");
    public static final ResourceLocation CYLINDER_ROTATE_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_rotate");
    public static final ResourceLocation CYLINDER_SPEEDLOADER_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "revolver_cylinder_speedloader");

    public static final float HOLD_LENGTH_SECONDS = 0.625F;
    public static final float SHOOT_LENGTH_SECONDS = 0.5F;
    public static final float COCK_LENGTH_SECONDS = 0.5F;
    public static final float DECOCK_LENGTH_SECONDS = 0.5F;
    public static final float CYLINDER_OUT_LENGTH_SECONDS = 0.625F;
    public static final float CYLINDER_IN_LENGTH_SECONDS = 0.625F;
    public static final float CYLINDER_EJECT_LENGTH_SECONDS = 0.875F;
    public static final float CYLINDER_TAKE_LENGTH_SECONDS = 1.125F;
    public static final float CYLINDER_PUT_LENGTH_SECONDS = 1.125F;
    public static final float CYLINDER_ROTATE_LENGTH_SECONDS = 0.375F;
    public static final float CYLINDER_SPEEDLOADER_LENGTH_SECONDS = 0.75F;

    private HeavyRevolverPlayerAnims() {}

    public static void registerMeta() {
        PlayerAnimationMeta.register(HOLD_ID, HOLD_LENGTH_SECONDS);
        PlayerAnimationMeta.register(SHOOT_ID, SHOOT_LENGTH_SECONDS);
        PlayerAnimationMeta.register(COCK_ID, COCK_LENGTH_SECONDS);
        PlayerAnimationMeta.register(DECOCK_ID, DECOCK_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_OUT_ID, CYLINDER_OUT_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_IN_ID, CYLINDER_IN_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_EJECT_ID, CYLINDER_EJECT_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_TAKE_ID, CYLINDER_TAKE_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_PUT_ID, CYLINDER_PUT_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_ROTATE_ID, CYLINDER_ROTATE_LENGTH_SECONDS);
        PlayerAnimationMeta.register(CYLINDER_SPEEDLOADER_ID, CYLINDER_SPEEDLOADER_LENGTH_SECONDS);
    }

    public static PlayOptions holdOptions() {
        return baseOptions().loopMode(LoopMode.HOLD_LAST).build();
    }

    public static PlayOptions shootOptions() {
        return actionOptions();
    }

    public static PlayOptions actionOptions() {
        return baseOptions().loopMode(LoopMode.PLAY_ONCE).build();
    }

    public static PlayOptions actionOptions(boolean reverse) {
        return baseOptions().loopMode(LoopMode.PLAY_ONCE).reverse(reverse).build();
    }

    private static PlayOptions.Builder baseOptions() {
        return PlayOptions.builder()
                .override(true)
                .renderFirstPerson(true)
                .renderFirstPersonBody(false)
                .renderFirstPersonHead(false)
                .aimFollowArms(true)
                .aimFollowArmsX(true)
                .aimFollowMode(AimFollowMode.PARENT_ROTATION)
                .mirrorForLeftHanded(true);
    }

    public static boolean isRevolverAnim(ResourceLocation id) {
        return HOLD_ID.equals(id)
                || SHOOT_ID.equals(id)
                || COCK_ID.equals(id)
                || DECOCK_ID.equals(id)
                || CYLINDER_OUT_ID.equals(id)
                || CYLINDER_IN_ID.equals(id)
                || CYLINDER_EJECT_ID.equals(id)
                || CYLINDER_TAKE_ID.equals(id)
                || CYLINDER_PUT_ID.equals(id)
                || CYLINDER_ROTATE_ID.equals(id)
                || CYLINDER_SPEEDLOADER_ID.equals(id);
    }
}
