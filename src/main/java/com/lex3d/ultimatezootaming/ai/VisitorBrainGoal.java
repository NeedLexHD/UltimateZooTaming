package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * CERVEAU du visiteur : au lieu de laisser les goals se battre (ce qui faisait
 * tourner en rond), ce goal maitre planifie un VRAI PARCOURS de visite. A chaque
 * etape terminee, il decide de la suivante selon l'etat du visiteur :
 *   - soif haute -> va boire
 *   - faim haute -> va manger
 *   - sinon -> va voir un enclos pas encore visite
 *   - de temps en temps -> un souvenir
 *   - enclos tous vus / joie faite / temps ecoule -> il part.
 * Il MARCHE reellement d'un point a l'autre (navigation de parc), il ne
 * teleporte pas et ne sautille pas sur place.
 */
public class VisitorBrainGoal extends Goal {

    public enum Step { VIEW, DRINK, EAT, SOUVENIR, ACTIVITY, REST, LEAVE }

    private final VisitorEntity visitor;
    private Step step = Step.VIEW;
    private BlockPos target;
    private int stepTimer;
    private int stuckTimer;
    private final List<java.util.UUID> visitedZones = new ArrayList<>();
    private int enclosuresSeen = 0;
    private int minEnclosures = 3;
    private net.minecraft.core.BlockPos lastEnclosureCenter;
    private final java.util.Set<Long> usedStations = new java.util.HashSet<>();
    private int lastViewedWelfare = 60; // bien-etre estime du dernier enclos regarde
    private boolean viewedSpecial = false; // vedette ou bebe dans l'enclos regarde
    private int repathCd;

