package com.trolmastercard.sexmod.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SexProposalManager — Gestiona las peticiones de interacción entre jugadores y NPCs.
 */
public class SexProposalManager {

    // El "Símbolo" que le faltaba a ClientSetup
    public static SexProposalManager INSTANCE;

    // Un mapa para rastrear quién le pidió qué a quién
    private final ConcurrentHashMap<UUID, String> activeProposals = new ConcurrentHashMap<>();

    public SexProposalManager() {
        // Constructor vacío para la inicialización en ClientSetup
    }

    public void createProposal(UUID targetNpc, String action) {
        activeProposals.put(targetNpc, action);
    }

    public void clearProposal(UUID targetNpc) {
        activeProposals.remove(targetNpc);
    }

    public String getAction(UUID targetNpc) {
        return activeProposals.get(targetNpc);
    }
}