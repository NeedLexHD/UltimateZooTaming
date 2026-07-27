package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.welfare.ZooScore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rythme du zoo : ouverture au lever du jour, fermeture a la nuit avec BILAN
 * de la journee (billets + ventes - salaires) et annonce des jalons franchis.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZooDayHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 100 != 0) return;

        ServerLevel level = event.getServer().overworld();
        ZooLedger ledger = ZooLedger.get(level);
        ledger.tickHype(100);
        boolean isDay = level.isDay();

        if (isDay && !ledger.isOpen() && !ledger.isPaused()) {
            // OUVERTURE (sauf si le zoo est en pause manuelle)
            ledger.setOpen(true);
            ledger.newDay();
            // PROMOTION DU ZOO : verifier si un nouveau rang a ete atteint
            checkPromotion(event.getServer(), level, ledger);
            // FLUX SOCIAL : les posts recents accumulent des likes
            ledger.getFeed().tickLikes(ledger.getDay());

            // CONTRATS INTERNATIONAUX : vieillissement puis nouvelle proposition
            if (ledger.tickContract()) {
                broadcast(event.getServer(), Component.literal("\u2709 ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.translatable("message.ultimatezootaming.contract_expired")
                                .withStyle(ChatFormatting.GRAY)));
            }
            if (ledger.getContract() == null && level.random.nextInt(3) == 0) {
                var species = listOwnedSpecies(level);
                if (!species.isEmpty()) {
                    var offer = com.lex3d.ultimatezootaming.contracts.ZooContract.roll(
                            species, new java.util.Random(ledger.getDay() * 31L),
                            ledger.getHighestRank());
                    if (offer.isActive()) {
                        ledger.setContract(offer);
                        var sp = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                                .getValue(net.minecraft.resources.ResourceLocation.tryParse(offer.species));
                        broadcast(event.getServer(), Component.literal("\u2709 ")
                                .withStyle(ChatFormatting.AQUA)
                                .append(Component.translatable(
                                        "message.ultimatezootaming.contract_new",
                                        offer.client,
                                        sp != null ? Component.translatable(sp.getDescriptionId())
                                                   : Component.literal(offer.species))
                                        .withStyle(ChatFormatting.WHITE)));
                    }
                }
            }

            // Missions journalieres basees sur l'ETAT (mis a jour au dbut de journee)
            int scoreToday = com.lex3d.ultimatezootaming.welfare.ZooScore.compute(level);
            ledger.setMissionProgress(
                    com.lex3d.ultimatezootaming.progression.DailyMission.SCORE_60, scoreToday);
            ledger.setMissionProgress(
                    com.lex3d.ultimatezootaming.progression.DailyMission.AMBIANCE_5,
                    computeAvgAmbiance(level));
            broadcast(event.getServer(), Component.literal("\u2600 ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable("message.ultimatezootaming.zoo_open", ledger.getDay())
                            .withStyle(ChatFormatting.GRAY)));
            // Evenement du jour : 6 types possibles (NONE plus probable)
            var rnd = new java.util.Random(ledger.getDay() * 13337L + level.getSeed());
            var dyn = com.lex3d.ultimatezootaming.events.dynamic.DynamicEvent.roll(rnd);
            int ev = dyn.ordinal();
            ledger.setDailyEvent(ev);
            // Appliquer les effets immediats (arrivees speciales, primes, etc.)
            applyEventStart(event.getServer(), level, dyn, ledger);
            if (ev > 0) {
                broadcast(event.getServer(), Component.literal("\u2600 ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("event.ultimatezootaming." + dyn.key)
                                .withStyle(ChatFormatting.YELLOW)));
            }
        } else if (!isDay && ledger.isOpen()) {
            // FERMETURE : bilan + depart des visiteurs
            ledger.setOpen(false);
            // ANCIENNETE : une journee travaillee rapporte de l'XP a tout le monde,
            // y compris aux metiers sans tache comptabilisee (le garde notamment).
            // Un employe en greve ne gagne rien : il n'a pas travaille.
            for (var k : level.getEntitiesOfClass(
                    com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                    new net.minecraft.world.phys.AABB(-30000000, -64, -30000000,
                            30000000, 320, 30000000))) {
                if (!k.isOnStrike()) k.addXp(8);
            }

            // PAIE DU JOUR : preleve les salaires maintenant, pour que le bilan
            // quotidien affiche le vrai montant (avant, un timer serveur separe
            // desynchronise faisait afficher "Salaires : -0").
            com.lex3d.ultimatezootaming.events.SalaryHandler.payAllKeepers(level);
            for (VisitorEntity v : level.getEntitiesOfClass(VisitorEntity.class,
                    new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
                v.setLeaving(true);
            }
            int profit = ledger.getTickets() + ledger.getSales() - ledger.getSalaries();
            ledger.pushProfit(profit);
            // Inspection sanitaire : zero malade a la fermeture = prime de 60
            if (ledger.getDailyEvent() == 2) {
                var detail = ZooScore.detailed(level);
                var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(level);
                if (detail.sick() == 0 && detail.animals() > 0 && vault != null) {
                    vault.deposit(60);
                    broadcast(event.getServer(), Component.literal("\u2695 ").withStyle(ChatFormatting.GREEN)
                            .append(Component.translatable("event.ultimatezootaming.inspection_ok")
                                    .withStyle(ChatFormatting.GREEN)));
                } else {
                    broadcast(event.getServer(), Component.literal("\u2695 ").withStyle(ChatFormatting.RED)
                            .append(Component.translatable("event.ultimatezootaming.inspection_fail",
                                    detail.sick()).withStyle(ChatFormatting.RED)));
                }
            }
            // Journaliste : article = prime selon la note du zoo a la fermeture
            if (ledger.getDailyEvent() == com.lex3d.ultimatezootaming.events.dynamic.DynamicEvent.JOURNALIST.ordinal()) {
                int score = ZooScore.compute(level);
                var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(level);
                if (score >= 60 && vault != null) {
                    int prime = 40 + score;
                    vault.deposit(prime);
                    broadcast(event.getServer(), Component.literal("\u270D ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.translatable("event.ultimatezootaming.journalist_good", prime)
                                    .withStyle(ChatFormatting.YELLOW)));
                } else {
                    broadcast(event.getServer(), Component.literal("\u270D ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.translatable("event.ultimatezootaming.journalist_bad")
                                    .withStyle(ChatFormatting.RED)));
                }
            }
            ledger.setDailyEvent(0);
            broadcast(event.getServer(), Component.literal("\u263D ")
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.translatable("message.ultimatezootaming.zoo_close", ledger.getDay())
                            .withStyle(ChatFormatting.GRAY)));
            broadcast(event.getServer(), Component.translatable(
                    "message.ultimatezootaming.day_report",
                    ledger.getVisitorsToday(), ledger.getTickets(), ledger.getSales(), ledger.getSalaries())
                    .withStyle(ChatFormatting.GRAY));
            broadcast(event.getServer(), Component.translatable(
                    "message.ultimatezootaming.day_profit", profit)
                    .withStyle(profit >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            checkMilestones(event.getServer(), level, ledger);
        }
    }

    /** Les 10 OBJECTIFS du zoo : {id, cible, recompense}. La progression est
     *  [especes, especes, animaux, note, note, note, note, visiteurs cumules,
     *   emeraudes cumulees, employes]. */
    public static final int[][] GOALS = {
            {0, 5, 50},    // 5 especes
            {1, 10, 100},  // 10 especes
            {2, 20, 100},  // 20 animaux
            {3, 25, 30},   // note 25
            {4, 50, 80},   // note 50
            {5, 70, 150},  // note 70
            {6, 90, 300},  // note 90
            {7, 100, 120}, // 100 visiteurs cumules
            {8, 1000, 200},// 1000 emeraudes gagnees
            {9, 5, 80},    // 5 employes
    };

    /** La progression actuelle de chaque objectif (meme ordre que GOALS). */
    public static int[] goalProgress(ServerLevel level, ZooLedger ledger) {
        var detail = ZooScore.detailed(level);
        int keepers = level.getEntitiesOfClass(
                com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000)).size();
        return new int[]{
                detail.species(), detail.species(), detail.animals(),
                detail.score(), detail.score(), detail.score(), detail.score(),
                (int) Math.min(Integer.MAX_VALUE, ledger.getTotalVisitors()),
                (int) Math.min(Integer.MAX_VALUE, ledger.getTotalEarned()),
                keepers};
    }

    /** Objectifs atteints : annonce + RECOMPENSE deposee dans la Caisse du Zoo. */
    private static void checkMilestones(net.minecraft.server.MinecraftServer server,
                                        ServerLevel level, ZooLedger ledger) {
        int[] progress = goalProgress(level, ledger);
        for (int[] goal : GOALS) {
            int id = goal[0];
            if (progress[id] < goal[1]) continue;
            // Il faut une Caisse pour toucher la prime (sinon retente demain)
            var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(level);
            if (vault == null) return;
            if (!ledger.reachMilestone(id)) continue;
            vault.deposit(goal[2]);
            broadcast(server, Component.literal("\u2605 ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("goal.ultimatezootaming." + id)
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("message.ultimatezootaming.goal_reward", goal[2])
                            .withStyle(ChatFormatting.GREEN)));
        }
    }


    /** Verifie si le zoo a monte de rang depuis le dernier jour. Verse une PRIME
     *  a chaque nouveau rang atteint (une seule fois par rang, historiquement). */
    private static void checkPromotion(net.minecraft.server.MinecraftServer server,
                                        ServerLevel level, ZooLedger ledger) {
        int score = com.lex3d.ultimatezootaming.welfare.ZooScore.compute(level);
        int species = countGlobalSpecies(level);
        com.lex3d.ultimatezootaming.progression.ZooRank rank =
                com.lex3d.ultimatezootaming.progression.ZooRank.compute(
                        (int) Math.min(Integer.MAX_VALUE, ledger.getTotalVisitors()), species, score);
        if (rank.ordinal() > ledger.getHighestRank()) {
            // Nouveau rang atteint !
            int previousRank = ledger.getHighestRank();
            ledger.setHighestRank(rank.ordinal());
            // Verse la prime a la Tresorerie
            var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(level);
            if (vault != null && rank.promotionBonus > 0) {
                vault.deposit(rank.promotionBonus);
            }
            // Annonce (chaque rang gagne intermediaire aussi si on a saute plusieurs)
            for (int r = previousRank + 1; r <= rank.ordinal(); r++) {
                var reached = com.lex3d.ultimatezootaming.progression.ZooRank.values()[r];
                broadcast(server, Component.literal("\u2605 ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("message.ultimatezootaming.rank_promotion",
                                Component.translatable("rank.ultimatezootaming." + reached.key))
                                .withStyle(ChatFormatting.YELLOW)));
                if (reached.promotionBonus > 0) {
                    broadcast(server, Component.literal("+" + reached.promotionBonus + " Ƶ ")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }

    /** Effets DECLENCHES au debut du jour selon l'evenement. */
    private static void applyEventStart(net.minecraft.server.MinecraftServer server,
                                         ServerLevel level,
                                         com.lex3d.ultimatezootaming.events.dynamic.DynamicEvent ev,
                                         ZooLedger ledger) {
        switch (ev) {
            case SCHOOL_TRIP -> {
                // Sortie scolaire realiste : 2-5 accompagnateurs adultes qui
                // encadrent 10-25 enfants. Les enfants suivent, les adultes
                // se comportent comme des visiteurs normaux.
                var entrance = findEntrance(level);
                if (entrance != null) {
                    int adults = 2 + level.random.nextInt(4);   // 2 a 5
                    int kids = 10 + level.random.nextInt(16);   // 10 a 25
                    // Les accompagnateurs d'abord (ils servent de chefs de groupe)
                    java.util.List<com.lex3d.ultimatezootaming.entities.VisitorEntity> chaperones =
                            new java.util.ArrayList<>();
                    for (int i = 0; i < adults; i++) {
                        var adult = com.lex3d.ultimatezootaming.core.init.ModEntities.VISITOR.get()
                                .create(level);
                        if (adult == null) continue;
                        adult.moveTo(entrance.getX() + 0.5 + level.random.nextDouble() * 3 - 1.5,
                                entrance.getY(),
                                entrance.getZ() + 0.5 + level.random.nextDouble() * 3 - 1.5,
                                level.random.nextFloat() * 360, 0);
                        adult.setTicket(true); // le groupe entre en bloc
                        level.addFreshEntity(adult);
                        chaperones.add(adult);
                    }
                    // Puis les enfants, repartis entre les accompagnateurs
                    for (int i = 0; i < kids; i++) {
                        var kid = com.lex3d.ultimatezootaming.core.init.ModEntities.VISITOR.get()
                                .create(level);
                        if (kid == null) continue;
                        kid.moveTo(entrance.getX() + 0.5 + level.random.nextDouble() * 5 - 2.5,
                                entrance.getY(),
                                entrance.getZ() + 0.5 + level.random.nextDouble() * 5 - 2.5,
                                level.random.nextFloat() * 360, 0);
                        kid.setChild(true);
                        kid.setTicket(true);
                        // Chaque enfant suit un accompagnateur (groupes equilibres)
                        if (!chaperones.isEmpty()) {
                            var chief = chaperones.get(i % chaperones.size());
                            kid.setGroupLeader(chief.getUUID());
                        }
                        level.addFreshEntity(kid);
                    }
                }
            }
            case CELEBRITY -> {
                // Fait spawn UN VIP celebre qui reclame une espece rare
                var entrance = findEntrance(level);
                if (entrance != null) {
                    var vip = com.lex3d.ultimatezootaming.core.init.ModEntities.VISITOR.get()
                            .create(level);
                    if (vip != null) {
                        vip.moveTo(entrance.getX() + 0.5, entrance.getY(),
                                entrance.getZ() + 0.5, 0, 0);
                        // La celebrite reclame la 1re espece qu'on trouve dans le zoo
                        String wish = pickRareSpecies(level);
                        if (!wish.isEmpty()) vip.makeVip(wish);
                        level.addFreshEntity(vip);
                    }
                }
            }
            case JOURNALIST -> {
                // Le journaliste boostera la fin de journee : rien a faire au start
            }
            case PROTEST -> {
                // Manifestation : quelques visiteurs "manifestants" pres de l'entree
                // (ils ne rentrent pas, ils tournent en rond)
                var entrance = findEntrance(level);
                if (entrance != null) {
                    int n = 2 + level.random.nextInt(3);
                    for (int i = 0; i < n; i++) {
                        var prot = com.lex3d.ultimatezootaming.core.init.ModEntities.VISITOR.get()
                                .create(level);
                        if (prot != null) {
                            var offset = entrance.offset(
                                    level.random.nextInt(6) - 3, 0, level.random.nextInt(6) - 3);
                            prot.moveTo(offset.getX() + 0.5, offset.getY(),
                                    offset.getZ() + 0.5, 0, 0);
                            prot.setLeaving(true); // pour qu'ils s'en aillent apres un temps
                            level.addFreshEntity(prot);
                        }
                    }
                }
            }
            default -> {}
        }
    }

    /** Trouve l'entree du zoo (le premier ZooEntranceBlock rencontre). */
    private static net.minecraft.core.BlockPos findEntrance(ServerLevel level) {
        // On cherche parmi les zones enregistrees
        for (var zone : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            var box = zone.boundingBox();
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                    (int) box.minX, (int) box.minY, (int) box.minZ,
                    (int) box.maxX, (int) box.maxY, (int) box.maxZ)) {
                if (level.getBlockState(pos).getBlock()
                        instanceof com.lex3d.ultimatezootaming.blocks.ZooEntranceBlock) {
                    return pos.immutable();
                }
            }
            break; // une seule zone suffit
        }
        return null;
    }

    /** Choisit une espece "rare" (peu representee) du zoo, ou l'une des tendances. */
    private static String pickRareSpecies(ServerLevel level) {
        var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level);
        // Prend la 1re espece de la demande VIP existante si dispo
        var demands = ledger.getTopDemandedSpecies(3);
        if (!demands.isEmpty()) return demands.get(0);
        // Sinon prend une espece au hasard parmi celles du zoo
        java.util.List<String> species = new java.util.ArrayList<>();
        for (var zone : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue;
            for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                    zone.boundingBox(), animal -> zone.contains(animal.blockPosition()))) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                if (id != null) species.add(id.toString());
            }
        }
        return species.isEmpty() ? "" : species.get(level.random.nextInt(species.size()));
    }

    private static int computeAvgAmbiance(ServerLevel level) {
        return (int) com.lex3d.ultimatezootaming.welfare.AmbianceScore.zooAverage(level);
    }

    /** Compte les especes distinctes dans le zoo (pour le rang). */
    private static int countGlobalSpecies(ServerLevel level) {
        java.util.Set<net.minecraft.world.entity.EntityType<?>> types = new java.util.HashSet<>();
        for (var zone : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue;
            for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                    zone.boundingBox(), animal -> zone.contains(animal.blockPosition()))) {
                types.add(a.getType());
            }
        }
        return types.size();
    }

    private static void broadcast(net.minecraft.server.MinecraftServer server, Component msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(msg);
    }

    /** Les especes reellement presentes dans le zoo, pour ne proposer que du realisable. */
    private static java.util.List<String> listOwnedSpecies(ServerLevel level) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (var zone : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue;
            for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                    zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                if (id != null) set.add(id.toString());
            }
        }
        return new java.util.ArrayList<>(set);
    }
}
