package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.client.render.item.HeavyRevolverGeoRenderer;
import destiny.null_ouroboros.server.entity.BulletEntity;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class HeavyRevolverItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HeavyRevolverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.fail(stack);

        if (level.isClientSide()) {
            player.getCapability(CapabilityRegistry.RECOIL_CAPABILITY).ifPresent(recoil -> {
                recoil.addRecoil(15.0F, ModUtil.getBoundRandomFloatStatic(level, -10, 10), 0.2F);
            });
        } else {
            level.playSound(null, player.blockPosition(), SoundRegistry.HEAVY_REVOLVER_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);

            Vec3 look = player.getLookAngle();
            BulletEntity bullet = EntityRegistry.BULLET.get().create(level);
            if (bullet != null) {
                bullet.setPos(player.getEyePosition().add(look.scale(0.5)));
                bullet.shoot(look.x, look.y, look.z, 3F);
                level.addFreshEntity(bullet);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HeavyRevolverGeoRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new HeavyRevolverGeoRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
