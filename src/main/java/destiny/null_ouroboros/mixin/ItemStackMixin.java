package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurtAndBreak(int amount, LivingEntity entity, Consumer<LivingEntity> onBroken, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;

        if (entity.level().isClientSide || !self.isDamageableItem()) return;

        if (self.getDamageValue() + amount < self.getMaxDamage()) return;

        if (!(entity instanceof Player player)) return;
        if (!self.is(ItemRegistry.BINARY_SWORD.get())) return;

        ItemStack shard = new ItemStack(ItemRegistry.BINARY_SHARD.get());
        if (self.hasTag()) {
            shard.setTag(self.getTag().copy());
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand) == self) {
                player.setItemInHand(hand, shard);
                break;
            }
        }

        entity.level().playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);

        ci.cancel();
    }
}
