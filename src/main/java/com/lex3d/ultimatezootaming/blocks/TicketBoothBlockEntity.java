package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * BlockEntity GeckoLib du guichet de billetterie 1x2.
 * Modele geometry.ticket_booth : structure fermee avec espace interieur
 * pour le PNJ caissier, comptoir bas + vitre haute cote joueur, toit debordant,
 * bandeau enseigne BILLETS au sommet.
 *
 * Aucune animation autre que l'idle. Le PNJ Caissier navigue librement
 * dans l'espace interieur (logique separee dans KeeperTransformHandler).
 */
public class TicketBoothBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin()
            .thenLoop("animation.ticket_booth.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TicketBoothBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TICKET_BOOTH.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "main", 0,
                s -> s.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
