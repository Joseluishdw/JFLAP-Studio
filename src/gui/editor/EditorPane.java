package gui.editor;

import automata.Automaton;
import automata.Note;
import automata.State;
import automata.turing.TuringMachine;
import gui.action.OpenAction;
import gui.environment.Universe;
import gui.viewer.AutomatonDrawer;
import gui.viewer.SelectionDrawer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import plus.JFLAPPlus;
import plus.JetBrainsUI;
import plus.RuleTable;
import plus.Theme;
import plus.VSIcons;

public class EditorPane extends JComponent {
    protected Automaton automaton;
    protected AutomatonDrawer drawer;
    protected EditCanvas pane;
    protected ToolBar toolbar;

    private JPanel workspacePanel;
    private JetBrainsUI.RoundedPanel sidebarIsland;
    private JetBrainsUI.IslandCanvasContainer canvasIsland;
    private JPanel ideHeaderPanel;

    // Modern State Inspector Controls
    private JLabel inspHeaderLabel;
    private JLabel selectedStateLabel;
    private JLabel selectedMetaLabel;
    private JetBrainsUI.ModernSwitch switchInitial;
    private JetBrainsUI.ModernSwitch switchFinal;
    private JLabel nameFieldLabel;
    private JLabel labelFieldLabel;
    private JetBrainsUI.ModernTextField nameField;
    private JetBrainsUI.ModernTextField labelField;
    private boolean syncingInspector = false;
    private State lastInspectedState = null;

    // Project Explorer Controls
    private JLabel expTitleLabel;
    private JPanel explorerListPanel;

    // Bottom Status Bar Controls
    private JPanel bottomBar;
    private JLabel statusLabel;
    private JLabel hintLabel;
    private JLabel zoomTitle;
    private AutomatonSizeSlider slider;
    private Timer statusTimer;

    public EditorPane(Automaton automaton) {
        this(new SelectionDrawer(automaton));
    }

    public EditorPane(Automaton automaton, ToolBox box) {
        this(new SelectionDrawer(automaton), box);
    }

    public EditorPane(AutomatonDrawer drawer) {
        this(drawer, (ToolBox) new DefaultToolBox());
    }

    public EditorPane(AutomatonDrawer drawer, ToolBox box) {
        this(drawer, box, false);
    }

