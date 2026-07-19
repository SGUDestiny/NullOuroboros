package destiny.null_ouroboros.server.terminal.filesystem;

import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.p2p.ComputerEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TerminusSavedData extends SavedData {
    private static final String DATA_NAME = "terminus";
    private static final int IPV_INF_DIGITS = 6;
    private static final int IPV_INF_LIMIT = 1_000_000;
    private final Map<String, ComputerRecord> computers = new HashMap<>();
    private final Map<String, TerminusSession> sessions = new HashMap<>();

    public static boolean isValidIpvInf(String value) {
        return value != null && value.matches("\\d{" + IPV_INF_DIGITS + "}");
    }

    public static String generateIpvInfValue() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(IPV_INF_LIMIT));
    }

    public String generateUniqueIpvInf() {
        String id;
        do {
            id = generateIpvInfValue();
        } while (computers.containsKey(id));
        return id;
    }

    public String reassignIpvInf(String oldIpvInf) {
        ComputerRecord record = computers.remove(oldIpvInf);
        TerminusSession session = sessions.remove(oldIpvInf);
        String newIpvInf = generateUniqueIpvInf();
        if (record == null) {
            record = new ComputerRecord(newIpvInf);
        } else {
            record.setIpvInf(newIpvInf);
        }
        computers.put(newIpvInf, record);
        if (session != null) {
            session.setIpvInf(newIpvInf);
            sessions.put(newIpvInf, session);
        }
        setDirty();
        return newIpvInf;
    }

    public ComputerRecord getOrCreateComputer(String ipvInf, BlockPos computerPos) {
        ComputerRecord record = computers.computeIfAbsent(ipvInf, id -> {
            setDirty();
            return new ComputerRecord(id);
        });
        getOrCreateSession(ipvInf, computerPos);
        return record;
    }

    @Nullable
    public ComputerRecord getByIpvInf(String ipvInf) {
        return computers.get(ipvInf);
    }

    public TerminusFileSystem getOrCreateFileSystem(String ipvInf) {
        return computers.computeIfAbsent(ipvInf, id -> {
            setDirty();
            return new ComputerRecord(id);
        }).getFileSystem();
    }

    public TerminusSession getOrCreateSession(String ipvInf, BlockPos computerPos) {
        TerminusSession existing = sessions.get(ipvInf);
        if (existing != null) {
            existing.setComputerPos(computerPos);
            return existing;
        }
        setDirty();
        TerminusSession session = new TerminusSession(ipvInf, computerPos);
        attachDirtyCallback(session);
        sessions.put(ipvInf, session);
        return session;
    }

    @Nullable
    public TerminusSession getSession(String ipvInf) {
        return sessions.get(ipvInf);
    }

    public void relocateComputer(String ipvInf, BlockPos pos, ResourceLocation dimension) {
        ComputerRecord record = computers.computeIfAbsent(ipvInf, id -> new ComputerRecord(id));
        record.setEndpoint(new ComputerEndpoint(dimension, pos));
        setDirty();
    }

    public void clearEndpoint(String ipvInf) {
        ComputerRecord record = computers.get(ipvInf);
        if (record != null) {
            record.setEndpoint(null);
            setDirty();
        }
    }

    public static TerminusSavedData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getDataStorage().computeIfAbsent(TerminusSavedData::load, TerminusSavedData::new, DATA_NAME);
    }

    public void remove(String ipvInf) {
        computers.remove(ipvInf);
        sessions.remove(ipvInf);
        setDirty();
    }

    private void attachDirtyCallback(TerminusSession session) {
        session.setDirtyCallback(this::setDirty);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag computersList = new ListTag();
        for (ComputerRecord record : computers.values()) {
            computersList.add(record.toNBT());
        }
        tag.put("Computers", computersList);

        ListTag sessionsList = new ListTag();
        for (TerminusSession session : sessions.values()) {
            sessionsList.add(session.toNBT());
        }
        tag.put("Sessions", sessionsList);
        return tag;
    }

    public static TerminusSavedData load(CompoundTag tag) {
        TerminusSavedData data = new TerminusSavedData();
        if (tag.contains("Computers")) {
            ListTag list = tag.getList("Computers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ComputerRecord record = ComputerRecord.fromNBT(list.getCompound(i));
                if (!isValidIpvInf(record.getIpvInf()) || data.computers.containsKey(record.getIpvInf())) {
                    record.setIpvInf(data.generateUniqueIpvInf());
                }
                data.computers.put(record.getIpvInf(), record);
            }
        } else {
            ListTag legacyList = tag.getList("FileSystems", Tag.TAG_COMPOUND);
            for (int i = 0; i < legacyList.size(); i++) {
                CompoundTag entryTag = legacyList.getCompound(i);
                String ipvInf = data.generateUniqueIpvInf();
                if (data.computers.containsKey(ipvInf)) {
                    continue;
                }
                TerminusFileSystem fs = new TerminusFileSystem();
                fs.fromNBT(entryTag.getCompound("FS"));
                data.computers.put(ipvInf, new ComputerRecord(ipvInf, fs, null));
            }
        }

        if (tag.contains("Sessions", Tag.TAG_LIST)) {
            ListTag sessionsList = tag.getList("Sessions", Tag.TAG_COMPOUND);
            for (int i = 0; i < sessionsList.size(); i++) {
                CompoundTag sessionTag = sessionsList.getCompound(i);
                String ipvInf = sessionTag.getString("IpvInf");
                if (!isValidIpvInf(ipvInf) || data.sessions.containsKey(ipvInf) || !data.computers.containsKey(ipvInf)) {
                    continue;
                }
                TerminusFileSystem fs = data.computers.get(ipvInf).getFileSystem();
                TerminusSession session = TerminusSession.fromNBT(sessionTag, BlockPos.ZERO, fs);
                data.attachDirtyCallback(session);
                data.sessions.put(ipvInf, session);
            }
        }

        return data;
    }
}
