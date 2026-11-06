package gui.sim;

import automata.Automaton;
import automata.AutomatonSimulator;
import automata.Configuration;
import automata.State;
import automata.Transition;
import automata.fsa.FSAConfiguration;
import automata.fsa.FSATransition;
import automata.mealy.MealyConfiguration;
import automata.mealy.MealyTransition;
import automata.pda.PDAConfiguration;
import automata.pda.PDATransition;
import automata.turing.TMConfiguration;
import automata.turing.TMSimulator;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.turing.Tape;
import automata.turing.TuringMachine;
import gui.viewer.SelectionDrawer;
import java.awt.Component;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import plus.Theme;

public class ConfigurationController implements ConfigurationSelectionListener {
    private ConfigurationPane configurations;
    private AutomatonSimulator simulator;
    private SelectionDrawer drawer;
    private Component component;
    private HashMap<Configuration, TraceWindow> configurationToTraceWindow = new HashMap<Configuration, TraceWindow>();
    private Configuration[] originalConfigurations = new Configuration[0];

    // History stack for Step Back ("◀ Paso Atrás")
    private final Deque<StepSnapshot> historyStack = new ArrayDeque<StepSnapshot>();
    private final Set<Transition> lastFiredTransitions = new HashSet<Transition>();
    private Runnable statusCallback;

    public void setStatusCallback(Runnable callback) {
        this.statusCallback = callback;
    }

    private void notifyStatusChanged() {
        if (this.statusCallback != null) {
            this.statusCallback.run();
        }
    }

    private static final String NO_CONFIGURATION_ERROR = "Selecciona al menos una configuración.";
    private static final String NO_CONFIGURATION_ERROR_TITLE = "Sin configuración seleccionada";
    private static final String FOCUS_CONFIGURATION_ERROR = "JFLAP solo puede enfocar una configuración a la vez.";
    private static final String FOCUS_CONFIGURATION_ERROR_TITLE = "Demasiadas configuraciones seleccionadas";

    private static final class ConfigEntry {
        final Configuration config;
        final int buttonState;

        ConfigEntry(Configuration config, int buttonState) {
            this.config = config;
            this.buttonState = buttonState;
        }
    }

    private static final class StepSnapshot {
        final List<ConfigEntry> entries;
        final Set<Transition> firedTransitions;
        final int lastHeadDelta;

        StepSnapshot(List<ConfigEntry> entries, Set<Transition> firedTransitions, int lastHeadDelta) {
            this.entries = entries;
            this.firedTransitions = new HashSet<Transition>(firedTransitions);
            this.lastHeadDelta = lastHeadDelta;
        }
    }

    public ConfigurationController(ConfigurationPane pane, AutomatonSimulator simulator,
                                   SelectionDrawer drawer, Component component) {
        this.configurations = pane;
        this.simulator = simulator;
        this.drawer = drawer;
        this.component = component;
        Theme.clearFiredTransitions();
        changeSelection();
        this.configurations.addSelectionListener(this);
        this.originalConfigurations = cloneConfigurationsArray(this.configurations.getConfigurations());
    }

    private Configuration cloneSingleConfiguration(Configuration c) {
        if (c instanceof TMConfiguration) {
            return (Configuration) ((TMConfiguration) c).clone();
        }
        return c;
    }

    private Configuration[] cloneConfigurationsArray(Configuration[] src) {
        if (src == null) {
            return new Configuration[0];
        }
        Configuration[] copy = new Configuration[src.length];
        for (int i = 0; i < src.length; i++) {
            copy[i] = cloneSingleConfiguration(src[i]);
        }
        return copy;
    }

    public boolean canStepBack() {
        return !this.historyStack.isEmpty();
    }

    public int getStepCount() {
        return this.historyStack.size();
    }

    public void reset() {
        this.historyStack.clear();
        this.lastFiredTransitions.clear();
        Theme.clearFiredTransitions();
        this.configurations.clear();

        if (this.simulator instanceof TMSimulator) {
            TMSimulator tmSim = (TMSimulator) this.simulator;
            Configuration[] inits = tmSim.getInitialConfigurations(tmSim.getInputStrings());
            for (int i = 0; i < inits.length; i++) {
                this.configurations.add(inits[i]);
            }
        } else {
            for (int i = 0; i < this.originalConfigurations.length; i++) {
                Configuration c = cloneSingleConfiguration(this.originalConfigurations[i]);
                c.reset();
                this.configurations.add(c);
            }
        }
        this.configurations.validate();
        this.configurations.repaint();
        changeSelection();
    }

    public void cleanup() {
        Collection<TraceWindow> windows = this.configurationToTraceWindow.values();
        for (TraceWindow w : windows) {
            w.dispose();
        }
        this.configurationToTraceWindow.clear();
        Theme.clearActiveSimStates();
        Theme.clearFiredTransitions();
    }

