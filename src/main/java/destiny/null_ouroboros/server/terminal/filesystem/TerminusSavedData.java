package destiny.null_ouroboros.server.terminal.filesystem;

import destiny.null_ouroboros.server.terminal.TerminusSession;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerminusSavedData extends SavedData {
    private static final String DATA_NAME = "terminus";
    private final Map<UUID, TerminusFileSystem> fileSystems = new HashMap<>();
    private final Map<UUID, TerminusSession> sessions = new HashMap<>();

    public TerminusFileSystem getOrCreateFileSystem(UUID uuid) {
        return fileSystems.computeIfAbsent(uuid, id -> {
            setDirty();
            return new TerminusFileSystem();
        });
    }

    public TerminusSession getOrCreateSession(UUID uuid, BlockPos computerPos) {
        TerminusSession existing = sessions.get(uuid);
        if (existing != null) {
            existing.setComputerPos(computerPos);
            return existing;
        }
        setDirty();
        TerminusSession session = new TerminusSession(uuid, computerPos);
        sessions.put(uuid, session);
        return session;
    }

    public static TerminusSavedData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getDataStorage().computeIfAbsent(TerminusSavedData::load, TerminusSavedData::new, DATA_NAME);
    }

    public void remove(UUID uuid) {
        fileSystems.remove(uuid);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, TerminusFileSystem> entry : fileSystems.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("UUID", entry.getKey());
            entryTag.put("FS", entry.getValue().toNBT());
            list.add(entryTag);
        }
        tag.put("FileSystems", list);
        return tag;
    }

    public static TerminusSavedData load(CompoundTag tag) {
        TerminusSavedData data = new TerminusSavedData();
        ListTag list = tag.getList("FileSystems", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            UUID uuid = entryTag.getUUID("UUID");
            TerminusFileSystem fs = new TerminusFileSystem();
            fs.fromNBT(entryTag.getCompound("FS"));
            data.fileSystems.put(uuid, fs);
        }
        return data;
    }
}