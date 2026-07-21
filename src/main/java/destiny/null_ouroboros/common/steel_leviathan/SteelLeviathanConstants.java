package destiny.null_ouroboros.common.steel_leviathan;

import net.minecraft.resources.ResourceLocation;
import destiny.null_ouroboros.NullOuroboros;

public final class SteelLeviathanConstants {
    private SteelLeviathanConstants() {}

    public static final float MAX_HEALTH = 500.0F;
    public static final float ANTIBUTCHER_THRESHOLD = 100.0F;
    public static final float HEATSINK_MAX_HP = 5.0F;
    public static final int MAX_HEATSINKS = 4;

    public static final float SOUND_VOLUME_64 = 4.0F;
    public static final float SOUND_VOLUME_128 = 8.0F;
    public static final float SOUND_VOLUME_256 = 16.0F;

    public static final int ARMOR_SHED_TELEGRAPH_TICKS = 20;

    public static final float ARMOR_SHED_DAMAGE_FRACTION = 0.1F;

    public static final int MIN_SEGMENTS = 16;
    public static final int MAX_SEGMENTS = 32;

    public static final int NATURAL_SPAWN_CAP = 3;
    public static final int NATURAL_SPAWN_CHANCE = 32;
    public static final double SIGHT_ADVANCEMENT_RING_WIDTH = 32.0D;
    public static final int SIGHT_ADVANCEMENT_CHECK_INTERVAL = 20;
    public static final float NATURAL_SPAWN_YAW_AMPLITUDE = 35.0F;
    public static final float NATURAL_SPAWN_PITCH_AMPLITUDE = 25.0F;
    public static final float NATURAL_SPAWN_CURVE_FREQUENCY = 0.45F;

    public static final float BACK_CONNECTION_Z_MODEL = 64.0F;
    public static final float BACK_CONNECTION_Z = BACK_CONNECTION_Z_MODEL / 16.0F;

    public static final float SEGMENT_SPACING = Math.abs(BACK_CONNECTION_Z);

    public static final float SEGMENT_WIDTH = 4.5F;
    public static final float SEGMENT_HEIGHT = 4.5F;

    public static final float SEGMENT_COLLIDER_FORWARD = 2.0F;

    public static final double SURFACE_GROUND_OFFSET = 2.5D;

    public static final double BURROW_GROUND_DEPTH = 1.75D;

    public static final float GROUND_GUIDE_MAX_STEP = 0.06F;

    public static final float BOB_Y_FOLLOW = 0.16F;

    public static final float BURROW_SPRING_GAIN = 0.08F;
    public static final float SURFACE_SPRING_GAIN = 0.04F;
    public static final double BURROW_SPRING_DEADZONE = 0.5D;

    public static final int WANDER_HEADING_INTERVAL_MIN_TICKS = 80;
    public static final int WANDER_HEADING_INTERVAL_MAX_TICKS = 200;

    public static final float WANDER_YAW_JITTER_DEGREES = 45.0F;
    public static final double CRUISE_SPEED = 0.35D;

    public static final double GRAVITY_ACCEL = 0.04D;
    public static final double GRAVITY_MAX_FALL = 0.55D;
    public static final float GRAVITY_NOSE_DOWN_PITCH = 70.0F;
    public static final float GRAVITY_PITCH_RATE = 4.0F;

    public static final float MAX_SEGMENT_BEND = 35.0F;

    public static final float SEGMENT_TURN_RATE = 5.0F;

    public static final float SEGMENT_Y_FOLLOW = 0.12F;

    public static final float HEAD_TURN_RATE_PASSIVE = 3.0F;

    public static final float HEAD_TURN_RATE_COMBAT = 5.5F;

    public static final float BOB_SPEED = 0.022F;
    public static final double BOB_BURROW_DEEP = 12.0D;
    public static final double BOB_SURFACE_PEEK = 8.0D;

    public static final float BOB_SURFACE_THRESHOLD = 0.25F;

