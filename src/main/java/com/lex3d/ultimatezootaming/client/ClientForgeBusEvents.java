package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.config.ZooClientConfig;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.lex3d.ultimatezootaming.core.network.ConfigSyncC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeBusEvents {

    private static final double REACH = 6.0;
    private static int hintCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // On consomme TOUJOURS le clic (meme si un GUI est deja ouvert), pour ne pas
        // le mettre en file d'attente et le voir s'ouvrir en differe/en rafale plus
        // tard une fois l'autre GUI ferme (comportement confus sinon).
        while (ClientModBusEvents.OPEN_MATERNITY.consumeClick()) {
            if (Minecraft.getInstance().screen == null && !ClientSetup.getPendingBabies().isEmpty()) {
                ClientSetup.openMaternityScreen();
            }
        }

        showBaitHint();
        showWelfareHint();
    }

    /**
     * En visant un familier apprivoise (main libre ou non), affiche son bien-etre
     * en actionbar : humeur + barre de satisfaction. Demande la donnee au serveur
     * (throttle 5 ticks) et l'affiche depuis le cache client.
     */
    private static int welfareCooldown = 0;

    private static void showWelfareHint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) {
            com.lex3d.ultimatezootaming.client.ClientWelfareCache.clearCurrentTarget();
            return;
        }

        // Ne pas se marcher dessus avec l'indice de regime (croquette/sifflet en main)
        boolean holdingBaitItem = player.getMainHandItem().getItem() instanceof KibbleItem
                || player.getMainHandItem().is(ModItems.WHISTLE.get())
                || player.getOffhandItem().getItem() instanceof KibbleItem
                || player.getOffhandItem().is(ModItems.WHISTLE.get());
        if (holdingBaitItem) {
            com.lex3d.ultimatezootaming.client.ClientWelfareCache.clearCurrentTarget();
            return;
        }

        LivingEntity target = getLookedAtLivingEntity(player);
        if (target == null) {
            // On ne vise plus rien : la barre disparait immediatement
            com.lex3d.ultimatezootaming.client.ClientWelfareCache.clearCurrentTarget();
            return;
        }

        // On vise un mob : memorise la cible courante (l'overlay decidera d'afficher)
        com.lex3d.ultimatezootaming.client.ClientWelfareCache.setCurrentTarget(
                target.getId(), target.getName().getString());

        // Demande le bien-etre au serveur (throttle)
        if (welfareCooldown-- <= 0) {
            welfareCooldown = 5;
            NetworkHandler.CHANNEL.sendToServer(
                    new com.lex3d.ultimatezootaming.core.network.RequestWelfareC2SPacket(target.getId()));
        }
    }

    /**
     * "Savoir quel appat est demande pour quel mob" : en visant un mob avec une
     * Croquette ou le Sifflet en main, affiche en actionbar son regime attendu.
     * Verifie seulement 1x toutes les 5 ticks (pas chaque tick) pour rester leger.
     */
    private static void showBaitHint() {
        if (hintCooldown-- > 0) return;
        hintCooldown = 5;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) return;

        boolean holdingRelevantItem = player.getMainHandItem().getItem() instanceof KibbleItem
                || player.getMainHandItem().is(ModItems.WHISTLE.get())
                || player.getOffhandItem().getItem() instanceof KibbleItem
                || player.getOffhandItem().is(ModItems.WHISTLE.get());
        if (!holdingRelevantItem) return;

        LivingEntity target = getLookedAtLivingEntity(player);
        if (target == null) return;

        String diet = guessDiet(target);
        if (diet != null) {
            player.displayClientMessage(
                    Component.translatable("message.ultimatezootaming.diet_hint",
                            Component.translatable("tooltip.ultimatezootaming.kibble.diet." + diet)),
                    true);
        }
    }

    private static String guessDiet(LivingEntity target) {
        if (KibbleItem.Diet.CARNIVORE.matches(target)) return "carnivore";
        if (KibbleItem.Diet.PISCIVORE.matches(target)) return "piscivore";
        if (KibbleItem.Diet.HERBIVORE.matches(target)) return "herbivore";
        return null;
    }

    private static LivingEntity getLookedAtLivingEntity(Player player) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 reachEnd = eyePos.add(look.scale(REACH));

        HitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, reachEnd,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        double maxDist = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(eyePos) : REACH;

        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eyePos, reachEnd, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof Player),
                maxDist * maxDist);

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /**
     * Filet de securite : si le joueur a coche des mods dans ConfigModScreen SANS etre
     * connecte a un monde (ex: depuis l'ecran-titre), on renvoie sa Forced List des
     * qu'il se connecte, plutot que de compter uniquement sur le bouton "Enregistrer"
     * du GUI (qui peut ne pas re-etre ouvert a chaque session).
     */
    @SubscribeEvent
    public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        NetworkHandler.CHANNEL.sendToServer(
                new ConfigSyncC2SPacket(new ArrayList<>(ZooClientConfig.FORCED_MOD_IDS.get())));
    }
}

