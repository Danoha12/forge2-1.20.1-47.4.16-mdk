package com.trolmastercard.sexmod.util;

import net.minecraft.util.RandomSource;

/**
 * KoboldNameList — Portado a 1.20.1.
 * * Lista de nombres procedimentales para los miembros de la tribu.
 */
public enum KoboldNameList {
    Vags, Snis, Suk, Snok, Orpu, Dovni, Ahza, Zarbu, Rupli, Kagri,
    Nud, Gox, Vum, Snek, Aglo, Givlu, Gukle, Vutu, Evni, Kakla,
    Tuks, Nev, Kugs, Sneks, Vihli, Snuppu, Sogi, Guldo, Durbi, Hikbu,
    Guv, San, Ken, Nern, Zogni, Ahze, Snoblo, Snoggi, Nutro, Vekda,
    Morn, Snogs, Teg, Tigs, Rokko, Oblu, Tihzi, Mohru, Sahsu, Mahlu;

    /**
     * Devuelve un nombre aleatorio de la lista usando el RandomSource de Minecraft.
     */
    public static String getRandomName(RandomSource random) {
        KoboldNameList[] names = values();
        return names[random.nextInt(names.length)].name();
    }
}