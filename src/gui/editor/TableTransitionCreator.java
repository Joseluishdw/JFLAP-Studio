package gui.editor;

import automata.Automaton;
import automata.State;
import automata.Transition;
import automata.turing.TMTransition;
import gui.LambdaCellRenderer;
import gui.viewer.AutomatonPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import plus.JFLAPPlus;
import plus.JetBrainsUI;
import plus.Theme;

/**
 * Modernized Inline Transition Creator & Editor for JFLAP Studio.
 * Provides a generously sized inline table plus an attached floating quick-action bar
 * that allows chaining multiple rules on the same loop/arrow, reversing direction,
 * or spawning a new connected state without ever re-dragging arrows.
 */
public abstract class TableTransitionCreator extends TransitionCreator {
    protected JTable editingTable = null;
    private JPanel helperBar = null;
    private Dimension tableDimensions;
    private Point tablePoint = new Point();
    private MouseListener viewListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (helperBar != null && helperBar.getBounds().contains(e.getPoint())) {
                return;
            }
            stopEditing(false);
        }
    };
    private boolean isNew;
    private Transition transition;

    public TableTransitionCreator(AutomatonPane parent) {
        super(parent);
        parent.addMouseListener(this.viewListener);
    }

    protected abstract Transition initTransition(State from, State to);

    protected abstract TableModel createModel(Transition transition);

    protected JTable createTable(final Transition transition) {
        TableModel model = createModel(transition);
        final TipLambdaCellRenderer[] renders = new TipLambdaCellRenderer[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            renders[i] = (transition instanceof TMTransition)
                    ? new TipLambdaCellRenderer("\u25A1", model.getColumnName(i))
                    : new TipLambdaCellRenderer(model.getColumnName(i));
        }

        JTable table = new JTable(createModel(transition)) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return renders[column];
            }

            @Override
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                if (ks.getKeyCode() == KeyEvent.VK_ENTER && !ks.isOnKeyRelease()) {
                    State from = transition.getFromState();
                    State to = transition.getToState();
                    boolean shift = e != null && e.isShiftDown();
                    boolean wasNew = isNew;
                    boolean modified = isRowModifiedFromDefault(this.getModel(), transition);
                    boolean arrowAlreadyHasTransitions = hasExistingTransitionsBetween(from, to);

                    if (wasNew && !shift && !modified && arrowAlreadyHasTransitions) {
                        // Pressing Enter on an untouched row when the arrow already has rules finishes editing cleanly
                        stopEditing(true);
                        return true;
                    }

                    stopEditing(false);
                    if (shift || (wasNew && modified)) {
                        // Automatically open a new input row on the same arrow/loop without dragging!
                        createTransition(from, to);
                    }
                    return true;
                }
                if (ks.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    stopEditing(true);
                    return true;
                }
                return super.processKeyBinding(ks, e, condition, pressed);
            }
        };

        table.setRowHeight(28);
        table.setFont(new Font("Consolas", Font.BOLD, 14));
        table.setBackground(Theme.CARD_BG);
        table.setForeground(Theme.BLACK);
        table.setGridColor(Theme.CARD_BORDER);
        table.setBorder(BorderFactory.createLineBorder(Theme.ACCENT, 2));

        int cols = Math.max(1, model.getColumnCount());
        int rows = Math.max(1, model.getRowCount());
        int colW = (transition instanceof TMTransition) ? 64 : 84;
        for (int c = 0; c < cols; c++) {
            try {
                TableColumn tc = table.getColumnModel().getColumn(c);
                tc.setPreferredWidth(colW);
                tc.setWidth(colW);
            } catch (Throwable ignored) {
            }
        }
        Dimension pref = new Dimension(cols * colW + 4, rows * 28 + 4);
        table.setPreferredSize(pref);
        table.setSize(pref);
        return table;
    }

    private boolean isRowModifiedFromDefault(TableModel currentModel, Transition baseTransition) {
        try {
            if (editingTable != null && editingTable.getCellEditor() != null) {
                try {
                    editingTable.getCellEditor().stopCellEditing();
                } catch (Throwable ignored) {
                }
            }
            TableModel defaultModel = createModel(initTransition(baseTransition.getFromState(), baseTransition.getToState()));
            int rows = Math.min(currentModel.getRowCount(), defaultModel.getRowCount());
            int cols = Math.min(currentModel.getColumnCount(), defaultModel.getColumnCount());
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Object v1 = currentModel.getValueAt(r, c);
                    Object v2 = defaultModel.getValueAt(r, c);
                    String s1 = v1 == null ? "" : v1.toString();
                    String s2 = v2 == null ? "" : v2.toString();
                    if (!s1.equals(s2)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean hasExistingTransitionsBetween(State from, State to) {
        try {
            Automaton aut = getParent().getDrawer().getAutomaton();
            Transition[] existing = aut.getTransitionsFromStateToState(from, to);
            return existing != null && existing.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected abstract Transition modifyTransition(Transition transition, TableModel model);

    private void stopEditing(boolean cancel) {
        if (this.editingTable == null) {
            return;
        }
        try {
            if (this.editingTable.getCellEditor() != null) {
                this.editingTable.getCellEditor().stopCellEditing();
            }
        } catch (NullPointerException ignored) {
        } catch (IllegalArgumentException e) {
            System.err.println("Odd 'focusCycleRoot' exception thrown from the depths of Java again.");
        }

        if (!cancel) {
            Transition newTransition = modifyTransition(this.transition, this.editingTable.getModel());
            if (newTransition != null) {
                Automaton aut = getParent().getDrawer().getAutomaton();
                JFLAPPlus.saveUndo(aut);
                if (this.isNew) {
                    aut.addTransition(newTransition);
                } else {
                    aut.replaceTransition(this.transition, newTransition);
                }
            }
        }

        if (this instanceof TMTransitionCreator) {
            ((TMTransitionCreator) this).setBlockTransition(false);
        }

        getParent().remove(this.editingTable);
        if (this.helperBar != null) {
            getParent().remove(this.helperBar);
            this.helperBar = null;
        }
        getParent().validate();
        getParent().repaint();
        this.editingTable = null;
        getParent().requestFocus();
    }

    @Override
    public boolean editTransition(Transition transition) {
        return false;
    }

    @Override
    public Transition createTransition(State from, State to) {
        Transition t = initTransition(from, to);
        editTransition(t, null);
        return null;
    }

    @Override
    public void editTransition(final Transition transition, Point point) {
        stopEditing(false);
        this.transition = transition;
        this.isNew = (point == null);

        final State from = transition.getFromState();
        final State to = transition.getToState();

        if (this.isNew) {
            if (from == to) {
                point = new Point(from.getPoint().x, from.getPoint().y - 52);
            } else {
                point = new Point((from.getPoint().x + to.getPoint().x) / 2,
                        (from.getPoint().y + to.getPoint().y) / 2);
            }
        }

        getParent().setTablePoint(this.tablePoint);
        this.editingTable = createTable(transition);
        getParent().add(this.editingTable);
        getParent().validate();

        Dimension pref = this.editingTable.getPreferredSize();
        this.tableDimensions = (pref != null && pref.width > 20 && pref.height > 16)
                ? pref
                : new Dimension(196, 32);
        this.editingTable.setSize(this.tableDimensions);

        this.tablePoint = getParent().transformFromAutomatonToView(point);
        this.tablePoint.translate(-this.tableDimensions.width / 2, -this.tableDimensions.height / 2);

        // Keep table inside visible canvas bounds
        int pw = getParent().getWidth();
        int ph = getParent().getHeight();
        if (pw > 260 && ph > 140) {
            this.tablePoint.x = Math.max(16, Math.min(pw - this.tableDimensions.width - 16, this.tablePoint.x));
            this.tablePoint.y = Math.max(48, Math.min(ph - this.tableDimensions.height - 68, this.tablePoint.y));
        }

        getParent().setTablePoint(this.tablePoint);

        this.editingTable.addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                e.getComponent().setLocation(tablePoint);
                positionHelperBar();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                e.getComponent().setSize(tableDimensions);
                positionHelperBar();
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }
        });

        this.editingTable.setLocation(this.tablePoint);
        this.editingTable.setSize(this.tableDimensions);
        this.editingTable.setCellSelectionEnabled(true);
        this.editingTable.changeSelection(0, 0, false, false);

        // Create and attach the floating helper bar right below the editingTable
        this.helperBar = buildHelperBar(transition, from, to);
        getParent().add(this.helperBar);
        positionHelperBar();
        this.helperBar.addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                Rectangle r = computeHelperBounds();
                if (!e.getComponent().getLocation().equals(r.getLocation())) {
                    e.getComponent().setLocation(r.getLocation());
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                Rectangle r = computeHelperBounds();
                if (!e.getComponent().getSize().equals(r.getSize())) {
                    e.getComponent().setSize(r.getSize());
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }
        });

        this.editingTable.requestFocus();
        // Start editing cell (0,0) immediately so the user can type right away
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (editingTable != null) {
                    editingTable.requestFocusInWindow();
                }
            }
        });
        getParent().repaint();
    }

    private JPanel buildHelperBar(final Transition t, final State from, final State to) {
        JetBrainsUI.RoundedPanel bar = new JetBrainsUI.RoundedPanel(new BorderLayout(0, 4), 10);
        bar.setCustomColors(Theme.CARD_BG, Theme.CARD_BORDER);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        String routeText = (from == to)
                ? ("Lazo en " + from.getName() + " (" + from.getName() + " \u2192 " + from.getName() + ")")
                : ("Arista: " + from.getName() + " \u2192 " + to.getName());
        String colsHint = (t instanceof TMTransition)
                ? "Leer  |  Escribir  |  Mov (R/L/S)"
                : "Enter: Guardar y agregar otra";

        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);

        JLabel routeLbl = new JLabel(routeText);
        routeLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        routeLbl.setForeground(Theme.ACCENT);

        JLabel hintLbl = new JLabel(colsHint);
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hintLbl.setForeground(Theme.TEXT_MUTED);

        topRow.add(routeLbl, BorderLayout.WEST);
        topRow.add(hintLbl, BorderLayout.EAST);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actionsRow.setOpaque(false);

        JButton addAnotherBtn = new JButton("+ Otra regla (Enter)");
        JetBrainsUI.styleSecondaryButton(addAnotherBtn, true,
                "Guardar esta transición y agregar otra en la misma arista/lazo sin arrastrar la flecha");
        addAnotherBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        addAnotherBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        addAnotherBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEditing(false);
                createTransition(from, to);
            }
        });
        actionsRow.add(addAnotherBtn);

        if (from != to) {
            JButton reverseBtn = new JButton("Invertir (" + to.getName() + "\u2192" + from.getName() + ")");
            JetBrainsUI.styleCompactButton(reverseBtn,
                    "Guardar y agregar transición en sentido inverso (" + to.getName() + " \u2192 " + from.getName() + ")");
            reverseBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditing(false);
                    createTransition(to, from);
                }
            });
            actionsRow.add(reverseBtn);
        } else {
            JButton loopNextBtn = new JButton("+ \u2192 Nuevo Estado");
            JetBrainsUI.styleCompactButton(loopNextBtn,
                    "Guardar este lazo, crear un nuevo estado al lado y conectarlo automáticamente");
            loopNextBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditing(false);
                    Automaton aut = getParent().getDrawer().getAutomaton();
                    JFLAPPlus.saveUndo(aut);
                    Point base = from.getPoint();
                    int nx = Theme.isSnapToGrid() ? Theme.snap(base.x + 140) : (base.x + 140);
                    int ny = Theme.isSnapToGrid() ? Theme.snap(base.y) : base.y;
                    State nextState = aut.createState(new Point(nx, ny));
                    getParent().repaint();
                    createTransition(from, nextState);
                }
            });
            actionsRow.add(loopNextBtn);
        }

        if (from != to) {
            JButton chainStateBtn = new JButton("+ \u2192 Estado");
            JetBrainsUI.styleCompactButton(chainStateBtn,
                    "Guardar, crear un nuevo estado a partir de " + to.getName() + " y conectarlo sin arrastrar");
            chainStateBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditing(false);
                    Automaton aut = getParent().getDrawer().getAutomaton();
                    JFLAPPlus.saveUndo(aut);
                    Point base = to.getPoint();
                    int nx = Theme.isSnapToGrid() ? Theme.snap(base.x + 140) : (base.x + 140);
                    int ny = Theme.isSnapToGrid() ? Theme.snap(base.y) : base.y;
                    State nextState = aut.createState(new Point(nx, ny));
                    getParent().repaint();
                    createTransition(to, nextState);
                }
            });
            actionsRow.add(chainStateBtn);
        }

        JButton doneBtn = new JButton("Listo (Esc)");
        JetBrainsUI.styleCompactButton(doneBtn, "Guardar transición y cerrar (Esc)");
        doneBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEditing(false);
            }
        });
        actionsRow.add(doneBtn);

        bar.add(topRow, BorderLayout.NORTH);
        bar.add(actionsRow, BorderLayout.SOUTH);

        Dimension d = bar.getPreferredSize();
        bar.setSize(new Dimension(Math.max(d.width, 310), Math.max(d.height, 52)));
        return bar;
    }

    private Rectangle computeHelperBounds() {
        if (this.helperBar == null || this.tableDimensions == null) {
            return new Rectangle(0, 0, 310, 52);
        }
        Dimension hd = this.helperBar.getPreferredSize();
        int hw = Math.max(hd != null ? hd.width : 310, Math.max(310, this.tableDimensions.width));
        int hh = Math.max(hd != null ? hd.height : 52, 52);
        int hx = this.tablePoint.x + (this.tableDimensions.width - hw) / 2;
        int hy = this.tablePoint.y + this.tableDimensions.height + 5;
        if (getParent() != null && getParent().getWidth() > hw + 16) {
            hx = Math.max(8, Math.min(getParent().getWidth() - hw - 8, hx));
        }
        return new Rectangle(hx, hy, hw, hh);
    }

    private void positionHelperBar() {
        if (this.helperBar == null || this.editingTable == null) return;
        Rectangle r = computeHelperBounds();
        this.helperBar.setBounds(r);
    }

    protected void editTransition(Transition transition, Point point, int table) {
    }

    public static class TipLambdaCellRenderer extends LambdaCellRenderer {
        public TipLambdaCellRenderer(String toReplace, String tip) {
            super(toReplace);
            setToolTipText(tip);
        }

        public TipLambdaCellRenderer(String tip) {
            super();
            setToolTipText(tip);
        }
    }
}
