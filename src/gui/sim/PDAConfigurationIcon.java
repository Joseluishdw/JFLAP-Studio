package gui.sim;

import automata.Configuration;
import automata.pda.PDAConfiguration;
import java.awt.Component;
import java.awt.Graphics2D;

public class PDAConfigurationIcon extends ConfigurationIcon {
    public PDAConfigurationIcon(Configuration configuration) {
        super(configuration);
    }

    @Override
    public int getIconHeight() {
        return 142;
    }

    @Override
    public void paintConfiguration(Component c, Graphics2D g, int width, int height) {
        PDAConfiguration config = (PDAConfiguration) getConfiguration();
        String input = config.getInput() != null ? config.getInput() : "";
        String unproc = config.getUnprocessedInput() != null ? config.getUnprocessedInput() : "";
        int headIdx = Math.max(0, input.length() - unproc.length());
        String display = input.isEmpty() ? "\u25A1" : (headIdx >= input.length() ? input + "\u25A1" : input);
        String stateName = config.getCurrentState() != null ? config.getCurrentState().getName() : "q0";

        Torn.paintCarouselTape(g, display, 4f, 2f, Math.max(80f, width - 8f),
                headIdx, headIdx, stateName, false);

        String stackStr = config.getStack() != null ? config.getStack().toString() : "";
        if (stackStr.isEmpty()) {
            stackStr = "\u25A1";
        }
        Torn.paintCarouselTape(g, stackStr, 4f, 80f, Math.max(80f, width - 8f),
                0f, 0, "Pila (Top)", false);
    }
}
