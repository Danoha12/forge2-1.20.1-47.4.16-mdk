package com.trolmastercard.sexmod.client;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.Main;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * OutlineShaderManager - ported from ae.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Loads and manages the post-processing outline shader for NPC entities.
 *
 * In 1.12.2:
 *   - Used {@link net.minecraft.client.shader.ShaderGroup} and {@link net.minecraft.client.shader.ShaderLinkHelper}
 *   - Registered via {@code ClientRegistry.registerEntityShader(em.class, location)}
 *
 * In 1.20.1:
 *   - Entity outline rendering is handled automatically when the entity's
 *     renderer returns true from {@code shouldShowName} or by overriding
 *     {@code shouldRenderWhileSleeping}. The outline effect itself is driven
 *     by the vanilla glowing effect or by a custom {@link PostChain}.
 *   - {@link PostChain} replaces the old {@code ShaderGroup}.
 *   - The "final" target is retrieved via {@code postChain.getTempTarget("final")}.
 *
 * Usage:
 * <pre>
 *   // In your client-setup event:
 *   OutlineShaderManager.load();
 * </pre>
 *
 * The shader JSON should live at:
 *   {@code assets/sexmod/shaders/post/outline.json}
 */
@OnlyIn(Dist.CLIENT)
public class OutlineShaderManager {

    private static final ResourceLocation SHADER_LOCATION =
        new ResourceLocation("sexmod", "shaders/post/outline.json");

    /** The loaded post-processing chain, or null if loading failed. */
    public static PostChain postChain;

    /** The "final" render target output from the shader chain. */
    public static RenderTarget finalTarget;

    private OutlineShaderManager() {}

    // =========================================================================
    //  Load
    // =========================================================================

    /**
     * Loads the outline shader. Call this from your client setup event
     * ({@code FMLClientSetupEvent}) or from a window-resize handler.
     *
     * Equivalent to: {@code ae.a()}
     */
    public static void load() {
        Minecraft mc = Minecraft.getInstance();

        try {
            postChain = new PostChain(
                mc.getTextureManager(),
                mc.getResourceManager(),
                mc.getMainRenderTarget(),
                SHADER_LOCATION);

            postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            finalTarget = postChain.getTempTarget("final");

            System.out.println("succ registered the outline shader :)");

        } catch (Exception e) {
            Main.LOGGER.warn("Failed to load shader: {}", SHADER_LOCATION, e);
            postChain   = null;
            finalTarget = null;
        }
    }

    // =========================================================================
    //  Resize handler
    // =========================================================================

    /**
     * Resize the shader targets to match the new window dimensions.
     * Call this from a {@link net.minecraftforge.client.event.MovementInputUpdateEvent}
     * or window-resize callback.
     */
    public static void resize(int width, int height) {
        if (postChain != null) {
            postChain.resize(width, height);
            finalTarget = postChain.getTempTarget("final");
        }
    }

    /**
     * Returns true if the shader is loaded and ready to use.
     */
    public static boolean isAvailable() {
        return postChain != null;
    }
}
