package com.trolmastercard.sexmod.registry;

// --- IMPORTACIONES ---
// Comentamos las cosas que aún no hemos creado en la 1.20.1 para que no den error
// import com.trolmastercard.sexmod.item.AlliesLampItem;
// import com.trolmastercard.sexmod.item.GalathCoinItem;
// import com.trolmastercard.sexmod.item.HornyPotion;
// import com.trolmastercard.sexmod.item.SlimeItem;
// import com.trolmastercard.sexmod.item.StaffItem;
// import com.trolmastercard.sexmod.block.SexmodFireBlock;
// import com.trolmastercard.sexmod.item.GalathWandItem;

// Dejamos vivos SOLO los huevos que ya arreglamos
import com.trolmastercard.sexmod.item.KoboldEggSpawnItem;
import com.trolmastercard.sexmod.item.TribeEggItem;

public final class ModItems {

    private ModItems() {}

    public static void register() {
        // Silenciamos el registro de los ítems fantasma
        // SlimeItem.register();
        // AlliesLampItem.register();
        // StaffItem.register();
        // GalathCoinItem.register();
        // HornyPotion.register();
        // SexmodFireBlock.register();
        // GalathWandItem.register();

        // Registramos SOLO lo que sabemos que funciona
        TribeEggItem.register();
        KoboldEggSpawnItem.register();
    }
}