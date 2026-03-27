package com.trolmastercard.sexmod;

import net.minecraft.resources.ResourceLocation;

/**
 * NpcHandModel — stub for hand model rendering.
 * Subclasses provide specific hand models per character.
 */
public class NpcHandModel {

    protected final ResourceLocation texture;

    public NpcHandModel(ResourceLocation texture) {
        this.texture = texture;
    }

    public NpcHandModel() {
        this(new ResourceLocation("sexmod", "textures/entity/default_hand.png"));
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffer,
                       int packedLight, float partialTick) {
        // Override in subclasses
    }
}
