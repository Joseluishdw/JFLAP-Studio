package gui.editor;

import automata.State;
import automata.Transition;
import gui.environment.AutomatonEnvironment;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import plus.Theme;

/**
 * Enhanced Transition Creator Tool for JFLAP Studio:
 * - Click on an existing transition arrow/badge to add another rule to that arrow without dragging.
 * - Click once on a state to add a self-loop (Lazo) and chain rules with Enter / "+ Otra regla".
 * - Drag from a state to another state to connect them.
 * - Drag from a state into empty canvas space (>= 55px) to automatically create a new state there and connect to it!
 */
public class TransitionTool extends Tool {
    protected State first;
    protected Point hover;
    protected TransitionCreator creator;
    private static Stroke STROKE = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static Color COLOR = new Color(0x35, 0x74, 0xF0, 200);

    public TransitionTool(AutomatonPane view, AutomatonDrawer drawer, TransitionCreator creator) {
        super(view, drawer);
        this.creator = creator;
    }

    public TransitionTool(AutomatonPane view, AutomatonDrawer drawer) {
        super(view, drawer);
        this.creator = TransitionCreator.creatorForAutomaton(getAutomaton(), getView());
    }

    @Override
    public String getToolTip() {
        return "Creador de Transiciones y Lazos (Clic en estado = Lazo, Clic en arista = +Regla, Arrastrar al vacío = +Estado)";
    }

    @Override
    protected Icon getIcon() {
        URL url = getClass().getResource("/ICON/transition.gif");
        return new ImageIcon(url);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (getDrawer().getAutomaton().getEnvironmentFrame() != null
                && getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment() instanceof AutomatonEnvironment) {
            ((AutomatonEnvironment) getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
        }
        this.first = getDrawer().stateAtPoint(event.getPoint());
        if (this.first == null) {
            // If user clicked directly on an existing transition badge/arrow, add another rule to that same arrow!
            Transition existing = getDrawer().transitionAtPoint(event.getPoint());
            if (existing != null) {
                this.creator.createTransition(existing.getFromState(), existing.getToState());
            }
            return;
        }
        this.hover = this.first.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (this.first == null) {
            return;
        }
        this.hover = event.getPoint();
        getView().repaint();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (this.first == null) {
            return;
        }
        Point releasePt = event.getPoint();
        State target = getDrawer().stateAtPoint(releasePt);
        if (target != null) {
            this.creator.createTransition(this.first, target);
        } else {
            // If dragged at least 55px into empty canvas space, auto-create a new state right there and connect!
            Point startPt = this.first.getPoint();
            double dist = startPt.distance(releasePt);
            if (dist >= 55.0) {
                Point snapped = Theme.isSnapToGrid()
                        ? new Point(Theme.snap(releasePt.x), Theme.snap(releasePt.y))
                        : releasePt;
                State newState = getAutomaton().createState(snapped);
                getView().repaint();
                this.creator.createTransition(this.first, newState);
            }
        }
        this.first = null;
        getView().repaint();
    }

    @Override
    public void draw(Graphics g) {
        if (this.first == null || this.hover == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        Stroke s = g2.getStroke();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(STROKE);
        g2.setColor(COLOR);
        Point p1 = this.first.getPoint();
        g2.drawLine(p1.x, p1.y, this.hover.x, this.hover.y);

        // If hovering over empty space far enough to spawn a state, draw a preview ghost circle
        if (getDrawer().stateAtPoint(this.hover) == null && p1.distance(this.hover) >= 55.0) {
            int gx = Theme.isSnapToGrid() ? Theme.snap(this.hover.x) : this.hover.x;
            int gy = Theme.isSnapToGrid() ? Theme.snap(this.hover.y) : this.hover.y;
            g2.setColor(new Color(0x35, 0x74, 0xF0, 65));
            g2.fillOval(gx - 20, gy - 20, 40, 40);
            g2.setColor(COLOR);
            g2.drawOval(gx - 20, gy - 20, 40, 40);
        }

        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
        g2.setStroke(s);
    }

    @Override
    public KeyStroke getKey() {
        return KeyStroke.getKeyStroke('t');
    }
}
