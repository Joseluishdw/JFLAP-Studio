package gui.sim;

import automata.Configuration;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import plus.Theme;

public class ConfigurationButton extends JToggleButton {
    protected int state = NORMAL;
    private Configuration config = null;
    private ConfigurationIcon icon = null;

    public static final int NORMAL = 0;
    public static final int ACCEPT = 1;
    public static final int REJECT = 2;
    public static final int FREEZE = 3;
    public static final int FOCUSED = 4;

    private static final String[] TEXT = {
        "Activa", "Aceptada", "Rechazada", "Congelada", "Enfocada"
    };

    public static final Color[] STATE_COLOR = {
        new Color(0, 0, 0, 0),
        new Color(46, 160, 67, 45),
        new Color(229, 87, 101, 45),
        new Color(76, 194, 255, 45),
        new Color(242, 201, 76, 45)
    };

    protected static final int PADDING = 5;

    public ConfigurationButton(Configuration config) {
        this(config, NORMAL);
    }

    public ConfigurationButton(Configuration config, int state) {
        super();
        this.config = config;
        this.icon = ConfigurationIconFactory.iconForConfiguration(config);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalTextPosition(0);
        setVerticalTextPosition(1);
        setState(state);
        setPreferredSize(new Dimension(0, 14 + this.icon.getIconHeight()));
    }

    public void setState(int newState) {
        if (this.config.isAccept()) {
            newState = ACCEPT;
        }
        if (newState < 0 || newState >= TEXT.length) {
            newState = NORMAL;
        }
        this.state = newState;
        repaint();
    }

    public Configuration getConfiguration() {
        return this.config;
    }

    public ConfigurationIcon getConfigurationIcon() {
        return this.icon;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Card container surface inside the #1E1F22 simulation panel
        g2.setColor(Theme.SURFACE);
        g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));

        // Subtle status tint if Accept / Reject / Freeze / Focused
        Color badgeAccent = Theme.ACCENT;
        if (this.state == ACCEPT) {
            badgeAccent = Theme.RUN_GREEN;
            g2.setColor(new Color(46, 160, 67, 24));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
        } else if (this.state == REJECT) {
            badgeAccent = new Color(229, 87, 101);
            g2.setColor(new Color(229, 87, 101, 24));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
        } else if (this.state == FREEZE) {
            badgeAccent = Theme.FIRED_CYAN;
            g2.setColor(new Color(76, 194, 255, 22));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
        } else if (this.state == FOCUSED) {
            badgeAccent = new Color(242, 201, 76);
            g2.setColor(new Color(242, 201, 76, 22));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
        }

        // Card container border (highlighted when selected or in special state)
        boolean sel = isSelected();
        boolean roll = getModel().isRollover();
        if (sel) {
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(1.8f));
        } else if (this.state != NORMAL) {
            g2.setColor(badgeAccent);
            g2.setStroke(new BasicStroke(1.4f));
        } else {
            g2.setColor(roll ? Theme.FLOATING_BORDER : Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, w - 5f, h - 5f, 10, 10));

        // Render the tape carousel icon
        g2.translate(6, 6);
        this.icon.paintConfiguration(this, g2, w - 12, h - 12);
        g2.translate(-6, -6);

        // Status pill in the top-right corner when Accept / Reject / Freeze / Focused
        if (this.state != NORMAL) {
            String statusText = TEXT[this.state];
            Font f = new Font("Segoe UI", Font.BOLD, 11);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            int pw = fm.stringWidth(statusText) + 16;
            int ph = 20;
            int px = w - pw - 12;
            int py = 8;

            g2.setColor(badgeAccent);
            g2.fill(new RoundRectangle2D.Float(px, py, pw, ph, 10, 10));
            g2.setColor(Color.WHITE);
            int ty = py + (ph - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
            g2.drawString(statusText, px + 8, ty);
        }

        g2.dispose();
    }
}
