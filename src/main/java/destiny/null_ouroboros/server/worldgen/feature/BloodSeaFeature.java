package destiny.null_ouroboros.server.worldgen.feature;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.AshPileBlock;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BloodSeaFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> VERMILLION_TEARS = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "vermillion_tears"));

    private static final int CELL_SIZE = 160;
    private static final int MIN_RADIUS = 42;
    private static final int MAX_RADIUS = 55;
    private static final int MAX_DEPTH = 4;
    private static final float LAKE_CHANCE = 0.55F;
    private static final double SHORE_START = 0.78;
    private static final int MAX_BLOBS = 4;

    public BloodSeaFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!(level instanceof WorldGenRegion)) {
            return false;
        }

        ChunkGenerator generator = context.chunkGenerator();
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        BiomeSource biomeSource = generator.getBiomeSource();
        ChunkAccess chunk = level.getChunk(context.origin());
        long seed = level.getSeed();

        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();

        int cellMinX = Math.floorDiv(minX - MAX_RADIUS - 16, CELL_SIZE);
        int cellMaxX = Math.floorDiv(maxX + MAX_RADIUS + 16, CELL_SIZE);
        int cellMinZ = Math.floorDiv(minZ - MAX_RADIUS - 16, CELL_SIZE);
        int cellMaxZ = Math.floorDiv(maxZ + MAX_RADIUS + 16, CELL_SIZE);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState blood = BlockRegistry.BLOOD.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState ash = BlockRegistry.ASH_BLOCK.get().defaultBlockState();
        boolean placed = false;

        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                RandomSource cellRandom = RandomSource.create(Mth.getSeed(cellX, 0, cellZ) ^ seed * 341873128712L);
                if (cellRandom.nextFloat() > LAKE_CHANCE) {
                    continue;
                }

                int baseRadius = MIN_RADIUS + cellRandom.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
                int centerX = cellX * CELL_SIZE + cellRandom.nextInt(CELL_SIZE);
                int centerZ = cellZ * CELL_SIZE + cellRandom.nextInt(CELL_SIZE);

                if (!isVermillion(biomeSource, randomState, centerX, centerZ, level.getSeaLevel())) {
                    continue;
                }

                LakeShape shape = LakeShape.create(cellRandom, centerX, centerZ, baseRadius);
                int pad = shape.boundsRadius();
                int lakeMinX = Math.max(minX, centerX - pad);
                int lakeMaxX = Math.min(maxX, centerX + pad);
                int lakeMinZ = Math.max(minZ, centerZ - pad);
                int lakeMaxZ = Math.min(maxZ, centerZ + pad);
                if (lakeMinX > lakeMaxX || lakeMinZ > lakeMaxZ) {
                    continue;
                }

                int waterline = sampleWaterline(generator, level, randomState, shape);
                if (waterline == Integer.MIN_VALUE) {
                    continue;
                }

                for (int x = lakeMinX; x <= lakeMaxX; x++) {
                    for (int z = lakeMinZ; z <= lakeMaxZ; z++) {
                        double t = shape.normalizedDistance(x, z);
                        if (t >= 1.0) {
                            continue;
                        }

                        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x & 15, z & 15);

                        if (t >= SHORE_START) {
                            double shoreT = (t - SHORE_START) / (1.0 - SHORE_START);
                            for (int y = surfaceY; y > waterline; y--) {
                                pos.set(x, y, z);
                                BlockState state = chunk.getBlockState(pos);
                                if (state.is(Blocks.BEDROCK)) {
                                    break;
                                }
                                if (isCarvable(state) || state.is(BlockRegistry.ASH_PILE.get())) {
                                    chunk.setBlockState(pos, air, false);
                                    placed = true;
                                }
                            }
                            resurface(chunk, pos, x, waterline, z, ash);
                            double rise = Math.max(0, surfaceY - waterline);
                            double targetRise = Mth.lerp(shoreT * shoreT, 0.125, Math.max(rise, 0.125));
                            placeAshRamp(chunk, pos, x, waterline, z, targetRise, ash);
                            placed = true;
                            continue;
                        }

                        double depthFactor = (1.0 - t / SHORE_START);
                        depthFactor = depthFactor * depthFactor;
                        int depth = Math.max(1, (int) Math.round(MAX_DEPTH * depthFactor));
                        int floorY = waterline - depth;

                        for (int y = surfaceY; y > waterline; y--) {
                            pos.set(x, y, z);
                            BlockState state = chunk.getBlockState(pos);
                            if (state.is(Blocks.BEDROCK)) {
                                break;
                            }
                            if (isCarvable(state)) {
                                chunk.setBlockState(pos, air, false);
                                placed = true;
                            }
                        }

                        for (int y = waterline; y > floorY; y--) {
                            pos.set(x, y, z);
                            BlockState state = chunk.getBlockState(pos);
                            if (state.is(Blocks.BEDROCK)) {
                                break;
                            }
                            if (isCarvable(state) || state.isAir()) {
                                chunk.setBlockState(pos, blood, false);
                                placed = true;
                            }
                        }

                        pos.set(x, floorY, z);
                        BlockState floor = chunk.getBlockState(pos);
                        if (isCarvable(floor) || floor.is(BlockRegistry.TRAMPLED_ASH.get())) {
                            chunk.setBlockState(pos, ash, false);
                        }
                    }
                }
            }
        }

        return placed;
    }

    private static void resurface(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState ash) {
        pos.set(x, y, z);
        BlockState state = chunk.getBlockState(pos);
        if (state.is(BlockRegistry.TRAMPLED_ASH.get())
                || state.is(BlockRegistry.VEINED_TRAMPLED_ASH.get())
                || state.is(BlockRegistry.ASH_BLOCK.get())
                || state.is(BlockRegistry.VEINED_ASH_BLOCK.get())
                || state.isAir()) {
            chunk.setBlockState(pos, ash, false);
        }
    }

    private static void placeAshRamp(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int waterline, int z,
                                     double targetRise, BlockState ash) {
        int fullBlocks = Mth.floor(targetRise);
        int layers = Mth.clamp((int) Math.round((targetRise - fullBlocks) * AshPileBlock.MAX_HEIGHT), 0, AshPileBlock.MAX_HEIGHT);
        if (fullBlocks == 0 && layers == 0) {
            layers = 1;
        }
        if (layers == AshPileBlock.MAX_HEIGHT) {
            fullBlocks++;
            layers = 0;
        }

        for (int i = 1; i <= fullBlocks; i++) {
            pos.set(x, waterline + i, z);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || isCarvable(state) || state.is(BlockRegistry.ASH_PILE.get())) {
                chunk.setBlockState(pos, ash, false);
            }
        }

        if (layers > 0) {
            pos.set(x, waterline + fullBlocks + 1, z);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir() || isCarvable(state) || state.is(BlockRegistry.ASH_PILE.get())) {
                chunk.setBlockState(pos, BlockRegistry.ASH_PILE.get().defaultBlockState()
                        .setValue(AshPileBlock.LAYERS, layers), false);
            }
        }
    }

    private static int sampleWaterline(ChunkGenerator generator, WorldGenLevel level, RandomState randomState, LakeShape shape) {
        int minSurface = Integer.MAX_VALUE;
        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI / 6.0);
            int sx = shape.centerX + (int) Math.round(Math.cos(angle) * shape.sampleRadius());
            int sz = shape.centerZ + (int) Math.round(Math.sin(angle) * shape.sampleRadius());
            minSurface = Math.min(minSurface, generator.getFirstOccupiedHeight(
                    sx, sz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState));
        }
        minSurface = Math.min(minSurface, generator.getFirstOccupiedHeight(
                shape.centerX, shape.centerZ, Heightmap.Types.WORLD_SURFACE_WG, level, randomState));
        if (minSurface == Integer.MAX_VALUE) {
            return Integer.MIN_VALUE;
        }
        return minSurface;
    }

    private static boolean isVermillion(BiomeSource biomeSource, RandomState randomState, int x, int z, int y) {
        Holder<Biome> biome = biomeSource.getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                randomState.sampler());
        return biome.is(VERMILLION_TEARS);
    }

    private static boolean isCarvable(BlockState state) {
        return state.is(BlockRegistry.ASH_BLOCK.get())
                || state.is(BlockRegistry.TRAMPLED_ASH.get())
                || state.is(BlockRegistry.VEINED_ASH_BLOCK.get())
                || state.is(BlockRegistry.VEINED_TRAMPLED_ASH.get())
                || state.is(BlockRegistry.ASH_PILE.get());
    }

    private static final class LakeShape {
        private final int centerX;
        private final int centerZ;
        private final Blob[] blobs;
        private final double warpPhaseA;
        private final double warpPhaseB;

        private LakeShape(int centerX, int centerZ, Blob[] blobs, double warpPhaseA, double warpPhaseB) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.blobs = blobs;
            this.warpPhaseA = warpPhaseA;
            this.warpPhaseB = warpPhaseB;
        }

        static LakeShape create(RandomSource random, int centerX, int centerZ, int baseRadius) {
            int blobCount = 2 + random.nextInt(MAX_BLOBS - 1);
            Blob[] blobs = new Blob[blobCount];
            double rotation = random.nextDouble() * Math.PI;
            double stretch = 0.72 + random.nextDouble() * 0.45;
            blobs[0] = new Blob(centerX, centerZ, baseRadius, baseRadius * stretch, rotation);

            for (int i = 1; i < blobCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                int offset = 8 + random.nextInt(Math.max(1, baseRadius / 3));
                int bx = centerX + (int) Math.round(Math.cos(angle) * offset);
                int bz = centerZ + (int) Math.round(Math.sin(angle) * offset);
                int br = Math.max(16, (int) (baseRadius * (0.45 + random.nextDouble() * 0.4)));
                double bStretch = 0.7 + random.nextDouble() * 0.5;
                blobs[i] = new Blob(bx, bz, br, br * bStretch, random.nextDouble() * Math.PI);
            }

            return new LakeShape(centerX, centerZ, blobs, random.nextDouble() * Math.PI * 2.0, random.nextDouble() * Math.PI * 2.0);
        }

        int boundsRadius() {
            int max = 0;
            for (Blob blob : blobs) {
                int reach = (int) Math.ceil(Math.max(blob.radiusX, blob.radiusZ))
                        + Math.max(Math.abs(blob.x - centerX), Math.abs(blob.z - centerZ))
                        + 4;
                max = Math.max(max, reach);
            }
            return max;
        }

        double sampleRadius() {
            return Math.max(8.0, boundsRadius() * SHORE_START);
        }

        double normalizedDistance(int x, int z) {
            double best = Double.POSITIVE_INFINITY;
            for (Blob blob : blobs) {
                double dx = x - blob.x;
                double dz = z - blob.z;
                double lx = dx * blob.cos + dz * blob.sin;
                double lz = -dx * blob.sin + dz * blob.cos;
                double nx = lx / blob.radiusX;
                double nz = lz / blob.radiusZ;
                double d = Math.sqrt(nx * nx + nz * nz);
                double angle = Math.atan2(lz, lx);
                double warp = 1.0
                        + 0.12 * Math.sin(angle * 3.0 + warpPhaseA)
                        + 0.08 * Math.sin(angle * 5.0 + warpPhaseB);
                d /= Math.max(0.75, warp);
                if (d < best) {
                    best = d;
                }
            }
            return best;
        }
    }

    private static final class Blob {
        private final int x;
        private final int z;
        private final double radiusX;
        private final double radiusZ;
        private final double cos;
        private final double sin;

        private Blob(int x, int z, double radiusX, double radiusZ, double rotation) {
            this.x = x;
            this.z = z;
            this.radiusX = radiusX;
            this.radiusZ = radiusZ;
            this.cos = Math.cos(rotation);
            this.sin = Math.sin(rotation);
        }
    }
}
