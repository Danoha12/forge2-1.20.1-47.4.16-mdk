package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * FigureNpcRenderer (d1) - BaseNpcRenderer subclass that additionally hides
 * the "figure" bone (used by NPC models that carry a separate figure mesh
 * in the same geo file but want it culled by default).
 */
public class FigureNpcRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    public FigureNpcRenderer(GeoModel<T> model, double shadowRadius) {
        super(model, shadowRadius);
    }

    @Override
    public HashSet<String> getHiddenBones() {
        HashSet<String> bones = super.getHiddenBones();
        bones.add("figure");
        return bones;
    }
}
