package com.lex3d.ultimatezootaming.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Comptabilite du zoo : recettes et depenses de la journee en cours. */
public class ZooLedger extends SavedData {

    private static final String NAME = "ultimatezootame_ledger";

    private int tickets, sales, salaries, visitorsToday, day;
    private long totalEarned;          // cumul historique (pour les jalons)
    private boolean open = true;       // zoo ouvert (jour) ou ferme (nuit)
    /** Evenement du jour : 0 aucun, 1 Journee promo, 2 Inspection sanitaire, 3 Canicule. */
    private int dailyEvent = 0;

    /** Hype (naissance...) : tant que > 0, l'affluence est boostee. Decremente par le DayHandler. */
    private int hypeTicks = 0;

    /** Politique tarifaire du billet : 0 Bas, 1 Normal, 2 Cher. */
    private int ticketPolicy = 1;
    private final boolean[] milestones = new boolean[10];
    /** Cumul de visiteurs depuis l'ouverture (objectifs). */
    private long totalVisitors = 0;
    /** Benefices des 7 derniers jours (le plus recent en dernier). */
    /** Missions journalieres : indices dans DailyMission.values(), remis a jour chaque jour. */
    private int[] dailyMissions = new int[]{0, 1, 2};
    private int[] dailyProgress = new int[]{0, 0, 0};
    private boolean[] dailyClaimed = new boolean[]{false, false, false};

    public int[] getMissions() { return dailyMissions; }
    public int[] getMissionProgress() { return dailyProgress; }
    public boolean[] getMissionClaimed() { return dailyClaimed; }

    /** Incremente la progression d'un type de mission (si c'est une des 3 du jour). */
    public void addMissionProgress(com.lex3d.ultimatezootaming.progression.DailyMission type, int amount) {
        for (int i = 0; i < dailyMissions.length; i++) {
            if (dailyMissions[i] == type.ordinal() && !dailyClaimed[i]) {
                dailyProgress[i] = Math.min(type.target, dailyProgress[i] + amount);
                setDirty();
            }
        }
    }

    /** Force la progression a une valeur precise (pour les missions basees sur des etats,
     *  ex. note du zoo, ambiance moyenne). */
    public void setMissionProgress(com.lex3d.ultimatezootaming.progression.DailyMission type, int value) {
        for (int i = 0; i < dailyMissions.length; i++) {
            if (dailyMissions[i] == type.ordinal() && !dailyClaimed[i]) {
                if (value > dailyProgress[i]) {
                    dailyProgress[i] = Math.min(type.target, value);
                    setDirty();
                }
            }
        }
    }

    /** Reclame la prime d'une mission accomplie. Retourne la prime (0 si echec). */
    public int claimMission(int slot) {
        if (slot < 0 || slot >= 3 || dailyClaimed[slot]) return 0;
        var mission = com.lex3d.ultimatezootaming.progression.DailyMission.values()[dailyMissions[slot]];
        if (dailyProgress[slot] < mission.target) return 0;
        dailyClaimed[slot] = true;
        setDirty();
        return mission.reward;
    }

    /** Tire 3 nouvelles missions distinctes au hasard (appele au nouveau jour). */
    public void rollDailyMissions(java.util.Random rng) {
        var all = com.lex3d.ultimatezootaming.progression.DailyMission.values();
        java.util.List<Integer> pool = new java.util.ArrayList<>();
        for (int i = 0; i < all.length; i++) pool.add(i);
        java.util.Collections.shuffle(pool, rng);
        for (int i = 0; i < 3; i++) {
            dailyMissions[i] = pool.get(i);
            dailyProgress[i] = 0;
            dailyClaimed[i] = false;
        }
        setDirty();
    }

    /** Rang max atteint historiquement (index dans ZooRank.values()). */
    private int highestRankReached = 0;
    public int getHighestRank() { return highestRankReached; }
    public void setHighestRank(int r) { this.highestRankReached = r; setDirty(); }

    /** Le flux social du zoo (posts des visiteurs). */
    private com.lex3d.ultimatezootaming.social.ZooFeed feed =
            new com.lex3d.ultimatezootaming.social.ZooFeed();

    public com.lex3d.ultimatezootaming.social.ZooFeed getFeed() { return feed; }

    /** Publie un post et marque la sauvegarde. */
    public void publishPost(com.lex3d.ultimatezootaming.social.ZooPost post) {
        feed.publish(post);
        setDirty();
    }

