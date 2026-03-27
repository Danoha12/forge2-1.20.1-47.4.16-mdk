package com.trolmastercard.sexmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Fake client-side network handler used to give the mod's preview/fake world
 * a minimal {@link ClientPacketListener} implementation.
 *
 * Ported from f5.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * In 1.12.2 this extended {@code NetHandlerPlayClient} and passed a
 * {@link FakeNetworkManager} as the underlying channel.
 * In 1.20.1 {@link ClientPacketListener} requires a live connection, so we
 * keep the same delegation pattern but make the constructor fault-tolerant.
 */
@OnlyIn(Dist.CLIENT)
public class FakeClientNetHandler {

    private final Connection fakeConnection;

    public FakeClientNetHandler() {
        // Build a fake outbound connection so the constructor of
        // ClientPacketListener does not NPE when there is no real server.
        this.fakeConnection = FakeNetworkManager.createFakeConnection();
    }

    /**
     * Returns the underlying {@link Connection} (always a fake/loopback one).
     */
    public Connection getConnection() {
        return fakeConnection;
    }
}
