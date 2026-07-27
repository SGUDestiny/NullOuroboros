package destiny.null_ouroboros.common.player_anim;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.respirator.FilterAction;
import net.minecraft.resources.ResourceLocation;

public final class RespiratorPlayerAnims {
    public static final ResourceLocation FILTER_REMOVE_RIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "filter_remove_right");
    public static final ResourceLocation FILTER_PUT_RIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "filter_put_right");
    public static final ResourceLocation FILTER_REMOVE_LEFT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "filter_remove_left");
    public static final ResourceLocation FILTER_PUT_LEFT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "filter_put_left");

    public static final float LENGTH_SECONDS = 2.25F;
    public static final int SOUND_DELAY_TICKS = 10;
    public static final int COMMIT_DELAY_TICKS = 35;

    private RespiratorPlayerAnims() {
    }

    public static void registerMeta() {
        PlayerAnimationMeta.register(FILTER_REMOVE_RIGHT_ID, LENGTH_SECONDS);
        PlayerAnimationMeta.register(FILTER_PUT_RIGHT_ID, LENGTH_SECONDS);
        PlayerAnimationMeta.register(FILTER_REMOVE_LEFT_ID, LENGTH_SECONDS);
        PlayerAnimationMeta.register(FILTER_PUT_LEFT_ID, LENGTH_SECONDS);
    }

    public static PlayOptions actionOptions() {
        return PlayOptions.builder()
                .loopMode(LoopMode.PLAY_ONCE)
                .override(true)
                .renderFirstPerson(true)
                .renderFirstPersonBody(false)
                .renderFirstPersonHead(false)
                .aimFollowArms(true)
                .aimFollowArmsX(true)
                .aimFollowMode(AimFollowMode.PARENT_ROTATION)
                .mirrorForLeftHanded(true)
                .build();
    }

    public static ResourceLocation animationId(FilterAction action) {
        return switch (action) {
            case REMOVE_RIGHT -> FILTER_REMOVE_RIGHT_ID;
            case REMOVE_LEFT -> FILTER_REMOVE_LEFT_ID;
            case PUT -> FILTER_PUT_RIGHT_ID;
        };
    }

    public static boolean isRespiratorAnim(ResourceLocation id) {
        return FILTER_REMOVE_RIGHT_ID.equals(id)
                || FILTER_PUT_RIGHT_ID.equals(id)
                || FILTER_REMOVE_LEFT_ID.equals(id)
                || FILTER_PUT_LEFT_ID.equals(id);
    }
}
