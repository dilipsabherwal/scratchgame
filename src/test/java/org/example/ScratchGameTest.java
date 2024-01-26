package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ScratchGameTest {

    @Test
    void calculateReward_WithValidInputs_ReturnsCorrectReward() {
        JsonObject config = createTestConfig();
        List<List<String>> matrix = createTestMatrix();
        int bettingAmount = 100;

        ScratchGame scratchGame = new ScratchGame();
        StringBuilder appliedBonusSymbol = new StringBuilder();
        int reward = scratchGame.calculateReward(matrix, config, bettingAmount, new HashMap<>(), appliedBonusSymbol);

        assertEquals(300, reward);
    }

    @Test
    void generateMatrix_WithValidConfig_ReturnsMatrixWithCorrectSize() {
        JsonObject config = createTestConfig();
        int columns = 3;
        int rows = 3;

        ScratchGame scratchGame = new ScratchGame();
        List<List<String>> matrix = scratchGame.generateMatrix(config, columns, rows);

        assertEquals(rows, matrix.size());
        assertEquals(columns, matrix.get(0).size());
    }

    @Test
    void chooseSymbol_WithValidProbabilities_ReturnsSymbol() {
        JsonObject symbolsProbabilities = new JsonObject();
        symbolsProbabilities.addProperty("symbolA", 50);
        symbolsProbabilities.addProperty("symbolB", 50);

        ScratchGame scratchGame = new ScratchGame();
        String symbol = scratchGame.chooseSymbol(new Random(), symbolsProbabilities);

        assertTrue(symbolsProbabilities.has(symbol));
    }

    @Test
    void convertMatrixToJsonArray_WithValidMatrix_ReturnsJsonArray() {
        List<List<String>> matrix = createTestMatrix();

        ScratchGame scratchGame = new ScratchGame();
        JsonArray result = scratchGame.convertMatrixToJsonArray(matrix).getAsJsonArray();

        assertFalse(result.isEmpty());

        for (JsonElement element : result) {
            assertTrue(element.isJsonArray());
            JsonArray row = element.getAsJsonArray();
            assertFalse(row.isEmpty());
            for (JsonElement symbol : row) {
                assertTrue(symbol.isJsonPrimitive());
            }
        }
    }


    private JsonObject createTestConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("columns", 3);
        config.addProperty("rows", 3);

        JsonObject probabilities = new JsonObject();
        JsonArray standardSymbols = new JsonArray();
        JsonObject symbolProbabilities = new JsonObject();
        symbolProbabilities.addProperty("symbolA", 50);
        symbolProbabilities.addProperty("symbolB", 50);

        standardSymbols.add(createTestCellConfig(0, 0, symbolProbabilities));
        standardSymbols.add(createTestCellConfig(0, 1, symbolProbabilities));
        standardSymbols.add(createTestCellConfig(0, 2, symbolProbabilities));

        probabilities.add("standard_symbols", standardSymbols);

        JsonObject bonusSymbols = new JsonObject();
        JsonObject bonusSymbolProbabilities = new JsonObject();
        bonusSymbolProbabilities.addProperty("bonusSymbolA", 50);
        bonusSymbolProbabilities.addProperty("bonusSymbolB", 50);
        bonusSymbols.add("symbols", bonusSymbolProbabilities);
        bonusSymbols.addProperty("impact", "multiply_reward");
        bonusSymbols.addProperty("type", "bonus");
        bonusSymbols.addProperty("reward_multiplier", 2.0);
        bonusSymbols.addProperty("extra", 100);

        probabilities.add("bonus_symbols", bonusSymbols);

        config.add("probabilities", probabilities);

        JsonObject winCombinations = new JsonObject();
        JsonObject winCombinationDetails = new JsonObject();
        winCombinationDetails.addProperty("when", "same_symbols");
        winCombinationDetails.addProperty("count", 3);
        winCombinationDetails.addProperty("reward_multiplier", 1.0);
        winCombinationDetails.addProperty("when", "same_symbols");
        winCombinationDetails.addProperty("count", 4);
        winCombinationDetails.addProperty("reward_multiplier", 2.0);
        winCombinationDetails.addProperty("when", "same_symbols");
        winCombinationDetails.addProperty("count", 5);
        winCombinationDetails.addProperty("reward_multiplier", 3.0);

        winCombinations.add("combinationA", winCombinationDetails);
        config.add("win_combinations", winCombinations);

        JsonObject symbols = new JsonObject();
        JsonObject symbolDetailsA = new JsonObject();
        symbolDetailsA.addProperty("type", "normal");
        symbolDetailsA.addProperty("reward_multiplier", 1.0);
        symbols.add("symbolA", symbolDetailsA);

        JsonObject symbolDetailsB = new JsonObject();
        symbolDetailsB.addProperty("type", "normal");
        symbolDetailsB.addProperty("reward_multiplier", 1.0);
        symbols.add("symbolB", symbolDetailsB);

        symbols.add("bonusSymbolA", bonusSymbols);

        config.add("symbols", symbols);

        return config;
    }

    private JsonObject createTestCellConfig(int row, int column, JsonObject symbolsProbabilities) {
        JsonObject cellConfig = new JsonObject();
        cellConfig.addProperty("row", row);
        cellConfig.addProperty("column", column);
        cellConfig.add("symbols", symbolsProbabilities);
        return cellConfig;
    }

    private List<List<String>> createTestMatrix() {
        List<List<String>> matrix = List.of(
                List.of("symbolA", "symbolA", "symbolA"),
                List.of("symbolB", "symbolB", "symbolB"),
                List.of("symbolA", "symbolB", "symbolA")
        );
        return matrix;
    }
}