    public VisitorBrainGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Le visiteur ne fait sa visite QU'APRES avoir son ticket (guichet passe).
        // S'il n'y a pas de guichets, hasTicket est mis a true au spawn (cf handler).
        return !visitor.isLeaving() && visitor.hasTicket()
                && visitor.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return !visitor.isLeaving();
    }

    @Override
    public void start() {
        // Il visite au moins 3 enclos, ou tous s'il y en a peu — mais pas plus de 6
        // (sinon une visite s'eternise dans un grand parc).
        if (visitor.level() instanceof ServerLevel level) {
            int total = 0;
            for (ZooZone z : ZooSavedData.get(level).getAllZones()) if (z.isAnimalZone()) total++;
            minEnclosures = Math.max(2, Math.min(6, total));
        }
        planNextStep();
    }

    @Override
    public void tick() {
        if (!(visitor.level() instanceof ServerLevel level)) return;
        stepTimer++;

        // EMOTES au-dessus de la tete : rend l'etat interne visible (toutes les ~2s)
        if (stepTimer % 40 == 0) {
            emitEmote(level);
        }

        if (target == null) { planNextStep(); return; }

        // FILE D'ATTENTE pour les etapes de service (boire/manger/souvenir/activite) :
        // le visiteur ne fonce pas sur la case exacte, il s'arrete DERRIERE les autres
        // visiteurs deja proches de la cible. Ca forme une file au lieu d'un tas.
        boolean serviceStep = step == Step.DRINK || step == Step.EAT
                || step == Step.SOUVENIR || step == Step.ACTIVITY;
        double arriveDist = 4.0;
        if (serviceStep && visitor.level() instanceof ServerLevel lvl) {
            int ahead = queueAhead(lvl);
            // chaque personne devant ajoute ~1.3 bloc de recul dans la file
            arriveDist = Math.max(4.0, Math.pow(1.2 + ahead * 1.3, 2));
        }

        // Regarde la cible, marche vers elle
        visitor.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        double dist = visitor.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (dist > arriveDist) {
            // Deja proche de la cible mais recule par la file d'attente ? On PATIENTE
            // (la place se liberera), on n'abandonne pas. "Proche" = a moins de 6 blocs.
            boolean queuing = serviceStep && dist < 36.0;
            // en route : re-path si bloque
            if (visitor.getNavigation().isDone() && --repathCd <= 0) {
                visitor.navigateVia(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                repathCd = queuing ? 20 : 30;
            }
            // anti-blocage : si immobile trop longtemps, on passe a autre chose
            // (mais on est plus patient en file d'attente)
            if (visitor.getNavigation().isDone()) {
                int limit = queuing ? 300 : 100; // ~15s en file, ~5s sinon
                if (++stuckTimer > limit) { stuckTimer = 0; planNextStep(); }
            } else {
                stuckTimer = 0;
            }
            return;
        }

        // Arrive a destination : resoudre l'etape
        visitor.getNavigation().stop();
        resolveStep(level);
    }

    /** Execute l'effet de l'etape en cours puis planifie la suivante. */
    private void resolveStep(ServerLevel level) {
        switch (step) {
            case VIEW -> {
                // Temps d'observation VARIABLE selon le rythme du visiteur.
                int base = switch (visitor.getPace()) {
                    case 0 -> 50; case 2 -> 180; default -> 100;
                };
                // Vedette ou bebe present : le visiteur s'extasie, reste bien plus
                // longtemps et emet des emotes speciales.
                int watchTime = base + (lastViewedWelfare >= 70 ? 60 : 0)
                        + (lastViewedWelfare < 0 ? -30 : 0)
                        + (viewedSpecial ? 80 : 0) // +4s devant une vedette/bebe
                        // JUMELLES : on prend le temps de bien regarder (+2 s)
                        + (visitor.getHeldItem() == com.lex3d.ultimatezootaming.entities
                                .VisitorEntity.HELD_BINOCULARS ? 40 : 0);
                if (viewedSpecial && stepTimer % 30 == 10) {
                    // paillettes + coeur d'emerveillement
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            visitor.getX(), visitor.getEyeY() + 0.8, visitor.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                            visitor.getX(), visitor.getEyeY() + 0.9, visitor.getZ(), 1, 0.15, 0.1, 0.15, 0.0);
                }
                // PERCHE A SELFIE : a mi-contemplation, il se prend en photo
                // devant l'enclos et partage le cliche sur ZooTok.
                if (visitor.getHeldItem() == VisitorEntity.HELD_SELFIE
                        && stepTimer == Math.max(20, watchTime / 2)) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                            visitor.getX(), visitor.getEyeY() + 0.9, visitor.getZ(), 1, 0, 0, 0, 0);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                            visitor.getX(), visitor.getEyeY() + 1.1, visitor.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                    level.playSound(null, visitor.blockPosition(),
                            net.minecraft.sounds.SoundEvents.SPYGLASS_USE,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.7f);
                    visitor.addJoy(8);
                    // Un selfie devant un bel enclos fait un bon post
                    if (lastViewedWelfare >= 50) {
                        com.lex3d.ultimatezootaming.social.PostGenerator.postFrom(level, visitor);
                    }
                }
                if (stepTimer > Math.max(30, watchTime)) {
                    boolean happy = lastViewedWelfare >= 50;
                    visitor.addJoy(viewedSpecial ? 18 : happy ? 10 : 2); // grosse joie si vedette/bebe
                    String key = viewedSpecial ? "star_animal" : happy ? "happy_animals" : "sad_animals";
                    VisitorOpinion.say(level, visitor, key);
                    planNextStep();
                }
            }
            case DRINK -> {
                visitor.satisfyThirst(); visitor.addJoy(3);
                visitor.setHeldItem(VisitorEntity.HELD_SODA); // repart avec un soda
                planNextStep();
            }
            case EAT -> {
                visitor.satisfyHunger(); visitor.addJoy(3);
                // popcorn, glace ou barbe a papa au hasard
                int[] snacks = {VisitorEntity.HELD_POPCORN, VisitorEntity.HELD_ICECREAM, VisitorEntity.HELD_COTTON};
                visitor.setHeldItem(snacks[visitor.getRandom().nextInt(snacks.length)]);
                planNextStep();
            }
            case SOUVENIR -> {
                visitor.addJoy(5);
                // souvenir = un ballon colore
                visitor.setHeldItem(VisitorEntity.HELD_BALLOON);
                visitor.setBalloonColor(visitor.getRandom().nextInt(VisitorEntity.BALLOON_COLORS));
                VisitorOpinion.say(level, visitor, "learned");
                planNextStep();
            }
            case ACTIVITY -> {
                // A la borne : le visiteur regarde la borne et DECLENCHE lui-meme
                // l'effet + l'animation (flash / jet / nourrissage) pendant ~3s.
                visitor.getNavigation().stop();
                visitor.getLookControl().setLookAt(
                        target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
                // Rejoue l'effet toutes les ~1s pour que ce soit bien visible
                if (stepTimer % 20 == 5) {
                    com.lex3d.ultimatezootaming.blocks.StationEffect.play(level, target);
                    if (level.getBlockEntity(target)
                            instanceof com.lex3d.ultimatezootaming.blocks.StationBlockEntity sbe) {
                        sbe.triggerUse(); // animation GeckoLib (flash s'allume / buse tire)
                    }
                    // paiement 2 diamants a la caisse du zoo
                    com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addSales(2);
                }
                if (stepTimer > 60) {
                    visitor.addJoy(12);
                    // avis selon la borne
                    if (level.getBlockState(target).getBlock()
                            instanceof com.lex3d.ultimatezootaming.blocks.InteractionStationBlock stb) {
                        String key = switch (stb.getKind()) {
                            case PHOTO -> "photo_fun"; case FEED -> "feed_fun"; case WATER -> "water_fun";
                        };
                        VisitorOpinion.say(level, visitor, key);
                    }
                    planNextStep();
                }
            }
            case REST -> {
                // Sur le banc : immobile, recupere sa fatigue en ~5s. On evite la
                // pose SITTING vanilla (mal rendue pour cette entite GeckoLib) : le
                // visiteur reste simplement assis-immobile sur le banc.
                visitor.getNavigation().stop();
                // FLUX SOCIAL : assis sur le banc, il sort son telephone et publie.
                // Une pause sur trois seulement, pour ne pas noyer le fil.
                if (stepTimer == 50 && visitor.getRandom().nextInt(3) == 0
                        && visitor.level() instanceof ServerLevel psl) {
                    com.lex3d.ultimatezootaming.social.PostGenerator.postFrom(psl, visitor);
                    psl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            visitor.getX(), visitor.getEyeY() + 0.1, visitor.getZ(),
                            3, 0.1, 0.05, 0.1, 0.0);
                }
                if (stepTimer > 100) {
                    visitor.rest();
                    visitor.addJoy(4);
                    planNextStep();
                }
            }
            case LEAVE -> visitor.setLeaving(true);
        }
    }

    /** Combien de visiteurs sont deja plus proches de la meme cible que moi ?
     *  (= devant moi dans la file d'attente pour ce service). */
    private int queueAhead(ServerLevel level) {
        if (target == null) return 0;
        net.minecraft.world.phys.Vec3 t = new net.minecraft.world.phys.Vec3(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        double myDist = visitor.position().distanceToSqr(t);
        int ahead = 0;
        var box = visitor.getBoundingBox().inflate(8);
        for (VisitorEntity other : level.getEntitiesOfClass(VisitorEntity.class, box)) {
            if (other == visitor || other.isLeaving()) continue;
            double d = other.position().distanceToSqr(t);
            if (d < myDist && d < 36.0) ahead++; // devant moi et proche de la cible
        }
        return ahead;
    }

    /** Affiche une particule "emote" au-dessus de la tete selon l'etat/l'etape. */
    private void emitEmote(ServerLevel level) {
        net.minecraft.core.particles.ParticleOptions p = null;
        double y = visitor.getEyeY() + 0.8;
        if (visitor.getThirst() > 70 || visitor.getHunger() > 70) {
            p = net.minecraft.core.particles.ParticleTypes.SPLASH; // goutte : soif/faim
        } else if (step == Step.VIEW && lastViewedWelfare >= 60) {
            p = net.minecraft.core.particles.ParticleTypes.HEART;  // coeur : bel enclos
        } else if (step == Step.VIEW && lastViewedWelfare < 0) {
            p = net.minecraft.core.particles.ParticleTypes.SMOKE;  // ennui : enclos vide
        } else if (step == Step.ACTIVITY || step == Step.SOUVENIR || visitor.getJoy() > 60) {
            p = net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER; // content
        }
        if (p != null) {
            level.sendParticles(p, visitor.getX(), y, visitor.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** Choisit intelligemment la prochaine etape selon l'etat du visiteur. */
    private void planNextStep() {
        stepTimer = 0;
        stuckTimer = 0;
        target = null;
        if (!(visitor.level() instanceof ServerLevel level)) return;

        // Priorite 1 : besoins vitaux
        if (visitor.getFatigue() > 75) {
            BlockPos bench = com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.nearest(
                    com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.Kind.BENCH,
                    level, visitor.blockPosition(), 30);
            if (bench != null) { step = Step.REST; target = bench; return; }
        }
        if (visitor.getThirst() > 70) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.DRINK);
            if (shop != null) { step = Step.DRINK; target = shop; return; }
            VisitorOpinion.say(level, visitor, "no_drink"); // soif mais aucun stand de boissons
        }
        if (visitor.getHunger() > 70) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.MEAL);
            if (shop == null) shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.ICECREAM);
            if (shop != null) { step = Step.EAT; target = shop; return; }
            VisitorOpinion.say(level, visitor, "no_food"); // faim mais aucun stand repas
        }

        // Priorite 2 : voir un enclos pas encore visite (tournee sans demi-tour)
        BlockPos enclosure = findUnseenEnclosure(level);
        if (enclosure != null) { step = Step.VIEW; target = enclosure; return; }

        // Priorite 2b : une borne d'interaction proche (photo / nourrissage / jet d'eau)
        if (visitor.getRandom().nextBoolean()) {
            BlockPos station = findInteractionStation(level);
            if (station != null) { step = Step.ACTIVITY; target = station; return; }
        }

        // Priorite 3 : de temps en temps, un souvenir (si joie deja haute)
        if (visitor.getJoy() > 40 && visitor.getRandom().nextInt(3) == 0) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.SOUVENIR);
            if (shop != null) { step = Step.SOUVENIR; target = shop; return; }
        }

        // Pas encore assez d'enclos vus ? Il refait un TOUR (revoit les enclos) au
        // lieu de partir trop vite. Il ne s'en va que quand il a bien profite.
        if (enclosuresSeen < minEnclosures) {
            visitedZones.clear(); // reset la tournee -> il peut revoir des enclos
            BlockPos again = findUnseenEnclosure(level);
            if (again != null) { step = Step.VIEW; target = again; return; }
        }

        // Vraiment tout fait : le visiteur repart content
        step = Step.LEAVE;
        visitor.setLeaving(true);
    }

    /** Un enclos avec des animaux, pas encore visite, poste devant le bord. */
    private BlockPos findUnseenEnclosure(ServerLevel level) {
        List<ZooZone> zones = new ArrayList<>();
        for (ZooZone z : ZooSavedData.get(level).getAllZones()) {
            if (z.isAnimalZone() && !visitedZones.contains(z.getId())) zones.add(z);
        }
        if (zones.isEmpty()) { return null; } // tout vu -> gere par planNextStep
        // CHOIX PONDERE PAR LA PERSONNALITE.
        //
        // On note chaque enclos non visite : la proximite compte pour tout le
        // monde, mais chaque profil ajoute ses propres criteres. Le visiteur
        // pioche ensuite au hasard parmi les 3 meilleurs, pour rester varie.
        var perso = visitor.getPersonalityEnum();
        java.util.Map<ZooZone, Double> scores = new java.util.HashMap<>();
        boolean raining = level.isRaining() || level.isThundering();
        for (ZooZone z : zones) {
            double dist = Math.sqrt(z.boundingBox().getCenter().distanceToSqr(visitor.position()));
            double score = 100.0 - Math.min(80.0, dist); // proche = attirant
            score += profileBonus(level, z, perso);
            // METEO : sous la pluie, un poste d'observation a couvert devient
            // nettement plus attirant qu'un enclos expose.
            if (raining && isSheltered(level, z)) score += 55;
            scores.put(z, score);
        }
        zones.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        int pick = Math.min(zones.size(), 3);
        ZooZone best = zones.get(visitor.getRandom().nextInt(pick));
        if (best == null) return null;
        final ZooZone chosen = best;
        visitedZones.add(chosen.getId());
        enclosuresSeen++;
        lastEnclosureCenter = net.minecraft.core.BlockPos.containing(chosen.boundingBox().getCenter());
        // Bien-etre moyen reel de l'enclos (pour la reaction du visiteur)
        var animals = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                chosen.boundingBox(), a -> a.isAlive() && chosen.contains(a.blockPosition()));
        // Detecte une espece VEDETTE ou un BEBE (pour la reaction speciale)
        viewedSpecial = false;
        var stars = com.lex3d.ultimatezootaming.config.ZooServerConfig.STAR_SPECIES.get();
        for (var a : animals) {
            if (a.isBaby()) { viewedSpecial = true; break; }
            if (stars != null && !stars.isEmpty()) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                if (id != null && stars.contains(id.toString())) { viewedSpecial = true; break; }
            }
        }
        if (animals.isEmpty()) {
            lastViewedWelfare = -1; // enclos vide : pas d'animaux a admirer
        } else {
            int sum = 0, n = 0;
            for (var a : animals) {
                var d = a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                        .resolve().orElse(null);
                if (d != null && d.isTamed()) { sum += d.getSatisfaction(); n++; }
            }
            lastViewedWelfare = n > 0 ? sum / n : -1;
        }
        // se poster sur une case de BORD (a l'exterieur, pour admirer)
        var borders = chosen.borderColumns();
        if (borders.isEmpty()) return chosen.nearestFloorPos(visitor.blockPosition());
        BlockPos closest = null; double cd = Double.MAX_VALUE;
        for (BlockPos b : borders) {
            double d = b.distSqr(visitor.blockPosition());
            if (d < cd) { cd = d; closest = b; }
        }
        return closest;
    }

    /** Une borne d'interaction (photo/nourrissage/jet d'eau) au hasard parmi
     *  celles a proximite pas encore utilisees cette visite. */
    private BlockPos findInteractionStation(ServerLevel level) {
        // Lecture du REGISTRE des bornes : plus aucun scan de volume ici.
        // On ecarte celles deja utilisees, et celles ou la file est trop longue.
        BlockPos best = com.lex3d.ultimatezootaming.blocks.InteractionStationBlock
                .nearestStation(level, visitor.blockPosition(), 30,
                        p -> !usedStations.contains(p.asLong()) && queueLength(level, p) < 3);
        if (best == null) return null;
        usedStations.add(best.asLong());
        return best;
    }

    /** Combien de visiteurs patientent deja autour de cette borne ? */
    private int queueLength(ServerLevel level, BlockPos station) {
        return level.getEntitiesOfClass(VisitorEntity.class,
                new net.minecraft.world.phys.AABB(station).inflate(3.0),
                v -> v != visitor && !v.isLeaving()).size();
    }

    private BlockPos findShop(ServerLevel level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType type) {
        // Lecture du REGISTRE des caisses : aucun scan de volume.
        BlockPos origin = visitor.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : com.lex3d.ultimatezootaming.blocks.ShopBlock.allShops(level)) {
            if (p.distSqr(origin) > 40 * 40) continue; // hors de portee de marche
            if (level.getBlockEntity(p) instanceof com.lex3d.ultimatezootaming.blocks.ShopBlockEntity shop
                    && shop.getShopTypeEnum() == type) {
                found.add(p.immutable());
            }
        }
        if (found.isEmpty()) return null;
        // Aleatoire parmi les 3 plus proches de ce type (varie sans traverser tout le parc)
        found.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        int pick = Math.min(found.size(), 3);
        return found.get(visitor.getRandom().nextInt(pick));
    }

    @Override
    public void stop() {
        target = null;
        visitor.getNavigation().stop();
    }

    /**
     * Ce qui attire particulierement ce profil de visiteur vers un enclos.
     *
     * Les criteres sont lus une seule fois au moment de choisir la prochaine
     * etape, pas a chaque tick : le cout reste marginal.
     */
    private double profileBonus(ServerLevel level, ZooZone zone,
                                com.lex3d.ultimatezootaming.entities.VisitorPersonality perso) {
        return switch (perso) {
            // Le photographe cherche le plus beau : l'ambiance prime sur le reste
            case PHOTOGRAPHER -> com.lex3d.ultimatezootaming.welfare.AmbianceScore.of(level, zone) * 8.0;

            // La famille veut des bebes a montrer aux enfants, et des bornes a essayer
            case FAMILY -> {
                double b = 0;
                if (hasBaby(level, zone)) b += 45;
                if (hasStationNear(level, zone)) b += 25;
                yield b;
            }

            // L'enfant seul est surtout attire par les bornes interactives
            case LONE_CHILD -> hasStationNear(level, zone) ? 40 : 0;

            // L'ornithologue ne s'interesse vraiment qu'aux oiseaux
            case BIRD_FAN -> hasBird(level, zone) ? 60 : -15;

            // La celebrite va vers les especes rares, ca fait de meilleures photos
            case CELEBRITY -> hasRare(level, zone) ? 50 : 0;

            default -> 0;
        };
    }

    private boolean hasBaby(ServerLevel level, ZooZone zone) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(),
                a -> a.isBaby() && zone.contains(a.blockPosition())).isEmpty();
    }

    private boolean hasBird(ServerLevel level, ZooZone zone) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
            // Un oiseau vole ou appartient a une famille connue
            if (a instanceof net.minecraft.world.entity.animal.FlyingAnimal) return true;
            var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
            if (id == null) continue;
            String p = id.getPath();
            if (p.contains("parrot") || p.contains("chicken") || p.contains("bird")
                    || p.contains("owl") || p.contains("penguin") || p.contains("duck")
                    || p.contains("flamingo") || p.contains("eagle")) return true;
        }
        return false;
    }

    private boolean hasRare(ServerLevel level, ZooZone zone) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
            var d = a.getCapability(
                    com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                    .resolve().orElse(null);
            if (d != null && d.getRarity() > 0) return true;
        }
        return false;
    }

    /**
     * Une borne d'interaction a proximite immediate de l'enclos ?
     * Scan PLAT et espace (un bloc sur deux, 3 niveaux) : on veut une reponse
     * approximative pas chere, pas un inventaire exhaustif.
     */
    private boolean hasStationNear(ServerLevel level, ZooZone zone) {
        var center = net.minecraft.core.BlockPos.containing(zone.boundingBox().getCenter());
        for (int dx = -14; dx <= 14; dx += 2) {
            for (int dz = -14; dz <= 14; dz += 2) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (level.getBlockState(center.offset(dx, dy, dz)).getBlock()
                            instanceof com.lex3d.ultimatezootaming.blocks.InteractionStationBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Le poste d'observation de cet enclos est-il a l'abri ?
     * On teste le bord le plus proche : s'il y a un toit au-dessus, le visiteur
     * pourra regarder sans se mouiller.
     */
    private boolean isSheltered(ServerLevel level, ZooZone zone) {
        var borders = zone.borderColumns();
        if (borders.isEmpty()) return false;
        // On echantillonne quelques bords seulement : reponse approximative,
        // cout constant meme sur un tres grand enclos.
        int checked = 0;
        for (BlockPos b : borders) {
            if (!level.canSeeSky(b.above())) return true;
            if (++checked >= 8) break;
        }
        return false;
    }
}
