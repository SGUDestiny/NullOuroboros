package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SteelLeviathanReputation {
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, Integer> cooldowns = new HashMap<>();
    private final Map<UUID, Float> damageDealt = new HashMap<>();
    private final Set<UUID> heatsinkDestroyers = new HashSet<>();

    public int getScore(UUID player) {
        return scores.getOrDefault(player, 0);
    }

    public void addScore(UUID player, int delta) {
        scores.put(player, getScore(player) + delta);
    }

    public float getDamage(UUID player) {
        return damageDealt.getOrDefault(player, 0.0F);
    }

    public void recordMainDamage(UUID player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        damageDealt.put(player, getDamage(player) + amount);
    }

    public void markHeatsinkDestroyed(UUID player) {
        heatsinkDestroyers.add(player);
    }

    public boolean hasGrudge(UUID player) {
        return getDamage(player) > 0.0F || heatsinkDestroyers.contains(player);
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
        Set<UUID> written = new HashSet<>();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            list.add(writeEntry(entry.getKey(), entry.getValue(),
                    cooldowns.getOrDefault(entry.getKey(), 0)));
            written.add(entry.getKey());
        }
        for (Map.Entry<UUID, Integer> entry : cooldowns.entrySet()) {
            if (written.add(entry.getKey())) {
                list.add(writeEntry(entry.getKey(), 0, entry.getValue()));
            }
        }
        for (UUID id : damageDealt.keySet()) {
            if (written.add(id)) {
                list.add(writeEntry(id, 0, cooldowns.getOrDefault(id, 0)));
            }
        }
        for (UUID id : heatsinkDestroyers) {
            if (written.add(id)) {
                list.add(writeEntry(id, 0, cooldowns.getOrDefault(id, 0)));
            }
        }
        tag.put("Reputation", list);
    }

    private CompoundTag writeEntry(UUID id, int score, int cooldown) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.putUUID("Id", id);
        entryTag.putInt("Score", score);
        entryTag.putInt("Cooldown", cooldown);
        entryTag.putFloat("Damage", getDamage(id));
        entryTag.putBoolean("HeatsinkKill", heatsinkDestroyers.contains(id));
        return entryTag;
    }

    public void load(CompoundTag tag) {
        scores.clear();
        cooldowns.clear();
        damageDealt.clear();
        heatsinkDestroyers.clear();
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
            float damage = entryTag.getFloat("Damage");
            if (damage > 0.0F) {
                damageDealt.put(id, damage);
            }
            if (entryTag.getBoolean("HeatsinkKill")) {
                heatsinkDestroyers.add(id);
            }
        }
    }
}
