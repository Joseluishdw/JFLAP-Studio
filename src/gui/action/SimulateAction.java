package gui.action;

import automata.Automaton;
import automata.AutomatonSimulator;
import automata.Configuration;
import automata.NondeterminismDetector;
import automata.NondeterminismDetectorFactory;
import automata.SimulatorFactory;
import automata.State;
import automata.graph.AutomatonDirectedGraph;
import automata.mealy.MealyMachine;
import automata.turing.TMSimulator;
import automata.turing.TuringMachine;
import grammar.Grammar;
import gui.environment.Environment;
import gui.environment.Universe;
import gui.environment.tag.CriticalTag;
import gui.sim.SimulatorPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import plus.AppIcon;
import plus.JetBrainsUI;
import plus.ModernFileChooser;
import plus.Theme;
import plus.VSIcons;

public class SimulateAction extends AutomatonAction {
    private Grammar gram;
    private Automaton automaton;
    private Environment environment;

    public SimulateAction(Automaton automaton, Environment environment) {
        super("Step...", null);
        if (SimulateNoClosureAction.isApplicable(automaton)) {
            putValue("Name", "Step with Closure...");
        }
        putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_R, MAIN_MENU_MASK));
        this.automaton = automaton;
        this.environment = environment;
    }

    public SimulateAction(Grammar gram, Environment environment) {
        super("Step...", null);
        if (SimulateNoClosureAction.isApplicable(this.automaton)) {
            putValue("Name", "Step with Closure...");
        }
        putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_R, MAIN_MENU_MASK));
        this.gram = gram;
        this.environment = environment;
    }

    protected AutomatonSimulator getSimulator(Automaton automaton) {
        return SimulatorFactory.getSimulator(automaton);
    }

    public void handleInteraction(Automaton automaton, AutomatonSimulator simulator,
                                  Configuration[] configurations, Object initialInput) {
        SimulatorPane simpane = new SimulatorPane(automaton, simulator, configurations, this.environment, false);
        if (initialInput instanceof String[]) {
            initialInput = Arrays.asList((String[]) initialInput);
        }
        this.environment.add(simpane, "Simulate: " + initialInput, new CriticalTag() {});
        this.environment.setActive(simpane);
    }

    protected Object initialInput(Component component, String description) {
        if ("".equals(description)) {
            description = "Input";
        }
        if (getObject() instanceof TuringMachine) {
            TuringMachine machine = (TuringMachine) getObject();
            int tapes = machine.tapes();
            if ("Expected Result? (Accept or Reject)".equals(description)) {
                description = "Result";
                tapes = 1;
            }
            if ("Expected Output?".equals(description)) {
                description = "Output";
            }
            return openInputGUI(component, description, tapes);
        }
        if ("".equals(description)) {
            return openInputGUI(component, "Input?", 0);
        }
        return openInputGUI(component, description, 0);
    }

    private Object openInputGUI(final Component component, String title, final int tapes) {
        Window owner = component != null ? SwingUtilities.getWindowAncestor(component) : null;
        final JDialog dlg = new JDialog(owner, "Iniciar Simulación Paso a Paso", Dialog.ModalityType.APPLICATION_MODAL);
        AppIcon.apply(dlg);

        final int count = Math.max(1, tapes);
        final JetBrainsUI.ModernTextField[] fields = new JetBrainsUI.ModernTextField[count];
        final boolean[] confirmed = new boolean[]{false};

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Top Header
        JPanel header = new JPanel(new BorderLayout(10, 2));
        header.setOpaque(false);
        JLabel hdrTitle = new JLabel("Depuración Paso a Paso  ·  Cadena de Entrada");
        hdrTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        hdrTitle.setForeground(Theme.BLACK);
        JLabel hdrSub = new JLabel(tapes > 0
                ? "Introduce los símbolos iniciales para la cinta de la Máquina de Turing (deja vacío para cinta en blanco □)."
                : "Introduce la cadena de entrada para evaluar en el autómata (deja vacío para λ).");
        hdrSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hdrSub.setForeground(Theme.TEXT_MUTED);
        header.add(hdrTitle, BorderLayout.NORTH);
        header.add(hdrSub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Input Card
        JetBrainsUI.RoundedPanel card = new JetBrainsUI.RoundedPanel(new GridLayout(count, 1, 0, 8), 10);
        card.setCustomColors(Theme.CARD_BG, Theme.CARD_BORDER);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        for (int i = 0; i < count; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            String labelText = (tapes > 1) ? (title + " Cinta " + (i + 1) + ":") : "Entrada:";
            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(Theme.BLACK);
            lbl.setPreferredSize(new Dimension(85, 30));

            final JetBrainsUI.ModernTextField tf = new JetBrainsUI.ModernTextField("Escribe la entrada (ej. aeiou+aeiou= o 1011)...");
            tf.setFont(new Font("Consolas", Font.BOLD, 14));
            tf.setPreferredSize(new Dimension(340, 32));
            tf.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmed[0] = true;
                    dlg.dispose();
                }
            });
            fields[i] = tf;

            row.add(lbl, BorderLayout.WEST);
            row.add(tf, BorderLayout.CENTER);
            card.add(row);
        }
        root.add(card, BorderLayout.CENTER);

        // Bottom Controls
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBtns.setOpaque(false);

        JButton emptyBtn = new JButton("Vacío (λ / □)");
        JetBrainsUI.styleCompactButton(emptyBtn, "Simular con entrada vacía / cinta en blanco");
        emptyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (JetBrainsUI.ModernTextField f : fields) {
                    f.setText("");
                }
                confirmed[0] = true;
                dlg.dispose();
            }
        });

        JButton fileBtn = new JButton("Cargar Archivo...");
        JetBrainsUI.styleCompactButton(fileBtn, "Leer cadena de entrada desde un archivo de texto");
        fileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = ModernFileChooser.showOpenDialog(dlg, null);
                if (f != null && f.exists()) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        try {
                            for (int i = 0; i < count; i++) {
                                String line = br.readLine();
                                fields[i].setText(line != null ? line : "");
                            }
                        } finally {
                            br.close();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
        leftBtns.add(emptyBtn);
        leftBtns.add(fileBtn);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        JButton cancelBtn = new JButton("Cancelar");
        JetBrainsUI.styleCompactButton(cancelBtn, "Cancelar simulación (Esc)");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed[0] = false;
                dlg.dispose();
            }
        });

        JButton startBtn = new JButton("Iniciar Simulación", VSIcons.get(VSIcons.MEDIA_PLAY, 13, Color.WHITE));
        JetBrainsUI.styleMediaPlayButton(startBtn, "Comenzar simulación interactiva (Enter)");
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed[0] = true;
                dlg.dispose();
            }
        });

        rightBtns.add(cancelBtn);
        rightBtns.add(startBtn);

        footer.add(leftBtns, BorderLayout.WEST);
        footer.add(rightBtns, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        KeyAdapter esc = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmed[0] = false;
                    dlg.dispose();
                }
            }
        };
        for (JetBrainsUI.ModernTextField tf : fields) {
            tf.addKeyListener(esc);
        }

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (fields.length > 0) {
                    fields[0].requestFocusInWindow();
                }
            }
        });
        dlg.setVisible(true);

        if (!confirmed[0]) {
            return null;
        }
        if (tapes == 0) {
            return fields[0].getText();
        }
        String[] result = new String[tapes];
        for (int i = 0; i < tapes; i++) {
            result[i] = fields[i].getText();
        }
        return result;
    }

    private void handleInputFile(Object input) {
        Configuration[] configs = null;
        AutomatonSimulator simulator = getSimulator(this.automaton);
        if (input == null) {
            return;
        }
        if (getObject() instanceof TuringMachine) {
            String[] s = (String[]) input;
            configs = ((TMSimulator) simulator).getInitialConfigurations(s);
        } else {
            String s = (String) input;
            configs = simulator.getInitialConfigurations(s);
        }
        handleInteraction(this.automaton, simulator, configs, input);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        boolean turing = false;
        if (getObject() instanceof TuringMachine) {
            turing = true;
        }
        if (!automatonActionPermissible((Component) event.getSource())) {
            return;
        }
        Object input = initialInput((Component) event.getSource(), "");
        if (input instanceof String && ((String) input).equals("Click to Open Input File")) {
            return;
        }
        if (input instanceof String[]) {
            String[] s = (String[]) input;
            for (int k = 0; k < s.length; k++) {
                if (s[k].equals("Click to Open Input File")) {
                    return;
                }
            }
        }
        Configuration[] configs = null;
        AutomatonSimulator simulator = getSimulator(this.automaton);
        if (input == null) {
            return;
        }
        if (getObject() instanceof TuringMachine) {
            String[] s = (String[]) input;
            configs = ((TMSimulator) simulator).getInitialConfigurations(s);
        } else {
            String s = (String) input;
            configs = simulator.getInitialConfigurations(s);
        }
        handleInteraction(this.automaton, simulator, configs, input);
    }

    protected boolean automatonActionPermissible(Component source) {
        if (!(getObject() instanceof Automaton)) {
            return true;
        }
        if (this.automaton.getInitialState() == null) {
            JOptionPane.showMessageDialog(source,
                    "La simulación requiere un autómata con un estado inicial (Initial).",
                    "Sin Estado Inicial", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (this.automaton instanceof MealyMachine) {
            NondeterminismDetector d = NondeterminismDetectorFactory.getDetector(this.automaton);
            State[] nd = d.getNondeterministicStates(this.automaton);
            if (nd.length > 0) {
                JOptionPane.showMessageDialog(source,
                        "Por favor elimina el no determinismo para simular.\nUsa Test : Highlight Nondeterminism para ver los estados no deterministas.",
                        "Estados no deterministas detectados", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (this.automaton instanceof TuringMachine && !Universe.curProfile.transitionsFromTuringFinalStateAllowed()) {
            TuringMachine tm = (TuringMachine) this.automaton;
            State[] finals = tm.getFinalStates();
            AutomatonDirectedGraph g = new AutomatonDirectedGraph(tm);
            for (int i = 0; i < finals.length; i++) {
                if (g.fromDegree(finals[i], false) > 0) {
                    JOptionPane.showMessageDialog(source,
                            "Hay transiciones saliendo de estados finales. Elimínalas o cambia la preferencia\nen el menú Preferencias.",
                            "Transiciones desde Estados Finales", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isApplicable(Object object) {
        return object instanceof Automaton;
    }

    protected Object getObject() {
        if (this.automaton != null) {
            return this.automaton;
        }
        return this.gram;
    }

    protected Environment getEnvironment() {
        return this.environment;
    }

    protected void setEnvironment(Environment newEnv) {
        this.environment = newEnv;
    }
}