    /**
     * Steps backward one step in the simulation history ("◀ Paso Atrás"),
     * restoring the previous tape and state configurations with smooth reverse tape scrolling.
     */
    public void stepBack() {
        if (this.historyStack.isEmpty()) {
            return;
        }
        StepSnapshot snap = this.historyStack.pop();
        this.configurations.clear();
        for (ConfigEntry entry : snap.entries) {
            Configuration restored = cloneSingleConfiguration(entry.config);
            this.configurations.add(restored, entry.buttonState);
            ConfigurationButton btn = this.configurations.getButton(restored);
            if (btn != null && btn.getConfigurationIcon() instanceof TMConfigurationIcon && snap.lastHeadDelta != 0) {
                ((TMConfigurationIcon) btn.getConfigurationIcon()).triggerSmoothScroll(-snap.lastHeadDelta);
            }
        }
        this.lastFiredTransitions.clear();
        this.lastFiredTransitions.addAll(snap.firedTransitions);
        Theme.setFiredTransitions(this.lastFiredTransitions);

        this.configurations.validate();
        this.configurations.repaint();
        changeSelection();
    }

    public void step(boolean blockStep) {
        Configuration[] valid = this.configurations.getValidConfigurations();
        if (valid.length == 0) {
            return;
        }

        // 1. Save deep snapshot of current configurations before stepping so Step Back works cleanly
        Configuration[] allCurrent = this.configurations.getConfigurations();
        List<ConfigEntry> snapEntries = new ArrayList<ConfigEntry>(allCurrent.length);
        for (Configuration c : allCurrent) {
            snapEntries.add(new ConfigEntry(cloneSingleConfiguration(c), this.configurations.getState(c)));
        }

        // Also keep pre-step snapshots of valid configurations to detect fired transitions & head movement
        Configuration[] beforeValid = cloneConfigurationsArray(valid);

        ArrayList<Configuration> nextList = new ArrayList<Configuration>();
        HashSet<Configuration> rejectedSet = new HashSet<Configuration>();
        Set<Transition> newlyFired = new HashSet<Transition>();
        HashMap<Configuration, Integer> headDeltaMap = new HashMap<Configuration, Integer>();
        int primaryDelta = 0;

        this.configurations.clearThawed();

        if (!blockStep) {
            for (int i = 0; i < valid.length; i++) {
                Configuration beforeSnap = beforeValid[i];
                @SuppressWarnings("unchecked")
                ArrayList<Configuration> children = this.simulator.stepConfiguration(valid[i]);
                if (children.size() == 0) {
                    rejectedSet.add(valid[i]);
                    nextList.add(valid[i]);
                } else {
                    for (Configuration child : children) {
                        nextList.add(child);
                        Transition fired = findFiredTransition(beforeSnap, child);
                        if (fired != null) {
                            newlyFired.add(fired);
                            int delta = computeHeadDelta(fired, beforeSnap, child);
                            headDeltaMap.put(child, Integer.valueOf(delta));
                            if (primaryDelta == 0 && delta != 0) {
                                primaryDelta = delta;
                            }
                        }
                    }
                }
            }
        } else {
            if (valid.length > 0 && valid[0] instanceof TMConfiguration && this.simulator instanceof TMSimulator) {
                Configuration beforeSnap = beforeValid[0];
                TMSimulator tmSim = (TMSimulator) this.simulator;
                @SuppressWarnings("unchecked")
                List<Configuration> children = tmSim.stepBlock((TMConfiguration) valid[0]);
                if (children.size() == 0) {
                    rejectedSet.add(valid[0]);
                    nextList.add(valid[0]);
                } else {
                    for (Configuration child : children) {
                        nextList.add(child);
                        Transition fired = findFiredTransition(beforeSnap, child);
                        if (fired != null) {
                            newlyFired.add(fired);
                            int delta = computeHeadDelta(fired, beforeSnap, child);
                            headDeltaMap.put(child, Integer.valueOf(delta));
                            if (primaryDelta == 0 && delta != 0) {
                                primaryDelta = delta;
                            }
                        }
                    }
                }
            }
        }

        // Push snapshot onto history stack
        this.historyStack.push(new StepSnapshot(snapEntries, this.lastFiredTransitions, primaryDelta));
        while (this.historyStack.size() > 200) {
            this.historyStack.removeLast();
        }

        this.lastFiredTransitions.clear();
        this.lastFiredTransitions.addAll(newlyFired);
        Theme.setFiredTransitions(this.lastFiredTransitions);

        Iterator<Configuration> it = nextList.iterator();
        while (it.hasNext()) {
            Configuration next = it.next();
            this.configurations.add(next);
            ConfigurationButton btn = this.configurations.getButton(next);
            Integer delta = headDeltaMap.get(next);
            if (btn != null && delta != null && delta.intValue() != 0
                    && btn.getConfigurationIcon() instanceof TMConfigurationIcon) {
                ((TMConfigurationIcon) btn.getConfigurationIcon()).triggerSmoothScroll(delta.intValue());
            }
            if (rejectedSet.contains(next)) {
                this.configurations.setReject(next);
            }
        }

        this.configurations.validate();
        this.configurations.repaint();
        changeSelection();

        try {
            JSplitPane split = (JSplitPane) this.configurations.getParent().getParent().getParent().getParent();
            int loc = split.getDividerLocation();
            split.setDividerLocation(loc - 1);
            split.setDividerLocation(loc);
        } catch (Throwable ignored) {
        }
    }

