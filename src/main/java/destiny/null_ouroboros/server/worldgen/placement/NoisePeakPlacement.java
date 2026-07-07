package destiny.null_ouroboros.server.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import destiny.null_ouroboros.server.registry.PlacementRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NoisePeakPlacement extends PlacementModifier {
    public static final Codec<NoisePeakPlacement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("threshold").forGetter(p -> p.threshold),
                    Codec.LONG.fieldOf("salt").forGetter(p -> p.salt),
                    Codec.INT.optionalFieldOf("check_radius", 2).forGetter(p -> p.checkRadius)
            ).apply(instance, NoisePeakPlacement::new)
    );

    private final double threshold;
    private final long salt;
    private final int checkRadius;

    public NoisePeakPlacement(double threshold, long salt, int checkRadius) {
        this.threshold = threshold;
        this.salt = salt;
        this.checkRadius = checkRadius;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos chunkOrigin) {
        long worldSeed = context.getLevel().getSeed();
        List<BlockPos> results = new ArrayList<>();

        int minX = chunkOrigin.getX() - checkRadius;
        int minZ = chunkOrigin.getZ() - checkRadius;
        int maxX = chunkOrigin.getX() + 15 + checkRadius;
        int maxZ = chunkOrigin.getZ() + 15 + checkRadius;

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        double[][] noise = new double[width][depth];

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                noise[x - minX][z - minZ] = sampleNoise(worldSeed, salt, x, z);
            }
        }

        for (int dx = 0; dx <= 15; dx++) {
            for (int dz = 0; dz <= 15; dz++) {
                int worldX = chunkOrigin.getX() + dx;
                int worldZ = chunkOrigin.getZ() + dz;
                int ix = worldX - minX;
                int iz = worldZ - minZ;

                double val = noise[ix][iz];
                if (val < threshold) continue;

                boolean isMax = true;
                for (int nx = -1; nx <= 1; nx++) {
                    for (int nz = -1; nz <= 1; nz++) {
                        if (nx == 0 && nz == 0) continue;
                        if (noise[ix + nx][iz + nz] >= val) {
                            isMax = false;
                            break;
                        }
                    }
                    if (!isMax) break;
                }

                if (isMax) {
                    results.add(new BlockPos(worldX, 0, worldZ));
                }
            }
        }
        return results.stream();
    }

    private double sampleNoise(long seed, long salt, int x, int z) {
        long h = mix(seed, salt, x, z);
        return (double) (h & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static long mix(long seed, long salt, int x, int z) {
        long a = seed ^ salt;
        a = a * 6364136223846793005L + x;
        a = a * 6364136223846793005L + z;
        a = a ^ (a >> 33);
        a = a * 0xFF51AFD7ED558CCDL;
        a = a ^ (a >> 33);
        a = a * 0xC4CEB9FE1A85EC53L;
        a = a ^ (a >> 33);
        return a;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementRegistry.NOISE_PEAK.get();
    }
}
