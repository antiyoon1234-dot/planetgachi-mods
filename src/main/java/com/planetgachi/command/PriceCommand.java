package com.planetgachi.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.planetgachi.PlanetgachiMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class PriceCommand {

    // anon key 없음 — Edge Function URL만 사용 (키는 서버에서만 보관)
    private static final String EDGE_URL    = "https://vdndejdmepjigbrssict.supabase.co/functions/v1/item-search";
    private static final String MOD_VERSION = "1.0.2";
    private static final int    MAX_RESULTS = 5;
    private static final int    TIMEOUT_SEC = 8;

    // 클라이언트 쿨다운 (도배 방지)
    private static long lastRequestTime = 0;
    private static final long COOLDOWN_MS = 1_500;

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("gachi")
                    .then(ClientCommandManager.literal("가격조회")
                        .then(ClientCommandManager.argument("아이템이름", StringArgumentType.greedyString())
                            .executes(PriceCommand::executeSearch)
                        )
                    )
                    .executes(ctx -> { sendHelp(ctx.getSource()); return 1; })
            )
        );
    }

    private static int executeSearch(CommandContext<FabricClientCommandSource> ctx) {
        String raw = StringArgumentType.getString(ctx, "아이템이름").trim();
        FabricClientCommandSource source = ctx.getSource();
        MinecraftClient client = source.getClient();

        if (raw.isEmpty()) { sendHelp(source); return 0; }

        if (raw.length() > 50) {
            source.sendFeedback(prefix().append(Text.literal("검색어는 50자 이하로 입력해주세요.").formatted(Formatting.RED)));
            return 0;
        }

        long now = System.currentTimeMillis();
        if (now - lastRequestTime < COOLDOWN_MS) {
            source.sendFeedback(prefix().append(Text.literal("잠시 후 다시 시도해주세요.").formatted(Formatting.YELLOW)));
            return 0;
        }
        lastRequestTime = now;

        source.sendFeedback(
            prefix()
                .append(Text.literal("\"" + raw + "\"").formatted(Formatting.WHITE))
                .append(Text.literal(" 검색 중...").formatted(Formatting.GRAY))
        );

        CompletableFuture.runAsync(() -> {
            try {
                String url = EDGE_URL + "?q=" + URLEncoder.encode(raw, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-mod-version", MOD_VERSION)
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .GET()
                    .build();

                HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                String body = response.body();

                client.execute(() -> {
                    if (status == 429) {
                        source.sendFeedback(prefix().append(Text.literal("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.").formatted(Formatting.YELLOW)));
                        return;
                    }
                    if (status != 200) {
                        source.sendFeedback(prefix().append(Text.literal("서버 오류 (" + status + ")").formatted(Formatting.RED)));
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        if (json.has("error")) {
                            source.sendFeedback(prefix().append(Text.literal(json.get("error").getAsString()).formatted(Formatting.RED)));
                            return;
                        }
                        displayResults(source, raw, json.getAsJsonArray("results"));
                    } catch (Exception e) {
                        source.sendFeedback(prefix().append(Text.literal("응답 파싱 실패").formatted(Formatting.RED)));
                    }
                });

            } catch (java.net.http.HttpTimeoutException e) {
                client.execute(() ->
                    source.sendFeedback(prefix().append(Text.literal("서버 응답 시간 초과.").formatted(Formatting.RED)))
                );
            } catch (Exception e) {
                PlanetgachiMod.LOGGER.error("[Planetgachi] API 오류", e);
                client.execute(() ->
                    source.sendFeedback(prefix().append(Text.literal("연결 실패: " + e.getMessage()).formatted(Formatting.RED)))
                );
            }
        });

        return 1;
    }

    private static void displayResults(FabricClientCommandSource source, String query, JsonArray items) {
        final String div = "━━━━━━━━━━━━━━━━━━━━━━━━━━";

        if (items == null || items.isEmpty()) {
            source.sendFeedback(prefix().append(Text.literal("\"" + query + "\" 에 해당하는 아이템이 없습니다.").formatted(Formatting.RED)));
            return;
        }

        source.sendFeedback(Text.literal(div).formatted(Formatting.DARK_GREEN));
        source.sendFeedback(Text.literal(" 🌐 Planetgachi 가격 조회").formatted(Formatting.GREEN));
        source.sendFeedback(
            Text.literal(" 검색어: ").formatted(Formatting.GRAY)
                .append(Text.literal("\"" + query + "\"").formatted(Formatting.WHITE))
                .append(Text.literal("  " + items.size() + "개").formatted(Formatting.DARK_GRAY))
        );
        source.sendFeedback(Text.literal(div).formatted(Formatting.DARK_GREEN));

        for (JsonElement el : items) {
            try {
                JsonObject item  = el.getAsJsonObject();
                String name      = item.get("name").getAsString();
                long   value     = item.get("current_value").getAsLong();
                String unitType  = (item.has("unit_type") && !item.get("unit_type").isJsonNull())
                                   ? item.get("unit_type").getAsString() : "piece";

                source.sendFeedback(
                    Text.literal(" ▸ ").formatted(Formatting.GOLD)
                        .append(Text.literal(name).formatted(Formatting.YELLOW))
                        .append(Text.literal("  →  ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(String.format("%,d원", value)).formatted(Formatting.GREEN))
                        .append(Text.literal(" / " + getUnitLabel(unitType)).formatted(Formatting.AQUA))
                );
            } catch (Exception e) {
                PlanetgachiMod.LOGGER.warn("[Planetgachi] 결과 파싱 오류: {}", e.getMessage());
            }
        }

        source.sendFeedback(Text.literal(div).formatted(Formatting.DARK_GREEN));
        if (items.size() == MAX_RESULTS) {
            source.sendFeedback(Text.literal(" 상위 " + MAX_RESULTS + "개만 표시됩니다. 이름을 더 구체적으로 입력하세요.").formatted(Formatting.DARK_GRAY));
        }
    }

    private static void sendHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
        source.sendFeedback(Text.literal(" 🌐 Planetgachi 명령어 도움말").formatted(Formatting.GREEN));
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
        source.sendFeedback(Text.literal(" /gachi 가격조회 ").formatted(Formatting.YELLOW).append(Text.literal("<아이템이름>").formatted(Formatting.WHITE)));
        source.sendFeedback(Text.literal(" 이름 일부만 입력해도 검색됩니다.").formatted(Formatting.GRAY));
        source.sendFeedback(Text.literal("  /gachi 가격조회 다이아").formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.literal("  /gachi 가격조회 네더라이트").formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GREEN));
    }

    private static MutableText prefix() {
        return Text.literal("[").formatted(Formatting.DARK_GRAY)
            .append(Text.literal("PG").formatted(Formatting.GREEN))
            .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }

    private static String getUnitLabel(String unitType) {
        return switch (unitType) {
            case "stack", "stack_64" -> "64개";
            case "set",   "stack_16" -> "16개";
            default                  -> "1개";
        };
    }
}
