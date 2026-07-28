package destiny.null_ouroboros.server.ash;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
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
import java.util.Queue;

public class AshAtmosphere implements INBTSerializable<CompoundTag> {
    private static final String CLEAN_KEY = "Clean";

    private final LongOpenHashSet clean = new LongOpenHashSet();
    private final Queue<Long> ashFrontier = new ArrayDeque<>();
    private final Queue<Long> cleanFrontier = new ArrayDeque<>();
    private final LongOpenHashSet ashQueued = new LongOpenHashSet();
    private final LongOpenHashSet cleanQueued = new LongOpenHashSet();

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
        return true;
    }

    public boolean clearClean(BlockPos pos) {
        long key = pos.asLong();
        boolean removed = clean.remove(key);
        if (removed) {
            ashQueued.remove(key);
        }
        return removed;
    }

    public void enqueueAsh(BlockPos pos) {
        long key = pos.asLong();
        if (!clean.contains(key)) {
            return;
        }
        if (ashQueued.add(key)) {
            ashFrontier.add(key);
        }
    }

    public void injectAsh(Level level, BlockPos pos) {
        if (!AshAirtight.isAirCell(level, pos) || AshAirtight.isSkyExposed(level, pos)) {
            return;
        }
        long key = pos.asLong();
        if (clean.remove(key)) {
            ashQueued.remove(key);
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (!AshAirtight.canFlow(level, pos, direction)) {
                continue;
            }
            cursor.setWithOffset(pos, direction);
            if (isClean(cursor)) {
                enqueueAsh(cursor.immutable());
            }
        }
    }

    public void enqueueClean(BlockPos pos) {
        long key = pos.asLong();
        if (clean.contains(key)) {
            return;
        }
        if (cleanQueued.add(key)) {
            cleanFrontier.add(key);
        }
    }

    public void seedAshAtBreach(ServerLevel level, BlockPos changed) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return;
        }
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
                enqueueAsh(cursor.immutable());
            }
        }
        if (isClean(changed) && AshAirtight.isAirCell(level, changed) && isAshSourceNeighbor(level, changed)) {
            enqueueAsh(changed);
        }
    }

    public boolean isAshSourceNeighbor(Level level, BlockPos cleanPos) {
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
        return AshAirtight.isSkyExposed(level, cleanPos);
    }

    public void serverTick(ServerLevel level) {
        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            return;
        }

        if (!ashFrontier.isEmpty()) {
            advanceAsh(level);
            return;
        }
        if (!cleanFrontier.isEmpty()) {
            advanceClean(level);
        }
    }

    private void advanceAsh(ServerLevel level) {
        int scanned = 0;
        while (!ashFrontier.isEmpty() && scanned < AshAirtight.SPREAD_SCAN_LIMIT) {
            scanned++;
            long key = ashFrontier.poll();
            ashQueued.remove(key);
            BlockPos pos = BlockPos.of(key);
            if (!clean.contains(key)) {
                continue;
            }
            if (!AshAirtight.isAirCell(level, pos)) {
                clean.remove(key);
                return;
            }
            if (!isAshSourceNeighbor(level, pos) && !AshAirtight.isSkyExposed(level, pos)) {
                continue;
            }
            clean.remove(key);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                cursor.setWithOffset(pos, direction);
                if (isClean(cursor)) {
                    enqueueAsh(cursor.immutable());
                }
            }
            return;
        }
    }

    private void advanceClean(ServerLevel level) {
        int scanned = 0;
        while (!cleanFrontier.isEmpty() && scanned < AshAirtight.SPREAD_SCAN_LIMIT) {
            scanned++;
            long key = cleanFrontier.poll();
            cleanQueued.remove(key);
            BlockPos pos = BlockPos.of(key);
            if (clean.contains(key)) {
                continue;
            }
            if (!AshAirtight.isAirCell(level, pos) || AshAirtight.isSkyExposed(level, pos)) {
                continue;
            }
            clean.add(key);
            return;
        }
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
        if (!AshAirtight.isAirCell(level, seed)) {
            return EnclosureResult.open(0);
        }
        if (AshAirtight.isSkyExposed(level, seed)) {
            return EnclosureResult.open(0);
        }
        LongOpenHashSet visited = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        visited.add(seed.asLong());
        int cleanFound = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            if (visited.size() > AshAirtight.ENCLOSURE_CELL_LIMIT) {
                return EnclosureResult.open(cleanFound);
            }
            BlockPos pos = queue.poll();
            if (AshAirtight.isSkyExposed(level, pos)) {
                return EnclosureResult.open(cleanFound);
            }
            if (isClean(pos)) {
                cleanFound++;
            }
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                if (!level.isLoaded(cursor)) {
                    return EnclosureResult.open(cleanFound);
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
        return EnclosureResult.enclosed(visited.size(), cleanFound, visited);
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
        ashFrontier.clear();
        cleanFrontier.clear();
        ashQueued.clear();
        cleanQueued.clear();
        ListTag list = tag.getList(CLEAN_KEY, Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            clean.add(((LongTag) list.get(i)).getAsLong());
        }
    }

    public record EnclosureResult(boolean enclosed, int volume, int cleanCount, LongOpenHashSet cells) {
        public static EnclosureResult open(int cleanCount) {
            return new EnclosureResult(false, 0, cleanCount, new LongOpenHashSet());
        }

        public static EnclosureResult enclosed(int volume, int cleanCount, LongOpenHashSet cells) {
            return new EnclosureResult(true, volume, cleanCount, cells);
        }
    }
}
