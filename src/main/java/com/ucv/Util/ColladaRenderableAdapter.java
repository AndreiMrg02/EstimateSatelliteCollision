package com.ucv.Util;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.ogc.collada.impl.ColladaController;

public class ColladaRenderableAdapter implements Renderable {
    private ColladaController colladaController;

    public ColladaRenderableAdapter(ColladaController colladaController) {
        this.colladaController = colladaController;

    }



    @Override
    public void render(DrawContext drawContext) {
        this.colladaController.render(drawContext);
    }
}