    /** Multiplicateur d'affluence issu du buzz (0.7 a 1.4). */
    public double buzzFactor() { return feed.buzzFactor(day); }

    /** Contrat international en cours (null = aucune demande). */
    private com.lex3d.ultimatezootaming.contracts.ZooContract contract;

    public com.lex3d.ultimatezootaming.contracts.ZooContract getContract() { return contract; }

    public void setContract(com.lex3d.ultimatezootaming.contracts.ZooContract c) {
        this.contract = c;
        setDirty();
    }

    public void clearContract() {
        this.contract = null;
        setDirty();
    }

    /** Fait vieillir le contrat d'un jour. Retourne true s'il vient d'expirer. */
    public boolean tickContract() {
        if (contract == null || !contract.isActive()) return false;
        contract.daysLeft--;
        setDirty();
        if (contract.daysLeft <= 0) {
            contract = null;
            return true;
        }
        return false;
    }

    /** Campagne pub active (ordinal AdCampaign, 0 = aucune). */
    private int activeCampaign = 0;
    /** Jours restants pour la campagne active. */
    private int campaignDaysLeft = 0;

    public int getActiveCampaign() { return activeCampaign; }
    public int getCampaignDaysLeft() { return campaignDaysLeft; }

    public com.lex3d.ultimatezootaming.marketing.AdCampaign getActiveCampaignEnum() {
        var vals = com.lex3d.ultimatezootaming.marketing.AdCampaign.values();
        return vals[Math.max(0, Math.min(vals.length - 1, activeCampaign))];
    }

    public void startCampaign(com.lex3d.ultimatezootaming.marketing.AdCampaign c) {
        this.activeCampaign = c.ordinal();
        this.campaignDaysLeft = c.durationDays;
        setDirty();
    }

    public void tickCampaign() {
        if (campaignDaysLeft > 0) {
            campaignDaysLeft--;
            if (campaignDaysLeft <= 0) activeCampaign = 0;
            setDirty();
        }
    }

    /** Multiplicateur d'affluence de la campagne active (1.0 si aucune). */
    public double campaignCrowdFactor() {
        return campaignDaysLeft > 0 ? (1.0 + getActiveCampaignEnum().crowdBonus) : 1.0;
    }

        private final int[] lastProfits = new int[7];
    private int profitCount = 0;

    /** TENDANCES : combien de fois chaque espece a ete DEMANDEE par les visiteurs.
     *  Sert a calculer le Top 5 des especes reclamees que le zoo n'a pas encore. */
    private final java.util.Map<String, Integer> speciesDemand = new java.util.HashMap<>();

    /** Un visiteur reclame une espece : +1 a sa demande (plafonnee pour eviter
     *  qu'une espece domine a vie). */
    public void addSpeciesDemand(String speciesId) {
        if (speciesId == null || speciesId.isEmpty()) return;
        speciesDemand.merge(speciesId, 3, (a, b) -> Math.min(999, a + b));
        setDirty();
    }

    /** Top N des especes les plus demandees ABSENTES du zoo (present = filtre applique
     *  par l'appelant). Retourne les paires (espece, score) triees par score decroissant. */
    public java.util.List<java.util.Map.Entry<String, Integer>> topDemands(
            java.util.Set<String> presentSpecies, int n) {
        java.util.List<java.util.Map.Entry<String, Integer>> list = new java.util.ArrayList<>();
        for (var e : speciesDemand.entrySet()) {
            if (presentSpecies != null && presentSpecies.contains(e.getKey())) continue; // deja au zoo
            if (e.getValue() <= 0) continue;
            list.add(e);
        }
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.size() > n ? list.subList(0, n) : list;
    }

    /** Quand une espece demandee est ajoutee au zoo : on retire sa demande (satisfaite)
     *  et on verse une PRIME proportionnelle a la demande accumulee. Retourne la prime. */
    public int fulfillDemand(String speciesId) {
        Integer score = speciesDemand.remove(speciesId);
        setDirty();
        if (score == null || score <= 0) return 0;
        // BONUS D'AFFLUENCE : ajouter une espece reclamee fait le buzz (dure d'autant
        // plus longtemps que la demande etait forte). ~24000 ticks = 1 jour MC.
        setHype(Math.min(24000, 6000 + score * 100));
        return Math.min(150, 20 + score * 2); // prime plafonnee a 150
    }

    /** Le score de demande d'une espece (0 si aucune). */
    public int demandOf(String speciesId) {
        return speciesDemand.getOrDefault(speciesId, 0);
    }

