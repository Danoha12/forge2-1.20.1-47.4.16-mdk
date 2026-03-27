package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import com.trolmastercard.sexmod.client.event.*;
import com.trolmastercard.sexmod.client.model.CustomModelManager;
import com.trolmastercard.sexmod.data.CustomModelSavedData;
import com.trolmastercard.sexmod.entity.*;
import com.trolmastercard.sexmod.event.*;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import com.trolmastercard.sexmod.item.WinchesterItem;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;

/**
 * EventRegistrar - ported from bn.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Central place that registers all Forge event-bus handlers.
 * Called from the main {@code @Mod} class setup.
 *
 * In 1.12.2 this was invoked once from {@code Main.preInit()} with
 * {@code paramBoolean = isClient}.  In 1.20.1, the distinction between
 * client-only and shared handlers is managed by {@code @OnlyIn(Dist.CLIENT)}
 * annotations on the handler classes themselves, but we keep the explicit
 * registration pattern for clarity.
 *
 * Original class list (bn.java - clean name):
 *   ah    - NpcDamageHandler
 *   eo    - (EntityRegistrar / world-gen event)
 *   q     - PlayerConnectionHandler
 *   co    - (unknown)
 *   gu    - (unknown)
 *   ho.a  - (unknown)
 *   g.a   - (unknown)
 *   ap.b  - AlliesLampItem.InteractHandler
 *   hy.b  - StaffItem.InteractHandler
 *   hj.a  - (unknown)
 *   gp    - (unknown)
 *   fu    - (unknown)
 *   eb.a  - NpcInventoryEntity.EventHandler
 *   ey    - (unknown)
 *   dw.a  - (unknown)
 *   ff.c  - KoboldEntity.EventHandler
 *   hy.a  - StaffItem.ModelHandler
 *   ax.b  - TribeManager.WorldEventHandler("tribes")
 *   c7    - TribeEggItem.EventHandler  (c7 is the pre-1.12 name for TribeEggItem)
 *   am    - NpcRenderEventHandler
 *   e3.c  - GoblinEntity.EventHandler
 *   eq.a  - PlayerGoblinEntity.EventHandler
 *   ap.a  - AlliesLampItem.InteractHandler
 *   ad    - DevToolsHandler
 *   f_.a  - GalathEntity.EventHandler
 *   v     - GalathOwnershipData
 *   cc.r  - GalathCoinItem.RightClickHandler
 *   aj.b  - WinchesterItem.InteractHandler
 *   fq    - (unknown)
 *   gy    - (unknown)
 *   bj    - CustomModelSavedData
 *   g3.b  - (unknown)
 *   f8.b  - MangleLieEntity.EventHandler
 *   f4    - (unknown)
 *
 * CLIENT-only (paramBoolean = true):
 *   fr    - (screen/render event)
 *   ds    - (unknown)
 *   fh    - (unknown)
 *   d3    - ClientStateManager
 *   l     - (key input handler)
 *   bq    - MenuClearHandler
 *   cn    - (unknown)
 *   e_    - (unknown)
 *   w     - SexProposalManager
 *   dv.a  - (unknown)
 *   gm    - (unknown)
 *   c6    - (unknown)
 *   NpcCustomizeScreen.b - NpcCustomizeScreen.EventHandler
 *   br.a  - CustomModelManager.ClientEventHandler
 *   gb    - (unknown)
 *   ga    - (unknown)
 *   hf    - (unknown)
 *
 * "dontAskAgain" flag: if {@code sexmod/dontAskAgain} file exists, client-side
 * consent UI is skipped (used to suppress the first-time disclaimer).
 *
 * TODO: fill in all remaining obfuscated class names once their ports are complete.
 */
public class EventRegistrar {

    /**
     * Registers all event handlers on the Forge bus.
     *
     * @param isClient true when running on the physical client side.
     */
    public static void register(boolean isClient) throws IOException {
        MinecraftForge.EVENT_BUS.register(new NpcDamageHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerConnectionHandler());
        MinecraftForge.EVENT_BUS.register(new AlliesLampItem.InteractHandler());
        MinecraftForge.EVENT_BUS.register(new TribeManager.WorldEventHandler("tribes"));
        MinecraftForge.EVENT_BUS.register(new NpcRenderEventHandler()); // am  shared? only active client-side
        MinecraftForge.EVENT_BUS.register(new DevToolsHandler());
        MinecraftForge.EVENT_BUS.register(new GalathOwnershipData());
        MinecraftForge.EVENT_BUS.register(new CustomModelSavedData());
        MinecraftForge.EVENT_BUS.register(WinchesterItem.INSTANCE);

        if (isClient) {
            registerClientHandlers();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static void registerClientHandlers() {
        boolean showConsent = shouldShowConsent();
        if (showConsent) {
            // Register consent UI handler (fr) - TODO when fr is ported
        }
        MinecraftForge.EVENT_BUS.register(new MenuClearHandler());
        MinecraftForge.EVENT_BUS.register(new SexProposalManager());
        MinecraftForge.EVENT_BUS.register(new NpcCustomizeScreen.EventHandler());
        MinecraftForge.EVENT_BUS.register(new CustomModelManager.ClientEventHandler());
    }

    /** Returns true if the "dontAskAgain" file does NOT exist (consent required). */
    static boolean shouldShowConsent() {
        File file = new File("sexmod/dontAskAgain");
        file.getParentFile().mkdirs();
        return !file.exists();
    }
}
