package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;

/**
 * NpcActionCallback — Portado a 1.20.1.
 * * Interfaz funcional para callbacks de acciones.
 * * Se dispara cuando una interacción o fase de animación con un avatar termina.
 */
@FunctionalInterface
public interface NpcActionCallback {

  /**
   * Se llama cuando la acción asociada se ejecuta.
   * * @param npc El avatar del jugador (PlayerKoboldEntity) involucrado.
   */
  void onAction(PlayerKoboldEntity npc);
}