    private Transition findFiredTransition(Configuration before, Configuration after) {
        if (before == null || after == null) {
            return null;
        }
        State from = before.getCurrentState();
        State to = after.getCurrentState();
        if (from == null || to == null || from.getAutomaton() == null) {
            return null;
        }
        Automaton aut = from.getAutomaton();
        Transition[] candidates = aut.getTransitionsFromStateToState(from, to);
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        if (candidates.length == 1) {
            return candidates[0];
        }

        if (before instanceof TMConfiguration) {
            Tape[] beforeTapes = ((TMConfiguration) before).getTapes();
            for (Transition tr : candidates) {
                if (tr instanceof TMTransition) {
                    TMTransition tmt = (TMTransition) tr;
                    boolean match = true;
                    int count = Math.min(beforeTapes.length, tmt.tapes());
                    for (int t = 0; t < count; t++) {
                        String readRule = tmt.getRead(t);
                        String tapeRead = beforeTapes[t].read();
                        if (isBlankSymbol(readRule) && isBlankSymbol(tapeRead)) {
                            continue;
                        }
                        if ("~".equals(readRule) || (readRule != null && readRule.equals(tapeRead))) {
                            continue;
                        }
                        match = false;
                        break;
                    }
                    if (match) {
                        return tmt;
                    }
                }
            }
        } else if (before instanceof FSAConfiguration && after instanceof FSAConfiguration) {
            String u1 = ((FSAConfiguration) before).getUnprocessedInput();
            String u2 = ((FSAConfiguration) after).getUnprocessedInput();
            String consumed = (u1 != null && u2 != null && u1.length() >= u2.length())
                    ? u1.substring(0, u1.length() - u2.length())
                    : "";
            for (Transition tr : candidates) {
                if (tr instanceof FSATransition) {
                    String lbl = ((FSATransition) tr).getLabel();
                    if (consumed.equals(lbl != null ? lbl : "")) {
                        return tr;
                    }
                }
            }
        } else if (before instanceof PDAConfiguration && after instanceof PDAConfiguration) {
            String u1 = ((PDAConfiguration) before).getUnprocessedInput();
            String u2 = ((PDAConfiguration) after).getUnprocessedInput();
            String consumed = (u1 != null && u2 != null && u1.length() >= u2.length())
                    ? u1.substring(0, u1.length() - u2.length())
                    : "";
            for (Transition tr : candidates) {
                if (tr instanceof PDATransition) {
                    String lbl = ((PDATransition) tr).getInputToRead();
                    if (consumed.equals(lbl != null ? lbl : "")) {
                        return tr;
                    }
                }
            }
        } else if (before instanceof MealyConfiguration && after instanceof MealyConfiguration) {
            String u1 = ((MealyConfiguration) before).getUnprocessedInput();
            String u2 = ((MealyConfiguration) after).getUnprocessedInput();
            String consumed = (u1 != null && u2 != null && u1.length() >= u2.length())
                    ? u1.substring(0, u1.length() - u2.length())
                    : "";
            for (Transition tr : candidates) {
                if (tr instanceof MealyTransition) {
                    String lbl = ((MealyTransition) tr).getLabel();
                    if (consumed.equals(lbl != null ? lbl : "")) {
                        return tr;
                    }
                }
            }
        }
        return candidates[0];
    }

    private boolean isBlankSymbol(String s) {
        return s == null || s.isEmpty() || " ".equals(s) || "\u25A1".equals(s);
    }

    private int computeHeadDelta(Transition fired, Configuration before, Configuration after) {
        if (fired instanceof TMTransition) {
            String dir = ((TMTransition) fired).getDirection(0);
            if ("R".equalsIgnoreCase(dir)) {
                return 1;
            }
            if ("L".equalsIgnoreCase(dir)) {
                return -1;
            }
            return 0;
        }
        if (before instanceof TMConfiguration && after instanceof TMConfiguration) {
            Tape[] t1 = ((TMConfiguration) before).getTapes();
            Tape[] t2 = ((TMConfiguration) after).getTapes();
            if (t1.length > 0 && t2.length > 0) {
                int d = t2[0].getTapeHead() - t1[0].getTapeHead();
                if (d == 0 && t2[0].getTapeHead() == 0
                        && t2[0].getContents().length() > t1[0].getContents().length()) {
                    return -1;
                }
                return d;
            }
        }
        return 0;
    }

