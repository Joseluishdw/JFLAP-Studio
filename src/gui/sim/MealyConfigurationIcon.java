package gui.sim;

import automata.Configuration;
import automata.mealy.MealyConfiguration;
import java.awt.Component;
import java.awt.Graphics2D;

public class MealyConfigurationIcon extends ConfigurationIcon {
    public MealyConfigurationIcon(Configuration configuration) {
        super(configuration);
    }

    @Override
    public int getIconHeight() {
        return 142;
    }

    @Override
    public void paintConfiguration(Component c, Graphics2D g, int width, int height) {
        MealyConfiguration config = (MealyConfiguration) getConfiguration();
        String input = config.getInput() != null ? config.getInput() : "";
        String unproc = config.getUnprocessedInput() != null ? config.getUnprocessedInput() : "";
        int headIdx = Math.max(0, input.length() - unproc.length());
        String display = input.isEmpty() ? "\u25A1" : (headIdx >= input.length() ? input + "\u25A1" : input);
        String stateName = config.getCurrentState() != null ? config.getCurrentState().getName() : "q0";

        Torn.paintCarouselTape(g, display, 4f, 2f, Math.max(80f, width - 8f),
                headIdx, headIdx, stateName, false);

        String out = config.getOutput() != null && !config.getOutput().isEmpty()
                ? config.getOutput()
                : "\u25A1";
        int outHead = Math.max(0, out.length() - 1);
        Torn.paintCarouselTape(g, out, 4f, 80f, Math.max(80f, width - 8f),
                outHead, outHead, "Salida", false);
    }
}
