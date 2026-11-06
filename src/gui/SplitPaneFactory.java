package gui;

import gui.environment.Environment;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JSplitPane;
import plus.JetBrainsUI;

public class SplitPaneFactory {
    private SplitPaneFactory() {
    }

    public static JSplitPane createSplit(Environment environment, boolean horizontal, double ratio,
                                         Component left, Component right) {
        JSplitPane split = new JSplitPane(
                horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT,
                true, left, right);
        JetBrainsUI.styleSplitPane(split);

        Dimension dim = environment.getSize();
        Component[] comps = environment.getComponents();
        if (comps.length != 0) {
            dim = comps[0].getSize();
        }
        int total = horizontal ? dim.width : dim.height;
        split.setDividerLocation((int) (total * ratio));
        split.setResizeWeight(ratio);
        return split;
    }
}
