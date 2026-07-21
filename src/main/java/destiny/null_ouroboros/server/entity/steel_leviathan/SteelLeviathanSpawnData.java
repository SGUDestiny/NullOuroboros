package destiny.null_ouroboros.server.entity.steel_leviathan;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class SteelLeviathanSpawnData extends SavedData {
    private static final String DATA_NAME = "steel_leviathan_spawn";

    private final LongOpenHashSet consideredChunks = new LongOpenHashSet();

    public static SteelLeviathanSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SteelLeviathanSpawnData::load, SteelLeviathanSpawnData::new, DATA_NAME);
    }

    public boolean tryMarkConsidered(long chunkKey) {
        if (!consideredChunks.add(chunkKey)) {
            return false;
        }
        setDirty();
        return true;
    }

    public static SteelLeviathanSpawnData load(CompoundTag tag) {
        SteelLeviathanSpawnData data = new SteelLeviathanSpawnData();
        if (tag.contains("ConsideredChunks")) {
            for (long key : tag.getLongArray("ConsideredChunks")) {
                data.consideredChunks.add(key);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("ConsideredChunks", consideredChunks.toLongArray());
        return tag;
    }
}
