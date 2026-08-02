package destiny.null_ouroboros.server.damage;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SimpleDeathMessageDamageSource extends DamageSource {
    public SimpleDeathMessageDamageSource(Holder<DamageType> type) {
        super(type);
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.translatable("death.attack." + this.getMsgId(), victim.getDisplayName());
    }
}
