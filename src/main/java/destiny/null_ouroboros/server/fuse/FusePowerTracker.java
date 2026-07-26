package destiny.null_ouroboros.server.fuse;

import destiny.null_ouroboros.NullOuroboros;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FusePowerTracker {
    private static final Map<ResourceKey<Level>, Object2IntMap<BlockPos>> POWERED = new ConcurrentHashMap<>();
    private static final Map<BoxKey, Set<BlockPos>> BOX_LINKS = new ConcurrentHashMap<>();
    private static final Map<BoxKey, LongSet> BOX_CHUNKS = new ConcurrentHashMap<>();

    private FusePowerTracker() {
    }

    public static boolean isPowered(Level level, BlockPos pos) {
        Object2IntMap<BlockPos> map = POWERED.get(level.dimension());
        return map != null && map.getInt(pos.immutable()) > 0;
    }

    public static void addPower(ServerLevel level, BlockPos fuseBoxPos, BlockPos linkedPos) {
        BlockPos immutableLinked = linkedPos.immutable();
        BlockPos immutableBox = fuseBoxPos.immutable();
        Object2IntMap<BlockPos> map = POWERED.computeIfAbsent(level.dimension(), k -> new Object2IntOpenHashMap<>());
        map.put(immutableLinked, map.getInt(immutableLinked) + 1);

        BoxKey key = new BoxKey(level.dimension(), immutableBox);
        BOX_LINKS.computeIfAbsent(key, k -> new HashSet<>()).add(immutableLinked);
        ensureChunk(level, immutableBox, immutableLinked);
        refreshNeighbors(level, immutableLinked);
    }

    public static void removePower(ServerLevel level, BlockPos fuseBoxPos, BlockPos linkedPos) {
        BlockPos immutableLinked = linkedPos.immutable();
        BlockPos immutableBox = fuseBoxPos.immutable();
        Object2IntMap<BlockPos> map = POWERED.get(level.dimension());
        if (map == null) {
            return;
        }

        int prev = map.getInt(immutableLinked);
        if (prev <= 1) {
            map.removeInt(immutableLinked);
            if (map.isEmpty()) {
                POWERED.remove(level.dimension());
            }
        } else {
            map.put(immutableLinked, prev - 1);
        }

        BoxKey key = new BoxKey(level.dimension(), immutableBox);
        Set<BlockPos> links = BOX_LINKS.get(key);
        if (links != null) {
            links.remove(immutableLinked);
            if (links.isEmpty()) {
                BOX_LINKS.remove(key);
            }
        }

        releaseChunkIfUnused(level, immutableBox, immutableLinked);
        refreshNeighbors(level, immutableLinked);
    }

    public static void clearBox(ServerLevel level, BlockPos fuseBoxPos) {
        BlockPos immutableBox = fuseBoxPos.immutable();
        BoxKey key = new BoxKey(level.dimension(), immutableBox);
        Set<BlockPos> links = BOX_LINKS.remove(key);
        if (links != null) {
            Object2IntMap<BlockPos> map = POWERED.get(level.dimension());
            for (BlockPos linked : links) {
                if (map != null) {
                    int prev = map.getInt(linked);
                    if (prev <= 1) {
                        map.removeInt(linked);
                    } else {
                        map.put(linked, prev - 1);
                    }
                }
                refreshNeighbors(level, linked);
            }
            if (map != null && map.isEmpty()) {
                POWERED.remove(level.dimension());
            }
        }

        LongSet chunks = BOX_CHUNKS.remove(key);
        if (chunks != null) {
            for (long chunk : chunks) {
                ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, immutableBox,
                        ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
            }
        }
    }

    private static void ensureChunk(ServerLevel level, BlockPos fuseBoxPos, BlockPos linkedPos) {
        BoxKey key = new BoxKey(level.dimension(), fuseBoxPos);
        long chunkKey = ChunkPos.asLong(linkedPos);
        LongSet chunks = BOX_CHUNKS.computeIfAbsent(key, k -> new LongOpenHashSet());
        if (chunks.add(chunkKey)) {
            ChunkPos chunkPos = new ChunkPos(linkedPos);
            ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, fuseBoxPos, chunkPos.x, chunkPos.z, true, true);
        }
    }

    private static void releaseChunkIfUnused(ServerLevel level, BlockPos fuseBoxPos, BlockPos linkedPos) {
        BoxKey key = new BoxKey(level.dimension(), fuseBoxPos);
        long chunkKey = ChunkPos.asLong(linkedPos);
        LongSet chunks = BOX_CHUNKS.get(key);
        if (chunks == null) {
            return;
        }

        Set<BlockPos> remaining = BOX_LINKS.get(key);
        boolean stillNeeded = remaining != null && remaining.stream().anyMatch(pos -> ChunkPos.asLong(pos) == chunkKey);
        if (!stillNeeded && chunks.remove(chunkKey)) {
            ChunkPos chunkPos = new ChunkPos(linkedPos);
            ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, fuseBoxPos, chunkPos.x, chunkPos.z, false, true);
            if (chunks.isEmpty()) {
                BOX_CHUNKS.remove(key);
            }
        }
    }

    private static void refreshNeighbors(ServerLevel level, BlockPos pos) {
        level.neighborChanged(pos, Blocks.AIR, pos);
        level.updateNeighborsAt(pos, Blocks.AIR);
    }

    private record BoxKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
