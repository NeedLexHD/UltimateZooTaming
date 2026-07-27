package com.lex3d.ultimatezootaming.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Attachee a TOUT LivingEntity (sauf Player) via AttachCapEventHandler.
 * C'est la source de verite pour : qui possede ce mob, sa confiance actuelle,
 * son rayon d'errance et son etat assis/suivi/garde.
 */
public class TamingData {

    private UUID ownerUUID;
    private float trust;
    private double wanderRadius = 16.0D;
    private boolean sitting;
    private boolean forcedTame; // true = tamed via Croquettes+RNG, false = taming natif du mob
    /** Dernier calin du directeur (game time) : bonus avec cooldown. */
    private long lastPet = 0;

    /** Animal echappe de son enclos : panique, seul le joueur le ramene. */
    private boolean escaped = false;

    /** Maladie grave : seul le joueur peut la soigner (Remede superieur). */
    private boolean severeSick = false;

    /** Dernieres composantes du bien-etre : espace, habitat, nourriture, compagnie, sante (0-100). */
    private final int[] welfareBreakdown = {50, 50, 50, 50, 50};

    /** Point autour duquel le mob erre librement en mode "Garder ici" (voir ZooGuardGoal). Null = mode inactif. */
    @Nullable
    private BlockPos guardPos;

    /** Enclos auquel ce familier est assigne (voir ZooZoneGoal). Null = aucun. */
    @Nullable
    private UUID zoneId;

    /** Bien-etre 0-100 (voir WelfareHandler). Recalcule periodiquement en enclos. */
    private int satisfaction = 60;
    /** Malade : satisfaction plafonnee tant que pas soigne. */
    private boolean sick;
    /** Compteur de temps passe malheureux (pour declencher la maladie). */
    private int miseryTimer;

    /** Trait de personnalite, tire une seule fois a la capture. */
    private Trait trait = Trait.NONE;

    // ---------- FICHE APPROFONDIE (Vague 3) ----------

    /** Nom personnalise donne par le joueur ("" si aucun). */
    private String customName = "";
    /** Jour du zoo ou l'animal a ete capture/apprivoise (0 = inconnu). */
    private int captureDay = 0;
    /** Nombre total de fois soigne (Remede ou Super-remede). */
    private int healCount = 0;
    /** Nombre total de bebes generes (au sens Vague 3 : reproduction selective). */
    private int babyCount = 0;
    /** UUID de son "meilleur ami" (autre animal du zoo, si applicable). */
    @Nullable
    private UUID bestFriend;

    // ---------- GENETIQUE (Vague 3) ----------

    /** Rarete genetique : 0 = normale, 1 = argent, 2 = or, 3 = albinos (rare mutation). */
    private int rarity = 0;
    /** UUID des parents (si ne au bloc Incubateur). Null pour animal sauvage. */
    @Nullable
    private UUID parent1;
    @Nullable
    private UUID parent2;
    /** Generation : 0 = sauvage, N = descend de N generations en captivite. */
    private int generation = 0;

    public String getCustomName() { return customName; }
    public void setCustomName(String s) { this.customName = s == null ? "" : s; }
    public int getCaptureDay() { return captureDay; }
    public void setCaptureDay(int d) { this.captureDay = d; }
    public int getHealCount() { return healCount; }
    public void addHealCount() { this.healCount++; }
    public int getBabyCount() { return babyCount; }
    public void addBabyCount() { this.babyCount++; }
    @Nullable public UUID getBestFriend() { return bestFriend; }
    public void setBestFriend(@Nullable UUID f) { this.bestFriend = f; }

    public int getRarity() { return rarity; }
    public void setRarity(int r) { this.rarity = Math.max(0, Math.min(3, r)); }
    @Nullable public UUID getParent1() { return parent1; }
    @Nullable public UUID getParent2() { return parent2; }
    public void setParents(@Nullable UUID p1, @Nullable UUID p2) { this.parent1 = p1; this.parent2 = p2; }
    public int getGeneration() { return generation; }
    public void setGeneration(int g) { this.generation = g; }

