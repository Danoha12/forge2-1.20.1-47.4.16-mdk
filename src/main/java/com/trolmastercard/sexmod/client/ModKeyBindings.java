package com.trolmastercard.sexmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.sexmod";

    // Teclas de ejemplo: 'R' para acción, 'UP'/'DOWN' para altura
    public static final KeyMapping ACTION_KEY = new KeyMapping(
            "key.sexmod.action",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping FLY_UP = new KeyMapping(
            "key.sexmod.fly_up",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            CATEGORY
    );

    public static final KeyMapping FLY_DOWN = new KeyMapping(
            "key.sexmod.fly_down",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DOWN,
            CATEGORY
    );
}