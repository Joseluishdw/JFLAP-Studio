package gui.editor;

import automata.Automaton;
import automata.Note;
import automata.State;
import automata.StateRenamer;
import automata.Transition;
import automata.graph.AutomatonGraph;
import automata.graph.LayoutAlgorithm;
import automata.graph.layout.GEMLayoutAlgorithm;
import automata.turing.TMState;
import automata.turing.TMTransition;
import automata.turing.TuringMachine;
import debug.EDebug;
import gui.environment.AutomatonEnvironment;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.environment.tag.CriticalTag;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import gui.viewer.CurvedArrow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import plus.RuleTable;
import plus.Theme;

public class ArrowTool extends Tool {
    private boolean transitionInFlux = false;
    private TransitionCreator creator;
    private State lastClickedState = null;
    private Transition lastClickedTransition = null;
    private Note lastClickedNote = null;
    private Point initialPointState = new Point();
    private Point initialPointClick = new Point();
    protected StateMenu stateMenu = new StateMenu();
    private TransitionMenu transitionMenu = new TransitionMenu();
    private EmptyMenu emptyMenu = new EmptyMenu();
    private Transition selectedTransition = null;

    public ArrowTool(AutomatonPane view, AutomatonDrawer drawer, TransitionCreator creator) {
        super(view, drawer);
        this.creator = creator;
    }

    public ArrowTool(AutomatonPane view, AutomatonDrawer drawer) {
        super(view, drawer);
        this.creator = TransitionCreator.creatorForAutomaton(getAutomaton(), getView());
    }

    @Override
    public String getToolTip() {
        return "Cursor / Editor de Atributos";
    }

    @Override
    protected Icon getIcon() {
        URL url = getClass().getResource("/ICON/arrow.gif");
        return new ImageIcon(url);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if ( javax.swing.SwingUtilities.isRightMouseButton(event) ) {
            return;
        }
        if (event.getClickCount() == 1) {
            Transition trans = getDrawer().transitionAtPoint(event.getPoint());
            if (trans != null) {
                if (trans.isSelected) {
                    trans.isSelected = false;
                    this.selectedTransition = null;
                } else {
                    if (this.selectedTransition != null) {
                        this.selectedTransition.isSelected = false;
                    }
                    trans.isSelected = true;
                    this.selectedTransition = trans;
                }
                getView().repaint();
                return;
            }
        }
        Transition trans = getDrawer().transitionAtPoint(event.getPoint());
        if (trans == null) {
            State clickedState = getDrawer().stateAtPoint(event.getPoint());
            if (clickedState == null) {
                Rectangle bounds = new Rectangle(0, 0, -1, -1);
                getView().getDrawer().getAutomaton().selectStatesWithinBounds(bounds);
                getView().repaint();
            }
            return;
        }
        EDebug.print("Beginning to edit with creator " + this.creator.getClass());
        this.creator.editTransition(trans, event.getPoint());
    }