    public EditorPane(AutomatonDrawer drawer, ToolBox box, boolean fit) {
        this.pane = new EditCanvas(drawer, fit);
        this.pane.setCreator(this);
        this.drawer = drawer;
        this.automaton = drawer.getAutomaton();
        setLayout(new BorderLayout());
        setBackground(Theme.ISLAND_BASE_BG);
        setOpaque(true);

        // Floating Island Toolbar (placed INSIDE the Canvas via IslandCanvasContainer)
        this.toolbar = new ToolBar(this.pane, drawer, box);
        this.pane.setToolBar(this.toolbar);

        JScrollPane vertical = new JScrollPane(this.pane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vertical.setBorder(BorderFactory.createEmptyBorder());
        vertical.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        vertical.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        vertical.setBackground(Theme.WHITE);
        vertical.getViewport().setBackground(Theme.WHITE);

        // Right Island: Automaton Canvas + Centered Floating Island Dock
        this.canvasIsland = new JetBrainsUI.IslandCanvasContainer(vertical, this.toolbar);

        // Left Island: JetBrains Island Sidebar (State Inspector + .JFF Project Tree)
        this.sidebarIsland = buildJetBrainsSidebarIsland();

        // Main Workspace with #141517 Deep Island Margin separating Left Sidebar Island & Right Canvas Island
        this.workspacePanel = new JPanel(new BorderLayout(8, 0));
        this.workspacePanel.setBackground(Theme.ISLAND_BASE_BG);
        this.workspacePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.workspacePanel.add(this.sidebarIsland, BorderLayout.WEST);
        this.workspacePanel.add(this.canvasIsland, BorderLayout.CENTER);

        add(this.workspacePanel, BorderLayout.CENTER);

        // Build Top-Right IDE Header Bar (integrated into JMenuBar alongside File/Input/Test/View/Convert/Help)
        this.ideHeaderPanel = buildIdeHeaderControls();

        // Sync inspector immediately when clicking/releasing on canvas
        MouseAdapter syncMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateStatusText();
                    }
                });
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateStatusText();
                    }
                });
            }
        };
        this.pane.addMouseListener(syncMouse);

        // Install IDE Header into JMenuBar and ModernTabbedPaneUI into parent JTabbedPane
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                installIntoWindowAndTabHierarchy();
            }
        });

        // IntelliJ IDEA Integrated Status Bar (#1E1F22, 1px top border #393B40, text #868A91)
        this.bottomBar = new JPanel(new BorderLayout(12, 0));
        this.bottomBar.setBackground(Theme.STATUS_BAR_BG);
        this.bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));

        this.statusLabel = new JLabel();
        this.statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.statusLabel.setForeground(Theme.TEXT_MUTED);

        this.hintLabel = new JLabel("Rueda: Zoom   ·   Clic Central / Alt+Arrastrar: Mover   ·   Doble Clic en Arista: Editar", JLabel.CENTER);
        this.hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.hintLabel.setForeground(Theme.TEXT_MUTED);

        JPanel zoomBox = new JPanel(new BorderLayout(8, 0));
        zoomBox.setOpaque(false);
        this.zoomTitle = new JLabel("Escala");
        this.zoomTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.zoomTitle.setForeground(Theme.TEXT_MUTED);

        this.slider = new AutomatonSizeSlider(this.pane, drawer);
        this.slider.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.slider.setOpaque(false);
        this.slider.setUI(new JetBrainsUI.ModernSliderUI(this.slider));
        this.slider.setPreferredSize(new Dimension(130, 20));
        zoomBox.add(this.zoomTitle, BorderLayout.WEST);
        zoomBox.add(this.slider, BorderLayout.CENTER);

        this.bottomBar.add(this.statusLabel, BorderLayout.WEST);
        this.bottomBar.add(this.hintLabel, BorderLayout.CENTER);
        this.bottomBar.add(zoomBox, BorderLayout.EAST);

        add(this.bottomBar, BorderLayout.SOUTH);

        ArrayList notes = drawer.getAutomaton().getNotes();
        for (int k = 0; k < notes.size(); k++) {
            ((Note) notes.get(k)).initializeForView(this.pane);
        }

        updateStatusText();

        this.statusTimer = new Timer(250, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) {
                    installIntoWindowAndTabHierarchy();
                    updateStatusText();
                }
            }
        });
        this.statusTimer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        installIntoWindowAndTabHierarchy();
    }

    /**
     * Styles the ancestor JTabbedPane ("Editor") with ModernTabbedPaneUI (36px flat tab, 3px #3574F0 underline)
     * and integrates the IDE Run Header Controls into the right side of the window's JMenuBar.
     */
    public void installIntoWindowAndTabHierarchy() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof JTabbedPane) {
                JTabbedPane tp = (JTabbedPane) p;
                if (!(tp.getUI() instanceof JetBrainsUI.ModernTabbedPaneUI)) {
                    tp.setUI(new JetBrainsUI.ModernTabbedPaneUI());
                }
                tp.setBackground(Theme.SURFACE);
                tp.setBorder(BorderFactory.createEmptyBorder());
            }
            p = p.getParent();
        }

        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof JFrame) {
            if (this.workspaceToolsPanel == null) {
                this.workspaceToolsPanel = buildWorkspaceToolsPanel();
            }
            if (win instanceof gui.environment.EnvironmentFrame) {
                ((gui.environment.EnvironmentFrame) win).setWorkspaceTools(this.workspaceToolsPanel);
            }
            JMenuBar mb = ((JFrame) win).getJMenuBar();
            if (mb != null) {
                if (this.ideHeaderPanel != null && this.ideHeaderPanel.getParent() != mb) {
                    for (int i = mb.getComponentCount() - 1; i >= 0; i--) {
                        Component c = mb.getComponent(i);
                        if ("IDE_HEADER_PANEL".equals(c.getName())) {
                            mb.remove(i);
                        }
                    }
                    this.ideHeaderPanel.setName("IDE_HEADER_PANEL");
                    mb.add(this.ideHeaderPanel);
                    JetBrainsUI.styleMenuBar(mb);
                    mb.revalidate();
                    mb.repaint();
                } else if (!(mb.getUI() instanceof JetBrainsUI.ModernMenuBarUI)) {
                    JetBrainsUI.styleMenuBar(mb);
                }
            }
        }
    }

    private JPanel workspaceToolsPanel;

    /**
     * Builds the Top-Right IDE Execution Header integrated in the coupled top window bar:
     * [⏯ Paso a Paso] [▶ Ejecutar (F5) in green #2EA043] | [Tema] [Ayuda]
     * Leaving plenty of center space for the coupled window title and menus (Antigravity IDE style).
     */
    private JPanel buildIdeHeaderControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        bar.setOpaque(false);

        JButton stepBtn = new JButton("Paso a Paso", VSIcons.get(VSIcons.PLAY, 14, null));
        JetBrainsUI.styleGhostButton(stepBtn, "Depurar / Simular autómata paso a paso (Step)");
        stepBtn.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        stepBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFLAPPlus.runStepSimulation(EditorPane.this);
            }
        });
        bar.add(stepBtn);

        JButton runBtn = new JButton("Ejecutar (F5)", VSIcons.get(VSIcons.PLAY, 13, Color.WHITE));
        JetBrainsUI.styleRunButton(runBtn, "Ejecutar pruebas múltiples sobre el autómata (F5)");
        runBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        runBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFLAPPlus.runMultipleSimulation(EditorPane.this);
            }
        });
        bar.add(runBtn);

        bar.add(JetBrainsUI.createToolbarSeparator());

        JButton themeBtn = new JButton("", VSIcons.get(VSIcons.THEME, 14, null));
        JetBrainsUI.styleGhostButton(themeBtn, "Alternar tema JetBrains Island Dark / Light (Ctrl+Shift+D)");
        themeBtn.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        themeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Theme.toggle();
            }
        });
        bar.add(themeBtn);

        JButton helpBtn = new JButton("", VSIcons.get(VSIcons.HELP, 15, null));
        JetBrainsUI.styleGhostButton(helpBtn, "Ver Guía Rápida de Atajos y Expresiones (F1)");
        helpBtn.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        helpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFLAPPlus.showShortcutsHelp(EditorPane.this);
            }
        });
        bar.add(helpBtn);

        return bar;
    }

    /**
     * Builds the Workspace Tools placed on the right side of the PROYECTOS bar:
     * [Explorador] [Cuadrícula] [Imán] | [Tabla TM]
     */
    private JPanel buildWorkspaceToolsPanel() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        bar.setOpaque(false);

        JButton explorerBtn = new JButton("Explorador", VSIcons.get(VSIcons.EXPLORER, 14, null));
        JetBrainsUI.styleGhostButton(explorerBtn, "Mostrar / Ocultar Panel Lateral Izquierdo (Ctrl+B)");
        explorerBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        explorerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSidebar();
            }
        });
        bar.add(explorerBtn);

        final JToggleButton gridBtn = new JToggleButton("Cuadrícula", VSIcons.get(VSIcons.GRID, 14, null), Theme.isShowGrid());
        JetBrainsUI.styleGhostButton(gridBtn, "Mostrar / Ocultar cuadrícula de puntos (Ctrl+G)");
        gridBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        gridBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Theme.setShowGrid(gridBtn.isSelected());
            }
        });
        bar.add(gridBtn);

        final JToggleButton snapBtn = new JToggleButton("Imán", VSIcons.get(VSIcons.SNAP, 14, null), Theme.isSnapToGrid());
        JetBrainsUI.styleGhostButton(snapBtn, "Ajustar movimiento de estados a la cuadrícula (Ctrl+Shift+G)");
        snapBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        snapBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Theme.setSnapToGrid(snapBtn.isSelected());
            }
        });
        bar.add(snapBtn);

        if (this.drawer != null && this.drawer.getAutomaton() instanceof TuringMachine) {
            bar.add(JetBrainsUI.createToolbarSeparator());
            JButton tmTableBtn = new JButton("Tabla TM", VSIcons.get(VSIcons.TABLE, 14, null));
            JetBrainsUI.styleGhostButton(tmTableBtn, "Abrir Editor de Tabla de Transiciones de Turing (Ctrl+Shift+T)");
            tmTableBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            tmTableBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    RuleTable.open(EditorPane.this);
                }
            });
            bar.add(tmTableBtn);
        }

        return bar;
    }

    /**
     * Builds the Left Sidebar as a rounded 12px JetBrains Island (#1E1F22 on #141517 base):
     * - Top: Modern State Inspector with Inicial/Final Switches in 1 row + editable Nombre & Etiqueta JTextFields
     * - Bottom: Borderless .JFF Project Tree
     */
    private JetBrainsUI.RoundedPanel buildJetBrainsSidebarIsland() {
        JetBrainsUI.RoundedPanel island = new JetBrainsUI.RoundedPanel(new BorderLayout(0, 12), 12);
        island.setPreferredSize(new Dimension(248, 460));
        island.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top Section: Modern State Inspector
        JPanel inspectorBox = new JPanel();
        inspectorBox.setLayout(new BoxLayout(inspectorBox, BoxLayout.Y_AXIS));
        inspectorBox.setOpaque(false);

        this.inspHeaderLabel = new JLabel("INSPECTOR DE ESTADO");
        this.inspHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        this.inspHeaderLabel.setForeground(Theme.TEXT_MUTED);
        this.inspHeaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.selectedStateLabel = new JLabel("Estado: q0");
        this.selectedStateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        this.selectedStateLabel.setForeground(Theme.BLACK);
        this.selectedStateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.selectedMetaLabel = new JLabel("Posición: (0, 0)");
        this.selectedMetaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.selectedMetaLabel.setForeground(Theme.TEXT_MUTED);
        this.selectedMetaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1: Modern Switches ("Inicial" and "Final") in a single row
        JPanel switchesRow = new JPanel(new GridLayout(1, 2, 8, 0));
        switchesRow.setOpaque(false);
        switchesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        switchesRow.setMaximumSize(new Dimension(260, 28));

        this.switchInitial = new JetBrainsUI.ModernSwitch("Inicial");
        this.switchInitial.setToolTipText("Activar o desactivar como Estado Inicial");
        this.switchInitial.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (syncingInspector) return;
                State s = getTargetState();
                if (s == null) return;
                JFLAPPlus.saveUndo(automaton);
                if (switchInitial.isSelected()) {
                    automaton.setInitialState(s);
                } else if (automaton.getInitialState() == s) {
                    automaton.setInitialState(null);
                }
                pane.repaint();
                updateStatusText();
            }
        });

        this.switchFinal = new JetBrainsUI.ModernSwitch("Final");
        this.switchFinal.setToolTipText("Activar o desactivar como Estado Final");
        this.switchFinal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (syncingInspector) return;
                State s = getTargetState();
                if (s == null) return;
                JFLAPPlus.saveUndo(automaton);
                if (switchFinal.isSelected()) {
                    if (!automaton.isFinalState(s)) {
                        automaton.addFinalState(s);
                    }
                } else {
                    if (automaton.isFinalState(s)) {
                        automaton.removeFinalState(s);
                    }
                }
                pane.repaint();
                updateStatusText();
            }
        });

        switchesRow.add(this.switchInitial);
        switchesRow.add(this.switchFinal);

        // Row 2: Direct Editable JTextField for "Nombre"
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(260, 30));
        this.nameFieldLabel = new JLabel("Nombre");
        this.nameFieldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.nameFieldLabel.setForeground(Theme.TEXT_MUTED);
        this.nameFieldLabel.setPreferredSize(new Dimension(54, 26));
        this.nameField = new JetBrainsUI.ModernTextField("ej. q0");
        final Runnable commitName = new Runnable() {
            @Override
            public void run() {
                if (syncingInspector) return;
                State s = getTargetState();
                if (s == null) return;
                String val = nameField.getText().trim();
                if (!val.isEmpty() && !val.equals(s.getName())) {
                    JFLAPPlus.saveUndo(automaton);
                    s.setName(val);
                    pane.repaint();
                    updateStatusText();
                }
            }
        };
        this.nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitName.run();
                pane.requestFocusInWindow();
            }
        });
        this.nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitName.run();
            }
        });
        nameRow.add(this.nameFieldLabel, BorderLayout.WEST);
        nameRow.add(this.nameField, BorderLayout.CENTER);

        // Row 3: Direct Editable JTextField for "Etiqueta"
        JPanel labelRow = new JPanel(new BorderLayout(8, 0));
        labelRow.setOpaque(false);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.setMaximumSize(new Dimension(260, 30));
        this.labelFieldLabel = new JLabel("Etiqueta");
        this.labelFieldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.labelFieldLabel.setForeground(Theme.TEXT_MUTED);
        this.labelFieldLabel.setPreferredSize(new Dimension(54, 26));
        this.labelField = new JetBrainsUI.ModernTextField("Sin etiqueta...");
        final Runnable commitLabel = new Runnable() {
            @Override
            public void run() {
                if (syncingInspector) return;
                State s = getTargetState();
                if (s == null) return;
                String val = labelField.getText().trim();
                String newLbl = val.isEmpty() ? null : val;
                String oldLbl = s.getLabel();
                if ((newLbl == null && oldLbl != null) || (newLbl != null && !newLbl.equals(oldLbl))) {
                    JFLAPPlus.saveUndo(automaton);
                    s.setLabel(newLbl);
                    if (newLbl != null && drawer != null) {
                        drawer.shouldDrawStateLabels(true);
                    }
                    pane.repaint();
                    updateStatusText();
                }
            }
        };
        this.labelField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitLabel.run();
                pane.requestFocusInWindow();
            }
        });
        this.labelField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitLabel.run();
            }
        });
        labelRow.add(this.labelFieldLabel, BorderLayout.WEST);
        labelRow.add(this.labelField, BorderLayout.CENTER);

        // Row 4: Compact secondary actions (Color & Eliminar)
        JPanel actionsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsRow.setMaximumSize(new Dimension(260, 28));

        JButton colorBtn = createInspectorButton("Color...", "Elegir color personalizado para el estado", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                State s = getTargetState();
                if (s == null) return;
                Color init = s.getFillColor() != null ? s.getFillColor() : Theme.ACCENT;
                Color picked = JColorChooser.showDialog(EditorPane.this, "Color del Estado", init);
                if (picked != null) {
                    JFLAPPlus.saveUndo(automaton);
                    s.setFillColor(picked);
                    for (State st : automaton.getStates()) {
                        if (st.isSelected()) {
                            st.setFillColor(picked);
                        }
                    }
                    pane.repaint();
                }
            }
        });

        JButton deleteBtn = createInspectorButton("Eliminar", "Eliminar estado o selección (Supr)", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFLAPPlus.deleteSelection(EditorPane.this);
                updateStatusText();
            }
        });

        actionsRow.add(colorBtn);
        actionsRow.add(deleteBtn);

        JPanel quickTransRow = new JPanel(new GridLayout(1, 2, 6, 0));
        quickTransRow.setOpaque(false);
        quickTransRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickTransRow.setMaximumSize(new Dimension(260, 28));

        JButton addLoopBtn = createInspectorButton("+ Lazo",
                "Agregar transiciones de lazo sobre el estado seleccionado sin arrastrar flechas", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                State s = getTargetState();
                if (s == null && automaton != null) {
                    JFLAPPlus.saveUndo(automaton);
                    s = automaton.createState(new java.awt.Point(140, 180));
                    s.setSelect(true);
                    pane.repaint();
                }
                if (s != null) {
                    TransitionCreator tc = TransitionCreator.creatorForAutomaton(automaton, pane);
                    if (tc != null) {
                        tc.createTransition(s, s);
                    }
                }
            }
        });

        final JButton connectToBtn = new JButton("+ Conectar a...");
        JetBrainsUI.styleCompactButton(connectToBtn,
                "Conectar este estado con otro estado existente o con un estado nuevo sin arrastrar la flecha");
        connectToBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showQuickConnectPopup(connectToBtn);
            }
        });

        quickTransRow.add(addLoopBtn);
        quickTransRow.add(connectToBtn);

        // Subtle horizontal divider between Inspector and Project Tree
        JPanel divider = new JPanel();
        divider.setBackground(Theme.CARD_BORDER);
        divider.setPreferredSize(new Dimension(200, 1));
        divider.setMaximumSize(new Dimension(260, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        inspectorBox.add(this.inspHeaderLabel);
        inspectorBox.add(Box.createVerticalStrut(4));
        inspectorBox.add(this.selectedStateLabel);
        inspectorBox.add(Box.createVerticalStrut(2));
        inspectorBox.add(this.selectedMetaLabel);
        inspectorBox.add(Box.createVerticalStrut(10));
        inspectorBox.add(switchesRow);
        inspectorBox.add(Box.createVerticalStrut(8));
        inspectorBox.add(nameRow);
        inspectorBox.add(Box.createVerticalStrut(6));
        inspectorBox.add(labelRow);
        inspectorBox.add(Box.createVerticalStrut(8));
        inspectorBox.add(actionsRow);
        inspectorBox.add(Box.createVerticalStrut(6));
        inspectorBox.add(quickTransRow);
        inspectorBox.add(Box.createVerticalStrut(12));
        inspectorBox.add(divider);

        island.add(inspectorBox, BorderLayout.NORTH);

        // Bottom Section: .jff Project Tree
        JPanel explorerWrap = new JPanel(new BorderLayout(0, 6));
        explorerWrap.setOpaque(false);

        JPanel expHeader = new JPanel(new BorderLayout());
        expHeader.setOpaque(false);
        this.expTitleLabel = new JLabel("PROYECTOS (.JFF)");
        this.expTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        this.expTitleLabel.setForeground(Theme.TEXT_MUTED);

        JButton openOtherBtn = new JButton("Abrir...");
        JetBrainsUI.styleCompactButton(openOtherBtn, "Examinar y abrir otro archivo .jff");
        openOtherBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new OpenAction().actionPerformed(e);
            }
        });
        expHeader.add(this.expTitleLabel, BorderLayout.WEST);
        expHeader.add(openOtherBtn, BorderLayout.EAST);
        explorerWrap.add(expHeader, BorderLayout.NORTH);

        this.explorerListPanel = new JPanel();
        this.explorerListPanel.setLayout(new BoxLayout(this.explorerListPanel, BoxLayout.Y_AXIS));
        this.explorerListPanel.setOpaque(false);
        this.explorerListPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        JScrollPane expScroll = new JScrollPane(this.explorerListPanel);
        expScroll.setBorder(BorderFactory.createEmptyBorder());
        expScroll.setOpaque(false);
        expScroll.getViewport().setOpaque(false);
        expScroll.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        expScroll.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        expScroll.getVerticalScrollBar().setUnitIncrement(16);

        explorerWrap.add(expScroll, BorderLayout.CENTER);

        refreshExplorerFiles();
        island.add(explorerWrap, BorderLayout.CENTER);

        return island;
    }

    private JButton createInspectorButton(String text, String tip, ActionListener al) {
        JButton b = new JButton(text);
        JetBrainsUI.styleCompactButton(b, tip);
        b.addActionListener(al);
        return b;
    }

    private void showQuickConnectPopup(Component invoker) {
        if (this.automaton == null) return;
        State from = getTargetState();
        if (from == null) {
            JFLAPPlus.saveUndo(this.automaton);
            from = this.automaton.createState(new java.awt.Point(140, 180));
            from.setSelect(true);
            this.pane.repaint();
        }
        final State sourceState = from;

        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        popup.setBackground(Theme.CARD_BG);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JLabel header = new JLabel("CONECTAR DESDE " + sourceState.getName().toUpperCase());
        header.setFont(new Font("Segoe UI", Font.BOLD, 10));
        header.setForeground(Theme.TEXT_MUTED);
        header.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        popup.add(header);
        popup.addSeparator();

        javax.swing.JMenuItem newStateItem = new javax.swing.JMenuItem("+ Crear Nuevo Estado y Conectar (" + sourceState.getName() + " \u2192 Nuevo)");
        newStateItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newStateItem.setForeground(Theme.ACCENT);
        newStateItem.setBackground(Theme.CARD_BG);
        newStateItem.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 12));
        newStateItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFLAPPlus.saveUndo(automaton);
                java.awt.Point base = sourceState.getPoint();
                int nx = Theme.isSnapToGrid() ? Theme.snap(base.x + 140) : (base.x + 140);
                int ny = Theme.isSnapToGrid() ? Theme.snap(base.y) : base.y;
                State created = automaton.createState(new java.awt.Point(nx, ny));
                pane.repaint();
                TransitionCreator tc = TransitionCreator.creatorForAutomaton(automaton, pane);
                if (tc != null) {
                    tc.createTransition(sourceState, created);
                }
            }
        });
        popup.add(newStateItem);
        popup.addSeparator();

        for (final State st : this.automaton.getStates()) {
            String desc = (st == sourceState)
                    ? (sourceState.getName() + " \u21BA " + st.getName() + " (Lazo)")
                    : (sourceState.getName() + " \u2192 " + st.getName());
            javax.swing.JMenuItem item = new javax.swing.JMenuItem(desc);
            item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.setForeground(Theme.BLACK);
            item.setBackground(Theme.CARD_BG);
            item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 12));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    TransitionCreator tc = TransitionCreator.creatorForAutomaton(automaton, pane);
                    if (tc != null) {
                        tc.createTransition(sourceState, st);
                    }
                }
            });
            popup.add(item);
        }

        if (invoker != null && invoker.isShowing()) {
            popup.show(invoker, 0, invoker.getHeight() + 2);
        }
    }

    private State getTargetState() {
        if (this.automaton == null) return null;
        for (State s : this.automaton.getStates()) {
            if (s.isSelected()) {
                return s;
            }
        }
        State init = this.automaton.getInitialState();
        if (init != null) {
            return init;
        }
        State[] all = this.automaton.getStates();
        return all != null && all.length > 0 ? all[0] : null;
    }

    public void refreshExplorerFiles() {
        if (this.explorerListPanel == null) return;
        this.explorerListPanel.removeAll();
        File currentFile = null;
        try {
            if (this.automaton != null && this.automaton.getEnvironmentFrame() != null
                    && this.automaton.getEnvironmentFrame().getEnvironment() != null) {
                currentFile = this.automaton.getEnvironmentFrame().getEnvironment().getFile();
            }
        } catch (Throwable ignored) {
        }
        String currentPath = currentFile != null ? currentFile.getAbsolutePath() : null;

        List<File> files = JFLAPPlus.discoverJffFiles();
        for (final File f : files) {
            final boolean isCurrent = (currentPath != null && currentPath.equalsIgnoreCase(f.getAbsolutePath()))
                    || (currentFile != null && currentFile.getName().equalsIgnoreCase(f.getName()));
            final JetBrainsUI.TreeItemPanel row = new JetBrainsUI.TreeItemPanel(new BorderLayout(8, 0));
            row.setActive(isCurrent);
            row.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            row.setPreferredSize(new Dimension(200, 32));
            row.setMaximumSize(new Dimension(280, 32));
            row.setMinimumSize(new Dimension(120, 32));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLbl = new JLabel(VSIcons.get(VSIcons.FILE_JFF, 15, null));
            final JLabel nameLbl = new JLabel(f.getName());
            nameLbl.setFont(new Font("Segoe UI", isCurrent ? Font.BOLD : Font.PLAIN, 12));
            nameLbl.setForeground(isCurrent ? (Theme.isDark() ? Color.WHITE : Theme.ACCENT) : Theme.BLACK);
            row.setToolTipText(f.getAbsolutePath() + " (Clic para abrir en esta ventana)");

            row.add(iconLbl, BorderLayout.WEST);
            row.add(nameLbl, BorderLayout.CENTER);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        if (Universe.CHOOSER != null && f.getParentFile() != null) {
                            Universe.CHOOSER.setCurrentDirectory(f.getParentFile());
                        }
                        OpenAction.openFile(f, OpenAction.makeFilters());
                    } catch (Throwable ex) {
                        JOptionPane.showMessageDialog(EditorPane.this,
                                "Error al abrir " + f.getName() + ":\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    row.setHovered(true);
                    nameLbl.setForeground(Theme.ACCENT);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    row.setHovered(false);
                    nameLbl.setForeground(isCurrent ? (Theme.isDark() ? Color.WHITE : Theme.ACCENT) : Theme.BLACK);
                }
            };
            row.addMouseListener(ma);
            nameLbl.addMouseListener(ma);
            this.explorerListPanel.add(row);
        }
        this.explorerListPanel.revalidate();
        this.explorerListPanel.repaint();
    }

    public void toggleSidebar() {
        if (this.sidebarIsland != null) {
            this.sidebarIsland.setVisible(!this.sidebarIsland.isVisible());
            revalidate();
            repaint();
        }
    }

    public void applyThemeColors() {
        setBackground(Theme.ISLAND_BASE_BG);
        if (this.workspacePanel != null) {
            this.workspacePanel.setBackground(Theme.ISLAND_BASE_BG);
        }
        if (this.toolbar != null) {
            this.toolbar.reapplyGhostStyles();
        }
        if (this.ideHeaderPanel != null) {
            for (Component c : this.ideHeaderPanel.getComponents()) {
                if (c instanceof JButton && ((JButton) c).getText() != null && ((JButton) c).getText().contains("Ejecutar")) {
                    JetBrainsUI.styleRunButton((AbstractButton) c, ((JButton) c).getToolTipText());
                    ((AbstractButton) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                } else if (c instanceof AbstractButton) {
                    JetBrainsUI.styleGhostButton((AbstractButton) c, ((AbstractButton) c).getToolTipText());
                    ((AbstractButton) c).setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
                }
            }
            this.ideHeaderPanel.repaint();
        }
        if (this.workspaceToolsPanel != null) {
            for (Component c : this.workspaceToolsPanel.getComponents()) {
                if (c instanceof AbstractButton) {
                    JetBrainsUI.styleGhostButton((AbstractButton) c, ((AbstractButton) c).getToolTipText());
                    ((AbstractButton) c).setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                }
            }
            this.workspaceToolsPanel.repaint();
        }
        if (this.sidebarIsland != null) {
            restyleButtonsRecursive(this.sidebarIsland);
            this.sidebarIsland.repaint();
        }
        if (this.canvasIsland != null) {
            this.canvasIsland.repaint();
        }
        if (this.inspHeaderLabel != null) this.inspHeaderLabel.setForeground(Theme.TEXT_MUTED);
        if (this.selectedStateLabel != null) this.selectedStateLabel.setForeground(Theme.BLACK);
        if (this.selectedMetaLabel != null) this.selectedMetaLabel.setForeground(Theme.TEXT_MUTED);
        if (this.nameFieldLabel != null) this.nameFieldLabel.setForeground(Theme.TEXT_MUTED);
        if (this.labelFieldLabel != null) this.labelFieldLabel.setForeground(Theme.TEXT_MUTED);
        if (this.expTitleLabel != null) this.expTitleLabel.setForeground(Theme.TEXT_MUTED);
        if (this.nameField != null) this.nameField.updateTheme();
        if (this.labelField != null) this.labelField.updateTheme();
        if (this.switchInitial != null) this.switchInitial.repaint();
        if (this.switchFinal != null) this.switchFinal.repaint();

        if (this.explorerListPanel != null) {
            refreshExplorerFiles();
        }
        if (this.bottomBar != null) {
            this.bottomBar.setBackground(Theme.STATUS_BAR_BG);
            this.bottomBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(4, 14, 4, 14)));
            this.statusLabel.setForeground(Theme.TEXT_MUTED);
            this.hintLabel.setForeground(Theme.TEXT_MUTED);
            this.zoomTitle.setForeground(Theme.TEXT_MUTED);
        }
        if (this.slider != null) {
            this.slider.setUI(new JetBrainsUI.ModernSliderUI(this.slider));
        }
        installIntoWindowAndTabHierarchy();
    }

    private void restyleButtonsRecursive(Component c) {
        if (c instanceof JetBrainsUI.ModernSwitch) {
            c.repaint();
        } else if (c instanceof JButton) {
            JButton b = (JButton) c;
            JetBrainsUI.styleCompactButton(b, b.getToolTipText());
        } else if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents()) {
                restyleButtonsRecursive(ch);
            }
        }
    }

    private void updateStatusText() {
        if (this.automaton == null || this.statusLabel == null) {
            return;
        }
        try {
            State[] states = this.automaton.getStates();
            int numStates = states != null ? states.length : 0;
            int numTrans = this.automaton.getTransitions() != null ? this.automaton.getTransitions().length : 0;
            State init = this.automaton.getInitialState();
            String initName = init != null ? init.getName() : "ninguno";
            State[] finals = this.automaton.getFinalStates();
            int numFinals = finals != null ? finals.length : 0;
            int zoomPct = (int) Math.round(this.pane.getScale() * 100.0);

            String gridInfo = Theme.isSnapToGrid() ? "Imán Activo" : (Theme.isShowGrid() ? "Cuadrícula" : "Lienzo Libre");
            this.statusLabel.setText(String.format(
                    "Estados: %d    |    Transiciones: %d    |    Inicial: %s    |    Finales: %d    |    Zoom: %d%%    |    %s",
                    numStates, numTrans, initName, numFinals, zoomPct, gridInfo));

            State target = getTargetState();
            int selCount = 0;
            if (states != null) {
                for (State s : states) {
                    if (s.isSelected()) {
                        selCount++;
                    }
                }
            }

            this.syncingInspector = true;
            try {
                if (selCount > 1) {
                    this.selectedStateLabel.setText(selCount + " estados seleccionados");
                    this.selectedMetaLabel.setText("Edición en lote disponible");
                } else if (target != null) {
                    this.selectedStateLabel.setText("Estado: " + target.getName());
                    this.selectedMetaLabel.setText("Posición: (" + target.getPoint().x + ", " + target.getPoint().y + ")");
                    boolean isInit = (init == target);
                    boolean isFin = this.automaton.isFinalState(target);
                    if (this.switchInitial.isSelected() != isInit) {
                        this.switchInitial.setSelected(isInit);
                    }
                    if (this.switchFinal.isSelected() != isFin) {
                        this.switchFinal.setSelected(isFin);
                    }
                    if (target != this.lastInspectedState || !this.nameField.isFocusOwner()) {
                        String curName = target.getName() != null ? target.getName() : "";
                        if (!curName.equals(this.nameField.getText())) {
                            this.nameField.setText(curName);
                        }
                    }
                    if (target != this.lastInspectedState || !this.labelField.isFocusOwner()) {
                        String curLbl = target.getLabel() != null ? target.getLabel() : "";
                        if (!curLbl.equals(this.labelField.getText())) {
                            this.labelField.setText(curLbl);
                        }
                    }
                    this.lastInspectedState = target;
                } else {
                    this.selectedStateLabel.setText("Sin estados");
                    this.selectedMetaLabel.setText("Crea un estado en el lienzo");
                    this.switchInitial.setSelected(false);
                    this.switchFinal.setSelected(false);
                    if (!this.nameField.isFocusOwner()) this.nameField.setText("");
                    if (!this.labelField.isFocusOwner()) this.labelField.setText("");
                    this.lastInspectedState = null;
                }
            } finally {
                this.syncingInspector = false;
            }
        } catch (Throwable ignored) {
        }
    }

    public ToolBar getToolBar() {
        return this.toolbar;
    }

    public AutomatonDrawer getDrawer() {
        return this.drawer;
    }

    public EditCanvas getAutomatonPane() {
        return this.pane;
    }

    @Override
    protected void printComponent(Graphics g) {
        this.pane.print(g);
    }

    @Override
    public void printChildren(Graphics g) {
    }

    public Automaton getAutomaton() {
        return this.automaton;
    }
}
