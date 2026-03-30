package com.trolmastercard.sexmod.util;

import net.minecraft.util.RandomSource;

/**
 * KoboldName — Portado a 1.20.1.
 * * Lista de nombres posibles para los miembros de la tribu.
 * Incluye un método estático para obtener un nombre aleatorio de forma eficiente.
 */
public enum KoboldName {
    Vags, Snis, Suk, Snok, Orpu, Dovni, Ahza, Zarbu, Rupli, Kagri,
    Nud, Gox, Vum, Snek, Aglo, Givlu, Gukle, Vutu, Evni, Kakla,
    Tuks, Nev, Kugs, Sneks, Vihli, Snuppu, Sogi, Guldo, Durbi, Hikbu,
    Guv, San, Ken, Nern, Zogni, Ahze, Snoblo, Snoggi, Nutro, Vekda,
    Morn, Snogs, Teg, Tigs, Rokko, Oblu, Tihzi, Mohru, Sahsu, Mahlu;

    /**
     * @param random Fuente de aleatoriedad (usualmente entity.getRandom())
     * @return Un nombre aleatorio de la lista convertido a String.
     */
    public static String getRandomName(RandomSource random) {
        KoboldName[] names = values();
        return names[random.nextInt(names.size())].name();
    }
}