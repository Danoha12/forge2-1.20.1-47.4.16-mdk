package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.client.gui.components.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Forge event handler for player-NPC interaction logic.
 *
 * Covers:
 *  - Blocking sleep when a PlayerKoboldEntity is controlling the player
 *  - Bed right-click to position and start a sex scene
 *  - Player respawn sync for PlayerKoboldEntity
 *  - Inventory screen strip/dress button (client)
 *  - LivingHurt / LivingDamage cancellation during sex states
 *  - Entity interact (player-on-player sex proposal)
 */
public class PlayerSexEventHandler {

    static final int STRIP_BUTTON_ID = 284453;

    // -- Sleep prevention ------------------------------------------------------

    @SubscribeEvent
    public void onPlayerSleep(SleepingLocationCheckEvent event) {
        // Note: 1.20.1 uses SleepingLocationCheckEvent; deny sleep if player
        // is currently controlled by a PlayerKoboldEntity.
        Player player = event.getEntity();
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
        if (kobold == null) return;
        if (!player.isSleeping()) return;
        event.setResult(Event.Result.DENY);
    }

    // -- Bed right-click: start sex scene -------------------------------------

    /**
     * Intercepts right-clicking on a bed block for a PlayerKoboldEntity (NPC)
     * that is controlled by the clicking player. Positions both NPC and player
     * adjacent to the bed, then starts the STARTDOGGY animation.
     */
    @SubscribeEvent
    public void onRightClickBedForNpc(PlayerInteractEvent.RightClickBlock event) {
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getByUUID(
                event.getEntity().getUUID());
        BlockPos clickedPos = event.getPos();
        Level world = event.getEntity().level();
        Player player = event.getEntity();

        if (kobold == null) return;
        if (!kobold.isOwner()) return;   // only the NPC that owns this player
        if (!NpcWorldUtil.isBedBlock(world, clickedPos, event.getHitVec(),
                event.getFace(), player)) return;

        // Block interaction if NPC is already immovable (in sex)
        if (kobold.entityData.get(BaseNpcEntity.DATA_IMMOVABLE)) {
            event.setCanceled(true);
            return;
        }
        if (!player.isSleeping()) return;

        // Collect adjacent air blocks for positioning
        ArrayList<BlockPos> candidates = new ArrayList<>();
        BlockPos up    = clickedPos.above();
        BlockPos north = clickedPos.north();
        BlockPos south = clickedPos.south();
        BlockPos east  = clickedPos.east();

        if (world.getBlockState(up).getBlock()    == Blocks.AIR) candidates.add(up);
        if (world.getBlockState(north).getBlock() == Blocks.AIR) candidates.add(north);
        if (world.getBlockState(south).getBlock() == Blocks.AIR) candidates.add(south);
        if (world.getBlockState(east).getBlock()  == Blocks.AIR) candidates.add(east);

        // Pick closest candidate to player
        BlockPos chosen = null;
        for (BlockPos bp : candidates) {
            if (chosen == null) { chosen = bp; continue; }
            Vec3 pPos = player.position();
            if (dist3(bp, pPos) < dist3(chosen, pPos)) chosen = bp;
        }

        if (chosen == null) {
            player.sendSystemMessage(Component.literal("Bed is obscured"));
            return;
        }

        // Orient player
        player.setPos(chosen.getX() + 0.5D, chosen.getY(), chosen.getZ() + 0.5D);
        if (clickedPos.above().equals(chosen))  player.setYRot(0.0F);
        if (clickedPos.north().equals(chosen))  player.setYRot(90.0F);
        if (clickedPos.south().equals(chosen))  player.setYRot(180.0F);
        if (clickedPos.east().equals(chosen))   player.setYRot(-90.0F);

        if (world.isClientSide) {
            ClientStateManager.setThirdPerson(false);
            kobold.clientBedInteract();
            return;
        }

        // Server side: lock kobold to bed position and start sex
        Vec3 bedCenter = new Vec3(chosen.getX() + 0.5D, chosen.getY(), chosen.getZ() + 0.5D);
        kobold.setPos(bedCenter);
        kobold.setYRot(player.getYRot());
        kobold.entityData.set(BaseNpcEntity.DATA_IMMOVABLE, true);
        kobold.startSexAtPosition();
    }