    /**
     * Traits de personnalite (inspire d'Animal Husbandry). Chacun module legerement
     * le bien-etre ou le comportement. NONE = ordinaire (pas d'effet).
     */
    public enum Trait {
        NONE(0),
        GLUTTON(0),      // Glouton : la Mangeoire compte double, mais se vide plus vite (gere ailleurs)
        CUDDLY(6),       // Calin : +bien-etre pres du joueur/soigneur
        GRUMPY(-4),      // Grognon : -bien-etre de base, plus dur a contenter
        ENERGETIC(0),    // Energique : bouge vite, se reproduit plus volontiers
        HARDY(0),        // Robuste : tombe malade bien moins souvent
        SOCIAL(5),       // Sociable : bonus quand des congeneres sont la
        CURIOUS(3),      // Curieux : attire plus de visiteurs (mesuré par le welfare)
        SHY(-2),         // Timide : baisse d'humeur si trop de visiteurs
        PLAYFUL(4);      // Joueur : les bornes de jeu marchent 2x mieux

        private final int welfareModifier;
        Trait(int mod) { this.welfareModifier = mod; }
        public int getWelfareModifier() { return welfareModifier; }
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public float getTrust() {
        return trust;
    }

    public void setTrust(float trust) {
        this.trust = Math.max(0f, Math.min(100f, trust));
    }

    public void addTrust(float amount) {
        setTrust(this.trust + amount);
    }

    public double getWanderRadius() {
        return wanderRadius;
    }

    public void setWanderRadius(double wanderRadius) {
        this.wanderRadius = Math.max(2.0, Math.min(64.0, wanderRadius));
    }

    public boolean isSitting() {
        return sitting;
    }

    public void setSitting(boolean sitting) {
        this.sitting = sitting;
        if (sitting) {
            this.guardPos = null; // Assis, Garder et Enclos sont mutuellement exclusifs
            this.zoneId = null;
        }
    }

    public boolean isForcedTame() {
        return forcedTame;
    }

    public void setForcedTame(boolean forcedTame) {
        this.forcedTame = forcedTame;
    }

    public long getLastPet() { return lastPet; }

    public void setLastPet(long time) { this.lastPet = time; }

    public boolean isEscaped() { return escaped; }

    public void setEscaped(boolean escaped) { this.escaped = escaped; }

    public boolean isSevereSick() { return severeSick; }

    public void setSevereSick(boolean severe) { this.severeSick = severe; }

    public int[] getWelfareBreakdown() { return welfareBreakdown; }

    public void setWelfareBreakdown(int space, int habitat, int food, int company, int health) {
        welfareBreakdown[0] = space;
        welfareBreakdown[1] = habitat;
        welfareBreakdown[2] = food;
        welfareBreakdown[3] = company;
        welfareBreakdown[4] = health;
    }

    public boolean isTamed() {
        return ownerUUID != null;
    }

    @Nullable
    public BlockPos getGuardPos() {
        return guardPos;
    }

    public void setGuardPos(@Nullable BlockPos guardPos) {
        this.guardPos = guardPos;
        if (guardPos != null) {
            this.sitting = false;
            this.zoneId = null;
        }
    }

    public boolean isGuarding() {
        return guardPos != null;
    }

    @Nullable
    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(@Nullable UUID zoneId) {
        this.zoneId = zoneId;
        if (zoneId != null) {
            this.sitting = false;
            this.guardPos = null; // l'enclos remplace le mode "Garder ici"
        }
    }

    public boolean isInZoneMode() {
        return zoneId != null;
    }

    // ---- Bien-etre (Phase C) ----

    public int getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(int satisfaction) {
        this.satisfaction = Math.max(0, Math.min(100, satisfaction));
    }

    public boolean isSick() {
        return sick;
    }

    public void setSick(boolean sick) {
        this.sick = sick;
    }

    public int getMiseryTimer() {
        return miseryTimer;
    }

    public void setMiseryTimer(int miseryTimer) {
        this.miseryTimer = miseryTimer;
    }

    /** Humeur lisible derivee de la satisfaction. */
    public Mood getMood() {
        if (sick) return Mood.SICK;
        if (satisfaction > 75) return Mood.HAPPY;
        if (satisfaction < 25) return Mood.MISERABLE;
        return Mood.NEUTRAL;
    }