    /** Les N especes les plus demandees (pour la celebrite / evenements). */
    public java.util.List<String> getTopDemandedSpecies(int n) {
        return speciesDemand.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    public static ZooLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ZooLedger::load, ZooLedger::new, NAME);
    }

    public void addTickets(int amount) { tickets += amount; totalEarned += amount; visitorsToday++; totalVisitors++; setDirty(); }

    public void addSales(int amount) { sales += amount; totalEarned += amount; setDirty(); }

    public void addSalaries(int amount) { salaries += amount; setDirty(); }

    public int getTickets() { return tickets; }
    public int getSales() { return sales; }
    public int getSalaries() { return salaries; }
    public int getVisitorsToday() { return visitorsToday; }
    public long getTotalEarned() { return totalEarned; }
    public long getTotalVisitors() { return totalVisitors; }

    /** Prestige gagne autrement qu'a l'entree (contrats honores, evenements). */
    public void addPrestige(int amount) {
        if (amount <= 0) return;
        totalVisitors += amount;
        setDirty();
    }

    public int[] getLastProfits() {
        int n = Math.min(profitCount, 7);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = lastProfits[(profitCount - n + i) % 7];
        return out;
    }

    /** Enregistre le benefice du jour ecoule (appele a la fermeture). */
    public void pushProfit(int profit) {
        lastProfits[profitCount % 7] = profit;
        profitCount++;
        setDirty();
    }
    public int getDay() { return day; }
    public boolean isOpen() { return open; }

    private boolean paused = false; // zoo ferme manuellement (pause)
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { this.paused = p; if (p) open = false; setDirty(); }

    public void setOpen(boolean open) { this.open = open; setDirty(); }

    /** Remet le zoo a ZERO (jour 0, stats, cumuls, jalons) — pour les tests. */
    public void resetAll() {
        tickets = sales = salaries = visitorsToday = day = 0;
        totalEarned = 0;
        totalVisitors = 0;
        open = true;
        paused = false; // sort le zoo de la pause au reset
        dailyEvent = 0;
        hypeTicks = 0;
        ticketPolicy = 1;
        java.util.Arrays.fill(milestones, false);
        java.util.Arrays.fill(lastProfits, 0);
        profitCount = 0;
        setDirty();
    }

    public int getTicketPolicy() { return ticketPolicy; }

    public void cycleTicketPolicy() { ticketPolicy = (ticketPolicy + 1) % 3; setDirty(); }

    /** Multiplicateur du prix du billet selon la politique. */
    public double priceFactor() {
        return (switch (ticketPolicy) { case 0 -> 0.5; case 2 -> 1.75; default -> 1.0; }) * eventPriceFactor();
    }

    public int getDailyEvent() { return dailyEvent; }

    public void setDailyEvent(int event) { this.dailyEvent = event; setDirty(); }

    public boolean isHyped() { return hypeTicks > 0; }

    public void setHype(int ticks) { hypeTicks = Math.max(hypeTicks, ticks); setDirty(); }

    public void tickHype(int elapsed) {
        if (hypeTicks > 0) { hypeTicks = Math.max(0, hypeTicks - elapsed); setDirty(); }
    }

    /** Multiplicateur du prix du billet selon l'evenement (promo = moitie prix). */
    public double eventPriceFactor() { return dailyEvent == 1 ? 0.5 : 1.0; }

    /** Multiplicateur d'affluence selon l'evenement (promo = foule). */
    public double eventCrowdFactor() {
        // Anciens events + nouveaux DynamicEvents
        if (dailyEvent == 1) return 1.6; // JOURNALIST : +60% affluence (curiosite)
        if (dailyEvent == com.lex3d.ultimatezootaming.events.dynamic.DynamicEvent.PROTEST.ordinal()) return 0.7; // -30%
        return 1.0;
    }

    /** Multiplicateur d'affluence selon la politique. */
    public double crowdFactor() {
        return (switch (ticketPolicy) { case 0 -> 1.3; case 2 -> 0.6; default -> 1.0; })
                * eventCrowdFactor() * campaignCrowdFactor() * buzzFactor();
    }

    /** Nouveau jour : remet les compteurs a zero. */
    public void newDay() {
        day++;
        tickets = 0;
        sales = 0;
        salaries = 0;
        visitorsToday = 0;
        // Tire 3 nouvelles missions du jour (seed base sur le jour pour la stabilite serveur)
        rollDailyMissions(new java.util.Random(day * 7919L));
        // Decompte de la campagne pub active
        tickCampaign();
        setDirty();
    }

    public boolean[] getMilestones() { return milestones.clone(); }

    /** Marque un jalon comme atteint. Retourne true si c'est la premiere fois. */
    public boolean reachMilestone(int index) {
        if (index < 0 || index >= milestones.length || milestones[index]) return false;
        milestones[index] = true;
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Tickets", tickets);
        tag.putInt("Sales", sales);
        tag.putInt("Salaries", salaries);
        tag.putInt("VisitorsToday", visitorsToday);
        tag.putInt("Day", day);
        tag.putLong("TotalEarned", totalEarned);
        tag.putBoolean("Open", open);
        tag.putBoolean("Paused", paused);
        tag.putInt("TicketPolicy", ticketPolicy);
        tag.putInt("HypeTicks", hypeTicks);
        tag.putInt("DailyEvent", dailyEvent);
        for (int i = 0; i < milestones.length; i++) tag.putBoolean("M" + i, milestones[i]);
        tag.putInt("HighestRank", highestRankReached);
        tag.put("Feed", feed.save());
        if (contract != null && contract.isActive()) tag.put("Contract", contract.save());
        tag.putInt("ActiveCampaign", activeCampaign);
        tag.putInt("CampaignDaysLeft", campaignDaysLeft);
        for (int i = 0; i < 3; i++) {
            tag.putInt("Mission" + i, dailyMissions[i]);
            tag.putInt("MissionP" + i, dailyProgress[i]);
            tag.putBoolean("MissionC" + i, dailyClaimed[i]);
        }
        tag.putLong("TotalVisitors", totalVisitors);
        tag.putIntArray("LastProfits", lastProfits);
        tag.putInt("ProfitCount", profitCount);
        // Registre des tendances (especes demandees)
        CompoundTag demands = new CompoundTag();
        for (var e : speciesDemand.entrySet()) demands.putInt(e.getKey(), e.getValue());
        tag.put("SpeciesDemand", demands);
        return tag;
    }

    public static ZooLedger load(CompoundTag tag) {
        ZooLedger l = new ZooLedger();
        l.tickets = tag.getInt("Tickets");
        l.sales = tag.getInt("Sales");
        l.salaries = tag.getInt("Salaries");
        l.visitorsToday = tag.getInt("VisitorsToday");
        l.day = tag.getInt("Day");
        l.totalEarned = tag.getLong("TotalEarned");
        l.open = tag.getBoolean("Open");
        l.paused = tag.getBoolean("Paused");
        l.ticketPolicy = tag.contains("TicketPolicy") ? tag.getInt("TicketPolicy") : 1;
        l.hypeTicks = tag.getInt("HypeTicks");
        l.dailyEvent = tag.getInt("DailyEvent");
        for (int i = 0; i < l.milestones.length; i++) l.milestones[i] = tag.getBoolean("M" + i);
        l.highestRankReached = tag.getInt("HighestRank");
        if (tag.contains("Feed")) {
            l.feed = com.lex3d.ultimatezootaming.social.ZooFeed.load(tag.getCompound("Feed"));
        }
        if (tag.contains("Contract")) {
            l.contract = com.lex3d.ultimatezootaming.contracts.ZooContract
                    .load(tag.getCompound("Contract"));
        }
        l.activeCampaign = tag.getInt("ActiveCampaign");
        l.campaignDaysLeft = tag.getInt("CampaignDaysLeft");
        for (int i = 0; i < 3; i++) {
            if (tag.contains("Mission" + i)) l.dailyMissions[i] = tag.getInt("Mission" + i);
            if (tag.contains("MissionP" + i)) l.dailyProgress[i] = tag.getInt("MissionP" + i);
            if (tag.contains("MissionC" + i)) l.dailyClaimed[i] = tag.getBoolean("MissionC" + i);
        }
        l.totalVisitors = tag.getLong("TotalVisitors");
        int[] lp = tag.getIntArray("LastProfits");
        if (lp.length == 7) System.arraycopy(lp, 0, l.lastProfits, 0, 7);
        l.profitCount = tag.getInt("ProfitCount");
        if (tag.contains("SpeciesDemand")) {
            CompoundTag demands = tag.getCompound("SpeciesDemand");
            for (String key : demands.getAllKeys()) l.speciesDemand.put(key, demands.getInt(key));
        }
        return l;
    }
}
