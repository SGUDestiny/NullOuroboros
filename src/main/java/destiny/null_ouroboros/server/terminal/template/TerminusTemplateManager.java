package destiny.null_ouroboros.server.terminal.template;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = NullOuroboros.MODID)
public final class TerminusTemplateManager implements PreparableReloadListener {
    public static final TerminusTemplateManager INSTANCE = new TerminusTemplateManager();

    private Map<ResourceLocation, TerminusTemplate> datapackTemplates = Map.of();
    private Map<String, TerminusTemplate> worldSaveTemplates = Map.of();
    @Nullable
    private MinecraftServer server;

    private TerminusTemplateManager() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        INSTANCE.server = event.getServer();
        INSTANCE.reloadWorldSaveTemplates();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> TerminusTemplateParser.parseDatapackTemplates(resourceManager), backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(parsed -> {
                    datapackTemplates = Collections.unmodifiableMap(new HashMap<>(parsed));
                    reloadWorldSaveTemplates();
                    NullOuroboros.LOGGER.info("Loaded {} datapack terminus templates", datapackTemplates.size());
                }, gameExecutor);
    }

    public void reloadWorldSaveTemplates() {
        if (server == null) {
            worldSaveTemplates = Map.of();
            return;
        }
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("terminus_templates");
        worldSaveTemplates = Collections.unmodifiableMap(new HashMap<>(TerminusTemplateParser.parseWorldSaveTemplates(root)));
        NullOuroboros.LOGGER.info("Loaded {} world-save terminus templates", worldSaveTemplates.size());
    }

    public Path getWorldSaveTemplatesRoot() {
        if (server == null) {
            return null;
        }
        return server.getWorldPath(LevelResource.ROOT).resolve("terminus_templates");
    }

    @Nullable
    public TerminusTemplate get(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        TerminusTemplate world = worldSaveTemplates.get(id.getPath());
        if (world != null) {
            return world;
        }
        return datapackTemplates.get(id);
    }

    @Nullable
    public TerminusTemplate getByWorldSaveName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        TerminusTemplate world = worldSaveTemplates.get(name);
        if (world != null) {
            return world;
        }
        for (Map.Entry<ResourceLocation, TerminusTemplate> entry : datapackTemplates.entrySet()) {
            if (entry.getKey().getPath().equals(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<ResourceLocation, TerminusTemplate> getDatapackTemplates() {
        return datapackTemplates;
    }

    public Map<String, TerminusTemplate> getWorldSaveTemplates() {
        return worldSaveTemplates;
    }
}
