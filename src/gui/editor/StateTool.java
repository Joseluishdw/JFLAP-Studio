package gui.editor;

import automata.State;
import gui.environment.AutomatonEnvironment;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import plus.Theme;

public class StateTool extends Tool {
    State state = null;

    public StateTool(AutomatonPane view, AutomatonDrawer drawer) {
        super(view, drawer);
    }

    @Override
    public String getToolTip() {
        return "Creador de Estados (State Creator)";
    }

    @Override
    protected Icon getIcon() {
        URL url = getClass().getResource("/ICON/state.gif");
        return new ImageIcon(url);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (javax.swing.SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
            return;
        }
        if (getDrawer().getAutomaton().getEnvironmentFrame() != null) {
            ((AutomatonEnvironment) getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
        }
        Point pt = event.getPoint();
        if (Theme.isSnapToGrid()) {
            pt = new Point(Theme.snap(pt.x), Theme.snap(pt.y));
        }
        this.state = getAutomaton().createState(pt);
        getView().repaint();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (javax.swing.SwingUtilities.isRightMouseButton(event) || this.state == null) {
            return;
        }
        Point pt = event.getPoint();
        if (Theme.isSnapToGrid()) {
            pt = new Point(Theme.snap(pt.x), Theme.snap(pt.y));
        }
        this.state.setPoint(pt);
        getView().repaint();
    }

    @Override
    public KeyStroke getKey() {
        return KeyStroke.getKeyStroke('s');
    }
}
