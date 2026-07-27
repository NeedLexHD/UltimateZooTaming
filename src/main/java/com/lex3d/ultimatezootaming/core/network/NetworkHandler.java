package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UltimateZooTame.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    public static void register() {
        CHANNEL.registerMessage(nextId(), ConfigSyncC2SPacket.class,
                ConfigSyncC2SPacket::encode, ConfigSyncC2SPacket::decode, ConfigSyncC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), WhistleCommandC2SPacket.class,
                WhistleCommandC2SPacket::encode, WhistleCommandC2SPacket::decode, WhistleCommandC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), UpdateWanderRadiusC2SPacket.class,
                UpdateWanderRadiusC2SPacket::encode, UpdateWanderRadiusC2SPacket::decode, UpdateWanderRadiusC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), BirthToastS2CPacket.class,
                BirthToastS2CPacket::encode, BirthToastS2CPacket::decode, BirthToastS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestFamiliarsC2SPacket.class,
                RequestFamiliarsC2SPacket::encode, RequestFamiliarsC2SPacket::decode, RequestFamiliarsC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SyncFamiliarsS2CPacket.class,
                SyncFamiliarsS2CPacket::encode, SyncFamiliarsS2CPacket::decode, SyncFamiliarsS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestWelfareC2SPacket.class,
                RequestWelfareC2SPacket::encode, RequestWelfareC2SPacket::decode,
                RequestWelfareC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), WelfareInfoS2CPacket.class,
                WelfareInfoS2CPacket::encode, WelfareInfoS2CPacket::decode,
                WelfareInfoS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestZonesC2SPacket.class,
                RequestZonesC2SPacket::encode, RequestZonesC2SPacket::decode,
                RequestZonesC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SyncZonesS2CPacket.class,
                SyncZonesS2CPacket::encode, SyncZonesS2CPacket::decode,
                SyncZonesS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RenameZoneC2SPacket.class,
                RenameZoneC2SPacket::encode, RenameZoneC2SPacket::decode,
                RenameZoneC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), DeleteZoneC2SPacket.class,
                DeleteZoneC2SPacket::encode, DeleteZoneC2SPacket::decode,
                DeleteZoneC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), ResetZooC2SPacket.class,
                ResetZooC2SPacket::encode, ResetZooC2SPacket::decode,
                ResetZooC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), PauseZooC2SPacket.class,
                PauseZooC2SPacket::encode, PauseZooC2SPacket::decode,
                PauseZooC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), AssignKeeperC2SPacket.class,
                AssignKeeperC2SPacket::encode, AssignKeeperC2SPacket::decode,
                AssignKeeperC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SetZoneTypeC2SPacket.class,
                SetZoneTypeC2SPacket::encode, SetZoneTypeC2SPacket::decode,
                SetZoneTypeC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestAnimalsC2SPacket.class,
                RequestAnimalsC2SPacket::encode, RequestAnimalsC2SPacket::decode,
                RequestAnimalsC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SyncAnimalsS2CPacket.class,
                SyncAnimalsS2CPacket::encode, SyncAnimalsS2CPacket::decode,
                SyncAnimalsS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), ReleaseAnimalC2SPacket.class,
                ReleaseAnimalC2SPacket::encode, ReleaseAnimalC2SPacket::decode,
                ReleaseAnimalC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), OpenPriceScreenS2CPacket.class,
                OpenPriceScreenS2CPacket::encode, OpenPriceScreenS2CPacket::decode,
                OpenPriceScreenS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), SetItemPriceC2SPacket.class,
                SetItemPriceC2SPacket::encode, SetItemPriceC2SPacket::decode,
                SetItemPriceC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SetTicketPolicyC2SPacket.class,
                SetTicketPolicyC2SPacket::encode, SetTicketPolicyC2SPacket::decode,
                SetTicketPolicyC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SetKeeperJobC2SPacket.class,
                SetKeeperJobC2SPacket::encode, SetKeeperJobC2SPacket::decode,
                SetKeeperJobC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), SetShopTypeC2SPacket.class,
                SetShopTypeC2SPacket::encode, SetShopTypeC2SPacket::decode,
                SetShopTypeC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), OpenAnimalCardS2CPacket.class,
                OpenAnimalCardS2CPacket::encode, OpenAnimalCardS2CPacket::decode,
                OpenAnimalCardS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestMapC2SPacket.class,
                RequestMapC2SPacket::encode, RequestMapC2SPacket::decode,
                RequestMapC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), MapDataS2CPacket.class,
                MapDataS2CPacket::encode, MapDataS2CPacket::decode,
                MapDataS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), MapEditC2SPacket.class,
                MapEditC2SPacket::encode, MapEditC2SPacket::decode,
                MapEditC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), TogglePathBlockC2SPacket.class,
                TogglePathBlockC2SPacket::encode, TogglePathBlockC2SPacket::decode,
                TogglePathBlockC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), OpenRecruitmentS2CPacket.class,
                OpenRecruitmentS2CPacket::encode, OpenRecruitmentS2CPacket::decode,
                OpenRecruitmentS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), HireCandidateC2SPacket.class,
                HireCandidateC2SPacket::encode, HireCandidateC2SPacket::decode,
                HireCandidateC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), OpenVaultS2CPacket.class,
                OpenVaultS2CPacket::encode, OpenVaultS2CPacket::decode,
                OpenVaultS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), VaultActionC2SPacket.class,
                VaultActionC2SPacket::encode, VaultActionC2SPacket::decode,
                VaultActionC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), ExchangeC2SPacket.class,
                ExchangeC2SPacket::encode, ExchangeC2SPacket::decode,
                ExchangeC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), RequestMissionsC2SPacket.class,
                RequestMissionsC2SPacket::encode, RequestMissionsC2SPacket::decode,
                RequestMissionsC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), SyncMissionsS2CPacket.class,
                SyncMissionsS2CPacket::encode, SyncMissionsS2CPacket::decode,
                SyncMissionsS2CPacket::handle);
        CHANNEL.registerMessage(nextId(), ClaimMissionC2SPacket.class,
                ClaimMissionC2SPacket::encode, ClaimMissionC2SPacket::decode,
                ClaimMissionC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), RequestFeedC2SPacket.class,
                RequestFeedC2SPacket::encode, RequestFeedC2SPacket::decode,
                RequestFeedC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), SyncFeedS2CPacket.class,
                SyncFeedS2CPacket::encode, SyncFeedS2CPacket::decode,
                SyncFeedS2CPacket::handle);
        CHANNEL.registerMessage(nextId(), RequestContractC2SPacket.class,
                RequestContractC2SPacket::encode, RequestContractC2SPacket::decode,
                RequestContractC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), SyncContractS2CPacket.class,
                SyncContractS2CPacket::encode, SyncContractS2CPacket::decode,
                SyncContractS2CPacket::handle);
        CHANNEL.registerMessage(nextId(), FulfillContractC2SPacket.class,
                FulfillContractC2SPacket::encode, FulfillContractC2SPacket::decode,
                FulfillContractC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), RequestSkillsC2SPacket.class,
                RequestSkillsC2SPacket::encode, RequestSkillsC2SPacket::decode,
                RequestSkillsC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), SyncSkillsS2CPacket.class,
                SyncSkillsS2CPacket::encode, SyncSkillsS2CPacket::decode,
                SyncSkillsS2CPacket::handle);
        CHANNEL.registerMessage(nextId(), UpgradeSkillC2SPacket.class,
                UpgradeSkillC2SPacket::encode, UpgradeSkillC2SPacket::decode,
                UpgradeSkillC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), RecycleWasteC2SPacket.class,
                RecycleWasteC2SPacket::encode, RecycleWasteC2SPacket::decode,
                RecycleWasteC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), BuyCampaignC2SPacket.class,
                BuyCampaignC2SPacket::encode, BuyCampaignC2SPacket::decode,
                BuyCampaignC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), RequestMarketingC2SPacket.class,
                RequestMarketingC2SPacket::encode, RequestMarketingC2SPacket::decode,
                RequestMarketingC2SPacket::handle);
        CHANNEL.registerMessage(nextId(), SyncMarketingS2CPacket.class,
                SyncMarketingS2CPacket::encode, SyncMarketingS2CPacket::decode,
                SyncMarketingS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), RequestHabitatGuiC2SPacket.class,
                RequestHabitatGuiC2SPacket::encode, RequestHabitatGuiC2SPacket::decode,
                RequestHabitatGuiC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), OpenHabitatGuiS2CPacket.class,
                OpenHabitatGuiS2CPacket::encode, OpenHabitatGuiS2CPacket::decode,
                OpenHabitatGuiS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), SaveHabitatC2SPacket.class,
                SaveHabitatC2SPacket::encode, SaveHabitatC2SPacket::decode,
                SaveHabitatC2SPacket::handle);

        CHANNEL.registerMessage(nextId(), FamiliarBadgeS2CPacket.class,
                FamiliarBadgeS2CPacket::encode, FamiliarBadgeS2CPacket::decode,
                FamiliarBadgeS2CPacket::handle);

        CHANNEL.registerMessage(nextId(), AdoptBabyC2SPacket.class,
                AdoptBabyC2SPacket::encode, AdoptBabyC2SPacket::decode, AdoptBabyC2SPacket::handle);
    }
}
