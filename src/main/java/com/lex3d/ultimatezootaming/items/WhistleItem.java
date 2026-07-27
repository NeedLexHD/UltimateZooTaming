package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.core.init.ModSounds;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.RequestFamiliarsC2SPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Clic droit dans le vide -> coup de sifflet (audible par tous) + GUI complet.
 * Sneak + Clic droit sur un familier -> GUI pre-filtre sur cette cible.
 */
public class WhistleItem extends Item implements software.bernie.geckolib.animatable.GeoItem {
    // ---- GeckoLib ----
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private com.lex3d.ultimatezootaming.client.render.WhistleRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) this.renderer = new com.lex3d.ultimatezootaming.client.render.WhistleRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "idle", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation.whistle.idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    public WhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // Demande la liste a jour des familiars au serveur avant d'ouvrir le GUI.
            NetworkHandler.CHANNEL.sendToServer(new RequestFamiliarsC2SPacket());
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.lex3d.ultimatezootaming.client.ClientSetup.openWhistleScreen(null));
        } else {
            // Coup de sifflet cote serveur : tout le monde autour l'entend
            level.playSound(null, player.blockPosition(), ModSounds.WHISTLE_BLOW.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        var data = target.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) {
            return InteractionResult.PASS;
        }

        // JAMAIS interagir avec un animal ASSIGNE a un enclos (regle stricte).
        if (data.getZoneId() != null) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            NetworkHandler.CHANNEL.sendToServer(new RequestFamiliarsC2SPacket());
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.lex3d.ultimatezootaming.client.ClientSetup.openWhistleScreen(target.getUUID()));
        }

        return InteractionResult.SUCCESS;
    }
}
