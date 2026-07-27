package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZooKeeperRenderer extends GeoEntityRenderer<ZooKeeperEntity> {
    public ZooKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new ZooKeeperModel());
        this.shadowRadius = 0.4f;
        // Outil de metier dans la main droite
        addRenderLayer(new KeeperToolLayer(this));
        addRenderLayer(new KeeperTaskLayer(this)); // tache affichee au-dessus de la tete
    }
}
