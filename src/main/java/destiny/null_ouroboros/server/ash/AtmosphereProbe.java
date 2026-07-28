package destiny.null_ouroboros.server.ash;

import destiny.null_ouroboros.server.block.OutputVentBlock;
import destiny.null_ouroboros.server.block.entity.OutputVentBlockEntity;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AtmosphereProbe {
    private AtmosphereProbe() {
    }

    public enum Locale {
        OUTSIDE,
        ROOM
    }

    public enum Activity {
        IDLE,
        CLEARING,
        CONTAMINATION
    }

    public record Result(
            Locale locale,
            Activity activity,
            boolean localClean,
            int volume,
            int cleanCells,
            int ventBudget,
            int activeVents
    ) {
    }

    public static Result probe(ServerLevel level, AshAtmosphere ash, BlockPos sample) {
        boolean localClean = !ash.isAshyAir(level, sample);
        if (AshAirtight.isSkyExposed(level, sample)) {
            return new Result(Locale.OUTSIDE, Activity.IDLE, false, 0, 0, 0, 0);
        }

        AshAtmosphere.EnclosureResult space = ash.inspectEnclosure(level, sample);
        if (!space.enclosed()) {
            return new Result(Locale.ROOM, Activity.CONTAMINATION, localClean, 0, 0, 0, 0);
        }

        int activeVents = countActiveFilteredVents(level, space);
        int ventBudget = activeVents * VentNetworkTracker.CELLS_PER_VENT;
        int volume = space.volume();
        int cleanCells = space.cleanCount();
        boolean contamination = isContaminating(level, ash, space);
        boolean clearing = !contamination && ventBudget > 0 && cleanCells < Math.min(volume, ventBudget);

        Activity activity = contamination ? Activity.CONTAMINATION : (clearing ? Activity.CLEARING : Activity.IDLE);
        return new Result(Locale.ROOM, activity, localClean, volume, cleanCells, ventBudget, activeVents);
    }

    private static int countActiveFilteredVents(ServerLevel level, AshAtmosphere.EnclosureResult space) {
        LongOpenHashSet counted = new LongOpenHashSet();
        int count = 0;
        for (long key : space.cells()) {
            BlockPos air = BlockPos.of(key);
            for (Direction direction : Direction.values()) {
                BlockPos ventPos = air.relative(direction);
                if (!counted.add(ventPos.asLong())) {
                    continue;
                }
                BlockState state = level.getBlockState(ventPos);
                if (!(state.getBlock() instanceof OutputVentBlock)) {
                    continue;
                }
                if (!state.getValue(OutputVentBlock.POWERED)) {
                    continue;
                }
                if (!OutputVentBlock.hasExposedOutlet(level, ventPos, state)) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(ventPos);
                if (be instanceof OutputVentBlockEntity vent && vent.hasWorkingFilter() && vent.isAtmosphereActive()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isContaminating(ServerLevel level, AshAtmosphere ash, AshAtmosphere.EnclosureResult space) {
        if (hasExteriorAshBreach(level, ash, space)) {
            return true;
        }
        return hasActiveContaminatingVent(level, space);
    }

    private static boolean hasExteriorAshBreach(ServerLevel level, AshAtmosphere ash, AshAtmosphere.EnclosureResult space) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (long key : space.cells()) {
            BlockPos pos = BlockPos.of(key);
            if (!ash.isClean(pos)) {
                continue;
            }
            if (AshAirtight.isSkyExposed(level, pos)) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                if (!AshAirtight.canFlow(level, pos, direction)) {
                    continue;
                }
                cursor.setWithOffset(pos, direction);
                if (space.cells().contains(cursor.asLong())) {
                    continue;
                }
                if (ash.isAshyAir(level, cursor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasActiveContaminatingVent(ServerLevel level, AshAtmosphere.EnclosureResult space) {
        LongOpenHashSet counted = new LongOpenHashSet();
        for (long key : space.cells()) {
            BlockPos air = BlockPos.of(key);
            for (Direction direction : Direction.values()) {
                BlockPos ventPos = air.relative(direction);
                if (!counted.add(ventPos.asLong())) {
                    continue;
                }
                BlockState state = level.getBlockState(ventPos);
                if (!(state.getBlock() instanceof OutputVentBlock)) {
                    continue;
                }
                if (!state.getValue(OutputVentBlock.POWERED)) {
                    continue;
                }
                if (!OutputVentBlock.hasExposedOutlet(level, ventPos, state)) {
                    continue;
                }
                BlockEntity be = level.getBlockEntity(ventPos);
                if (!(be instanceof OutputVentBlockEntity vent)
                        || vent.hasWorkingFilter()
                        || !vent.isAtmosphereActive()) {
                    continue;
                }
                BlockPos outlet = OutputVentBlock.firstAirNeighbor(level, ventPos);
                if (outlet != null && space.cells().contains(outlet.asLong())) {
                    return true;
                }
            }
        }
        return false;
    }
}
