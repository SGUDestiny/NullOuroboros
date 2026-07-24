package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.client.item.HeavyRevolverClientExtensions;
import destiny.null_ouroboros.common.player_anim.HeavyRevolverPlayerAnims;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.common.player_anim.PlayerAnimTracker;
import destiny.null_ouroboros.common.player_anim.PlayerAnimation;
import destiny.null_ouroboros.server.entity.BulletEntity;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class HeavyRevolverItem extends Item implements GeoItem {
    private static final Set<UUID> DRAW_DONE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HeavyRevolverItem(Properties properties) {
        super(properties);
    }

    public static void clearDrawState(UUID playerId) {
        DRAW_DONE.remove(playerId);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        boolean mainHandSelected = isSelected && player.getMainHandItem() == stack;
        PlayerAnimInstance active = PlayerAnimTracker.get(player.getUUID());

        if (mainHandSelected) {
            if (active != null && HeavyRevolverPlayerAnims.SHOOT_ID.equals(active.animationId())) {
                long elapsed = level.getGameTime() - active.startGameTime();
                long shootTicks = Math.max(1L, Math.round(HeavyRevolverPlayerAnims.SHOOT_LENGTH_SECONDS * 20.0F));
                if (elapsed >= shootTicks) {
                    DRAW_DONE.add(player.getUUID());
                    PlayerAnimation.playAtEnd(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
                }
                return;
            }
            if (active != null && HeavyRevolverPlayerAnims.HOLD_ID.equals(active.animationId())) {
                DRAW_DONE.add(player.getUUID());
                return;
            }
            if (DRAW_DONE.contains(player.getUUID())) {
                PlayerAnimation.playAtEnd(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
            } else {
                level.playSound(null, player.blockPosition(), SoundRegistry.FIREARM_HOLSTER_OUT.get(), SoundSource.PLAYERS, 1f, 1f);
                PlayerAnimation.play(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
            }
            return;
        }

        if (active != null && HeavyRevolverPlayerAnims.isRevolverAnim(active.animationId())) {
            PlayerAnimation.cancelAll(player);
            DRAW_DONE.remove(player.getUUID());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
            player.getCapability(CapabilityRegistry.RECOIL_CAPABILITY).ifPresent(recoil -> {
                recoil.addRecoil(15.0F, ModUtil.getBoundRandomFloatStatic(level, -10, 10), 0.2F);
            });
        } else {
            level.playSound(null, player.blockPosition(), SoundRegistry.HEAVY_REVOLVER_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);

            Vec3 look = player.getLookAngle();
            BulletEntity bullet = EntityRegistry.BULLET.get().create(level);
            if (bullet != null) {
                bullet.setOwner(player);
                bullet.setPos(player.getEyePosition().add(look.scale(0.5)));
                bullet.shoot(look.x, look.y, look.z, 4F);
                bullet.setDeltaMovement(bullet.getDeltaMovement().add(player.getDeltaMovement()));
                level.addFreshEntity(bullet);
            }

            DRAW_DONE.add(player.getUUID());
            PlayerAnimation.play(player, HeavyRevolverPlayerAnims.SHOOT_ID, HeavyRevolverPlayerAnims.shootOptions());
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HeavyRevolverClientExtensions.register(consumer));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
