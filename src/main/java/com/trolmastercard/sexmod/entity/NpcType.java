package com.trolmastercard.sexmod.entity;

import net.minecraft.world.entity.Entity;
import javax.annotation.Nullable;

/**
 * NpcType — Portado a 1.20.1.
 * * Mapea cada variante de NPC y avatar de jugador a sus clases e IDs numéricos.
 * * Utilizado para lógica de red, persistencia NBT y filtrado en la GUI.
 */
public enum NpcType {
    JENNY    (JennyEntity.class,       177013,  PlayerJennyEntity.class,  12388645),
    ELLIE    (EllieEntity.class,       228922,  PlayerEllieEntity.class,  46348348),
    BIA      (BiaEntity.class,         230053,  PlayerBiaEntity.class,    65456415),
    SLIME    (SlimeEntity.class,       168597,  PlayerSlimeEntity.class,  54816432),
    BEE      (BeeEntity.class,         4663354, PlayerBeeEntity.class,   48648638),
    ALLIE    (AllieEntity.class,       5614613, PlayerAllieEntity.class, 64867483),
    LUNA     (LunaEntity.class,        6816463, PlayerLunaEntity.class,  81234824),
    KOBOLD   (KoboldEntity.class,      5648456, PlayerKoboldEntity.class, 62484851, true),
    GOBLIN   (GoblinEntity.class,      4567275, PlayerGoblinEntity.class, 6584344, true),
    GALATH   (GalathEntity.class,      314351,  PlayerGalathEntity.class, 652535516),
    MANGLELIE(MangleLieEntity.class,   618151);

    public final int npcID;
    public final int playerID;
    public final Class<? extends BaseNpcEntity> npcClass;
    @Nullable public final Class<? extends PlayerKoboldEntity> playerClass;
    public final boolean isNpcOnly;
    public final boolean hasSpecifics;

    // ── Constructor Completo ─────────────────────────────────────────────────
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID,
            @Nullable Class<? extends PlayerKoboldEntity> playerClass, int playerID,
            boolean hasSpecifics) {
        this.npcClass = npcClass;
        this.npcID = npcID;
        this.playerClass = playerClass;
        this.playerID = playerID;
        this.isNpcOnly = (playerClass == null);
        this.hasSpecifics = hasSpecifics;
    }

    // ── Constructor con Player (Por defecto sin specifics) ──────────────────
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID,
            Class<? extends PlayerKoboldEntity> playerClass, int playerID) {
        this(npcClass, npcID, playerClass, playerID, false);
    }

    // ── Constructor Solo NPC ─────────────────────────────────────────────────
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID) {
        this(npcClass, npcID, null, 0, false);
    }

    // ── Métodos de Búsqueda ──────────────────────────────────────────────────

    /** Busca por nombre, ignorando mayúsculas. Por defecto JENNY. */
    public static NpcType fromString(String name) {
        for (NpcType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return JENNY;
    }

    /** Resuelve el tipo a partir de una entidad viva en el mundo. */
    @Nullable
    public static NpcType fromEntity(Entity entity) {
        if (!(entity instanceof BaseNpcEntity) && !(entity instanceof PlayerKoboldEntity)) return null;

        Class<?> entityClass = entity.getClass();
        for (NpcType type : values()) {
            if (type.npcClass.equals(entityClass)) return type;
            if (type.playerClass != null && type.playerClass.equals(entityClass)) return type;
        }
        return null;
    }
}