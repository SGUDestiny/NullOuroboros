package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.terminal.CommandRegistry;
import destiny.null_ouroboros.server.terminal.command.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = NullOuroboros.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.BURROW_BEACON.get(), BurrowBeaconEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        CommandRegistry.register("cd", CommandCd::new);
        CommandRegistry.register("chdir", CommandCd::new);
        CommandRegistry.register("ls", CommandLs::new);
        CommandRegistry.register("dir", CommandLs::new);
        CommandRegistry.register("rm", CommandRm::new);
        CommandRegistry.register("rmdir", CommandRm::new);
        CommandRegistry.register("del", CommandRm::new);
        CommandRegistry.register("ct", CommandCt::new);
        CommandRegistry.register("mk", CommandCt::new);
        CommandRegistry.register("rn", CommandRn::new);
        CommandRegistry.register("ren", CommandRn::new);
        CommandRegistry.register("mv", CommandMv::new);
    }
}