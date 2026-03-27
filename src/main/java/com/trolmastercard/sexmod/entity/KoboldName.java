package com.trolmastercard.sexmod.entity;

/**
 * KoboldName - Registro de Nombres de la Tribu.
 * Portado a 1.20.1.
 * * Enumeración de todos los nombres posibles para los miembros de la tribu.
 * * Se utiliza para asignar un nombre único aleatorio a cada Kobold al spawnear.
 */
public enum KoboldName {
    Vags, Snis, Suk, Snok, Orpu, Dovni, Ahza, Zarbu, Rupli, Kagri,
    Nud, Gox, Vum, Snek, Aglo, Givlu, Gukle, Vutu, Evni, Kakla,
    Tuks, Nev, Kugs, Sneks, Vihli, Snuppu, Sogi, Guldo, Durbi, Hikbu,
    Guv, San, Ken, Nern, Zogni, Ahze, Snoblo, Snoggi, Nutro, Vekda,
    Morn, Snogs, Teg, Tigs, Rokko, Oblu, Tihzi, Mohru, Sahsu, Mahlu;

    /**
     * Retorna un nombre aleatorio de la lista.
     * Útil para inicializar el DataParameter KOBOLD_NAME.
     */
    public static KoboldName getRandom(java.util.Random random) {
        return values()[random.nextInt(values().length)];
    }
}