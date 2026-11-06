package gui.viewer;

import automata.Transition;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import plus.Theme;

public class CurvedArrow {
    protected Point start;
    protected Point end;
    protected Point control;
    private Point high = new Point();
    protected float curvy;
    protected QuadCurve2D.Float curve = new QuadCurve2D.Float();
    protected boolean needsRefresh = true;
    protected boolean startArrow = false;
    protected boolean endArrow = false;
    protected String label = "";
    private static double ARROW_ANGLE = 0.3141592653589793;
    private static double ARROW_LENGTH = 14.0;
    private static AffineTransform AFFINE_TURN_180;
    protected Rectangle2D bounds = new Rectangle(0, 0);
    protected AffineTransform affineToText;
    private static final int CHARS_PER_STEP = 4;
    protected static Graphics GRAPHICS = null;
    protected static FontMetrics METRICS;
    private static double HEIGHT = 30.0;
    public static Color HIGHLIGHT_COLOR = new Color(53, 116, 240, 130);
    public Transition myTransition;

    // Unified Multi-Transition Grouping
    private static CurvedArrow lastPrimaryArrow = null;
    protected CurvedArrow groupParent = null;
    protected final List<CurvedArrow> groupArrows = new ArrayList<CurvedArrow>();
    protected int groupIndex = 0;
    protected float cellDrawX = 0f;
    protected float cellBaselineY = 0f;
    protected boolean hiddenInBadge = false;
    protected final Rectangle2D.Float groupBox = new Rectangle2D.Float();
    protected int groupCols = 1;
    protected int groupVisibleCount = 1;
    protected int groupDataRows = 1;
    protected int groupRowHeight = 20;
    protected int[] groupColWidths = new int[]{20};

    static {
        AFFINE_TURN_180 = new AffineTransform();
        AFFINE_TURN_180.rotate(Math.PI);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        GRAPHICS = img.getGraphics();
        METRICS = GRAPHICS.getFontMetrics();
    }

    public CurvedArrow(int x1, int y1, int x2, int y2, float curvy, Transition t) {
        this.start = new Point();
        this.end = new Point();
        this.control = new Point();
        setStart(x1, y1);
        setEnd(x2, y2);
        setCurvy(curvy);
        this.myTransition = t;
        registerInGroup(t);
        refreshCurve();
    }

    public CurvedArrow(Point start, Point end, float curvy, Transition t) {
        setStart(start);
        setEnd(end);
        this.control = new Point();
        setCurvy(curvy);
        this.myTransition = t;
        registerInGroup(t);
        refreshCurve();
    }

    private void registerInGroup(Transition t) {
        if (!(this instanceof InvisibleCurvedArrow)) {
            this.groupParent = this;
            this.groupIndex = 0;
            this.groupArrows.clear();
            this.groupArrows.add(this);
            lastPrimaryArrow = this;
        } else if (lastPrimaryArrow != null
                && lastPrimaryArrow.myTransition != null
                && t != null
                && lastPrimaryArrow.myTransition.getFromState() == t.getFromState()
                && lastPrimaryArrow.myTransition.getToState() == t.getToState()) {
            this.groupParent = lastPrimaryArrow;
            this.groupIndex = lastPrimaryArrow.groupArrows.size();
            lastPrimaryArrow.groupArrows.add(this);
        } else {
            this.groupParent = this;
            this.groupIndex = 0;
            this.groupArrows.clear();
            this.groupArrows.add(this);
        }
    }

    public void setStart(int x, int y) {
        this.start.x = x;
        this.start.y = y;
        this.needsRefresh = true;
    }

    public void setStart(Point start) {
        this.start = start;
        this.needsRefresh = true;
    }

    public void setEnd(int x, int y) {
        this.end.x = x;
        this.end.y = y;
        this.needsRefresh = true;
    }

    public void setEnd(Point end) {
        this.end = end;
        this.needsRefresh = true;
    }

    public void setCurvy(float curvy) {
        this.curvy = curvy;
        this.needsRefresh = true;
    }

