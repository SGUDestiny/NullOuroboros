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

    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_START = registerSoundEvent("mechanical_siren_start");
    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_START_DISTANT = registerSoundEvent("mechanical_siren_start_distant");
    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_LOOP = registerSoundEvent("mechanical_siren_loop");
    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_LOOP_DISTANT = registerSoundEvent("mechanical_siren_loop_distant");
    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_END = registerSoundEvent("mechanical_siren_end");
    public static final RegistryObject<SoundEvent> MECHANICAL_SIREN_END_DISTANT = registerSoundEvent("mechanical_siren_end_distant");

    public static final RegistryObject<SoundEvent> BURROW_BEACON_DEPLOY = registerSoundEvent("burrow_beacon_deploy");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_LAND = registerSoundEvent("burrow_beacon_land");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_DRILL = registerSoundEvent("burrow_beacon_drill");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_CONNECT = registerSoundEvent("burrow_beacon_connect");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_DISCONNECT = registerSoundEvent("burrow_beacon_disconnect");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_HIT = registerSoundEvent("burrow_beacon_hit");
    public static final RegistryObject<SoundEvent> BURROW_BEACON_BREAK = registerSoundEvent("burrow_beacon_break");

    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_START = registerSoundEvent("dusty_computer_start");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_LOOP = registerSoundEvent("dusty_computer_loop");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_STOP = registerSoundEvent("dusty_computer_stop");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_TYPE = registerSoundEvent("dusty_computer_type");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_ENTER = registerSoundEvent("dusty_computer_enter");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_ERASE = registerSoundEvent("dusty_computer_erase");

    private static RegistryObject<SoundEvent> registerSoundEvent(String sound) {
        return SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, sound)));
    }
}
