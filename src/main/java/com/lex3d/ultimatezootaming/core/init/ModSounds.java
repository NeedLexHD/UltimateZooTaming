package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sons custom du mod (synthetises, fichiers .ogg dans assets/.../sounds/).
 * cage_slam : claquement metallique de la porte guillotine
 * cage_fail : mecanisme a cliquet qui remonte + grincement
 * cage_success : carillon de capture reussie
 * whistle_blow : coup de sifflet a bille (a l'ouverture du GUI)
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UltimateZooTame.MODID);

    public static final RegistryObject<SoundEvent> CAGE_SLAM = register("cage_slam");
    public static final RegistryObject<SoundEvent> CAGE_FAIL = register("cage_fail");
    public static final RegistryObject<SoundEvent> CAGE_SUCCESS = register("cage_success");
    public static final RegistryObject<SoundEvent> WHISTLE_BLOW = register("whistle_blow");
    public static final RegistryObject<SoundEvent> TAME_SUCCESS = register("tame_success");
    public static final RegistryObject<SoundEvent> TAME_FAIL = register("tame_fail");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(UltimateZooTame.MODID, name)));
    }
}