    public static final float BOB_PITCH_AMPLITUDE = 55.0F;

    public static final float HEAD_WIDTH = 5.0F;
    public static final float HEAD_HEIGHT = 5.0F;
    public static final float TAIL_WIDTH = 5.0F;
    public static final float TAIL_HEIGHT = 5.0F;
    public static final float HEATSINK_SIZE = 1.5F;

    public static final double HEATSINK_HITSCAN_PIERCE = 3.0D;

    public static final double HEATSINK_HITSCAN_REACH = 8.0D;

    public static final float HEAD_CONTACT_DAMAGE = 1.0F;
    public static final int HEAD_CONTACT_INTERVAL = 1;
    public static final float BODY_CONTACT_DAMAGE = 2.0F;
    public static final int BODY_CONTACT_INTERVAL = 20;
    public static final float TAIL_CONTACT_DAMAGE = 5.0F;
    public static final int TAIL_CONTACT_INTERVAL = 20;

    public static final float MOVE_HIT_DAMAGE = 10.0F;
    public static final int MOVE_HIT_COOLDOWN_TICKS = 60;

    public static final int INTEREST_WAIT_TICKS = 15 * 20;
    public static final double INTEREST_DETECT_RANGE = 64.0D;
    public static final float INTEREST_DETECT_FOV_DEG = 160.0F;
    public static final int INTEREST_SCAN_TICKS = 5 * 20;
    public static final float SCAN_DRILL_FLARE_DEG = 15.0F;
    public static final float SCAN_DRILL_FLARE_RATE = 0.1F;
    public static final float THRUSTER_REST_Y_DEG = 22.5F;
    public static final float SCAN_BEAM_ORIGIN_WIDTH = 4.0F / 16.0F;
    public static final float SCAN_BEAM_END_WIDTH = 2.0F;
    public static final double SCAN_BEAM_OVERSHOOT = 1.5D;
    public static final float SCAN_BEAM_SWEEP_AMP = 2.0F;
    public static final float SCAN_BEAM_SWEEP_SPEED = 0.25F;

    public static final double INTEREST_STAND_DISTANCE = 16.0D;
    public static final double INTEREST_WALKAWAY_BLOCKS = 28.0D;

    public static final int INTEREST_GRACE_TICKS = 40;

    public static final int INTEREST_APPROACH_BURROW_TICKS = 120;

    public static final int INTEREST_APPROACH_EMERGE_TICKS = 40;

    public static final int INTEREST_APPROACH_ARC_TICKS = 50;

    public static final int INTEREST_APPROACH_RISE_TICKS =
            INTEREST_APPROACH_EMERGE_TICKS + INTEREST_APPROACH_ARC_TICKS;

    public static final double INTEREST_ARC_RADIUS = 8.0D;

    public static final double INTEREST_BREACH_ARRIVAL = 2.5D;

    public static final double INTEREST_APPROACH_BURROW_DEPTH = 9.0D;

    public static final double INTEREST_DEPART_BURROW_DEPTH = 9.0D;

    public static final int INTEREST_APPROACH_DIVE_TICKS = 95;

    public static final int INTEREST_APPROACH_DIVE_MIN_TICKS = 60;

    public static final float INTEREST_DIVE_SPRING_GAIN = 0.045F;

    public static final float INTEREST_BURROW_CRUISE_PITCH = 20.0F;

    public static final double INTEREST_BURROW_CRUISE_SPEED = 0.65D;

    public static final double INTEREST_HOLD_HEIGHT = 12.0D;

    public static final float INTEREST_CREST_LIFT_GAIN = 0.12F;
    public static final float INTEREST_HOLD_LIFT_GAIN = 0.12F;

    public static final float INTEREST_YAW_RATE = 2.0F;
    public static final float INTEREST_PITCH_RATE = 2.0F;

    public static final float LOOK_TRACK_YAW_RATE = 10.0F;
    public static final float LOOK_TRACK_PITCH_RATE = 8.0F;

