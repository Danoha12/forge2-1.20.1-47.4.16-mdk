package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.ModConstants;
import net.minecraft.world.entity.Entity;

/**
 * NpcType - El Directorio Principal de Entidades.
 * Portado a 1.20.1.
 * Mapea cada variante de NPC y su contraparte de Jugador a sus IDs numéricos.
 * IMPORTANTE: Los IDs numéricos se mantienen intactos para no romper la
 * compatibilidad de guardado de mundos ni los paquetes de red (Networking).
 */
public enum NpcType {
    JENNY     (JennyEntity.class,      177013,  PlayerJennyEntity.class,    12388645),
    ELLIE     (EllieEntity.class,      228922,  PlayerEllieEntity.class,    46348348),
    BIA       (BiaEntity.class,        230053,  PlayerBiaEntity.class,      65456415),
    SLIME     (SlimeEntity.class,      168597,  PlayerSlimeEntity.class,    54816432),
    BEE       (BeeEntity.class,        4663354, PlayerBeeEntity.class,     48648638),
    ALLIE     (AllieEntity.class,      5614613, PlayerAllieEntity.class,   64867483),
    LUNA      (LunaEntity.class,       6816463, PlayerLunaEntity.class,    81234824),
    KOBOLD    (KoboldEntity.class,     5648456, PlayerKoboldEntity.class,  62484851, true),
    GOBLIN    (GoblinEntity.class,     4567275, PlayerGoblinEntity.class,   6584344, true),
    GALATH    (GalathEntity.class,     314351,  PlayerGalathEntity.class,  652535516),
    MANGLELIE (MangleLieEntity.class,  618151); // Variante exclusiva de NPC

    // =========================================================================
    //  Variables Técnicas
    // =========================================================================

    public final int npcID;
    public final int playerID;
    public final Class<? extends BaseNpcEntity> npcClass;
    public final Class<? extends BaseNpcEntity> playerClass; // Generalizado a BaseNpcEntity para evitar errores de casteo
    public final boolean isNpcOnly;
    public final boolean hasSpecifics;
    public final int editorID;

    // -- Constructor Completo (Con bandera de especificaciones) --
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID,
            Class<? extends BaseNpcEntity> playerClass, int playerID,
            boolean hasSpecifics) {
        this.npcID      = npcID;
        this.playerID   = playerID;
        this.npcClass   = npcClass;
        this.playerClass = playerClass;
        this.isNpcOnly  = false;
        this.hasSpecifics = hasSpecifics;
        this.editorID   = ModConstants.nextEditorID();
    }

    // -- Constructor Estándar --
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID,
            Class<? extends BaseNpcEntity> playerClass, int playerID) {
        this(npcClass, npcID, playerClass, playerID, false);
    }

    // -- Constructor Solo-NPC (Ej. MangleLie) --
    NpcType(Class<? extends BaseNpcEntity> npcClass, int npcID) {
        this.npcID      = npcID;
        this.npcClass   = npcClass;
        this.isNpcOnly  = true;
        this.hasSpecifics = false;
        this.editorID   = ModConstants.nextEditorID();
        this.playerClass = null;
        this.playerID   = 0;
    }

    // =========================================================================
    //  Helpers de Búsqueda
    // =========================================================================

    /** Búsqueda por nombre ignorando mayúsculas, por defecto retorna JENNY. */
    public static NpcType fromString(String name) {
        for (NpcType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return JENNY;
    }

    /** Resuelve el tipo de NPC desde una instancia de entidad viva en el mundo. */
    public static NpcType fromEntity(Entity entity) {
        if (!(entity instanceof BaseNpcEntity npc)) return null;
        Class<?> cls = npc.getClass();
        for (NpcType t : values()) {
            if (cls.equals(t.npcClass))   return t;
            if (t.playerClass != null && cls.equals(t.playerClass)) return t;
        }
        return null;
    }
}