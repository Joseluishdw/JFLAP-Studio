package gui.sim;

import automata.Configuration;
import automata.State;
import automata.turing.TMConfiguration;
import automata.turing.Tape;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class TMConfigurationIcon extends ConfigurationIcon implements TuringConstants {
    private TMConfiguration config;
    private float[] animatedHead;
    private Timer smoothTimer;
    private Component hostComponent;

    public TMConfigurationIcon(Configuration configuration) {
        super(configuration);
        this.config = (TMConfiguration) configuration;
        initSmoothHead();
    }

    private void initSmoothHead() {
        Tape[] tapes = this.config.getTapes();
        this.animatedHead = new float[tapes.length];
        Configuration parent = this.config.getParent();
        TMConfiguration parentTM = (parent instanceof TMConfiguration) ? (TMConfiguration) parent : null;
        Tape[] parentTapes = (parentTM != null) ? parentTM.getTapes() : null;

        for (int i = 0; i < tapes.length; i++) {
            int currHead = tapes[i].getTapeHead();
            float startHead = currHead;
            if (parentTapes != null && i < parentTapes.length) {
                int prevHead = parentTapes[i].getTapeHead();
                int delta = currHead - prevHead;
                if (delta == 0 && currHead == 0
                        && tapes[i].getContents().length() > parentTapes[i].getContents().length()) {
                    delta = -1;
                }
                if (delta < -2) {
                    delta = -2;
                } else if (delta > 2) {
                    delta = 2;
                }
                startHead = currHead - delta;
            }
            this.animatedHead[i] = startHead;
        }
    }

    /**
     * Triggers a smooth horizontal scroll transition from a previous head offset.
     */
    public void triggerSmoothScroll(int deltaDirection) {
        Tape[] tapes = this.config.getTapes();
        if (this.animatedHead == null || this.animatedHead.length != tapes.length) {
            this.animatedHead = new float[tapes.length];
        }
        for (int i = 0; i < tapes.length; i++) {
            this.animatedHead[i] = tapes[i].getTapeHead() - deltaDirection;
        }
        ensureTimerRunning();
    }

    private void ensureTimerRunning() {
        if (this.smoothTimer != null && this.smoothTimer.isRunning()) {
            return;
        }
        this.smoothTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Tape[] tapes = config.getTapes();
                boolean stillMoving = false;
                for (int i = 0; i < tapes.length; i++) {
                    int target = tapes[i].getTapeHead();
                    float diff = target - animatedHead[i];
                    if (Math.abs(diff) > 0.015f) {
                        animatedHead[i] += diff * 0.26f;
                        stillMoving = true;
                    } else {
                        animatedHead[i] = target;
                    }
                }
                if (hostComponent != null) {
                    hostComponent.repaint();
                }
                if (!stillMoving && smoothTimer != null) {
                    smoothTimer.stop();
                }
            }
        });
        this.smoothTimer.start();
    }

    @Override
    public int getIconHeight() {
        int numTapes = (this.config != null && this.config.getTapes() != null)
                ? this.config.getTapes().length
                : 1;
        return 82 * numTapes + 6;
    }

    @Override
    public void paintConfiguration(Component c, Graphics2D g, int width, int height) {
        this.hostComponent = c;
        Tape[] tapes = this.config.getTapes();
        if (this.animatedHead == null || this.animatedHead.length != tapes.length) {
            initSmoothHead();
        }

        boolean needsAnim = false;
        for (int i = 0; i < tapes.length; i++) {
            if (Math.abs(this.animatedHead[i] - tapes[i].getTapeHead()) > 0.015f) {
                needsAnim = true;
                break;
            }
        }
        if (needsAnim) {
            ensureTimerRunning();
        }

        State currentState = this.config.getCurrentState();
        String stateName = (currentState != null) ? currentState.getName() : "q0";
        String stateLabels = "";
        if (currentState != null && currentState.getLabels() != null && currentState.getLabels().length > 0) {
            stateLabels = " (" + currentState.getLabels()[0] + ")";
        }

        int rowHeight = 82;
        float startY = Math.max(2f, (height - tapes.length * rowHeight) / 2.0f);

        for (int i = 0; i < tapes.length; i++) {
            String badge = (tapes.length > 1)
                    ? (stateName + stateLabels + " · T" + (i + 1))
                    : (stateName + stateLabels);
            String contents = tapes[i].getContents();
            int headIdx = tapes[i].getTapeHead();
            Torn.paintCarouselTape(g, contents, 4f, startY + i * rowHeight,
                    Math.max(80f, width - 8f), this.animatedHead[i], headIdx, badge, true);
        }
    }
}
