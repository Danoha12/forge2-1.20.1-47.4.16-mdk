package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * KoboldEggItemRenderer - ported from hn.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * GeckoLib4 item renderer for the KoboldEgg item ({@link KoboldEggSpawnItem}).
 * Overrides bone rendering to tint two bones with the kobold color variant:
 *
 *   "shell"      - tinted with {@link KoboldColorVariant#getShellColor()} (cp.b equivalent)
 *   "colorSpots" - tinted with the variant's main spot colour
 *
 * Color is read from the item's damage value (which encodes the wool/color index).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - GeoItemRenderer&lt;c7&gt; - GeoItemRenderer&lt;KoboldEggSpawnItem&gt;
 *   - new f6() - new KoboldEggItemModel()
 *   - renderRecursively(BufferBuilder, GeoBone, r,g,b,a) -
 *     renderRecursively(PoseStack, KoboldEggSpawnItem, GeoBone, VertexConsumer, tick, partialTick, light, overlay, r,g,b,a)
 *   - super.render(item, stack) - renderByItem(stack, displayContext, poseStack, buffer, light, overlay)
 *   - paramGeoBone.getName() - bone.getName()
 *   - cp.b.getRed/Green/Blue() - KoboldColorVariant.SHELL_COLOR.getRed/Green/Blue()
 *   - a(stack).getMainColor() - getColorVariant(stack).getSpotColor()
 *   - Vec3i.func_177958_n/o/p() - .getX/.getY/.getZ()
 *   - func_77960_j() - stack.getDamageValue() (metadata - damage in 1.20.1)
 *   - EyeAndKoboldColor.getColorByWoolId - KoboldColorVariant.getByIndex
 */
public class KoboldEggItemRenderer extends GeoItemRenderer<KoboldEggSpawnItem> {

    /** The last ItemStack being rendered (needed to read color in renderRecursively). */
    private ItemStack currentStack = null;

    public KoboldEggItemRenderer() {
        super(new KoboldEggItemModel());
    }

    // -- Render entry -----------------------------------------------------------

    @Override
    public void renderByItem(ItemStack stack,
                              net.minecraft.world.item.ItemDisplayContext displayContext,
                              PoseStack poseStack,
                              net.minecraft.client.renderer.MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay) {
        this.currentStack = stack;
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // -- Bone-level colour override ---------------------------------------------

    @Override
    public void renderRecursively(PoseStack poseStack,
                                   KoboldEggSpawnItem animatable,
                                   GeoBone bone,
                                   net.minecraft.client.renderer.RenderType renderType,
                                   net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                   VertexConsumer buffer,
                                   boolean isReRender,
                                   float partialTick,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {

        String name = bone.getName();

        if ("shell".equals(name)) {
            // Shell uses the global egg shell colour (cp.b in original = KoboldColorVariant.SHELL_COLOR)
            java.awt.Color shellColor = KoboldColorVariant.SHELL_COLOR;
            red   = shellColor.getRed()   / 255.0F;
            green = shellColor.getGreen() / 255.0F;
            blue  = shellColor.getBlue()  / 255.0F;
        } else if ("colorSpots".equals(name)) {
            // Spot colour comes from the item variant (encoded in damage/metadata)
            KoboldColorVariant variant = getColorVariant(currentStack);
            int[] rgb = variant.getMainColorRGB();
            red   = rgb[0] / 255.0F;
            green = rgb[1] / 255.0F;
            blue  = rgb[2] / 255.0F;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // -- Helper -----------------------------------------------------------------

    /** Resolves the {@link KoboldColorVariant} from the egg item's damage/metadata value. */
    private KoboldColorVariant getColorVariant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return KoboldColorVariant.getDefault();
        // In 1.12.2 metadata was func_77960_j(); in 1.20.1 we use getDamageValue() or NBT.
        int index = stack.getDamageValue();
        return KoboldColorVariant.getByIndex(index);
    }
}
