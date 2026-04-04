package com.planetgachi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
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
                    .then(ClientCommandManager.argument("price", LongArgumentType.longArg(1)) // ✅ 가격 먼저
                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString()) // ✅ 한글/공백 가능
                            .executes(VoteCommand::executeVote)
                        )
                    )
                )
        );
    }
}
