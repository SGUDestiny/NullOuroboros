package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Map;

public final class AdvancementMusicManager {
    private static final float VOLUME = 0.35F;
    private static final float FADE_SPEED = VOLUME / (1.0F * 20.0F);

    private static final Map<ResourceLocation, RegistryObject<SoundEvent>> STING_BY_ADVANCEMENT = Map.of(
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "verge_entry"),
            SoundRegistry.VERGE_ENTRY,
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_sighted"),
            SoundRegistry.STEEL_LEVIATHAN_SIGHTED
    );

    @Nullable private static AdvancementMusicSoundInstance current;
    @Nullable private static AdvancementMusicSoundInstance outgoing;

    private AdvancementMusicManager() {}

    public static void onAdvancementEarned(ResourceLocation advancementId) {
        RegistryObject<SoundEvent> sound = STING_BY_ADVANCEMENT.get(advancementId);
        if (sound == null) {
            return;
        }
        if (SteelLeviathanBossMusicManager.isPlaying()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean crossfade = isActive(current);
        if (crossfade) {
            current.setTargetVolume(0.0F);
            if (isActive(outgoing)) {
                stopInstance(mc, outgoing);
            }
            outgoing = current;
            current = null;
        }

        AdvancementMusicSoundInstance sting = new AdvancementMusicSoundInstance(sound.get(), FADE_SPEED);
        if (crossfade) {
            sting.forceVolume(0.0F);
            sting.setTargetVolume(VOLUME);
        } else {
            sting.forceVolume(VOLUME);
        }
        current = sting;
        mc.getSoundManager().play(sting);
    }

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (outgoing != null && outgoing.isStopped()) {
            outgoing = null;
        }
        if (current != null && current.isStopped()) {
            current = null;
        }

        if (SteelLeviathanBossMusicManager.isPlaying()) {
            if (isActive(current)) {
                current.setTargetVolume(0.0F);
            }
            if (isActive(outgoing)) {
                outgoing.setTargetVolume(0.0F);
            }
        }
    }

    public static void stopAll() {
        Minecraft mc = Minecraft.getInstance();
        stopInstance(mc, current);
        stopInstance(mc, outgoing);
        current = null;
        outgoing = null;
    }

    private static boolean isActive(@Nullable AdvancementMusicSoundInstance instance) {
        return instance != null && !instance.isStopped();
    }

    private static void stopInstance(Minecraft mc, @Nullable AdvancementMusicSoundInstance instance) {
        if (instance != null) {
            mc.getSoundManager().stop(instance);
        }
    }
}
