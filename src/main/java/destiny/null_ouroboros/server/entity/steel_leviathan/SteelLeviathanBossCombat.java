package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanSinew;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SteelLeviathanBossCombat {
    private final SteelLeviathanHeadEntity head;

    private int moveBudget = SteelLeviathanConstants.MOVE_BUDGET;
    private boolean intimidationPending;
    private SteelLeviathanMove hardNextMove = SteelLeviathanMove.NONE;

    @Nullable private Vec3 burstPos;
    @Nullable private Vec3 burstOrigin;
    @Nullable private Vec3 burstForward;
    @Nullable private Vec3 burrowDiveTarget;
    private int burrowPhase;
    private int burstPhase;
    private int burstPhaseTicks;
    private int standHoldTicks;
    private boolean standApproaching;
    private int pummelCount;
    private float circleAngle;
    private float circleAngleTravelled;
    private float circleBobPhase;
    private float circleRadius;
    private int circlePhase;
    private int circlePhaseTicks;
    private float departYaw;
    private int stuckPhase;
    private int stuckHoldTicks;

    private float stuckOrbitBlend;

    @Nullable private Double stuckHoldY;

    private float orbitSign = 1.0F;

    private double pendingBurrowDepth;

    private double activeBurrowDepth = SteelLeviathanConstants.BOSS_BURROW_DEPTH;
    private String debugPhase = "";

    public SteelLeviathanBossCombat(SteelLeviathanHeadEntity head) {
        this.head = head;
    }

    private double motionSpeed(double base) {
        return base * SteelLeviathanConstants.BOSS_MOTION_SCALE;
    }

    private float combatTurnRate() {
        return SteelLeviathanConstants.HEAD_TURN_RATE_COMBAT * SteelLeviathanConstants.BOSS_MOTION_SCALE;
    }

    private void setDebugPhase(String phase) {
        debugPhase = phase;
    }

    private void pushDebugActionBar() {
        LivingEntity target = head.getCombatTarget();
        if (!(target instanceof Player player) || head.level().isClientSide) {
            return;
        }
        SteelLeviathanMove move = head.getCurrentMove();
        String phase = debugPhase == null || debugPhase.isEmpty() ? "-" : debugPhase;
        player.displayClientMessage(Component.literal("Leviathan: " + move + ":" + phase), true);
    }

    public boolean shouldHeatsinksBeOpen() {
        if (head.heatsinksForcedOpen) {
            return true;
        }
        SteelLeviathanMove move = head.getCurrentMove();

        if (move == SteelLeviathanMove.STUCK) {
            return stuckPhase >= 1;
        }

        return move == SteelLeviathanMove.STAND && !standApproaching;
    }

    public void begin(LivingEntity target, boolean fromAttack) {
        head.combatTargetUuid = target.getUUID();
        if (target instanceof Player player) {
            head.getReputation().addScore(player.getUUID(), -1);
        }
        head.setBehaviorState(SteelLeviathanBehaviorState.BOSSFIGHT);
        head.setBossBarVisible(true);
        moveBudget = SteelLeviathanConstants.MOVE_BUDGET;
        hardNextMove = SteelLeviathanMove.NONE;
        head.heatsinksForcedOpen = false;
        head.setThrustersActive(false);
        resetMoveLocals();

        if (!head.isPhaseTwo() && head.getMainHealth() <= SteelLeviathanConstants.MAX_HEALTH * 0.5F) {
            head.setPhaseTwo(true);
        }

        if (fromAttack) {
            intimidationPending = true;
            beginMove(SteelLeviathanMove.BURROW, true);
        } else {
            intimidationPending = false;
            beginMove(SteelLeviathanMove.STAND, true);
        }
    }

    public void tick() {
        LivingEntity target = head.getCombatTarget();
        double escape = SteelLeviathanConstants.BOSS_ESCAPE_RANGE;
        if (target == null || !target.isAlive() || head.distanceToSqr(target) > escape * escape) {
            escape();
            return;
        }
        if (head.getMainHealth() <= 0.0F) {
            head.startDeathPublic(target);
            return;
        }
        if (!head.isPhaseTwo() && head.getMainHealth() <= SteelLeviathanConstants.MAX_HEALTH * 0.5F) {
            head.setPhaseTwo(true);
        }

        if (head.getCurrentMove() == SteelLeviathanMove.NONE) {
            beginMove(pickNextMove(), false);
        }

        switch (head.getCurrentMove()) {
            case BURROW -> tickBurrow(target);
            case BURST -> tickBurst(target);
            case STAND -> tickStand(target);
            case LUNGE -> tickLunge(target);
            case LAUNCH -> tickLaunch(target);
            case CIRCLE -> tickCircle(target);
            case PUMMEL -> tickPummel(target);
            case STUCK -> tickStuck();
            default -> beginMove(SteelLeviathanMove.BURROW, true);
        }
        pushDebugActionBar();
    }

    public void escape() {
        head.combatTargetUuid = null;
        head.setBossBarVisible(false);
        head.setCurrentMove(SteelLeviathanMove.NONE);
        head.setThrustersActive(false);
        head.heatsinksForcedOpen = false;
        intimidationPending = false;
        hardNextMove = SteelLeviathanMove.NONE;
        resetMoveLocals();
        head.setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
        head.setUnderground(true);
    }

    public void save(CompoundTag tag) {
        tag.putInt("MoveBudget", moveBudget);
        tag.putBoolean("IntimidationPending", intimidationPending);
        tag.putInt("HardNextMove", hardNextMove.ordinal());
    }

    public void load(CompoundTag tag) {
        moveBudget = tag.contains("MoveBudget") ? tag.getInt("MoveBudget") : SteelLeviathanConstants.MOVE_BUDGET;
        intimidationPending = tag.getBoolean("IntimidationPending");
        int hard = tag.getInt("HardNextMove");
        SteelLeviathanMove[] values = SteelLeviathanMove.values();
        hardNextMove = hard >= 0 && hard < values.length ? values[hard] : SteelLeviathanMove.NONE;
    }

    private void resetMoveLocals() {
        burstPos = null;
        burstOrigin = null;
        burstForward = null;
        burrowDiveTarget = null;
        burrowPhase = 0;
        burstPhase = 0;
        burstPhaseTicks = 0;
        standHoldTicks = 0;
        standApproaching = true;
        pummelCount = 0;
        circleAngle = 0.0F;
        circleAngleTravelled = 0.0F;
        circleBobPhase = 0.0F;
        circleRadius = SteelLeviathanConstants.CIRCLE_RADIUS_MIN;
        circlePhase = 0;
        circlePhaseTicks = 0;
        stuckPhase = 0;
        stuckHoldTicks = 0;
        stuckOrbitBlend = 1.0F;
        stuckHoldY = null;
        orbitSign = 1.0F;
        head.lockedLungePos = null;
        head.consumeTarget = null;
    }

    private boolean isPaid(SteelLeviathanMove move) {
        return switch (move) {
            case BURST, STAND, LUNGE, LAUNCH, CIRCLE, PUMMEL -> true;
            default -> false;
        };
    }

    private void beginMove(SteelLeviathanMove move, boolean free) {
        if (!free && isPaid(move)) {
            if (intimidationPending && move == SteelLeviathanMove.STAND) {

            } else if (moveBudget <= 0) {
                enterStuck();
                return;
            } else {
                moveBudget--;
            }
        }

        head.setCurrentMove(move);
        head.setThrustersActive(false);
        if (move != SteelLeviathanMove.STUCK) {
            head.heatsinksForcedOpen = false;
        }
        resetMoveLocals();

        LivingEntity target = head.getCombatTarget();
        switch (move) {
            case BURROW -> {
                burrowPhase = 0;
                activeBurrowDepth = pendingBurrowDepth > 0.0D
                        ? pendingBurrowDepth
                        : SteelLeviathanConstants.BOSS_BURROW_DEPTH;
                pendingBurrowDepth = 0.0D;
                double burrowDepth = activeBurrowDepth;
                if (target != null) {
                    Vec3 away = new Vec3(head.getX() - target.getX(), 0.0D, head.getZ() - target.getZ());
                    if (away.lengthSqr() < 1.0E-4D) {
                        away = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
                        away = new Vec3(away.x, 0.0D, away.z);
                    }
                    away = away.normalize();
                    departYaw = (float) (Mth.atan2(-away.x, away.z) * Mth.RAD_TO_DEG);
                    head.updateGroundGuide(head.getX(), head.getZ());
                    burrowDiveTarget = new Vec3(
                            head.getX() + away.x * 4.0D,
                            head.smoothedGroundY - burrowDepth,
                            head.getZ() + away.z * 4.0D);
                    head.consumeTarget = burrowDiveTarget;
                } else {
                    head.updateGroundGuide(head.getX(), head.getZ());
                    burrowDiveTarget = new Vec3(head.getX(),
                            head.smoothedGroundY - burrowDepth,
                            head.getZ());
                    head.consumeTarget = burrowDiveTarget;
                    departYaw = head.getYRot();
                }
            }
            case STAND -> {
                standApproaching = true;
                standHoldTicks = 0;
                head.resetSurfaceApproach();
                if (target != null) {
                    head.breachAnchor = pickStandBreach(target);
                }
            }
            case BURST, PUMMEL -> {
                if (target != null) {
                    burstPos = target.position();
                }
                burstPhase = 0;
                burstPhaseTicks = 0;
                pummelCount = 0;
            }
            case CIRCLE -> {
                circleRadius = SteelLeviathanConstants.CIRCLE_RADIUS_MIN
                        + head.getBossRandom().nextFloat()
                        * (SteelLeviathanConstants.CIRCLE_RADIUS_MAX - SteelLeviathanConstants.CIRCLE_RADIUS_MIN);
                if (target != null) {
                    double dx = head.getX() - target.getX();
                    double dz = head.getZ() - target.getZ();
                    circleAngle = (float) Math.atan2(dz, dx);
                } else {
                    circleAngle = head.getBossRandom().nextFloat() * Mth.TWO_PI;
                }
                orbitSign = head.getBossRandom().nextBoolean() ? 1.0F : -1.0F;
                circleAngleTravelled = 0.0F;
                circleBobPhase = 0.0F;
                circlePhase = 0;
                circlePhaseTicks = 0;
            }
            case LUNGE -> {
                if (target != null) {
                    head.lockedLungePos = target.position();
                }
            }
            case LAUNCH -> {
                head.updateGroundGuide(head.getX(), head.getZ());
                double baseY = head.smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;
                burstOrigin = new Vec3(head.getX(), baseY, head.getZ());
                Vec3 forward;
                if (target != null) {
                    forward = new Vec3(target.getX() - head.getX(), 0.0D, target.getZ() - head.getZ());
                } else {
                    forward = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
                    forward = new Vec3(forward.x, 0.0D, forward.z);
                }
                if (forward.lengthSqr() < 1.0E-4D) {
                    forward = new Vec3(0.0D, 0.0D, 1.0D);
                }
                burstForward = forward.normalize();
                float yaw = (float) (Mth.atan2(-burstForward.x, burstForward.z) * Mth.RAD_TO_DEG);
                head.setYRot(yaw);
            }
            case STUCK -> {
                head.heatsinksForcedOpen = false;
                initStuckExposeCircle(target);
            }
            default -> {
            }
        }
    }

    private void enterStuck() {
        head.setCurrentMove(SteelLeviathanMove.STUCK);
        head.setThrustersActive(false);
        head.heatsinksForcedOpen = false;
        resetMoveLocals();
        initStuckExposeCircle(head.getCombatTarget());
    }

    private void initStuckExposeCircle(@Nullable LivingEntity target) {
        stuckPhase = 0;
        stuckHoldTicks = 0;
        stuckHoldY = null;
        orbitSign = head.getBossRandom().nextBoolean() ? 1.0F : -1.0F;

        if (target != null) {
            double dx = head.getX() - target.getX();
            double dz = head.getZ() - target.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 1.0E-4D) {
                circleAngle = (float) Math.atan2(dz, dx);
                float clamped = (float) Mth.clamp(dist,
                        SteelLeviathanConstants.CIRCLE_RADIUS_MIN,
                        SteelLeviathanConstants.CIRCLE_RADIUS_MAX);
                circleRadius = clamped;
                stuckOrbitBlend = Math.abs(dist - clamped) <= 0.5D ? 1.0F : 0.0F;
            } else {
                circleAngle = head.getYRot() * Mth.DEG_TO_RAD;
                circleRadius = SteelLeviathanConstants.CIRCLE_RADIUS_MIN
                        + (SteelLeviathanConstants.CIRCLE_RADIUS_MAX - SteelLeviathanConstants.CIRCLE_RADIUS_MIN) * 0.35F;
                stuckOrbitBlend = 0.0F;
            }
        } else {
            circleAngle = 0.0F;
            circleRadius = SteelLeviathanConstants.CIRCLE_RADIUS_MIN
                    + (SteelLeviathanConstants.CIRCLE_RADIUS_MAX - SteelLeviathanConstants.CIRCLE_RADIUS_MIN) * 0.35F;
            stuckOrbitBlend = 0.0F;
        }
        circleAngleTravelled = 0.0F;
        circleBobPhase = 0.0F;
        circlePhaseTicks = 0;
    }

    private void completeMove(SteelLeviathanMove next) {
        if (intimidationPending && head.getCurrentMove() == SteelLeviathanMove.STAND) {
            intimidationPending = false;
        }
        if (next == SteelLeviathanMove.BURROW) {
            beginMove(SteelLeviathanMove.BURROW, true);
        } else {
            beginMove(next, false);
        }
    }

    private SteelLeviathanMove pickNextMove() {
        if (hardNextMove != SteelLeviathanMove.NONE) {
            SteelLeviathanMove move = hardNextMove;
            hardNextMove = SteelLeviathanMove.NONE;
            return move;
        }
        if (head.isPhaseTwo()) {
            return switch (head.getBossRandom().nextInt(5)) {
                case 0 -> SteelLeviathanMove.BURST;
                case 1 -> SteelLeviathanMove.STAND;
                case 2 -> SteelLeviathanMove.LAUNCH;
                case 3 -> SteelLeviathanMove.CIRCLE;
                default -> SteelLeviathanMove.PUMMEL;
            };
        }
        return head.getBossRandom().nextBoolean() ? SteelLeviathanMove.BURST : SteelLeviathanMove.STAND;
    }

    private Vec3 pickStandBreach(LivingEntity target) {
        double dist = SteelLeviathanConstants.STAND_RADIUS_MIN
                + head.getBossRandom().nextDouble()
                * (SteelLeviathanConstants.STAND_RADIUS_MAX - SteelLeviathanConstants.STAND_RADIUS_MIN);
        double angle = head.getBossRandom().nextDouble() * Math.PI * 2.0D;
        return new Vec3(
                target.getX() + Math.cos(angle) * dist,
                0.0D,
                target.getZ() + Math.sin(angle) * dist);
    }

    private void tickBurrow(LivingEntity target) {
        head.setThrustersActive(false);
        double burrowDepth = activeBurrowDepth > 0.0D
                ? activeBurrowDepth
                : SteelLeviathanConstants.BOSS_BURROW_DEPTH;
        if (burrowDiveTarget == null) {
            head.updateGroundGuide(head.getX(), head.getZ());
            burrowDiveTarget = new Vec3(head.getX(),
                    head.smoothedGroundY - burrowDepth,
                    head.getZ());
            head.consumeTarget = burrowDiveTarget;
        }

        boolean deepLaunchBurrow = burrowDepth >= SteelLeviathanConstants.LAUNCH_BURROW_DEPTH - 0.5D;
        int diveMin = head.scaled(deepLaunchBurrow
                ? SteelLeviathanConstants.LAUNCH_BURROW_DIVE_MIN_TICKS
                : SteelLeviathanConstants.BURROW_DIVE_MIN_TICKS);
        int cruiseTicks = head.scaled(SteelLeviathanConstants.BURROW_CRUISE_TICKS_BOSS);
        float yawRate = SteelLeviathanConstants.BOSS_UNDERGROUND_YAW_RATE;
        double deepY = head.smoothedGroundY - burrowDepth;
        boolean alreadyDeep = head.getY() <= deepY + 0.35D;

        if (burrowPhase == 0) {
            if (alreadyDeep) {
                burrowPhase = 1;
                head.moveTicks = 0;
            } else {
                setDebugPhase("dive");
                head.setUnderground(true);
                head.diveTowardPoint(burrowDiveTarget,
                        motionSpeed(SteelLeviathanConstants.BURROW_DIVE_SPEED),
                        yawRate);
                head.updateGroundGuide(head.getX(), head.getZ());
                boolean deep = head.getY() <= deepY + 0.35D;
                if ((deep && head.moveTicks >= diveMin) || head.moveTicks >= diveMin * 4) {
                    burrowPhase = 1;
                    head.moveTicks = 0;
                } else {
                    return;
                }
            }
        }

        setDebugPhase("cruise");
        head.departCruiseStep(departYaw,
                burrowDepth,
                SteelLeviathanConstants.INTEREST_DEPART_SPRING_GAIN,
                motionSpeed(SteelLeviathanConstants.INTEREST_MOVE_SPEED),
                yawRate);

        if (head.moveTicks >= cruiseTicks) {
            if (intimidationPending) {
                beginMove(SteelLeviathanMove.STAND, true);
            } else {
                beginMove(pickNextMove(), false);
            }
        }
    }

    private void tickBurst(LivingEntity target) {
        if (tickBurstCycle(target)) {
            hardNextMove = SteelLeviathanMove.STAND;
            completeMove(SteelLeviathanMove.BURROW);
        }
    }

    private boolean tickBurstCycle(LivingEntity target) {
        if (burstPos == null) {
            burstPos = target.position();
        }
        head.setThrustersActive(false);
        head.updateGroundGuide(burstPos.x, burstPos.z);
        double deepY = head.smoothedGroundY - SteelLeviathanConstants.BOSS_BURROW_DEPTH;
        double crestHeight = head.isPhaseTwo()
                ? SteelLeviathanConstants.BURST_CREST_HEIGHT_PHASE_TWO
                : SteelLeviathanConstants.BURST_CREST_HEIGHT;
        double crestY = head.smoothedGroundY + crestHeight;

        int telegraphTicks = head.scaled(SteelLeviathanConstants.BURST_TELEGRAPH_TICKS);
        int arcTicks = head.scaled(SteelLeviathanConstants.BURST_EMERGE_TICKS
                + SteelLeviathanConstants.BURST_DIVE_TICKS);

        if (burstPhase == 0) {
            setDebugPhase("telegraph");
            if (head.level() instanceof ServerLevel server && burstPhaseTicks % 4 == 0) {
                spawnBurstTelegraph(server, burstPos);
            }
            double sinkGain = 0.28D * SteelLeviathanConstants.BOSS_MOTION_SCALE;
            double nextY = head.getY() + (deepY - head.getY()) * Math.min(0.55D, sinkGain);
            head.setPos(
                    Mth.lerp(0.35D, head.getX(), burstPos.x),
                    nextY,
                    Mth.lerp(0.35D, head.getZ(), burstPos.z));
            head.setUnderground(true);
            head.verticalVel = 0.0D;
            head.approachPitch(SteelLeviathanConstants.INTEREST_BURROW_DIVE_PITCH, combatTurnRate());
            burstPhaseTicks++;
            boolean deepEnough = head.getY() <= deepY + 1.0D;
            if (burstPhaseTicks >= telegraphTicks && deepEnough) {
                burstPhase = 1;
                burstPhaseTicks = 0;
                burstOrigin = new Vec3(burstPos.x, Math.min(head.getY(), deepY), burstPos.z);
                Vec3 forward = new Vec3(target.getX() - burstOrigin.x, 0.0D, target.getZ() - burstOrigin.z);
                if (forward.lengthSqr() < 1.0E-4D) {
                    forward = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
                    forward = new Vec3(forward.x, 0.0D, forward.z);
                }
                if (forward.lengthSqr() < 1.0E-4D) {
                    forward = new Vec3(0.0D, 0.0D, 1.0D);
                }
                burstForward = forward.normalize();
                float yaw = (float) (Mth.atan2(-burstForward.x, burstForward.z) * Mth.RAD_TO_DEG);
                head.setYRot(yaw);
            }
            return false;
        }

        setDebugPhase(burstPhaseTicks * 2 < arcTicks ? "emerge" : "dive");
        if (burstOrigin == null || burstForward == null) {
            burstOrigin = new Vec3(burstPos.x, deepY, burstPos.z);
            burstForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        float t = Mth.clamp(burstPhaseTicks / (float) Math.max(1, arcTicks), 0.0F, 1.0F);
        float alpha = t * (float) Math.PI;
        double sinA = Math.sin(alpha);
        double cosA = Math.cos(alpha);
        double R = SteelLeviathanConstants.BURST_ARC_RADIUS;
        double lift = crestY - deepY;
        double x = burstOrigin.x + burstForward.x * R * (1.0D - cosA);
        double z = burstOrigin.z + burstForward.z * R * (1.0D - cosA);
        double y = deepY + lift * sinA;
        head.setPos(x, y, z);
        head.setUnderground(y < head.smoothedGroundY);
        head.verticalVel = 0.0D;
        head.updateGroundGuide(x, z);

        double horizTan = Math.max(R * sinA, 0.05D);
        float tangentPitch = (float) (Mth.atan2(-lift * cosA, horizTan) * Mth.RAD_TO_DEG);
        tangentPitch = Mth.clamp(tangentPitch, -85.0F, 85.0F);
        float yaw = (float) (Mth.atan2(-burstForward.x, burstForward.z) * Mth.RAD_TO_DEG);
        float nextYaw = Mth.approachDegrees(head.getYRot(), yaw, combatTurnRate());
        head.setLookRotation(nextYaw, tangentPitch);

        if (sinA > 0.55D) {
            head.tryHeadHit(target);
        }

        burstPhaseTicks++;
        return burstPhaseTicks >= arcTicks;
    }

    private void resetBurstCycle(LivingEntity target) {
        burstPos = target.position();
        burstOrigin = null;
        burstForward = null;
        burstPhase = 0;
        burstPhaseTicks = 0;
    }

    private void spawnBurstTelegraph(ServerLevel server, Vec3 center) {
        int r = SteelLeviathanConstants.BURST_RADIUS;
        double refY = center.y;
        int maxDrop = SteelLeviathanConstants.BURST_TELEGRAPH_MAX_DROP;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = Mth.floor(center.x) + dx;
                int z = Mth.floor(center.z) + dz;
                int surfaceY = server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (surfaceY <= server.getMinBuildHeight()) {
                    continue;
                }

                if (refY - surfaceY > maxDrop) {
                    continue;
                }
                BlockPos solid = new BlockPos(x, surfaceY - 1, z);
                BlockState state = server.getBlockState(solid);
                if (state.isAir()) {
                    continue;
                }
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        x + 0.5, surfaceY, z + 0.5,
                        2, 0.2, 0.05, 0.2, 0.01);
            }
        }
    }

    private void tickStand(LivingEntity target) {
        float approachScale = SteelLeviathanConstants.STAND_APPROACH_TIME_SCALE / head.getTimeScale();
        if (standApproaching) {
            setDebugPhase("approach");
            if (head.breachAnchor == null) {
                head.breachAnchor = pickStandBreach(target);
            }
            if (head.tickSurfaceApproach(target, head.breachAnchor, approachScale,
                    SteelLeviathanConstants.BOSS_MOTION_SCALE)) {
                standApproaching = false;
                standHoldTicks = 0;
                head.standAnchor = head.position();
            }
            return;
        }

        setDebugPhase("hold");
        head.tickElevatedHold(target);
        standHoldTicks++;
        int hold = head.scaled(SteelLeviathanConstants.STAND_OPEN_TICKS);
        if (standHoldTicks >= hold) {
            if (intimidationPending) {
                intimidationPending = false;
            }
            if (head.getBossRandom().nextBoolean()) {
                completeMove(SteelLeviathanMove.LUNGE);
            } else {
                completeMove(SteelLeviathanMove.BURROW);
            }
        }
    }

    private void tickLunge(LivingEntity target) {

        int windup = head.isPhaseTwo()
                ? SteelLeviathanConstants.LUNGE_WINDUP_TICKS_PHASE_TWO
                : SteelLeviathanConstants.LUNGE_WINDUP_TICKS;
        int charge = head.scaled(SteelLeviathanConstants.LUNGE_CHARGE_TICKS);
        head.setUnderground(false);

        if (head.lockedLungePos == null) {
            head.lockedLungePos = target.position();
        }
        Vec3 lock = head.lockedLungePos;

        if (head.moveTicks < windup) {
            setDebugPhase("windup");
            head.setThrustersActive(false);
            head.interestLookAt(lock, SteelLeviathanConstants.INTEREST_LOOK_PITCH_MAX);
            Vec3 away = head.position().subtract(lock);
            if (away.lengthSqr() < 1.0E-4D) {
                away = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
            }
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize().scale(0.1D * SteelLeviathanConstants.BOSS_MOTION_SCALE);
                head.setPos(head.getX() + away.x, head.getY(), head.getZ() + away.z);
            }
            head.verticalVel = 0.0D;
            return;
        }

        setDebugPhase("charge");
        head.setThrustersActive(true);
        chargeToward(lock, motionSpeed(SteelLeviathanConstants.LUNGE_CHARGE_SPEED));
        head.tryHeadHit(target);
        if (head.moveTicks >= windup + charge
                || head.position().distanceToSqr(lock) < 4.0D) {
            head.lockedLungePos = null;
            completeMove(SteelLeviathanMove.BURROW);
        }
    }

    private void tickLaunch(LivingEntity target) {
        int rise = head.scaled(SteelLeviathanConstants.LAUNCH_RISE_TICKS);
        int dive = head.scaled(SteelLeviathanConstants.LAUNCH_DIVE_TICKS);
        int total = Math.max(1, rise + dive);
        head.setThrustersActive(true);

        if (burstOrigin == null || burstForward == null) {
            head.updateGroundGuide(head.getX(), head.getZ());
            double baseY = head.smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;
            burstOrigin = new Vec3(head.getX(), baseY, head.getZ());
            Vec3 forward = new Vec3(target.getX() - head.getX(), 0.0D, target.getZ() - head.getZ());
            if (forward.lengthSqr() < 1.0E-4D) {
                forward = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
                forward = new Vec3(forward.x, 0.0D, forward.z);
            }
            if (forward.lengthSqr() < 1.0E-4D) {
                forward = new Vec3(0.0D, 0.0D, 1.0D);
            }
            burstForward = forward.normalize();
        }

        float t = Mth.clamp(head.moveTicks / (float) total, 0.0F, 1.0F);
        float alpha = t * (float) Math.PI;
        double sinA = Math.sin(alpha);
        double cosA = Math.cos(alpha);
        double R = SteelLeviathanConstants.LAUNCH_ARC_RADIUS;
        double apexH = SteelLeviathanConstants.LAUNCH_APEX_HEIGHT;

        head.updateGroundGuide(
                burstOrigin.x + burstForward.x * R * (1.0D - cosA),
                burstOrigin.z + burstForward.z * R * (1.0D - cosA));
        double baseY = head.smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;

        boolean pastApex = alpha >= (float) (Math.PI * 0.5D);
        if (pastApex && head.lockedLungePos == null) {
            head.lockedLungePos = target.position();
        }

        double x;
        double y;
        double z;
        double deepY = head.smoothedGroundY - SteelLeviathanConstants.LAUNCH_BURROW_DEPTH;
        if (!pastApex) {
            setDebugPhase("rise");
            x = burstOrigin.x + burstForward.x * R * (1.0D - cosA);
            z = burstOrigin.z + burstForward.z * R * (1.0D - cosA);
            y = Math.max(baseY, baseY + apexH * sinA);
            head.setUnderground(false);
        } else {
            setDebugPhase("dive");
            float diveT = Mth.clamp((alpha - (float) (Math.PI * 0.5D)) / (float) (Math.PI * 0.5D), 0.0F, 1.0F);
            double apexX = burstOrigin.x + burstForward.x * R;
            double apexZ = burstOrigin.z + burstForward.z * R;
            Vec3 lock = head.lockedLungePos != null ? head.lockedLungePos : target.position();
            x = Mth.lerp(diveT, apexX, lock.x);
            z = Mth.lerp(diveT, apexZ, lock.z);

            if (diveT < 0.55F) {
                y = Math.max(baseY, baseY + apexH * sinA);
                head.setUnderground(false);
            } else {
                float sink = (diveT - 0.55F) / 0.45F;
                double airY = Math.max(baseY, baseY + apexH * sinA);
                y = Mth.lerp(sink, airY, deepY);
                head.setUnderground(y < head.smoothedGroundY);
            }
        }

        head.setPos(x, y, z);
        head.verticalVel = 0.0D;

        double horizTan = Math.max(R * Math.abs(sinA), 0.05D);
        float tangentPitch = (float) (Mth.atan2(-apexH * cosA, horizTan) * Mth.RAD_TO_DEG);
        tangentPitch = Mth.clamp(tangentPitch, -85.0F, 85.0F);
        Vec3 lookDir = pastApex && head.lockedLungePos != null
                ? head.lockedLungePos.subtract(head.position())
                : burstForward;
        lookDir = new Vec3(lookDir.x, 0.0D, lookDir.z);
        float nextYaw = head.getYRot();
        if (lookDir.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(-lookDir.x, lookDir.z) * Mth.RAD_TO_DEG);
            nextYaw = Mth.approachDegrees(head.getYRot(), yaw, combatTurnRate());
        }
        head.setLookRotation(nextYaw, tangentPitch);

        if (pastApex) {
            head.tryHeadHit(target);
        }

        boolean nearLock = head.lockedLungePos != null
                && head.position().distanceToSqr(head.lockedLungePos) < 4.0D;
        if (head.moveTicks >= total || (pastApex && nearLock && t >= 0.85F)) {
            head.lockedLungePos = null;
            pendingBurrowDepth = SteelLeviathanConstants.LAUNCH_BURROW_DEPTH;
            completeMove(SteelLeviathanMove.BURROW);
        }
    }

    private double circleSurfaceBand() {
        return head.smoothedGroundY + SteelLeviathanConstants.BOB_SURFACE_PEEK;
    }

    private void clampCircleSurfaceY() {
        double minY = circleSurfaceBand();
        if (head.getY() < minY) {
            head.setPos(head.getX(), minY, head.getZ());
        }
    }

    private void tickCircle(LivingEntity target) {
        int chargeTicks = head.scaled(SteelLeviathanConstants.CIRCLE_CHARGE_TICKS);
        int orbitFailsafe = head.scaled(SteelLeviathanConstants.CIRCLE_ORBIT_TICKS * 3);
        int windup = head.isPhaseTwo()
                ? SteelLeviathanConstants.LUNGE_WINDUP_TICKS_PHASE_TWO
                : SteelLeviathanConstants.LUNGE_WINDUP_TICKS;
        head.setThrustersActive(true);

        if (circlePhase == 0) {
            setDebugPhase("orbit");

            double radius = Math.max(1.0D, circleRadius);
            float dAngle = (float) (motionSpeed(SteelLeviathanConstants.CIRCLE_ORBIT_SPEED) / radius);
            circleAngle += orbitSign * dAngle;
            circleAngleTravelled += dAngle;
            circleBobPhase += SteelLeviathanConstants.BOB_SPEED * SteelLeviathanConstants.CIRCLE_BOB_SPEED_MULT;

            double cx = target.getX() + Math.cos(circleAngle) * circleRadius;
            double cz = target.getZ() + Math.sin(circleAngle) * circleRadius;
            head.updateGroundGuide(cx, cz);

            float bob = Mth.sin(circleBobPhase);
            double deepY = head.smoothedGroundY - SteelLeviathanConstants.BOSS_BURROW_DEPTH;
            double peekY = circleSurfaceBand();
            double bobT = (bob + 1.0F) * 0.5F;
            double bobY = deepY + (peekY - deepY) * bobT;
            double nextY = head.getY() + (bobY - head.getY()) * SteelLeviathanConstants.BOB_Y_FOLLOW;
            head.setPos(cx, nextY, cz);
            head.verticalVel = 0.0D;
            head.setUnderground(nextY < head.smoothedGroundY);

            double tx = orbitSign > 0.0F ? -Math.sin(circleAngle) : Math.sin(circleAngle);
            double tz = orbitSign > 0.0F ? Math.cos(circleAngle) : -Math.cos(circleAngle);
            float yaw = (float) (Mth.atan2(-tx, tz) * Mth.RAD_TO_DEG);
            float bobPitch = -Mth.cos(circleBobPhase) * SteelLeviathanConstants.BOB_PITCH_AMPLITUDE;
            float nextYaw = Mth.approachDegrees(head.getYRot(), yaw, combatTurnRate());
            float nextPitch = Mth.approachDegrees(head.getBodyPitch(), bobPitch, combatTurnRate());
            head.setLookRotation(nextYaw, nextPitch);

            circlePhaseTicks++;
            if (circleAngleTravelled >= Mth.TWO_PI || circlePhaseTicks >= orbitFailsafe) {
                circlePhase = 1;
                circlePhaseTicks = 0;
            }
            return;
        }

        if (circlePhase == 1) {
            setDebugPhase("surface");
            head.setUnderground(false);
            head.updateGroundGuide(head.getX(), head.getZ());
            double surfaceBand = circleSurfaceBand();
            double liftGain = Math.min(0.55D, 0.28D * SteelLeviathanConstants.BOSS_MOTION_SCALE);
            double nextY = head.getY() + (surfaceBand - head.getY()) * liftGain;
            head.setPos(head.getX(), nextY, head.getZ());
            head.verticalVel = 0.0D;
            head.approachPitch(0.0F, combatTurnRate());
            circlePhaseTicks++;
            int surfaceFailsafe = head.scaled(40);
            if (head.getY() >= surfaceBand - 0.5D || circlePhaseTicks >= surfaceFailsafe) {
                head.setPos(head.getX(), Math.max(head.getY(), surfaceBand), head.getZ());
                circlePhase = 2;
                circlePhaseTicks = 0;
                head.lockedLungePos = target.position();
            }
            return;
        }

        if (circlePhase == 2) {

            setDebugPhase("windup");
            head.setUnderground(false);
            if (head.lockedLungePos == null) {
                head.lockedLungePos = target.position();
            }
            Vec3 lock = head.lockedLungePos;
            head.interestLookAt(lock, SteelLeviathanConstants.INTEREST_LOOK_PITCH_MAX);
            Vec3 away = head.position().subtract(lock);
            if (away.lengthSqr() < 1.0E-4D) {
                away = SteelLeviathanSinew.facingFromYawPitch(head.getYRot(), 0.0F);
            }
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize().scale(0.1D * SteelLeviathanConstants.BOSS_MOTION_SCALE);
                head.setPos(head.getX() + away.x, head.getY(), head.getZ() + away.z);
            }
            clampCircleSurfaceY();
            head.verticalVel = 0.0D;
            circlePhaseTicks++;
            if (circlePhaseTicks >= windup) {
                circlePhase = 3;
                circlePhaseTicks = 0;
            }
            return;
        }

        setDebugPhase("charge");
        if (head.lockedLungePos == null) {
            head.lockedLungePos = target.position();
        }
        head.setUnderground(false);
        chargeToward(head.lockedLungePos, motionSpeed(SteelLeviathanConstants.CIRCLE_CHARGE_SPEED));
        clampCircleSurfaceY();
        head.tryHeadHit(target);
        circlePhaseTicks++;
        if (circlePhaseTicks >= chargeTicks
                || head.position().distanceToSqr(head.lockedLungePos) < 4.0D) {
            head.lockedLungePos = null;
            completeMove(SteelLeviathanMove.BURROW);
        }
    }

    private void tickPummel(LivingEntity target) {
        if (tickBurstCycle(target)) {
            pummelCount++;
            if (pummelCount >= SteelLeviathanConstants.PUMMEL_STRIKES) {
                completeMove(SteelLeviathanMove.BURROW);
            } else {
                resetBurstCycle(target);
            }
        }
    }

    private void tickStuck() {
        LivingEntity target = head.getCombatTarget();
        head.setThrustersActive(false);
        head.setDeltaMovement(Vec3.ZERO);
        head.verticalVel = 0.0D;
        double surfaceY = head.smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;

        if (stuckPhase == 0) {

            setDebugPhase("expose");
            head.heatsinksForcedOpen = false;
            if (target == null) {
                stuckPhase = 1;
                return;
            }
            head.updateGroundGuide(head.getX(), head.getZ());
            boolean settled = Math.abs(head.getY() - surfaceY) <= 1.0D;
            if (!settled) {

                double settleY = head.getY() + (surfaceY - head.getY()) * 0.35D;
                head.setPos(head.getX(), settleY, head.getZ());
                head.setUnderground(false);
                head.approachPitch(0.0F, combatTurnRate());
                return;
            }

            double radius = Math.max(1.0D, circleRadius);
            float dAngle = (float) (motionSpeed(SteelLeviathanConstants.CIRCLE_ORBIT_SPEED) / radius);
            circleAngle += orbitSign * dAngle;
            circleAngleTravelled += dAngle;
            circleBobPhase += SteelLeviathanConstants.BOB_SPEED * SteelLeviathanConstants.CIRCLE_BOB_SPEED_MULT;

            double idealX = target.getX() + Math.cos(circleAngle) * circleRadius;
            double idealZ = target.getZ() + Math.sin(circleAngle) * circleRadius;
            stuckOrbitBlend = Math.min(1.0F,
                    stuckOrbitBlend + SteelLeviathanConstants.STUCK_ORBIT_BLEND_RATE);
            double cx = Mth.lerp(stuckOrbitBlend, head.getX(), idealX);
            double cz = Mth.lerp(stuckOrbitBlend, head.getZ(), idealZ);
            head.updateGroundGuide(cx, cz);

            head.setPos(cx, surfaceY, cz);
            head.setUnderground(false);

            double tx = orbitSign > 0.0F ? -Math.sin(circleAngle) : Math.sin(circleAngle);
            double tz = orbitSign > 0.0F ? Math.cos(circleAngle) : -Math.cos(circleAngle);
            float yaw = (float) (Mth.atan2(-tx, tz) * Mth.RAD_TO_DEG);
            float bobPitch = -Mth.cos(circleBobPhase) * (SteelLeviathanConstants.BOB_PITCH_AMPLITUDE * 0.35F);

            head.setLookRotation(yaw, bobPitch);

            circlePhaseTicks++;
            int exposeFailsafe = head.scaled(SteelLeviathanConstants.CIRCLE_ORBIT_TICKS * 3);
            if (circleAngleTravelled >= Mth.TWO_PI || circlePhaseTicks >= exposeFailsafe) {
                stuckPhase = 1;
                stuckHoldTicks = 0;
            }
            return;
        }

        if (stuckPhase == 1) {
            setDebugPhase("breach");
            head.heatsinksForcedOpen = true;
            head.updateGroundGuide(head.getX(), head.getZ());
            double liftGain = Math.min(0.45D, 0.2D * SteelLeviathanConstants.BOSS_MOTION_SCALE);
            double nextY = head.getY() + (surfaceY - head.getY()) * liftGain;
            head.setPos(head.getX(), nextY, head.getZ());
            head.setUnderground(head.getY() < head.smoothedGroundY);
            head.approachPitch(SteelLeviathanConstants.STUCK_PITCH, combatTurnRate());
            if (head.getY() >= surfaceY - 0.5D) {
                stuckPhase = 2;
                stuckHoldTicks = 0;
                stuckHoldY = surfaceY;
                head.setUnderground(false);
                head.setPos(head.getX(), stuckHoldY, head.getZ());

                head.setLookRotation(head.getYRot(), SteelLeviathanConstants.STUCK_PITCH);
                head.playSound(SoundRegistry.STEEL_LEVIATHAN_OVERHEAT_STALL.get(), 1.0F, 1.0F);
            }
            return;
        }

        setDebugPhase("overheat");
        head.heatsinksForcedOpen = true;
        head.setUnderground(false);
        if (stuckHoldY == null) {
            stuckHoldY = surfaceY;
        }
        head.setPos(head.getX(), stuckHoldY, head.getZ());
        stuckHoldTicks++;
        int stuckTicks = head.isPhaseTwo()
                ? SteelLeviathanConstants.STUCK_TICKS_PHASE_TWO
                : SteelLeviathanConstants.STUCK_TICKS;
        if (stuckHoldTicks >= stuckTicks) {
            moveBudget = SteelLeviathanConstants.MOVE_BUDGET;
            head.heatsinksForcedOpen = false;
            beginMove(SteelLeviathanMove.BURROW, true);
        }
    }

    private void chargeToward(Vec3 target, double speed) {
        Vec3 delta = target.subtract(head.position());
        if (delta.lengthSqr() > 1.0E-6D) {
            float yaw = (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
            double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float pitch = (float) (Mth.atan2(-delta.y, Math.max(horiz, 0.1D)) * Mth.RAD_TO_DEG);
            float nextYaw = Mth.approachDegrees(head.getYRot(), yaw, combatTurnRate());
            float nextPitch = Mth.approachDegrees(head.getBodyPitch(),
                    Mth.clamp(pitch, -70.0F, 85.0F), combatTurnRate());
            head.setLookRotation(nextYaw, nextPitch);
            Vec3 step = delta.normalize().scale(speed);
            head.setPos(head.getX() + step.x, head.getY() + step.y, head.getZ() + step.z);
        }
        head.verticalVel = 0.0D;
    }
}