    protected void showPopup(MouseEvent event) {
        if (event.isPopupTrigger()) {
            Point p = getView().transformFromAutomatonToView(event.getPoint());
            if (this.lastClickedState != null && shouldShowStatePopup()) {
                this.stateMenu.show(this.lastClickedState, getView(), p);
            } else {
                this.emptyMenu.show(getView(), p);
            }
        }
        this.lastClickedState = null;
        this.lastClickedTransition = null;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (getDrawer().getAutomaton().getEnvironmentFrame() != null) {
            ((AutomatonEnvironment) getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
        } else {
            EDebug.print("I cannot preserve what you ask");
        }
        this.initialPointClick.setLocation(event.getPoint());
        this.lastClickedState = getDrawer().stateAtPoint(event.getPoint());
        if (this.lastClickedState == null) {
            this.lastClickedTransition = getDrawer().transitionAtPoint(event.getPoint());
        }
        if (event.isPopupTrigger()) {
            showPopup(event);
            return;
        }
        if (this.lastClickedState != null) {
            this.initialPointState.setLocation(this.lastClickedState.getPoint());
            if (!this.lastClickedState.isSelected()) {
                Rectangle bounds = new Rectangle(0, 0, -1, -1);
                getView().getDrawer().getAutomaton().selectStatesWithinBounds(bounds);
                getView().getDrawer().setSelectionBounds(bounds);
                this.lastClickedState.setSelect(true);
            }
            getView().repaint();
        } else if (this.lastClickedTransition != null) {
            this.initialPointClick.setLocation(event.getPoint());
        } else {
            ArrayList notes = getDrawer().getAutomaton().getNotes();
            for (int k = 0; k < notes.size(); k++) {
                ((Note) notes.get(k)).setEditable(false);
                ((Note) notes.get(k)).setEnabled(false);
                ((Note) notes.get(k)).setCaretColor(new Color(255, 255, 150));
            }
            Rectangle bounds = new Rectangle(0, 0, -1, -1);
            getView().getDrawer().getAutomaton().selectStatesWithinBounds(bounds);
            getView().getDrawer().setSelectionBounds(bounds);
        }

        Transition[] trans = getAutomaton().getTransitions();
        for (int i = 0; i < trans.length; i++) {
            if (trans[i].isSelected) {
                this.selectedTransition = trans[i];
                return;
            }
        }
        this.selectedTransition = null;
    }

    protected boolean shouldShowStatePopup() {
        return true;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (javax.swing.SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
            return;
        }
        if (this.lastClickedState != null) {
            Point p = event.getPoint();
            State[] states = getView().getDrawer().getAutomaton().getStates();
            for (int k = 0; k < states.length; k++) {
                State curState = states[k];
                if (curState.isSelected()) {
                    int newX = curState.getPoint().x + p.x - this.initialPointClick.x;
                    int newY = curState.getPoint().y + p.y - this.initialPointClick.y;
                    curState.getPoint().setLocation(newX, newY);
                    curState.setPoint(curState.getPoint());
                }
            }
            this.initialPointClick = p;
            getView().repaint();
        } else if (this.lastClickedTransition != null) {
            Point p = event.getPoint();
            State f = this.lastClickedTransition.getFromState();
            State t = this.lastClickedTransition.getToState();
            if (f != t) {
                Transition[] trans = getAutomaton().getTransitionsFromStateToState(f, t);
                for (int n = 0; n < trans.length; n++) {
                    CurvedArrow arrow = (CurvedArrow) getView().getDrawer().transitionToArrowMap.get(trans[n]);
                    getView().getDrawer().arrowToTransitionMap.put(arrow, trans[n]);
                    getView().getDrawer().transitionToArrowMap.put(trans[n], arrow);
                }
            }
            this.initialPointClick.setLocation(p);
            getView().repaint();
        } else {
            int nowX = event.getPoint().x;
            int nowY = event.getPoint().y;
            int leftX = this.initialPointClick.x;
            int topY = this.initialPointClick.y;
            if (nowX < this.initialPointClick.x) {
                leftX = nowX;
            }
            if (nowY < this.initialPointClick.y) {
                topY = nowY;
            }
            Rectangle bounds = new Rectangle(leftX, topY, Math.abs(nowX - this.initialPointClick.x), Math.abs(nowY - this.initialPointClick.y));
            if (!this.transitionInFlux) {
                getView().getDrawer().getAutomaton().selectStatesWithinBounds(bounds);
                getView().getDrawer().setSelectionBounds(bounds);
            }
            getView().repaint();
        }

        if (this.selectedTransition != null) {
            CurvedArrow ca = (CurvedArrow) getView().getDrawer().transitionToArrowMap.get(this.selectedTransition);
            if (ca != null) {
                Point myClick = event.getPoint();
                Point2D control = ca.getCurve().getCtrlPt();
                if (this.transitionInFlux || Math.sqrt((control.getX() - myClick.x) * (control.getX() - myClick.x)
                        + (control.getY() - myClick.y) * (control.getY() - myClick.y)) < 15.0) {
                    this.selectedTransition.setControl(myClick);
                    ca.refreshCurve();
                    this.transitionInFlux = true;
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        this.transitionInFlux = false;
        if (event.isPopupTrigger()) {
            showPopup(event);
            return;
        }
        State[] states = getView().getDrawer().getAutomaton().getStates();
        if (Theme.isSnapToGrid() && this.lastClickedState != null
                && !this.initialPointState.equals(this.lastClickedState.getPoint())) {
            for (int k = 0; k < states.length; k++) {
                if (states[k].isSelected()) {
                    Point pt = states[k].getPoint();
                    pt.setLocation(Theme.snap(pt.x), Theme.snap(pt.y));
                    states[k].setPoint(pt);
                }
            }
        }
        Rectangle bounds = new Rectangle(0, 0, -1, -1);
        getView().getDrawer().setSelectionBounds(bounds);
        this.lastClickedState = null;
        this.lastClickedTransition = null;
        getView().repaint();
    }

    @Override
    public KeyStroke getKey() {
        return KeyStroke.getKeyStroke('a');
    }

    public boolean shouldAllowOnlyFinalStateChange() {
        return false;
    }

    private class TransitionMenu extends JPopupMenu {
    }

    private class EmptyMenu extends JPopupMenu implements ActionListener {
        private Point myPoint;
        private JCheckBoxMenuItem stateLabels;
        private Note curNote;
        private JMenuItem layoutGraph;
        private JMenuItem addNote;
        private JMenuItem renameStates;
        private JMenuItem adaptView;
        private JMenuItem ruleTable;
        private JCheckBoxMenuItem showGridItem;
        private JCheckBoxMenuItem snapGridItem;

        public EmptyMenu() {
            this.stateLabels = new JCheckBoxMenuItem("Mostrar Etiquetas de Estados");
            this.stateLabels.addActionListener(this);
            add(this.stateLabels);

            this.layoutGraph = new JMenuItem("Auto-Ordenar Grafo (Layout Graph)");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.layoutGraph.addActionListener(this);
                add(this.layoutGraph);
            }

            this.renameStates = new JMenuItem("Renombrar Estados (q0, q1, q2...)");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.renameStates.addActionListener(this);
                add(this.renameStates);
            }

            this.addNote = new JMenuItem("+ Añadir Nota de Texto");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.addNote.addActionListener(this);
                add(this.addNote);
            }

            this.ruleTable = new JMenuItem("Tabla de Transiciones (Turing)...  [Ctrl+Shift+T]");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool) && ArrowTool.this.getAutomaton() instanceof TuringMachine) {
                this.ruleTable.addActionListener(this);
                add(this.ruleTable);
            }

            addSeparator();

            this.showGridItem = new JCheckBoxMenuItem("# Mostrar Cuadrícula  [Ctrl+G]", Theme.isShowGrid());
            this.showGridItem.addActionListener(this);
            add(this.showGridItem);

            this.snapGridItem = new JCheckBoxMenuItem("+ Ajustar a Cuadrícula  [Ctrl+Shift+G]", Theme.isSnapToGrid());
            this.snapGridItem.addActionListener(this);
            add(this.snapGridItem);

            this.adaptView = new JCheckBoxMenuItem("Auto-Zoom (Ajustar a Ventana)");
            if (!(ArrowTool.this instanceof ArrowDisplayOnlyTool)) {
                this.adaptView.addActionListener(this);
                add(this.adaptView);
            }
        }

