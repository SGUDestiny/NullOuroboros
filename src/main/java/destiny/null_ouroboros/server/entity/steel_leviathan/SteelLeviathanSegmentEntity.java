package destiny.null_ouroboros.server.entity.steel_leviathan;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SteelLeviathanSegmentEntity extends SteelLeviathanPartEntity {
    public SteelLeviathanSegmentEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public PartKind getPartKind() {
        return PartKind.SEGMENT;
    }
}

