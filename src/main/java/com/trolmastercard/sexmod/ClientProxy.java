package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

/**
 * ClientProxy - ported from ClientProxy.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Client-side proxy. Extends CommonProxy and adds:
 *   - KeyBinding registration
 *   - Client-side event registration
 *   - Entity model preloading via FakeWorld
 *   - Client command registration
 *   - Particle factory registration
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - FMLPreInitializationEvent - FMLCommonSetupEvent / FMLClientSetupEvent
 *   - KeyBinding(name, keyCode, category) - KeyMapping (same ctor)
 *   - ClientRegistry.registerKeyBinding - ClientRegistry.registerKeyBinding (same)
 *   - RenderManager.func_188391_a - entityRenderer.render (use renderers directly in 1.20.1)
 *   - gj - FakeWorld
 *   - w.a = new w() - SexProposalManager.INSTANCE = new SexProposalManager()
 *   - ClientCommandHandler.instance.func_71560_a - ClientCommandHandler.instance.registerCommand
 *   - Minecraft.field_71452_i.func_178929_a(id, factory) - Minecraft.particleEngine.register(id, factory)
 *   - 625115 particle ID - ParticleTypes registration
 *   - fk.a() - EntityRenderRegistry.init()
 *   - new et(true) - GUI screen registration in MenuScreens
 *   - bn.a(true) - EventRegistrar.register(true)
 *   - fd.a / fx.a / a_.b - WhitelistServerCommand / SetModelCodeCommand / FutaCommand instances
 *
 * NOTE: In 1.20.1 preloading entities requires DeferredWorkQueue or onResourceManagerReload.
 *       The preload loop from ClientProxy is preserved in spirit but adapted.
 */
public class ClientProxy extends CommonProxy {

    public static boolean IS_PRELOADING = false;

    public static KeyMapping[] keyBindings;

    @Override
    public void postInit(FMLLoadCompleteEvent event) throws IOException {
        // Client does NOT call server-side custom model setup
    }

    @Override
    public void preInit(FMLCommonSetupEvent event) {
        super.preInit(event);
        EntityRenderRegistry.init();
    }

    @Override
    public void init(FMLCommonSetupEvent event) throws IOException {
        // Key bindings - registered during client setup
        keyBindings    = new KeyMapping[2];
        keyBindings[0] = new KeyMapping("Interact with your goblin",          GLFW.GLFW_KEY_G,  "Sex mod");
        keyBindings[1] = new KeyMapping("open character customisation menu",  GLFW.GLFW_KEY_L,  "Sex mod");
        for (KeyMapping kb : keyBindings) ClientRegistry.registerKeyBinding(kb);

        Main.setConfigs();
        ModSounds.register();
        EventRegistrar.register(true);
        ModNetwork.init();

        // Preload entity models through FakeWorld so textures/models are cached
        // before the player ever encounters these entities in-world.
        Minecraft mc = Minecraft.getInstance();
        FakeWorld fakeWorld = FakeWorld.createFromCurrentLevel(mc);
        IS_PRELOADING = true;
        try {
            for (NpcType type : NpcType.values()) {
                try {
                    BaseNpcEntity preview = (BaseNpcEntity)
                            type.npcClass
                                .getDeclaredConstructor(Level.class)
                                .newInstance(fakeWorld);
                    // Trigger renderer registration / caching
                    mc.getEntityRenderDispatcher().render(
                            preview, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F,
                            mc.renderBuffers().bufferSource().getBuffer(
                                    net.minecraft.client.renderer.RenderType.solid()),
                            0, 0, 1f, 1f, 1f, 1f);
                } catch (Exception ignored) {
                    // Non-fatal - model will be loaded on first render
                }
            }
        } catch (Exception e) {
            System.out.println("error while preloading:");
            e.printStackTrace();
        }
        IS_PRELOADING = false;

        // Singleton init
        SexProposalManager.INSTANCE = new SexProposalManager();

        // Client-side commands
        ClientCommandHandler.instance.registerCommand(WhitelistServerCommand.INSTANCE);
        ClientCommandHandler.instance.registerCommand(SetModelCodeCommand.INSTANCE);
        ClientCommandHandler.instance.registerCommand(FutaCommand.INSTANCE);

        // Particle factory for dragon breath / cummy particles (ID 625115)
        mc.particleEngine.register(
                SexmodDragonBreathParticle.TYPE,
                SexmodDragonBreathParticle.Factory::new);
    }
}
