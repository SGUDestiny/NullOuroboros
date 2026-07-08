package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DusterbikeFrameItem extends Item {
    private final boolean built;

    public DusterbikeFrameItem(Properties properties, boolean built) {
        super(properties);
        this.built = built;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        DusterbikeEntity bike = new DusterbikeEntity(EntityRegistry.DUSTERBIKE.get(), level);
        bike.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        bike.setYRot(player.getYRot() - 90);
        level.addFreshEntity(bike);

        if (built) {
            bike.initializeBuiltComponents();
        } else {
            bike.initializeEmptyFrame();
        }

        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}