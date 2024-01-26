package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.util.*;

public class ScratchGame {

    public static void main(String[] args) {
        if (args.length != 4 || !args[0].equals("--config") || !args[2].equals("--betting-amount")) {
            System.out.println("Invalid arguments. Usage: java -jar <your-jar-file> --config config.json --betting-amount 100");
            System.exit(1);
        }

        String configFile = args[1];
        int bettingAmount = Integer.parseInt(args[3]);

        ScratchGame scratchGame = new ScratchGame();
        scratchGame.playGame(configFile, bettingAmount);
    }

    public void playGame(String configFile, int bettingAmount) {
        try {
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);

            int columns = config.has("columns") ? config.get("columns").getAsInt() : 3;
            int rows = config.has("rows") ? config.get("rows").getAsInt() : 3;

            List<List<String>> matrix = generateMatrix(config, columns, rows);

            JsonObject result = new JsonObject();
            result.add("matrix", convertMatrixToJsonArray(matrix));

            Map<String, List<String>> appliedWinningCombinations = new HashMap<>();
            StringBuilder appliedBonusSymbol = new StringBuilder();

            int reward = calculateReward(matrix, config, bettingAmount, appliedWinningCombinations, appliedBonusSymbol);

            result.addProperty("reward", reward);
            result.add("applied_winning_combinations", convertWinningCombinationsToJson(appliedWinningCombinations));
            result.addProperty("applied_bonus_symbol", appliedBonusSymbol.toString().trim());

            System.out.println(result);
        } catch (Exception e) {
            System.out.println("Error during game execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<List<String>> generateMatrix(JsonObject config, int columns, int rows) {
        List<List<String>> matrix = new ArrayList<>();

        JsonArray standardSymbolsProbabilities = config.getAsJsonObject("probabilities").getAsJsonArray("standard_symbols");
        JsonObject bonusSymbolsConfig = config.getAsJsonObject("probabilities").getAsJsonObject("bonus_symbols");
        JsonObject bonusSymbolsProbabilities = bonusSymbolsConfig.getAsJsonObject("symbols");

        Random random = new Random();

        for (int i = 0; i < rows; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < columns; j++) {
                JsonObject cellConfig = findCellConfig(i, j, standardSymbolsProbabilities);
                JsonObject symbolsProbabilities = cellConfig.getAsJsonObject("symbols");
                String symbol = chooseSymbol(random, mergeJsonObjects(symbolsProbabilities, bonusSymbolsProbabilities));
                row.add(symbol);
            }
            matrix.add(row);
        }

        return matrix;
    }

    public static JsonObject mergeJsonObjects(JsonObject json1, JsonObject json2) {
        JsonObject mergedJson = new JsonObject();

        for (var entry : json1.entrySet()) {
            mergedJson.add(entry.getKey(), entry.getValue());
        }

        for (var entry : json2.entrySet()) {
            mergedJson.add(entry.getKey(), entry.getValue());
        }

        return mergedJson;
    }

    public JsonObject findCellConfig(int row, int column, JsonArray standardSymbolsProbabilities) {
        for (JsonElement element : standardSymbolsProbabilities) {
            JsonObject cellConfig = element.getAsJsonObject();
            if (cellConfig.get("row").getAsInt() == row && cellConfig.get("column").getAsInt() == column) {
                return cellConfig;
            }
        }

        System.out.println("Default configuration used for row " + row + " and column " + column);

        return getDefaultCellConfig();
    }

    public JsonObject getDefaultCellConfig() {
        JsonObject defaultConfig = new JsonObject();
        defaultConfig.addProperty("row", 0);
        defaultConfig.addProperty("column", 0);
        defaultConfig.add("symbols", new JsonObject());
        return defaultConfig;
    }

