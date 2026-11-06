package gui.viewer;

import automata.Automaton;
import automata.Note;
import automata.State;
import automata.event.AutomataStateEvent;
import automata.event.AutomataStateListener;
import automata.event.AutomataTransitionEvent;
import automata.event.AutomataTransitionListener;
import gui.JMultiLineToolTip;
import gui.editor.EditorPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plus.Theme;

public class AutomatonPane extends JPanel implements Scrollable {
    private Point tp = null;
    public AffineTransform transform = new AffineTransform();
    protected AutomatonDrawer drawer;
    private boolean adapt;
    private EditorPane myCreator;
    private double scaleBy = 1.0;
    private boolean transformNeedsReform = true;
    private JTable table;

    private Point panStartScreen = null;
    private boolean panningActive = false;

    public AutomatonPane(AutomatonDrawer drawer) {
        this(drawer, false);
    }

    public AutomatonPane(AutomatonDrawer drawer, boolean adapt) {
        super();
        this.drawer = drawer;
        this.adapt = adapt;
        setPreferredSize(new Dimension(400, 300));
        Listener listener = new Listener();
        drawer.getAutomaton().addStateListener(listener);
        drawer.getAutomaton().addTransitionListener(listener);
        if (drawer instanceof SelectionDrawer) {
            ((SelectionDrawer) drawer).addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    AutomatonPane.this.repaint();
                }
            });
        }
        addComponentListener(listener);
        setToolTipText("Beavis");
        setOpaque(true);

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rot = e.getWheelRotation();
                if (rot == 0) {
                    return;
                }
                double factor = rot < 0 ? 1.12 : (1.0 / 1.12);
                double next = Math.max(0.25, Math.min(4.0, scaleBy * factor));
                setScale(next);
                requestTransform();
                repaint();
            }
        });
    }

    public AutomatonPane(Automaton automaton) {
        this(new AutomatonDrawer(automaton));
    }

    @Override
    public JToolTip createToolTip() {
        return new JMultiLineToolTip();
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        try {
            return transformFromAutomatonToView(this.drawer.stateAtPoint(event.getPoint()).getPoint());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (!this.drawer.doesDrawStateLabels()) {
            State s = this.drawer.stateAtPoint(event.getPoint());
            if (s == null) {
                return null;
            }
            return s.getLabel();
        }
        return null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.transformNeedsReform) {
            reformTransform(new Rectangle(getSize()));
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(Theme.WHITE);
        g.fillRect(0, 0, getSize().width, getSize().height);

        if (Theme.isShowGrid()) {
            drawGrid(g2);
        }

        // IMPORTANT: Transform g2 directly so EditCanvas.paintComponent can draw tools
        // in automaton space and then invert the transform for child components (JTable/Note).
        g2.transform(this.transform);
        this.drawer.drawAutomaton(g);

        ArrayList notes = getDrawer().getAutomaton().getNotes();
        for (int k = 0; k < notes.size(); k++) {
            Note curNote = (Note) notes.get(k);
            curNote.setBackground(Theme.SURFACE);
            curNote.setForeground(Theme.BLACK);
            curNote.setDisabledTextColor(Theme.BLACK);
            curNote.setFont(Theme.MONO_FONT);
            curNote.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                    javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            curNote.updateView();
        }
    }

    private void drawGrid(Graphics2D g2) {
        int step = (int) Math.max(12, Math.round(Theme.getGridSize() * this.scaleBy));
        int w = getWidth();
        int h = getHeight();
        Color minorDot = Theme.isDark() ? new Color(60, 63, 70) : new Color(214, 218, 226);
        Color majorDot = Theme.isDark() ? new Color(80, 84, 94) : new Color(184, 190, 202);
        Color subtleLine = Theme.isDark() ? new Color(38, 40, 46) : new Color(242, 244, 248);

        // Ultra-faint major guide lines every 5 steps
        g2.setColor(subtleLine);
        int idxX = 0;
        for (int x = 0; x < w; x += step, idxX++) {
            if (idxX % 5 == 0) {
                g2.drawLine(x, 0, x, h);
            }
        }
        int idxY = 0;
        for (int y = 0; y < h; y += step, idxY++) {
            if (idxY % 5 == 0) {
                g2.drawLine(0, y, w, y);
            }
        }

        // Crisp dot grid at intersections
        idxX = 0;
        for (int x = 0; x < w; x += step, idxX++) {
            idxY = 0;
            boolean majX = (idxX % 5 == 0);
            for (int y = 0; y < h; y += step, idxY++) {
                boolean maj = majX && (idxY % 5 == 0);
                g2.setColor(maj ? majorDot : minorDot);
                int sz = maj ? 3 : 2;
                g2.fillOval(x - 1, y - 1, sz, sz);
            }
        }
    }

    @Override
    public void printComponent(Graphics g) {
        if (this.transformNeedsReform) {
            reformTransform(g.getClipBounds());
        }
        g.setColor(Theme.WHITE);
        g.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
        Graphics2D g2 = (Graphics2D) g;
        g2.transform(this.transform);
        this.drawer.invalidate();
        this.drawer.drawAutomaton(g);
        ArrayList notes = getDrawer().getAutomaton().getNotes();
        for (int k = 0; k < notes.size(); k++) {
            Note curNote = (Note) notes.get(k);
            curNote.updateView();
        }
    }

    protected Rectangle getAutomatonBounds() {
        Rectangle rect = this.drawer.getBounds();
        if (rect == null) {
            return new Rectangle(getSize());
        }
        return rect;
    }

    public void reformTransform(Rectangle viewBounds) {
        this.transformNeedsReform = false;
        Rectangle bounds = new Rectangle(getAutomatonBounds());
        if (!this.adapt) {
            Rectangle visible = getVisibleRect();
            Point viewportUpperLeft = new Point(
                    (int) (visible.x - this.transform.getTranslateX()),
                    (int) (visible.y - this.transform.getTranslateY()));
            Point upperLeft = new Point(
                    Math.min(bounds.x, Math.min(0, viewportUpperLeft.x)),
                    Math.min(bounds.y, Math.min(0, viewportUpperLeft.y)));
            this.transform = new AffineTransform();
            this.drawer.setTransform(this.transform);
            this.drawer.invalidateBounds();
            this.transform.translate(-upperLeft.x, -upperLeft.y);
            this.transform.scale(this.scaleBy, this.scaleBy);
            Dimension newSize = new Dimension(
                    Math.max(bounds.width + bounds.x, viewportUpperLeft.x + visible.width) - upperLeft.x,
                    Math.max(bounds.height + bounds.y, viewportUpperLeft.y + visible.height) - upperLeft.y);
            if (newSize.equals(getPreferredSize())) {
                return;
            }
            setPreferredSize(newSize);
            revalidate();
            scrollRectToVisible(visible);
            return;
        }

        this.transform = new AffineTransform();
        bounds.grow(20, 20);
        double aRatio = bounds.getWidth() / bounds.getHeight();
        double vRatio = viewBounds.getWidth() / viewBounds.getHeight();
        if (aRatio > vRatio) {
            double targetHeight = bounds.getWidth() / vRatio;
            targetHeight -= bounds.getHeight();
            bounds.setRect(bounds.getX(), bounds.getY() - targetHeight / 2.0,
                    bounds.getWidth(), bounds.getHeight() + targetHeight);
        } else {
            double targetWidth = bounds.getHeight() * vRatio;
            targetWidth -= bounds.getWidth();
            bounds.setRect(bounds.getX() - targetWidth / 2.0, bounds.getY(),
                    bounds.getWidth() + targetWidth, bounds.getHeight());
        }
        double scale = viewBounds.getWidth() / bounds.getWidth();
        this.transform.scale(scale, scale);
        this.transform.translate(viewBounds.getX() - bounds.getX(), viewBounds.getY() - bounds.getY());
    }

    public void transformMouseEvent(MouseEvent event) {
        if (this.transformNeedsReform) {
            reformTransform(new Rectangle(getSize()));
        }
        Point point = new Point();
        Point ePoint = event.getPoint();
        try {
            this.transform.inverseTransform(ePoint, point);
            event.translatePoint(point.x - ePoint.x, point.y - ePoint.y);
        } catch (NoninvertibleTransformException e) {
        }
    }

    public Point transformFromAutomatonToView(Point pt) {
        return (Point) this.transform.transform(pt, new Point());
    }

    public Point transformFromViewToAutomaton(Point pt) {
        try {
            return (Point) this.transform.inverseTransform(pt, new Point());
        } catch (NoninvertibleTransformException e) {
            return new Point(pt);
        }
    }

    @Override
    public void processMouseEvent(MouseEvent event) {
        boolean isPanTrigger = SwingUtilities.isMiddleMouseButton(event)
                || (SwingUtilities.isLeftMouseButton(event) && event.isAltDown());
        if (isPanTrigger && event.getID() == MouseEvent.MOUSE_PRESSED) {
            this.panStartScreen = new Point(event.getX(), event.getY());
            this.panningActive = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            return;
        }
        if (this.panningActive && event.getID() == MouseEvent.MOUSE_RELEASED) {
            this.panStartScreen = null;
            this.panningActive = false;
            setCursor(Cursor.getDefaultCursor());
            return;
        }
        transformMouseEvent(event);
        super.processMouseEvent(event);
    }

    @Override
    public void processMouseMotionEvent(MouseEvent event) {
        if (this.panningActive && event.getID() == MouseEvent.MOUSE_DRAGGED && this.panStartScreen != null) {
            int dxScreen = event.getX() - this.panStartScreen.x;
            int dyScreen = event.getY() - this.panStartScreen.y;
            double sc = this.scaleBy > 0.05 ? this.scaleBy : 1.0;
            int dx = (int) Math.round(dxScreen / sc);
            int dy = (int) Math.round(dyScreen / sc);
            if (dx != 0 || dy != 0) {
                panAutomatonStates(dx, dy);
                this.panStartScreen = new Point(event.getX(), event.getY());
            }
            return;
        }
        transformMouseEvent(event);
        super.processMouseMotionEvent(event);
    }

    private void panAutomatonStates(int dx, int dy) {
        if (this.drawer == null || this.drawer.getAutomaton() == null) {
            return;
        }
        Automaton aut = this.drawer.getAutomaton();
        for (State s : aut.getStates()) {
            Point p = s.getPoint();
            p.setLocation(Math.max(20, p.x + dx), Math.max(20, p.y + dy));
            s.setPoint(p);
        }
        repaint();
    }

    public AutomatonDrawer getDrawer() {
        return this.drawer;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == 1 ? visibleRect.height : visibleRect.width;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 5;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (this.adapt) {
            return true;
        }
        return getPreferredSize().width < getParent().getSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (this.adapt) {
            return true;
        }
        return getPreferredSize().height < getParent().getSize().height;
    }

    public Point getAutomatonOrigin() {
        return new Point(-(int) this.transform.getTranslateX(), -(int) this.transform.getTranslateY());
    }

    public void fitToBounds(int pad) {
        Rectangle viewBounds = getVisibleRect();
        Rectangle automatonBounds;
        if (this.drawer != null && this.drawer.getBounds() != null) {
            automatonBounds = new Rectangle(this.drawer.getBounds());
        } else {
            automatonBounds = new Rectangle(0, 0);
        }
        automatonBounds.grow(pad, pad);
        viewBounds.x = 0;
        viewBounds.y = 0;
        State[] states = this.drawer.getAutomaton().getStates();
        for (int i = 0; i < states.length; i++) {
            Point p = states[i].getPoint();
            p.setLocation((p.getX() - automatonBounds.getX()) * (viewBounds.getWidth() / automatonBounds.getWidth()),
                    (p.getY() - automatonBounds.getY()) * (viewBounds.getHeight() / automatonBounds.getHeight()));
            states[i].setPoint(p);
        }
    }

    @Override
    public Component add(Component c) {
        if (c instanceof JTable) {
            this.table = (JTable) c;
        }
        return super.add(c);
    }

    public void setTablePoint(Point p) {
        this.tp = p;
    }

    @Override
    public void doLayout() {
        if (this.table != null && this.tp != null) {
            this.table.setLocation(this.tp);
        }
    }

    public void setAdapt(boolean adapt) {
        this.adapt = adapt;
        this.transformNeedsReform = true;
        repaint();
    }

    public boolean getAdapt() {
        return this.adapt;
    }

    public void setCreator(EditorPane pane) {
        this.myCreator = pane;
    }

    public EditorPane getCreator() {
        return this.myCreator;
    }

    public void setScale(double scale) {
        this.scaleBy = scale;
    }

    public double getScale() {
        return this.scaleBy;
    }

    public void requestTransform() {
        this.transformNeedsReform = true;
        repaint();
    }

    private class Listener extends ComponentAdapter implements AutomataStateListener, AutomataTransitionListener {
        @Override
        public void automataTransitionChange(AutomataTransitionEvent e) {
            AutomatonPane.this.transformNeedsReform = true;
            AutomatonPane.this.repaint();
        }

        @Override
        public void automataStateChange(AutomataStateEvent e) {
            AutomatonPane.this.transformNeedsReform = true;
            AutomatonPane.this.repaint();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (AutomatonPane.this.adapt) {
                AutomatonPane.this.transformNeedsReform = true;
            }
            AutomatonPane.this.repaint();
        }
    }
}
