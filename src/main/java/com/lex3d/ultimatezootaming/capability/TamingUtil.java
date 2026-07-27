package com.lex3d.ultimatezootaming.capability;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Centralise la logique "ce mob devient un familier de ce joueur", utilisee partout
 * (croquette main-a-main, cage, filet, capture natif, adoption bebe) pour etre sur
 * que TOUT taming applique systematiquement les 2 memes choses :
 * 1) Les donnees de possession (TamingData)
 * 2) persistenceRequired(true) pour empecher le despawn naturel -- un familier ne
 *    doit JAMAIS disparaitre tout seul, meme laisse loin du joueur longtemps.
 */
public final class TamingUtil {

    private TamingUtil() {}

    private static final java.util.Random TRAIT_RNG = new java.util.Random();

    public static void tame(Mob mob, UUID ownerUUID, boolean forcedTame) {
        // Loup, chat, perroquet : on laisse le taming vanilla faire son travail
        if (com.lex3d.ultimatezootaming.capability.PetSpecies.isPet(mob)) return;
        mob.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            boolean wasWild = data.getOwnerUUID() == null;
            data.setOwnerUUID(ownerUUID);
            data.setForcedTame(forcedTame);
            data.setTrust(100f);
            // Tire un trait de personnalite UNE SEULE FOIS, a la premiere capture
            // (~55% ordinaire, le reste reparti entre les traits speciaux).
            if (wasWild && data.getTrait() == TamingData.Trait.NONE) {
                data.setTrait(rollTrait());
            }
            // FICHE APPROFONDIE : date de capture + rarete tiree au sort
            if (wasWild && mob.level() instanceof net.minecraft.server.level.ServerLevel sl0) {
                var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl0);
                data.setCaptureDay(ledger.getDay());
                // Rarete au taming : 90% normal, 7% argent, 2% or, 1% albinos
                int roll = TRAIT_RNG.nextInt(100);
                if (roll < 90) data.setRarity(0);
                else if (roll < 97) data.setRarity(1);
                else if (roll < 99) data.setRarity(2);
                else data.setRarity(3);
            }
            // TENDANCES : si cette espece etait reclamee par les visiteurs, on
            // satisfait la demande -> prime en diamants + bonus d'affluence (buzz).
            if (wasWild && mob.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                if (id != null) {
                    var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl);
                    int prime = ledger.fulfillDemand(id.toString());
                    if (prime > 0) {
                        // verse la prime a la caisse du zoo
                        var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock
                                .nearestVault(sl, mob.blockPosition(), 128);
                        if (vault == null) vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock
                                .scanForVault(sl, mob.blockPosition(), 48);
                        if (vault != null) vault.deposit(prime);
                        // message au(x) joueur(s)
                        for (var p : sl.getServer().getPlayerList().getPlayers()) {
                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2605 ")
                                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                                    .append(net.minecraft.network.chat.Component.translatable(
                                            "message.ultimatezootaming.demand_fulfilled",
                                            net.minecraft.network.chat.Component.translatable(mob.getType().getDescriptionId()),
                                            prime).withStyle(net.minecraft.ChatFormatting.YELLOW)));
                        }
                    }
                }
            }
        });
        mob.setPersistenceRequired();
    }

    private static TamingData.Trait rollTrait() {
        float r = TRAIT_RNG.nextFloat();
        if (r < 0.55f) return TamingData.Trait.NONE;
        TamingData.Trait[] specials = {
                TamingData.Trait.GLUTTON, TamingData.Trait.CUDDLY, TamingData.Trait.GRUMPY,
                TamingData.Trait.ENERGETIC, TamingData.Trait.HARDY, TamingData.Trait.SOCIAL,
                TamingData.Trait.CURIOUS, TamingData.Trait.SHY, TamingData.Trait.PLAYFUL
        };
        return specials[TRAIT_RNG.nextInt(specials.length)];
    }

    public static void tame(Mob mob, Player owner, boolean forcedTame) {
        tame(mob, owner.getUUID(), forcedTame);
    }
}
