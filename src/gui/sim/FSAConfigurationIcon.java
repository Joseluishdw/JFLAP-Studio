package gui.sim;

import automata.Configuration;
import automata.fsa.FSAConfiguration;
import java.awt.Component;
import java.awt.Graphics2D;

public class FSAConfigurationIcon extends ConfigurationIcon {
    public FSAConfigurationIcon(Configuration configuration) {
        super(configuration);
    }

    @Override
    public int getIconHeight() {
        return 82;
    }

    @Override
    public void paintConfiguration(Component c, Graphics2D g, int width, int height) {
        FSAConfiguration config = (FSAConfiguration) getConfiguration();
        String input = config.getInput() != null ? config.getInput() : "";
        String unproc = config.getUnprocessedInput() != null ? config.getUnprocessedInput() : "";
        int headIdx = Math.max(0, input.length() - unproc.length());
        String display = input.isEmpty() ? "\u25A1" : (headIdx >= input.length() ? input + "\u25A1" : input);
        String stateName = config.getCurrentState() != null ? config.getCurrentState().getName() : "q0";

        float startY = Math.max(2f, (height - 78) / 2.0f);
        Torn.paintCarouselTape(g, display, 4f, startY, Math.max(80f, width - 8f),
                headIdx, headIdx, stateName, false);
    }
}
