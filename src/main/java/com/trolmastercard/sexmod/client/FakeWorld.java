package com.trolmastercard.sexmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * FakeWorld - returns the current ClientLevel or null.
 * Simplified for 1.20.1 since ClientLevel constructor is not public.
 */
@OnlyIn(Dist.CLIENT)
public class FakeWorld {

    private final ClientLevel level;

    public FakeWorld() {
        this.level = Minecraft.getInstance().level;
    }

    public ClientLevel getLevel() {
        return level;
    }

    public static ClientLevel get() {
        return Minecraft.getInstance().level;
    }
}