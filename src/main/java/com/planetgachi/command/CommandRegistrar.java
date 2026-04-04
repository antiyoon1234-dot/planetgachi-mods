package com.planetgachi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;   //  추가
import com.mojang.brigadier.arguments.LongArgumentType;     //  추가
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
                    .then(ClientCommandManager.argument("item", StringArgumentType.string())   // ✅ 수정
                        .then(ClientCommandManager.argument("price", LongArgumentType.longArg(1)) // ✅ 수정
                            .executes(VoteCommand::executeVote)
                        )
                    )
                )
        );
    }
}
