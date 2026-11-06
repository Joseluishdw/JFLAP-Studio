package plus;

import automata.Automaton;
import automata.State;
import automata.Transition;
import automata.graph.AutomatonGraph;
import automata.graph.LayoutAlgorithm;
import automata.graph.layout.GEMLayoutAlgorithm;
import gui.action.MultipleOutputSimulateAction;
import gui.action.MultipleSimulateAction;
import gui.action.OpenAction;
import gui.action.SimulateAction;
import gui.editor.EditorPane;
import gui.editor.RedoTool;
import gui.editor.Tool;
import gui.editor.ToolBar;
import gui.editor.UndoTool;
import gui.environment.AutomatonEnvironment;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.environment.Universe;
import gui.viewer.AutomatonDrawer;
import gui.viewer.AutomatonPane;
import gui.viewer.SelectionDrawer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

public class JFLAPPlus {
    public static void main(String[] args) {
        Theme.apply(Theme.savedPreference(), false);
        installGlobalShortcuts();
        try {
            Class.forName("JFLAP").getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void installGlobalShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                int code = e.getKeyCode();
                int mods = e.getModifiersEx();
                boolean ctrl = (mods & KeyEvent.CTRL_DOWN_MASK) != 0;
                boolean shift = (mods & KeyEvent.SHIFT_DOWN_MASK) != 0;
                boolean alt = (mods & KeyEvent.ALT_DOWN_MASK) != 0;

                if (code == KeyEvent.VK_F1 && !ctrl && !alt) {
                    showShortcutsHelp(focus);
                    return true;
                }

                if (ctrl && shift && !alt && code == KeyEvent.VK_D) {
                    Theme.toggle();
                    return true;
                }

                if (ctrl && shift && !alt && code == KeyEvent.VK_T) {
                    EditorPane ep = activeEditor(focus);
                    if (ep != null) {
                        RuleTable.open(ep);
                        return true;
                    }
                    return false;
                }

                if (ctrl && !alt && code == KeyEvent.VK_B) {
                    EditorPane ep = activeEditor(focus);
                    if (ep != null) {
                        ep.toggleSidebar();
                        return true;
                    }
                }

                if (ctrl && !alt && code == KeyEvent.VK_G) {
                    if (shift) {
                        Theme.toggleSnapToGrid();
                    } else {
                        Theme.toggleGrid();
                    }
                    return true;
                }

                if (inTextOrTable(focus)) {
                    return false;
                }

                EditorPane ep = activeEditor(focus);
                if (ep != null) {
                    if (hasActiveTransitionEditor(ep)) {
                        return false;
                    }
                    if (!ctrl && !shift && !alt && (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE)) {
                        return deleteSelection(ep);
                    }
                    if (ctrl && !alt && code == KeyEvent.VK_Z) {
                        if (shift) {
                            return redo(ep);
                        }
                        return undo(ep);
                    }
                    if (ctrl && !shift && !alt && code == KeyEvent.VK_Y) {
                        return redo(ep);
                    }
                    if (ctrl && !shift && !alt && code == KeyEvent.VK_A) {
                        selectAllStates(ep);
                        return true;
                    }
                    if (ctrl && !shift && !alt && code == KeyEvent.VK_D) {
                        duplicateSelectedStates(ep);
                        return true;
                    }
                    if (ctrl && !alt && (code == KeyEvent.VK_PLUS || code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_ADD)) {
                        zoom(ep, 1.18);
                        return true;
                    }
                    if (ctrl && !alt && (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT)) {
                        zoom(ep, 1.0 / 1.18);
                        return true;
                    }
                    if (ctrl && !alt && (code == KeyEvent.VK_0 || code == KeyEvent.VK_NUMPAD0)) {
                        resetZoom(ep);
                        return true;
                    }
                    if (!ctrl && !shift && !alt && code == KeyEvent.VK_F5) {
                        runMultipleSimulation(ep);
                        return true;
                    }
                    if ((ctrl || alt) && !shift && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                        return pickTool(ep, code - KeyEvent.VK_1);
                    }
                }
                return false;
            }
        });
    }

    private static boolean hasActiveTransitionEditor(EditorPane ep) {
        if (ep == null || ep.getAutomatonPane() == null) {
            return false;
        }
        for (Component c : ep.getAutomatonPane().getComponents()) {
            if (c instanceof JTable && c.isShowing()) {
                return true;
            }
        }
        return false;
    }

    private static boolean inTextOrTable(Component c) {
        for (Component cur = c; cur != null; cur = cur.getParent()) {
            if (cur instanceof JTextComponent) {
                return ((JTextComponent) cur).isEditable() && cur.isEnabled();
            }
            if (cur instanceof JTable) {
                return true;
            }
        }
        return false;
    }

    private static EditorPane activeEditor(Component c) {
        Window w = c != null ? SwingUtilities.getWindowAncestor(c) : null;
        if (w == null) {
            w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        }
        if (w == null) {
            return null;
        }
        return findShowingEditor(w);
    }

    private static EditorPane findShowingEditor(Component c) {
        if (c == null || !c.isShowing()) {
            return null;
        }
        if (c instanceof EditorPane) {
            return (EditorPane) c;
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                EditorPane found = findShowingEditor(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static List<File> discoverJffFiles() {
        Set<File> dirs = new LinkedHashSet<File>();
        try {
            if (Universe.CHOOSER != null && Universe.CHOOSER.getCurrentDirectory() != null) {
                dirs.add(Universe.CHOOSER.getCurrentDirectory());
            }
        } catch (Throwable ignored) {
        }
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isEmpty()) {
            dirs.add(new File(userHome, "Desktop\\Trabajos\\JFLAP"));
            dirs.add(new File(userHome, "Desktop\\Trabajos"));
            dirs.add(new File(userHome, "Desktop"));
            dirs.add(new File(userHome, "Downloads"));
            dirs.add(new File(userHome, "Documents"));
        }
        dirs.add(new File("."));

        Set<File> found = new LinkedHashSet<File>();
        Set<String> seenCanonical = new LinkedHashSet<String>();
        try {
            File lastOpened = OpenAction.getLastFileOpened();
            if (lastOpened != null && lastOpened.exists() && lastOpened.isFile()) {
                File canon = lastOpened.getCanonicalFile();
                if (seenCanonical.add(canon.getAbsolutePath().toLowerCase())) {
                    found.add(canon);
                }
            }
        } catch (Throwable ignored) {
        }
        for (File dir : dirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] children = dir.listFiles();
                if (children != null) {
                    for (File c : children) {
                        if (c.isFile() && c.getName().toLowerCase().endsWith(".jff")) {
                            try {
                                File canon = c.getCanonicalFile();
                                if (seenCanonical.add(canon.getAbsolutePath().toLowerCase())) {
                                    found.add(canon);
                                }
                            } catch (Throwable t) {
                                if (seenCanonical.add(c.getAbsolutePath().toLowerCase())) {
                                    found.add(c);
                                }
                            }
                        }
                    }
                }
            }
        }
        List<File> list = new ArrayList<File>(found);
        list.sort(new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return list;
    }

    public static void zoom(EditorPane ep, double factor) {
        if (ep == null || ep.getAutomatonPane() == null) return;
        AutomatonPane pane = ep.getAutomatonPane();
        double current = pane.getScale();
        if (current <= 0.05) current = 1.0;
        double next = Math.max(0.25, Math.min(4.0, current * factor));
        pane.setScale(next);
        pane.requestTransform();
        pane.repaint();
    }

    public static void resetZoom(EditorPane ep) {
        if (ep == null || ep.getAutomatonPane() == null) return;
        AutomatonPane pane = ep.getAutomatonPane();
        pane.setScale(1.0);
        pane.requestTransform();
        pane.repaint();
    }

    public static void autoLayout(EditorPane ep) {
        if (ep == null || ep.getAutomaton() == null) return;
        saveUndo(ep.getAutomaton());
        AutomatonGraph graph = new AutomatonGraph(ep.getAutomaton());
        LayoutAlgorithm layout = new GEMLayoutAlgorithm();
        layout.layout(graph, null);
        graph.moveAutomatonStates();
        if (ep.getAutomatonPane() != null) {
            ep.getAutomatonPane().fitToBounds(30);
            ep.getAutomatonPane().requestTransform();
            ep.getAutomatonPane().repaint();
        }
    }

    public static void selectAllStates(EditorPane ep) {
        if (ep == null || ep.getAutomaton() == null) return;
        Automaton aut = ep.getAutomaton();
        AutomatonDrawer drawer = ep.getDrawer();
        for (State s : aut.getStates()) {
            s.setSelect(true);
            if (drawer instanceof SelectionDrawer) {
                ((SelectionDrawer) drawer).addSelected(s);
            }
        }
        if (ep.getAutomatonPane() != null) {
            ep.getAutomatonPane().repaint();
        }
    }

    public static void duplicateSelectedStates(EditorPane ep) {
        if (ep == null || ep.getAutomaton() == null) return;
        Automaton aut = ep.getAutomaton();
        State[] all = aut.getStates();
        Map<State, State> map = new HashMap<State, State>();
        boolean any = false;
        for (State s : all) {
            if (s.isSelected()) {
                any = true;
                break;
            }
        }
        if (!any) return;

        saveUndo(aut);
        for (State s : all) {
            if (s.isSelected()) {
                s.setSelect(false);
                Point pt = s.getPoint();
                State copy = aut.createState(new Point(pt.x + 40, pt.y + 40));
                if (s.getLabel() != null) copy.setLabel(s.getLabel());
                if (s.getFillColor() != null) copy.setFillColor(s.getFillColor());
                copy.setSelect(true);
                map.put(s, copy);
            }
        }
        for (Transition t : aut.getTransitions()) {
            State fromCopy = map.get(t.getFromState());
            State toCopy = map.get(t.getToState());
            if (fromCopy != null && toCopy != null) {
                try {
                    Transition copyT = t.copy(fromCopy, toCopy);
                    aut.addTransition(copyT);
                } catch (Throwable ignored) {
                }
            }
        }
        if (ep.getAutomatonPane() != null) {
            ep.getAutomatonPane().repaint();
        }
    }

    public static void runStepSimulation(EditorPane ep) {
        if (ep == null || ep.getAutomaton() == null) return;
        Automaton aut = ep.getAutomaton();
        EnvironmentFrame frame = aut.getEnvironmentFrame();
        if (frame != null && frame.getEnvironment() != null) {
            Environment env = frame.getEnvironment();
            if (SimulateAction.isApplicable(aut)) {
                new SimulateAction(aut, env).actionPerformed(new ActionEvent(ep, ActionEvent.ACTION_PERFORMED, "Simulate"));
            }
        }
    }

    public static void runMultipleSimulation(EditorPane ep) {
        if (ep == null || ep.getAutomaton() == null) return;
        Automaton aut = ep.getAutomaton();
        EnvironmentFrame frame = aut.getEnvironmentFrame();
        if (frame != null && frame.getEnvironment() != null) {
            Environment env = frame.getEnvironment();
            if (MultipleOutputSimulateAction.isApplicable(aut)) {
                new MultipleOutputSimulateAction(aut, env).actionPerformed(new ActionEvent(ep, ActionEvent.ACTION_PERFORMED, "MultipleOutput"));
            } else if (SimulateAction.isApplicable(aut)) {
                new MultipleSimulateAction(aut, env).actionPerformed(new ActionEvent(ep, ActionEvent.ACTION_PERFORMED, "MultipleRun"));
            }
        }
    }

    public static boolean deleteSelection(EditorPane ep) {
        Automaton aut = ep.getAutomaton();
        if (aut == null) {
            return false;
        }
        AutomatonDrawer drawer = ep.getDrawer();
        SelectionDrawer sel = drawer instanceof SelectionDrawer ? (SelectionDrawer) drawer : null;
        boolean any = false;
        for (State s : aut.getStates()) {
            if (s.isSelected() || (sel != null && sel.isSelected(s))) {
                any = true;
                break;
            }
        }
        if (!any) {
            for (Transition t : aut.getTransitions()) {
                if (t.isSelected || (sel != null && sel.isSelected(t))) {
                    any = true;
                    break;
                }
            }
        }
        if (!any) {
            return false;
        }
        saveUndo(aut);
        for (Transition t : (Transition[]) aut.getTransitions().clone()) {
            if (t.isSelected || (sel != null && sel.isSelected(t))) {
                aut.removeTransition(t);
            }
        }
        for (State s : (State[]) aut.getStates().clone()) {
            if (s.isSelected() || (sel != null && sel.isSelected(s))) {
                aut.removeState(s);
            }
        }
        if (sel != null) {
            sel.clearSelected();
            sel.clearSelectedTransitions();
        }
        ep.getAutomatonPane().repaint();
        return true;
    }

    public static void saveUndo(Automaton aut) {
        try {
            EnvironmentFrame ef = aut.getEnvironmentFrame();
            if (ef != null && ef.getEnvironment() instanceof AutomatonEnvironment) {
                ((AutomatonEnvironment) ef.getEnvironment()).saveStatus();
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean undo(EditorPane ep) {
        try {
            EnvironmentFrame ef = ep.getAutomaton().getEnvironmentFrame();
            if (ef != null && ef.getEnvironment() instanceof AutomatonEnvironment) {
                ((AutomatonEnvironment) ef.getEnvironment()).restoreStatus();
                ep.getAutomatonPane().repaint();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return clickToolOfClass(ep.getToolBar(), UndoTool.class);
    }

    public static boolean redo(EditorPane ep) {
        try {
            EnvironmentFrame ef = ep.getAutomaton().getEnvironmentFrame();
            if (ef != null && ef.getEnvironment() instanceof AutomatonEnvironment) {
                ((AutomatonEnvironment) ef.getEnvironment()).redo();
                ep.getAutomatonPane().repaint();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return clickToolOfClass(ep.getToolBar(), RedoTool.class);
    }

    public static boolean pickTool(EditorPane ep, int index) {
        ToolBar tb = ep.getToolBar();
        if (tb == null) {
            return false;
        }
        List<Tool> tools = getTools(tb);
        if (tools == null || index < 0 || index >= tools.size()) {
            return false;
        }
        Tool target = tools.get(index);
        if (target instanceof UndoTool) {
            return undo(ep);
        }
        if (target instanceof RedoTool) {
            return redo(ep);
        }
        Map<AbstractButton, Tool> map = getButtonMap(tb);
        if (map != null) {
            for (Map.Entry<AbstractButton, Tool> entry : map.entrySet()) {
                if (entry.getValue() == target) {
                    entry.getKey().doClick();
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean clickToolOfClass(ToolBar tb, Class<?> cls) {
        if (tb == null) {
            return false;
        }
        Map<AbstractButton, Tool> map = getButtonMap(tb);
        if (map == null) {
            return false;
        }
        for (Map.Entry<AbstractButton, Tool> e : map.entrySet()) {
            if (cls.isInstance(e.getValue())) {
                e.getKey().doClick();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Tool> getTools(ToolBar tb) {
        try {
            Field f = ToolBar.class.getDeclaredField("tools");
            f.setAccessible(true);
            return (List<Tool>) f.get(tb);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<AbstractButton, Tool> getButtonMap(ToolBar tb) {
        try {
            Field f = ToolBar.class.getDeclaredField("buttonsToTools");
            f.setAccessible(true);
            return (Map<AbstractButton, Tool>) f.get(tb);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void showShortcutsHelp(Component parent) {
        Window win = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        final JDialog dlg = new JDialog(win, "JFLAP Studio — Guía Rápida y Atajos", JDialog.ModalityType.MODELESS);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel(new BorderLayout(6, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("JFLAP Studio  |  Atajos de Teclado y Expresiones");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Theme.ACCENT);
        JLabel subtitle = new JLabel("Entorno estilo Visual Studio para diseñar y depurar autómatas, gramáticas y máquinas de Turing.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Theme.TEXT_MUTED);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 14, 0));
        content.setOpaque(false);

        JPanel col1 = createHelpCard("Atajos del Editor (Visual Studio Style)", new String[][]{
            {"Ctrl + 1..4 / Alt + 1..4", "Cambiar herramienta (Cursor, Estado, Transición, Borrar)"},
            {"Ctrl + B", "Mostrar / Ocultar Explorador e Inspector lateral"},
            {"Supr / Backspace", "Eliminar estado o transición seleccionada"},
            {"Ctrl + Z / Ctrl + Y", "Deshacer / Rehacer cambios en el autómata"},
            {"Ctrl + A", "Seleccionar todos los estados del lienzo"},
            {"Ctrl + D", "Duplicar los estados seleccionados y sus aristas"},
            {"Ctrl + G", "Mostrar / Ocultar cuadrícula de guía"},
            {"Ctrl + Shift + G", "Activar / Desactivar ajuste magnético (Imán)"},
            {"Ctrl + Shift + D", "Alternar entre tema VS Dark+ y VS Light+"},
            {"F5", "Ejecutar Prueba Múltiple (Multiple Run) al instante"}
        });

        JPanel col2 = createHelpCard("Lienzo, Navegación y Expresiones", new String[][]{
            {"Rueda del Ratón", "Acercar / Alejar zoom suavemente en el lienzo"},
            {"Clic Central / Alt+Arrastrar", "Desplazar el lienzo libremente (Panning)"},
            {"Doble Clic en Arista", "Editar los símbolos de una transición existente"},
            {"Clic Derecho en Estado", "Menú contextual (Inicial, Final, Nombre, Color)"},
            {"Ctrl + Shift + T", "Abrir editor de Tabla de Transiciones (Turing)"},
            {"Rangos [a-z], [0-9]", "Válidos en transiciones AFD/AFND/AP/Turing (ej. [0-2])"},
            {"Atajos \\d, \\w, \\s", "Dígito [0-9], palabra [A-Za-z0-9_], espacio en blanco"},
            {"Negación [^abc]", "Cualquier símbolo excepto los indicados"},
            {"Regla !a en Turing", "Coincide con cualquier símbolo distinto de 'a'"},
            {"Simulación Auto", "Botón 'Auto' en simulación paso a paso para animar"}
        });

        content.add(col1);
        content.add(col2);
        root.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel tip = new JLabel("Tip: Usa el Inspector lateral izquierdo para marcar estados Iniciales/Finales o abrir otros archivos .jff.");
        tip.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        tip.setForeground(Theme.TEXT_MUTED);
        JButton closeBtn = new JButton("Cerrar");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setBackground(Theme.ACCENT);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dlg.dispose());
        footer.add(tip, BorderLayout.WEST);
        footer.add(closeBtn, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        AppIcon.apply(dlg);
        WindowChrome.installDialog(dlg);
        dlg.pack();
        dlg.setLocationRelativeTo(win);
        dlg.setVisible(true);
    }

    private static JPanel createHelpCard(String heading, String[][] rows) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel hLabel = new JLabel(heading);
        hLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hLabel.setForeground(Theme.BLACK);
        hLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hLabel);
        card.add(Box.createVerticalStrut(10));

        for (String[] row : rows) {
            JPanel line = new JPanel(new BorderLayout(10, 2));
            line.setOpaque(false);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            line.setMaximumSize(new Dimension(540, 32));

            JLabel key = new JLabel(row[0]);
            key.setFont(new Font("Segoe UI", Font.BOLD, 12));
            key.setForeground(Theme.ACCENT_HOVER);
            key.setPreferredSize(new Dimension(165, 22));

            JLabel desc = new JLabel(row[1]);
            desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            desc.setForeground(Theme.BLACK);

            line.add(key, BorderLayout.WEST);
            line.add(desc, BorderLayout.CENTER);
            card.add(line);
            card.add(Box.createVerticalStrut(4));
        }
        return card;
    }
}
