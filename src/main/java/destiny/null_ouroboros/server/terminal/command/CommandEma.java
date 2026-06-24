package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.manifolding.ManifoldingForecast;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class CommandEma extends TerminalCommand {
    private static final int LOADING_DURATION_TICKS = 200;
    private static final int LOADING_BAR_WIDTH = 20;
    private static final int LOADING_BLOCK_LINE_COUNT = 3;
    private static final int LOADING_PERCENT_MIN = 1;
    private static final int LOADING_PERCENT_MAX = 50;
    private static final int LOADING_UPDATE_MIN_TICKS = 10;
    private static final int LOADING_UPDATE_MAX_TICKS = 40;

    private final String args;
    @Nullable
    private ForecastState forecastState;

    public CommandEma(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.ema.missing_subcommand");
            setDone();
            return;
        }

        String subcommand = args.split("\\s+")[0].toLowerCase();
        if (!subcommand.equals("forecast")) {
            printlnTranslatable("message.null_ouroboros.terminus.ema.unknown_subcommand", subcommand);
            setDone();
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            printlnTranslatable("message.null_ouroboros.terminus.internal_error");
            setDone();
            return;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(computerPos);
        if (!(blockEntity instanceof DustyComputerBlockEntity computer)) {
            printlnTranslatable("message.null_ouroboros.terminus.internal_error");
            setDone();
            return;
        }

        computer.refreshEmaConnection();
        if (!computer.hasConnectedEma()) {
            printlnTranslatable("message.null_ouroboros.terminus.ema.no_peripheral");
            setDone();
            return;
        }

        forecastState = new ForecastState(serverLevel);
        serverLevel.playSound(null, computerPos, SoundRegistry.DUSTY_COMPUTER_LOAD.get(), SoundSource.BLOCKS, 0.5f, 1.0f);

        printlnTranslatable("message.null_ouroboros.terminus.ema.gathering");
        output.add(buildLoadingBarComponent(forecastState.displayPercent));
        printlnTranslatable("message.null_ouroboros.terminus.ema.cancel_hint");
        awaitInput();
    }

    @Override
    public boolean tick() {
        if (forecastState == null || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        long elapsed = serverLevel.getGameTime() - forecastState.startTick;
        if (forecastState.displayPercent >= 100 || elapsed >= LOADING_DURATION_TICKS) {
            finishForecast(serverLevel);
            return true;
        }
        return false;
    }

    @Override
    public boolean updatesLiveDisplay() {
        return forecastState != null && !isDone();
    }

    @Override
    public void updateLiveDisplay(TerminusSession session) {
        if (forecastState == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (forecastState.loadingBlockStartIndex < 0) {
            forecastState.loadingBlockStartIndex = session.getLines().size() - LOADING_BLOCK_LINE_COUNT;
        }

        long now = serverLevel.getGameTime();
        if (now >= forecastState.nextBarUpdateTick) {
            forecastState.displayPercent = Math.min(
                    100,
                    forecastState.displayPercent + randomLoadingIncrement(serverLevel.random)
            );
            forecastState.nextBarUpdateTick = now + randomUpdateInterval(serverLevel.random);
        }

        session.replaceLine(
                forecastState.loadingBlockStartIndex + 1,
                buildLoadingBarComponent(forecastState.displayPercent).getString()
        );
    }

    @Override
    public boolean handleInput(String input) {
        return false;
    }

    @Override
    public void cancel() {
        printlnTranslatable("message.null_ouroboros.terminus.ema.aborted");
        forecastState = null;
        setDone();
    }

    private void finishForecast(ServerLevel serverLevel) {
        ManifoldingForecast forecast;
        if (serverLevel.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
            forecast = serverLevel.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY)
                    .map(cap -> ManifoldingForecast.from(cap, serverLevel.getGameTime()))
                    .orElseGet(ManifoldingForecast::createInsufficient);
        } else {
            forecast = ManifoldingForecast.createInsufficient();
        }

        appendForecastOutput(forecast);
        setDone();
    }

    private void appendForecastOutput(ManifoldingForecast forecast) {
        printlnTranslatable("message.null_ouroboros.terminus.ema.forecast.header");
        printlnForecastRow("message.null_ouroboros.terminus.ema.forecast.et_arv", formatTimeValue(forecast.eta()));
        printlnForecastRow("message.null_ouroboros.terminus.ema.forecast.et_dur", formatTimeValue(forecast.etd()));
        printlnForecastRow("message.null_ouroboros.terminus.ema.forecast.et_ang", formatAngleReading(forecast.estimatedAngle()));
        if (forecast.insufficient()) {
            printlnTranslatable("message.null_ouroboros.terminus.ema.forecast.insufficient");
        }
    }

    private void printlnForecastRow(String labelKey, Component value) {
        printlnTranslatable("message.null_ouroboros.terminus.ema.forecast.row", Component.translatable(labelKey), value);
    }

    private Component formatTimeValue(ManifoldingForecast.TimeValue value) {
        return switch (value.kind()) {
            case NULL -> Component.translatable("message.null_ouroboros.terminus.ema.forecast.null_value");
            case NOW -> Component.translatable("message.null_ouroboros.terminus.ema.forecast.now");
            case UNDER_MIN -> Component.translatable("message.null_ouroboros.terminus.ema.forecast.under_min");
            case MINUTES -> Component.translatable("message.null_ouroboros.terminus.ema.forecast.minutes", value.minutes());
        };
    }

    private Component formatAngleReading(ManifoldingForecast.AngleValue value) {
        if (value.kind() == ManifoldingForecast.AngleValue.AngleKind.NULL) {
            return Component.translatable("message.null_ouroboros.terminus.ema.forecast.null_value");
        }
        return Component.translatable(
                "message.null_ouroboros.terminus.ema.forecast.estimated_angle_reading",
                value.degrees(),
                Component.translatable(value.directionKey())
        );
    }

    private Component buildLoadingBarComponent(int percent) {
        int filled = (percent * LOADING_BAR_WIDTH) / 100;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < LOADING_BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        bar.append(']');
        return Component.translatable("message.null_ouroboros.terminus.ema.loading_bar", bar.toString(), percent);
    }

    private static int randomLoadingIncrement(RandomSource random) {
        return random.nextInt(LOADING_PERCENT_MIN, LOADING_PERCENT_MAX + 1);
    }

    private static int randomUpdateInterval(RandomSource random) {
        return random.nextInt(LOADING_UPDATE_MIN_TICKS, LOADING_UPDATE_MAX_TICKS + 1);
    }

    private static final class ForecastState {
        private final long startTick;
        private int loadingBlockStartIndex = -1;
        private int displayPercent;
        private long nextBarUpdateTick;

        private ForecastState(ServerLevel level) {
            this.startTick = level.getGameTime();
            this.displayPercent = randomLoadingIncrement(level.random);
            this.nextBarUpdateTick = startTick + randomUpdateInterval(level.random);
        }
    }
}
