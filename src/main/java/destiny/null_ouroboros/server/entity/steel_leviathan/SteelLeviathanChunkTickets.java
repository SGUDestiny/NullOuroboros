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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SteelLeviathanChunkTickets {
    private record ChainKey(ResourceKey<Level> dimension, UUID headUuid) {
    }

    private static final class Pending {
        final LongOpenHashSet chunks = new LongOpenHashSet();
        ServerLevel level;
        boolean headRebuilt;
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
            for (UUID owner : new ArrayList<>(helper.getEntityTickets().keySet())) {
                helper.removeAllTickets(owner);
            }
            ResourceKey<Level> dim = level.dimension();
            ACTIVE.keySet().removeIf(key -> key.dimension().equals(dim));
            PENDING.keySet().removeIf(key -> key.dimension().equals(dim));
        });
    }

    public static void beginServerTick() {
        PENDING.clear();
    }

    public static void forceChunkNow(ServerLevel level, UUID headUuid, double x, double z) {
        ChunkPos pos = new ChunkPos((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        forceChunkNow(level, headUuid, ChunkPos.asLong(pos.x, pos.z));
    }

    public static void forceChunkNow(ServerLevel level, UUID headUuid, long chunkKey) {
        ChainKey key = new ChainKey(level.dimension(), headUuid);
        ActiveState state = ACTIVE.computeIfAbsent(key, k -> new ActiveState());
        if (state.tickets.add(chunkKey)) {
            ChunkPos pos = new ChunkPos(chunkKey);
            ForgeChunkManager.forceChunk(level, NullOuroboros.MODID, headUuid,
                    pos.x, pos.z, true, true);
        }
        Pending pending = PENDING.computeIfAbsent(key, k -> new Pending());
        pending.level = level;
        pending.chunks.add(chunkKey);
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

        pending.chunks.add(ChunkPos.asLong(part.chunkPosition().x, part.chunkPosition().z));
        part.addChunkHints(pending.chunks);

        if (part instanceof SteelLeviathanHeadEntity head) {
            if (!pending.headRebuilt) {
                pending.headRebuilt = true;
                for (SteelLeviathanPartEntity chainPart : head.collectParts()) {
                    pending.chunks.add(ChunkPos.asLong(chainPart.chunkPosition().x, chainPart.chunkPosition().z));
                    chainPart.addChunkHints(pending.chunks);
                }
                head.refreshChainChunksFromLoadedParts();
            }
        } else {
            SteelLeviathanHeadEntity head = part.resolveHead();
            if (head != null) {
                pending.chunks.addAll(head.getChainChunkKeys());
            }
        }
    }

    public static void contributeChain(SteelLeviathanHeadEntity head, Iterable<SteelLeviathanPartEntity> parts) {
        if (head.level().isClientSide || !(head.level() instanceof ServerLevel level)) {
            return;
        }
        ChainKey key = new ChainKey(level.dimension(), head.getUUID());
        Pending pending = PENDING.computeIfAbsent(key, k -> new Pending());
        pending.level = level;
        pending.headRebuilt = true;
        for (SteelLeviathanPartEntity part : parts) {
            pending.chunks.add(ChunkPos.asLong(part.chunkPosition().x, part.chunkPosition().z));
            part.addChunkHints(pending.chunks);
        }
        head.refreshChainChunksFromLoadedParts();
        commitContribution(key, pending);
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
                continue;
            }
            release(level, key.headUuid());
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
        LongOpenHashSet desired = pending.chunks;

        if (previous != null && previous.tickets.equals(desired)) {
            return;
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
}
