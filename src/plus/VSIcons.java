package plus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

public final class VSIcons {
    public static final int PLAY = 1;
    public static final int MULTI_PLAY = 2;
    public static final int EXPLORER = 3;
    public static final int CURSOR = 4;
    public static final int STATE = 5;
    public static final int TRANSITION = 6;
    public static final int DELETE = 7;
    public static final int GRID = 8;
    public static final int SNAP = 9;
    public static final int LAYOUT = 10;
    public static final int TABLE = 11;
    public static final int THEME = 12;
    public static final int HELP = 13;
    public static final int FILE_JFF = 14;
    public static final int UNDO = 15;
    public static final int REDO = 16;
    public static final int MEDIA_RESET = 17;
    public static final int MEDIA_STEP_BACK = 18;
    public static final int MEDIA_PLAY = 19;
    public static final int MEDIA_PAUSE = 20;
    public static final int MEDIA_STEP_FORWARD = 21;
    public static final int MEDIA_STOP = 22;
    public static final int MEDIA_TRACE = 23;
    public static final int MEDIA_FREEZE = 24;
    public static final int MEDIA_FOCUS = 25;
    public static final int MEDIA_OPTIONS = 26;

    private VSIcons() {
    }

    public static Icon get(final int type, final int size, final Color fixedColor) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);

                Color fg = fixedColor != null ? fixedColor : (c != null ? c.getForeground() : Theme.BLACK);
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                switch (type) {
                    case PLAY: {
                        g2.setColor(fixedColor != null ? fixedColor : new Color(87, 150, 92));
                        int[] px = {3, size - 3, 3};
                        int[] py = {2, size / 2, size - 2};
                        g2.fillPolygon(px, py, 3);
                        break;
                    }
                    case MULTI_PLAY: {
                        g2.setColor(fixedColor != null ? fixedColor : new Color(87, 150, 92));
                        int mid = size / 2;
                        int[] px1 = {1, mid, 1};
                        int[] py1 = {2, size / 2, size - 2};
                        g2.fillPolygon(px1, py1, 3);
                        int[] px2 = {mid - 1, size - 1, mid - 1};
                        int[] py2 = {2, size / 2, size - 2};
                        g2.fillPolygon(px2, py2, 3);
                        break;
                    }
                    case EXPLORER: {
                        g2.drawRoundRect(2, 2, size - 5, size - 5, 3, 3);
                        g2.drawLine(6, 2, 6, size - 3);
                        break;
                    }
                    case CURSOR: {
                        int[] px = {4, 4, 7, 10, 12, 9, 13};
                        int[] py = {2, 13, 10, 15, 14, 9, 9};
                        g2.fillPolygon(px, py, 7);
                        break;
                    }
                    case STATE: {
                        g2.setColor(Theme.ACCENT);
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawOval(2, 2, size - 5, size - 5);
                        break;
                    }
                    case TRANSITION: {
                        g2.drawLine(2, size - 3, size - 3, 3);
                        g2.drawLine(size - 8, 3, size - 3, 3);
                        g2.drawLine(size - 3, 3, size - 3, 8);
                        break;
                    }
                    case DELETE: {
                        g2.setColor(new Color(229, 87, 101));
                        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(3, 3, size - 3, size - 3);
                        g2.drawLine(size - 3, 3, 3, size - 3);
                        break;
                    }
                    case GRID: {
                        g2.setColor(fg);
                        int[] pts = {3, size / 2, size - 4};
                        for (int gx : pts) {
                            for (int gy : pts) {
                                g2.fillOval(gx - 1, gy - 1, 3, 3);
                            }
                        }
                        break;
                    }
                    case SNAP: {
                        g2.drawArc(3, 2, size - 6, size - 4, 180, 180);
                        g2.drawLine(3, 3, 3, size / 2);
                        g2.drawLine(size - 3, 3, size - 3, size / 2);
                        g2.setColor(Theme.ACCENT);
                        g2.fillRect(2, 2, 3, 3);
                        g2.fillRect(size - 5, 2, 3, 3);
                        break;
                    }
                    case LAYOUT: {
                        g2.setColor(Theme.ACCENT);
                        g2.fillOval(2, 2, 5, 5);
                        g2.fillOval(size - 7, 2, 5, 5);
                        g2.fillOval(size / 2 - 2, size - 7, 5, 5);
                        g2.setColor(fg);
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawLine(5, 5, size - 5, 5);
                        g2.drawLine(5, 5, size / 2, size - 5);
                        g2.drawLine(size - 5, 5, size / 2, size - 5);
                        break;
                    }
                    case TABLE: {
                        g2.drawRoundRect(2, 2, size - 4, size - 4, 3, 3);
                        g2.drawLine(2, 6, size - 2, 6);
                        g2.drawLine(size / 2, 6, size / 2, size - 2);
                        break;
                    }
                    case THEME: {
                        g2.setColor(Theme.ACCENT);
                        g2.fillOval(3, 3, size - 6, size - 6);
                        break;
                    }
                    case HELP: {
                        g2.setColor(Theme.TEXT_MUTED);
                        g2.drawOval(1, 1, size - 2, size - 2);
                        g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, size - 4));
                        g2.drawString("?", size / 2 - 3, size - 3);
                        break;
                    }
                    case FILE_JFF: {
                        g2.setColor(Theme.ACCENT);
                        g2.drawRoundRect(3, 2, size - 6, size - 4, 3, 3);
                        g2.setColor(Theme.TEXT_MUTED);
                        g2.drawLine(5, 6, size - 5, 6);
                        g2.drawLine(5, 9, size - 5, 9);
                        break;
                    }
                    case UNDO: {
                        g2.drawArc(3, 4, size - 6, size - 6, 0, 180);
                        g2.drawLine(3, 7, 3, 3);
                        g2.drawLine(3, 7, 7, 7);
                        break;
                    }
                    case REDO: {
                        g2.drawArc(3, 4, size - 6, size - 6, 0, 180);
                        g2.drawLine(size - 3, 7, size - 3, 3);
                        g2.drawLine(size - 3, 7, size - 7, 7);
                        break;
                    }
                    case MEDIA_RESET: {
                        // ⏮ Vertical bar on left + left-pointing triangle
                        g2.fillRoundRect(2, 3, 2, size - 6, 1, 1);
                        int[] px = {size - 2, 5, size - 2};
                        int[] py = {3, size / 2, size - 3};
                        g2.fillPolygon(px, py, 3);
                        break;
                    }
                    case MEDIA_STEP_BACK: {
                        // ◀ Crisp left-pointing triangle
                        int[] px = {size - 3, 3, size - 3};
                        int[] py = {2, size / 2, size - 2};
                        g2.fillPolygon(px, py, 3);
                        break;
                    }
                    case MEDIA_PLAY: {
                        // ▶ Crisp right-pointing triangle
                        int[] px = {3, size - 2, 3};
                        int[] py = {2, size / 2, size - 2};
                        g2.fillPolygon(px, py, 3);
                        break;
                    }
                    case MEDIA_PAUSE: {
                        // ⏸ Two vertical rounded bars
                        g2.fillRoundRect(3, 2, 3, size - 4, 2, 2);
                        g2.fillRoundRect(size - 6, 2, 3, size - 4, 2, 2);
                        break;
                    }
                    case MEDIA_STEP_FORWARD: {
                        // ▶❙ Right-pointing triangle + vertical bar on right
                        int[] px = {2, size - 5, 2};
                        int[] py = {3, size / 2, size - 3};
                        g2.fillPolygon(px, py, 3);
                        g2.fillRoundRect(size - 4, 3, 2, size - 6, 1, 1);
                        break;
                    }
                    case MEDIA_STOP: {
                        // ⏹ Rounded stop square in soft red
                        g2.setColor(fixedColor != null ? fixedColor : new Color(229, 87, 101));
                        g2.fillRoundRect(3, 3, size - 6, size - 6, 3, 3);
                        break;
                    }
                    case MEDIA_TRACE: {
                        // Pulse / history trace line
                        g2.setColor(Theme.ACCENT);
                        int m = size / 2;
                        g2.drawLine(1, m, 4, m);
                        g2.drawLine(4, m, 6, 3);
                        g2.drawLine(6, 3, 9, size - 3);
                        g2.drawLine(9, size - 3, 11, m);
                        g2.drawLine(11, m, size - 2, m);
                        break;
                    }
                    case MEDIA_FREEZE: {
                        // Snowflake / freeze crystal
                        g2.setColor(new Color(76, 194, 255));
                        int center = size / 2;
                        g2.drawLine(center, 2, center, size - 2);
                        g2.drawLine(2, center, size - 2, center);
                        g2.drawLine(4, 4, size - 4, size - 4);
                        g2.drawLine(size - 4, 4, 4, size - 4);
                        break;
                    }
                    case MEDIA_FOCUS: {
                        // Target crosshair
                        g2.drawOval(3, 3, size - 6, size - 6);
                        g2.setColor(Theme.ACCENT);
                        g2.fillOval(size / 2 - 2, size / 2 - 2, 4, 4);
                        break;
                    }
                    case MEDIA_OPTIONS: {
                        // Sleek sliders / options icon
                        g2.drawLine(2, 4, size - 3, 4);
                        g2.drawLine(2, size / 2, size - 3, size / 2);
                        g2.drawLine(2, size - 4, size - 3, size - 4);
                        g2.setColor(Theme.ACCENT);
                        g2.fillOval(4, 2, 4, 4);
                        g2.fillOval(size - 8, size / 2 - 2, 4, 4);
                        g2.fillOval(5, size - 6, 4, 4);
                        break;
                    }
                }
                g2.dispose();
            }
        };
    }
}
