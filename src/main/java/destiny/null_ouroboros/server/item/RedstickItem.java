package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.entity.RedstickEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RedstickItem extends Item {
    private static final int MAX_CHARGE_TICKS = 25;
    private static final int USE_DURATION_TICKS = 72000;
    private static final double MAX_THROW_SPEED = 1.2D;
    private static final double MAX_SPIN_SPEED = 0.35D;

    public RedstickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        int chargeTicks = getUseDuration(stack) - timeLeft;
        double charge = getCharge(chargeTicks);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.DROPLIGHT_DROP.get(), SoundSource.PLAYERS, 0.5f, 1f);

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            Vec3 start = player.getEyePosition().add(look.scale(0.75D));
            Vec3 velocity = look.scale(MAX_THROW_SPEED * charge).add(player.getDeltaMovement());
            RedstickEntity.spawnThrown(level, start, look, velocity, MAX_SPIN_SPEED * charge);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private static double getCharge(int chargeTicks) {
        return Math.min(1.0D, Math.max(0.0D, chargeTicks / (double) MAX_CHARGE_TICKS));
    }
}
