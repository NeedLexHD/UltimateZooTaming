package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.ai.ZooAttractionGoal;
import com.lex3d.ultimatezootaming.ai.ZooFollowGoal;
import com.lex3d.ultimatezootaming.ai.ZooGuardGoal;
import com.lex3d.ultimatezootaming.ai.ZooSitGoal;
import com.lex3d.ultimatezootaming.ai.ZooZoneGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * On ne peut pas modifier les classes des mobs d'autres mods (Alex's Mobs etc), donc
 * on injecte nos Goals depuis l'exterieur a la jointure du monde. Aucun des Goals
 * ne fait quoi que ce soit tant que sa condition n'est pas remplie (voir canUse() de
 * chacun), donc les injecter systematiquement sur TOUT PathfinderMob est sans risque
 * de perf (le Goal system de Minecraft ne "tick" pas les goals dont canUse() est false).
 *
 * Priorite 0 pour Sit (la plus dominante) ; priorite 2 pour Follow et Guard (mutuellement
 * exclusifs par construction) ; priorite 3 pour Attraction (mobs sauvages uniquement,
 * cf canUse() qui exclut les mobs deja tames).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class GoalInjectionEventHandler {

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof PathfinderMob mob) {
            // Le Soigneur est un PNJ de service : pas de goals de familier ni d'attraction
            if (mob instanceof com.lex3d.ultimatezootaming.entities.ZooKeeperEntity) return;
            mob.goalSelector.addGoal(0, new ZooSitGoal(mob));
            mob.goalSelector.addGoal(2, new ZooFollowGoal(mob));
            mob.goalSelector.addGoal(2, new ZooGuardGoal(mob));
            mob.goalSelector.addGoal(2, new ZooZoneGoal(mob));
            mob.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.ZooEatGoal(mob));
            mob.goalSelector.addGoal(3, new ZooAttractionGoal(mob));
        }
    }
}
