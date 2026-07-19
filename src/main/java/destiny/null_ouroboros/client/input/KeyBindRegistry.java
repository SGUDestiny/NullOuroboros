package destiny.null_ouroboros.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NullOuroboros.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindRegistry {
    public static final String CATEGORY = "key.categories.null_ouroboros";

    public static final KeyMapping FORWARD = new KeyMapping(
            "key.null_ouroboros.dusterbike.forward",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W,
            CATEGORY
    );
    public static final KeyMapping BACKWARD = new KeyMapping(
            "key.null_ouroboros.dusterbike.backward",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_S,
            CATEGORY
    );
    public static final KeyMapping STEER_LEFT = new KeyMapping(
            "key.null_ouroboros.dusterbike.steer_left",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            CATEGORY
    );
    public static final KeyMapping STEER_RIGHT = new KeyMapping(
            "key.null_ouroboros.dusterbike.steer_right",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_D,
            CATEGORY
    );
    public static final KeyMapping HANDBRAKE = new KeyMapping(
            "key.null_ouroboros.dusterbike.handbrake",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            CATEGORY
    );
    public static final KeyMapping SHIFT_UP = new KeyMapping(
            "key.null_ouroboros.dusterbike.shift_up",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    public static final KeyMapping SHIFT_DOWN = new KeyMapping(
            "key.null_ouroboros.dusterbike.shift_down",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );
    public static final KeyMapping HEADLIGHTS = new KeyMapping(
            "key.null_ouroboros.dusterbike.headlights",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    private KeyBindRegistry() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FORWARD);
        event.register(BACKWARD);
        event.register(STEER_LEFT);
        event.register(STEER_RIGHT);
        event.register(HANDBRAKE);
        event.register(SHIFT_UP);
        event.register(SHIFT_DOWN);
        event.register(HEADLIGHTS);
    }
}
