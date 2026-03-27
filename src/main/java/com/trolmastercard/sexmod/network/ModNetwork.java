package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * ModNetwork - registers all network packets for the mod.
 * Ported from ge.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original: SimpleNetworkWrapper - SimpleChannel
 *   ge.b  - ModNetwork.CHANNEL
 *   ge.a() - ModNetwork.register()
 *
 * Migration notes:
 *   NetworkRegistry.INSTANCE.newSimpleChannel("sexmodchannel")
 *     - NetworkRegistry.newSimpleChannel(ResourceLocation, version, ...)
 *   b.registerMessage(HandlerClass, MsgClass, id, side)
 *     - CHANNEL.registerMessage(id, MsgClass, encode, decode, handle, Optional<side>)
 *
 * All packet class names are their ported (deobfuscated) equivalents.
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("sexmod", "sexmodchannel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;
    private static int nextId() { return id++; }

    public static void register() {
        // -- Bidirectional -----------------------------------------------------
        CHANNEL.registerMessage(nextId(), SendChatMessagePacket.class,
                SendChatMessagePacket::encode, SendChatMessagePacket::decode,
                SendChatMessagePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), SendChatMessagePacket.class,
                SendChatMessagePacket::encode, SendChatMessagePacket::decode,
                SendChatMessagePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), ResetControllerPacket.class,
                ResetControllerPacket::encode, ResetControllerPacket::decode,
                ResetControllerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), ResetControllerPacket.class,
                ResetControllerPacket::encode, ResetControllerPacket::decode,
                ResetControllerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), TribeUIValuesPacket.class,
                TribeUIValuesPacket::encode, TribeUIValuesPacket::decode,
                TribeUIValuesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), TribeUIValuesPacket.class,
                TribeUIValuesPacket::encode, TribeUIValuesPacket::decode,
                TribeUIValuesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), SexPromptPacket.class,
                SexPromptPacket::encode, SexPromptPacket::decode,
                SexPromptPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), SexPromptPacket.class,
                SexPromptPacket::encode, SexPromptPacket::decode,
                SexPromptPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), TribeHighlightPacket.class,
                TribeHighlightPacket::encode, TribeHighlightPacket::decode,
                TribeHighlightPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), TribeHighlightPacket.class,
                TribeHighlightPacket::encode, TribeHighlightPacket::decode,
                TribeHighlightPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), RequestServerModelAvailabilityPacket.class,
                RequestServerModelAvailabilityPacket::encode, RequestServerModelAvailabilityPacket::decode,
                RequestServerModelAvailabilityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), RequestServerModelAvailabilityPacket.class,
                RequestServerModelAvailabilityPacket::encode, RequestServerModelAvailabilityPacket::decode,
                RequestServerModelAvailabilityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextId(), SyncCustomModelsPacket.class,
                SyncCustomModelsPacket::encode, SyncCustomModelsPacket::decode,
                SyncCustomModelsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId(), SyncCustomModelsPacket.class,
                SyncCustomModelsPacket::encode, SyncCustomModelsPacket::decode,
                SyncCustomModelsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // -- Server-bound (PLAY_TO_SERVER) -------------------------------------
        reg(CameraControlPacket.class,
                CameraControlPacket::encode, CameraControlPacket::decode,
                CameraControlPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(TeleportPlayerPacket.class,
                TeleportPlayerPacket::encode, TeleportPlayerPacket::decode,
                TeleportPlayerPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(StartGalathSexPacket.class,
                StartGalathSexPacket::encode, StartGalathSexPacket::decode,
                StartGalathSexPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SetPlayerForNpcPacket.class,
                SetPlayerForNpcPacket::encode, SetPlayerForNpcPacket::decode,
                SetPlayerForNpcPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(TransferOwnershipPacket.class,
                TransferOwnershipPacket::encode, TransferOwnershipPacket::decode,
                TransferOwnershipPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(ResetNpcPacket.class,
                ResetNpcPacket::encode, ResetNpcPacket::decode,
                ResetNpcPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(ChangeDataParameterPacket.class,
                ChangeDataParameterPacket::encode, ChangeDataParameterPacket::decode,
                ChangeDataParameterPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(OpenNpcInventoryPacket.class,
                OpenNpcInventoryPacket::encode, OpenNpcInventoryPacket::decode,
                OpenNpcInventoryPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SendCompanionHomePacket.class,
                SendCompanionHomePacket::encode, SendCompanionHomePacket::decode,
                SendCompanionHomePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SetNpcHomePacket.class,
                SetNpcHomePacket::encode, SetNpcHomePacket::decode,
                SetNpcHomePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SyncInventoryPacket.class,
                SyncInventoryPacket::encode, SyncInventoryPacket::decode,
                SyncInventoryPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(RemoveItemsPacket.class,
                RemoveItemsPacket::encode, RemoveItemsPacket::decode,
                RemoveItemsPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SummonAlliePacket.class,
                SummonAlliePacket::encode, SummonAlliePacket::decode,
                SummonAlliePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(DespawnClothingPacket.class,
                DespawnClothingPacket::encode, DespawnClothingPacket::decode,
                DespawnClothingPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(MakeRichWishPacket.class,
                MakeRichWishPacket::encode, MakeRichWishPacket::decode,
                MakeRichWishPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(UpdatePlayerModelPacket.class,
                UpdatePlayerModelPacket::encode, UpdatePlayerModelPacket::decode,
                UpdatePlayerModelPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(AcceptSexPacket.class,
                AcceptSexPacket::encode, AcceptSexPacket::decode,
                AcceptSexPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(BeeOpenChestPacket.class,
                BeeOpenChestPacket::encode, BeeOpenChestPacket::decode,
                BeeOpenChestPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(CatEatingDonePacket.class,
                CatEatingDonePacket::encode, CatEatingDonePacket::decode,
                CatEatingDonePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(CatThrowAwayItemPacket.class,
                CatThrowAwayItemPacket::encode, CatThrowAwayItemPacket::decode,
                CatThrowAwayItemPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(ClaimTribePacket.class,
                ClaimTribePacket::encode, ClaimTribePacket::decode,
                ClaimTribePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SetTribeFollowModePacket.class,
                SetTribeFollowModePacket::encode, SetTribeFollowModePacket::decode,
                SetTribeFollowModePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(FallTreePacket.class,
                FallTreePacket::encode, FallTreePacket::decode,
                FallTreePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(CancelTaskPacket.class,
                CancelTaskPacket::encode, CancelTaskPacket::decode,
                CancelTaskPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(SendEggPacket.class,
                SendEggPacket::encode, SendEggPacket::decode,
                SendEggPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(CustomizeNpcPacket.class,
                CustomizeNpcPacket::encode, CustomizeNpcPacket::decode,
                CustomizeNpcPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(GalathRapePouncePacket.class,
                GalathRapePouncePacket::encode, GalathRapePouncePacket::decode,
                GalathRapePouncePacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(UpdateGalathVelocityPacket.class,
                UpdateGalathVelocityPacket::encode, UpdateGalathVelocityPacket::decode,
                UpdateGalathVelocityPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(GalathBackOffPacket.class,
                GalathBackOffPacket::encode, GalathBackOffPacket::decode,
                GalathBackOffPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        reg(RequestRidingPacket.class,
                RequestRidingPacket::encode, RequestRidingPacket::decode,
                RequestRidingPacket::handle, NetworkDirection.PLAY_TO_SERVER);

        // -- Client-bound (PLAY_TO_CLIENT) -------------------------------------
        reg(ModelListPacket.class,
                ModelListPacket::encode, ModelListPacket::decode,
                ModelListPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(ForcePlayerGirlUpdatePacket.class,
                ForcePlayerGirlUpdatePacket::encode, ForcePlayerGirlUpdatePacket::decode,
                ForcePlayerGirlUpdatePacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(OwnershipSyncPacket.class,
                OwnershipSyncPacket::encode, OwnershipSyncPacket::decode,
                OwnershipSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(SpawnEnergyBallParticlesPacket.class,
                SpawnEnergyBallParticlesPacket::encode, SpawnEnergyBallParticlesPacket::decode,
                SpawnEnergyBallParticlesPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(SetPlayerCamPacket.class,
                SetPlayerCamPacket::encode, SetPlayerCamPacket::decode,
                SetPlayerCamPacket::handle, NetworkDirection.PLAY_TO_CLIENT);

        reg(UpdateEquipmentPacket.class,
                UpdateEquipmentPacket::encode, UpdateEquipmentPacket::decode,
                UpdateEquipmentPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
    }

    // -- Helper ----------------------------------------------------------------

    @FunctionalInterface interface Encoder<T>  { void encode(T msg, net.minecraft.network.FriendlyByteBuf buf); }
    @FunctionalInterface interface Decoder<T>  { T decode(net.minecraft.network.FriendlyByteBuf buf); }
    @FunctionalInterface interface Handler<T>  { void handle(T msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx); }

    private static <T> void reg(Class<T> cls, Encoder<T> enc, Decoder<T> dec,
                                Handler<T> handler, NetworkDirection dir) {
        CHANNEL.registerMessage(nextId(), cls, enc::encode, dec::decode,
                handler::handle, Optional.of(dir));
    }
}
