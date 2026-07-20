package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanBehaviorState;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.List;

public final class SteelLeviathanBossMusicManager {
    private static final float MAX_VOLUME = 0.2F;
    private static final float FADE_SPEED = MAX_VOLUME / (1.5F * 20.0F);
    private static final double RANGE_BLOCKS = 128.0D;
    private static final double RANGE_SQR = RANGE_BLOCKS * RANGE_BLOCKS;

    private static final int PHASE_1_INTRO_TICKS = 501;

    private static final int PHASE_2_INTRO_TICKS = 253;

    private enum Stage {
        SILENT,
        PHASE1_INTRO,
        PHASE1_LOOP,
        PHASE2_INTRO,
        PHASE2_LOOP,
        OUTRO
    }

    @Nullable private static FightMusic state;

    private SteelLeviathanBossMusicManager() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }

        SteelLeviathanHeadEntity head = findFightHead(level, player);
        if (head == null) {
            if (state != null) {
                fadeAndClearIfDone(mc);
            }
            return;
        }

        if (state == null || state.headId != head.getId()) {
            stopAll();
            state = new FightMusic(head.getId());
        }

        boolean inRange = player.distanceToSqr(head) <= RANGE_SQR;
        if (!inRange) {
            tickOutOfRange(mc);
            return;
        }

        if (head.isDying()) {
            tickDying(mc);
            return;
        }

        if (head.getBehaviorState() != SteelLeviathanBehaviorState.BOSSFIGHT) {
            fadeAndClearIfDone(mc);
            return;
        }

        if (head.isPhaseTwo()) {
            tickPhaseTwo(mc);
        } else {
            tickPhaseOne(mc);
        }

        tickOutgoing(mc);
    }

    public static void stopAll() {
        Minecraft mc = Minecraft.getInstance();
        if (state != null) {
            stopInstance(mc, state.current);
            stopInstance(mc, state.outgoing);
            state = null;
        }
    }

    private static SteelLeviathanHeadEntity findFightHead(ClientLevel level, LocalPlayer player) {
        AABB search = player.getBoundingBox().inflate(RANGE_BLOCKS + 32.0D);
        List<SteelLeviathanHeadEntity> heads = level.getEntitiesOfClass(
                SteelLeviathanHeadEntity.class, search, head -> head.isAlive()
                        && (head.getBehaviorState() == SteelLeviathanBehaviorState.BOSSFIGHT || head.isDying()));

        SteelLeviathanHeadEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (SteelLeviathanHeadEntity head : heads) {
            double dist = player.distanceToSqr(head);
            if (dist < bestDist) {
                bestDist = dist;
                best = head;
            }
        }
        return best;
    }

    private static void tickPhaseOne(Minecraft mc) {
        FightMusic s = state;
        if (s.stage == Stage.PHASE2_INTRO || s.stage == Stage.PHASE2_LOOP || s.stage == Stage.OUTRO) {
            maintainCurrent(mc);
            return;
        }

        if (s.phase1IntroDone || s.stage == Stage.PHASE1_LOOP) {
            ensureLoop(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_1_LOOP.get(), Stage.PHASE1_LOOP);
            return;
        }

        if (s.stage != Stage.PHASE1_INTRO) {
            startClip(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_1_INTRO.get(), false, false);
            s.stage = Stage.PHASE1_INTRO;
            s.introTicks = 0;
        } else {
            ensureIntroPlaying(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_1_INTRO.get());
        }

        if (isCurrentActive(mc)) {
            s.introTicks++;
        }
        maintainCurrent(mc);
        if (s.introTicks >= PHASE_1_INTRO_TICKS) {
            s.phase1IntroDone = true;

            cutTo(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_1_LOOP.get(), true);
            s.stage = Stage.PHASE1_LOOP;
        }
    }

    private static void tickPhaseTwo(Minecraft mc) {
        FightMusic s = state;
        if (s.stage == Stage.OUTRO) {
            return;
        }

        if (s.phase2IntroDone || s.stage == Stage.PHASE2_LOOP) {
            ensureLoop(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_2_LOOP.get(), Stage.PHASE2_LOOP);
            return;
        }

        if (s.stage != Stage.PHASE2_INTRO) {
            crossfadeTo(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_2_INTRO.get(), false);
            s.stage = Stage.PHASE2_INTRO;
            s.introTicks = 0;
            s.phase1IntroDone = true;
        } else {
            ensureIntroPlaying(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_2_INTRO.get());
        }

        if (isCurrentActive(mc)) {
            s.introTicks++;
        }
        maintainCurrent(mc);
        if (s.introTicks >= PHASE_2_INTRO_TICKS) {
            s.phase2IntroDone = true;

            cutTo(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_2_LOOP.get(), true);
            s.stage = Stage.PHASE2_LOOP;
        }
    }

    private static void tickDying(Minecraft mc) {
        FightMusic s = state;
        if (s.stage != Stage.OUTRO) {
            if (s.current != null && !s.current.isStopped()) {
                s.current.setTargetVolume(0.0F);
                if (s.outgoing != null && !s.outgoing.isStopped()) {
                    stopInstance(mc, s.outgoing);
                }
                s.outgoing = s.current;
            }
            ManifoldingSoundInstance outro = new ManifoldingSoundInstance(
                    SoundRegistry.STEEL_LEVIATHAN_PHASE_2_OUTRO.get(), SoundSource.MUSIC, false, FADE_SPEED);
            outro.forceVolume(MAX_VOLUME);
            s.current = outro;
            s.stage = Stage.OUTRO;
            s.playGraceTicks = 10;
            mc.getSoundManager().play(outro);
            return;
        }

        if (s.current == null || s.current.isStopped() || !mc.getSoundManager().isActive(s.current)) {
            stopInstance(mc, s.current);
            stopInstance(mc, s.outgoing);
            state = null;
            return;
        }
        s.current.setTargetVolume(MAX_VOLUME);
        tickOutgoing(mc);
    }

    private static void tickOutOfRange(Minecraft mc) {
        FightMusic s = state;
        if (s.current != null) {
            s.current.setTargetVolume(0.0F);
            if (s.current.isStopped()) {
                stopInstance(mc, s.current);
                s.current = null;
            }
        }
        if (s.outgoing != null) {
            s.outgoing.setTargetVolume(0.0F);
            if (s.outgoing.isStopped()) {
                stopInstance(mc, s.outgoing);
                s.outgoing = null;
            }
        }
        if (s.current == null && s.outgoing == null) {

            s.stage = Stage.SILENT;
        }
    }

    private static void ensureLoop(Minecraft mc, SoundEvent loop, Stage stage) {
        FightMusic s = state;
        if (s.stage == Stage.SILENT || s.current == null || s.current.isStopped()
                || !mc.getSoundManager().isActive(s.current)) {

            startClip(mc, loop, true, false);
            s.stage = stage;
            return;
        }
        if (s.stage != stage) {
            crossfadeTo(mc, loop, true);
            s.stage = stage;
        } else {
            s.current.setTargetVolume(MAX_VOLUME);
        }
    }

    private static void ensureIntroPlaying(Minecraft mc, SoundEvent intro) {
        FightMusic s = state;
        if (s.playGraceTicks > 0) {
            s.playGraceTicks--;
            return;
        }
        if (s.current == null || s.current.isStopped() || !mc.getSoundManager().isActive(s.current)) {
            startClip(mc, intro, false, false);
            s.introTicks = 0;
        }
    }

    private static boolean isCurrentActive(Minecraft mc) {
        FightMusic s = state;
        return s.current != null && !s.current.isStopped() && mc.getSoundManager().isActive(s.current);
    }

    private static void maintainCurrent(Minecraft mc) {
        FightMusic s = state;
        if (s.current != null && !s.current.isStopped()) {
            s.current.setTargetVolume(MAX_VOLUME);
            if (!mc.getSoundManager().isActive(s.current) && s.stage != Stage.OUTRO) {
                if (s.stage == Stage.PHASE1_LOOP) {
                    startClip(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_1_LOOP.get(), true, false);
                } else if (s.stage == Stage.PHASE2_LOOP) {
                    startClip(mc, SoundRegistry.STEEL_LEVIATHAN_PHASE_2_LOOP.get(), true, false);
                }
            }
        }
    }

    private static void startClip(Minecraft mc, SoundEvent event, boolean looping, boolean fadeIn) {
        FightMusic s = state;
        if (s.current != null && !s.current.isStopped()) {
            s.current.setTargetVolume(0.0F);
            if (s.outgoing != null && !s.outgoing.isStopped()) {
                stopInstance(mc, s.outgoing);
            }
            s.outgoing = s.current;
        }
        ManifoldingSoundInstance clip = new ManifoldingSoundInstance(event, SoundSource.MUSIC, looping, FADE_SPEED);
        if (fadeIn) {
            clip.forceVolume(0.0F);
            clip.setTargetVolume(MAX_VOLUME);
        } else {
            clip.forceVolume(MAX_VOLUME);
        }
        s.current = clip;
        s.playGraceTicks = 10;
        mc.getSoundManager().play(clip);
    }

    private static void crossfadeTo(Minecraft mc, SoundEvent event, boolean looping) {
        FightMusic s = state;
        if (s.current != null && !s.current.isStopped()) {
            s.current.setTargetVolume(0.0F);
            if (s.outgoing != null && !s.outgoing.isStopped()) {
                stopInstance(mc, s.outgoing);
            }
            s.outgoing = s.current;
        }
        ManifoldingSoundInstance clip = new ManifoldingSoundInstance(event, SoundSource.MUSIC, looping, FADE_SPEED);
        clip.forceVolume(0.0F);
        clip.setTargetVolume(MAX_VOLUME);
        s.current = clip;
        s.playGraceTicks = 10;
        mc.getSoundManager().play(clip);
    }

    private static void cutTo(Minecraft mc, SoundEvent event, boolean looping) {
        FightMusic s = state;
        if (s.current != null) {
            stopInstance(mc, s.current);
            s.current = null;
        }
        if (s.outgoing != null) {
            stopInstance(mc, s.outgoing);
            s.outgoing = null;
        }
        ManifoldingSoundInstance clip = new ManifoldingSoundInstance(event, SoundSource.MUSIC, looping, FADE_SPEED);
        clip.forceVolume(MAX_VOLUME);
        s.current = clip;
        s.playGraceTicks = 10;
        mc.getSoundManager().play(clip);
    }

    private static void tickOutgoing(Minecraft mc) {
        FightMusic s = state;
        if (s == null || s.outgoing == null) {
            return;
        }
        if (s.outgoing.isStopped()) {
            s.outgoing = null;
        }
    }

    private static void fadeAndClearIfDone(Minecraft mc) {
        FightMusic s = state;
        if (s.current != null) {
            s.current.setTargetVolume(0.0F);
            if (s.current.isStopped()) {
                stopInstance(mc, s.current);
                s.current = null;
            }
        }
        if (s.outgoing != null) {
            s.outgoing.setTargetVolume(0.0F);
            if (s.outgoing.isStopped()) {
                stopInstance(mc, s.outgoing);
                s.outgoing = null;
            }
        }
        if (s.current == null && s.outgoing == null) {
            state = null;
        }
    }

    private static void stopInstance(Minecraft mc, @Nullable ManifoldingSoundInstance instance) {
        if (instance != null) {
            mc.getSoundManager().stop(instance);
        }
    }

    private static final class FightMusic {
        private final int headId;
        private Stage stage = Stage.SILENT;
        @Nullable private ManifoldingSoundInstance current;
        @Nullable private ManifoldingSoundInstance outgoing;
        private int introTicks;
        private int playGraceTicks;
        private boolean phase1IntroDone;
        private boolean phase2IntroDone;

        private FightMusic(int headId) {
            this.headId = headId;
        }
    }
}

