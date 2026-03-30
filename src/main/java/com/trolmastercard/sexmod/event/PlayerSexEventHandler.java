package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.SlimeNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerSexEventHandler — Portado a 1.20.1.
 * * Maneja las interacciones físicas del avatar del jugador (cama, daño, interfaces).
 * * Dividido en CommonEvents (Servidor/Cliente) y ClientEvents (Solo Cliente) para evitar crasheos.
 */
public class PlayerSexEventHandler {

    // ── EVENTOS COMUNES (Cliente y Servidor) ─────────────────────────────────

    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommonEvents {

        // ── Prevención de Dormir ──
        @SubscribeEvent
        public static void onPlayerSleep(SleepingLocationCheckEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
            if (kobold == null || !player.isSleeping()) return;
            event.setResult(Event.Result.DENY);
        }

        // ── Interacción con Cama (Click Derecho) ──
        @SubscribeEvent
        public static void onRightClickBedForNpc(PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            Level level = event.getLevel();
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);

            if (kobold == null || !kobold.isOwner()) return;

            BlockPos clickedPos = event.getPos();

            // Asumiendo que isBedBlock se ha actualizado para usar BlockTags.BEDS
            if (!NpcWorldUtil.isBedBlock(level, clickedPos, event.getHitVec(), event.getFace(), player)) return;

            if (kobold.getEntityData().get(BaseNpcEntity.DATA_IMMOVABLE)) {
                event.setCanceled(true);
                return;
            }
            if (!player.isSleeping()) return;

            // Buscar bloques de aire adyacentes
            List<BlockPos> candidates = new ArrayList<>();
            BlockPos[] checks = {clickedPos.above(), clickedPos.north(), clickedPos.south(), clickedPos.east()};

            for (BlockPos bp : checks) {
                if (level.getBlockState(bp).isAir()) candidates.add(bp);
            }

            BlockPos chosen = null;
            Vec3 pPos = player.position();
            for (BlockPos bp : candidates) {
                if (chosen == null || bp.distToCenterSqr(pPos) < chosen.distToCenterSqr(pPos)) {
                    chosen = bp;
                }
            }

            if (chosen == null) {
                player.displayClientMessage(Component.literal("§cLa cama está obstruida."), true);
                return;
            }

            // Orientación
            player.setPos(chosen.getX() + 0.5D, chosen.getY(), chosen.getZ() + 0.5D);
            if (clickedPos.north().equals(chosen)) player.setYRot(90.0F);
            else if (clickedPos.south().equals(chosen)) player.setYRot(180.0F);
            else if (clickedPos.east().equals(chosen)) player.setYRot(-90.0F);
            else player.setYRot(0.0F);

            if (level.isClientSide()) {
                ClientSafeCalls.handleBedInteract(kobold);
                return;
            }

            // Lado del Servidor
            Vec3 bedCenter = new Vec3(chosen.getX() + 0.5D, chosen.getY(), chosen.getZ() + 0.5D);
            kobold.setPos(bedCenter.x, bedCenter.y, bedCenter.z);
            kobold.setYRot(player.getYRot());
            kobold.getEntityData().set(BaseNpcEntity.DATA_IMMOVABLE, true);
            // Asumiendo que startSexAtPosition existe
            kobold.startSexAtPosition();
        }

        // ── Interacción en Suelo (NPC Inventory) ──
        @SubscribeEvent
        public static void onRightClickBlockForInventoryNpc(PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);

            if (!(kobold instanceof NpcInventoryEntity)) return;
            if (!player.isSleeping() || !player.getMainHandItem().isEmpty()) return;
            if (kobold.getEntityData().get(BaseNpcEntity.DATA_IMMOVABLE)) return;
            if (player.getXRot() < 20.0F) return;

            Vec3 hitVec = event.getHitVec();
            if (hitVec.distanceToSqr(player.position()) > 9.0D) return; // 3.0D al cuadrado

