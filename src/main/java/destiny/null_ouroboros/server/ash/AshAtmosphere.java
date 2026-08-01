package destiny.null_ouroboros.server.ash;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class AshAtmosphere implements INBTSerializable<CompoundTag> {
    private static final String CLEAN_KEY = "Clean";

    private final LongOpenHashSet clean = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<RoomWorkspace> rooms = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<EnclosureResult> enclosureBySeed = new Long2ObjectOpenHashMap<>();
    private final Long2BooleanOpenHashMap exteriorAshMemo = new Long2BooleanOpenHashMap();

    public boolean isClean(BlockPos pos) {
        return clean.contains(pos.asLong());
    }

    public boolean isClean(long packed) {
        return clean.contains(packed);
    }

    public boolean isAshyAir(Level level, BlockPos pos) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return false;
        }
        if (!AshAirtight.isAirCell(level, pos)) {
            return false;
        }
        if (AshAirtight.isSkyExposed(level, pos)) {
            return true;
        }
        return !isClean(pos);
    }

    public int cleanCount() {
        return clean.size();
    }

    public boolean trySetClean(ServerLevel level, BlockPos pos) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return false;
        }
        if (!AshAirtight.isAirCell(level, pos) || AshAirtight.isSkyExposed(level, pos)) {
            return false;
        }
        long key = pos.asLong();
        if (clean.contains(key)) {
            return false;
        }
        clean.add(key);
        invalidateExteriorAshMemo();
        return true;
    }

    public boolean clearClean(BlockPos pos) {
        long key = pos.asLong();
        boolean removed = clean.remove(key);
        if (removed) {
            invalidateExteriorAshMemo();
        }
        return removed;
    }

    public void enqueueAsh(BlockPos pos) {
        enqueueAsh((RoomWorkspace) null, pos);
    }

    public void enqueueAsh(Level level, BlockPos pos) {
        enqueueAsh(level == null ? null : workspaceFor(level, pos), pos);
    }

    public void injectAsh(Level level, BlockPos pos) {
        if (!AshAirtight.isAirCell(level, pos) || AshAirtight.isSkyExposed(level, pos)) {
            return;
        }
        RoomWorkspace room = workspaceFor(level, pos);
        enqueueAsh(room, pos);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (!AshAirtight.canFlow(level, pos, direction)) {
                continue;
            }
            cursor.setWithOffset(pos, direction);
            if (isClean(cursor)) {
                enqueueAsh(room, cursor.immutable());
            }
        }
    }

    public boolean enqueueClean(BlockPos pos) {
        return enqueueClean((RoomWorkspace) null, pos);
    }

    public boolean enqueueClean(Level level, BlockPos pos) {
        return enqueueClean(level == null ? null : workspaceFor(level, pos), pos);
    }

    private void enqueueAsh(RoomWorkspace room, BlockPos pos) {
        long key = pos.asLong();
        if (!clean.contains(key)) {
            return;
        }
        workspaceOrFallback(room, pos).enqueueAsh(key);
    }

    private boolean enqueueClean(RoomWorkspace room, BlockPos pos) {
        long key = pos.asLong();
        return !clean.contains(key) && workspaceOrFallback(room, pos).enqueueClean(key);
    }

    private RoomWorkspace workspaceOrFallback(RoomWorkspace room, BlockPos pos) {
        if (room != null) {
            return room;
        }
        return rooms.computeIfAbsent(pos.asLong(), RoomWorkspace::new);
    }

    private RoomWorkspace workspaceFor(Level level, BlockPos pos) {
        EnclosureResult space = inspectEnclosure(level, pos);
        long key = space.roomKey() != Long.MIN_VALUE ? space.roomKey() : pos.asLong();
        return rooms.computeIfAbsent(key, RoomWorkspace::new);
    }

    public void seedAshAtBreach(ServerLevel level, BlockPos changed) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return;
        }
        invalidateAt(changed);
        if (clean.isEmpty()) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            cursor.setWithOffset(changed, direction);
            if (!isClean(cursor)) {
                continue;
            }
            if (isAshSourceNeighbor(level, cursor)) {
                enqueueAsh(level, cursor.immutable());
            }
        }
        if (isClean(changed) && AshAirtight.isAirCell(level, changed) && isAshSourceNeighbor(level, changed)) {
            enqueueAsh(level, changed);
        }
    }

    public boolean isExteriorAsh(Level level, BlockPos seed) {
        if (!isAshyAir(level, seed)) {
            return false;
        }
        long seedKey = seed.asLong();
        if (exteriorAshMemo.containsKey(seedKey)) {
            return exteriorAshMemo.get(seedKey);
        }
        if (AshAirtight.isSkyExposed(level, seed)) {
            exteriorAshMemo.put(seedKey, true);
            return true;
        }

        LongOpenHashSet visited = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seedKey);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean exterior = false;

        while (!queue.isEmpty()) {
            if (visited.size() > AshAirtight.EXTERIOR_SCAN_LIMIT) {
                exterior = true;
                break;
            }
            BlockPos pos = queue.poll();
            if (AshAirtight.isSkyExposed(level, pos)) {
                exterior = true;
                break;
            }
            for (Direction direction : Direction.values()) {
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                cursor.setWithOffset(pos, direction);
                if (!level.isLoaded(cursor)) {
                    exterior = true;
                    break;
                }
                if (!isAshyAir(level, cursor)) {
                    continue;
                }
                long key = cursor.asLong();
                if (!visited.add(key)) {
                    continue;
                }
                queue.add(cursor.immutable());
            }
            if (exterior) {
                break;
            }
        }

        for (long key : visited) {
            exteriorAshMemo.put(key, exterior);
        }
        return exterior;
    }

    public boolean isAshSourceNeighbor(Level level, BlockPos cleanPos) {
        if (AshAirtight.isSkyExposed(level, cleanPos)) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (!AshAirtight.canFlow(level, cleanPos, direction)) {
                continue;
            }
            cursor.setWithOffset(cleanPos, direction);
            if (isExteriorAsh(level, cursor)) {
                return true;
            }
        }
        return false;
    }

    private boolean touchesAshyAir(Level level, BlockPos cleanPos) {
        if (AshAirtight.isSkyExposed(level, cleanPos)) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (!AshAirtight.canFlow(level, cleanPos, direction)) {
                continue;
            }
            cursor.setWithOffset(cleanPos, direction);
            if (isAshyAir(level, cursor)) {
                return true;
            }
        }
        return false;
    }

    private void invalidateExteriorAshMemo() {
        exteriorAshMemo.clear();
    }

    public void invalidateAt(BlockPos changed) {
        LongOpenHashSet affected = new LongOpenHashSet();
        for (var entry : enclosureBySeed.long2ObjectEntrySet()) {
            EnclosureResult space = entry.getValue();
            if (space.cells().contains(changed.asLong())) {
                affected.add(entry.getLongKey());
                rooms.remove(space.roomKey());
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (space.cells().contains(changed.relative(direction).asLong())) {
                    affected.add(entry.getLongKey());
                    rooms.remove(space.roomKey());
                    break;
                }
            }
        }
        for (long key : affected) {
            enclosureBySeed.remove(key);
        }
        invalidateExteriorAshMemo();
    }

    public void serverTick(ServerLevel level) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return;
        }

        Iterator<RoomWorkspace> iterator = rooms.values().iterator();
        while (iterator.hasNext()) {
            RoomWorkspace room = iterator.next();
            int remaining = AshAirtight.ROOM_MUTATIONS_PER_TICK;
            remaining -= advanceAsh(level, room, remaining);
            if (remaining > 0) {
                advanceClean(level, room, remaining);
            }
            if (room.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private int advanceAsh(ServerLevel level, RoomWorkspace room, int limit) {
        int scanned = 0;
        int changed = 0;
        while (!room.ashFrontier.isEmpty() && scanned < AshAirtight.SPREAD_SCAN_LIMIT && changed < limit) {
            scanned++;
            long key = room.ashFrontier.poll();
            room.ashQueued.remove(key);
            BlockPos pos = BlockPos.of(key);
            if (!clean.contains(key)) {
                continue;
            }
            if (!AshAirtight.isAirCell(level, pos)) {
                clean.remove(key);
                invalidateExteriorAshMemo();
                changed++;
                continue;
            }
            if (!touchesAshyAir(level, pos)) {
                continue;
            }
            clean.remove(key);
            invalidateExteriorAshMemo();
            changed++;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                cursor.setWithOffset(pos, direction);
                if (isClean(cursor)) {
                    enqueueAsh(room, cursor.immutable());
                }
            }
        }
        return changed;
    }

    private int advanceClean(ServerLevel level, RoomWorkspace room, int limit) {
        int scanned = 0;
        int changed = 0;
        while (!room.cleanFrontier.isEmpty() && scanned < AshAirtight.SPREAD_SCAN_LIMIT && changed < limit) {
            scanned++;
            long key = room.cleanFrontier.poll();
            room.cleanQueued.remove(key);
            BlockPos pos = BlockPos.of(key);
            if (clean.contains(key)) {
                continue;
            }
            if (!AshAirtight.isAirCell(level, pos) || AshAirtight.isSkyExposed(level, pos)) {
                continue;
            }
            clean.add(key);
            invalidateExteriorAshMemo();
            changed++;
        }
        return changed;
    }

    public int countCleanInSpace(Level level, BlockPos seed, int limit) {
        if (!AshAirtight.isAirCell(level, seed)) {
            return 0;
        }
        LongOpenHashSet visited = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.asLong());
        int cleanFound = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty() && visited.size() <= limit) {
            BlockPos pos = queue.poll();
            if (isClean(pos)) {
                cleanFound++;
            }
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                if (!level.isLoaded(cursor)) {
                    continue;
                }
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                long key = cursor.asLong();
                if (!visited.add(key)) {
                    continue;
                }
                queue.add(cursor.immutable());
            }
        }
        return cleanFound;
    }

    public EnclosureResult inspectEnclosure(Level level, BlockPos seed) {
        EnclosureResult cached = enclosureBySeed.get(seed.asLong());
        if (cached != null) {
            return cached;
        }
        if (!AshAirtight.isAirCell(level, seed)) {
            return EnclosureResult.open(0, 0, new LongOpenHashSet());
        }
        if (AshAirtight.isSkyExposed(level, seed)) {
            return EnclosureResult.open(0, 0, new LongOpenHashSet());
        }
        LongOpenHashSet visited = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.asLong());
        int cleanFound = 0;
        boolean open = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            if (visited.size() > AshAirtight.ENCLOSURE_CELL_LIMIT) {
                open = true;
                break;
            }
            BlockPos pos = queue.poll();
            if (isClean(pos)) {
                cleanFound++;
            }
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                if (!level.isLoaded(cursor)) {
                    open = true;
                    continue;
                }
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                if (AshAirtight.isSkyExposed(level, cursor)) {
                    open = true;
                    continue;
                }
                if (!isClean(cursor) && isExteriorAsh(level, cursor)) {
                    open = true;
                    continue;
                }
                long key = cursor.asLong();
                if (!visited.add(key)) {
                    continue;
                }
                queue.add(cursor.immutable());
            }
        }
        long roomKey = Long.MAX_VALUE;
        for (long key : visited) {
            roomKey = Math.min(roomKey, key);
        }
        EnclosureResult result = open
                ? EnclosureResult.open(roomKey, visited.size(), cleanFound, visited)
                : EnclosureResult.enclosed(roomKey, visited.size(), cleanFound, visited);
        if (!open) {
            for (long key : visited) {
                enclosureBySeed.put(key, result);
            }
        }
        return result;
    }

    public boolean reachesExterior(Level level, BlockPos seed) {
        if (!AshAirtight.isAirCell(level, seed)) {
            return false;
        }
        if (AshAirtight.isSkyExposed(level, seed)) {
            return true;
        }
        LongOpenHashSet visited = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.asLong());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            if (visited.size() > AshAirtight.EXTERIOR_SCAN_LIMIT) {
                return true;
            }
            BlockPos pos = queue.poll();
            if (AshAirtight.isSkyExposed(level, pos)) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                if (!level.isLoaded(cursor)) {
                    return true;
                }
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                long key = cursor.asLong();
                if (!visited.add(key)) {
                    continue;
                }
                queue.add(cursor.immutable());
            }
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (long key : clean) {
            list.add(LongTag.valueOf(key));
        }
        tag.put(CLEAN_KEY, list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        clean.clear();
        rooms.clear();
        enclosureBySeed.clear();
        invalidateExteriorAshMemo();
        ListTag list = tag.getList(CLEAN_KEY, Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            clean.add(((LongTag) list.get(i)).getAsLong());
        }
    }

    public record EnclosureResult(boolean enclosed, long roomKey, int volume, int cleanCount, LongOpenHashSet cells) {
        public static EnclosureResult open(int volume, int cleanCount, LongOpenHashSet cells) {
            return open(Long.MIN_VALUE, volume, cleanCount, cells);
        }

        public static EnclosureResult open(long roomKey, int volume, int cleanCount, LongOpenHashSet cells) {
            return new EnclosureResult(false, roomKey, volume, cleanCount, cells);
        }

        public static EnclosureResult enclosed(long roomKey, int volume, int cleanCount, LongOpenHashSet cells) {
            return new EnclosureResult(true, roomKey, volume, cleanCount, cells);
        }
    }

    private static final class RoomWorkspace {
        private final long key;
        private final Queue<Long> ashFrontier = new ArrayDeque<>();
        private final Queue<Long> cleanFrontier = new ArrayDeque<>();
        private final LongOpenHashSet ashQueued = new LongOpenHashSet();
        private final LongOpenHashSet cleanQueued = new LongOpenHashSet();

        private RoomWorkspace(long key) {
            this.key = key;
        }

        private void enqueueAsh(long packed) {
            if (ashQueued.add(packed)) {
                ashFrontier.add(packed);
            }
        }

        private boolean enqueueClean(long packed) {
            if (!cleanQueued.add(packed)) {
                return false;
            }
            cleanFrontier.add(packed);
            return true;
        }

        private boolean isEmpty() {
            return ashFrontier.isEmpty() && cleanFrontier.isEmpty();
        }
    }
}
