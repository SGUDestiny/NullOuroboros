package destiny.null_ouroboros.server.manifolding;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public final class ManifoldingChunkErasure {
    private static final int CHECKS_PER_TICK = 2048;
    private static final int CONVERTS_PER_TICK = 48;

    private static final Queue<Job> QUEUE = new ArrayDeque<>();

    private ManifoldingChunkErasure() {
    }

    private static final class Job {
        final ServerLevel level;
        final long chunkKey;
        final float chance;
        final int minSection;
        final int sectionCount;
        int sectionIdx;
        int x;
        int y;
        int z;
        boolean marked;

        Job(ServerLevel level, LevelChunk chunk, float chance) {
            this.level = level;
            this.chunkKey = chunk.getPos().toLong();
            this.chance = chance;
            this.minSection = chunk.getMinSection();
            this.sectionCount = chunk.getMaxSection() - this.minSection;
        }
    }

    public static void enqueue(ServerLevel level, LevelChunk chunk, ManifoldingCapability cap) {
        if (cap.getPhase() != ManifoldingPhase.ACTIVE) {
            return;
        }

        long elapsed = level.getGameTime() - cap.getPhaseStartTime();
        float chance = Mth.clamp((float) elapsed / Math.max(1, cap.getActiveDuration()), 0.0f, 1.0f);
        if (chance <= 0) {
            cap.markChunkEroded(chunk.getPos().toLong());
            return;
        }

        long chunkKey = chunk.getPos().toLong();
        for (Job existing : QUEUE) {
            if (existing.chunkKey == chunkKey && existing.level == level) {
                return;
            }
        }

        QUEUE.add(new Job(level, chunk, chance));
    }

    public static void processNewChunk(ServerLevel level, LevelChunk chunk, ManifoldingCapability cap) {
        enqueue(level, chunk, cap);
    }

    public static void tick(ServerLevel level, ManifoldingCapability cap) {
        if (QUEUE.isEmpty()) {
            return;
        }

        int checksLeft = CHECKS_PER_TICK;
        int convertsLeft = CONVERTS_PER_TICK;

        Iterator<Job> iterator = QUEUE.iterator();
        while (iterator.hasNext() && checksLeft > 0 && convertsLeft > 0) {
            Job job = iterator.next();
            if (job.level != level) {
                continue;
            }
            int[] budget = {checksLeft, convertsLeft};
            if (advance(job, cap, budget)) {
                iterator.remove();
            }
            checksLeft = budget[0];
            convertsLeft = budget[1];
        }
    }

    private static boolean advance(Job job, ManifoldingCapability cap, int[] budget) {
        LevelChunk chunk = job.level.getChunkSource().getChunkNow(
                ChunkPos.getX(job.chunkKey), ChunkPos.getZ(job.chunkKey));
        if (chunk == null) {
            markDone(job, cap);
            return true;
        }

        RandomSource random = job.level.getRandom();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        while (job.sectionIdx < job.sectionCount) {
            LevelChunkSection section = chunk.getSection(job.sectionIdx);
            if (section.hasOnlyAir()
                    || !section.getStates().maybeHas(state -> !state.is(ManifoldingCapability.ISNT_CONVERTED_BY_MANIFOLDING))) {
                job.sectionIdx++;
                job.x = 0;
                job.y = 0;
                job.z = 0;
                continue;
            }

            int sectionY = (job.minSection + job.sectionIdx) * 16;

            while (job.x < 16) {
                while (job.y < 16) {
                    while (job.z < 16) {
                        if (budget[0] <= 0 || budget[1] <= 0) {
                            return false;
                        }
                        budget[0]--;

                        int localZ = job.z;
                        BlockState state = section.getBlockState(job.x, job.y, localZ);
                        job.z++;

                        if (state.is(ManifoldingCapability.ISNT_CONVERTED_BY_MANIFOLDING)) {
                            continue;
                        }

                        pos.set(chunkX + job.x, sectionY + job.y, chunkZ + localZ);
                        int surfaceY = job.level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
                        if (pos.getY() < surfaceY) {
                            continue;
                        }
                        if (!ManifoldingErasure.hasExposedFace(job.level, pos)) {
                            continue;
                        }
                        if (random.nextFloat() >= job.chance) {
                            continue;
                        }

                        ManifoldingErasure.convertBlock(job.level, pos, state);
                        budget[1]--;
                    }
                    job.z = 0;
                    job.y++;
                }
                job.y = 0;
                job.x++;
            }

            job.x = 0;
            job.y = 0;
            job.z = 0;
            job.sectionIdx++;
        }

        markDone(job, cap);
        return true;
    }

    private static void markDone(Job job, ManifoldingCapability cap) {
        if (!job.marked) {
            cap.markChunkEroded(job.chunkKey);
            job.marked = true;
        }
    }
}
