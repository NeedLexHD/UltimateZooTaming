package com.lex3d.ultimatezootaming.command;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.OpenHabitatGuiS2CPacket;
import com.lex3d.ultimatezootaming.welfare.HabitatManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** /zootame habitats (admin, perm 2) : ouvre le GUI d'assignation habitat+regime par mob. */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZooCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        HabitatManager.load(event.getServer());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zootame")
                .then(Commands.literal("habitats")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                    new OpenHabitatGuiS2CPacket(HabitatManager.all()));
                            return 1;
                        })));
    }

}
