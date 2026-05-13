package com.keet.ezgt;

import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;

public final class RecipeModifier {

    private static final String[] ORE_PROCESSING_BONUS_CHANCE_RECIPE_MAPS = { "gt.recipe.macerator",
        "gt.recipe.orewasher", "gt.recipe.centrifuge", "gt.recipe.thermalcentrifuge", "gt.recipe.chemicalbath",
        "gt.recipe.electromagneticseparator" };

    private static final String[] ORE_PROCESSING_INPUT_PREFIXES = { "ore", "rawOre", "crushed", "crushedPurified",
        "crushedCentrifuged", "cleanGravel", "dirtyGravel", "reduced", "dustImpure", "dustPure", "shard", "clump",
        "crystalline", "dustRefined" };

    private static boolean modifiedRecipes;

    private RecipeModifier() {}

    public static void applyConfiguredRecipeUpdates() {
        if (modifiedRecipes) return;

        modifiedRecipes = true;
        EZGT.LOG.info("Applying configured EZGT recipe updates");

        modifyCasingRecipes();
        modifyCasingCraftingRecipes();
        modifyOreProcessingBonusChances();
        modifyCircuitRecipes();
        modifyMotorRecipes();
    }

    private static void modifyCasingRecipes() {
        if (ModConfig.Rates.gtCasingAssemblerOutputs == 1.0f) return;

        RecipeMap<?> assembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.assembler");
        if (assembler == null) return;

        int count = 0;
        for (GTRecipe recipe : assembler.getAllRecipes()) {
            ItemStack output = firstOutput(recipe);
            if (output != null && isGtCasingOutput(output)) {
                multiplyStackSize(output, ModConfig.Rates.gtCasingAssemblerOutputs);
                count++;
            }
        }

        EZGT.LOG.info("Modified {} GT casing assembler recipes", count);
    }

    private static boolean isGtCasingOutput(ItemStack output) {
        for (int id : OreDictionary.getOreIDs(output)) {
            if (isBlockCasingsName(OreDictionary.getOreName(id))) return true;
        }

        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(output.getItem());
        return identifier != null && isBlockCasingsName(identifier.toString());
    }

    private static boolean isBlockCasingsName(String name) {
        if (name == null) return false;

        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("blockcasings5")) return ModConfig.Rates.easyCoils;