    public static final double INTEREST_MOVE_SPEED = CRUISE_SPEED;

    public static final float INTEREST_LOOK_PITCH_MAX = 55.0F;

    public static final float INTEREST_BURROW_DIVE_PITCH = 85.0F;

    public static final float INTEREST_SURFACE_NOSE_UP_PITCH = -85.0F;

    public static final int INTEREST_POST_FEED_BURROW_TICKS = 60;
    public static final float INTEREST_DEPART_SPEED = (float) INTEREST_MOVE_SPEED;
    public static final float INTEREST_DEPART_SPRING_GAIN = 0.2F;
    public static final double INTEREST_CONSUME_MIN_SPEED = 0.15D;
    public static final int INTEREST_CONSUME_TIMEOUT_TICKS = 10 * 20;
    public static final int INTEREST_COOLDOWN_MIN_TICKS = 60 * 20;
    public static final int INTEREST_COOLDOWN_MAX_TICKS = 5 * 60 * 20;
    public static final int REP_GIFT_THRESHOLD = 8;
    public static final int REP_PACIFIST_THRESHOLD = 16;
    public static final int COOLDOWN_REDUCTION_PER_REP = 10 * 20;

    public static final int MOVE_BUDGET = 3;
    public static final int STUCK_TICKS = 10 * 20;

    public static final int STUCK_TICKS_PHASE_TWO = 5 * 20;

    public static final float STUCK_PITCH = 22.5F;
    public static final int STAND_OPEN_TICKS = 5 * 20;
    public static final float STAND_LUNGE_CHANCE = 0.75F;
    public static final int BURST_TELEGRAPH_TICKS = 3 * 20;
    public static final int BURST_EMERGE_TICKS = 40;
    public static final int BURST_CREST_HOLD_TICKS = 8;
    public static final int BURST_DIVE_TICKS = 30;
    public static final double BURST_CREST_HEIGHT = 8.0D;

    public static final double BURST_CREST_HEIGHT_PHASE_TWO = 14.0D;

    public static final double BURST_ARC_RADIUS = 11.0D;

    public static final int BURST_TELEGRAPH_MAX_DROP = 8;

    public static final int LUNGE_WINDUP_TICKS = 3 * 20;

    public static final int LUNGE_WINDUP_TICKS_PHASE_TWO = 2 * 20;
    public static final int LUNGE_CHARGE_TICKS = 20;
    public static final int LAUNCH_RISE_TICKS = 46;
    public static final int LAUNCH_DIVE_TICKS = 40;

    public static final double LAUNCH_APEX_HEIGHT = 52.0D;

    public static final double LAUNCH_ARC_RADIUS = 18.0D;

    public static final double LAUNCH_BURROW_DEPTH = 56.0D;

    public static final int LAUNCH_BURROW_DIVE_MIN_TICKS = 70;
    public static final int CIRCLE_ORBIT_TICKS = 80;
    public static final int CIRCLE_CHARGE_TICKS = 40;
    public static final int CIRCLE_BOB_HALF_TICKS = 30;
    public static final double CIRCLE_BOB_DEEP = 6.0D;
    public static final double CIRCLE_BOB_PEEK = 3.0D;
    public static final float CIRCLE_BOB_Y_FOLLOW = 0.5F;

    public static final float CIRCLE_BOB_SPEED_MULT = 2.5F;

    public static final double CIRCLE_ORBIT_SPEED = 0.7D;

    public static final double BOSS_BURROW_DEPTH = 20.0D;

    public static final float STUCK_ORBIT_BLEND_RATE = 0.08F;
    public static final int PUMMEL_CYCLE_TICKS = 20;

    public static final int PUMMEL_STRIKES = 3;
    public static final int BURROW_DIVE_MIN_TICKS = 25;
    public static final int BURROW_CRUISE_TICKS = 40;

