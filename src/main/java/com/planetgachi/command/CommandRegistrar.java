
package com.planetgachi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class CommandRegistrar {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("gachi")
                .then(ClientCommandManager.literal("vote")
                    .then(ClientCommandManager.argument("item", net.minecraft.command.argument.StringArgumentType.string())
                        .then(ClientCommandManager.argument("price", net.minecraft.command.argument.LongArgumentType.longArg(1))
                            .executes(VoteCommand::executeVote)
                        )
                    )
                )
        );
    }
}
