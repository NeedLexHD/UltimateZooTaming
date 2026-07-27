package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Protege le personnel du zoo (soigneurs, visiteurs) et les animaux apprivoises :
 *  - ils ne prennent AUCUN degat
 *  - les mobs agressifs ne peuvent pas les prendre pour cible
 *  - les mobs ne s'attaquent pas entre eux a l'interieur du zoo
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZooProtectionHandler {

    /** Une entite protegee par le zoo ? (employe, visiteur, ou animal apprivoise) */
    private static boolean isProtected(Entity e) {
        if (e instanceof ZooKeeperEntity || e instanceof VisitorEntity) return true;
        if (e instanceof LivingEntity le) {
            return le.getCapability(CapabilityHandler.TAMING_DATA)
                    .map(d -> d.isTamed()).orElse(false);
        }
        return false;
    }

    /** Annule tout degat subi par une entite protegee. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (isProtected(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Empeche un mob de prendre pour cible une entite protegee. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && isProtected(target)) {
            event.setCanceled(true);
        }
    }
}
