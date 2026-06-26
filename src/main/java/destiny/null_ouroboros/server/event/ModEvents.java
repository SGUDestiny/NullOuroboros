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

        CommandRegistry.registerPrimary("cr", "message.null_ouroboros.terminus.cr.usage", CommandCr::new);
        CommandRegistry.registerAlias("mk", "ct");

        CommandRegistry.registerPrimary("ed", "message.null_ouroboros.terminus.ed.usage", CommandEd::new);

        CommandRegistry.registerPrimary("rn", "message.null_ouroboros.terminus.rn.usage", CommandRn::new);
        CommandRegistry.registerAlias("ren", "rn");

        CommandRegistry.registerPrimary("mv", "message.null_ouroboros.terminus.mv.usage", CommandMv::new);

        CommandRegistry.registerPrimary("dp", "message.null_ouroboros.terminus.dp.usage", CommandDp::new);

        CommandRegistry.registerPrimary("ipvinf", "message.null_ouroboros.terminus.ipvinf.usage", CommandIpvinf::new);

        CommandRegistry.registerPrimary("help", "message.null_ouroboros.terminus.help.usage", CommandHelp::new);

        CommandRegistry.registerPrimary("shutdown", "message.null_ouroboros.terminus.shutdown.usage", CommandShutdown::new);

        CommandRegistry.registerPrimary("sr", "message.null_ouroboros.terminus.sr.usage", CommandSr::new);
        CommandRegistry.registerAlias("search", "sr");

        CommandRegistry.registerPrimary("cl", "message.null_ouroboros.terminus.cl.usage", CommandCl::new);
        CommandRegistry.registerAlias("clear", "cl");

        CommandRegistry.registerPrimary("forecast", "message.null_ouroboros.terminus.forecast.usage", CommandForecast::new);
        CommandRegistry.registerPrimary("p2p", "message.null_ouroboros.terminus.p2p.usage", CommandP2p::new);

        CommandRegistry.registerHelp("p2p alias", "message.null_ouroboros.terminus.p2p.alias.usage");
        CommandRegistry.registerHelp("p2p filter", "message.null_ouroboros.terminus.p2p.filter.usage");
        CommandRegistry.registerHelp("p2p filter file", "message.null_ouroboros.terminus.p2p.filter.file.usage");
        CommandRegistry.registerHelp("p2p filter add", "message.null_ouroboros.terminus.p2p.filter.add.usage");
        CommandRegistry.registerHelp("p2p filter remove", "message.null_ouroboros.terminus.p2p.filter.remove.usage");
        CommandRegistry.registerHelp("p2p log_directory", "message.null_ouroboros.terminus.p2p.log_directory.usage");
        CommandRegistry.registerHelp("p2p reciever_directory", "message.null_ouroboros.terminus.p2p.reciever_directory.usage");
        CommandRegistry.registerHelp("p2p request", "message.null_ouroboros.terminus.p2p.request.usage");
        CommandRegistry.registerHelp("p2p disconnect", "message.null_ouroboros.terminus.p2p.disconnect.usage");
        CommandRegistry.registerHelp("p2p send", "message.null_ouroboros.terminus.p2p.send.usage");
    }
}