    public void draw(Graphics2D g) {
        if (this.needsRefresh) {
            refreshCurve();
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Check if any sibling transition in this edge group is selected or just fired in simulation
        boolean anySiblingSelected = false;
        boolean anySiblingFired = false;
        for (int i = 0; i < this.groupArrows.size(); i++) {
            Transition tr = this.groupArrows.get(i).myTransition;
            if (tr != null) {
                if (tr.isSelected) {
                    anySiblingSelected = true;
                }
                if (Theme.isTransitionFired(tr)) {
                    anySiblingFired = true;
                }
            }
        }
        if (anySiblingFired) {
            Graphics2D gh = (Graphics2D) g.create();
            gh.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gh.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gh.setColor(new Color(Theme.FIRED_CYAN.getRed(), Theme.FIRED_CYAN.getGreen(), Theme.FIRED_CYAN.getBlue(), 70));
            gh.draw(this.curve);
            gh.dispose();
        } else if (anySiblingSelected) {
            Graphics2D gh = (Graphics2D) g.create();
            gh.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gh.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gh.setColor(HIGHLIGHT_COLOR);
            gh.draw(this.curve);
            gh.dispose();
        }

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(anySiblingFired ? 2.8f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(anySiblingFired ? Theme.FIRED_CYAN : Theme.BLACK);
        g.draw(this.curve);
        drawArrow(g, this.end, this.control);
        g.setStroke(oldStroke);
        drawText(g);
    }

    public void drawControlPoint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = (int) this.curve.getCtrlX();
        int cy = (int) this.curve.getCtrlY();
        Color old = g.getColor();
        g.setColor(Theme.ACCENT);
        g.fillOval(cx - 5, cy - 5, 10, 10);
        g.setColor(Theme.BLACK);
        g.drawOval(cx - 5, cy - 5, 10, 10);
        g.setColor(old);
    }

    public void drawHighlight(Graphics2D g) {
        if (this.needsRefresh) {
            refreshCurve();
        }
        // When the primary transition is selected, AutomatonDrawer calls drawHighlight instead of draw.
        // Call draw(g) so the curve, arrowhead, and unified container are all rendered cleanly!
        if (!(this instanceof InvisibleCurvedArrow)) {
            draw(g);
        }
    }

    public void drawText(Graphics2D g) {
        if (this instanceof InvisibleCurvedArrow && this.groupParent != this) {
            // Drawn as part of the primary arrow's unified container block
            return;
        }
        updateGroupLayout();
        if (this.groupArrows.isEmpty()) {
            return;
        }

        boolean anyFired = false;
        for (int i = 0; i < this.groupArrows.size(); i++) {
            if (Theme.isTransitionFired(this.groupArrows.get(i).myTransition)) {
                anyFired = true;
                break;
            }
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.transform(this.affineToText);
        g2.setFont(Theme.MONO_FONT);

        int bx = Math.round(this.groupBox.x);
        int by = Math.round(this.groupBox.y);
        int bw = Math.round(this.groupBox.width);
        int bh = Math.round(this.groupBox.height);

        // Soft drop shadow under unified container
        if (Theme.isDark()) {
            g2.setColor(new Color(0, 0, 0, 75));
            g2.fillRoundRect(bx + 1, by + 2, bw, bh, 10, 10);
        }

        // Unified Container Background (#1E1F22 at 90% opacity in Dark mode)
        g2.setColor(Theme.LABEL_PILL_BG);
        g2.fillRoundRect(bx, by, bw, bh, 10, 10);

        // Rounded Border (#393B40 default, or #4CC2FF cyan when an edge rule just fired)
        g2.setColor(anyFired ? Theme.FIRED_CYAN : Theme.CARD_BORDER);
        g2.setStroke(new BasicStroke(anyFired ? 1.5f : 1.0f));
        g2.drawRoundRect(bx, by, bw, bh, 10, 10);

        int padX = 7;
        int padY = 6;
        int colGap = 16;

        // Draw each visible transition cell inside the unified container
        for (int k = 0; k < this.groupVisibleCount; k++) {
            CurvedArrow ak = this.groupArrows.get(k);
            boolean fired = ak.myTransition != null && Theme.isTransitionFired(ak.myTransition);
            boolean sel = ak.myTransition != null && ak.myTransition.isSelected;
            if (fired) {
                int cx = (int) Math.round(ak.bounds.getX());
                int cy = (int) Math.round(ak.bounds.getY()) + 1;
                int cw = (int) Math.round(ak.bounds.getWidth());
                int ch = (int) Math.round(ak.bounds.getHeight()) - 2;
                g2.setColor(new Color(Theme.FIRED_GREEN.getRed(), Theme.FIRED_GREEN.getGreen(), Theme.FIRED_GREEN.getBlue(), 55));
                g2.fillRoundRect(cx, cy, cw, ch, 6, 6);
                g2.setColor(Theme.FIRED_GREEN);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(cx, cy, cw, ch, 6, 6);
                g2.setStroke(new BasicStroke(1.0f));
            } else if (sel) {
                int cx = (int) Math.round(ak.bounds.getX());
                int cy = (int) Math.round(ak.bounds.getY()) + 1;
                int cw = (int) Math.round(ak.bounds.getWidth());
                int ch = (int) Math.round(ak.bounds.getHeight()) - 2;
                g2.setColor(Theme.ACTIVE_BG);
                g2.fillRoundRect(cx, cy, cw, ch, 6, 6);
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(cx, cy, cw, ch, 6, 6);
            }

            g2.setColor(fired ? Color.WHITE : Theme.BLACK);
            String txt = ak.getLabel() != null ? ak.getLabel() : "";
            g2.drawString(txt, ak.cellDrawX, ak.cellBaselineY);

            // Draw '|' separator between columns on the same row
            int c = k % this.groupCols;
            if (c < this.groupCols - 1 && (k + 1) < this.groupVisibleCount) {
                float sepX = bx + padX;
                for (int j = 0; j <= c; j++) {
                    sepX += this.groupColWidths[j];
                    if (j < c) {
                        sepX += colGap;
                    }
                }
                sepX += colGap / 2.0f - 3.0f;
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString("|", sepX, ak.cellBaselineY);
            }
        }

        // If N > groupVisibleCount, draw the clean "+X más" badge row at the bottom of the container
        int totalN = this.groupArrows.size();
        if (totalN > this.groupVisibleCount) {
            int hidden = totalN - this.groupVisibleCount;
            String firedHiddenLabel = null;
            for (int k = this.groupVisibleCount; k < totalN; k++) {
                CurvedArrow hk = this.groupArrows.get(k);
                if (hk.myTransition != null && Theme.isTransitionFired(hk.myTransition)) {
                    firedHiddenLabel = hk.getLabel();
                    break;
                }
            }
            String badge = (firedHiddenLabel != null)
                    ? (firedHiddenLabel + " (+" + hidden + ")")
                    : ("+" + hidden + " más");
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(badge);
            int badgeW = tw + 14;
            int badgeH = this.groupRowHeight - 3;
            int badgeX = Math.round(bx + (bw - badgeW) / 2.0f);
            int badgeY = Math.round(by + padY + this.groupDataRows * this.groupRowHeight + 1);

            if (firedHiddenLabel != null) {
                g2.setColor(new Color(Theme.FIRED_GREEN.getRed(), Theme.FIRED_GREEN.getGreen(), Theme.FIRED_GREEN.getBlue(), 65));
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
                g2.setColor(Theme.FIRED_GREEN);
                g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
                g2.setColor(Theme.ACCENT);
                g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
                g2.setColor(Theme.BLACK);
            }
            float textX = badgeX + (badgeW - tw) / 2.0f;
            float textY = badgeY + (badgeH - (fm.getAscent() + fm.getDescent())) / 2.0f + fm.getAscent();
            g2.drawString(badge, textX, textY);
        }

        g2.dispose();
    }

    protected void updateGroupLayout() {
        CurvedArrow primary = (this.groupParent != null) ? this.groupParent : this;
        if (primary != this) {
            primary.updateGroupLayout();
            return;
        }
        if (this.needsRefresh || this.affineToText == null) {
            refreshCurveInternal();
        }

        GRAPHICS.setFont(Theme.MONO_FONT);
        FontMetrics fm = GRAPHICS.getFontMetrics(Theme.MONO_FONT);
        METRICS = fm;

        int n = this.groupArrows.size();
        if (n == 0) {
            return;
        }

        int maxLen = 0;
        boolean anySelected = false;
        for (int i = 0; i < n; i++) {
            CurvedArrow a = this.groupArrows.get(i);
            String s = a.getLabel();
            if (s != null && s.length() > maxLen) {
                maxLen = s.length();
            }
            if (a.myTransition != null && a.myTransition.isSelected) {
                anySelected = true;
            }
        }

        // Smart multi-column compaction so transitions never form tall towers or wide overlapping boxes:
        // - Up to 3 short 1-2 char labels on a single row (e.g. @ | & | %)
        // - For > 4 transitions (like Turing Machine edges), show 2x2 (first 4 rules separated by '|') + "+X más" badge!
        int cols;
        if (n == 1) {
            cols = 1;
        } else if (n == 2) {
            cols = (maxLen <= 6) ? 2 : 1;
        } else if (n == 3) {
            cols = (maxLen <= 3) ? 3 : 2;
        } else {
            cols = (maxLen <= 3 && !anySelected) ? 3 : 2;
        }

        int maxVisible = (n > 4 && !anySelected) ? 4 : Math.min(n, 12);
        int dataRows = (maxVisible + cols - 1) / cols;
        boolean hasBadge = (maxVisible < n);
        int totalRows = dataRows + (hasBadge ? 1 : 0);

        int[] colW = new int[cols];
        for (int c = 0; c < cols; c++) {
            colW[c] = 10;
        }
        for (int k = 0; k < maxVisible; k++) {
            int c = k % cols;
            String lbl = this.groupArrows.get(k).getLabel();
            int w = fm.stringWidth(lbl != null ? lbl : "");
            if (w > colW[c]) {
                colW[c] = w;
            }
        }

        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int rowH = ascent + descent + 5; // 20px per row -> zero character clipping!
        int padX = 7;
        int padY = 6;                    // 6px container padding
        int colGap = 16;

        int contentW = 0;
        for (int c = 0; c < cols; c++) {
            contentW += colW[c];
        }
        contentW += (cols - 1) * colGap;

        if (hasBadge) {
            String badge = "+" + (n - maxVisible) + " más";
            contentW = Math.max(contentW, fm.stringWidth(badge) + 18);
        }

        float boxW = contentW + padX * 2;
        float boxH = totalRows * rowH + padY * 2;
        float boxX = -boxW / 2.0f;

        boolean upsideDown = this.end.x < this.start.x;
        boolean belowCurve = (this.curvy < 0.0f) ^ upsideDown;
        double spanDist = Math.hypot(this.end.x - this.start.x, this.end.y - this.start.y);
        float clearance = (spanDist > 1.0 && spanDist < 110.0) ? 14.0f : 5.0f;
        float boxY = belowCurve ? clearance : (-clearance - boxH);

        this.groupBox.setRect(boxX, boxY, boxW, boxH);
        this.groupCols = cols;
        this.groupVisibleCount = maxVisible;
        this.groupDataRows = dataRows;
        this.groupRowHeight = rowH;
        this.groupColWidths = colW;

        for (int k = 0; k < n; k++) {
            CurvedArrow ak = this.groupArrows.get(k);
            ak.affineToText = new AffineTransform(this.affineToText);
            if (k < maxVisible) {
                ak.hiddenInBadge = false;
                int r = k / cols;
                int c = k % cols;
                float cx = boxX + padX;
                for (int j = 0; j < c; j++) {
                    cx += colW[j] + colGap;
                }
                float cy = boxY + padY + r * rowH;
                String lbl = ak.getLabel() != null ? ak.getLabel() : "";
                int lw = fm.stringWidth(lbl);
                ak.cellDrawX = cx + (colW[c] - lw) / 2.0f;
                ak.cellBaselineY = cy + (rowH - (ascent + descent)) / 2.0f + ascent;
                ak.bounds = new Rectangle2D.Float(cx - 2, cy, colW[c] + 4, rowH);
            } else {
                ak.hiddenInBadge = true;
                float cy = boxY + padY + dataRows * rowH;
                ak.bounds = new Rectangle2D.Float(boxX + padX, cy, contentW, rowH);
            }
        }
    }

    public void setLabel(String label) {
        if (label != null) {
            this.label = label.replace(" ; ", ";").replace(" , ", ",");
        } else {
            this.label = "";
        }
        updateGroupLayout();
    }

    public String getLabel() {
        return this.label;
    }

    private void drawArrow(Graphics g, Point tip, Point tail) {
        double angle = Math.atan2(tail.x - tip.x, tail.y - tip.y);
        angle += ARROW_ANGLE;
        int x1 = ((int) (Math.sin(angle) * ARROW_LENGTH)) + tip.x;
        int y1 = ((int) (Math.cos(angle) * ARROW_LENGTH)) + tip.y;
        g.drawLine(tip.x, tip.y, x1, y1);
        angle -= 2.0 * ARROW_ANGLE;
        int x2 = ((int) (Math.sin(angle) * ARROW_LENGTH)) + tip.x;
        int y2 = ((int) (Math.cos(angle) * ARROW_LENGTH)) + tip.y;
        g.drawLine(tip.x, tip.y, x2, y2);
    }

    public void refreshCurve() {
        refreshCurveInternal();
        updateGroupLayout();
    }

    private void refreshCurveInternal() {
        this.needsRefresh = false;
        double lengthx = this.end.x - this.start.x;
        double lengthy = this.end.y - this.start.y;
        double centerx = ((double) (this.start.x + this.end.x)) / 2.0;
        double centery = ((double) (this.start.y + this.end.y)) / 2.0;
        double length = Math.sqrt(lengthx * lengthx + lengthy * lengthy);
        double factorx = length == 0.0 ? 0.0 : lengthx / length;
        double factory = length == 0.0 ? 0.0 : lengthy / length;

        if (this.myTransition == null || this.myTransition.getControl() == null) {
            this.control.x = (int) (centerx + ((double) this.curvy) * HEIGHT * factory);
            this.control.y = (int) (centery - ((double) this.curvy) * HEIGHT * factorx);
            this.high.x = (int) (centerx + (((double) this.curvy) * HEIGHT * factory) / 2.0);
            this.high.y = (int) (centery - (((double) this.curvy) * HEIGHT * factorx) / 2.0);
        } else {
            this.control.x = this.myTransition.getControl().x;
            this.control.y = this.myTransition.getControl().y;
            double dfx = (double) this.control.x - centerx;
            double dfy = centery - (double) this.control.y;
            this.high.x = (int) (centerx + dfx / 2.0);
            this.high.y = (int) (centery - dfy / 2.0);
        }

        this.curve.setCurve((float) this.start.x, (float) this.start.y,
                (float) this.control.x, (float) this.control.y,
                (float) this.end.x, (float) this.end.y);

        this.affineToText = new AffineTransform();
        this.affineToText.translate(this.high.x, this.high.y);
        this.affineToText.rotate(Math.atan2(lengthy, lengthx));
        if (this.end.x < this.start.x) {
            this.affineToText.rotate(Math.PI);
        }
    }

    public Rectangle2D getBounds() {
        if (this.needsRefresh) {
            refreshCurve();
        }
        if (this instanceof InvisibleCurvedArrow && this.groupParent != null && this.groupParent != this) {
            this.groupParent.updateGroupLayout();
            Area area = new Area(this.bounds);
            area.transform(this.affineToText);
            return area.getBounds();
        }
        updateGroupLayout();
        Rectangle b = this.curve.getBounds();
        Area area = new Area(this.groupBox);
        area.transform(this.affineToText);
        b.add(area.getBounds());
        return b;
    }

    public boolean isNear(Point point, int fudge) {
        if (this.needsRefresh) {
            refreshCurve();
        }
        if (this.groupParent != null) {
            this.groupParent.updateGroupLayout();
        }
        try {
            if (this.affineToText != null && this.bounds.contains(this.affineToText.inverseTransform(point, null))) {
                return true;
            }
        } catch (NoninvertibleTransformException e) {
        } catch (NullPointerException e) {
            return false;
        }
        if (this instanceof InvisibleCurvedArrow) {
            return false;
        }
        return intersects(point, fudge, this.curve);
    }

    private boolean intersects(Point point, int fudge, QuadCurve2D.Float c) {
        if (!c.intersects(point.x - fudge, point.y - fudge, fudge << 1, fudge << 1)) {
            return false;
        }
        if (c.getFlatness() < fudge) {
            return true;
        }
        QuadCurve2D.Float f1 = new QuadCurve2D.Float();
        QuadCurve2D.Float f2 = new QuadCurve2D.Float();
        c.subdivide(f1, f2);
        return intersects(point, fudge, f1) || intersects(point, fudge, f2);
    }

    public QuadCurve2D getCurve() {
        return this.curve;
    }
}
