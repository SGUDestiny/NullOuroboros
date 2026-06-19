package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NullOuroboros.MODID);

    public static final RegistryObject<SoundEvent> DROPLIGHT_ON = registerSoundEvent("droplight_on");
    public static final RegistryObject<SoundEvent> DROPLIGHT_OFF = registerSoundEvent("droplight_off");
    public static final RegistryObject<SoundEvent> DROPLIGHT_FLICKER = registerSoundEvent("droplight_flicker");
    public static final RegistryObject<SoundEvent> DROPLIGHT_DROP = registerSoundEvent("droplight_drop");

    public static final RegistryObject<SoundEvent> STROBELIGHT_ALARM = registerSoundEvent("strobelight_alarm");

    public static final RegistryObject<SoundEvent> MANIFOLDING_START = registerSoundEvent("manifolding_start");
    public static final RegistryObject<SoundEvent> MANIFOLDING_LOOP = registerSoundEvent("manifolding_loop");
    public static final RegistryObject<SoundEvent> MANIFOLDING_END = registerSoundEvent("manifolding_end");
    public static final RegistryObject<SoundEvent> MANIFOLDING_START_MUFFLED = registerSoundEvent("manifolding_start_muffled");
    public static final RegistryObject<SoundEvent> MANIFOLDING_LOOP_MUFFLED = registerSoundEvent("manifolding_loop_muffled");
    public static final RegistryObject<SoundEvent> MANIFOLDING_END_MUFFLED = registerSoundEvent("manifolding_end_muffled");
    public static final RegistryObject<SoundEvent> MANIFOLDING_THUNDER = registerSoundEvent("manifolding_thunder");

    private static RegistryObject<SoundEvent> registerSoundEvent(String sound) {
        return SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, sound)));
    }
}
