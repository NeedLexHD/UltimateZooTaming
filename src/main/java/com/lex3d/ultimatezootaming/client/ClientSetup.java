package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.client.gui.ConfigModScreen;
import com.lex3d.ultimatezootaming.client.gui.MaternityScreen;
import com.lex3d.ultimatezootaming.client.gui.WhistleScreen;
import com.lex3d.ultimatezootaming.client.toasts.BirthToast;
import com.lex3d.ultimatezootaming.core.network.SyncFamiliarsS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Point d'entree cote client uniquement (jamais charge sur un serveur dedie grace
 * au DistExecutor dans UltimateZooTame). Sert de petit hub pour que les packets et
 * les items puissent piloter les ecrans/toasts sans dependre les uns des autres.
 */
public class ClientSetup {

    private static final List<SyncFamiliarsS2CPacket.FamiliarInfo> FAMILIARS_CACHE = new ArrayList<>();
    private static final List<PendingBaby> PENDING_BABIES = new ArrayList<>();

    public record PendingBaby(UUID uuid, String descriptionId) {}

    public static void init() {
        UltimateZooTame.LOGGER.info("[UltimateZooTaming] Client setup termine.");
    }

    /**
     * Accroche ConfigModScreen au bouton "Config" de l'ecran des mods Forge natif.
     * DOIT rester dans cette classe (client-only) et jamais dans UltimateZooTame :
     * meme derriere un DistExecutor, une methode qui reference ConfigScreenHandler/
     * ConfigModScreen depuis la classe principale du mod fait planter le chargement
     * sur serveur dedie (la classe entiere est verifiee au chargement, pas juste
     * la partie executee).
     */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parentScreen) -> new ConfigModScreen(parentScreen)));
    }

    public static void updateFamiliarsCache(List<SyncFamiliarsS2CPacket.FamiliarInfo> familiars) {
        FAMILIARS_CACHE.clear();
        FAMILIARS_CACHE.addAll(familiars);
        if (Minecraft.getInstance().screen instanceof WhistleScreen whistleScreen) {
            whistleScreen.refresh(FAMILIARS_CACHE);
        }
    }

    public static List<SyncFamiliarsS2CPacket.FamiliarInfo> getFamiliarsCache() {
        return FAMILIARS_CACHE;
    }

    public static void openHabitatScreen(java.util.Map<String, com.lex3d.ultimatezootaming.welfare.HabitatManager.Entry> overrides) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.HabitatConfigScreen(overrides));
    }

    public static void openAnimalScreen(java.util.UUID zoneId, String zoneName,
            java.util.List<com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket.AnimalInfo> animals) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.AnimalListScreen(zoneId, zoneName, animals));
    }

    public static void openRecruitment(com.lex3d.ultimatezootaming.core.network.OpenRecruitmentS2CPacket data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.RecruitmentScreen(data));
    }

    public static void openMissions(com.lex3d.ultimatezootaming.core.network.SyncMissionsS2CPacket data) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof com.lex3d.ultimatezootaming.client.gui.MissionsScreen ms) {
            ms.update(data);
        } else {
            mc.setScreen(new com.lex3d.ultimatezootaming.client.gui.MissionsScreen(data));
        }
    }


    public static void openFeed(com.lex3d.ultimatezootaming.core.network.SyncFeedS2CPacket data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.FeedScreen(data));
    }

    public static void openContract(com.lex3d.ultimatezootaming.core.network.SyncContractS2CPacket data) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.setScreen(new com.lex3d.ultimatezootaming.client.gui.ContractScreen(mc.screen, data));
    }

    public static void openSkills(com.lex3d.ultimatezootaming.core.network.SyncSkillsS2CPacket data) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof com.lex3d.ultimatezootaming.client.gui.SkillScreen s) {
            s.update(data); // deja ouvert : on rafraichit juste
        } else {
            mc.setScreen(new com.lex3d.ultimatezootaming.client.gui.SkillScreen(data));
        }
    }

    public static void openMarketing(com.lex3d.ultimatezootaming.core.network.SyncMarketingS2CPacket data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.MarketingScreen(
                        data.highestRank, data.activeCampaign, data.campaignDaysLeft));
    }

    public static void openVault(com.lex3d.ultimatezootaming.core.network.OpenVaultS2CPacket data) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof com.lex3d.ultimatezootaming.client.gui.VaultScreen vs) {
            vs.update(data);
        } else if (mc.screen instanceof com.lex3d.ultimatezootaming.client.gui.ExchangeScreen es) {
            es.setBalance(data.total); // maj du solde (tresorerie totale) apres un echange
        } else {
            mc.setScreen(new com.lex3d.ultimatezootaming.client.gui.VaultScreen(data));
        }
    }

    public static void openOrUpdateMap(com.lex3d.ultimatezootaming.core.network.MapDataS2CPacket data) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof com.lex3d.ultimatezootaming.client.gui.ZooMapScreen map) {
            map.updateData(data);
        } else {
            mc.setScreen(new com.lex3d.ultimatezootaming.client.gui.ZooMapScreen(data));
        }
    }

    public static void openAnimalCard(
            com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket.AnimalInfo animal) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.AnimalCardScreen(animal));
    }

    public static void openPriceScreen(net.minecraft.resources.ResourceLocation itemId,
            int currentPrice, int shopType) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.PriceScreen(itemId, currentPrice, shopType));
    }

    public static void openGuideScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.lex3d.ultimatezootaming.client.gui.ZooGuideScreen());
    }

    public static void openZoneScreen(
            java.util.List<com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket.ZoneInfo> zones,
            java.util.List<com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket.KeeperInfo> keepers,
            int zooScore, int globalSpecies, int treasury,
            java.util.List<com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket.ShopInfo> shops,
            com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket.ZooStats stats,
            net.minecraft.core.BlockPos computerPos) {
        boolean computerMode = computerPos != null;
        var screen = new com.lex3d.ultimatezootaming.client.gui.ZoneManagerScreen(
                zones, keepers, zooScore, globalSpecies, treasury, shops, stats, computerMode);
        if (computerMode) screen.setComputerPos(computerPos);
        net.minecraft.client.Minecraft.getInstance().setScreen(screen);
    }

    public static void openWhistleScreen(UUID focusUUID) {
        Minecraft.getInstance().setScreen(new WhistleScreen(FAMILIARS_CACHE, focusUUID));
    }

    public static void showBirthToast(UUID babyUUID, String descriptionId) {
        PENDING_BABIES.add(new PendingBaby(babyUUID, descriptionId));
        Minecraft.getInstance().getToasts().addToast(new BirthToast(descriptionId));
    }

    public static List<PendingBaby> getPendingBabies() {
        return PENDING_BABIES;
    }

    public static void openMaternityScreen() {
        Minecraft.getInstance().setScreen(new MaternityScreen(new ArrayList<>(PENDING_BABIES)));
    }

    public static void clearPendingBaby(UUID uuid) {
        PENDING_BABIES.removeIf(b -> b.uuid().equals(uuid));
    }
}
