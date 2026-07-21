package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NullOuroboros.MODID);
    public static final RegistryObject<SoundEvent> VERGE_ENTRY = registerSoundEvent("verge_entry");

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
    public static final RegistryObject<SoundEvent> VERGE_AMBIENCE = registerSoundEvent("verge_ambience");

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
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_LOAD = registerSoundEvent("dusty_computer_load");
    public static final RegistryObject<SoundEvent> DUSTY_COMPUTER_LOAD_SHORT = registerSoundEvent("dusty_computer_load_short");

    public static final RegistryObject<SoundEvent> STOP_SIGN_HIT = registerSoundEvent("stop_sign_hit");

    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_IDLE = registerSoundEvent("dusterbike_engine_idle");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_GEAR_1 = registerSoundEvent("dusterbike_engine_gear_1");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_GEAR_2 = registerSoundEvent("dusterbike_engine_gear_2");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_GEAR_3 = registerSoundEvent("dusterbike_engine_gear_3");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_REVERSE = registerSoundEvent("dusterbike_engine_reverse");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_ENGINE_OFF = registerSoundEvent("dusterbike_engine_off");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_IGNITION_START = registerSoundEvent("dusterbike_ignition_start");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_IGNITION_ATTEMPT = registerSoundEvent("dusterbike_ignition_attempt");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_IGNITION_SUCCESS = registerSoundEvent("dusterbike_ignition_success");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_GEAR_CHANGE_1 = registerSoundEvent("dusterbike_gear_change_1");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_GEAR_CHANGE_2 = registerSoundEvent("dusterbike_gear_change_2");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_GEAR_CHANGE_3 = registerSoundEvent("dusterbike_gear_change_3");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_HEADLIGHT_ON = registerSoundEvent("dusterbike_headlight_on");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_HEADLIGHT_OFF = registerSoundEvent("dusterbike_headlight_off");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_PART_INSTALL = registerSoundEvent("dusterbike_part_install");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_KEY_INSERT = registerSoundEvent("dusterbike_key_insert");
    public static final RegistryObject<SoundEvent> DUSTERBIKE_FUEL_POUR = registerSoundEvent("dusterbike_fuel_pour");

    public static final RegistryObject<SoundEvent> WRENCH_INTERACT = registerSoundEvent("wrench_interact");
    public static final RegistryObject<SoundEvent> SPRAY_CAN_INTERACT = registerSoundEvent("spray_can_interact");
    public static final RegistryObject<SoundEvent> ENGINE_HOIST_INTERACT = registerSoundEvent("engine_hoist_interact");

    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_PHASE_1_INTRO = registerSoundEvent("steel_leviathan_phase_1_intro");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_PHASE_1_LOOP = registerSoundEvent("steel_leviathan_phase_1_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_PHASE_2_INTRO = registerSoundEvent("steel_leviathan_phase_2_intro");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_PHASE_2_LOOP = registerSoundEvent("steel_leviathan_phase_2_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_PHASE_2_OUTRO = registerSoundEvent("steel_leviathan_phase_2_outro");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_UNDERGROUND_LOOP = registerSoundEvent("steel_leviathan_underground_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_BREACHING_GROUND_LOOP = registerSoundEvent("steel_leviathan_breaching_ground_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_SCAN_LOOP = registerSoundEvent("steel_leviathan_scan_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_BREACH_GROUND = registerSoundEvent("steel_leviathan_breach_ground");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_HEATSINK_HIT = registerSoundEvent("steel_leviathan_heatsink_hit");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_HEATSINK_HISS = registerSoundEvent("steel_leviathan_heatsink_hiss");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_SEGMENT_OVERHEAT = registerSoundEvent("steel_leviathan_segment_overheat");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_OVERHEAT_STALL = registerSoundEvent("steel_leviathan_overheat_stall");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_METAL_HIT = registerSoundEvent("steel_leviathan_metal_hit");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_DEATH = registerSoundEvent("steel_leviathan_death");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_ENGINE_LOOP = registerSoundEvent("steel_leviathan_engine_loop");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_ENGINE_IGNITE = registerSoundEvent("steel_leviathan_engine_ignite");
    public static final RegistryObject<SoundEvent> STEEL_LEVIATHAN_MISSILE_LAUNCH = registerSoundEvent("steel_leviathan_missile_launch");

    private static RegistryObject<SoundEvent> registerSoundEvent(String sound) {
        return SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, sound)));
    }
}
