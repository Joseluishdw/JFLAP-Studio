package gui.sim;

import automata.Automaton;
import automata.AutomatonSimulator;
import automata.Configuration;
import automata.turing.TuringMachine;
import gui.SplitPaneFactory;
import gui.editor.ArrowDisplayOnlyTool;
import gui.environment.Environment;
import gui.viewer.AutomatonPane;
import gui.viewer.SelectionDrawer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plus.JetBrainsUI;
import plus.Theme;

public class SimulatorPane extends JPanel {
    private Automaton automaton;
    private AutomatonSimulator simulator;

    public SimulatorPane(Automaton automaton, AutomatonSimulator simulator,
                         Configuration[] configurations, Environment environment, boolean blockStep) {
        this.automaton = automaton;
        this.simulator = simulator;
        initView(configurations, environment, blockStep);
    }

    private void initView(Configuration[] configs, final Environment environment, boolean blockStep) {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);

        SelectionDrawer drawer = new SelectionDrawer(this.automaton);
        AutomatonPane ap = new AutomatonPane(drawer, true);
        ArrowDisplayOnlyTool arrow = new ArrowDisplayOnlyTool(ap, drawer);
        ap.addMouseListener(arrow);

        JPanel lower = new JPanel(new BorderLayout());
        lower.setBackground(Theme.SURFACE);
        lower.setOpaque(true);

        JScrollPane scroller = new JScrollPane(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setBackground(Theme.SURFACE);
        scroller.getViewport().setBackground(Theme.SURFACE);
        if (scroller.getVerticalScrollBar() != null) {
            scroller.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            scroller.getVerticalScrollBar().setUnitIncrement(18);
        }

        ConfigurationPane simPane = new ConfigurationPane(this.automaton);
        // Turing Machine tapes use full-width horizontal card carousels (1 column);
        // other automata use 1 column for single path or 2 wide columns for non-deterministic branching.
        int cols = (this.automaton instanceof TuringMachine) ? 1 : 2;
        simPane.setLayout(new GridLayout(0, cols, 10, 8));

        for (int i = 0; i < configs.length; i++) {
            simPane.add(configs[i]);
        }

        final ConfigurationController controller = new ConfigurationController(
                simPane, this.simulator, drawer, ap);

        environment.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (environment.contains(SimulatorPane.this)) {
                    return;
                }
                environment.removeChangeListener(this);
                Theme.clearActiveSimStates();
                Theme.clearFiredTransitions();
                controller.cleanup();
            }
        });

        ControlPanel controlPanel = new ControlPanel(controller);
        controlPanel.setBlock(blockStep);

        scroller.getViewport().setView(simPane);
        lower.add(scroller, BorderLayout.CENTER);
        lower.add(controlPanel, BorderLayout.SOUTH);

        Component mainDisplay = SplitPaneFactory.createSplit(environment, false, 0.58, ap, lower);
        if (mainDisplay instanceof JSplitPane) {
            JetBrainsUI.styleSplitPane((JSplitPane) mainDisplay);
        }
        add(mainDisplay, BorderLayout.CENTER);
    }

    public void setAutomaton(Automaton newAutomaton) {
        this.automaton = newAutomaton;
    }
}
