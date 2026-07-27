package com.lex3d.ultimatezootaming.contracts;

import net.minecraft.nbt.CompoundTag;

/**
 * CONTRAT INTERNATIONAL : un zoo etranger reclame un animal precis contre une
 * belle somme et du prestige.
 *
 * Structure volontairement plate (quelques champs primitifs) : la sauvegarde
 * reste legere et le contrat se transmet au client en une poignee d'octets.
 */
public class ZooContract {

    /** Ce que le zoo demandeur exige de l'animal. */
    public enum Requirement {
        /** N'importe quel individu de l'espece. */
        ANY("any"),
        /** Un bebe (releve de l'elevage). */
        BABY("baby"),
        /** Un adulte en pleine forme (bien-etre >= 80). */
        HEALTHY("healthy"),
        /** Un specimen rare (argente ou mieux). */
        RARE("rare");

        public final String key;
        Requirement(String key) { this.key = key; }
    }

    /** Identifiant de l'espece reclamee (ex "minecraft:panda"). */
    public String species = "";
    /** Nom du zoo demandeur, purement cosmetique. */
    public String client = "";
    public Requirement requirement = Requirement.ANY;
    /** Recompense en billets. */
    public int reward = 0;
    /** Jours restants avant expiration. */
    public int daysLeft = 0;
    /** Prestige gagne a la livraison (visiteurs cumules offerts). */
    public int prestige = 0;

    public boolean isActive() { return !species.isEmpty() && daysLeft > 0; }

    /** Cle de traduction decrivant l'exigence. */
    public String requirementKey() {
        return "contract.ultimatezootaming.req." + requirement.key;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Species", species);
        t.putString("Client", client);
        t.putString("Req", requirement.name());
        t.putInt("Reward", reward);
        t.putInt("Days", daysLeft);
        t.putInt("Prestige", prestige);
        return t;
    }

    public static ZooContract load(CompoundTag t) {
        ZooContract c = new ZooContract();
        c.species = t.getString("Species");
        c.client = t.getString("Client");
        try {
            c.requirement = Requirement.valueOf(t.getString("Req"));
        } catch (IllegalArgumentException e) {
            c.requirement = Requirement.ANY;
        }
        c.reward = t.getInt("Reward");
        c.daysLeft = t.getInt("Days");
        c.prestige = t.getInt("Prestige");
        return c;
    }

    /** Noms de zoos etrangers, pour l'ambiance. */
    private static final String[] CLIENTS = {
        "Zoo de Reykjavik", "Parc de Kyoto", "Safari de Nairobi", "Zoo de Valparaiso",
        "Jardin de Lisbonne", "Reserve de Darwin", "Parc de Montreal", "Zoo de Wellington"
    };

    /**
     * Tire un contrat au sort a partir des especes REELLEMENT presentes dans le
     * zoo : inutile de reclamer un animal que le joueur ne possede pas.
     */
    public static ZooContract roll(java.util.List<String> availableSpecies,
                                   java.util.Random rng, int zooRank) {
        ZooContract c = new ZooContract();
        if (availableSpecies.isEmpty()) return c; // pas de contrat possible
        c.species = availableSpecies.get(rng.nextInt(availableSpecies.size()));
        c.client = CLIENTS[rng.nextInt(CLIENTS.length)];

        // Plus le zoo est repute, plus les demandes sont exigeantes et payantes
        int roll = rng.nextInt(100);
        if (roll < 40) c.requirement = Requirement.ANY;
        else if (roll < 70) c.requirement = Requirement.HEALTHY;
        else if (roll < 90) c.requirement = Requirement.BABY;
        else c.requirement = Requirement.RARE;

        int base = switch (c.requirement) {
            case ANY -> 120;
            case HEALTHY -> 220;
            case BABY -> 320;
            case RARE -> 600;
        };
        c.reward = base + zooRank * 40 + rng.nextInt(60);
        c.daysLeft = switch (c.requirement) {
            case ANY -> 3;
            case HEALTHY -> 3;
            case BABY -> 4;
            case RARE -> 5;
        };
        c.prestige = switch (c.requirement) {
            case ANY -> 15;
            case HEALTHY -> 30;
            case BABY -> 50;
            case RARE -> 100;
        };
        return c;
    }
}
