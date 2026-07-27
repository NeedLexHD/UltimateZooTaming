package com.lex3d.ultimatezootaming.entities;

import com.lex3d.ultimatezootaming.ai.KeeperCareGoal;
import com.lex3d.ultimatezootaming.ai.KeeperRefillGoal;
import com.lex3d.ultimatezootaming.ai.KeeperStrollGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Le Soigneur : PNJ humanoide anime (GeckoLib) qu'on assigne a un enclos avec le
 * Baton d'arpenteur. Il y patrouille (KeeperCareGoal), remonte le bien-etre des
 * animaux qu'il croise et guerit les malades. Look premium (modele custom).
 */
public class ZooKeeperEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> WORKING =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.INT);
    /** Action en cours : 0 = aucune, 1 = soin, 2 = remplissage de mangeoire. */
    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.INT);
    /** Metier : 0 = Polyvalent, 1 = Veterinaire, 2 = Nourrisseur, 3 = Gardien. */
    private static final EntityDataAccessor<Integer> JOB =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.INT);
    /** Impaye : l'employe se met en greve. */
    private static final EntityDataAccessor<Boolean> STRIKE =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.BOOLEAN);

    /** Tache en cours, purement informative (affichee au-dessus de la tete). */
    private static final EntityDataAccessor<Integer> TASK =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.INT);

    public KeeperTask getTask() { return KeeperTask.byOrdinal(this.entityData.get(TASK)); }

    /** Declare ce que fait l'employe. Appele par les Goals. */
    public void setTask(KeeperTask task) {
        if (this.entityData.get(TASK) != task.ordinal()) {
            this.entityData.set(TASK, task.ordinal());
        }
    }

    /** XP total accumule (progresse en travaillant). */
    private static final EntityDataAccessor<Integer> XP =
            SynchedEntityData.defineId(ZooKeeperEntity.class, EntityDataSerializers.INT);

    /** Niveau 1-20 (calcule depuis XP : level = min(20, 1 + xp/50)). */
    public int getKeeperLevel() { return Math.min(20, 1 + getXp() / 50); }
    public int getXp() { return this.entityData.get(XP); }
    public void setXp(int xp) { this.entityData.set(XP, Math.max(0, xp)); }
    /** Ajoute de l'XP quand l'employe travaille (soigne, nourrit, vend, garde...). */
    public void addXp(int amount) {
        int oldLevel = getKeeperLevel();
        setXp(getXp() + amount);
        int newLevel = getKeeperLevel();
        if (newLevel > oldLevel && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            // Effet visuel + son de level up
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 1.5, getZ(), 15, 0.4, 0.5, 0.4, 0.05);
            sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.5f);
            // Annonce au chat
            for (var p : sl.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2605 ")
                        .withStyle(net.minecraft.ChatFormatting.GOLD)
                        .append(net.minecraft.network.chat.Component.translatable(
                                "message.ultimatezootaming.keeper_levelup",
                                getName(), newLevel)
                                .withStyle(net.minecraft.ChatFormatting.YELLOW)));
            }
        }
    }

    /**
     * L'outil que l'employe tient en main, selon son metier et ce qu'il fait.
     * Il ne sort son materiel QUE lorsqu'il travaille (setWorking) ou qu'il est
     * en deplacement vers une tache : au repos, il a les mains vides.
     */
    public net.minecraft.world.item.ItemStack getToolStack() {
        if (isOnStrike() || isSleeping()) return net.minecraft.world.item.ItemStack.EMPTY;
        return switch (getJob()) {
            // Veterinaire : sa fiole de remede
            case 1 -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.ANIMAL_REMEDY.get());
            // Nourrisseur : un seau de fourrage
            case 2 -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.FODDER.get());
            // Garde : son sifflet
            case 3 -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.WHISTLE.get());
            // Vendeur : la caisse a billets
            case 4 -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.PARK_TICKET.get());
            // Agent d'entretien : son sac de dechets
            case 5 -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.RECYCLABLE_WASTE.get());
            // Polyvalent : rien de specifique
            default -> net.minecraft.world.item.ItemStack.EMPTY;
        };
    }

    // ---------------- COMPETENCES ----------------

    /** Rang investi dans chaque competence (index = ordinal de KeeperSkill). */
    private final int[] skillRanks =
            new int[com.lex3d.ultimatezootaming.progression.KeeperSkill.values().length];

    public int[] getSkillRanks() { return skillRanks; }

    public int getSkillRank(com.lex3d.ultimatezootaming.progression.KeeperSkill s) {
        return skillRanks[s.ordinal()];
    }

    /** Points deja depenses, toutes competences confondues. */
    public int getSpentPoints() {
        int n = 0;
        for (int r : skillRanks) n += r;
        return n;
    }

    /** Points de competence encore disponibles. */
    public int getFreePoints() {
        return com.lex3d.ultimatezootaming.progression.KeeperSkill
                .pointsForLevel(getKeeperLevel()) - getSpentPoints();
    }

    /**
     * Investit un point dans une competence.
     * @return true si l'investissement a eu lieu
     */
    public boolean upgradeSkill(com.lex3d.ultimatezootaming.progression.KeeperSkill s) {
        if (getFreePoints() <= 0) return false;
        if (skillRanks[s.ordinal()] >= s.maxRank) return false;
        skillRanks[s.ordinal()]++;
        return true;
    }

    /** Reinitialise toutes les competences (les points redeviennent libres). */
    public void resetSkills() {
        java.util.Arrays.fill(skillRanks, 0);
    }

    /** Bonus de vitesse selon le niveau : +5% par niveau au-dessus de 1, max +100% au niveau 20. */
    public double getSpeedBonus() {
        double base = 1.0 + (getKeeperLevel() - 1) * 0.05;
        // Celerite : +8% par rang
        return base + 0.08 * getSkillRank(
                com.lex3d.ultimatezootaming.progression.KeeperSkill.SPEED);
    }

    /** Rayon d'action bonus : le veterinaire soigne plus loin, le nourrisseur remplit plus de mangeoires. */
    public int getRangeBonus() {
        // +1 rayon tous les 4 niveaux, +2 par rang de Portee
        return getKeeperLevel() / 4 + 2 * getSkillRank(
                com.lex3d.ultimatezootaming.progression.KeeperSkill.REACH);
    }

    /** Multiplicateur de temps d'attente : -20% par rang d'Efficacite (min 40%). */
    public double getCooldownFactor() {
        return Math.max(0.4, 1.0 - 0.2 * getSkillRank(
                com.lex3d.ultimatezootaming.progression.KeeperSkill.EFFICIENCY));
    }

    /** Jours d'impaye toleres avant la greve, grace au Devouement. */
    public int getStrikeTolerance() {
        return getSkillRank(com.lex3d.ultimatezootaming.progression.KeeperSkill.DEDICATION);
    }

    /** Bonus de maitrise du metier (0 a 3), a interpreter selon le job. */
    public int getMastery() {
        return getSkillRank(com.lex3d.ultimatezootaming.progression.KeeperSkill.MASTERY);
    }

    /** Nombre de skins disponibles : textures/entity/keeper/zookeeper_1.png .. zookeeper_4.png */
    /** Nombre de skins par metier. Pour ajouter des skins : depose des PNGs
     *  64x64 dans textures/entity/keeper/<metier>/skin_1.png a skin_N.png.
     *  Les dossiers metier sont : generalist, vet, feeder, guard, vendor. */
    public static final int SKIN_COUNT = 8;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.zookeeper.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.zookeeper.walk");
    private static final RawAnimation CARE = RawAnimation.begin().thenLoop("animation.zookeeper.care");
    private static final RawAnimation REFILL = RawAnimation.begin().thenLoop("animation.zookeeper.refill");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    /** Enclos dont cet employe a la charge (3 au maximum). Il alterne entre eux
     *  pendant ses rondes et intervient dans chacun. */
    private final java.util.List<UUID> assignedZones = new java.util.ArrayList<>();

    /** Nombre maximum d'enclos qu'un seul employe peut prendre en charge. */
    public static final int MAX_ZONES = 3;
    @Nullable
    private UUID ownerUUID;

    public ZooKeeperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // PATHFINDING : le soigneur EVITE l'eau autant que possible. Un malus eleve
        // (mais fini) le fait contourner les bassins quand un chemin sec existe,
        // tout en lui laissant la possibilite de traverser si c'est le seul acces.
        // Avant, le malus etait a 0 : l'eau coutait autant que la terre ferme, donc
        // ils barbotaient au lieu de faire le tour.
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 8.0f);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER_BORDER, 6.0f);
        // Dangers evidents : jamais volontairement
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.LAVA, -1.0f);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, -1.0f);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, 12.0f);
        // Barrieres et cloture : ce sont les murs des enclos, on ne cherche pas a
        // les longer inutilement
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.FENCE, -1.0f);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason,
            @Nullable net.minecraft.world.entity.SpawnGroupData spawnData,
            @Nullable net.minecraft.nbt.CompoundTag dataTag) {
        // Chaque soigneur tire un skin au hasard a l'apparition, garde a vie
        this.setSkin(this.random.nextInt(SKIN_COUNT));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    private static final String[] MALE_NAMES = {
            "Lucas", "Hugo", "Nathan", "Louis", "Jules", "Arthur", "Gabriel",
            "Raphael", "Theo", "Paul", "Antoine", "Maxime", "Nicolas", "Thomas", "Adam"
    };
    private static final String[] FEMALE_NAMES = {
            "Emma", "Lea", "Chloe", "Manon", "Camille", "Zoe", "Ines",
            "Jade", "Louise", "Alice", "Juliette", "Clara", "Eva", "Nina", "Rose"
    };
    /** Skins feminins (index 0-based) : zookeeper_2 et zookeeper_4. */
    private static final java.util.Set<Integer> FEMALE_SKINS = java.util.Set.of(1, 3);

    private boolean skinRolled = false;

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSkin(int skin) {
        this.entityData.set(SKIN, Math.floorMod(skin, SKIN_COUNT));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.4) // 0.6 + 1.4 = grimpe 2 blocs
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this)); // flotte, ne se noie pas
        // Gardien (job 3) uniquement : combat les monstres proches
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true) {
            @Override public boolean canUse() { return getJob() == 3 && super.canUse(); }
            @Override public boolean canContinueToUse() { return getJob() == 3 && super.canContinueToUse(); }
        });
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                e -> getJob() == 3 && e instanceof net.minecraft.world.entity.monster.Enemy
                        && !(e instanceof net.minecraft.world.entity.monster.Creeper)));
        this.goalSelector.addGoal(1, new KeeperRefillGoal(this)); // ravitaille la mangeoire depuis les coffres
        this.goalSelector.addGoal(1, new com.lex3d.ultimatezootaming.ai.KeeperSleepGoal(this)); // dort la nuit au dortoir
        // GREVE : passe avant tout travail. Un impaye ne fait plus rien d'autre
        // que rejoindre le piquet devant la Tresorerie.
        this.goalSelector.addGoal(1, new com.lex3d.ultimatezootaming.ai.KeeperStrikeGoal(this));
        this.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.GuardContainGoal(this)); // Gardien : contenir les evasions
        this.goalSelector.addGoal(2, new KeeperCareGoal(this)); // soin dans l'enclos
        // Regarnir la caisse AVANT de la tenir : une boutique vide ne vend rien.
        this.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.KeeperRestockGoal(this));
        // Agent d'entretien : ramasser les detritus du parc
        this.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.KeeperCleanGoal(this));
        this.goalSelector.addGoal(3, new com.lex3d.ultimatezootaming.ai.KeeperShopGoal(this)); // Vendeur : tenir la boutique
        // RONDE AVANT REPOS : un employe avec des enclos assignes patrouille en
        // permanence (inspection des animaux). Il ne va au vestiaire QUE s'il n'a
        // aucun enclos, ou si le zoo est ferme.
        // Rythme de la journee : prise de poste, pause dejeuner, fin de service.
        // Au-dessus de la ronde, en dessous des urgences (soin, evasion, boutique).
        this.goalSelector.addGoal(3, new com.lex3d.ultimatezootaming.ai.KeeperRoutineGoal(this));
        this.goalSelector.addGoal(4, new com.lex3d.ultimatezootaming.ai.KeeperPatrolGoal(this)); // ronde dans les enclos assignes
        this.goalSelector.addGoal(5, new com.lex3d.ultimatezootaming.ai.KeeperRestGoal(this)); // repos (dernier recours)
        this.goalSelector.addGoal(5, new com.lex3d.ultimatezootaming.ai.KeeperGreetGoal(this)); // salue le directeur qui passe
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WORKING, false);
        this.entityData.define(SKIN, 0);
        this.entityData.define(ACTION, 0);
        this.entityData.define(JOB, 0);
        this.entityData.define(STRIKE, false);
        this.entityData.define(XP, 0);
        this.entityData.define(TASK, 0);
    }

    public void setWorking(boolean working) {
        this.entityData.set(WORKING, working);
        // compat : "working" simple = soin
        this.entityData.set(ACTION, working ? 1 : 0);
    }

    public boolean isWorking() {
        return this.entityData.get(WORKING);
    }

    /** Definit l'action animee : 0 = aucune, 1 = soin, 2 = remplissage. */
    public void setAction(int action) {
        this.entityData.set(ACTION, action);
        this.entityData.set(WORKING, action != 0);
    }

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    /** Applique la vitesse issue du niveau et de la Celerite (verif periodique). */
    private void applySkillSpeed() {
        var attr = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        double target = 0.28 * getSpeedBonus();
        if (Math.abs(attr.getBaseValue() - target) > 0.001) attr.setBaseValue(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 40 == 0) applySkillSpeed();
        // Deux etats couvrent tout le reste, on les impose ici plutot que dans
        // chaque Goal : ils sont visibles meme quand aucun Goal ne tourne.
        if (!level().isClientSide() && tickCount % 20 == 0) {
            if (isOnStrike()) setTask(KeeperTask.STRIKE);
            else if (isSleeping()) setTask(KeeperTask.SLEEPING);
        }
        if (!level().isClientSide() && tickCount % 600 == 0
                && level() instanceof net.minecraft.server.level.ServerLevel psl) {
            pruneNonEnclosureZones(psl);
        }
        // NAGE : dans l'eau, les mobs terrestres sont tres ralentis. On compense en
        // poussant le soigneur vers sa destination pour qu'il traverse l'eau a une
        // vitesse correcte (remplissage des mangeoires aquatiques sans lambiner).
        if (!level().isClientSide() && this.isInWater() && !this.getNavigation().isDone()) {
            var path = this.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                net.minecraft.world.phys.Vec3 next = path.getNextEntityPos(this);
                net.minecraft.world.phys.Vec3 push = next.subtract(this.position());
                if (push.lengthSqr() > 0.01) {
                    push = push.normalize().scale(0.035); // poussee de nage (moderee)
                    this.setDeltaMovement(this.getDeltaMovement().add(push.x, push.y * 0.5, push.z));
                }
            }
        }
        // Filet de securite : garantit un skin aleatoire meme si finalizeSpawn
        // n'a pas ete appele (transformation, invocation par commande, etc.)
        if (!level().isClientSide() && !skinRolled) {
            skinRolled = true;
            setSkin(this.random.nextInt(SKIN_COUNT));
            // Nom francais aleatoire si pas deja nomme
            if (!hasCustomName()) {
                String[] pool = FEMALE_SKINS.contains(getSkin()) ? FEMALE_NAMES : MALE_NAMES;
                setCustomName(net.minecraft.network.chat.Component.literal(
                        pool[this.random.nextInt(pool.length)]));
                setCustomNameVisible(true);
            }
        }
    }

    /** Le PREMIER enclos assigne (null si aucun). Conserve pour toute la logique
     *  qui n'a besoin que d'un enclos de reference. */
    @Nullable
    public UUID getAssignedZone() {
        return assignedZones.isEmpty() ? null : assignedZones.get(0);
    }

    /** Tous les enclos dont il a la charge (liste non modifiable). */
    public java.util.List<UUID> getAssignedZones() {
        return java.util.Collections.unmodifiableList(assignedZones);
    }

    public boolean hasZone(UUID zoneId) {
        return zoneId != null && assignedZones.contains(zoneId);
    }

    public int getZoneCount() { return assignedZones.size(); }

    /** Remplace toute l'affectation par un seul enclos (null = plus rien). */
    public void setAssignedZone(@Nullable UUID zoneId) {
        assignedZones.clear();
        if (zoneId != null) assignedZones.add(zoneId);
    }

    /**
     * Ajoute ou retire un enclos de sa charge.
     * @return true si l'operation a eu lieu, false si le maximum est atteint.
     */
    public boolean toggleZone(UUID zoneId) {
        if (zoneId == null) return false;
        if (assignedZones.remove(zoneId)) return true;   // il l'avait -> on retire
        if (assignedZones.size() >= MAX_ZONES) return false; // deja 3 enclos
        assignedZones.add(zoneId);
        return true;
    }

    /** Retire un enclos supprime du monde de la charge de cet employe. */
    public void forgetZone(UUID zoneId) {
        assignedZones.remove(zoneId);
    }

    /**
     * Purge les zones qui ne sont pas des enclos.
     *
     * Avant le correctif, on pouvait affecter un employe a une salle de repos :
     * elle occupait une place et le compteur affichait 1/3 a tort. On nettoie
     * donc les affectations heritees, une fois de temps en temps.
     */
    public void pruneNonEnclosureZones(net.minecraft.server.level.ServerLevel level) {
        if (assignedZones.isEmpty()) return;
        var data = com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level);
        assignedZones.removeIf(id -> {
            var z = data.getZone(id);
            return z == null || !z.isAnimalZone();
        });
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    // Ne despawn jamais (c'est un PNJ pose volontairement)
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(
            net.minecraft.world.level.Level level) {
        // Navigation de parc terrestre (marche fiable + preference des allees) ;
        // canFloat=true lui permet de nager/plonger vers les mangeoires aquatiques.
        com.lex3d.ultimatezootaming.ai.ZooPathNavigation nav =
                new com.lex3d.ultimatezootaming.ai.ZooPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // le soigneur plonge pour remplir les mangeoires aquatiques
    }

    @Override
    public boolean isPushedByFluid() {
        return false; // stable dans l'eau
    }

    /** Clic droit sur le Soigneur : diagnostic de l'enclos (que faut-il pour 100% ?). */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (level().isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // DIAGNOSTIC de l'employe : explique ce qui l'empeche de travailler.
        String problem = diagnoseProblem(serverLevel);
        if (problem != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u26A0 ")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(net.minecraft.network.chat.Component.translatable(problem)
                            .withStyle(net.minecraft.ChatFormatting.YELLOW)), false);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // (Le metier se change dans le TABLEAU DE BORD, plus par sneak+clic.)
        if (getAssignedZone() == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.ultimatezootaming.keeper_no_zone_hint",
                    net.minecraft.network.chat.Component.translatable(
                            "job.ultimatezootaming." + jobKey(getJob()))), true);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        com.lex3d.ultimatezootaming.zones.ZooZone zone =
                com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(serverLevel).getZone(getAssignedZone());
        if (zone == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.ultimatezootaming.keeper_no_zone_hint",
                    net.minecraft.network.chat.Component.translatable(
                            "job.ultimatezootaming." + jobKey(getJob()))), true);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        com.lex3d.ultimatezootaming.welfare.WelfareDiagnostic.report(serverLevel, zone, player);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /** Detecte ce qui empeche l'employe de bien travailler. Retourne une cle de
     *  traduction (message) ou null si tout va bien. */
    private String diagnoseProblem(net.minecraft.server.level.ServerLevel level) {
        // 1. Impaye = greve
        if (isOnStrike()) return "keeper_problem.ultimatezootaming.strike";
        // 2. Pas d'enclos assigne (sauf nourrisseur/garde/vendeur qui bossent partout)
        int job = getJob();
        boolean needsZone = job == 0 || job == 1; // polyvalent / veterinaire
        if (needsZone && getAssignedZone() == null) {
            return "keeper_problem.ultimatezootaming.no_zone";
        }
        // 3. Vendeur : a-t-il une caisse a proximite ?
        if (job == 4) {
            boolean hasShop = false;
            net.minecraft.core.BlockPos c = blockPosition();
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                    c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
                if (level.getBlockEntity(pos)
                        instanceof com.lex3d.ultimatezootaming.blocks.ShopBlockEntity) { hasShop = true; break; }
            }
            if (!hasShop) return "keeper_problem.ultimatezootaming.no_shop";
        }
        // 4. Pas de vestiaire (casier) de son metier dans un rayon raisonnable
        if (!hasLockerNearby(level)) {
            return "keeper_problem.ultimatezootaming.no_locker";
        }
        // 5. Pas de lit pour dormir (dortoir)
        if (!hasBedNearby(level)) {
            return "keeper_problem.ultimatezootaming.no_bed";
        }
        return null; // tout va bien -> on affichera le rapport de zone
    }

    private boolean hasLockerNearby(net.minecraft.server.level.ServerLevel level) {
        // Scan LIMITE (rayon 16, hauteur ±4) pour eviter tout freeze.
        net.minecraft.core.BlockPos c = blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                c.offset(-16, -4, -16), c.offset(16, 4, 16))) {
            if (level.getBlockState(pos).getBlock()
                    instanceof com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock) return true;
        }
        return false;
    }

    private boolean hasBedNearby(net.minecraft.server.level.ServerLevel level) {
        net.minecraft.core.BlockPos c = blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                c.offset(-16, -4, -16), c.offset(16, 4, 16))) {
            if (level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BedBlock) return true;
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Multi-enclos : on ecrit la liste, plus l'ancienne cle simple pour qu'un
        // downgrade du mod ne perde pas au moins le premier enclos.
        net.minecraft.nbt.ListTag zonesTag = new net.minecraft.nbt.ListTag();
        for (UUID z : assignedZones) zonesTag.add(net.minecraft.nbt.NbtUtils.createUUID(z));
        tag.put("AssignedZones", zonesTag);
        if (!assignedZones.isEmpty()) tag.putUUID("AssignedZone", assignedZones.get(0));
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putInt("Skin", getSkin());
        tag.putBoolean("SkinRolled", skinRolled);
        tag.putInt("Job", getJob());
        tag.putBoolean("Strike", isOnStrike());
        tag.putInt("Xp", getXp());
        tag.putIntArray("Skills", skillRanks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        assignedZones.clear();
        if (tag.contains("AssignedZones")) {
            net.minecraft.nbt.ListTag zonesTag =
                    tag.getList("AssignedZones", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            for (int i = 0; i < zonesTag.size() && assignedZones.size() < MAX_ZONES; i++) {
                assignedZones.add(net.minecraft.nbt.NbtUtils.loadUUID(zonesTag.get(i)));
            }
        } else if (tag.hasUUID("AssignedZone")) {
            // Ancien save mono-enclos
            assignedZones.add(tag.getUUID("AssignedZone"));
        }
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("Skin")) this.entityData.set(SKIN, tag.getInt("Skin"));
        if (tag.contains("Job")) this.entityData.set(JOB, tag.getInt("Job"));
        if (tag.contains("Strike")) this.entityData.set(STRIKE, tag.getBoolean("Strike"));
        if (tag.contains("Xp")) this.entityData.set(XP, tag.getInt("Xp"));
        if (tag.contains("Skills")) {
            int[] saved = tag.getIntArray("Skills");
            for (int i = 0; i < skillRanks.length && i < saved.length; i++) {
                var s = com.lex3d.ultimatezootaming.progression.KeeperSkill.values()[i];
                skillRanks[i] = Math.max(0, Math.min(s.maxRank, saved[i]));
            }
        }
    }

    public int getJob() { return this.entityData.get(JOB); }

    /** Nombre de metiers : 0 Polyvalent, 1 Veterinaire, 2 Nourrisseur,
     *  3 Garde, 4 Vendeur, 5 Agent d'entretien. */
    public static final int JOB_COUNT = 6;
    public void setJob(int job) { this.entityData.set(JOB, Math.floorMod(job, JOB_COUNT)); }

    public static String jobKey(int job) {
        return switch (job) {
            case 1 -> "vet";
            case 2 -> "feeder";
            case 3 -> "guard";
            case 4 -> "vendor";
            case 5 -> "janitor";
            default -> "generalist";
        };
    }

    /** En greve : employe impaye, il ne travaille plus. */
    public boolean isOnStrike() { return this.entityData.get(STRIKE); }

    public void setOnStrike(boolean strike) { this.entityData.set(STRIKE, strike); }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animController));
    }

    private PlayState animController(AnimationState<ZooKeeperEntity> state) {
        int action = getAction();
        if (action == 2) {
            return state.setAndContinue(REFILL);
        }
        if (action == 1) {
            return state.setAndContinue(CARE);
        }
        if (state.isMoving()) {
            return state.setAndContinue(WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
