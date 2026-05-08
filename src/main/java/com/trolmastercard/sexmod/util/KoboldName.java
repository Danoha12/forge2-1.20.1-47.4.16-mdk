package com.trolmastercard.sexmod.util;

import net.minecraft.util.RandomSource;

/**
 * KoboldName — Portado a 1.20.1.
 * * Lista de nombres posibles para los miembros de la tribu.
 */
public enum KoboldName {
    Vags, Snis, Suk, Snok, Orpu, Dovni, Ahza, Zarbu, Rupli, Kagri,
    Nud, Gox, Vum, Snek, Aglo, Givlu, Gukle, Vutu, Evni, Kakla,
    Tuks, Nev, Kugs, Sneks, Vihli, Snuppu, Sogi, Guldo, Durbi, Hikbu,
    Guv, San, Ken, Nern, Zogni, Ahze, Snoblo, Snoggi, Nutro, Vekda,
    Morn, Snogs, Teg, Tigs, Rokko, Oblu, Tihzi, Mohru, Sahsu, Mahlu;

    /**
     * 🚨 REPARACIÓN: Cambiamos names.size() por names.length
     * y el nombre a randomName para que coincida con la entidad.
     */
    public static String randomName(RandomSource random) {
        KoboldName[] names = values();
        // Usamos .length porque es un array
        return names[random.nextInt(names.length)].name();
    }
}