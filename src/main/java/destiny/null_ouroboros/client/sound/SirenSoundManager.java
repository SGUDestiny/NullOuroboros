package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity.State;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SirenSoundManager {
    private static final Map<BlockPos, TrackedSound> ACTIVE_SOUNDS = new HashMap<>();

    public static void clientTick(MechanicalSirenBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        State state = blockEntity.getState();
        State trackedPhase = getTrackedPhase(pos);

        if (state == State.IDLE) {
            if (trackedPhase == State.END || (trackedPhase == null && isActive(pos))) {
                stop(pos);
            }
            return;
        }

        if (trackedPhase == null) {
            if (state != State.END) {
                syncFromBlockEntity(blockEntity);
            }
            return;
        }

        if (trackedPhase.ordinal() > state.ordinal()) {
            return;
        }

        if (trackedPhase != state) {
            syncFromBlockEntity(blockEntity);
            return;
        }

        if (state == State.LOOP && !isFullyActive(pos)) {
            syncFromBlockEntity(blockEntity);
        }
    }

    public static void syncFromBlockEntity(MechanicalSirenBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        State state = blockEntity.getState();

        if (state == State.IDLE) {
            stop(pos);
            return;
        }

        SoundEvent normal = blockEntity.getNormalSoundEvent();
        SoundEvent distant = blockEntity.getDistantSoundEvent();
        if (normal == null || distant == null) {
            stop(pos);
            return;
        }

        applySoundState(pos, state, normal, distant, state == State.LOOP, blockEntity);
    }

    public static void applyFromPacket(BlockPos pos, SoundEvent normalEvent, SoundEvent distantEvent, boolean looping) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (!(mc.level.getBlockEntity(pos) instanceof MechanicalSirenBlockEntity blockEntity)) {
            return;
        }

        State phase = inferPhaseFromEvents(normalEvent, looping);
        applySoundState(pos, phase, normalEvent, distantEvent, looping, blockEntity);
    }

    public static boolean isActive(BlockPos pos) {
        TrackedSound tracked = ACTIVE_SOUNDS.get(pos);
        return tracked != null && isPairActive(tracked.pair);
    }

    public static boolean isFullyActive(BlockPos pos) {
        TrackedSound tracked = ACTIVE_SOUNDS.get(pos);
        return tracked != null && isPairFullyActive(tracked.pair);
    }

    @Nullable
    public static State getTrackedPhase(BlockPos pos) {
        TrackedSound tracked = ACTIVE_SOUNDS.get(pos);
        return tracked != null ? tracked.phase : null;
    }

    public static void stop(BlockPos pos) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        TrackedSound tracked = ACTIVE_SOUNDS.remove(pos);
        if (tracked != null) {
            sm.stop(tracked.pair.normal);
            sm.stop(tracked.pair.distant);
        }
    }

    public static void stopAll() {
        for (BlockPos pos : ACTIVE_SOUNDS.keySet().toArray(BlockPos[]::new)) {
            stop(pos);
        }
    }

    private static void applySoundState(BlockPos pos, State phase, SoundEvent normalEvent, SoundEvent distantEvent,
                                        boolean looping, MechanicalSirenBlockEntity blockEntity) {
        TrackedSound tracked = ACTIVE_SOUNDS.get(pos);
        if (tracked != null && tracked.phase == phase && (phase != State.LOOP || isPairFullyActive(tracked.pair))) {
            return;
        }

        SoundManager sm = Minecraft.getInstance().getSoundManager();
        if (tracked != null) {
            sm.stop(tracked.pair.normal);
            sm.stop(tracked.pair.distant);
        }

        var normal = new SirenSoundInstance(normalEvent, SoundSource.WEATHER, blockEntity, looping, 0f, 128f, false);
        var distant = new SirenSoundInstance(distantEvent, SoundSource.WEATHER, blockEntity, looping, 64f, 256f, true);
        sm.play(normal);
        sm.play(distant);
        ACTIVE_SOUNDS.put(pos, new TrackedSound(phase, new SoundPair(normal, distant)));
    }

    private static boolean isPairActive(SoundPair pair) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        return sm.isActive(pair.normal) || sm.isActive(pair.distant);
    }

    private static boolean isPairFullyActive(SoundPair pair) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        return sm.isActive(pair.normal) && sm.isActive(pair.distant);
    }

    private static State inferPhaseFromEvents(SoundEvent normalEvent, boolean looping) {
        if (looping) {
            return State.LOOP;
        }
        if (normalEvent == SoundRegistry.MECHANICAL_SIREN_END.get()) {
            return State.END;
        }
        return State.START;
    }

    private record TrackedSound(State phase, SoundPair pair) {}

    private record SoundPair(SirenSoundInstance normal, SirenSoundInstance distant) {}
}
