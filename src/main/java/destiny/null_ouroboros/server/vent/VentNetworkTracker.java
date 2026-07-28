package destiny.null_ouroboros.server.vent;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.ash.AshAirtight;
import destiny.null_ouroboros.server.ash.AshAtmosphere;
import destiny.null_ouroboros.server.block.IntakeFanBlock;
import destiny.null_ouroboros.server.block.OutputVentBlock;
import destiny.null_ouroboros.server.block.VentilationRouterBlock;
import destiny.null_ouroboros.server.block.VentilationShaftBlock;
import destiny.null_ouroboros.server.block.entity.IntakeFanBlockEntity;
import destiny.null_ouroboros.server.block.entity.OutputVentBlockEntity;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VentNetworkTracker {
    public static final int VENTS_PER_FAN = 3;
    public static final int CELLS_PER_VENT = 128;

    private static final Map<ResourceKey<Level>, LongOpenHashSet> DUCTS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Component>> COMPONENTS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Boolean> DIRTY = new ConcurrentHashMap<>();

    private VentNetworkTracker() {
    }

    public static void addDuct(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        DUCTS.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(pos.asLong());
        markDirty(level);
    }

    public static void removeDuct(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        LongOpenHashSet set = DUCTS.get(level.dimension());
        if (set != null) {
            set.remove(pos.asLong());
        }
        markDirty(level);
    }

    public static void markDirty(Level level) {
        if (level.isClientSide) {
            return;
        }
        DIRTY.put(level.dimension(), Boolean.TRUE);
    }

    public static void serverTick(ServerLevel level) {
        if (Boolean.TRUE.equals(DIRTY.get(level.dimension()))) {
            rebuild(level);
            DIRTY.put(level.dimension(), Boolean.FALSE);
        }
        Long2ObjectOpenHashMap<Component> map = COMPONENTS.get(level.dimension());
        if (map == null || map.isEmpty()) {
            return;
        }
        LongOpenHashSet seen = new LongOpenHashSet();
        for (Component component : map.values()) {
            if (!seen.add(component.id)) {
                continue;
            }
            component.tick(level);
        }
    }

    private static void rebuild(ServerLevel level) {
        LongOpenHashSet ducts = DUCTS.get(level.dimension());
        Long2ObjectOpenHashMap<Component> map = new Long2ObjectOpenHashMap<>();
        if (ducts == null || ducts.isEmpty()) {
            COMPONENTS.put(level.dimension(), map);
            return;
        }
        LongOpenHashSet seen = new LongOpenHashSet();
        long nextId = 1;
        for (long packed : ducts) {
            if (!seen.add(packed)) {
                continue;
            }
            BlockPos start = BlockPos.of(packed);
            if (!AshAirtight.isDuctBlock(level.getBlockState(start))) {
                continue;
            }
            Component component = floodComponent(level, start, seen, nextId++);
            for (long member : component.members) {
                map.put(member, component);
            }
        }
        COMPONENTS.put(level.dimension(), map);
    }

    private static Component floodComponent(ServerLevel level, BlockPos start, LongOpenHashSet globalSeen, long id) {
        Component component = new Component(id);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        component.members.add(start.asLong());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof IntakeFanBlock) {
                component.fans.add(pos.immutable());
            } else if (state.getBlock() instanceof OutputVentBlock) {
                component.vents.add(pos.immutable());
            } else if (state.getBlock() instanceof VentilationShaftBlock) {
                for (Direction open : VentilationShaftBlock.openEnds(state, level, pos)) {
                    component.openEnds.add(new OpenEnd(pos.immutable(), open));
                }
            }

            for (Direction direction : Direction.values()) {
                if (!connectsToward(state, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction);
                BlockState nextState = level.getBlockState(next);
                if (!AshAirtight.isDuctBlock(nextState)) {
                    continue;
                }
                if (!connectsToward(nextState, direction.getOpposite())) {
                    continue;
                }
                long key = next.asLong();
                if (!component.members.add(key)) {
                    continue;
                }
                globalSeen.add(key);
                queue.add(next);
            }
        }
        component.fans.sort(Comparator.comparingLong(BlockPos::asLong));
        component.vents.sort(Comparator.comparingLong(BlockPos::asLong));
        return component;
    }

    private static boolean connectsToward(BlockState state, Direction direction) {
        if (state.getBlock() instanceof VentilationShaftBlock) {
            return VentilationShaftBlock.connects(state, direction);
        }
        if (state.getBlock() instanceof VentilationRouterBlock) {
            return direction.getAxis().isHorizontal();
        }
        if (state.getBlock() instanceof IntakeFanBlock || state.getBlock() instanceof OutputVentBlock) {
            return true;
        }
        return false;
    }

    public static final class Component {
        private final long id;
        private final LongOpenHashSet members = new LongOpenHashSet();
        private final List<BlockPos> fans = new ArrayList<>();
        private final List<BlockPos> vents = new ArrayList<>();
        private final List<OpenEnd> openEnds = new ArrayList<>();

        private Component(long id) {
            this.id = id;
        }

        void tick(ServerLevel level) {
            if (!VergeOfRealityDimension.isVergeOfReality(level)) {
                for (BlockPos fanPos : fans) {
                    BlockEntity be = level.getBlockEntity(fanPos);
                    if (be instanceof IntakeFanBlockEntity fan) {
                        fan.setOperational(false);
                    }
                }
                for (BlockPos ventPos : vents) {
                    BlockEntity be = level.getBlockEntity(ventPos);
                    if (be instanceof OutputVentBlockEntity vent) {
                        vent.setVisuallyActive(false);
                        vent.setAtmosphereActive(false);
                        vent.setEmittingClean(false);
                    }
                }
                return;
            }

            int validFans = 0;
            for (BlockPos fanPos : fans) {
                BlockEntity be = level.getBlockEntity(fanPos);
                if (!(be instanceof IntakeFanBlockEntity fan)) {
                    continue;
                }
                BlockState state = level.getBlockState(fanPos);
                if (!state.getValue(IntakeFanBlock.POWERED)) {
                    fan.setOperational(false);
                    continue;
                }
                boolean operational = IntakeFanBlock.hasExposedIntake(level, fanPos, state)
                        && IntakeFanBlock.isExteriorValid(level, fanPos, state);
                fan.setOperational(operational);
                if (operational) {
                    validFans++;
                }
            }

            int capacity = validFans * VENTS_PER_FAN;
            int used = 0;
            AshAtmosphere atmosphere = level.getCapability(CapabilityRegistry.ASH_ATMOSPHERE_CAPABILITY).orElse(null);

            for (BlockPos ventPos : vents) {
                BlockEntity be = level.getBlockEntity(ventPos);
                if (!(be instanceof OutputVentBlockEntity vent)) {
                    continue;
                }
                BlockState state = level.getBlockState(ventPos);
                boolean powered = state.getValue(OutputVentBlock.POWERED);
                boolean exposed = OutputVentBlock.hasExposedOutlet(level, ventPos, state);
                boolean visuallyActive = powered && exposed && validFans > 0;
                vent.setVisuallyActive(visuallyActive);

                if (!visuallyActive) {
                    vent.setAtmosphereActive(false);
                    vent.setEmittingClean(false);
                    continue;
                }

                BlockPos outlet = OutputVentBlock.firstAirNeighbor(level, ventPos);
                AshAtmosphere.EnclosureResult space = null;
                boolean enclosed = false;
                if (atmosphere != null && outlet != null) {
                    space = atmosphere.inspectEnclosure(level, outlet);
                    enclosed = space.enclosed() && !hasPressurizedOpenEndInSpace(space);
                }

                boolean workingFilter = vent.hasWorkingFilter();
                vent.setEmittingClean(workingFilter);

                boolean atmosphereActive = enclosed && used < capacity;
                if (atmosphereActive) {
                    used++;
                }
                vent.setAtmosphereActive(atmosphereActive);

                if (!atmosphereActive || atmosphere == null || outlet == null || space == null) {
                    continue;
                }

                if (workingFilter) {
                    int pooled = countPooledFilteredVents(level, atmosphere, space, capacity);
                    int budget = pooled * CELLS_PER_VENT;
                    if (space.cleanCount() < budget) {
                        if (enqueueNextClean(level, atmosphere, outlet, space)) {
                            vent.hurtFilterIfNeeded();
                        }
                    }
                } else {
                    atmosphere.injectAsh(level, outlet);
                }
            }

            if (validFans <= 0 || atmosphere == null) {
                return;
            }
            for (OpenEnd end : openEnds) {
                BlockPos leak = end.pos.relative(end.direction);
                if (!AshAirtight.isAirCell(level, leak)) {
                    continue;
                }
                if (atmosphere.isClean(leak)) {
                    atmosphere.enqueueAsh(leak);
                }
                if (level.random.nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.SMOKE,
                            leak.getX() + 0.5,
                            leak.getY() + 0.5,
                            leak.getZ() + 0.5,
                            1,
                            end.direction.getStepX() * 0.05,
                            end.direction.getStepY() * 0.05,
                            end.direction.getStepZ() * 0.05,
                            0.01);
                }
            }
        }

        private boolean hasPressurizedOpenEndInSpace(AshAtmosphere.EnclosureResult space) {
            for (OpenEnd end : openEnds) {
                BlockPos leak = end.pos.relative(end.direction);
                if (space.cells().contains(leak.asLong())) {
                    return true;
                }
            }
            return false;
        }

        private boolean enqueueNextClean(ServerLevel level, AshAtmosphere atmosphere, BlockPos outlet, AshAtmosphere.EnclosureResult space) {
            if (!atmosphere.isClean(outlet) && AshAirtight.isAirCell(level, outlet) && !AshAirtight.isSkyExposed(level, outlet)) {
                atmosphere.enqueueClean(outlet);
                return true;
            }
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (long key : space.cells()) {
                BlockPos pos = BlockPos.of(key);
                if (!atmosphere.isClean(pos)) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    if (!AshAirtight.canFlow(level, pos, direction)) {
                        continue;
                    }
                    cursor.setWithOffset(pos, direction);
                    if (!atmosphere.isClean(cursor)
                            && AshAirtight.isAirCell(level, cursor)
                            && !AshAirtight.isSkyExposed(level, cursor)
                            && space.cells().contains(cursor.asLong())) {
                        atmosphere.enqueueClean(cursor.immutable());
                        return true;
                    }
                }
            }
            return false;
        }

        private int countPooledFilteredVents(
                ServerLevel level,
                AshAtmosphere atmosphere,
                AshAtmosphere.EnclosureResult space,
                int capacity
        ) {
            int used = 0;
            int count = 0;
            for (BlockPos ventPos : vents) {
                BlockEntity be = level.getBlockEntity(ventPos);
                if (!(be instanceof OutputVentBlockEntity vent)) {
                    continue;
                }
                BlockState state = level.getBlockState(ventPos);
                if (!state.getValue(OutputVentBlock.POWERED) || !OutputVentBlock.hasExposedOutlet(level, ventPos, state)) {
                    continue;
                }
                BlockPos outlet = OutputVentBlock.firstAirNeighbor(level, ventPos);
                if (outlet == null) {
                    continue;
                }
                AshAtmosphere.EnclosureResult ventSpace = atmosphere.inspectEnclosure(level, outlet);
                if (!ventSpace.enclosed() || hasPressurizedOpenEndInSpace(ventSpace)) {
                    continue;
                }
                if (used >= capacity) {
                    continue;
                }
                used++;
                if (!vent.hasWorkingFilter()) {
                    continue;
                }
                if (sharesSpace(level, ventPos, outlet, space)) {
                    count++;
                }
            }
            return count;
        }

        private static boolean sharesSpace(
                ServerLevel level,
                BlockPos ventPos,
                BlockPos outlet,
                AshAtmosphere.EnclosureResult space
        ) {
            if (space.cells().contains(outlet.asLong())) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                BlockPos air = ventPos.relative(direction);
                if (AshAirtight.isOpenAirCell(level, air) && space.cells().contains(air.asLong())) {
                    return true;
                }
            }
            return false;
        }
    }

    public record OpenEnd(BlockPos pos, Direction direction) {
    }
}