        return lower.contains("blockcasings");
    }

    private static boolean hasOreDictionaryPrefix(ItemStack stack, String[] prefixes) {
        if (stack == null) return false;

        for (int id : OreDictionary.getOreIDs(stack)) {
            String oreName = OreDictionary.getOreName(id);
            for (String prefix : prefixes) {
                if (matchesOreDictionaryPrefix(oreName, prefix)) return true;
            }
        }

        return false;
    }

    private static boolean matchesOreDictionaryPrefix(String oreName, String prefix) {
        return oreName != null && oreName.length() > prefix.length()
            && oreName.startsWith(prefix)
            && Character.isUpperCase(oreName.charAt(prefix.length()));
    }

    private static void modifyCasingCraftingRecipes() {
        if (ModConfig.Rates.gtCasingAssemblerOutputs == 1.0f) return;

        int count = 0;
        for (IRecipe recipe : craftingRecipes()) {
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && isGtCasingOutput(output)) {
                multiplyStackSize(output, ModConfig.Rates.gtCasingAssemblerOutputs);
                count++;
            }
        }

        EZGT.LOG.info("Modified {} GT casing crafting recipes", count);
    }

    private static void modifyOreProcessingBonusChances() {
        if (ModConfig.Rates.oreProcessingBonusOutputChances == 1.0f) return;

        int recipeCount = 0;
        int chanceCount = 0;
        for (String mapName : ORE_PROCESSING_BONUS_CHANCE_RECIPE_MAPS) {
            RecipeMap<?> recipeMap = RecipeMap.ALL_RECIPE_MAPS.get(mapName);
            if (recipeMap == null) continue;

            for (GTRecipe recipe : recipeMap.getAllRecipes()) {
                if (!hasOreProcessingInput(recipe)) continue;

                int modifiedChances = multiplyBonusOutputChances(recipe);
                if (modifiedChances > 0) {
                    recipeCount++;
                    chanceCount += modifiedChances;
                }
            }
        }

        EZGT.LOG.info("Modified {} GT ore processing bonus output chances across {} recipes", chanceCount, recipeCount);
    }

    private static boolean hasOreProcessingInput(GTRecipe recipe) {
        if (recipe == null || recipe.mInputs == null) return false;

        for (ItemStack input : recipe.mInputs) {
            if (hasOreDictionaryPrefix(input, ORE_PROCESSING_INPUT_PREFIXES)) return true;
        }

        return false;
    }

    private static int multiplyBonusOutputChances(GTRecipe recipe) {
        if (recipe.mOutputs == null || recipe.mOutputChances == null) return 0;

        int count = 0;
        int limit = Math.min(recipe.mOutputs.length, recipe.mOutputChances.length);
        for (int i = 0; i < limit; i++) {
            int chance = recipe.mOutputChances[i];
            if (recipe.mOutputs[i] != null && chance > 0 && chance < 10000) {
                recipe.mOutputChances[i] = clampedChance(chance, ModConfig.Rates.oreProcessingBonusOutputChances);
                count++;
            }
        }

        return count;
    }

    private static void modifyCircuitRecipes() {
        if (!ModConfig.Rates.easyCircuits) return;

        RecipeMap<?> circuitAssembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.circuitassembler");
        RecipeMap<?> cal = RecipeMap.ALL_RECIPE_MAPS.get("bw.recipe.cal");
        if (circuitAssembler == null && cal == null) return;

        ItemStack ic2Basic = GTModHandler.getIC2Item("electronicCircuit", 1L);

        ItemStack[] processorOutputs = { ic2Basic, ItemList.Circuit_Basic.get(1L),
            ItemList.Circuit_Microprocessor.get(2L), ItemList.Circuit_Processor.get(1L),
            ItemList.Circuit_Nanoprocessor.get(1L), ItemList.Circuit_Quantumprocessor.get(1L),
            ItemList.Circuit_Crystalprocessor.get(1), ItemList.Circuit_Neuroprocessor.get(1) };

        ItemStack[] assemblyOutputs = { ItemList.Circuit_Integrated_Good.get(1L), ItemList.Circuit_Computer.get(1L),
            ItemList.Circuit_Nanocomputer.get(1L), ItemList.Circuit_Quantumcomputer.get(1L),
            ItemList.Circuit_Crystalcomputer.get(1), ItemList.Circuit_Wetwarecomputer.get(1) };

        ItemStack[] socInputs = { ItemList.Circuit_Chip_Simple_SoC.get(1L), ItemList.Circuit_Chip_SoC.get(1L),
            ItemList.Circuit_Chip_SoC2.get(1L), ItemList.Circuit_Chip_CrystalSoC.get(1L) };

        RecipeCounts counts = new RecipeCounts();

        if (circuitAssembler != null)
            applyCircuitMultipliers(circuitAssembler, processorOutputs, assemblyOutputs, socInputs, 2, 4, 2, counts);
        if (cal != null) applyCircuitMultipliers(cal, processorOutputs, assemblyOutputs, socInputs, 32, 32, 32, counts);

        EZGT.LOG.info(
            "Applied easy circuit output ratios to {} processor recipes ({} SoC) and {} assembly recipes",
            counts.processorRecipes,
            counts.socProcessorRecipes,
            counts.assemblyRecipes);
    }

    private static void applyCircuitMultipliers(RecipeMap<?> map, ItemStack[] processorOutputs,
        ItemStack[] assemblyOutputs, ItemStack[] socInputs, int processorAmt, int socProcessorAmt, int assemblyAmt,
        RecipeCounts counts) {
        for (GTRecipe recipe : map.getAllRecipes()) {
            ItemStack output = firstOutput(recipe);
            if (output == null) continue;

            if (matchesAny(output, processorOutputs)) {
                boolean isSocRecipe = hasInput(recipe, socInputs);
                output.stackSize = clampedStackSize(output, isSocRecipe ? socProcessorAmt : processorAmt);
                counts.processorRecipes++;
                if (isSocRecipe) counts.socProcessorRecipes++;
            } else if (matchesAny(output, assemblyOutputs)) {
                output.stackSize = clampedStackSize(output, assemblyAmt);
                counts.assemblyRecipes++;
            }
        }
    }

    private static void modifyMotorRecipes() {
        if (ModConfig.Rates.easyMotors == 1.0f) return;

        ItemStack[] motorOutputs = { ItemList.Electric_Motor_LV.get(1L), ItemList.Electric_Motor_MV.get(1L),
            ItemList.Electric_Motor_HV.get(1L), ItemList.Electric_Motor_EV.get(1L), ItemList.Electric_Motor_IV.get(1L),
            ItemList.Electric_Motor_LuV.get(1L), ItemList.Electric_Motor_ZPM.get(1L),
            ItemList.Electric_Motor_UV.get(1L), ItemList.Electric_Motor_UHV.get(1L),
            ItemList.Electric_Motor_UEV.get(1L), ItemList.Electric_Motor_UIV.get(1L),
            ItemList.Electric_Motor_UMV.get(1L), ItemList.Electric_Motor_UXV.get(1L) };

        int assemblerCount = 0;
        int craftingCount = 0;

        RecipeMap<?> assembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.assembler");
        if (assembler != null) {
            for (GTRecipe recipe : assembler.getAllRecipes()) {
                ItemStack output = firstOutput(recipe);
                if (output != null && matchesAny(output, motorOutputs)) {
                    multiplyStackSize(output, ModConfig.Rates.easyMotors);
                    assemblerCount++;
                }
            }
        }

        for (IRecipe recipe : craftingRecipes()) {
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && matchesAny(output, motorOutputs)) {
                multiplyStackSize(output, ModConfig.Rates.easyMotors);
                craftingCount++;
            }
        }

        EZGT.LOG.info("Modified {} GT motor assembler recipes and {} crafting recipes", assemblerCount, craftingCount);
    }

    private static List<IRecipe> craftingRecipes() {
        @SuppressWarnings("unchecked")
        List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        return recipes;
    }

    private static ItemStack firstOutput(GTRecipe recipe) {
        if (recipe == null || recipe.mOutputs == null || recipe.mOutputs.length == 0) return null;

        return recipe.mOutputs[0];
    }

    private static boolean hasInput(GTRecipe recipe, ItemStack[] inputs) {
        if (recipe.mInputs == null) return false;

        for (ItemStack input : recipe.mInputs) {
            if (matchesAny(input, inputs)) return true;
        }

        return false;
    }

    private static boolean matchesAny(ItemStack stack, ItemStack[] targets) {
        if (stack == null || targets == null) return false;

        for (ItemStack target : targets) {
            if (matchesItem(stack, target)) return true;
        }

        return false;
    }

    private static boolean matchesItem(ItemStack stack, ItemStack target) {
        if (target == null || stack.getItem() != target.getItem()) return false;

        return target.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || stack.getItemDamage() == target.getItemDamage();
    }

    private static void multiplyStackSize(ItemStack output, float multiplier) {
        output.stackSize = clampedStackSize(output, (int) (output.stackSize * multiplier));
    }

    private static int clampedStackSize(ItemStack output, int stackSize) {
        return Math.min(Math.max(1, stackSize), output.getMaxStackSize());
    }

    private static int clampedChance(int chance, float multiplier) {
        return Math.min(10000, Math.max(1, (int) (chance * multiplier)));
    }

    private static final class RecipeCounts {

        private int processorRecipes;
        private int socProcessorRecipes;
        private int assemblyRecipes;
    }
}