    private double dist3(BlockPos bp, Vec3 v) {
        double dx = bp.getX() - v.x;
        double dy = bp.getY() - v.y;
        double dz = bp.getZ() - v.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    // -- Bed right-click: NpcInventoryEntity (ec) self-position ---------------

    /**
     * Handles a right-click on a floor block by a NpcInventoryEntity-controlled
     * player. Snaps the NPC to the floor surface and starts STARTDOGGY.
     */
    @SubscribeEvent
    public void onRightClickBlockForInventoryNpc(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
        if (kobold == null) return;
        if (!(kobold instanceof NpcInventoryEntity)) return;
        if (!player.isSleeping()) return;
        if (!player.getMainHandItem().equals(ItemStack.EMPTY)) return;
        if (kobold.entityData.get(BaseNpcEntity.DATA_IMMOVABLE)) return;
        if (player.getXRot() < 20.0F) return;

        Vec3 hitVec = event.getHitVec();
        if (hitVec == null) return;
        if (hitVec.distanceTo(player.position()) > 3.0D) return;

        Vec3 snapPos = new Vec3(hitVec.x, Math.floor(hitVec.y), hitVec.z);
        player.setPos(snapPos.x, Math.floor(hitVec.y), snapPos.z);
        kobold.setPos(snapPos);
        kobold.setYRot(player.getYRot());
        kobold.entityData.set(BaseNpcEntity.DATA_IMMOVABLE, true);
        kobold.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
        kobold.setAnimState(AnimState.STARTDOGGY);

        if (event.getLevel().isClientSide) {
            if (Minecraft.getInstance().player != null &&
                    Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                ClientStateManager.setThirdPerson(false);
            }
        }
    }

    // -- Player respawn sync ---------------------------------------------------

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getByUUID(player.getUUID());
        if (kobold == null) return;
        Vec3 pos = player.position();
        kobold.level(player.level());
        kobold.setPos(pos.x, pos.y, pos.z);
        kobold.baseTick();
        System.out.println(player.level().isLoaded(kobold.blockPosition()));
    }

    // -- Player entity interact (sex proposal between players) -----------------

    /** Triggered when a controlled NPC player (ei) right-clicks a non-NPC player target. */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onEntityInteractAsNpc(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (!event.getEntity().getUUID().equals(
                Minecraft.getInstance().player.getUUID())) return;

        LocalPlayer local = Minecraft.getInstance().player;
        PlayerKoboldEntity selfKobold  = PlayerKoboldEntity.getByUUID(local.getUUID());
        Player target     = (Player) event.getTarget();
        PlayerKoboldEntity targetKobold = PlayerKoboldEntity.getForPlayer(target);

        if (targetKobold == null) return;
        if (selfKobold != null) {
            local.displayClientMessage(Component.literal("no lesbo yet owo"), true);
            return;
        }
        if (!targetKobold.canAcceptSexProposal()) return;
        if (targetKobold.isWaitingForProposal()) {
            targetKobold.proposeToPlayer(local);
        }
    }

    /** Triggered when a normal player right-clicks a player controlled by a NPC (ei). */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onEntityInteractOnNpcPlayer(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (!event.getEntity().getUUID().equals(
                Minecraft.getInstance().player.getUUID())) return;

        LocalPlayer local = Minecraft.getInstance().player;
        PlayerKoboldEntity selfKobold  = PlayerKoboldEntity.getByUUID(local.getUUID());
        if (selfKobold == null) return;

        Player target       = (Player) event.getTarget();
        PlayerKoboldEntity targetKobold = PlayerKoboldEntity.getByUUID(target.getUUID());
        if (targetKobold != null) {
            target.displayClientMessage(Component.literal("no lesbo yet owo"), true);
            return;
        }
        if (selfKobold.isWaitingForProposal()) {
            selfKobold.waitingForPlayerResponse = false;
            selfKobold.proposeToPlayer(target);
        }
    }

    // -- LivingHurt: cancel fall damage during sex -----------------------------

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!event.getSource().equals(event.getEntity().level().damageSources().fall())) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(player);
        if (kobold == null) return;
        if (kobold instanceof AllieEntity || kobold instanceof SlimeNpcEntity) {
            event.setCanceled(true);
        }
    }

    // -- Inventory screen: add strip/dress button ------------------------------

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onInventoryScreenInit(ScreenEvent.Init.Post event) {
        Screen gui = event.getScreen();
        if (!(gui instanceof InventoryScreen) && !(gui instanceof CreativeModeInventoryScreen)) return;

        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null) return;
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(local);
        if (kobold == null) return;
        if (kobold.isCustomizable()) return;

        String label = net.minecraft.client.resources.language.I18n.get(
                kobold.getOutfitLevel() == 0 ? "action.names.dressup" : "action.names.strip");

        Button stripBtn = Button.builder(Component.literal(label), btn -> onStripButtonClick(local, kobold))
                .pos((int)(gui.width * 0.5D - 35), (int)(gui.height * 0.87D))
                .size(70, 20)
                .build();

        event.addListener(stripBtn);
    }

    @OnlyIn(Dist.CLIENT)
    private void onStripButtonClick(LocalPlayer local, PlayerKoboldEntity kobold) {
        if (kobold.isCustomizable()) return;
        if (kobold.getCurrentSexPartner() != null) return;
        if (kobold.getAnimState() != AnimState.NULL) return;

        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        mc.setScreen(null);
        kobold.setAnimState(AnimState.STRIP);
        ClientStateManager.setThirdPerson(false);
        local.closeContainer();
    }

    // -- LivingDamage: cancel void damage for floor-positioned NPCs -------------

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!event.getSource().equals(event.getEntity().level().damageSources().fall())) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerKoboldEntity kobold = PlayerKoboldEntity.getByUUID(player.getUUID());
        if (kobold == null) return;
        if (kobold instanceof NpcInventoryEntity) {
            event.setResult(Event.Result.DENY);
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }
}
