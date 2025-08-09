package net.dawson.adorablehamsterpets.client.option;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Holds the mod's key mappings. */
public class ModKeyBindings {
    // Translation keys for the category and action.
    public static final String KEY_CATEGORY_HAMSTERPETS = "key.categories.adorablehamsterpets.main";
    public static final String KEY_THROW_HAMSTER = "key.adorablehamsterpets.throw_hamster";

    /** The actual keybinding instance. It will be registered by the platform-specific module. */
    public static KeyBinding THROW_HAMSTER_KEY;

    /** Create the key mapping. Do not register it here! */
    public static void init() {
        THROW_HAMSTER_KEY = new KeyBinding(
                KEY_THROW_HAMSTER,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_HAMSTERPETS);
    }
}