        public void show(Component comp, Point pt) {
            this.stateLabels.setSelected(ArrowTool.this.getDrawer().doesDrawStateLabels());
            this.adaptView.setSelected(ArrowTool.this.getView().getAdapt());
            this.showGridItem.setSelected(Theme.isShowGrid());
            this.snapGridItem.setSelected(Theme.isSnapToGrid());
            this.myPoint = pt;
            show(comp, pt.x, pt.y);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            if (item == this.stateLabels) {
                ArrowTool.this.getView().getDrawer().shouldDrawStateLabels(item.isSelected());
            } else if (item == this.layoutGraph) {
                AutomatonGraph g = new AutomatonGraph(ArrowTool.this.getAutomaton());
                LayoutAlgorithm alg = new GEMLayoutAlgorithm();
                alg.layout(g, null);
                g.moveAutomatonStates();
                ArrowTool.this.getView().fitToBounds(30);
            } else if (item == this.renameStates) {
                ((AutomatonEnvironment) ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                StateRenamer.rename(ArrowTool.this.getAutomaton());
            } else if (item == this.adaptView) {
                ArrowTool.this.getView().setAdapt(item.isSelected());
            } else if (item == this.showGridItem) {
                Theme.setShowGrid(this.showGridItem.isSelected());
            } else if (item == this.snapGridItem) {
                Theme.setSnapToGrid(this.snapGridItem.isSelected());
            } else if (item == this.ruleTable) {
                RuleTable.open(ArrowTool.this.getView().getCreator());
            } else if (item == this.addNote) {
                ((AutomatonEnvironment) ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
                Note newNote = new Note(this.myPoint, "Escribe una nota aquí...");
                newNote.initializeForView(ArrowTool.this.getView());
                ArrowTool.this.getView().getDrawer().getAutomaton().addNote(newNote);
            }
            ArrowTool.this.getView().repaint();
            ArrowTool.this.emptyMenu = new EmptyMenu();
        }
    }

    public class StateMenu extends JPopupMenu implements ActionListener {
        private State state;
        protected JCheckBoxMenuItem makeFinal;
        protected JCheckBoxMenuItem makeInitial;
        private JMenuItem changeLabel;
        private JMenuItem deleteLabel;
        private JMenuItem deleteAllLabels;
        private JMenuItem editBlock;
        private JMenuItem copyBlock;
        private JMenuItem replaceSymbol;
        private JMenuItem setName;
        private JMenuItem setColor;
        private JMenuItem resetColor;
        private JMenuItem deleteStateItem;

        public StateMenu() {
            this.makeFinal = new JCheckBoxMenuItem("Estado Final (Final)");
            this.setColor = new JMenuItem("Color del Estado...");
            this.resetColor = new JMenuItem("Restablecer Color");
            this.deleteStateItem = new JMenuItem("Eliminar Estado");
            this.makeFinal.addActionListener(this);
            add(this.makeFinal);

            this.makeInitial = new JCheckBoxMenuItem("Estado Inicial (Initial)");
            this.changeLabel = new JMenuItem("Cambiar Etiqueta (Change Label)");
            this.deleteLabel = new JMenuItem("Borrar Etiqueta (Clear Label)");
            this.deleteAllLabels = new JMenuItem("Borrar Todas las Etiquetas");
            this.editBlock = new JMenuItem("Editar Bloque (Edit Block)");
            this.copyBlock = new JMenuItem("Duplicar Bloque (Duplicate Block)");
            this.replaceSymbol = new JMenuItem("Reemplazar Símbolo en Bloque");
            this.setName = new JMenuItem("Cambiar Nombre (Set Name)");

            if (ArrowTool.this.shouldAllowOnlyFinalStateChange()) {
                return;
            }
            this.makeInitial.addActionListener(this);
            this.changeLabel.addActionListener(this);
            this.deleteLabel.addActionListener(this);
            this.deleteAllLabels.addActionListener(this);
            this.editBlock.addActionListener(this);
            this.setName.addActionListener(this);
            this.copyBlock.addActionListener(this);
            this.replaceSymbol.addActionListener(this);
            this.setColor.addActionListener(this);
            this.resetColor.addActionListener(this);
            this.deleteStateItem.addActionListener(this);

            add(this.makeInitial);
            addSeparator();
            add(this.setName);
            add(this.changeLabel);
            add(this.deleteLabel);
            add(this.deleteAllLabels);
            addSeparator();
            add(this.setColor);
            add(this.resetColor);
            addSeparator();
            add(this.deleteStateItem);
        }

        public void show(State state, Component comp, Point at) {
            remove(this.editBlock);
            remove(this.copyBlock);
            remove(this.replaceSymbol);
            this.state = state;
            if (state instanceof TMState) {
                addSeparator();
                add(this.editBlock);
                add(this.copyBlock);
                this.editBlock.setEnabled(true);
                this.copyBlock.setEnabled(true);
                add(this.replaceSymbol);
                this.replaceSymbol.setEnabled(true);
            }
            this.makeFinal.setSelected(ArrowTool.this.getAutomaton().isFinalState(state));
            this.makeInitial.setSelected(ArrowTool.this.getAutomaton().getInitialState() == state);
            this.deleteLabel.setEnabled(state.getLabel() != null);
            show(comp, at.x, at.y);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            if (ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame() != null) {
                ((AutomatonEnvironment) ArrowTool.this.getDrawer().getAutomaton().getEnvironmentFrame().getEnvironment()).saveStatus();
            }
            if (item == this.makeFinal) {
                if (item.isSelected()) {
                    ArrowTool.this.getAutomaton().addFinalState(this.state);
                } else {
                    ArrowTool.this.getAutomaton().removeFinalState(this.state);
                }
            } else if (item == this.makeInitial) {
                if (!item.isSelected()) {
                    this.state = null;
                }
                ArrowTool.this.getAutomaton().setInitialState(this.state);
            } else if (item == this.changeLabel) {
                String oldlabel = this.state.getLabel();
                oldlabel = oldlabel == null ? "" : oldlabel;
                String label = (String) JOptionPane.showInputDialog(this,
                        "Ingresa una nueva etiqueta (o déjalo vacío para quitarla):",
                        "Nueva Etiqueta", JOptionPane.QUESTION_MESSAGE, null, null, oldlabel);
                if (label == null) {
                    return;
                }
                if (label.equals("")) {
                    label = null;
                }
                this.state.setLabel(label);
            } else if (item == this.deleteLabel) {
                this.state.setLabel(null);
            } else if (item == this.deleteAllLabels) {
                State[] states = ArrowTool.this.getAutomaton().getStates();
                for (int i = 0; i < states.length; i++) {
                    states[i].setLabel(null);
                }
            } else if (item == this.deleteStateItem) {
                ArrowTool.this.getAutomaton().removeState(this.state);
            } else if (item == this.editBlock) {
                TMState orig = (TMState) this.state;
                while (((TuringMachine) orig.getAutomaton()).getParent() != null) {
                    orig = ((TuringMachine) orig.getAutomaton()).getParent();
                }
                EditBlockPane editor = new EditBlockPane(((TMState) this.state).getInnerTM());
                EnvironmentFrame rootFrame = orig.getAutomaton().getEnvironmentFrame();
                editor.setBlock(this.state);
                Environment env = rootFrame.getEnvironment();
                env.add(editor, "Edit Block", new CriticalTag() {
                });
                env.setActive(editor);
            } else if (item == this.setColor || item == this.resetColor) {
                Color picked = null;
                if (item == this.setColor) {
                    Color initial = this.state.getFillColor() != null ? this.state.getFillColor() : Theme.STATE_FILL;
                    picked = JColorChooser.showDialog(this, "Color del Estado", initial);
                    if (picked == null) {
                        return;
                    }
                }
                this.state.setFillColor(picked);
                for (State s : ArrowTool.this.getAutomaton().getStates()) {
                    if (s.isSelected()) {
                        s.setFillColor(picked);
                    }
                }
            } else if (item == this.setName) {
                String oldName = this.state.getName();
                oldName = oldName == null ? "" : oldName;
                String name = (String) JOptionPane.showInputDialog(this,
                        "Ingresa un nuevo nombre para el estado:",
                        "Nuevo Nombre", JOptionPane.QUESTION_MESSAGE, null, null, oldName);
                if (name == null) {
                    return;
                }
                if (name.equals("")) {
                    name = null;
                }
                this.state.setName(name);
            } else if (item == this.copyBlock) {
                TMState buffer = ((TuringMachine) ArrowTool.this.getAutomaton()).createTMState(
                        new Point(this.state.getPoint().x + 4, this.state.getPoint().y));
                buffer.setInnerTM((TuringMachine) ((TMState) this.state).getInnerTM().clone());
            } else if (item == this.replaceSymbol) {
                assert this.state instanceof TMState;
                String replaceWith = null;
                String toReplace = null;
                Object oldChar = JOptionPane.showInputDialog((Component) null, "Buscar símbolo (Find):");
                if (oldChar == null) {
                    return;
                }
                if (oldChar instanceof String) {
                    toReplace = (String) oldChar;
                }
                Object newChar = JOptionPane.showInputDialog((Component) null, "Reemplazar con (Replace With):");
                if (newChar == null) {
                    return;
                }
                if (newChar instanceof String) {
                    replaceWith = (String) newChar;
                }
                replaceCharactersInBlock((TMState) this.state, toReplace, replaceWith);
            }
            ArrowTool.this.getView().repaint();
        }

        private void replaceCharactersInBlock(TMState block, String toReplace, String replaceWith) {
            TuringMachine tm = block.getInnerTM();
            for (int i = 0; i < tm.getStates().length; i++) {
                replaceCharactersInBlock((TMState) tm.getStates()[i], toReplace, replaceWith);
            }
            Transition[] trans = tm.getTransitions();
            for (int i = 0; i < trans.length; i++) {
                TMTransition tmTrans = (TMTransition) trans[i];
                for (int k = 0; k < tmTrans.tapes(); k++) {
                    String read = tmTrans.getRead(k);
                    tmTrans.setRead(k, read.replaceAll(toReplace, replaceWith));
                    String write = tmTrans.getWrite(k);
                    tmTrans.setWrite(k, write.replaceAll(toReplace, replaceWith));
                }
            }
        }
    }
}
