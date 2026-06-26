package destiny.null_ouroboros.server.terminal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public class TerminusLoadingBar {
    private static final int LOADING_DURATION_TICKS = 200;
    private static final int LOADING_BAR_WIDTH = 20;
    private static final int LOADING_PERCENT_MIN = 1;
    private static final int LOADING_PERCENT_MAX = 50;
    private static final int LOADING_UPDATE_MIN_TICKS = 10;
    private static final int LOADING_UPDATE_MAX_TICKS = 40;

    private final long startTick;
    private int loadingBlockStartIndex = -1;
    private int displayPercent;
    private long nextBarUpdateTick;

    public TerminusLoadingBar(ServerLevel level) {
        this.startTick = level.getGameTime();
        this.displayPercent = randomLoadingIncrement(level.random);
        this.nextBarUpdateTick = startTick + randomUpdateInterval(level.random);
    }

    public boolean tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now >= nextBarUpdateTick) {
            displayPercent = Math.min(100, displayPercent + randomLoadingIncrement(level.random));
            nextBarUpdateTick = now + randomUpdateInterval(level.random);
        }
        if (now - startTick >= LOADING_DURATION_TICKS) {
            displayPercent = 100;
        }
        return displayPercent >= 100;
    }

    public int getDisplayPercent() {
        return displayPercent;
    }

    public void setDisplayPercent(int displayPercent) {
        this.displayPercent = Math.max(0, Math.min(100, displayPercent));
    }

    public int getLoadingBlockStartIndex() {
        return loadingBlockStartIndex;
    }

    public void setLoadingBlockStartIndex(int loadingBlockStartIndex) {
        this.loadingBlockStartIndex = loadingBlockStartIndex;
    }

    public Component buildBarComponent() {
        return buildBarComponent(displayPercent);
    }

    public static Component buildBarComponent(int percent) {
        int filled = (percent * LOADING_BAR_WIDTH) / 100;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < LOADING_BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        bar.append(']');
        return Component.translatable("message.null_ouroboros.terminus.forecast.loading_bar", bar.toString(), percent);
    }

    private static int randomLoadingIncrement(RandomSource random) {
        return random.nextInt(LOADING_PERCENT_MIN, LOADING_PERCENT_MAX + 1);
    }

    private static int randomUpdateInterval(RandomSource random) {
        return random.nextInt(LOADING_UPDATE_MIN_TICKS, LOADING_UPDATE_MAX_TICKS + 1);
    }
}
