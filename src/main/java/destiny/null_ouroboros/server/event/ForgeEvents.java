package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.manifolding.ManifoldingChunkErasure;
import destiny.null_ouroboros.server.manifolding.ManifoldingErasure;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    private static final int SLEEP_FADE_COMPLETE = 100;

    private static final Set<UUID> SHOWN_REST_MESSAGE = new HashSet<>();

    private static final double KEY_HOLD_SEARCH_RADIUS = 64.0D;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel serverLevel) {
            serverLevel.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                if (event.phase == TickEvent.Phase.START) {
                    cap.serverTick(serverLevel);
                } else if (event.phase == TickEvent.Phase.END) {
                    cap.applyWindToAllEntities(serverLevel);
                    ManifoldingErasure.tick(serverLevel, cap);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
                level.setWeatherParameters(0, 0, false, false);
                level.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(101, level.getServer());
                level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(ManifoldingCapability::scheduleSirenRevalidation);
            }
        }
    }

    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (event.level instanceof ServerLevel level) {
            if (level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
                level.rainLevel = 0;
                level.thunderLevel = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (chunk.getInhabitedTime() != 0) return;

        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () ->
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                if (cap.getPhase() != ManifoldingPhase.ACTIVE) return;

                long chunkPosLong = chunk.getPos().toLong();
                if (cap.isChunkEroded(chunkPosLong)) return;

                ManifoldingChunkErasure.processNewChunk(level, chunk, cap);
                cap.markChunkEroded(chunkPosLong);
            })
        ));
    }

    @SubscribeEvent
    public static void attachToLevel(AttachCapabilitiesEvent<Level> event) {
        if (event.getObject() instanceof ServerLevel) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "manifolding_capability"), new ICapabilitySerializable<CompoundTag>() {
                final ManifoldingCapability instance = new ManifoldingCapability();

                @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                    return cap == CapabilityRegistry.MANIFOLDING_CAPABILITY ? LazyOptional.of(() -> instance).cast() : LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT() {
                    return instance.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    instance.deserializeNBT(nbt);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (VergeOfRealityDimension.isVergeOfReality(event.getEntity().level())) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!VergeOfRealityDimension.isVergeOfReality(player.level()) || !player.isSleeping()) return;

        if (player.getSleepTimer() == SLEEP_FADE_COMPLETE && SHOWN_REST_MESSAGE.add(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.null_ouroboros.cannot_rest_on_verge"), true);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (VergeOfRealityDimension.isVergeOfReality(event.getEntity().level())) {
            SHOWN_REST_MESSAGE.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        double radius = KEY_HOLD_SEARCH_RADIUS;
        for (DusterbikeEntity bike : player.level().getEntitiesOfClass(DusterbikeEntity.class, player.getBoundingBox().inflate(radius, radius, radius))) {
            bike.releaseKeyHoldForPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (isDrivingDusterbike(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isDrivingDusterbike(Player player) {
        return player.getVehicle() instanceof DusterbikeEntity;
    }
}