package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "Si le mob possede une mecanique de domptage native : on le laisse faire.
 *  Des qu'il est apprivoise nativement, on intercepte l'evenement pour l'ajouter
 *  silencieusement a notre systeme."
 *
 * NOTE : AnimalTameEvent est l'event Forge standard, la plupart des mods de mobs
 * (Alex's Mobs y compris) passent par TamableAnimal#tame() qui le declenche. Si un
 * mod tiers a une mecanique de taming totalement custom qui ne passe pas par la, il
 * faudra ajouter un hook specifique pour ce mod (a voir au cas par cas).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class PassiveTameEventHandler {

    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        // Un loup, un chat ou un perroquet reste un compagnon VANILLA : le mod
        // ne l'absorbe pas. Sans ce filtre, apprivoiser un chien en faisait un
        // pensionnaire du zoo avec fiche, bien-etre et soigneur attitre.
        if (com.lex3d.ultimatezootaming.capability.PetSpecies.isPet(event.getAnimal())) return;
        Player player = event.getTamer();

        event.getAnimal().getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            data.setOwnerUUID(player.getUUID());
            data.setForcedTame(false);
            data.setTrust(100f);
            event.getAnimal().setPersistenceRequired();
            ZooSavedData.get((ServerLevel) event.getEntity().level())
                    .addFamiliar(player.getUUID(), event.getAnimal().getUUID());
        });
    }
}
