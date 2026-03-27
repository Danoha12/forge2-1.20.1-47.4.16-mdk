package com.trolmastercard.sexmod.entity;

/**
 * KoboldNameList - Lista extendida de nombres para la Tribu.
 * Portado a 1.20.1.
 * * Enumeración de nombres posibles para la generación procedimental de Kobolds.
 */
public enum KoboldNameList {
    Vags, Snis, Suk, Snok, Orpu, Dovni, Ahza, Zarbu, Rupli, Kagri,
    Nud, Gox, Vum, Snek, Aglo, Givlu, Gukle, Vutu, Evni, Kakla,
    Tuks, Nev, Kugs, Sneks, Vihli, Snuppu, Sogi, Guldo, Durbi, Hikbu,
    Guv, San, Ken, Nern, Zogni, Ahze, Snoblo, Snoggi, Nutro, Vekda,
    Morn, Snogs, Teg, Tigs, Rokko, Oblu, Tihzi, Mohru, Sahsu, Mahlu;

    /**
     * Retorna un elemento aleatorio de la lista.
     * * @param rng Instancia de Random para la selección.
     * @return Una variante de nombre aleatoria.
     */
    public static KoboldNameList random(java.util.Random rng) {
        KoboldNameList[] values = values();
        return values[rng.nextInt(values.length)];
    }
}