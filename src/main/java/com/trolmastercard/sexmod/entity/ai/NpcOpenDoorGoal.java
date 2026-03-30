package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

import java.util.EnumSet;

/**
 * NpcOpenDoorGoal — Portado a 1.20.1.
 * * Permite a los NPCs abrir y cerrar puertas de madera en su camino.
 * * Usa Tags de bloque en lugar de Materiales (obsoleto).
 */
public class NpcOpenDoorGoal extends Goal {

    protected final Mob mob;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected DoorBlock doorBlock;
    protected boolean crossed;
    protected float initDx;
    protected float initDz;
    protected int closeCountdown = 10;

    public NpcOpenDoorGoal(Mob mob) {
        this.mob = mob;
        // Verificamos que el NPC pueda caminar por tierra
        if (!(mob.getNavigation() instanceof GroundPathNavigation)) {
            throw new IllegalArgumentException("NpcOpenDoorGoal requiere GroundPathNavigation");
        }
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    // ── Lógica de Activación ─────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (!mob.horizontalCollision) return false;

        GroundPathNavigation nav = (GroundPathNavigation) mob.getNavigation();
        Path path = nav.getPath();

        if (path == null || path.isDone() || !nav.isInProgress()) return false;

        // Escaneamos los nodos cercanos en la ruta
        for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
            Node node = path.getNode(i);
            this.doorPos = new BlockPos(node.x, node.y, node.z);

            // Si la puerta está a una distancia razonable
            if (mob.distanceToSqr(this.doorPos.getX(), mob.getY(), this.doorPos.getZ()) <= 2.25D) {
                this.doorBlock = getWoodenDoor(this.doorPos);
                if (this.doorBlock != null) return true;
            }
        }

        // Fallback: verificar el bloque frente a los ojos
        this.doorPos = mob.blockPosition().above();
        this.doorBlock = getWoodenDoor(this.doorPos);
        return this.doorBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.crossed && this.closeCountdown > 0;
    }

    // ── Ciclo de Vida del Goal ───────────────────────────────────────────────

    @Override
    public void start() {
        this.crossed = false;
        this.closeCountdown = 10;

        // Calculamos el vector inicial para saber cuándo cruzamos la puerta
        this.initDx = (float) ((double) this.doorPos.getX() + 0.5D - mob.getX());
        this.initDz = (float) ((double) this.doorPos.getZ() + 0.5D - mob.getZ());

        // Abrir la puerta
        setDoorState(true);
    }

    @Override
    public void tick() {
        float dx = (float) ((double) this.doorPos.getX() + 0.5D - mob.getX());
        float dz = (float) ((double) this.doorPos.getZ() + 0.5D - mob.getZ());

        // Producto escalar para detectar si el NPC ya pasó el umbral
        float dotProduct = this.initDx * dx + this.initDz * dz;

        if (dotProduct < 0.0F) {
            this.crossed = true;
        }

        if (this.crossed) {
            if (this.closeCountdown-- <= 0) {
                setDoorState(false); // Cerrar tras el NPC
            }
        }
    }

    @Override
    public void stop() {
        // Aseguramos que la puerta se cierre si el Goal se cancela abruptamente
        if (this.doorBlock != null && getWoodenDoor(this.doorPos) != null) {
            setDoorState(false);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DoorBlock getWoodenDoor(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        // En 1.20.1 usamos Tags para verificar si es madera
        if (state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS)) {
            return (DoorBlock) state.getBlock();
        }
        return null;
    }

    private void setDoorState(boolean open) {
        BlockState state = mob.level().getBlockState(this.doorPos);
        if (state.getBlock() instanceof DoorBlock) {
            ((DoorBlock) state.getBlock()).setOpen(mob, mob.level(), state, this.doorPos, open);
        }
    }
}