            Vec3 snapPos = new Vec3(hitVec.x, Math.floor(hitVec.y), hitVec.z);
            player.setPos(snapPos.x, snapPos.y, snapPos.z);
            kobold.setPos(snapPos.x, snapPos.y, snapPos.z);
            kobold.setYRot(player.getYRot());
            kobold.getEntityData().set(BaseNpcEntity.DATA_IMMOVABLE, true);
            kobold.getEntityData().set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
            kobold.setAnimState(AnimState.STARTDOGGY);

            if (event.getLevel().isClientSide()) {
                ClientSafeCalls.setThirdPerson();
            }
        }

        // ── Sincronización al Reaparecer ──
        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            Player player = event.getEntity();
            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
            if (kobold == null) return;

            Vec3 pos = player.position();
            kobold.setPos(pos.x, pos.y, pos.z);
            // kobold.baseTick(); // Revisa si esto es seguro llamarlo en este evento
        }

        // ── Cancelar Daño por Caída ──
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            // En 1.20.1 se usan Tags para identificar el tipo de daño de forma segura
            if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;

            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
            if (kobold instanceof AllieEntity || kobold instanceof SlimeNpcEntity) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;

            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
            if (kobold instanceof NpcInventoryEntity) {
                event.setAmount(0.0F);
                event.setCanceled(true);
            }
        }
    }

    // ── EVENTOS EXCLUSIVOS DEL CLIENTE ───────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {

        // ── Botón de Ropa en el Inventario ──
        @SubscribeEvent
        public static void onInventoryScreenInit(ScreenEvent.Init.Post event) {
            Screen gui = event.getScreen();
            if (!(gui instanceof InventoryScreen) && !(gui instanceof CreativeModeInventoryScreen)) return;

            LocalPlayer local = Minecraft.getInstance().player;
            if (local == null) return;

            PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(local);
            if (kobold == null || kobold.isCustomizable()) return;

            String labelKey = kobold.getOutfitLevel() == 0 ? "action.names.dressup" : "action.names.strip";

            // En 1.20.1, I18n ha sido reemplazado por Component.translatable
            Button stripBtn = Button.builder(Component.translatable(labelKey), btn -> onStripButtonClick(local, kobold))
                    .pos((int)(gui.width * 0.5D - 35), (int)(gui.height * 0.87D))
                    .size(70, 20)
                    .build();

            event.addListener(stripBtn);
        }

        private static void onStripButtonClick(LocalPlayer local, PlayerKoboldEntity kobold) {
            if (kobold.isCustomizable() || kobold.getCurrentSexPartner() != null || kobold.getAnimState() != AnimState.NULL) return;

            Minecraft mc = Minecraft.getInstance();
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            local.closeContainer(); // Cierra el inventario

            kobold.setAnimState(AnimState.STRIP);
            ClientStateManager.setThirdPerson(false);
        }

        // ── Propuestas entre Jugadores (Click Derecho a otro jugador) ──
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (!(event.getTarget() instanceof Player target)) return;

            LocalPlayer local = Minecraft.getInstance().player;
            if (local == null || !event.getEntity().getUUID().equals(local.getUUID())) return;

            PlayerKoboldEntity selfKobold = PlayerKoboldEntity.getForPlayer(local);
            PlayerKoboldEntity targetKobold = PlayerKoboldEntity.getForPlayer(target);

            // Yo soy NPC y le hago click a un jugador
            if (selfKobold != null && targetKobold == null) {
                target.displayClientMessage(Component.literal("§cNo lesbo yet owo"), true);
                return;
            }

            // Yo soy jugador y le hago click a un NPC
            if (selfKobold == null && targetKobold != null) {
                if (targetKobold.canAcceptSexProposal() && targetKobold.isWaitingForProposal()) {
                    targetKobold.proposeToPlayer(local);
                }
                return;
            }
        }
    }

    // ── Helper de Aislamiento de Cliente ─────────────────────────────────────

    private static class ClientSafeCalls {
        @OnlyIn(Dist.CLIENT)
        public static void handleBedInteract(PlayerKoboldEntity kobold) {
            ClientStateManager.setThirdPerson(false);
            // kobold.clientBedInteract(); // Descomenta si este método existe en tu porte
        }

        @OnlyIn(Dist.CLIENT)
        public static void setThirdPerson() {
            ClientStateManager.setThirdPerson(false);
        }
    }
}