package gui.viewer;

import automata.Automaton;
import automata.State;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import plus.Theme;

public class StateDrawer {
    private int radius = 20;
    public static final int STATE_RADIUS = 20;
    public static final Color STATE_COLOR = new Color(255, 255, 150);
    public static final int STATE_LABEL_PAD = 3;

    public StateDrawer() {
        this.radius = 20;
    }

    public StateDrawer(int radius) {
        this.radius = radius;
    }

    public void drawState(Graphics g, Automaton automaton, State state) {
        drawState(g, automaton, state, state.getPoint());
    }

    public void drawState(Graphics g, Automaton automaton, State state, Point point) {
        drawState(g, automaton, state, point,
                state.getFillColor() != null ? state.getFillColor() : Theme.STATE_FILL);
    }

    public void drawState(Graphics g, Automaton automaton, State state, Point point, Color color) {
        if (color == STATE_COLOR) {
            color = state.getFillColor() != null ? state.getFillColor() : Theme.STATE_FILL;
        }
        drawArea(g, automaton, state, point, color);
    }

    private boolean isStateHighlighted(State state, Color color) {
        if (state == null) {
            return false;
        }
        if (state.isSelected() || Theme.isActiveSimState(state)) {
            return true;
        }
        if (color != null && color != STATE_COLOR && !color.equals(Theme.STATE_FILL)
                && (state.getFillColor() == null || !color.equals(state.getFillColor()))) {
            return true;
        }
        return false;
    }

    private void drawArea(Graphics g, Automaton automaton, State state, Point point, Color color) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Stroke oldStroke = g2.getStroke();
        Font oldFont = g2.getFont();

        boolean activeOrSelected = isStateHighlighted(state, color);
        drawBackground(g, state, point, color, activeOrSelected);

        Color renderedFill = activeOrSelected
                ? Theme.ACTIVE_BG
                : (color != null ? color : Theme.STATE_FILL);

        // State name with automatic luminance contrast
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if (activeOrSelected) {
            g2.setColor(Color.WHITE);
        } else {
            double lum = 0.299 * renderedFill.getRed() + 0.587 * renderedFill.getGreen() + 0.114 * renderedFill.getBlue();
            g2.setColor(lum > 145.0 ? new Color(30, 31, 34) : Theme.BLACK);
        }

        FontMetrics fm = g2.getFontMetrics();
        String name = state.getName();
        int dx = (int) fm.getStringBounds(name, g2).getWidth() >> 1;
        int dy = fm.getAscent() >> 1;
        g2.drawString(name, point.x - dx, point.y + dy - 1);

        // 3px #3574F0 selection border when active in simulation (or 2px default border)
        float borderStroke = activeOrSelected ? 3.0f : 2.0f;
        g2.setStroke(new BasicStroke(borderStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(activeOrSelected ? Theme.ACCENT : Theme.STATE_BORDER);
        g2.drawOval(point.x - this.radius, point.y - this.radius, 2 * this.radius, 2 * this.radius);

        if (automaton.isFinalState(state)) {
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(activeOrSelected ? Color.WHITE : Theme.BLACK);
            g2.drawOval(point.x - this.radius + 4, point.y - this.radius + 4, (this.radius - 4) << 1, (this.radius - 4) << 1);
        }

        // Initial state modern arrow indicator
        if (automaton.getInitialState() == state) {
            int[] x = new int[]{point.x - this.radius - 1, point.x - (this.radius << 1) + 2, point.x - (this.radius << 1) + 2};
            int[] y = new int[]{point.y, point.y - this.radius + 4, point.y + this.radius - 4};
            g2.setColor(Theme.ACCENT);
            g2.fillPolygon(x, y, 3);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.BLACK);
            g2.drawPolygon(x, y, 3);
        }

        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
    }

    public void drawStateLabel(Graphics g, State state, Point point, Color color) {
        String[] labels = state.getLabels();
        if (labels.length == 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();
        int height = 0;
        int textWidth = 0;
        for (int i = 0; i < labels.length; i++) {
            Rectangle2D bounds = fm.getStringBounds(labels[i], g);
            textWidth = Math.max((int) bounds.getWidth(), textWidth);
            height += ascent + 3;
        }
        int width = textWidth + 12;
        height = (height - 3) + 6;
        int x = point.x - (width >> 1);
        int y = point.y + 20 + 2;
        int baselineY = y;

        g.setColor(Theme.SURFACE);
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(Theme.CARD_BORDER);
        g.drawRoundRect(x, y, width, height, 8, 8);
        g.setColor(Theme.BLACK);
        for (int i = 0; i < labels.length; i++) {
            baselineY += ascent + 3;
            g.drawString(labels[i], x + 6, baselineY);
        }
    }

    public void drawBackground(Graphics g, State state, Point point, Color color) {
        drawBackground(g, state, point, color, isStateHighlighted(state, color));
    }

    private void drawBackground(Graphics g, State state, Point point, Color color, boolean activeOrSelected) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Multi-layer soft drop shadow
        int d = 2 * this.radius;
        g2.setColor(new Color(0, 0, 0, Theme.isDark() ? 45 : 18));
        g2.fillOval(point.x - this.radius - 1, point.y - this.radius + 3, d + 2, d + 2);
        g2.setColor(new Color(0, 0, 0, Theme.isDark() ? 80 : 28));
        g2.fillOval(point.x - this.radius, point.y - this.radius + 2, d, d);

        // Active / Selected outer pulsing halo ring (#3574F0)
        if (activeOrSelected) {
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 42));
            int outerR = this.radius + 8;
            g2.fillOval(point.x - outerR, point.y - outerR, 2 * outerR, 2 * outerR);

            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 85));
            int innerR = this.radius + 4;
            g2.fillOval(point.x - innerR, point.y - innerR, 2 * innerR, 2 * innerR);
        }

        Color fill = activeOrSelected
                ? Theme.ACTIVE_BG
                : (color != null && color != STATE_COLOR ? color : Theme.STATE_FILL);
        g.setColor(fill);
        g.fillOval(point.x - this.radius, point.y - this.radius, d, d);
    }

    public int getRadius() {
        return this.radius;
    }
}
