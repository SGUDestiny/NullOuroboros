package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class BurrowBeaconItem extends Item {
    public BurrowBeaconItem(Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext useContext) {
        Direction clickedFace = useContext.getClickedFace();

        if (clickedFace == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level level = useContext.getLevel();
            BlockPlaceContext placeContext = new BlockPlaceContext(useContext);
            BlockPos pos = placeContext.getClickedPos().above().above();
            ItemStack stack = useContext.getItemInHand();
            Vec3 posVec = Vec3.atBottomCenterOf(pos);
            AABB aabb = EntityRegistry.BURROW_BEACON.get().getDimensions().makeBoundingBox(posVec.x(), posVec.y(), posVec.z());

            if (level.noCollision(null, aabb) && level.getEntities(null, aabb).isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    Consumer<BurrowBeaconEntity> consumer = EntityType.createDefaultStackConfig(serverLevel, stack, useContext.getPlayer());
                    BurrowBeaconEntity burrowBeacon = EntityRegistry.BURROW_BEACON.get().create(serverLevel, stack.getTag(), consumer, pos, MobSpawnType.TRIGGERED, true, true);

                    if (burrowBeacon == null) {
                        return InteractionResult.FAIL;
                    }

                    float yaw = (float) Mth.floor((Mth.wrapDegrees(useContext.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    burrowBeacon.moveTo(burrowBeacon.getX(), burrowBeacon.getY(), burrowBeacon.getZ(), yaw, 0.0F);
                    serverLevel.addFreshEntityWithPassengers(burrowBeacon);

                    burrowBeacon.gameEvent(GameEvent.ENTITY_PLACE, useContext.getPlayer());
                }

                stack.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }
}
