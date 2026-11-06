package gui.viewer;

import automata.Transition;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;

public class InvisibleCurvedArrow extends CurvedArrow {
    public InvisibleCurvedArrow(int x1, int y1, int x2, int y2, float curvy, Transition t) {
        super(x1, y1, x2, y2, curvy, t);
    }

    public InvisibleCurvedArrow(Point start, Point end, float curvy, Transition t) {
        super(start, end, curvy, t);
    }

    @Override
    public void draw(Graphics2D g) {
        if (this.needsRefresh) {
            refreshCurve();
        }
        // If this arrow is part of a primary arrow's unified group, the primary arrow renders the unified block.
        if (this.groupParent == this) {
            drawText(g);
        }
    }

    @Override
    public void drawHighlight(Graphics2D g) {
        if (this.needsRefresh) {
            refreshCurve();
        }
        // Highlighting is rendered inside the primary arrow's unified block
    }

    @Override
    public Rectangle2D getBounds() {
        if (this.groupParent != null) {
            this.groupParent.updateGroupLayout();
        }
        Area area = new Area(this.bounds);
        if (this.affineToText != null) {
            area.transform(this.affineToText);
        }
        return area.getBounds();
    }

    @Override
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
        }
        return false;
    }
}
