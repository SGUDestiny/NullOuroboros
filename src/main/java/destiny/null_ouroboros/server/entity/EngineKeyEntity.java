package destiny.null_ouroboros.server.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class EngineKeyEntity extends ParentLinkedHitboxEntity {
    public EngineKeyEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EngineKeyEntity(EntityType<?> type, Level level, int parentId, UUID parentUuid, double x, double y, double z) {
        this(type, level);
        initParentLink(parentId, parentUuid, x, y, z);
    }

    public EngineEntity getParent() {
        Entity entity = getParentId() != NO_PARENT ? this.level().getEntity(getParentId()) : null;
        return entity instanceof EngineEntity engine ? engine : null;
    }

    @Override
    protected Entity resolveParent() {
        return findParentOfType(EngineEntity.class);
    }

    @Override
    protected AABB makeBoundingBox() {
        double hs = 0.05;
        return new AABB(getX() - hs, getY() - hs * 2, getZ() - hs,
                getX() + hs, getY() + hs * 2, getZ() + hs);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        EngineEntity engine = getParent();
        if (engine != null) {
            engine.handleKeyPortInteraction(player, hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
