package com.lex3d.ultimatezootaming.entities;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Visiteur du zoo : entre par l'Entree, va admirer les enclos, achete en
 * boutique, puis repart. Duree de vie limitee (anti-lag).
 */
public class VisitorEntity extends PathfinderMob implements GeoEntity {

    /** Nombre de skins locaux disponibles (textures/entity/visitor/visitor_1..N.png).
     *  Pour en ajouter : depose visitor_9.png, visitor_10.png... et monte ce nombre. */
    /** Nombre de skins disponibles pour les visiteurs (60 = beaucoup de variete).
     *  Pour ajouter des skins : depose des PNGs 64x64 dans
     *  src/main/resources/assets/ultimatezootaming/textures/entity/visitor/
     *  nommes visitor_1.png a visitor_60.png. Les manquants -> texture par defaut. */
    public static final int SKIN_COUNT = 60;

    /** Nombre de skins ENFANTS. Depose des PNGs 64x64 dans
     *  textures/entity/visitor/child/visitor_1.png a visitor_20.png. */
    public static final int CHILD_SKIN_COUNT = 20;
    /** Rythme de visite : 0 = presse (rapide), 1 = normal, 2 = contemplatif (flane). */
    private int pace = 1;
    /** Personnalite (ordinal de VisitorPersonality). Tire au spawn. */
    private int personality = 0;
    public int getPersonality() { return personality; }
    public com.lex3d.ultimatezootaming.entities.VisitorPersonality getPersonalityEnum() {
        var vals = com.lex3d.ultimatezootaming.entities.VisitorPersonality.values();
        return vals[Math.max(0, Math.min(vals.length - 1, personality))];
    }
    public void setPersonality(int p) { this.personality = p; }
    public int getPace() { return pace; }
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LEAVING =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.BOOLEAN);
    /** Pseudo dont on emprunte le skin en ligne. Vide = skin Minecraft par defaut. */
    private static final EntityDataAccessor<String> SKIN_NAME =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> CHILD =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.BOOLEAN);
    // Objet tenu en main : 0 rien, 1 soda, 2 popcorn, 3 glace, 4 barbe a papa, 5 ballon
    private static final EntityDataAccessor<Integer> HELD =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BALLOON_COLOR =
            SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Position de l'entree par laquelle il est arrive (pour repartir). */
    private BlockPos entrance;
    /** Ticks de vie restants avant depart force. */
    private int visitTicks = 4800; // ~4 min
    /** Ticks restants pendant lesquels le visiteur admire la photo qu'il vient
     *  de prendre. A zero, il la range et reprend son objet precedent. */
    private int photoTicks = 0;
    /** Objet qu'il tenait avant de sortir sa photo, pour le lui rendre apres. */
    private int heldBeforePhoto = HELD_NONE;

    /** Le visiteur sort sa photo et la contemple quelques secondes. */
    public void admirePhoto(int ticks) {
        if (photoTicks <= 0) heldBeforePhoto = getHeldItem();
        photoTicks = Math.max(photoTicks, ticks);
        setHeldItem(HELD_PHOTO);
    }

    /** Le visiteur porte-t-il une casquette du zoo achetee en boutique ? */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CAP =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    VisitorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public boolean hasCap() { return this.entityData.get(CAP); }
    public void setCap(boolean b) { this.entityData.set(CAP, b); }

    /** L'objet a poser sur la tete, vide s'il ne porte rien. */
    public net.minecraft.world.item.ItemStack getHeadStack() {
        return hasCap()
                ? new net.minecraft.world.item.ItemStack(
                        com.lex3d.ultimatezootaming.core.init.ModItems.ZOO_CAP.get())
                : net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** True pendant qu'il discute avec un autre visiteur (evite les groupes
     *  de discussion en chaine et les doubles engagements). */
    private boolean chatting = false;
    public boolean isChatting() { return chatting; }
    public void setChatting(boolean b) { this.chatting = b; }

    /** True quand le visiteur a passe le guichet (ou spawn sans guichet). */
    private boolean hasTicket = false;
    public boolean hasTicket() { return hasTicket; }
    public void setTicket(boolean t) { this.hasTicket = t; }
    private int snackTimer = 0; // progression de consommation du snack
    /** Satisfaction accumulee en regardant les animaux (pour les achats). */
    private int joy = 0;
    /** Besoins 0-100 : au-dela de 60 il cherche une solution, sinon il rale. */
    private int hunger = 0, thirst = 0, fatigue = 0;
    /** VIP : veut voir une espece precise, prime s'il la voit. */
    private String vipWish = "";
    private boolean vipRewarded = false;
    private int vipTimer = 0;
    /** Il a un dechet en main apres un achat. */
    private boolean litter = false;
    private boolean skinRolled = false;

    public VisitorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired(); // gere par le mod, pas par le despawn vanilla
        setMaxUpStep(1.0f); // monte les marches d'un bloc entier (escaliers, rebords)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.lex3d.ultimatezootaming.ai.VisitorLeaveGoal(this));
        // FILE D'ATTENTE au guichet (priorite haute : le visiteur DOIT payer avant
        // de visiter). Actif seulement s'il y a des guichets et pas encore de ticket.
        this.goalSelector.addGoal(1, new com.lex3d.ultimatezootaming.ai.VisitorTicketQueueGoal(this));
        // Rencontres occasionnelles : priorite basse, purement decoratif
        this.goalSelector.addGoal(5, new com.lex3d.ultimatezootaming.ai.VisitorChatGoal(this));
        // CERVEAU de visite : planifie un vrai parcours (voir -> boire -> manger ->
        // souvenir -> partir). Remplace les anciens goals qui se battaient et
        // faisaient tourner en rond.
        this.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.VisitorBrainGoal(this));
        // Suivi de groupe : les accompagnants restent pres de leur chef (prio > cerveau
        // quand ils s'eloignent trop, sinon le cerveau les fait visiter normalement)
        this.goalSelector.addGoal(2, new com.lex3d.ultimatezootaming.ai.VisitorGroupGoal(this));
        // Filet de secours : petite errance UNIQUEMENT si le cerveau n'a rien a faire
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, 0);
        this.entityData.define(LEAVING, false);
        this.entityData.define(CAP, false);
        this.entityData.define(SKIN_NAME, "");
        this.entityData.define(CHILD, false);
        this.entityData.define(HELD, 0);
        this.entityData.define(BALLOON_COLOR, 0);
    }

    /** Objet tenu : 0 rien / 1 soda / 2 popcorn / 3 glace / 4 barbe a papa / 5 ballon. */
    public int getHeldItem() { return this.entityData.get(HELD); }
    public void setHeldItem(int h) { this.entityData.set(HELD, h); }
    public int getBalloonColor() { return this.entityData.get(BALLOON_COLOR); }
    public void setBalloonColor(int cc) { this.entityData.set(BALLOON_COLOR, cc); }
    public static final int HELD_NONE=0, HELD_SODA=1, HELD_POPCORN=2, HELD_ICECREAM=3, HELD_COTTON=4, HELD_BALLOON=5, HELD_UMBRELLA=6, HELD_PHOTO=7, HELD_BINOCULARS=8, HELD_SELFIE=9;
    public static final int BALLOON_COLORS = 6; // rouge, bleu, vert, jaune, rose, violet

    /** L'ItemStack a afficher dans la main selon l'objet tenu. Utilise des items
     *  vanilla proches (facile a changer pour des items custom plus tard). */
    public net.minecraft.world.item.ItemStack getHeldStack() {
        return switch (getHeldItem()) {
            case HELD_SODA -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.VISITOR_SODA.get());
            case HELD_POPCORN -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.VISITOR_POPCORN.get());
            case HELD_ICECREAM -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.VISITOR_ICECREAM.get());
            case HELD_COTTON -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.VISITOR_COTTON.get());
            case HELD_BALLOON -> balloonStack();
            case HELD_UMBRELLA -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.UMBRELLA.get());
            case HELD_PHOTO -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.VISITOR_PHOTO.get());
            case HELD_BINOCULARS -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.BINOCULARS.get());
            case HELD_SELFIE -> new net.minecraft.world.item.ItemStack(
                    com.lex3d.ultimatezootaming.core.init.ModItems.SELFIE_STICK.get());
            default -> net.minecraft.world.item.ItemStack.EMPTY;
        };
    }

    private net.minecraft.world.item.ItemStack balloonStack() {
        net.minecraft.world.item.Item[] colors = {
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_RED.get(),
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_BLUE.get(),
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_GREEN.get(),
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_YELLOW.get(),
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_PINK.get(),
            com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_PURPLE.get()
        };
        int i = Math.max(0, Math.min(colors.length - 1, getBalloonColor()));
        return new net.minecraft.world.item.ItemStack(colors[i]);
    }

    private java.util.UUID groupLeader; // chef du groupe a suivre (null = solo/chef)
    public java.util.UUID getGroupLeader() { return groupLeader; }
    public void setGroupLeader(java.util.UUID id) { this.groupLeader = id; }

    public boolean isChild() { return this.entityData.get(CHILD); }
    public void setChild(boolean c) {
        this.entityData.set(CHILD, c);
        refreshDimensions();
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDimensions(pose);
        return isChild() ? base.scale(0.65f) : base;
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHILD.equals(key)) refreshDimensions();
    }

    public int getSkin() { return this.entityData.get(SKIN); }

    public String getSkinName() { return this.entityData.get(SKIN_NAME); }

    public void setSkinName(String name) { this.entityData.set(SKIN_NAME, name); }

    public boolean isLeaving() { return this.entityData.get(LEAVING); }

    public void setLeaving(boolean leaving) { this.entityData.set(LEAVING, leaving); }

    public BlockPos getEntrance() { return entrance; }

    public void setEntrance(BlockPos pos) { this.entrance = pos; }

    public int getJoy() { return joy; }

    public void addJoy(int amount) { this.joy = Math.min(100, joy + amount); }

    public void spendJoy(int amount) { this.joy = Math.max(0, joy - amount); }

    public int getHunger() { return hunger; }
    public int getThirst() { return thirst; }
    public int getFatigue() { return fatigue; }

    public void satisfyHunger() { hunger = 0; }
    public void satisfyThirst() { thirst = 0; }

    /** Va vers une cible : le pathfinding PREFERE les chemins (slabs + Allee de
     *  zoo) grace au malus de terrain — les visiteurs empruntent tes allees
     *  naturellement, comme dans un vrai parc. */
    public void navigateVia(double x, double y, double z, double speed) {
        getNavigation().moveTo(x, y, z, speed);
    }

    /** Transforme ce visiteur en VIP qui reve de voir une espece. */
    public void makeVip(String speciesId) {
        this.vipWish = speciesId;
        setCustomName(net.minecraft.network.chat.Component.literal("\u2605 VIP"));
        setCustomNameVisible(true);
        // La demande entre TOUT DE SUITE dans les tendances du parc (le VIP reve de
        // cette espece des maintenant). Si le zoo l'a deja, computeTrends la filtrera.
        if (level() instanceof net.minecraft.server.level.ServerLevel sl && !speciesId.isEmpty()) {
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl).addSpeciesDemand(speciesId);
        }
    }
    public void rest() { fatigue = Math.max(0, fatigue - 70); }

    public boolean hasLitter() { return litter; }
    public void setLitter(boolean litter) { this.litter = litter; }

    /** Le besoin le plus urgent : 0 rien, 1 faim, 2 soif, 3 fatigue. */
    public int urgentNeed() {
        int max = Math.max(hunger, Math.max(thirst, fatigue));
        if (max < 60) return 0;
        if (max == hunger) return 1;
        if (max == thirst) return 2;
        return 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (!skinRolled) {
                this.entityData.set(SKIN, this.random.nextInt(SKIN_COUNT));
                // Rythme de visite aleatoire : 25% presse, 50% normal, 25% contemplatif
                int r = this.random.nextInt(4);
                pace = (r == 0) ? 0 : (r == 3) ? 2 : 1;
                // PERSONNALITE : chaque visiteur a un type qui influence son comportement
                var perso = com.lex3d.ultimatezootaming.entities.VisitorPersonality.roll(this.random);
                setPersonality(perso.ordinal());
                // EQUIPEMENT : un visiteur sur six arrive avec du materiel.
                // Le photographe est bien plus souvent equipe que les autres.
                boolean gearProne = perso == com.lex3d.ultimatezootaming.entities.VisitorPersonality.PHOTOGRAPHER;
                if (this.random.nextInt(gearProne ? 2 : 6) == 0) {
                    setHeldItem(this.random.nextBoolean() ? HELD_BINOCULARS : HELD_SELFIE);
                }
                // Effets initiaux selon la personnalite
                switch (perso) {
                    case PHOTOGRAPHER -> pace = 2; // contemplatif : passe du temps
                    case FAMILY -> pace = 2;       // avec des enfants : lent
                    case LONE_CHILD -> {
                        setChild(true);
                        setHeldItem(HELD_BALLOON);
                        setBalloonColor(this.random.nextInt(BALLOON_COLORS));
                    }
                    case CELEBRITY -> {
                        // Une celebrite entre TOUJOURS en tenant un objet chic
                        setHeldItem(HELD_COTTON); // barbe a papa (visible)
                        // Marquage visuel par un nom special (pas obligatoire)
                    }
                    default -> {}
                }
                // Certains visiteurs arrivent DEJA avec un objet (~1 sur 3) :
                // souvent un ballon colore (les enfants adorent), parfois un snack.
                if (this.random.nextInt(3) == 0) {
                    if (isChild() || this.random.nextBoolean()) {
                        setHeldItem(HELD_BALLOON);
                        setBalloonColor(this.random.nextInt(BALLOON_COLORS));
                    } else {
                        setHeldItem(1 + this.random.nextInt(4)); // soda/popcorn/glace/barbe
                    }
                }
                // Vitesse selon le rythme : presse marche plus vite, contemplatif lambine
                var spd = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (spd != null) spd.setBaseValue(pace == 0 ? 0.30 : pace == 2 ? 0.22 : 0.26);
                skinRolled = true;
            }
            if (--visitTicks <= 0) setLeaving(true);

            // CONSOMMATION du snack : un visiteur qui tient de la nourriture (pas un
            // ballon) la mange peu a peu, avec des particules, puis l'objet disparait.
            int held = getHeldItem();
            if (held != HELD_NONE && held != HELD_BALLOON) {
                snackTimer++;
                if (level() instanceof net.minecraft.server.level.ServerLevel sl3) {
                    // particules de bouchee toutes les ~2s (miettes / gouttes)
                    if (snackTimer % 40 == 20) {
                        var p = (held == HELD_SODA)
                                ? net.minecraft.core.particles.ParticleTypes.SPLASH
                                : net.minecraft.core.particles.ParticleTypes.ITEM_SLIME;
                        sl3.sendParticles(p, getX(), getEyeY(), getZ(), 3, 0.15, 0.1, 0.15, 0.0);
                    }
                }
                // Fini apres ~15s : l'objet disparait (il l'a termine)
                if (snackTimer > 300) {
                    setHeldItem(HELD_NONE);
                    snackTimer = 0;
                    addJoy(3); // petit plaisir d'avoir mange
                }
            } else {
                snackTimer = 0;
            }
            // Les besoins montent doucement pendant la visite
            if (tickCount % 100 == 0) {
                hunger = Math.min(100, hunger + 3);
                int thirstGain = 4;
                if (level() instanceof net.minecraft.server.level.ServerLevel sl2
                        && com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl2).getDailyEvent() == 3) {
                    thirstGain = 8; // canicule !
                }
                thirst = Math.min(100, thirst + thirstGain);
                fatigue = Math.min(100, fatigue + 2);
                // Besoin non satisfait trop longtemps : il perd patience
                if (hunger > 85 || thirst > 85 || fatigue > 90) spendJoy(3);
            }
            // Filet anti-lag : trop loin de l'entree = il disparait
            if (entrance != null && blockPosition().distSqr(entrance) > 200 * 200) discard();
            // VIP : cherche son espece reve toutes les 5s
            if (!vipWish.isEmpty() && !vipRewarded && !isLeaving() && tickCount % 100 == 0
                    && level() instanceof net.minecraft.server.level.ServerLevel svip) {
                vipTimer += 100;
                var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                        .getValue(net.minecraft.resources.ResourceLocation.tryParse(vipWish));
                boolean found = type != null && !svip.getEntitiesOfClass(
                        net.minecraft.world.entity.animal.Animal.class,
                        getBoundingBox().inflate(16),
                        a -> a.getType() == type && a.isAlive()).isEmpty();
                if (found) {
                    vipRewarded = true;
                    // l'espece est presente : la demande est satisfaite, on la retire
                    com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(svip).fulfillDemand(vipWish);
                    var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(svip);
                    if (vault != null) vault.deposit(20);
                    for (var p : svip.players()) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal("\u2605 ")
                                .withStyle(net.minecraft.ChatFormatting.GOLD)
                                .append(net.minecraft.network.chat.Component.translatable(
                                        "message.ultimatezootaming.vip_happy",
                                        net.minecraft.network.chat.Component.translatable(
                                                type.getDescriptionId()))
                                        .withStyle(net.minecraft.ChatFormatting.GOLD)), false);
                    }
                    com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(svip, this, "vip_thrilled");
                } else if (vipTimer > 6000) {
                    // Il n'a pas trouve son bonheur : il repart decu (sa demande est
                    // deja dans les tendances depuis sa creation).
                    vipWish = "";
                    com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(svip, this, "vip_sad");
                    setLeaving(true);
                }
            }
            // Hors du territoire du zoo : il rebrousse chemin
            if (tickCount % 60 == 0 && !isLeaving()
                    && level() instanceof net.minecraft.server.level.ServerLevel sl) {
                var t = com.lex3d.ultimatezootaming.saveddata.ZooTerritory.get(sl);
                if (!t.isEmpty() && !t.isClaimed(blockPosition().getX(), blockPosition().getZ())) {
                    setLeaving(true);
                }
            }
            // PHOTO : il contemple son cliche puis le range
            if (photoTicks > 0) {
                photoTicks--;
                if (photoTicks == 0 && getHeldItem() == HELD_PHOTO) {
                    setHeldItem(heldBeforePhoto);
                    heldBeforePhoto = HELD_NONE;
                }
            }
            // METEO : sortir/ranger le parapluie selon la pluie (verif toutes les 5s)
            if (tickCount % 100 == 0) {
                boolean raining = level().isRaining() || level().isThundering();
                boolean canSeeSky = level().canSeeSky(blockPosition());
                if (raining && canSeeSky && getHeldItem() == HELD_NONE && photoTicks <= 0) {
                    // Sort le parapluie
                    setHeldItem(HELD_UMBRELLA);
                } else if ((!raining || !canSeeSky) && getHeldItem() == HELD_UMBRELLA) {
                    // Range le parapluie
                    setHeldItem(HELD_NONE);
                }
            }
            // BALLON ECHAPPE : un enfant qui court peut lacher son ballon.
            // Verifie toutes les 3 s, et seulement s'il se deplace vraiment.
            if (tickCount % 60 == 0 && isBaby() && getHeldItem() == HELD_BALLOON
                    && level() instanceof net.minecraft.server.level.ServerLevel bsl) {
                boolean running = getDeltaMovement().horizontalDistanceSqr() > 0.008;
                if (running && random.nextInt(12) == 0) {
                    releaseBalloon(bsl);
                }
            }
            // DECHETS : quand il finit sa consommation, il cherche une poubelle.
            // S'il n'en trouve aucune a proximite, il jette par terre.
            if (tickCount % 400 == 0 && hasTicket()
                    && level() instanceof net.minecraft.server.level.ServerLevel lsl) {
                int carried = getHeldItem();
                boolean consumable = carried == HELD_SODA || carried == HELD_POPCORN
                        || carried == HELD_ICECREAM || carried == HELD_COTTON;
                if (consumable && random.nextInt(3) == 0) {
                    setHeldItem(HELD_NONE); // il a fini
                    if (!binNearby(lsl, 8)) {
                        // Pas de poubelle : il jette au sol (et ca se verra)
                        com.lex3d.ultimatezootaming.blocks.LitterBlock.tryLitter(
                                lsl, blockPosition());
                    }
                }
            }
            // EXPOSITION A LA PLUIE : rester dehors sans abri use le moral.
            // Le parapluie protege, le toit aussi.
            if (tickCount % 100 == 0 && hasTicket()
                    && (level().isRaining() || level().isThundering())
                    && level().canSeeSky(blockPosition())
                    && getHeldItem() != HELD_UMBRELLA) {
                spendJoy(4);
            }
            // AVIS CONTEXTUELS : toutes les 30s, le visiteur juge son environnement
            // (prix, affluence, proprete, beaute, panneaux, personnel...).
            if (tickCount % 600 == 0 && hasTicket()
                    && level() instanceof net.minecraft.server.level.ServerLevel osl) {
                com.lex3d.ultimatezootaming.ai.VisitorAmbientOpinion.evaluate(osl, this);
            }
        }
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(
            net.minecraft.world.level.Level level) {
        return new com.lex3d.ultimatezootaming.ai.ZooPathNavigation(this, level);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Skin", getSkin());
        tag.putInt("Pace", pace);
        tag.putBoolean("Child", isChild());
        if (groupLeader != null) tag.putUUID("GroupLeader", groupLeader);
        tag.putInt("Held", getHeldItem());
        tag.putInt("BalloonColor", getBalloonColor());
        tag.putInt("Personality", personality);
        tag.putBoolean("HasTicket", hasTicket);
        tag.putBoolean("Cap", hasCap());
        tag.putString("SkinName", getSkinName());
        tag.putBoolean("SkinRolled", skinRolled);
        tag.putBoolean("Leaving", isLeaving());
        tag.putInt("VisitTicks", visitTicks);
        tag.putInt("Joy", joy);
        tag.putInt("Hunger", hunger);
        tag.putInt("Thirst", thirst);
        tag.putInt("Fatigue", fatigue);
        tag.putBoolean("Litter", litter);
        if (entrance != null) {
            tag.putInt("EntX", entrance.getX());
            tag.putInt("EntY", entrance.getY());
            tag.putInt("EntZ", entrance.getZ());
            tag.putString("VipWish", vipWish);
        tag.putBoolean("VipRewarded", vipRewarded);
    }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Skin")) this.entityData.set(SKIN, tag.getInt("Skin"));
        if (tag.contains("SkinName")) this.entityData.set(SKIN_NAME, tag.getString("SkinName"));
        if (tag.contains("Pace")) pace = tag.getInt("Pace");
        if (tag.contains("Child")) setChild(tag.getBoolean("Child"));
        if (tag.hasUUID("GroupLeader")) groupLeader = tag.getUUID("GroupLeader");
        if (tag.contains("Held")) setHeldItem(tag.getInt("Held"));
        if (tag.contains("BalloonColor")) setBalloonColor(tag.getInt("BalloonColor"));
        if (tag.contains("Personality")) setPersonality(tag.getInt("Personality"));
        if (tag.contains("HasTicket")) setTicket(tag.getBoolean("HasTicket"));
        if (tag.contains("Cap")) setCap(tag.getBoolean("Cap"));
        skinRolled = tag.getBoolean("SkinRolled");
        setLeaving(tag.getBoolean("Leaving"));
        if (tag.contains("VisitTicks")) visitTicks = tag.getInt("VisitTicks");
        joy = tag.getInt("Joy");
        hunger = tag.getInt("Hunger");
        thirst = tag.getInt("Thirst");
        fatigue = tag.getInt("Fatigue");
        litter = tag.getBoolean("Litter");
        if (tag.contains("EntX")) {
            entrance = new BlockPos(tag.getInt("EntX"), tag.getInt("EntY"), tag.getInt("EntZ"));
            vipWish = tag.getString("VipWish");
        vipRewarded = tag.getBoolean("VipRewarded");
    }
    }

    // ---- GeckoLib : reutilise le modele et les animations du soigneur ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.zookeeper.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.zookeeper.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    /** Y a-t-il une poubelle du zoo a portee ? Scan plat, peu couteux. */
    private boolean binNearby(net.minecraft.server.level.ServerLevel level, int radius) {
        var bin = com.lex3d.ultimatezootaming.core.init.ModBlocks.ZOO_BIN.get();
        var base = blockPosition();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (level.getBlockState(base.offset(dx, dy, dz)).is(bin)) return true;
                }
            }
        }
        return false;
    }

    /**
     * L'enfant lache son ballon : il s'envole reellement, et le petit pleure.
     * Un vendeur pourra lui en revendre un plus tard.
     */
    private void releaseBalloon(net.minecraft.server.level.ServerLevel level) {
        var balloon = com.lex3d.ultimatezootaming.core.init.ModEntities.LOOSE_BALLOON.get()
                .create(level);
        if (balloon != null) {
            balloon.setColor(getBalloonColor());
            balloon.moveTo(getX(), getEyeY() + 0.6, getZ(), getYRot(), 0f);
            level.addFreshEntity(balloon);
        }
        setHeldItem(HELD_NONE);
        // Chagrin : il perd de la joie et le fait savoir
        spendJoy(25);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                getX(), getEyeY(), getZ(), 6, 0.2, 0.1, 0.2, 0.0);
        level.playSound(null, blockPosition(),
                net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.9f);
    }
}
