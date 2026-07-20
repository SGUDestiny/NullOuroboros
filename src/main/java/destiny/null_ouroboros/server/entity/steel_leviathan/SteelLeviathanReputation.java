package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SteelLeviathanReputation {
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> cooldowns = new HashMap<>();

    public int getScore(UUID player) {
        return scores.getOrDefault(player, 0);
    }

    public void addScore(UUID player, int delta) {
        scores.put(player, getScore(player) + delta);
    }

    public boolean isOnCooldown(UUID player) {
        return cooldowns.getOrDefault(player, 0) > 0;
    }

    public void tickCooldowns() {
        cooldowns.replaceAll((uuid, ticks) -> Math.max(0, ticks - 1));
    }

    public void startCooldown(UUID player, RandomSource random) {
        int score = getScore(player);
        int max = SteelLeviathanConstants.INTEREST_COOLDOWN_MAX_TICKS;
        if (score > SteelLeviathanConstants.REP_GIFT_THRESHOLD) {
            int reduction = (score - SteelLeviathanConstants.REP_GIFT_THRESHOLD)
                    * SteelLeviathanConstants.COOLDOWN_REDUCTION_PER_REP;
            max = Math.max(SteelLeviathanConstants.INTEREST_COOLDOWN_MIN_TICKS, max - reduction);
        }
        int min = SteelLeviathanConstants.INTEREST_COOLDOWN_MIN_TICKS;
        int rolled = min + random.nextInt(Math.max(1, max - min + 1));
        cooldowns.put(player, rolled);
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Id", entry.getKey());
            entryTag.putInt("Score", entry.getValue());
            entryTag.putInt("Cooldown", cooldowns.getOrDefault(entry.getKey(), 0));
            list.add(entryTag);
        }
        for (Map.Entry<UUID, Integer> entry : cooldowns.entrySet()) {
            if (!scores.containsKey(entry.getKey())) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("Id", entry.getKey());
                entryTag.putInt("Score", 0);
                entryTag.putInt("Cooldown", entry.getValue());
                list.add(entryTag);
            }
        }
        tag.put("Reputation", list);
    }

    public void load(CompoundTag tag) {
        scores.clear();
        cooldowns.clear();
        if (!tag.contains("Reputation", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("Reputation", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            if (!entryTag.hasUUID("Id")) {
                continue;
            }
            UUID id = entryTag.getUUID("Id");
            scores.put(id, entryTag.getInt("Score"));
            cooldowns.put(id, entryTag.getInt("Cooldown"));
        }
    }
}

