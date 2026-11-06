package gui;

import gui.action.BatchMultipleSimulateAction;
import gui.action.MultipleSimulateAction;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import plus.JetBrainsUI;
import plus.Theme;
import plus.VSIcons;

/**
 * Modernized JetBrains Test Runner Table for Multiple Run (F5) and Transducer Simulation.
 */
public class JTableExtender extends JTable {
    private MultipleSimulateAction myMultSimAct;

    public JTableExtender(TableModel model, MultipleSimulateAction mult) {
        super(model);
        this.myMultSimAct = mult;
        applyModernTableStyle();
    }

    public JTableExtender(TableModel model, BatchMultipleSimulateAction mult) {
        super(model);
        this.myMultSimAct = mult;
        applyModernTableStyle();
    }

    private void applyModernTableStyle() {
        setBackground(Theme.WHITE);
        setForeground(Theme.BLACK);
        setGridColor(Theme.BORDER);
        setShowGrid(true);
        setRowHeight(32);
        setFont(new Font("Consolas", Font.PLAIN, 14));
        setSelectionBackground(Theme.TOOL_ACTIVE_BG);
        setSelectionForeground(Color.WHITE);
        setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = getTableHeader();
        if (header != null) {
            header.setBackground(Theme.SURFACE);
            header.setForeground(Theme.TEXT_MUTED);
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setReorderingAllowed(false);
            header.setPreferredSize(new Dimension(100, 32));
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    lbl.setBackground(Theme.SURFACE);
                    lbl.setForeground(Theme.TEXT_MUTED);
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER),
                            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                    return lbl;
                }
            });
        }

        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String txt = value != null ? value.toString() : "";
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                lbl.setFont(table.getFont());

                if (isSelected) {
                    lbl.setBackground(Theme.TOOL_ACTIVE_BG);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(row % 2 == 0 ? Theme.WHITE : Theme.SURFACE);
                    lbl.setForeground(Theme.BLACK);
                }

                if ("Accept".equalsIgnoreCase(txt)) {
                    lbl.setText("✓ Accept");
                    lbl.setFont(table.getFont().deriveFont(Font.BOLD));
                    lbl.setForeground(Theme.FIRED_GREEN);
                } else if ("Reject".equalsIgnoreCase(txt)) {
                    lbl.setText("✕ Reject");
                    lbl.setFont(table.getFont().deriveFont(Font.BOLD));
                    lbl.setForeground(Theme.DANGER);
                }
                return lbl;
            }
        });
    }

    @Override
    public void setGridColor(Color gridColor) {
        // Ignore legacy Color.lightGray and keep JetBrains #393B40 border color
        super.setGridColor(Theme.BORDER != null ? Theme.BORDER : gridColor);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        styleSurroundingMultipleRunPanel();
    }

    private void styleSurroundingMultipleRunPanel() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) p;
                sp.setBackground(Theme.SURFACE);
                sp.getViewport().setBackground(Theme.WHITE);
                sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
                sp.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
                sp.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            } else if (p instanceof JPanel) {
                p.setBackground(Theme.SURFACE);
                for (Component child : p.getComponents()) {
                    if (child instanceof JToolBar) {
                        styleMultipleRunToolBar((JToolBar) child);
                    }
                }
            }
            p = p.getParent();
        }
    }

    private void styleMultipleRunToolBar(JToolBar tb) {
        tb.setFloatable(false);
        tb.setBackground(Theme.SURFACE);
        tb.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
        tb.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        for (Component c : tb.getComponents()) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                String txt = b.getText() != null ? b.getText() : "";
                if ("Run Inputs".equalsIgnoreCase(txt)) {
                    b.setText("Ejecutar Entradas");
                    b.setIcon(VSIcons.get(VSIcons.PLAY, 13, Color.WHITE));
                    JetBrainsUI.styleRunButton(b, "Ejecutar todas las entradas de la tabla");
                } else if ("Load Inputs".equalsIgnoreCase(txt)) {
                    b.setText("Cargar Archivo");
                    JetBrainsUI.styleCompactButton(b, "Cargar cadenas de prueba desde archivo");
                } else if ("Clear".equalsIgnoreCase(txt)) {
                    b.setText("Limpiar");
                    JetBrainsUI.styleCompactButton(b, "Limpiar todas las filas de la tabla");
                } else if (txt.startsWith("Enter ")) {
                    b.setText("Insertar Vacío (λ)");
                    JetBrainsUI.styleCompactButton(b, "Insertar cadena vacía en la celda seleccionada");
                } else if ("View Trace".equalsIgnoreCase(txt)) {
                    b.setText("Ver Traza");
                    b.setIcon(VSIcons.get(VSIcons.MEDIA_TRACE, 14, null));
                    JetBrainsUI.styleCompactButton(b, "Ver la traza paso a paso del resultado seleccionado");
                } else {
                    JetBrainsUI.styleCompactButton(b, txt);
                }
            }
        }
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
        if (this.myMultSimAct != null) {
            this.myMultSimAct.viewAutomaton(this);
        }
    }
}
