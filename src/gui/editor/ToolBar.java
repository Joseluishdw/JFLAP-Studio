package gui.editor;

import gui.viewer.AutomatonDrawer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import plus.JFLAPPlus;
import plus.JetBrainsUI;
import plus.Theme;
import plus.VSIcons;

/**
 * Floating Island Toolbar (Floating Dock) centered at the top of the Automaton Canvas.
 * Contains ONLY drawing and layout tools:
 * Cursor, Estado, Transición, Borrar | Deshacer, Rehacer | Auto-Ordenar.
 * Background: #2B2D30, Border: #43454A, arc = 16, soft drop shadow.
 */
public class ToolBar extends JToolBar implements ActionListener {
    private Component view;
    private AutomatonDrawer drawer;
    private List tools;
    private HashMap buttonsToTools;
    private ToolAdapter adapter;
    private Tool currentTool = null;

    public ToolBar(EditCanvas view, AutomatonDrawer drawer, ToolBox box) {
        this.adapter = new ToolAdapter(view);
        this.view = view;
        this.drawer = drawer;
        this.tools = box.tools(view, drawer);
        this.buttonsToTools = new HashMap();
        setFloatable(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        initBar();
        view.addMouseListener(this.adapter);
        view.addMouseMotionListener(this.adapter);
    }

    protected Component getView() {
        return this.view;
    }

    protected AutomatonDrawer getDrawer() {
        return this.drawer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Soft multi-layer drop shadow under the floating pill
        g2.setColor(new Color(0, 0, 0, Theme.isDark() ? 85 : 28));
        g2.fill(new RoundRectangle2D.Float(5, 5, w - 10, h - 7, 18, 18));
        g2.setColor(new Color(0, 0, 0, Theme.isDark() ? 45 : 16));
        g2.fill(new RoundRectangle2D.Float(3, 4, w - 6, h - 5, 20, 20));

        // Floating Island Pill Fill (#2B2D30 in Island Dark)
        g2.setColor(Theme.CARD_BG);
        g2.fill(new RoundRectangle2D.Float(4, 2, w - 8, h - 8, 16, 16));

        // 1px Floating Dock Border (#43454A in Island Dark)
        g2.setColor(Theme.FLOATING_BORDER);
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(new RoundRectangle2D.Float(4.5f, 2.5f, w - 9f, h - 9f, 16, 16));

        g2.dispose();
    }

    private void initBar() {
        ButtonGroup group = new ButtonGroup();
        Iterator it = this.tools.iterator();
        int idx = 1;
        boolean addedUndoSeparator = false;
        boolean hasStateTool = false;
        boolean hasTransitionTool = false;
        while (it.hasNext()) {
            Tool tool = (Tool) it.next();
            if (tool instanceof StateTool) {
                if (hasStateTool) {
                    continue;
                }
                hasStateTool = true;
            } else if (tool instanceof TransitionTool) {
                if (hasTransitionTool) {
                    continue;
                }
                hasTransitionTool = true;
            } else if (!(tool instanceof ArrowTool || tool instanceof DeleteTool || tool instanceof UndoTool || tool instanceof RedoTool)) {
                continue;
            }
            String label = friendlyLabel(tool, idx);
            Icon icon = modernToolIcon(tool);
            String shortcutTip = tool.getToolTip() + "  (Ctrl+" + idx + " / Alt+" + idx + ")";
            if (tool instanceof UndoTool || tool instanceof RedoTool) {
                if (!addedUndoSeparator) {
                    add(JetBrainsUI.createToolbarSeparator());
                    addedUndoSeparator = true;
                }
                JButton button = new JButton(label, icon);
                JetBrainsUI.styleGhostButton(button, tool instanceof UndoTool ? "Deshacer (Ctrl+Z)" : "Rehacer (Ctrl+Y)");
                this.buttonsToTools.put(button, tool);
                add(button);
                button.addActionListener(this);
            } else {
                JToggleButton button = new JToggleButton(label, icon);
                JetBrainsUI.styleGhostButton(button, shortcutTip);
                this.buttonsToTools.put(button, tool);
                add(button);
                group.add(button);
                button.addActionListener(this);
                if (this.currentTool == null) {
                    this.currentTool = tool;
                    this.adapter.setAdapter(tool);
                    button.setSelected(true);
                }
            }
            add(Box.createHorizontalStrut(2));
            idx++;
        }

        add(JetBrainsUI.createToolbarSeparator());

        JButton layoutBtn = createGhostButton("Auto-Ordenar", VSIcons.get(VSIcons.LAYOUT, 15, null),
                "Organizar automáticamente los estados del grafo (Layout Graph)", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        EditorPane ep = getEditorPane();
                        if (ep != null) {
                            JFLAPPlus.autoLayout(ep);
                        }
                    }
                });
        add(layoutBtn);
    }

    public void reapplyGhostStyles() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        for (Component c : getComponents()) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                JetBrainsUI.styleGhostButton(b, b.getToolTipText());
            }
        }
        repaint();
    }

    private Icon modernToolIcon(Tool tool) {
        if (tool instanceof ArrowTool) {
            return VSIcons.get(VSIcons.CURSOR, 15, null);
        }
        if (tool instanceof StateTool) {
            return VSIcons.get(VSIcons.STATE, 15, null);
        }
        if (tool instanceof TransitionTool) {
            return VSIcons.get(VSIcons.TRANSITION, 15, null);
        }
        if (tool instanceof DeleteTool) {
            return VSIcons.get(VSIcons.DELETE, 15, null);
        }
        if (tool instanceof UndoTool) {
            return VSIcons.get(VSIcons.UNDO, 15, null);
        }
        if (tool instanceof RedoTool) {
            return VSIcons.get(VSIcons.REDO, 15, null);
        }
        return tool.getIcon();
    }

    private EditorPane getEditorPane() {
        if (this.view instanceof EditCanvas) {
            return ((EditCanvas) this.view).getCreator();
        }
        return null;
    }

    private String friendlyLabel(Tool tool, int idx) {
        String tip = tool.getToolTip();
        if (tool instanceof ArrowTool) {
            return "Cursor";
        }
        if (tool instanceof StateTool) {
            return "Estado";
        }
        if (tool instanceof TransitionTool) {
            return "Transición";
        }
        if (tool instanceof DeleteTool) {
            return "Borrar";
        }
        if (tool instanceof UndoTool) {
            return "";
        }
        if (tool instanceof RedoTool) {
            return "";
        }
        if (tip != null && tip.toLowerCase().contains("block")) {
            return "Bloque";
        }
        return String.valueOf(idx);
    }

    private JButton createGhostButton(String text, Icon icon, String tooltip, ActionListener listener) {
        JButton btn = icon != null ? new JButton(text, icon) : new JButton(text);
        JetBrainsUI.styleGhostButton(btn, tooltip);
        btn.addActionListener(listener);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Tool tool = (Tool) this.buttonsToTools.get(e.getSource());
        if (tool != null) {
            this.adapter.setAdapter(tool);
            this.currentTool = tool;
            this.view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (tool instanceof UndoTool) {
            EditorPane ep = getEditorPane();
            if (ep != null && ep.getAutomaton() != null && ep.getAutomaton().getEnvironmentFrame() != null) {
                ((gui.environment.AutomatonEnvironment) ep.getAutomaton().getEnvironmentFrame().getEnvironment()).restoreStatus();
                this.view.repaint();
            }
        }
        if (tool instanceof RedoTool) {
            EditorPane ep = getEditorPane();
            if (ep != null && ep.getAutomaton() != null && ep.getAutomaton().getEnvironmentFrame() != null) {
                ((gui.environment.AutomatonEnvironment) ep.getAutomaton().getEnvironmentFrame().getEnvironment()).redo();
                this.view.repaint();
            }
        }
    }

    public void drawTool(Graphics g) {
        if (this.currentTool == null) {
            return;
        }
        this.currentTool.draw(g);
    }

    public Tool getCurrentTool() {
        return this.currentTool;
    }
}
