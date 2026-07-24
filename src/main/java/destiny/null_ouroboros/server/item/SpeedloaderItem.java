package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.common.revolver.RevolverState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpeedloaderItem extends Item {
    public static final int CAPACITY = RevolverState.CHAMBER_COUNT;
    private static final String ROUNDS_TAG = "SpeedloaderRounds";

    public SpeedloaderItem(Properties properties) {
        super(properties);
    }

    public static List<RevolverCartridge> getRounds(ItemStack stack) {
        int[] serialized = stack.getOrCreateTag().getIntArray(ROUNDS_TAG);
        List<RevolverCartridge> rounds = new ArrayList<>(Math.min(serialized.length, CAPACITY));
        for (int value : serialized) {
            RevolverCartridge cartridge = RevolverCartridge.byOrdinal(value);
            if (cartridge.isLive() && rounds.size() < CAPACITY) {
                rounds.add(cartridge);
            }
        }
        return rounds;
    }

    public static void setRounds(ItemStack stack, List<RevolverCartridge> rounds) {
        int[] serialized = rounds.stream()
                .filter(RevolverCartridge::isLive)
                .limit(CAPACITY)
                .mapToInt(Enum::ordinal)
                .toArray();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putIntArray(ROUNDS_TAG, serialized);
    }

    public void loadInto(ItemStack revolver, ItemStack speedloader) {
        List<RevolverCartridge> rounds = getRounds(speedloader);
        for (int chamber = 0; chamber < rounds.size(); chamber++) {
            RevolverState.setChamber(revolver, chamber, rounds.get(chamber));
        }
        setRounds(speedloader, List.of());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !getRounds(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getRounds(stack).size() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xBA915A;
    }
}
