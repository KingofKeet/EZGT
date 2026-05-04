package com.keet.ezgt;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

public class ModConfig {

    private static final Class<?>[] configClasses = { Rates.class };

    @Config(modid = EZGT.MODID)
    public static class Rates {

        @Config.Comment("Multiplier for GT casing assembler recipe outputs (gt.blockcasing oredict)")
        @Config.RangeFloat(min = 0.1F, max = 64F)
        public static float gtCasingAssemblerOutputs = 2F;

        @Config.Comment("[Requires restart] Makes circuit assembler / CAL recipes use Monifactory-style output ratios: processors output 2, SoC processor recipes output 4, and assemblies output 2.")
        @Config.DefaultBoolean(true)
        @Config.Name("easycircuits")
        public static boolean easyCircuits;

        @Config.Comment("Multiplier for GT coil assembler recipe outputs (gt.blockcasing5 oredict)")
        @Config.DefaultBoolean(false)
        @Config.Name("easycoils")
        public static boolean easyCoils;

        @Config.Comment("Multiplier for GT ore vein density. 2.0 = double density for all veins. Works great with GTNHRates!")
        @Config.RangeFloat(min = 0.1F, max = 8F)
        public static float veinRichness = 2.0F;
    }

    public static void registerConfigClasses() {
        try {
            for (Class<?> c : configClasses) {
                ConfigurationManager.registerConfig(c);
            }
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    public static class GuiConfig extends SimpleGuiConfig {

        public GuiConfig(GuiScreen parent) throws ConfigException {
            super(parent, EZGT.MODID, "EZGT", true, configClasses);
        }
    }

    public static class GUIFactory implements SimpleGuiFactory {

        @Override
        public Class<? extends GuiScreen> mainConfigGuiClass() {
            return GuiConfig.class;
        }
    }
}
