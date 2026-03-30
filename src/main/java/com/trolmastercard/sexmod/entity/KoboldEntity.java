package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.util.KoboldNames;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * KoboldEntity — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Entidad principal de la tribu. Maneja:
 * - IA de trabajo y defensa coordinada.
 * - Inventario de 27 slots.
 * - Escala dinámica (Body Size) que afecta la vida y el tono de voz (Pitch).
 */
public class KoboldEntity extends BaseNpcEntity implements GeoEntity {

  // ── Datos Sincronizados ──────────────────────────────────────────────────

  public static final EntityDataAccessor<Float> BODY_SIZE =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.FLOAT);

  public static final EntityDataAccessor<String> KOBOLD_NAME =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.STRING);

  public static final EntityDataAccessor<Optional<UUID>> TRIBE_ID =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  public static final EntityDataAccessor<Boolean> IS_MINING =
          SynchedEntityData.defineId(KoboldEntity.class, EntityDataSerializers.BOOLEAN);

  // ── Propiedades de Instancia ─────────────────────────────────────────────

  public final ItemStackHandler inventory = new ItemStackHandler(27);
  private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

  private int attackTick = 0;
  private int healTick = 0;
  private int cumCounter = -1;

  public KoboldEntity(EntityType<? extends KoboldEntity> type, Level level) {
    super(type, level);
  }

  // ── Atributos y Fábrica ──────────────────────────────────────────────────

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.ATTACK_DAMAGE, 3.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
  }

  public static KoboldEntity createForTribe(Level level, UUID tribeId, float size) {
    KoboldEntity kobold = new KoboldEntity(ModEntities.KOBOLD.get(), level);
    kobold.getEntityData().set(TRIBE_ID, Optional.of(tribeId));
    kobold.getEntityData().set(BODY_SIZE, size);
    // Ajustar vida máxima según tamaño: los pequeños son más resistentes (lógica original)
    double health = 20.0D + (0.25D - size) * 40.0D;
    kobold.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
    kobold.setHealth((float) health);
    return kobold;
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(BODY_SIZE, 0.15F);
    this.entityData.define(KOBOLD_NAME, KoboldNames.randomName());
    this.entityData.define(TRIBE_ID, Optional.empty());
    this.entityData.define(IS_MINING, false);
  }

  // ── Interacción ──────────────────────────────────────────────────────────

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    // Renombrar con NameTag
    if (stack.getItem() instanceof NameTagItem && isMaster(player)) {
      this.entityData.set(KOBOLD_NAME, stack.getHoverName().getString());
      if (!player.getAbilities().instabuild) stack.shrink(1);
      return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // Si ya está en una interacción especial, ignorar clics
    if (isInteractiveMode()) return InteractionResult.PASS;

    if (this.level().isClientSide()) {
      openKoboldActionMenu(player);
    } else {
      this.setPartnerUUID(player.getUUID());
      this.getNavigation().stop();
    }

    return InteractionResult.sidedSuccess(this.level().isClientSide());
  }

  private void openKoboldActionMenu(Player player) {
    String[] actions = { "action.names.special_A", "action.names.special_B", "action.names.special_C" };
    openActionMenu(player, this, actions, false);
  }

  // ── Lógica de Tick y Estados ──────────────────────────────────────────────

  @Override
  public void aiStep() {
    super.aiStep();

    if (this.level().isClientSide()) return;

    // Curación pasiva
    if (this.tickCount % 100 == 0 && this.getHealth() < this.getMaxHealth()) {
      this.heal(2.0F);
      ((ServerLevel)this.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
              this.getX(), this.getY() + 1, this.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
    }

    // Gestión de la Tribu
    this.entityData.get(TRIBE_ID).ifPresent(TribeManager::heartbeat);

    // Lógica de combate
    if (getAnimState() == AnimState.ATTACK) {
      tickAttackLogic();
    }
  }

  private void tickAttackLogic() {
    attackTick++;
    if (attackTick == 30) { // Hit tick
      this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.5D)).forEach(e -> {
        if (e != this && !(e instanceof KoboldEntity)) {
          e.hurt(this.damageSources().mobAttack(this), 5.0F);
          this.playSound(ModSounds.KOBOLD_ATTACK.get(), 1.0F, getPitch());
        }
      });
    }
    if (attackTick >= 60) {
      setAnimStateFiltered(AnimState.NULL);
      attackTick = 0;
    }
  }

  public float getPitch() {
    // Los Kobolds pequeños tienen voces más agudas
    return 1.2F - (this.entityData.get(BODY_SIZE) * 2.0F);
  }

  // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

  @Override
  public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
    registrar.add(new AnimationController<>(this, "movement", 5, state -> {
      if (getAnimState() != AnimState.NULL) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.null"));

      if (state.isMoving()) {
        String anim = this.isCrouching() ? "animation.kobold.crouch_walk" : "animation.kobold.walk";
        return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
      }
      return state.setAndContinue(RawAnimation.begin().thenLoop("animation.kobold.idle"));
    }));

    registrar.add(new AnimationController<>(this, "action", 0, this::handleActionState)
            .setSoundKeyframeHandler(this::handleSoundKeyframes));
  }

  private PlayState handleActionState(AnimationState<KoboldEntity> state) {
    AnimState anim = getAnimState();
    String name = switch (anim) {
      case ATTACK -> "animation.kobold.attack";
      case INTERACTION_START_A -> "animation.kobold.special_a_start";
      case INTERACTION_LOOP_A -> "animation.kobold.special_a_loop";
      case INTERACTION_FINISH_A -> "animation.kobold.special_a_finish";
      case INTERACTION_START_C -> "animation.kobold.special_c_start";
      case MINING -> "animation.kobold.mine";
      default -> "animation.kobold.null";
    };
    return state.setAndContinue(RawAnimation.begin().thenLoop(name));
  }

  private void handleSoundKeyframes(software.bernie.geckolib.core.animation.event.SoundKeyframeEvent<KoboldEntity> event) {
    String key = event.getKeyframeData().getSound();
    switch (key) {
      case "vocal_happy" -> playRandomSound(ModSounds.KOBOLD_VOICE_HAPPY);
      case "interaction_hit" -> {
        playRandomSound(ModSounds.INTERACTION_VOICE);
        if (isOwnerLocal()) net.minecraft.client.Minecraft.getInstance().gameRenderer.displayItemActivation(new ItemStack(net.minecraft.world.item.Items.HEART_OF_THE_SEA));
      }
    }
  }

  // ── Persistencia (NBT) ───────────────────────────────────────────────────

  @Override
  public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);
    nbt.putFloat("BodySize", this.entityData.get(BODY_SIZE));
    nbt.putString("KoboldName", this.entityData.get(KOBOLD_NAME));
    nbt.put("Inventory", this.inventory.serializeNBT());
    this.entityData.get(TRIBE_ID).ifPresent(uuid -> nbt.putUUID("TribeId", uuid));
  }

  @Override
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);
    this.entityData.set(BODY_SIZE, nbt.getFloat("BodySize"));
    this.entityData.set(KOBOLD_NAME, nbt.getString("KoboldName"));
    this.inventory.deserializeNBT(nbt.getCompound("Inventory"));
    if (nbt.hasUUID("TribeId")) {
      this.entityData.set(TRIBE_ID, Optional.of(nbt.getUUID("TribeId")));
    }
  }

  @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

  // ── Diálogos de Combate (Enmascarados) ───────────────────────────────────

  private static final String[] COMBAT_LINES = {
          "You're in the wrong neighborhood, pal!",
          "The tribe will protect its master!",
          "Is that the best you've got?",
          "Don't touch our shiny things!",
          "Go back to the mines!",
          "Ligma emeralds!"
  };

  public void sendCombatChat() {
    if (!this.level().isClientSide()) {
      this.sendChatBubble(COMBAT_LINES[this.random.nextInt(COMBAT_LINES.length)]);
    }
  }
}