    public String chooseSymbol(Random random, JsonObject symbolsProbabilities) {
        int totalProbability = symbolsProbabilities.entrySet().stream()
                .mapToInt(entry -> entry.getValue().getAsInt())
                .sum();

        if (totalProbability > 0) {
            int randomNumber = random.nextInt(totalProbability) + 1;

            for (Map.Entry<String, JsonElement> entry : symbolsProbabilities.entrySet()) {
                int probability = entry.getValue().getAsInt();
                if (randomNumber <= probability) {
                    return entry.getKey();
                }
                randomNumber -= probability;
            }
        } else {
            System.out.println("No symbols with positive probability. Returning a default symbol.");
            return "defaultSymbol";
        }

        throw new IllegalStateException("Symbol choice logic error");
    }

    public JsonArray convertMatrixToJsonArray(List<List<String>> matrix) {
        JsonArray jsonArray = new JsonArray();
        for (List<String> row : matrix) {
            JsonArray jsonRow = new JsonArray();
            for (String element : row) {
                jsonRow.add(element);
            }
            jsonArray.add(jsonRow);
        }
        return jsonArray;
    }

    public JsonArray convertWinningCombinationsToJson(Map<String, List<String>> appliedWinningCombinations) {
        JsonArray jsonArray = new JsonArray();
        for (Map.Entry<String, List<String>> entry : appliedWinningCombinations.entrySet()) {
            JsonObject jsonEntry = new JsonObject();
            jsonEntry.addProperty(entry.getKey(), String.join(",", entry.getValue()));
            jsonArray.add(jsonEntry);
        }
        return jsonArray;
    }

    public int calculateReward(List<List<String>> matrix, JsonObject config, int bettingAmount,
                                Map<String, List<String>> appliedWinningCombinations, StringBuilder appliedBonusSymbol) {
        int reward = 0;

        JsonObject winCombinations = config.getAsJsonObject("win_combinations");
        JsonObject symbols = config.getAsJsonObject("symbols");

        reward = getReward(matrix, bettingAmount, appliedWinningCombinations, winCombinations, symbols, reward);
        reward = getBonusReward(matrix, appliedBonusSymbol, reward, symbols);

        return reward;
    }

