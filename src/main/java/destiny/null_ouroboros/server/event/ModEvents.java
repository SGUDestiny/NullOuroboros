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
        CommandRegistry.registerPrimary("cd", "message.null_ouroboros.terminus.cd.usage", CommandCd::new);
        CommandRegistry.registerAlias("chdir", "cd");

        CommandRegistry.registerPrimary("ls", "message.null_ouroboros.terminus.ls.usage", CommandLs::new);
        CommandRegistry.registerAlias("dir", "ls");

        CommandRegistry.registerPrimary("rm", "message.null_ouroboros.terminus.rm.usage", CommandRm::new);
        CommandRegistry.registerAlias("rmdir", "rm");
        CommandRegistry.registerAlias("del", "rm");

        CommandRegistry.registerPrimary("ct", "message.null_ouroboros.terminus.ct.usage", CommandCt::new);
        CommandRegistry.registerAlias("mk", "ct");

        CommandRegistry.registerPrimary("ed", "message.null_ouroboros.terminus.ed.usage", CommandEd::new);

        CommandRegistry.registerPrimary("rn", "message.null_ouroboros.terminus.rn.usage", CommandRn::new);
        CommandRegistry.registerAlias("ren", "rn");

        CommandRegistry.registerPrimary("mv", "message.null_ouroboros.terminus.mv.usage", CommandMv::new);

        CommandRegistry.registerPrimary("help", "message.null_ouroboros.terminus.help.usage", CommandHelp::new);

        CommandRegistry.registerPrimary("shutdown", "message.null_ouroboros.terminus.shutdown.usage", CommandShutdown::new);

        CommandRegistry.registerPrimary("sr", "message.null_ouroboros.terminus.sr.usage", CommandSr::new);
        CommandRegistry.registerAlias("search", "sr");

        CommandRegistry.registerPrimary("cl", "message.null_ouroboros.terminus.cl.usage", CommandCl::new);
        CommandRegistry.registerAlias("clear", "cl");
    }
}
