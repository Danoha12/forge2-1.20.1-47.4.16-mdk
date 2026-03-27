package com.trolmastercard.sexmod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * VersionChecker - ported from c6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * CLIENT-ONLY class that originally fetched a version.txt from schnurrritv.com
 * and prompted the player to update if outdated.  The tick handler body was
 * empty in the decompiled source (all logic removed/obfuscated away).
 *
 * In 1.20.1 this is kept as a no-op stub.
 *
 * Field mapping:
 *   c = versionUrl  (https://schnurrritv.com/version.txt)
 *   a = twitterUrl  (https://twitter.com/Schnurri_tv)
 *   b = hasChecked  (boolean)
 *
 * In 1.12.2:
 *   @SideOnly(Side.CLIENT) - @OnlyIn(Dist.CLIENT)
 *   TickEvent.ClientTickEvent handler signature unchanged in 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class VersionChecker {

    private final String versionUrl = "https://schnurrritv.com/version.txt";
    private final String twitterUrl = "https://twitter.com/Schnurri_tv";
    private boolean hasChecked = false;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // No-op stub - version check logic not present in decompiled source.
    }
}
