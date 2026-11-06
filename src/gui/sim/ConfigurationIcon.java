package gui.sim;

import automata.Automaton;
import automata.Configuration;
import gui.viewer.StateDrawer;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.Icon;

public abstract class ConfigurationIcon implements Icon {
    private Configuration configuration;
    private Automaton automaton;
    protected static final int RADIUS = 13;
    protected static final StateDrawer STATE_DRAWER = new StateDrawer(13);
    protected static final Point STATE_POINT = new Point(26, 13);
    protected static final Point BB_POINT = new Point((int) (STATE_POINT.getX() * 3.5), (int) STATE_POINT.getY());
    protected static final Point RIGHT_STATE = new Point(39, 0);
    protected static final Point BELOW_STATE = new Point(0, 26);

    public ConfigurationIcon(Configuration configuration) {
        this.configuration = configuration;
        this.automaton = configuration.getCurrentState().getAutomaton();
    }

    @Override
    public int getIconWidth() {
        return 420;
    }

    @Override
    public int getIconHeight() {
        return 82;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public Automaton getAutomaton() {
        return this.automaton;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        paintConfiguration(c, g2, getIconWidth(), getIconHeight());
        g2.dispose();
    }

    public void paintConfiguration(Component c, Graphics2D g, int width, int height) {
        // Modern ConfigurationIcons render the current state as a floating Badge Pill
        // directly above the active Playhead cell on the tape carousel.
    }
}
