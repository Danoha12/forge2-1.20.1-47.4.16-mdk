package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.DespawnClothingPacket;
import com.trolmastercard.sexmod.network.packet.MakeRichWishPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Allie NPC entity - a magical wish-granting companion.
 * Ported from ev.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
public class AllieEntity extends BaseNpcEntity {

    // -- Constants -------------------------------------------------------------
    public static final int SUMMON_PARTICLE_COUNT = 300;
    public static final int TAIL_BONE_COUNT = 8;
    public static final Vec3 HITBOX = new Vec3(0.5D, 1.0D, 0.0D);

    // -- DataParameters --------------------------------------------------------
    public static final EntityDataAccessor<ItemStack> LAMP_SLOT =
            SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.ITEM_STACK);

    // -- Fields ----------------------------------------------------------------
    private float disappearTimer = 1.0F;
    public boolean pendingSummon = false;
    private int cowgirlSlowVariant = 1;
    private int cowgirlFastVariant = 1;
    private boolean moanToggle = false;
    private boolean animVariantLocked = false;
    private boolean isSandVariant = false;
    private String animationFollowUp = "";
    public float scaleProgress = 0.0f;
    public void setAnimationFollowUp(String anim) {
        this.animationFollowUp = anim;
    }

    public String getAnimFollowUp() {
        return this.animationFollowUp;
    }
    // -- GeckoLib4 cache -------------------------------------------------------
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // -- Constructor ----------------------------------------------------------
    public AllieEntity(EntityType<? extends AllieEntity> type, Level level) {
        super(type, level);
        // setBoundingBox(getBoundingBoxForPose(getPose())); // Descomentar si existe en BaseNpcEntity
    }

    public AllieEntity(EntityType<? extends AllieEntity> type, Level level, ItemStack lamp) {
        this(type, level);
        entityData.set(LAMP_SLOT, lamp);
    }

    // -- Data -----------------------------------------------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LAMP_SLOT, ItemStack.EMPTY);
    }


    public String getNpcName() { return "Allie"; }


    public float getEyeHeightOffset() { return 1.0F; }

    // -- Wish helpers ---------------------------------------------------------
    public boolean isFirstSummon() {
        CompoundTag tag = entityData.get(LAMP_SLOT).getTag();
        if (tag == null) return true;
        return tag.getInt("sexmodUses") == 1;
    }

    // -- Tick -----------------------------------------------------------------
    @Override
    public void aiStep() {
        super.aiStep();

        if (disappearTimer != 1.0F && disappearTimer != -69.0F) {
            if (disappearTimer <= 0.0F) {
                if (isOwnerLocal()) {
                    ModNetwork.CHANNEL.sendToServer(new DespawnClothingPacket(this.getUUID()));
                    ClientStateManager.setAllieActive(true);
                }
                disappearTimer = -69.0F;
            }
        }

        if (!level().isClientSide()) return;

        if (pendingSummon) {
            triggerClientSummon();
        }

        spawnTailParticles();
    }

    // -- Client particle effects -----------------------------------------------
    @OnlyIn(Dist.CLIENT)
    private void spawnTailParticles() {
        if (tickCount % 10 != 0) return;
        int bone = getRandom().nextInt(TAIL_BONE_COUNT);
        Vec3 bonePos = this.getBonePosition("tail" + bone);
        level().addParticle(ParticleTypes.PORTAL,
                bonePos.x, bonePos.y, bonePos.z,
                getRandom().nextGaussian() * 0.01D,
                getRandom().nextGaussian() * 0.01D,
                getRandom().nextGaussian() * 0.01D);
    }

    @OnlyIn(Dist.CLIENT)
    private void triggerClientSummon() {
        this.openMenu((Player) Minecraft.getInstance().player); // <-- Cambiado a openMenu
        pendingSummon = false;
    }

    @OnlyIn(Dist.CLIENT)
    public void requestSummonParticles() {
        if (!isSandVariant) pendingSummon = true;
    }

    // -- Interaction ----------------------------------------------------------
    public boolean openMenu(Player player) {
        isSandVariant = false;
        String[] actions = {
                "action.names.makemerichallie",
                "action.names.deepthroat",
                "Reverse cowgirl"
        };
        openActionMenu(player, this, actions, false);
        return true;
    }

    // -- Action dispatch -------------------------------------------------------
    @Override
    public void triggerAction(String actionKey, UUID playerUUID) {
        isSandVariant = true;

        if ("action.names.makemerichallie".equals(actionKey)) {
            setAnimState(isFirstSummon() ? AnimState.RICH_FIRST_TIME : AnimState.RICH_NORMAL);
            return;
        }

        setAnimationFollowUp(isFirstSummon() ? "deepthroat" : "reverse_cowgirl");
        setAnimState(isFirstSummon()
                ? AnimState.ALLIE_PREPARE_FIRST_TIME
                : AnimState.ALLIE_PREPARE_NORMAL);
    }

    // -- Animation state guards ------------------------------------------------
    @Override
    public void setAnimState(AnimState newState) {
        AnimState cur = getAnimState();

        if (cur == AnimState.DEEPTHROAT_CUM
                && (newState == AnimState.DEEPTHROAT_FAST || newState == AnimState.DEEPTHROAT_SLOW))
            return;

        if (cur == AnimState.REVERSE_COWGIRL_CUM
                && (newState == AnimState.REVERSE_COWGIRL_SLOW
                || newState == AnimState.REVERSE_COWGIRL_FAST_START
                || newState == AnimState.REVERSE_COWGIRL_FAST_CONTINUES))
            return;

        if (!level().isClientSide() && newState == AnimState.REVERSE_COWGIRL_START) {
            teleportOwnerToSitPos();
        }

        super.setAnimState(newState);
    }

    private void teleportOwnerToSitPos() {
        Player owner = getOwnerPlayer();
        if (owner == null) return;
        Vec3 sit = getSitPosition();
        owner.teleportTo(sit.x, sit.y, sit.z);
    }

    // -- AnimState mappings ----------------------------------------

    protected AnimState getCumState(AnimState current) {
        return switch (current) {
            case DEEPTHROAT_FAST, DEEPTHROAT_SLOW    -> AnimState.DEEPTHROAT_CUM;
            case REVERSE_COWGIRL_SLOW,
                 REVERSE_COWGIRL_FAST_START,
                 REVERSE_COWGIRL_FAST_CONTINUES      -> AnimState.REVERSE_COWGIRL_CUM;
            default -> null;
        };
    }


    protected AnimState getFastVariant(AnimState current) {
        return switch (current) {
            case DEEPTHROAT_SLOW      -> AnimState.DEEPTHROAT_FAST;
            case REVERSE_COWGIRL_SLOW -> AnimState.REVERSE_COWGIRL_FAST_START;
            default -> null;
        };
    }

    // -- GeckoLib4 -------------------------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "eyes", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL && anim.autoBlink) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
        }));

        registrar.add(new AnimationController<>(this, "movement", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail"))
        ));

        var actionCtrl = new AnimationController<>(this, "action", 0, this::handleActionState);
        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    private PlayState handleActionState(AnimationState<AllieEntity> state) {
        AnimState anim = getAnimState();
        RawAnimation raw = switch (anim) {
            case SUMMON              -> RawAnimation.begin().thenPlay("animation.allie.summon");
            case SUMMON_NORMAL       -> RawAnimation.begin().thenPlay("animation.allie.summon_normal");
            case SUMMON_NORMAL_WAIT        -> RawAnimation.begin().thenLoop("animation.allie.summon_normal_wait");
            case SUMMON_WAIT               -> RawAnimation.begin().thenLoop("animation.allie.summon_wait");
            case ALLIE_PREPARE_FIRST_TIME  -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_prepare");
            case ALLIE_PREPARE_NORMAL      -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_normal_prepare");
            case DEEPTHROAT_START          -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_start");
            case DEEPTHROAT_SLOW           -> RawAnimation.begin().thenLoop("animation.allie.deepthroat_slow");
            case DEEPTHROAT_FAST           -> RawAnimation.begin().thenLoop("animation.allie.deepthroat_fast");
            case DEEPTHROAT_CUM            -> RawAnimation.begin().thenPlay("animation.allie.deepthroat_cum");
            case RICH_FIRST_TIME           -> RawAnimation.begin().thenPlay("animation.allie.rich");
            case RICH_NORMAL               -> RawAnimation.begin().thenPlay("animation.allie.rich_normal");
            case SUMMON_SAND         -> RawAnimation.begin().thenPlay("animation.allie.summon_sand");
            case REVERSE_COWGIRL_START     -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_start");
            case REVERSE_COWGIRL_SLOW      -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_slow" + cowgirlSlowVariant);
            case REVERSE_COWGIRL_FAST_CONTINUES -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fastc" + cowgirlFastVariant);
            case REVERSE_COWGIRL_FAST_START -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fasts");
            case REVERSE_COWGIRL_CUM       -> RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_cum");
            default -> RawAnimation.begin().thenLoop("animation.allie.null");
        };
        return state.setAndContinue(raw);
    }

    // -- Sound keyframe dispatcher ---------------------------------------------
    @OnlyIn(Dist.CLIENT)
    private void handleSoundKeyframe(software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent<AllieEntity> event) {
        String sound = event.getKeyframeData().getSound();
        switch (sound) {
            case "summonMSG1" -> { displayDialogue("allie.dialogue.summon1"); playSound(ModSounds.GIRLS_ALLIE_SCAWY[0].get(), 0.5F); }
            case "summonMSG2" -> { displayDialogue("allie.dialogue.summon2"); playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE); }
            case "summonMSG3" ->   displayDialogue("allie.dialogue.summon3");
            case "summonMSG4" -> { displayDialogue("allie.dialogue.summon4"); playSound(ModSounds.GIRLS_ALLIE_LIGHTBREATHING[2].get(), 1.0F); }
            case "summonMSG5" -> { displayDialogue("allie.dialogue.summon5"); playSound(ModSounds.GIRLS_ALLIE_HMPH[4].get(), 1.0F); }
            case "summonMSG6" -> { displayDialogue("allie.dialogue.summon6"); playSound(ModSounds.GIRLS_ALLIE_GIGGLE[3].get(), 1.0F); }
            case "summonMSG7" ->   displayDialogue("allie.dialogue.summon7");
            case "summonMSG8" -> {
                displayDialogue("allie.dialogue.summon8");
                playRandSound(ModSounds.GIRLS_ALLIE_HUH);
                if (isOwnerLocal()) openMenu(Minecraft.getInstance().player);
            }
            case "summonDone" -> setAnimState(AnimState.SUMMON_WAIT);
            case "deepthroat_prepareMSG1" -> { displayDialogue("allie.dialogue.hihi"); playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE); }
            case "deepthroat_prepareMSG2" -> { displayDialogue("allie.dialogue.boys"); playSound(ModSounds.GIRLS_ALLIE_SIGH[0].get(), 1.0F); }
            case "scream"     -> playRandSound(ModSounds.MISC_SCREAM);
            case "blackscreen" -> { if (isOwnerLocal()) ClientStateManager.triggerBlackScreen(); }
            case "deepthroat_prepareDone" -> {
                if (!isOwnerLocal()) break;
                if ("reverse_cowgirl".equals(getAnimFollowUp())) {
                    setPitch(30.0F);
                    setAnimState(AnimState.REVERSE_COWGIRL_START);
                } else {
                    setAnimState(AnimState.DEEPTHROAT_START);
                            ModNetwork.CHANNEL.sendToServer(new com.trolmastercard.sexmod.network.packet.GalathBackOffPacket());
                    setRotation(getYRot() + 180.0F, getXRot());
                    setOffsetPosition(0.0D, 0.0D, 1.35D, 0.0F, 30.0F);

                    // 🚨 RECONECTADO: Inicia el minijuego de la barra lateral
                    com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.onSexStart();
                }
            }
            case "deepthroat_fastDone"  -> { if (isOwnerLocal() && !ClientStateManager.isLeader()) setAnimState(AnimState.DEEPTHROAT_SLOW); }
            case "deepthroat_startDone" ->   setAnimState(AnimState.DEEPTHROAT_SLOW);
            case "deepthroat_fastMSG1"  -> {
                playRandSound(ModSounds.GIRLS_ALLIE_BJMOAN);
                if (isOwnerLocal()) {
                    com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.setVisible(true);
                    com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.addValue(0.04D);
                }
            }
            case "deepthroat_slowMSG1"  -> {
                if (getRandom().nextFloat() > 0.33F) playRandSound(ModSounds.GIRLS_ALLIE_LIPSOUND);
                else playRandSound(ModSounds.GIRLS_ALLIE_BJMOAN);
                if (isOwnerLocal()) {
                    com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.setVisible(true);
                    com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.addValue(0.02D);
                }
            }
            case "deepthroat_cumMSG1" -> { playRandSound(ModSounds.GIRLS_ALLIE_MOAN); playRandSound(ModSounds.GIRLS_ALLIE_LIPSOUND); playRandSound(ModSounds.MISC_CUMINFLATION, 1.5F); }
            case "cowgirl_cumDone", "deepthroat_cumDone" -> {
                if (isOwnerLocal()) { onSessionEnd(); ModNetwork.CHANNEL.sendToServer(new DespawnClothingPacket(this.getUUID())); }
            }
            case "summon_normalMSG1" -> { displayDialogue("allie.dialogue.sup"); playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE); }
            case "summon_normalMSG2" ->   displayDialogue("allie.dialogue.youhave");
            case "summon_normalMSG3" -> {
                int uses = entityData.get(LAMP_SLOT).getOrCreateTag().getInt("sexmodUses");
                displayDialogue(uses == 2 ? "allie.dialogue.2wishes" : "allie.dialogue.1wish");
                playSound(ModSounds.GIRLS_ALLIE_HMPH[4].get(), 1.0F);
            }
            case "summon_normalMSG4" ->   displayDialogue("So...");
            case "summon_normalMSG5" -> { displayDialogue("allie.dialogue.tellme"); playRandSound(ModSounds.GIRLS_ALLIE_HUH); }
            case "summon_normalDone" -> {
                setAnimState(AnimState.SUMMON_NORMAL_WAIT);
                if (isOwnerLocal()) openMenu(Minecraft.getInstance().player);
            }
            case "deepthroat_normal_prepareMSG1" -> { displayDialogue("allie.dialogue.alright"); playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE); }
            case "rich_MSG1" -> {
                displayDialogue("allie.dialogue.wishgranted");
                playRandSound(ModSounds.MISC_PLOB);
                if (isOwnerLocal()) ModNetwork.CHANNEL.sendToServer(new MakeRichWishPacket(position()));
            }
            case "disappear"      ->   disappearTimer = 0.99F;
            case "summon_sandMSG1" -> { displayDialogue("allie.dialogue.nooo"); playSound(ModSounds.GIRLS_ALLIE_SCAWY[2].get(), 1.0F); }
            case "summon_sandMSG2" -> { if (isSandVariant()) showSandMessage(); }
            case "giggle"         ->   playRandSound(ModSounds.GIRLS_ALLIE_GIGGLE);
            case "pounding"       ->   playRandSound(ModSounds.MISC_POUNDING);
            case "moan"           ->   playRandSound(ModSounds.GIRLS_ALLIE_MOAN);
            case "mmm"            ->   playRandSound(ModSounds.GIRLS_ALLIE_MMM);
            case "slide"          ->   playRandSound(ModSounds.MISC_SLIDE);
            case "slowMoan"       -> {
                if (getRandom().nextBoolean()) playRandSound(ModSounds.GIRLS_ALLIE_AHH);
                if (isOwnerLocal()) com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.addValue(0.02D);
            }
            case "cowgirlSlowDone" -> {
                int old = cowgirlSlowVariant;
                do { cowgirlSlowVariant = getRandom().nextInt(3) + 1; } while (cowgirlSlowVariant == old);
            }
            case "fastMoan" -> {
                if (isOwnerLocal()) com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.addValue(0.04D);
                if (!moanToggle) { playRandSound(ModSounds.GIRLS_ALLIE_MOAN); moanToggle = true; }
                else moanToggle = false;
            }
            case "fastSwitch" -> {
                if (!isOwnerLocal() || !ClientStateManager.isLeader()) break;
                AnimState cur = getAnimState();
                if (cur == AnimState.REVERSE_COWGIRL_FAST_START) {
                    setAnimState(AnimState.REVERSE_COWGIRL_FAST_CONTINUES);
                } else {
                    onFastRoundComplete();
                    int old = cowgirlFastVariant;
                    do { cowgirlFastVariant = getRandom().nextInt(3) + 1; } while (cowgirlFastVariant == old);
                }
            }
            case "openSexUi" -> { if (isOwnerLocal()) com.trolmastercard.sexmod.client.gui.HornyMeterOverlay.setVisible(true); }
            case "cum"       ->   playRandSound(ModSounds.MISC_INSERTS, 6.0F);
            case "aftermoan" ->   playRandSound(ModSounds.GIRLS_ALLIE_AFTERSESSIONMOAN);
        }
    }

    // -- Sand check -----------------------------------------------------------
    public boolean isSandVariant() { return isSandVariant; }

    @OnlyIn(Dist.CLIENT)
    private void showSandMessage() {
        if (!level().isClientSide()) return;
        displayNpcSubtitleMessage("allie.dialogue.phobia", true);
    }
    @OnlyIn(Dist.CLIENT)
    protected void displayDialogue(String textKey) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            // Convierte el texto/llave en un componente traducible
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(textKey);

            // El 'true' hace que el texto aparezca como subtítulo arriba de la barra de vida
            // (Si le pones 'false', aparecería en el chat normal de la izquierda)
            localPlayer.displayClientMessage(message, true);
        }
    }
    // -- Adaptador de Sonido (Cubre el Pitch faltante) ------------------------

    protected void playSound(net.minecraft.sounds.SoundEvent sound, float volume) {
        // Le inyecta automáticamente el 1.0F de Pitch (tono normal)
        this.playSound(sound, volume, 1.0F);
    }
    // -- Utilidades de Sonido -------------------------------------------------

    protected void playRandSound(net.minecraftforge.registries.RegistryObject<net.minecraft.sounds.SoundEvent>[] soundArray) {
        // Llama a la versión de abajo con volumen por defecto de 1.0F
        this.playRandSound(soundArray, 1.0F);
    }

    protected void playRandSound(net.minecraftforge.registries.RegistryObject<net.minecraft.sounds.SoundEvent>[] soundArray, float volume) {
        // Verifica que la lista exista y tenga al menos un sonido
        if (soundArray != null && soundArray.length > 0) {
            // Escoge uno al azar, le saca el .get() y lo reproduce
            this.playSound(soundArray[this.random.nextInt(soundArray.length)].get(), volume, 1.0F);
        }
    }
    // -- Bone Positions --------------------------------------------------------

    @Override
    public Vec3 getBonePosition(String boneName) {
        // Por defecto, devolvemos el centro de Allie un bloque hacia arriba
        return this.position().add(0, 1, 0);
    }

    // -- GeckoLib4 boilerplate -------------------------------------------------
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
// -- Utilidades de Dueño (Owner) ------------------------------------------

    public boolean isOwnerLocal() {
        // Si el código está corriendo en el servidor, no hay "jugador local"
        if (!this.level().isClientSide()) return false;
        return isLocalPlayerOwner();
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isLocalPlayerOwner() {
        Player localPlayer = Minecraft.getInstance().player;
        // Comparamos si el UUID del jugador en tu pantalla es el mismo que el dueño de Allie
        return localPlayer != null && localPlayer.getUUID().equals(this.getOwnerUUID());
    }
    @OnlyIn(Dist.CLIENT)
    public void openActionMenu(Player player, BaseNpcEntity npc, String[] actions, boolean cancelable) {
        // En la 1.20.1, abrimos la NpcActionScreen que ya está programada
        // (null en el array de costos porque Allie no te cobra por los deseos... al principio).
        Minecraft.getInstance().setScreen(new com.trolmastercard.sexmod.client.gui.NpcActionScreen(npc, player, actions, null, false));
    }
// -- Utilidades de Posición (Asientos) ------------------------------------

    public Vec3 getSitPosition() {
        // Por defecto, teletransporta al jugador exactamente a la misma posición donde está Allie.
        // (Si ves que en el juego el jugador queda muy arriba o muy abajo,
        // puedes cambiarlo a algo como: return this.position().add(0, 0.5, 0); )
        return this.position();
    }
// -- Adaptador de Rotación (Cuello / Pitch) -------------------------------

    public void setPitch(float pitch) {
        // En 1.20.1, el Pitch (mirar arriba/abajo) se llama XRot
        this.setXRot(pitch);
    }
// -- Adaptador de Dueño (Extrae el UUID del jugador) ----------------------

    public UUID getOwnerUUID() {
        // Usamos el método que ya sabemos que existe en tu BaseNpcEntity
        Player owner = this.getOwnerPlayer();

        // Si el dueño está presente, le sacamos su ID. Si no, devolvemos null.
        return owner != null ? owner.getUUID() : null;
    }
// -- Adaptador de Rotación Completa (Yaw y Pitch) -------------------------

    public void setRotation(float yaw, float pitch) {
        // En 1.20.1, asignamos las rotaciones por separado
        this.setYRot(yaw);
        this.setXRot(pitch);
    }
// -- Adaptador de Posición Relativa (Offset) ------------------------------

    public void setOffsetPosition(double x, double y, double z, float yawOffset, float pitchOffset) {
        // 1. Calculamos hacia dónde está mirando Allie (en radianes)
        float f = this.getYRot() * ((float) Math.PI / 180F);

        // 2. Aplicamos la trigonometría de Minecraft para que la mueva de forma relativa
        // (Z es hacia adelante/atrás, X es hacia los lados)
        double newX = this.getX() - (double)(net.minecraft.util.Mth.sin(f)) * z + (double)(net.minecraft.util.Mth.cos(f)) * x;
        double newY = this.getY() + y;
        double newZ = this.getZ() + (double)(net.minecraft.util.Mth.cos(f)) * z + (double)(net.minecraft.util.Mth.sin(f)) * x;

        // 3. La teletransportamos a su nuevo lugarcito
        this.setPos(newX, newY, newZ);

        // 4. Le sumamos los grados extra a su cuello y cintura
        this.setYRot(this.getYRot() + yawOffset);
        this.setXRot(this.getXRot() + pitchOffset);
    }
// -- Limpieza de Animación (Session End) ----------------------------------

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void onSessionEnd() {
        // 1. Le devolvemos el control al jugador para que ya se pueda mover
        com.trolmastercard.sexmod.client.handler.ClientStateManager.setCanMove(true);

        // 2. Apagamos el estado de Allie en el cliente
        com.trolmastercard.sexmod.client.handler.ClientStateManager.setAllieActive(false);

        // 3. Reseteamos el medidor de progreso a cero
        com.trolmastercard.sexmod.util.SexAnimationTracker.setProgress(0.0F);

        // 4. Si la cámara estaba bloqueada o en un ángulo raro, aquí podrías resetearla
        // Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
    }
// -- Lógica de Progreso de Animación --------------------------------------

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void onFastRoundComplete() {
        // 1. Obtenemos cuánto progreso llevamos actualmente
        float currentProgress = com.trolmastercard.sexmod.util.SexAnimationTracker.getProgress();

        // 2. Le sumamos un porcentaje (0.20F significa que en 5 vueltas se llena)
        // Puedes cambiar el 0.20F por 0.10F si quieres que la escena dure más.
        float newProgress = Math.min(1.0F, currentProgress + 0.20F);

        // 3. Actualizamos el Tracker para que el "Horny Meter" suba visualmente
        com.trolmastercard.sexmod.util.SexAnimationTracker.setProgress(newProgress);

        // 4. (Opcional) Si quieres que suene un 'slap' o algo cada vez que completa la vuelta:
        // this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.2F);
    }
// -- Adaptador de Subtítulos (Cuerdas vocales v2) -------------------------

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    protected void displayNpcSubtitleMessage(String textKey, boolean isActionBar) {
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer != null) {
            // En la 1.20.1 usamos Component.translatable para que el texto sea traducible
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(textKey);

            // El 'isActionBar' (el true/false que viene en el error) decide la posición
            localPlayer.displayClientMessage(message, isActionBar);
        }
    }
    public boolean isSubVariant() {
        // Por defecto, le decimos al modelo que no es una sub-variante.
        // (Si más adelante le programas una variante en tus SynchedEntityData,
        // aquí puedes cambiarlo para que lea ese dato real).
        return false;
    }
}