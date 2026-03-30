package com.trolmastercard.sexmod.network.fake;

import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * FakeClientNetHandler — Portado a 1.20.1.
 * * Manejador de red cliente falso usado para mundos de previsualización (Menús/UI).
 * * Envuelve una Connection dummy para evitar los crashes por falta de telemetría y perfil
 * * que exige el ClientPacketListener moderno.
 */
@OnlyIn(Dist.CLIENT)
public class FakeClientNetHandler {

    private final Connection fakeConnection;

    public FakeClientNetHandler() {
        // Construimos una conexión de salida falsa para que el renderizador del
        // mundo falso no tire un NPE (NullPointerException) al intentar enviar paquetes.
        this.fakeConnection = FakeNetworkManager.createFakeConnection();
    }

    /**
     * Retorna la conexión subyacente (siempre será una falsa/loopback).
     */
    public Connection getConnection() {
        return this.fakeConnection;
    }
}