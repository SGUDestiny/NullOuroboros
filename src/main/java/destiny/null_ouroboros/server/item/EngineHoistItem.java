package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EngineHoistItem extends Item {
    public EngineHoistItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos pos = placeContext.getClickedPos();
        Vec3 posVec = Vec3.atBottomCenterOf(pos);
        AABB aabb = EntityRegistry.ENGINE_HOIST.get().getDimensions().makeBoundingBox(posVec.x(), posVec.y(), posVec.z());
        if (!level.noCollision(null, aabb) || !level.getEntities(null, aabb).isEmpty()) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            EngineHoistEntity hoist = EntityRegistry.ENGINE_HOIST.get().create(serverLevel);
            if (hoist == null) {
                return InteractionResult.FAIL;
            }
            float yaw = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
            hoist.moveTo(posVec.x(), posVec.y(), posVec.z(), yaw, 0.0F);
            hoist.setFacing(context.getPlayer());
            serverLevel.addFreshEntity(hoist);
            hoist.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