    public enum Mood { HAPPY, NEUTRAL, MISERABLE, SICK }

    public Trait getTrait() {
        return trait;
    }

    public void setTrait(Trait trait) {
        this.trait = trait;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putFloat("Trust", trust);
        tag.putDouble("WanderRadius", wanderRadius);
        tag.putBoolean("Sitting", sitting);
        tag.putBoolean("ForcedTame", forcedTame);
        tag.putIntArray("WelfareBd", welfareBreakdown);
        tag.putBoolean("SevereSick", severeSick);
        tag.putBoolean("Escaped", escaped);
        tag.putLong("LastPet", lastPet);
        if (guardPos != null) {
            tag.putInt("GuardX", guardPos.getX());
            tag.putInt("GuardY", guardPos.getY());
            tag.putInt("GuardZ", guardPos.getZ());
        }
        if (zoneId != null) tag.putUUID("ZoneId", zoneId);
        tag.putInt("Satisfaction", satisfaction);
        tag.putBoolean("Sick", sick);
        tag.putInt("MiseryTimer", miseryTimer);
        tag.putString("Trait", trait.name());
        // Fiche approfondie
        tag.putString("CustomName", customName);
        tag.putInt("CaptureDay", captureDay);
        tag.putInt("HealCount", healCount);
        tag.putInt("BabyCount", babyCount);
        if (bestFriend != null) tag.putUUID("BestFriend", bestFriend);
        // Genetique
        tag.putInt("Rarity", rarity);
        if (parent1 != null) tag.putUUID("Parent1", parent1);
        if (parent2 != null) tag.putUUID("Parent2", parent2);
        tag.putInt("Generation", generation);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.ownerUUID = tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
        this.trust = tag.getFloat("Trust");
        this.wanderRadius = tag.contains("WanderRadius") ? tag.getDouble("WanderRadius") : 16.0D;
        this.sitting = tag.getBoolean("Sitting");
        this.forcedTame = tag.getBoolean("ForcedTame");
        int[] bd = tag.getIntArray("WelfareBd");
        if (bd.length == 5) System.arraycopy(bd, 0, welfareBreakdown, 0, 5);
        severeSick = tag.getBoolean("SevereSick");
        escaped = tag.getBoolean("Escaped");
        lastPet = tag.getLong("LastPet");
        this.guardPos = tag.contains("GuardX")
                ? new BlockPos(tag.getInt("GuardX"), tag.getInt("GuardY"), tag.getInt("GuardZ"))
                : null;
        this.zoneId = tag.hasUUID("ZoneId") ? tag.getUUID("ZoneId") : null;
        this.satisfaction = tag.contains("Satisfaction") ? tag.getInt("Satisfaction") : 60;
        this.sick = tag.getBoolean("Sick");
        this.miseryTimer = tag.getInt("MiseryTimer");
        try {
            this.trait = Trait.valueOf(tag.getString("Trait"));
        } catch (IllegalArgumentException e) {
            this.trait = Trait.NONE;
        }
        // Fiche approfondie
        this.customName = tag.getString("CustomName");
        this.captureDay = tag.getInt("CaptureDay");
        this.healCount = tag.getInt("HealCount");
        this.babyCount = tag.getInt("BabyCount");
        this.bestFriend = tag.hasUUID("BestFriend") ? tag.getUUID("BestFriend") : null;
        // Genetique
        this.rarity = tag.getInt("Rarity");
        this.parent1 = tag.hasUUID("Parent1") ? tag.getUUID("Parent1") : null;
        this.parent2 = tag.hasUUID("Parent2") ? tag.getUUID("Parent2") : null;
        this.generation = tag.getInt("Generation");
    }

    public void copyFrom(TamingData other) {
        this.ownerUUID = other.ownerUUID;
        this.trust = other.trust;
        this.wanderRadius = other.wanderRadius;
        this.sitting = other.sitting;
        this.forcedTame = other.forcedTame;
        this.guardPos = other.guardPos;
        this.zoneId = other.zoneId;
        this.satisfaction = other.satisfaction;
        this.sick = other.sick;
        this.miseryTimer = other.miseryTimer;
        this.trait = other.trait;
    }
}
