package gui.tree;

import gui.environment.Universe;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.tree.TreeNode;
import plus.Theme;

/**
 * Modern Vector Tree Node Drawer for Derivation Trees (LL/LR/BruteForce/CYK).
 * Replaces legacy 1990s yellow ovals with JetBrains Island Dark rounded vector badges,
 * distinguishing non-terminal internal nodes (accent blue) from terminal leaf nodes (emerald green).
 */
public class DefaultNodeDrawer implements NodeDrawer {
    public static final float NODE_RADIUS = 13.0f;
    public static final Shape NODE_SHAPE = new Ellipse2D.Float(-13.0f, -13.0f, 26.0f, 26.0f);
    public static final Rectangle2D NODE_SIZE = new Rectangle(13, 13, 26, 26);

    @Override
    public void draw(Graphics2D g, TreeNode node) {
        draw(g, node, false);
    }

    public void draw(Graphics2D g, TreeNode node, boolean flag) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        Stroke oldStroke = g.getStroke();

        String s = node != null ? node.toString() : "null";
        if (s == null) {
            s = "null";
        }
        if (s.length() == 0) {
            s = Universe.curProfile.getEmptyString();
        }

        Font nodeFont = new Font("Consolas", Font.BOLD, 12);
        g.setFont(nodeFont);
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(s);
        int boxW = Math.max(flag ? 52 : 30, textW + 16);
        int boxH = 26;

        boolean isLeaf = node != null && node.isLeaf();
        // If caller set a custom highlight color (not yellow/black), honor it as border/glow
        boolean customHighlight = oldColor != null
                && !oldColor.equals(Color.yellow)
                && !oldColor.equals(Color.YELLOW)
                && !oldColor.equals(Theme.BLACK);

        Color fillBg = customHighlight
                ? new Color(oldColor.getRed(), oldColor.getGreen(), oldColor.getBlue(), 60)
                : Theme.TAPE_CELL_BG;
        Color borderCol = customHighlight
                ? oldColor
                : (isLeaf ? Theme.RUN_GREEN : Theme.ACCENT);

        Shape pill = new RoundRectangle2D.Float(-boxW / 2.0f, -boxH / 2.0f, boxW, boxH, 10.0f, 10.0f);
        g.setColor(fillBg);
        g.fill(pill);

        g.setStroke(new BasicStroke(1.8f));
        g.setColor(borderCol);
        g.draw(pill);

        g.setColor(Theme.BLACK);
        int tx = -textW / 2;
        int ty = (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(s, tx, ty);

        g.setStroke(oldStroke);
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    protected Rectangle2D getBounds(Graphics2D g, String string) {
        return g.getFontMetrics().getStringBounds(string, g);
    }

    @Override
    public Rectangle2D nodeSize() {
        return NODE_SIZE;
    }

    @Override
    public Rectangle2D nodeSize(TreeNode node) {
        return nodeSize();
    }

    @Override
    public boolean onNode(TreeNode node, double x, double y) {
        return Math.sqrt(x * x + y * y) <= 16.0;
    }
}
