package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.entity.steel_leviathan.BurrowMissileEntity;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanBehaviorState;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanMove;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SteelLeviathanAmbienceSoundManager {
    private static final float UNDERGROUND_MAX_VOLUME = 0.45F;
    private static final float UNDERGROUND_FADE_SPEED = UNDERGROUND_MAX_VOLUME / (2.5F * 20.0F);
    private static final float BREACHING_MAX_VOLUME = 0.55F;
    private static final double MOVE_EPSILON_SQR = 0.0004D;

    private static final int BREACHING_HOLD_TICKS = 12;

    private static ManifoldingSoundInstance undergroundLoop;

    private static final float SCAN_MAX_VOLUME = 0.7F;

    private static final Map<Integer, BreachingLoopState> BREACHING_LOOPS = new HashMap<>();
    private static final Map<Integer, ScanLoopState> SCAN_LOOPS = new HashMap<>();
    private static final Map<Integer, SteelLeviathanEngineLoopSound> ENGINE_LOOPS = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_THRUSTERS_ACTIVE = new HashMap<>();
    private static final Map<Integer, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final Map<Integer, Integer> MOVING_HOLD = new HashMap<>();

    private SteelLeviathanAmbienceSoundManager() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }

        boolean burstTelegraphUnderground = false;
        Set<Integer> seenParts = new HashSet<>();
        Set<Integer> scanningHeads = new HashSet<>();
        Set<Integer> engineLoopIds = new HashSet<>();

        Map<Integer, List<SteelLeviathanPartEntity>> eligibleByWorm = new HashMap<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof BurrowMissileEntity missile && missile.isAlive()) {
                int missileId = missile.getId();
                engineLoopIds.add(missileId);
                ensureEngineLoop(mc, missileId, missile);
                continue;
            }

            if (!(entity instanceof SteelLeviathanPartEntity part) || !part.isAlive()) {
                continue;
            }

            int id = part.getId();
            seenParts.add(id);

            if (isBreaching(part)) {
                int wormKey = wormKey(part);
                eligibleByWorm.computeIfAbsent(wormKey, k -> new ArrayList<>()).add(part);
            }

            if (part instanceof SteelLeviathanHeadEntity head) {
                if (isBossfightBurstTelegraph(head)) {
                    burstTelegraphUnderground = true;
                }
                if (head.getBehaviorState() == SteelLeviathanBehaviorState.INTEREST_SCAN) {
                    scanningHeads.add(head.getId());
                    ScanLoopState scanState = SCAN_LOOPS.computeIfAbsent(head.getId(), k -> new ScanLoopState());
                    scanState.head = head;
                    ensureScanLoop(mc, scanState);
                }
                tickThrusterSounds(level, head);
                if (head.areThrustersActive()) {
                    engineLoopIds.add(id);
                    ensureEngineLoop(mc, id, head);
                }
            }

            LAST_POSITIONS.put(id, part.position());
        }

        Iterator<Map.Entry<Integer, SteelLeviathanEngineLoopSound>> engineIt = ENGINE_LOOPS.entrySet().iterator();
        while (engineIt.hasNext()) {
            Map.Entry<Integer, SteelLeviathanEngineLoopSound> entry = engineIt.next();
            if (!engineLoopIds.contains(entry.getKey()) || entry.getValue().isStopped()) {
                mc.getSoundManager().stop(entry.getValue());
                engineIt.remove();
            }
        }

        LAST_THRUSTERS_ACTIVE.keySet().removeIf(id -> !seenParts.contains(id));

        Map<Integer, SteelLeviathanPartEntity> playPartByWorm = resolveBreachingPlayParts(eligibleByWorm);

        for (Map.Entry<Integer, SteelLeviathanPartEntity> entry : playPartByWorm.entrySet()) {
            BreachingLoopState state = BREACHING_LOOPS.computeIfAbsent(entry.getKey(), k -> new BreachingLoopState());
            state.holdTicks = BREACHING_HOLD_TICKS;
            state.followPart = entry.getValue();
        }

        Iterator<Map.Entry<Integer, BreachingLoopState>> it = BREACHING_LOOPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BreachingLoopState> entry = it.next();
            BreachingLoopState state = entry.getValue();
            boolean eligible = playPartByWorm.containsKey(entry.getKey());
            if (!eligible) {
                if (state.holdTicks > 0) {
                    state.holdTicks--;
                }
            }

            boolean shouldPlay = eligible || state.holdTicks > 0;
            if (shouldPlay && state.followPart != null) {
                ensureBreachingLoop(mc, state);
            } else {
                fadeBreachingLoop(state);
                if (state.loop == null || state.loop.isStopped()) {
                    it.remove();
                }
            }
        }

        Iterator<Map.Entry<Integer, ScanLoopState>> scanIt = SCAN_LOOPS.entrySet().iterator();
        while (scanIt.hasNext()) {
            Map.Entry<Integer, ScanLoopState> entry = scanIt.next();
            ScanLoopState state = entry.getValue();
            if (scanningHeads.contains(entry.getKey()) && state.head != null) {
                ensureScanLoop(mc, state);
            } else {
                fadeScanLoop(state);
                if (state.loop == null || state.loop.isStopped()) {
                    scanIt.remove();
                }
            }
        }

        tickUndergroundLoop(mc, burstTelegraphUnderground);

        LAST_POSITIONS.keySet().removeIf(id -> !seenParts.contains(id));
        MOVING_HOLD.keySet().removeIf(id -> !seenParts.contains(id));
    }

    public static void stopAll() {
        Minecraft mc = Minecraft.getInstance();
        if (undergroundLoop != null) {
            mc.getSoundManager().stop(undergroundLoop);
            undergroundLoop = null;
        }
        for (BreachingLoopState state : BREACHING_LOOPS.values()) {
            if (state.loop != null) {
                mc.getSoundManager().stop(state.loop);
            }
        }
        BREACHING_LOOPS.clear();
        for (ScanLoopState state : SCAN_LOOPS.values()) {
            if (state.loop != null) {
                mc.getSoundManager().stop(state.loop);
            }
        }
        SCAN_LOOPS.clear();
        for (SteelLeviathanEngineLoopSound loop : ENGINE_LOOPS.values()) {
            mc.getSoundManager().stop(loop);
        }
        ENGINE_LOOPS.clear();
        LAST_THRUSTERS_ACTIVE.clear();
        LAST_POSITIONS.clear();
        MOVING_HOLD.clear();
    }

    private static void tickThrusterSounds(ClientLevel level, SteelLeviathanPartEntity part) {
        int id = part.getId();
        boolean active = part.areThrustersActive();
        Boolean prev = LAST_THRUSTERS_ACTIVE.put(id, active);
        if (prev != null && !prev && active) {
            level.playLocalSound(
                    part.getX(), part.getY(), part.getZ(),
                    SoundRegistry.STEEL_LEVIATHAN_ENGINE_IGNITE.get(),
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F,
                    false);
        }
    }

    private static void ensureEngineLoop(Minecraft mc, int id, Entity entity) {
        SteelLeviathanEngineLoopSound loop = ENGINE_LOOPS.get(id);
        boolean needsNew = loop == null || loop.isStopped() || !mc.getSoundManager().isActive(loop);
        if (needsNew) {
            loop = new SteelLeviathanEngineLoopSound(entity);
            ENGINE_LOOPS.put(id, loop);
            mc.getSoundManager().play(loop);
        }
    }

    private static boolean isBossfightBurstTelegraph(SteelLeviathanHeadEntity head) {
        return head.getBehaviorState() == SteelLeviathanBehaviorState.BOSSFIGHT
                && head.getCurrentMove() == SteelLeviathanMove.BURST
                && head.isUnderground();
    }

    private static void tickUndergroundLoop(Minecraft mc, boolean playUnderground) {
        float target = playUnderground ? UNDERGROUND_MAX_VOLUME : 0.0F;

        if (undergroundLoop == null && target > 0.0F) {
            undergroundLoop = new ManifoldingSoundInstance(
                    SoundRegistry.STEEL_LEVIATHAN_UNDERGROUND_LOOP.get(),
                    SoundSource.HOSTILE,
                    true,
                    UNDERGROUND_FADE_SPEED);
            mc.getSoundManager().play(undergroundLoop);
        }

        if (undergroundLoop != null) {
            undergroundLoop.setTargetVolume(target);
            if (undergroundLoop.isStopped()) {
                undergroundLoop = null;
            }
        }
    }

    private static int wormKey(SteelLeviathanPartEntity part) {
        if (part instanceof SteelLeviathanHeadEntity) {
            return part.getId();
        }
        SteelLeviathanHeadEntity head = part.resolveHead();
        return head != null ? head.getId() : part.getId();
    }

    private static Map<Integer, SteelLeviathanPartEntity> resolveBreachingPlayParts(
            Map<Integer, List<SteelLeviathanPartEntity>> eligibleByWorm) {
        Map<Integer, SteelLeviathanPartEntity> playParts = new HashMap<>();
        for (Map.Entry<Integer, List<SteelLeviathanPartEntity>> entry : eligibleByWorm.entrySet()) {
            List<SteelLeviathanPartEntity> eligible = entry.getValue();
            if (eligible.isEmpty()) {
                continue;
            }
            if (eligible.size() == 1) {
                playParts.put(entry.getKey(), eligible.get(0));
                continue;
            }
            int headId = entry.getKey();
            for (SteelLeviathanPartEntity part : eligible) {
                if (part.getId() == headId || part instanceof SteelLeviathanHeadEntity) {
                    playParts.put(entry.getKey(), part);
                    break;
                }
            }
        }
        return playParts;
    }

    private static void ensureBreachingLoop(Minecraft mc, BreachingLoopState state) {
        SteelLeviathanPartEntity part = state.followPart;
        if (part == null) {
            return;
        }

        SteelLeviathanBreachingLoopSound loop = state.loop;
        boolean needsNew = loop == null || loop.isStopped() || !mc.getSoundManager().isActive(loop);
        if (needsNew) {

            boolean recovering = loop != null;
            loop = new SteelLeviathanBreachingLoopSound(part);
            if (recovering) {
                loop.forceVolume(BREACHING_MAX_VOLUME);
            } else {
                loop.forceVolume(0.0F);
                loop.setTargetVolume(BREACHING_MAX_VOLUME);
            }
            state.loop = loop;
            mc.getSoundManager().play(loop);
            return;
        }

        loop.setPart(part);
        loop.setTargetVolume(BREACHING_MAX_VOLUME);
    }

    private static void fadeBreachingLoop(BreachingLoopState state) {
        if (state.loop != null) {
            state.loop.setTargetVolume(0.0F);
            if (state.loop.isStopped()) {
                state.loop = null;
            }
        }
    }

    private static void ensureScanLoop(Minecraft mc, ScanLoopState state) {
        SteelLeviathanHeadEntity head = state.head;
        if (head == null) {
            return;
        }

        SteelLeviathanScanLoopSound loop = state.loop;
        boolean needsNew = loop == null || loop.isStopped() || !mc.getSoundManager().isActive(loop);
        if (needsNew) {
            boolean recovering = loop != null;
            loop = new SteelLeviathanScanLoopSound(head);
            if (recovering) {
                loop.forceVolume(SCAN_MAX_VOLUME);
            } else {
                loop.forceVolume(0.0F);
                loop.setTargetVolume(SCAN_MAX_VOLUME);
            }
            state.loop = loop;
            mc.getSoundManager().play(loop);
            return;
        }

        loop.setHead(head);
        loop.setTargetVolume(SCAN_MAX_VOLUME);
    }

    private static void fadeScanLoop(ScanLoopState state) {
        if (state.loop != null) {
            state.loop.setTargetVolume(0.0F);
            if (state.loop.isStopped()) {
                state.loop = null;
            }
        }
    }

    private static boolean isBreaching(SteelLeviathanPartEntity part) {
        if (!isMoving(part)) {
            return false;
        }
        AABB box = part.getBoundingBox().deflate(0.25D);
        if (part.level().noCollision(part, box)) {
            return false;
        }

        if (part instanceof SteelLeviathanHeadEntity) {
            return true;
        }
        return !isFullyBuried(part, box);
    }

    private static boolean isMoving(SteelLeviathanPartEntity part) {
        int id = part.getId();
        Vec3 last = LAST_POSITIONS.get(id);
        if (last != null && part.position().distanceToSqr(last) > MOVE_EPSILON_SQR) {
            MOVING_HOLD.put(id, BREACHING_HOLD_TICKS);
            return true;
        }
        Integer hold = MOVING_HOLD.get(id);
        if (hold != null && hold > 0) {
            MOVING_HOLD.put(id, hold - 1);
            return true;
        }
        return false;
    }

    private static boolean isFullyBuried(SteelLeviathanPartEntity part, AABB box) {

        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    if (!isSolidAt(part, x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return isSolidAt(part, box.getCenter().x, box.getCenter().y, box.getCenter().z);
    }

    private static boolean isSolidAt(SteelLeviathanPartEntity part, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = part.level().getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(part.level(), pos).isEmpty();
    }

    private static final class BreachingLoopState {
        private SteelLeviathanBreachingLoopSound loop;
        private SteelLeviathanPartEntity followPart;
        private int holdTicks;
    }

    private static final class ScanLoopState {
        private SteelLeviathanScanLoopSound loop;
        private SteelLeviathanHeadEntity head;
    }
}
