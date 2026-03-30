package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.BeeNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * BeeBodyRenderer — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * Renderizador de cuerpo completo para el NPC Bee.
 * Sobreescribe el conjunto de huesos ocultos para apagar ciertos elementos de la
 * malla base ("leaf7" y "leaf8", además de elementos anatómicos) para
 * evitar recortes visuales con el modelo principal.
 */
public class BeeBodyRenderer extends BaseNpcRenderer<BeeNpcEntity> {

    // Constructor actualizado para 1.20.1 exigiendo el Context del RenderProvider
    public BeeBodyRenderer(EntityRendererProvider.Context context, GeoModel<BeeNpcEntity> model) {
        super(context, model, 0.5f); // 0.5f es el tamaño de la sombra por defecto
    }

    /**
     * Retorna el conjunto de huesos que siempre deben ser invisibles para este renderizador,
     * independientemente del estado de la ropa de la entidad.
     */
    // NOTA: Si en BaseNpcRenderer este método se llama diferente, asegúrate de unificar el nombre.
    // Por ahora lo dejamos como getExtraHiddenBones para respetar tu lógica original.
    public HashSet<String> getExtraHiddenBones() {
        return new HiddenBoneSet();
    }

    // -------------------------------------------------------------------------
    //  Definición de huesos ocultos (Mapeo directo a Blockbench)
    // -------------------------------------------------------------------------

    /** * Huesos que son descartados del renderizado del cuerpo de Bee.
     * Los nombres deben coincidir exactamente con el archivo .geo.json.
     */
    private static class HiddenBoneSet extends HashSet<String> {
        HiddenBoneSet() {
            // Elementos de la malla anatómica base
            add("boobs");
            add("booty");
            add("vagina");
            add("fuckhole");
            // Elementos de ropa / hojas
            add("leaf7");
            add("leaf8");
        }
    }
}