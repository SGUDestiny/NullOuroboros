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
        String msgId = "death.attack." + this.getMsgId();
        Entity causingEntity = this.getEntity();

        if (causingEntity != null && causingEntity.getUUID().equals(victim.getUUID())) {
            return Component.translatable(msgId + ".self", victim.getDisplayName());
        }

        if (causingEntity != null) {
            return Component.translatable(msgId, victim.getDisplayName(), causingEntity.getDisplayName());
        }

        Entity directEntity = this.getDirectEntity();
        if (directEntity != null) {
            return Component.translatable(msgId, victim.getDisplayName(), directEntity.getDisplayName());
        }

        return super.getLocalizedDeathMessage(victim);
    }
}