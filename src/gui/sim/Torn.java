package gui.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import plus.Theme;

public class Torn {
    public static final int TOP = 0;
    public static final int MIDDLE = 1;
    public static final int BOTTOM = 2;
    public static final float PADDING = 2.0f;
    public static Color HIGHLIGHT_COLOR = new Color(53, 116, 240, 51);

    public static final int CELL_W = 44;
    public static final int CELL_H = 48;
    public static final int CELL_GAP = 5;
    public static final int CELL_ARC = 6;
    public static final int BADGE_SPACE = 26;

    private Torn() {
    }

    /**
     * Renders a modern horizontal carousel of card-style tape cells:
     * - Independent rounded cards (44px x 48px, #26282E fill, 1px #393B40 border, arc = 6px)
     * - Clean 16pt Bold monospaced typography (#DFE1E5 for symbols, #5A5D63 for blank □)
     * - Modern Playhead on the active cell: 2px #3574F0 border + rgba(53, 116, 240, 0.2) fill
     * - Floating Badge Pill (e.g. q0) in #3574F0 with white bold text directly above the active cell
     * - Supports smooth horizontal scrolling via animatedHead interpolation.
     */
    public static void paintCarouselTape(Graphics2D g, String seq, float x, float y, float width,
                                         float animatedHead, int activeIndex,
                                         String stateBadge, boolean infiniteBlanks) {
        if (seq == null) {
            seq = "";
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Shape oldClip = g2.getClip();
        int clipX = Math.round(x);
        int clipY = Math.round(y);
        int clipW = Math.max(40, Math.round(width));
        int clipH = BADGE_SPACE + CELL_H + 8;
        g2.clipRect(clipX, clipY, clipW, clipH);

        int cellY = Math.round(y + (stateBadge != null ? BADGE_SPACE : 4));
        int step = CELL_W + CELL_GAP;
        float centerX = x + width / 2.0f;

        int halfCells = Math.max(4, (int) Math.ceil((width / 2.0f) / step) + 2);
        int centerIdx = Math.round(animatedHead);
        int minK = centerIdx - halfCells;
        int maxK = centerIdx + halfCells;

        g2.setFont(Theme.TAPE_FONT);
        FontMetrics fmTape = g2.getFontMetrics(Theme.TAPE_FONT);
        int textAscent = fmTape.getAscent();
        int textDescent = fmTape.getDescent();
        int textBaselineY = cellY + (CELL_H - (textAscent + textDescent)) / 2 + textAscent;

        // 1. Draw all non-active tape cards in the horizontal carousel
        for (int k = minK; k <= maxK; k++) {
            if (k == activeIndex) {
                continue;
            }
            float cardCenterX = centerX + (k - animatedHead) * step;
            int cardX = Math.round(cardCenterX - CELL_W / 2.0f);
            if (cardX + CELL_W < clipX - 4 || cardX > clipX + clipW + 4) {
                continue;
            }

            char raw = (k >= 0 && k < seq.length()) ? seq.charAt(k) : '\u25A1';
            boolean isBlank = (raw == '\u25A1' || raw == ' ');
            String sym = isBlank ? "\u25A1" : String.valueOf(raw);

            // Card fill (#26282E)
            g2.setColor(Theme.TAPE_CELL_BG);
            g2.fill(new RoundRectangle2D.Float(cardX, cellY, CELL_W, CELL_H, CELL_ARC, CELL_ARC));

            // 1px subtle border (#393B40)
            g2.setColor(Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(cardX + 0.5f, cellY + 0.5f, CELL_W - 1.0f, CELL_H - 1.0f, CELL_ARC, CELL_ARC));

            // Symbol (#DFE1E5 or #5A5D63 muted for blank □)
            if ( !infiniteBlanks && k < activeIndex) {
                // Processed input symbols in FSA/PDA/Mealy shown slightly dimmed
                g2.setColor(Theme.TEXT_MUTED);
            } else {
                g2.setColor(isBlank ? Theme.TAPE_BLANK_FG : Theme.BLACK);
            }
            int sw = fmTape.stringWidth(sym);
            g2.drawString(sym, cardX + (CELL_W - sw) / 2.0f, textBaselineY);
        }

        // 2. Draw active Playhead cell and floating State Badge Pill
        if (activeIndex >= minK - 1 && activeIndex <= maxK + 1) {
            float activeCenterX = centerX + (activeIndex - animatedHead) * step;
            int activeX = Math.round(activeCenterX - CELL_W / 2.0f);

            char raw = (activeIndex >= 0 && activeIndex < seq.length()) ? seq.charAt(activeIndex) : '\u25A1';
            boolean isBlank = (raw == '\u25A1' || raw == ' ');
            String sym = isBlank ? "\u25A1" : String.valueOf(raw);

            // Soft outer glow around active playhead card
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 35));
            g2.fill(new RoundRectangle2D.Float(activeX - 2, cellY - 2, CELL_W + 4, CELL_H + 4, CELL_ARC + 3, CELL_ARC + 3));

            // Base cell card (#26282E) + translucent blue playhead fill rgba(53, 116, 240, 0.2)
            g2.setColor(Theme.TAPE_CELL_BG);
            g2.fill(new RoundRectangle2D.Float(activeX, cellY, CELL_W, CELL_H, CELL_ARC, CELL_ARC));
            g2.setColor(Theme.TAPE_HEAD_FILL);
            g2.fill(new RoundRectangle2D.Float(activeX, cellY, CELL_W, CELL_H, CELL_ARC, CELL_ARC));

            // Bright 2px #3574F0 Playhead border
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new RoundRectangle2D.Float(activeX + 1.0f, cellY + 1.0f, CELL_W - 2.0f, CELL_H - 2.0f, CELL_ARC, CELL_ARC));

