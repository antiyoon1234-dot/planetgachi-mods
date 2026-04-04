package com.planetgachi.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.planetgachi.PlanetgachiMod;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class VoteCommand {

    private static final String VOTE_URL    = "https://vdndejdmepjigbrssict.supabase.co/functions/v1/item-vote";
    private static final String MOD_VERSION = "1.2.0";
    private static final int    TIMEOUT_SEC = 8;

    // 투표 쿨다운 (도배 방지 — 10초)
    private static long lastVoteTime = 0;
    private static final long COOLDOWN_MS = 10_000;

    public static int executeVote(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = StringArgumentType.getString(ctx, "item").trim().replace("_", " ");
        long   price    = LongArgumentType.getLong(ctx, "price");
        FabricClientCommandSource source = ctx.getSource();
        MinecraftClient client = source.getClient();

        if (itemName.isEmpty()) { sendHelp(source); return 0; }

        if (itemName.length() > 50) {
            source.sendFeedback(prefix().append(Text.literal("아이템 이름은 50자 이하로 입력해주세요.").formatted(Formatting.RED)));
            return 0;
        }

        if (price > 999_999_999L) {
            source.sendFeedback(prefix().append(Text.literal("가격은 999,999,999 이하로 입력해주세요.").formatted(Formatting.RED)));
            return 0;
        }

        long now = System.currentTimeMillis();
        if (now - lastVoteTime < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastVoteTime)) / 1000 + 1;
            source.sendFeedback(prefix().append(Text.literal(remaining + "초 후에 다시 투표할 수 있습니다.").formatted(Formatting.YELLOW)));
            return 0;
        }
        lastVoteTime = now;

        // 플레이어 이름 가져오기 (없으면 "unknown")
        String playerName = "unknown";
        if (client.player != null) {
            playerName = client.player.getName().getString();
        }

        final String finalPlayerName = playerName;

        source.sendFeedback(
            prefix()
                .append(Text.literal("\"" + itemName + "\"").formatted(Formatting.WHITE))
                .append(Text.literal(" → ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.format("%,d원", price)).formatted(Formatting.GREEN))
                .append(Text.literal(" 투표 전송 중...").formatted(Formatting.GRAY))
        );

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("item_name",   itemName);
                body.addProperty("price",        price);
                body.addProperty("player_name",  finalPlayerName);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VOTE_URL))
                    .header("Content-Type", "application/json")
                    .header("x-mod-version", MOD_VERSION)
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

                int    status  = response.statusCode();
                String resBody = response.body();

                client.execute(() -> {
                    if (status == 429) {
                        source.sendFeedback(prefix().append(Text.literal("투표가 너무 많습니다. 잠시 후 다시 시도해주세요.").formatted(Formatting.YELLOW)));
                        return;
                    }
                    if (status == 409) {
                        source.sendFeedback(prefix().append(Text.literal("이미 해당 아이템에 투표하셨습니다.").formatted(Formatting.YELLOW)));
                        return;
                    }
                    if (status != 200 && status != 201) {
                        source.sendFeedback(prefix().append(Text.literal("서버 오류 (" + status + ")").formatted(Formatting.RED)));
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(resBody).getAsJsonObject();
                        if (json.has("error")) {
                            source.sendFeedback(prefix().append(Text.literal(json.get("error").getAsString()).formatted(Formatting.RED)));
                            return;
                        }
                        sendSuccess(source, itemName, price, json);
                    } catch (Exception e) {
                        source.sendFeedback(prefix().append(Text.literal("응답 파싱 실패").formatted(Formatting.RED)));
                    }
                });

            } catch (java.net.http.HttpTimeoutException e) {
                client.execute(() ->
                    source.sendFeedback(prefix().append(Text.literal("서버 응답 시간 초과.").formatted(Formatting.RED)))
                );
            } catch (Exception e) {
                PlanetgachiMod.LOGGER.error("[Planetgachi] 투표 오류", e);
                client.execute(() ->
                    source.sendFeedback(prefix().append(Text.literal("연결 실패: " + e.getMessage()).formatted(Formatting.RED)))
                );
            }
        });

        return 1;
    }

    private static void sendSuccess(FabricClientCommandSource source, String itemName, long price, JsonObject json) {
        final String div = "━━━━━━━━━━━━━━━━━━━━━━━━━━";

        // 서버에서 집계된 평균/투표수를 내려줄 경우 표시
        long  totalVotes = json.has("total_votes") ? json.get("total_votes").getAsLong() : -1;
        long  avgPrice   = json.has("avg_price")   ? json.get("avg_price").getAsLong()   : -1;

        source.sendFeedback(Text.literal(div).formatted(Formatting.DARK_GREEN));
        source.sendFeedback(Text.literal(" ✅ 투표가 등록되었습니다!").formatted(Formatting.GREEN));
        source.sendFeedback(
            Text.literal(" 아이템: ").formatted(Formatting.GRAY)
                .append(Text.literal(itemName).formatted(Formatting.YELLOW))
        );
        source.sendFeedback(
            Text.literal(" 내 투표 가격: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.format("%,d원", price)).formatted(Formatting.GREEN))
        );
        if (totalVotes >= 0) {
            source.sendFeedback(
                Text.literal(" 총 투표 수: ").formatted(Formatting.GRAY)
                    .append(Text.literal(totalVotes + "표").formatted(Formatting.AQUA))
            );
        }
        if (avgPrice >= 0) {
            source.sendFeedback(
                Text.literal(" 현재 평균가: ").formatted(Formatting.GRAY)
                    .append(Text.literal(String.format("%,d원", avgPrice)).formatted(Formatting.GOLD))
            );
        }
        source.sendFeedback(Text.literal(div).formatted(Formatting.DARK_GREEN));
    }

    public static void sendHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
        source.sendFeedback(Text.literal(" 🗳 Planetgachi 투표 명령어").formatted(Formatting.GREEN));
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
        source.sendFeedback(
            Text.literal(" /gachi vote ").formatted(Formatting.YELLOW)
                .append(Text.literal("<아이템이름> <가격>").formatted(Formatting.WHITE))
        );
        source.sendFeedback(Text.literal(" 아이템의 적정 가격을 투표합니다.").formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("  /gachi vote 다이아몬드 5000").formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.literal("  /gachi vote 네더라이트_주괴 80000").formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.literal(" ※ 띄어쓰기는 _ 언더바로 입력하세요.").formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
    }

    private static MutableText prefix() {
        return Text.literal("[").formatted(Formatting.DARK_GRAY)
            .append(Text.literal("PG").formatted(Formatting.GREEN))
            .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }
}
