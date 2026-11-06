package gui.environment;

import automata.Automaton;
import gui.pumping.CFPumpingLemmaChooser;
import gui.pumping.RegPumpingLemmaChooser;
import java.awt.Dimension;
import java.io.Serializable;
import pumping.ContextFreePumpingLemma;
import pumping.RegularPumpingLemma;

public class FrameFactory {
    private static EnvironmentFrame findReusableIdeFrame() {
        EnvironmentFrame[] existing = Universe.frames();
        if (existing != null) {
            for (EnvironmentFrame f : existing) {
                if (f != null && f.isDisplayable()) {
                    return f;
                }
            }
        }
        return null;
    }

    public static EnvironmentFrame createFrame(Serializable object) {
        Environment environment = EnvironmentFactory.getEnvironment(object);
        if (environment == null) {
            return null;
        }
        EnvironmentFrame existing = findReusableIdeFrame();
        if (existing != null) {
            if (object instanceof Automaton) {
                ((Automaton) object).setEnvironmentFrame(existing);
            }
            existing.addOrSwitchEnvironment(environment);
            if (!existing.isVisible()) {
                existing.setVisible(true);
            }
            existing.toFront();
            return existing;
        }

        EnvironmentFrame frame = new EnvironmentFrame(environment);
        if (object instanceof Automaton) {
            ((Automaton) object).setEnvironmentFrame(frame);
        }
        frame.pack();
        int width = 1120;
        int height = 740;
        if (object instanceof RegPumpingLemmaChooser || object instanceof RegularPumpingLemma) {
            width = 850;
            height = 720;
        }
        if (object instanceof CFPumpingLemmaChooser || object instanceof ContextFreePumpingLemma) {
            width = 900;
            height = 780;
        }
        width = Math.max(width, frame.getSize().width);
        height = Math.max(height, frame.getSize().height);
        frame.setSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    public static EnvironmentFrame createFrame(Serializable object, int multiple) {
        Environment environment = EnvironmentFactory.getEnvironment(object);
        if (environment == null) {
            return null;
        }
        EnvironmentFrame existing = findReusableIdeFrame();
        if (existing != null) {
            if (object instanceof Automaton) {
                ((Automaton) object).setEnvironmentFrame(existing);
            }
            existing.addOrSwitchEnvironment(environment);
            if (!existing.isVisible()) {
                existing.setVisible(true);
            }
            existing.toFront();
            return existing;
        }

        EnvironmentFrame frame = new EnvironmentFrame(environment, 0);
        if (object instanceof Automaton) {
            ((Automaton) object).setEnvironmentFrame(frame);
        }
        frame.pack();
        int width = 1120;
        int height = 740;
        if (object instanceof RegPumpingLemmaChooser || object instanceof RegularPumpingLemma) {
            width = 850;
            height = 720;
        }
        if (object instanceof CFPumpingLemmaChooser || object instanceof ContextFreePumpingLemma) {
            width = 900;
            height = 780;
        }
        width = Math.max(width, frame.getSize().width);
        height = Math.max(height, frame.getSize().height);
        frame.setSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    public static EnvironmentFrame createFrame(Serializable object, boolean isTuring) {
        return createFrame(object);
    }
}
