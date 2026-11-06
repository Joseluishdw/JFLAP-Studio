package gui;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plus.JetBrainsUI;
import plus.Theme;

public class TableTextSizeSlider extends JSlider {
    static final int FONT_SIZE_MIN = 1;
    static final int FONT_SIZE_MAX = 600;
    static final int FONT_SIZE_INIT = 140;
    static final String TABLE_SIZE_TITLE = "Tamaño de Texto de Tabla";
    JTable myTable;

    public TableTextSizeSlider(JTable table) {
        super(FONT_SIZE_MIN, FONT_SIZE_MAX, FONT_SIZE_INIT);
        this.myTable = table;
        setBackground(Theme.SURFACE);
        setOpaque(true);
        setUI(new JetBrainsUI.ModernSliderUI(this));
        setPreferredSize(new Dimension(220, 26));
        setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        setToolTipText("Ajustar tamaño de fuente de la tabla de pruebas");
        addChangeListener(new SliderListener());
    }

    class SliderListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            int sz = Math.max(11, source.getValue() / 10);
            myTable.setFont(new Font("Consolas", Font.PLAIN, sz));
            myTable.setRowHeight(sz + 14);
        }
    }
}
