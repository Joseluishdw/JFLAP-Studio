package gui;

import gui.environment.Universe;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import plus.Theme;

/**
 * Modernized cell renderer for inline canvas transition tables and lambda/blank cells.
 * Automatically replaces legacy "?" blank placeholders with "□" (\u25A1) and styles inline transition tables.
 */
public class LambdaCellRenderer extends DefaultTableCellRenderer {
    private String toSubstitute;

    public LambdaCellRenderer(String string) {
        super();
        if ("?".equals(string)) {
            this.toSubstitute = "\u25A1";
        } else {
            this.toSubstitute = string;
        }
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    public LambdaCellRenderer() {
        this(Universe.curProfile.getEmptyString());
    }

    public final String getEmpty() {
        return this.toSubstitute;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (table != null && table.getBorder() instanceof EtchedBorder) {
            table.setBackground(Theme.SURFACE);
            table.setForeground(Theme.BLACK);
            table.setGridColor(Theme.BORDER);
            table.setSelectionBackground(Theme.TOOL_ACTIVE_BG);
            table.setSelectionForeground(Color.WHITE);
            table.setFont(new Font("Consolas", Font.BOLD, 13));
            table.setBorder(BorderFactory.createLineBorder(Theme.ACCENT, 2));
        }

        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(new Font("Consolas", Font.BOLD, 13));
        l.setBackground(isSelected ? Theme.TOOL_ACTIVE_BG : Theme.SURFACE);
        l.setForeground(isSelected ? Color.WHITE : Theme.BLACK);
        l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        if (hasFocus && table.isCellEditable(row, column)) {
            return l;
        }
        if ("".equals(value)) {
            l.setText(this.toSubstitute);
            if (!isSelected) {
                l.setForeground(Theme.ACCENT_HOVER);
            }
        }
        return l;
    }
}
