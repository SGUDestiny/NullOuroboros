package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.NullOuroboros;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SteelLeviathanChunkTickets {
    private record ChainKey(ResourceKey<Level> dimension, UUID headUuid) {}

    private static final class Pending {
        final LongOpenHashSet chunks = new LongOpenHashSet();
        ServerLevel level;
    }

    private static final class ActiveState {
        final LongOpenHashSet tickets = new LongOpenHashSet();
    }

    private static final Map<ChainKey, ActiveState> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<ChainKey, Pending> PENDING = new ConcurrentHashMap<>();

    private SteelLeviathanChunkTickets() {
    }

    public static void register() {
        ForgeChunkManager.setForcedChunkLoadingCallback(NullOuroboros.MODID, (level, helper) -> {
            ResourceKey<Level> dim = level.dimension();
            ACTIVE.keySet().removeIf(key -> key.dimension().equals(dim));
            PENDING.keySet().removeIf(key -> key.dimension().equals(dim));
        });
    }

    public static void beginServerTick() {
        PENDING.clear();
    }

    public static void contribute(SteelLeviathanPartEntity part) {
        if (part.level().isClientSide || !(part.level() instanceof ServerLevel level)) {
            return;
        }
        UUID headUuid = resolveHeadUuid(part);
        if (headUuid == null) {
            return;
        }
        ChainKey key = new ChainKey(level.dimension(), headUuid);
        Pending pending = PENDING.computeIfAbsent(key, k -> new Pending());
        pending.level = level;

        long partChunk = ChunkPos.asLong(part.chunkPosition().x, part.chunkPosition().z);
        pending.chunks.add(partChunk);
        part.addChunkHints(pending.chunks);
        collectLoadedNeighborhood(part, pending.chunks);

        SteelLeviathanHeadEntity head = part instanceof SteelLeviathanHeadEntity h ? h : part.resolveHead();
        if (head != null) {
            head.rememberChainChunk(partChunk);
            part.addChunkHints(head.getChainChunkKeysMutable());

            if (part == head) {
                head.refreshChainChunksFromLoadedParts();
            }
            pending.chunks.addAll(head.getChainChunkKeys());
        }
    }

    public static void endServerTick(MinecraftServer server) {
        Set<ChainKey> committed = new HashSet<>();
        for (Map.Entry<ChainKey, Pending> entry : new ArrayList<>(PENDING.entrySet())) {
            commitContribution(entry.getKey(), entry.getValue());
            committed.add(entry.getKey());
        }
        PENDING.clear();

        for (ChainKey key : new ArrayList<>(ACTIVE.keySet())) {
            if (committed.contains(key)) {
                continue;
            }
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                ACTIVE.remove(key);
            }
        }
    }

    public static void release(ServerLevel level, UUID headUuid) {
        ChainKey key = new ChainKey(level.dimension(), headUuid);
        ActiveState state = ACTIVE.remove(key);
        PENDING.remove(key);
        if (state == null || state.tickets.isEmpty()) {
            return;
        }
        for (long chunkKey : state.tickets) {
            ChunkPos pos = new ChunkPos(chunkKey);
            ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, headUuid,
                    pos.x, pos.z, false, true);
        }
    }

    public static boolean isKeepAliveActive(UUID headUuid) {
        for (Map.Entry<ChainKey, ActiveState> entry : ACTIVE.entrySet()) {
            if (entry.getKey().headUuid().equals(headUuid) && !entry.getValue().tickets.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void commitContribution(ChainKey key, Pending pending) {
        ServerLevel level = pending.level;
        if (level == null) {
            return;
        }
        ActiveState previous = ACTIVE.get(key);
        LongOpenHashSet desired = new LongOpenHashSet(pending.chunks);
        if (previous != null) {
            desired.addAll(previous.tickets);
        }

        applyTickets(level, key.headUuid(), previous != null ? previous.tickets : LongSets.EMPTY_SET, desired);
        ActiveState state = ACTIVE.computeIfAbsent(key, k -> new ActiveState());
        state.tickets.clear();
        state.tickets.addAll(desired);
    }

    private static void applyTickets(ServerLevel level, UUID headUuid, LongSet previous, LongSet desired) {
        for (long chunkKey : desired) {
            if (!previous.contains(chunkKey)) {
                ChunkPos pos = new ChunkPos(chunkKey);
                ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, headUuid,
                        pos.x, pos.z, true, true);
            }
        }
        for (long chunkKey : previous) {
            if (!desired.contains(chunkKey)) {
                ChunkPos pos = new ChunkPos(chunkKey);
                ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, headUuid,
                        pos.x, pos.z, false, true);
            }
        }
    }

    private static UUID resolveHeadUuid(SteelLeviathanPartEntity part) {
        if (part instanceof SteelLeviathanHeadEntity) {
            return part.getUUID();
        }
        Optional<UUID> synced = part.getHeadUuid();
        if (synced.isPresent()) {
            return synced.get();
        }
        return part.savedHeadUuid;
    }

    private static void collectLoadedNeighborhood(SteelLeviathanPartEntity seed, LongOpenHashSet out) {
        Queue<SteelLeviathanPartEntity> queue = new ArrayDeque<>();
        Map<Integer, Boolean> seen = new HashMap<>();
        queue.add(seed);
        seen.put(seed.getId(), Boolean.TRUE);
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 64) {
            SteelLeviathanPartEntity current = queue.poll();
            out.add(ChunkPos.asLong(current.chunkPosition().x, current.chunkPosition().z));
            current.addChunkHints(out);
            enqueue(queue, seen, current.resolvePrev());
            enqueue(queue, seen, current.resolveNext());
            if (current instanceof SteelLeviathanHeadEntity head) {
                for (SteelLeviathanPartEntity part : head.collectParts()) {
                    enqueue(queue, seen, part);
                }
            } else {
                SteelLeviathanHeadEntity head = current.resolveHead();
                if (head != null) {
                    enqueue(queue, seen, head);
                }
            }
        }
    }

    private static void enqueue(Queue<SteelLeviathanPartEntity> queue, Map<Integer, Boolean> seen,
                                SteelLeviathanPartEntity part) {
        if (part == null || seen.containsKey(part.getId())) {
            return;
        }
        seen.put(part.getId(), Boolean.TRUE);
        queue.add(part);
    }
}