    public void freeze() {
        Configuration[] selected = this.configurations.getSelected();
        if (selected.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR,
                    NO_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < selected.length; i++) {
            this.configurations.setFrozen(selected[i]);
        }
        this.configurations.deselectAll();
        this.configurations.repaint();
    }

    public void remove() {
        Configuration[] selected = this.configurations.getSelected();
        if (selected.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR,
                    NO_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < selected.length; i++) {
            this.configurations.remove(selected[i]);
        }
        this.configurations.validate();
        this.configurations.repaint();
    }

    public void focus() {
        Configuration[] selected = this.configurations.getSelected();
        if (selected.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR,
                    NO_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selected.length > 1) {
            JOptionPane.showMessageDialog(this.configurations, FOCUS_CONFIGURATION_ERROR,
                    FOCUS_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        Configuration c = selected[0];
        this.configurations.setFocused(c);
        c.setFocused(true);
        this.component.repaint();
    }

    public void changeSelection() {
        this.drawer.clearSelected();
        this.drawer.clearSelectedTransitions();
        Set<State> activeStates = new HashSet<State>();

        Configuration[] all = this.configurations.getConfigurations();
        boolean focusedSet = false;
        for (int i = 0; i < all.length; i++) {
            Configuration c = all[i];
            focusedSet = setFocusIfNeeded(c, focusedSet);
            if (c instanceof TMConfiguration) {
                TMState s = (TMState) c.getCurrentState();
                while (((TuringMachine) s.getAutomaton()).getParent() != null) {
                    s = ((TuringMachine) s.getAutomaton()).getParent();
                }
                this.drawer.addSelected(s);
                activeStates.add(s);
            }
            State st = c.getCurrentState();
            if (st != null) {
                this.drawer.addSelected(st);
                activeStates.add(st);
            }
        }

        for (Transition t : this.lastFiredTransitions) {
            try {
                this.drawer.addSelected(t);
            } catch ( Throwable ignored) {
            }
        }

        Theme.setActiveSimStates(activeStates);
        Theme.setFiredTransitions(this.lastFiredTransitions);
        this.component.repaint();
        notifyStatusChanged();
    }

    private boolean setFocusIfNeeded(Configuration config, boolean alreadyFocused) {
        Configuration parent = config.getParent();
        if (parent == null) {
            return alreadyFocused;
        }
        if (parent.getFocused()) {
            config.setFocused(true);
            if (!alreadyFocused) {
                this.configurations.setFocused(config);
                this.drawer.setAutomaton(config.getCurrentState().getAutomaton());
                this.drawer.invalidate();
            }
            return true;
        }
        return alreadyFocused;
    }

    public void defocus() {
        Configuration[] all = this.configurations.getConfigurations();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getFocused()) {
                this.configurations.defocus(all[i]);
            }
        }
        this.drawer.setAutomaton(this.simulator.getAutomaton());
        this.drawer.invalidate();
        this.component.repaint();
    }

    public void thaw() {
        Configuration[] selected = this.configurations.getSelected();
        if (selected.length == 0) {
            JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR,
                    NO_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < selected.length; i++) {
            this.configurations.setNormal(selected[i]);
        }
        this.configurations.deselectAll();
        this.configurations.repaint();
    }

    public void trace() {
        Configuration[] selected = this.configurations.getSelected();
        if (selected.length == 0) {
            // Convenience UX: if only 1 configuration exists and none is manually toggled, trace it automatically!
            Configuration[] all = this.configurations.getConfigurations();
            if (all.length == 1) {
                selected = all;
            } else {
                JOptionPane.showMessageDialog(this.configurations, NO_CONFIGURATION_ERROR,
                        NO_CONFIGURATION_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        for (int i = 0; i < selected.length; i++) {
            TraceWindow tw = this.configurationToTraceWindow.get(selected[i]);
            if (tw == null) {
                this.configurationToTraceWindow.put(selected[i], new TraceWindow(selected[i]));
            } else {
                tw.setVisible(true);
                tw.toFront();
            }
        }
    }

    public int autoStatus() {
        Configuration[] all = this.configurations.getConfigurations();
        for (int i = 0; i < all.length; i++) {
            if (this.configurations.getState(all[i]) == ConfigurationButton.ACCEPT) {
                return 1;
            }
        }
        if (this.configurations.getValidConfigurations().length == 0) {
            return 2;
        }
        return 0;
    }

    public boolean isTuringMachine() {
        return this.simulator instanceof TMSimulator;
    }

    @Override
    public void configurationSelectionChange(ConfigurationSelectionEvent event) {
    }
}
