package destiny.null_ouroboros.common.dusterbike;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.RandomSource;

public final class EngineAssembly {
    public static final List<DusterbikePartType> PARTS = List.of(
            DusterbikePartType.ENGINE,
            DusterbikePartType.PISTON_FRONT,
            DusterbikePartType.PISTON_REAR,
            DusterbikePartType.SPARK_PLUG_FRONT,
            DusterbikePartType.SPARK_PLUG_REAR,
            DusterbikePartType.KEY
    );

    public static final List<DusterbikePartType> DESTROY_STRIP_ORDER = List.of(
            DusterbikePartType.SPARK_PLUG_FRONT,
            DusterbikePartType.SPARK_PLUG_REAR,
            DusterbikePartType.PISTON_FRONT,
            DusterbikePartType.PISTON_REAR,
            DusterbikePartType.ENGINE
    );

    private static final float DESTROY_STRIP_CHANCE = 0.25F;

    private EngineAssembly() {}

    public static int computeMask(Function<DusterbikePartType, DusterbikePartState> parts) {
        int mask = 0;
        for (DusterbikePartType pt : PARTS) {
            DusterbikePartState state = parts.apply(pt);
            if (state != null && state.installed()) {
                mask |= (1 << pt.ordinal());
            }
        }
        return mask;
    }

    public static void copyParts(
            Function<DusterbikePartType, DusterbikePartState> source,
            Function<DusterbikePartType, DusterbikePartState> dest) {
        for (DusterbikePartType pt : PARTS) {
            DusterbikePartState src = source.apply(pt);
            DusterbikePartState dst = dest.apply(pt);
            if (src != null && dst != null) {
                dst.copyFrom(src);
            }
        }
    }

    public static void copyPartsIntoMap(
            Function<DusterbikePartType, DusterbikePartState> source,
            Map<DusterbikePartType, DusterbikePartState> dest) {
        for (DusterbikePartType pt : PARTS) {
            DusterbikePartState src = source.apply(pt);
            if (src == null) {
                continue;
            }
            DusterbikePartState copy = new DusterbikePartState(pt, src.durability(), src.installed());
            copy.setMainColor(src.mainColor());
            copy.setGlowColor(src.glowColor());
            dest.put(pt, copy);
        }
    }

    public static void clearInstalled(Function<DusterbikePartType, DusterbikePartState> parts) {
        for (DusterbikePartType pt : PARTS) {
            DusterbikePartState state = parts.apply(pt);
            if (state != null) {
                state.setInstalled(false);
            }
        }
    }

    public static boolean isPistonBlockedBySparkPlug(
            DusterbikePartType type,
            Function<DusterbikePartType, Boolean> installed) {
        if (type == DusterbikePartType.PISTON_FRONT) {
            return Boolean.TRUE.equals(installed.apply(DusterbikePartType.SPARK_PLUG_FRONT));
        }
        if (type == DusterbikePartType.PISTON_REAR) {
            return Boolean.TRUE.equals(installed.apply(DusterbikePartType.SPARK_PLUG_REAR));
        }
        return false;
    }

    public static boolean canRemoveEngineBlock(Function<DusterbikePartType, Boolean> installed) {
        return Boolean.TRUE.equals(installed.apply(DusterbikePartType.ENGINE))
                && !Boolean.TRUE.equals(installed.apply(DusterbikePartType.PISTON_FRONT))
                && !Boolean.TRUE.equals(installed.apply(DusterbikePartType.PISTON_REAR))
                && !Boolean.TRUE.equals(installed.apply(DusterbikePartType.SPARK_PLUG_FRONT))
                && !Boolean.TRUE.equals(installed.apply(DusterbikePartType.SPARK_PLUG_REAR))
                && !Boolean.TRUE.equals(installed.apply(DusterbikePartType.KEY));
    }

    public static void writeToEngineState(
            Function<DusterbikePartType, DusterbikePartState> source,
            DusterbikeEngineState dest) {
        for (DusterbikePartType pt : PARTS) {
            DusterbikePartState src = source.apply(pt);
            if (src != null) {
                dest.part(pt).copyFrom(src);
            }
        }
    }

    public static void stripEngineAssemblyOnDestroy(
            Function<DusterbikePartType, DusterbikePartState> parts,
            RandomSource random) {
        if (!Boolean.TRUE.equals(isInstalled(DusterbikePartType.ENGINE, parts))) {
            return;
        }

        for (DusterbikePartType type : List.of(DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR)) {
            if (isInstalled(type, parts) && random.nextFloat() < DESTROY_STRIP_CHANCE) {
                parts.apply(type).setInstalled(false);
            }
        }

        if (isInstalled(DusterbikePartType.PISTON_FRONT, parts)
                && !isInstalled(DusterbikePartType.SPARK_PLUG_FRONT, parts)
                && random.nextFloat() < DESTROY_STRIP_CHANCE) {
            parts.apply(DusterbikePartType.PISTON_FRONT).setInstalled(false);
        }
        if (isInstalled(DusterbikePartType.PISTON_REAR, parts)
                && !isInstalled(DusterbikePartType.SPARK_PLUG_REAR, parts)
                && random.nextFloat() < DESTROY_STRIP_CHANCE) {
            parts.apply(DusterbikePartType.PISTON_REAR).setInstalled(false);
        }

        if (!anyEngineInternalsInstalled(parts)
                && isInstalled(DusterbikePartType.ENGINE, parts)
                && random.nextFloat() < DESTROY_STRIP_CHANCE) {
            parts.apply(DusterbikePartType.ENGINE).setInstalled(false);
        }
    }

    private static boolean isInstalled(DusterbikePartType type, Function<DusterbikePartType, DusterbikePartState> parts) {
        DusterbikePartState state = parts.apply(type);
        return state != null && state.installed();
    }

    private static boolean anyEngineInternalsInstalled(Function<DusterbikePartType, DusterbikePartState> parts) {
        return isInstalled(DusterbikePartType.SPARK_PLUG_FRONT, parts)
                || isInstalled(DusterbikePartType.SPARK_PLUG_REAR, parts)
                || isInstalled(DusterbikePartType.PISTON_FRONT, parts)
                || isInstalled(DusterbikePartType.PISTON_REAR, parts);
    }
}
