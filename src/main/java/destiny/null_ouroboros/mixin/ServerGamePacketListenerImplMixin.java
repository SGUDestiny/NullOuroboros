package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Redirect(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"
            )
    )
    private boolean nullOuroboros$allowDusterbikeBodyThroughSteps(ServerLevel level, Entity entity, AABB box) {
        if (entity instanceof DusterbikeEntity) {
            return true;
        }
        return level.noCollision(entity, box);
    }
}