    public static int getBonusReward(List<List<String>> matrix, StringBuilder appliedBonusSymbol, int reward, JsonObject symbols) {
        if (reward != 0) {
            for (List<String> strings : matrix) {
                for (String string : strings) {
                    JsonObject symbolDetails = symbols.getAsJsonObject(string);
                    if (symbolDetails != null && symbolDetails.has("type") &&
                            symbolDetails.get("type").getAsString().equals("bonus")) {

                        String bonusType = symbolDetails.get("impact").getAsString();

                        switch (bonusType) {
                            case "multiply_reward":
                                double multiplier = symbolDetails.get("reward_multiplier").getAsDouble();
                                reward *= multiplier;
                                appliedBonusSymbol.append(string).append(" ");
                                break;
                            case "extra_bonus":
                                int extraAmount = symbolDetails.get("extra").getAsInt();
                                reward += extraAmount;
                                appliedBonusSymbol.append(string).append(" ");

                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return reward;
    }

    public int getReward(List<List<String>> matrix, int bettingAmount, Map<String, List<String>> appliedWinningCombinations, JsonObject winCombinations, JsonObject symbols, int reward) {
        for (Map.Entry<String, JsonElement> entry : winCombinations.entrySet()) {
            String winCombinationKey = entry.getKey();
            JsonObject winCombinationDetails = entry.getValue().getAsJsonObject();

            if (winCombinationDetails.has("when") && winCombinationDetails.get("when").getAsString().equals("same_symbols")) {
                int count = winCombinationDetails.get("count").getAsInt();

                Map<String, List<String>> symbolToMatchingGroups = new HashMap<>();

                for (List<String> row : matrix) {
                    for (String symbol : row) {
                        List<String> matchingGroup = findMatchingSymbols(matrix, winCombinationDetails);
                        symbolToMatchingGroups.computeIfAbsent(symbol, k -> new ArrayList<>()).addAll(matchingGroup);
                    }
                }

                for (Map.Entry<String, List<String>> symbolEntry : symbolToMatchingGroups.entrySet()) {
                    List<String> matchingSymbols = symbolEntry.getValue();
                    if (matchingSymbols.size() >= count) {
                        List<String> matchingGroup = findMatchingGroup(matchingSymbols, symbolEntry.getKey(), count);
                        if (!matchingGroup.isEmpty()) {
                            String symbol = matchingGroup.get(0);
                            JsonElement symbolElement = symbols.getAsJsonObject(symbol).get("reward_multiplier");

                            double symbolRewardMultiplier = (symbolElement != null && symbolElement.isJsonPrimitive())
                                    ? symbolElement.getAsJsonPrimitive().getAsDouble()
                                    : 0.0;

                            double winCombinationRewardMultiplier = winCombinationDetails.get("reward_multiplier").getAsDouble();

                            for (String matchingSymbol : matchingGroup) {
                                JsonObject symbolDetails = symbols.getAsJsonObject(matchingSymbol);
                                if (symbolDetails != null && symbolDetails.has("reward_multiplier")) {
                                    reward += bettingAmount * symbolRewardMultiplier * winCombinationRewardMultiplier;
                                    appliedWinningCombinations.computeIfAbsent(winCombinationKey, k -> new ArrayList<>()).add(matchingSymbol);
                                }
                            }
                        }
                    }
                }

            } else if (winCombinationDetails.has("when") && winCombinationDetails.get("when").getAsString().equals("linear_symbols")) {
                JsonElement countElement = winCombinationDetails.get("count");
                int count = (countElement != null && !countElement.isJsonNull()) ? countElement.getAsInt() : 0;

                for (int col = 0; col < matrix.get(0).size(); col++) {
                    int consecutiveCount = 0;
                    String currentSymbol = null;

                    for (int row = 0; row < matrix.size(); row++) {
                        String symbol = matrix.get(row).get(col);

                        if (symbol.equals(currentSymbol)) {
                            consecutiveCount++;
                        } else {
                            consecutiveCount = 1;
                            currentSymbol = symbol;
                        }

                        if (consecutiveCount == count) {
                            for (int i = 0; i < count; i++) {
                                reward += bettingAmount * symbols.getAsJsonObject(symbol).get("reward_multiplier").getAsDouble() *
                                        winCombinationDetails.get("reward_multiplier").getAsDouble();
                                appliedWinningCombinations.computeIfAbsent(winCombinationKey, k -> new ArrayList<>()).add(symbol);
                            }
                            consecutiveCount = 0;
                        }
                    }
                }
            }
        }
        return reward;
    }

    public List<String> findMatchingSymbols(List<List<String>> matrix, JsonObject winCombinationDetails) {
        List<String> matchingSymbols = new ArrayList<>();

        if (winCombinationDetails.has("when") && winCombinationDetails.get("when").getAsString().equals("same_symbols")) {
            int count = winCombinationDetails.get("count").getAsInt();

            Map<String, Integer> symbolCount = new HashMap<>();

            for (List<String> row : matrix) {
                for (String symbol : row) {
                    symbolCount.put(symbol, symbolCount.getOrDefault(symbol, 0) + 1);
                    if (symbolCount.get(symbol) == count) {
                        matchingSymbols.add(symbol);
                    }
                }
            }

            matchingSymbols.removeIf(symbol -> symbolCount.get(symbol) > count);

            return matchingSymbols;
        } else {
            return new ArrayList<>();
        }
    }

    public List<String> findMatchingGroup(List<String> row, String symbol, int count) {
        List<String> matchingGroup = new ArrayList<>();
        int consecutiveCount = 0;

        for (String currentSymbol : row) {
            if (currentSymbol.equals(symbol)) {
                consecutiveCount++;
                if (consecutiveCount == count) {
                    if (matchingGroup.isEmpty() || !matchingGroup.get(matchingGroup.size() - 1).equals(symbol)) {
                        matchingGroup.add(symbol);
                    }
                    consecutiveCount = 0;
                }
            } else {
                consecutiveCount = 0;
            }
        }

        return matchingGroup;
    }
}