    public static final int BURROW_CRUISE_TICKS_BOSS = 12;
    public static final double BURROW_DIVE_SPEED = 0.65D;
    public static final double LUNGE_CHARGE_SPEED = 1.4D;
    public static final double LAUNCH_RISE_SPEED = 0.8D;
    public static final double LAUNCH_DIVE_SPEED = 1.2D;
    public static final double CIRCLE_CHARGE_SPEED = 1.3D;

    public static final float BOSS_MOTION_SCALE = 2.0F;

    public static final float BOSS_UNDERGROUND_YAW_RATE = HEAD_TURN_RATE_COMBAT * BOSS_MOTION_SCALE;

    public static final int STAND_APPROACH_CRUISE_FAILSAFE_TICKS = 120;
    public static final double BOSS_ESCAPE_RANGE = 128.0D;

    public static final float STAND_APPROACH_TIME_SCALE = 0.25F;
    public static final int MISSILE_LAUNCH_INTERVAL = 20;

    public static final float DEATH_SHAKE_MAX_OFFSET_PIXELS = 4.0F;
    public static final float DEATH_SHAKE_INTENSITY = 1.0F;

    public static final int DEATH_BLOOD_COUNT = 80;
    public static final double DEATH_BLOOD_SPREAD = 2.0D;
    public static final double DEATH_BLOOD_SPEED = 0.55D;

    public static final ResourceLocation DEATH_LOOT_TABLE_HEAD =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "entities/steel_leviathan_head");
    public static final ResourceLocation DEATH_LOOT_TABLE_SEGMENT =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "entities/steel_leviathan_segment");
    public static final ResourceLocation DEATH_LOOT_TABLE_TAIL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "entities/steel_leviathan_tail");
    public static final double DEATH_LOOT_SPREAD = 1.25D;
    public static final double DEATH_LOOT_SPEED = 0.45D;

    public static final int BURST_RADIUS = 7;
    public static final int STAND_RADIUS_MIN = 16;
    public static final int STAND_RADIUS_MAX = 32;
    public static final int CIRCLE_RADIUS_MIN = 32;
    public static final int CIRCLE_RADIUS_MAX = 64;

    public static final float BLINKER_WAVE_LENGTH = 7.0F;
    public static final float BLINKER_WAVE_SPEED = 0.06F;
    public static final float BLINKER_WAVE_LENGTH_BOSS = 4.5F;
    public static final float BLINKER_WAVE_SPEED_BOSS = 0.12F;
    public static final float BLINKER_WAVE_POWER = 2.0F;
    public static final float BLINKER_SUB_INDEX_SCALE = 0.15F;
    public static final float BLINKER_STUCK_FLICKER_RATE = 0.55F;
    public static final float BLINKER_SCAN_FLICKER_RATE = 0.55F;

    public static final int ENGINE_FRAME_TICKS = 2;
    public static final int THRUSTER_FRAME_COUNT = 3;

    public static final float BODY_GEAR_SPIN_PER_BLOCK = -1F;
    public static final float MAW_GEAR_SPIN_PER_TICK = 0.18F;
    public static final float DRILL_SPIN_PER_BLOCK = 1F;

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan.png");
    public static final ResourceLocation TEXTURE_VULNERABLE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan_vulnerable.png");
    public static final ResourceLocation TEXTURE_BROKEN_HEATSINK =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan_broken_heatsink.png");
    public static final ResourceLocation TEXTURE_ENGINE_0 =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan_engine_active_0.png");
    public static final ResourceLocation TEXTURE_ENGINE_1 =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan_engine_active_1.png");
    public static final ResourceLocation TEXTURE_ENGINE_2 =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/steel_leviathan_engine_active_2.png");
    public static final ResourceLocation BOSS_BAR =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/gui/steel_leviathan_boss_bar.png");

    public static ResourceLocation engineTexture(int frame) {
        return switch (frame % THRUSTER_FRAME_COUNT) {
            case 1 -> TEXTURE_ENGINE_1;
            case 2 -> TEXTURE_ENGINE_2;
            default -> TEXTURE_ENGINE_0;
        };
    }
}

