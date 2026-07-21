package destiny.null_ouroboros.server.damage;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class AttributedDamageSource extends DamageSource {
    public AttributedDamageSource(Holder<DamageType> type, Entity directEntity, Entity causingEntity) {
        super(type, directEntity, causingEntity);
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(LivingEntity victim) {
        Entity causingEntity = this.getEntity();

        if (causingEntity != null && causingEntity.equals(victim)) {
            String selfMsgId = this.getMsgId() + ".self";
            return Component.translatable(selfMsgId, victim.getDisplayName());
        }

        return super.getLocalizedDeathMessage(victim);
    }
}