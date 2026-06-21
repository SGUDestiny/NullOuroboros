package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.level instanceof ServerLevel serverLevel) {
            serverLevel.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                cap.serverTick(serverLevel);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (event.player.level() instanceof ServerLevel serverLevel) {
            serverLevel.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                cap.applyWindToEntity(event.player, serverLevel);
            });
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
                level.setWeatherParameters(0, 0, false, false);
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
}
