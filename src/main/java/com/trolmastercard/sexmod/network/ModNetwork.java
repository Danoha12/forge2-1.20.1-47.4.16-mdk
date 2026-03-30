package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.network.packet.*;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * ModNetwork — Portado a 1.20.1.
 * * Orquestador de paquetes de red.
 * * Asegúrate de llamar a ModNetwork.register() en el constructor de tu clase principal del Mod.
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static int id = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModConstants.MOD_ID, "main_channel"))
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static int nextId() { return id++; }

    public static void register() {
        // ── BIDIRECCIONALES (Request & Response) ─────────────────────────────

        // ModelList: Cliente pide la lista, Servidor la envía y abre la GUI
        regBi(ModelListPacket.class, ModelListPacket::encode, ModelListPacket::decode, ModelListPacket::handle);

        regBi(SendChatMessagePacket.class, SendChatMessagePacket::encode, SendChatMessagePacket::decode, SendChatMessagePacket::handle);
        regBi(SexPromptPacket.class, SexPromptPacket::encode, SexPromptPacket::decode, SexPromptPacket::handle);
        regBi(SyncCustomModelsPacket.class, SyncCustomModelsPacket::encode, SyncCustomModelsPacket::decode, SyncCustomModelsPacket::handle);

        // ── CLIENTE -> SERVIDOR (PLAY_TO_SERVER) ─────────────────────────────

        reg(UpdatePlayerModelPacket.class, UpdatePlayerModelPacket::encode, UpdatePlayerModelPacket::decode, UpdatePlayerModelPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(ChangeDataParameterPacket.class, ChangeDataParameterPacket::encode, ChangeDataParameterPacket::decode, ChangeDataParameterPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(OpenNpcInventoryPacket.class, OpenNpcInventoryPacket::getEncode, OpenNpcInventoryPacket::decode, OpenNpcInventoryPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(SyncInventoryPacket.class, SyncInventoryPacket::encode, SyncInventoryPacket::decode, SyncInventoryPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(RemoveItemsPacket.class, RemoveItemsPacket::encode, RemoveItemsPacket::decode, RemoveItemsPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(CatThrowAwayItemPacket.class, CatThrowAwayItemPacket::encode, CatThrowAwayItemPacket::decode, CatThrowAwayItemPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(SetNpcHomePacket.class, SetNpcHomePacket::encode, SetNpcHomePacket::decode, SetNpcHomePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(AcceptSexPacket.class, AcceptSexPacket::encode, AcceptSexPacket::decode, AcceptSexPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        // ── SERVIDOR -> CLIENTE (PLAY_TO_CLIENT) ─────────────────────────────

        // CameraControl: El servidor le dice al cliente "bloquea la cámara porque empezó la animación"
        reg(CameraControlPacket.class, CameraControlPacket::encode, CameraControlPacket::decode, CameraControlPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(SetPlayerCamPacket.class, SetPlayerCamPacket::encode, SetPlayerCamPacket::decode, SetPlayerCamPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(UpdateEquipmentPacket.class, UpdateEquipmentPacket::encode, UpdateEquipmentPacket::decode, UpdateEquipmentPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(ForcePlayerGirlUpdatePacket.class, ForcePlayerGirlUpdatePacket::encode, ForcePlayerGirlUpdatePacket::decode, ForcePlayerGirlUpdatePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
    }

    // ── Helpers Funcionales ──────────────────────────────────────────────────

    private static <T> void reg(Class<T> cls, Encoder<T> enc, Decoder<T> dec, Handler<T> handler, NetworkDirection dir) {
        CHANNEL.registerMessage(nextId(), cls, enc::encode, dec::decode, handler::handle, Optional.of(dir));
    }

    private static <T> void regBi(Class<T> cls, Encoder<T> enc, Decoder<T> dec, Handler<T> handler) {
        CHANNEL.registerMessage(nextId(), cls, enc::encode, dec::decode, handler::handle, Optional.empty());
    }

    @FunctionalInterface interface Encoder<T> { void encode(T msg, FriendlyByteBuf buf); }
    @FunctionalInterface interface Decoder<T> { T decode(FriendlyByteBuf buf); }
    @FunctionalInterface interface Handler<T> { void handle(T msg, Supplier<NetworkEvent.Context> ctx); }
}