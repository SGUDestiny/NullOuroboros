package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.server.datagen.BricksBlockset;
import destiny.null_ouroboros.server.datagen.StoneBlockset;
import destiny.null_ouroboros.server.datagen.WoodBlockset;

import java.util.List;

public final class BlocksetRegistry {
    public static final List<StoneBlockset> STONE_BLOCKSETS = List.of(
            new StoneBlockset(
                    "blackmetal_plate",
                    "blackmetal_plate",
                    "blackmetal/blackmetal_plate",
                    "blackmetal_plate",
                    2,
                    false,
                    false,
                    false
            ),
            new StoneBlockset(
                    "blackmetal_tiles",
                    "blackmetal_tiles",
                    "blackmetal/blackmetal_tiles",
                    "blackmetal_tiles",
                    2,
                    false,
                    false,
                    false
            ),
            new StoneBlockset(
                    "blackmetal_support",
                    "blackmetal_support",
                    "blackmetal/blackmetal_support",
                    "blackmetal_support",
                    2,
                    false,
                    false,
                    false
            )
    );

    public static final List<BricksBlockset> BRICKS_BLOCKSETS = List.of(
    );

    public static final List<WoodBlockset> WOOD_BLOCKSETS = List.of(
            new WoodBlockset(
                    "scorched",
                    "scorched_planks",
                    "scorched",
                    "scorched_log",
                    0,
                    false
            ),
            new WoodBlockset(
                    "sanguine",
                    "sanguine_planks",
                    "sanguine",
                    "sanguine_log",
                    0,
                    false
            )
    );

    private BlocksetRegistry() {
    }
}