            // Active cell symbol
            g2.setFont(Theme.TAPE_FONT);
            g2.setColor(isBlank ? Theme.TAPE_BLANK_FG.brighter() : Theme.BLACK);
            int sw = fmTape.stringWidth(sym);
            g2.drawString(sym, activeX + (CELL_W - sw) / 2.0f, textBaselineY);

            // Floating State Badge Pill (e.g. q0) directly above the active playhead cell
            if (stateBadge != null && !stateBadge.isEmpty()) {
                Font badgeFont = new Font("Segoe UI", Font.BOLD, 11);
                g2.setFont(badgeFont);
                FontMetrics fmBadge = g2.getFontMetrics(badgeFont);
                int bw = Math.max(32, fmBadge.stringWidth(stateBadge) + 14);
                int bh = 19;
                int bx = Math.round(activeCenterX - bw / 2.0f);
                int by = cellY - bh - 5;

                // Soft shadow under badge pill
                g2.setColor(new Color(0, 0, 0, 65));
                g2.fill(new RoundRectangle2D.Float(bx, by + 1, bw, bh, 10, 10));

                // #3574F0 Pill background
                g2.setColor(Theme.ACCENT);
                g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 10, 10));

                // Subtle pointer stem connecting badge pill to the active cell
                int midX = Math.round(activeCenterX);
                int[] px = {midX - 4, midX + 4, midX};
                int[] py = {by + bh - 1, by + bh - 1, cellY - 1};
                g2.fillPolygon(px, py, 3);

                // White bold state label
                g2.setColor(Color.WHITE);
                int tw = fmBadge.stringWidth(stateBadge);
                int ty = by + (bh - (fmBadge.getAscent() + fmBadge.getDescent())) / 2 + fmBadge.getAscent();
                g2.drawString(stateBadge, bx + (bw - tw) / 2.0f, ty);
            }
        }

        g2.setClip(oldClip);
        g2.dispose();
    }

    /**
     * Backwards-compatible paintString method that renders non-TM strings as sleek mini card strips.
     */
    public static float paintString(Graphics2D g, String string, float x, float y,
                                    int align, float width, boolean tornLeft,
                                    boolean tornRight, int selectable) {
        if (string == null) {
            string = "";
        }
        if (align == MIDDLE) {
            y -= (CELL_H * 0.5f);
        } else if (align == BOTTOM) {
            y -= CELL_H;
        }
        int head = (selectable >= 0 && selectable < string.length()) ? selectable : 0;
        paintCarouselTape(g, string, x, y - BADGE_SPACE, width, head, selectable, null, tornLeft && tornRight);
        return CELL_H;
    }
}
