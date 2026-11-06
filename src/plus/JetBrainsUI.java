package plus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.MenuElement;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.plaf.basic.BasicMenuBarUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * Custom Java2D UI delegates and rounded Island UI components that replicate
 * JetBrains New UI / IntelliJ Island Dark & Light in pure Swing.
 */
public final class JetBrainsUI {
    private JetBrainsUI() {
    }

    public static void styleGhostButton(AbstractButton b, String tooltip) {
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(Theme.BLACK);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        String txt = b.getText();
        if (txt == null || txt.isEmpty()) {
            b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        } else {
            b.setBorder(BorderFactory.createEmptyBorder(6, 11, 6, 11));
        }
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (b instanceof JToggleButton) {
            b.setUI(new GhostToggleButtonUI());
        } else {
            b.setUI(new GhostButtonUI());
        }
    }

    public static void styleRunButton(AbstractButton b, String tooltip) {
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setUI(new RunButtonUI());
    }

    public static void styleSecondaryButton(AbstractButton b, boolean primary, String tooltip) {
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(primary ? Color.WHITE : Theme.BLACK);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setUI(new RoundedActionButtonUI(primary, 8));
    }

    public static void styleCompactButton(AbstractButton b, String tooltip) {
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setForeground(Theme.BLACK);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setUI(new RoundedActionButtonUI(false, 8));
    }

    public static JComponent createToolbarSeparator() {
        JComponent sep = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.FLOATING_BORDER);
                int x = getWidth() / 2;
                g2.drawLine(x, 6, x, getHeight() - 6);
                g2.dispose();
            }
        };
        sep.setPreferredSize(new Dimension(11, 26));
        sep.setMaximumSize(new Dimension(11, 26));
        sep.setOpaque(false);
        return sep;
    }

    /**
     * Styles a JMenuBar and all its JMenus and JPopupMenus in JetBrains Island Dark/Light:
     * - No border on JMenuBar, integrated in #1E1F22
     * - EmptyBorder(6, 10, 6, 10) on each JMenu
     * - #2B2D30 background and #43454A 1px border on all JPopupMenu dropdowns
     */
    public static void styleMenuBar(JMenuBar menuBar) {
        if (menuBar == null) {
            return;
        }
        menuBar.setUI(new ModernMenuBarUI());
        menuBar.setBackground(Theme.SURFACE);
        menuBar.setOpaque(true);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                styleMenuRecursive(menu);
            }
        }
        WindowChrome.integrateMenuBar(menuBar);
    }

    private static void styleMenuRecursive(JMenu menu) {
        menu.setUI(new ModernMenuUI());
        menu.setFont(Theme.FONT_PLAIN);
        menu.setForeground(Theme.BLACK);
        menu.setBackground(Theme.SURFACE);
        menu.setOpaque(false);
        menu.setBorderPainted(false);
        menu.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPopupMenu popup = menu.getPopupMenu();
        if (popup != null) {
            stylePopupMenu(popup);
        }
    }

    public static void stylePopupMenu(JPopupMenu popup) {
        if (popup == null) {
            return;
        }
        popup.setUI(new ModernPopupMenuUI());
        popup.setBackground(Theme.CARD_BG);
        popup.setOpaque(true);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.FLOATING_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 2, 4, 2)));

        for (MenuElement elem : popup.getSubElements()) {
            Component comp = elem.getComponent();
            if (comp instanceof JMenu) {
                styleMenuRecursive((JMenu) comp);
            } else if (comp instanceof javax.swing.JCheckBoxMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                item.setUI(new ModernCheckBoxMenuItemUI());
                item.setFont(Theme.FONT_PLAIN);
                item.setForeground(Theme.BLACK);
                item.setBackground(Theme.CARD_BG);
                item.setOpaque(true);
                item.setBorderPainted(false);
                item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
            } else if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                item.setUI(new ModernMenuItemUI());
                item.setFont(Theme.FONT_PLAIN);
                item.setForeground(Theme.BLACK);
                item.setBackground(Theme.CARD_BG);
                item.setOpaque(true);
                item.setBorderPainted(false);
                item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
            }
        }
    }

    /**
     * Flat Modern MenuBarUI (#1E1F22 background, zero 3D borders).
     */
    public static class ModernMenuBarUI extends BasicMenuBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernMenuBarUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            c.setBackground(Theme.SURFACE);
            c.setOpaque(true);
            c.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
    }

    /**
     * Flat Modern MenuUI with EmptyBorder(6, 10, 6, 10) and rounded #2E3035 hover pill.
     */
    public static class ModernMenuUI extends BasicMenuUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernMenuUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            menuItem.setOpaque(false);
            menuItem.setBorderPainted(false);
            menuItem.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            menuItem.setFont(Theme.FONT_PLAIN);
            menuItem.setForeground(Theme.BLACK);
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            ButtonModel model = menuItem.getModel();
            if (model.isArmed() || model.isSelected()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.HOVER_BG);
                g2.fill(new RoundRectangle2D.Float(2, 2, menuItem.getWidth() - 4, menuItem.getHeight() - 4, 6, 6));
                g2.dispose();
            }
        }
    }

    /**
     * Flat Modern PopupMenuUI (#2B2D30 background, 1px #43454A border).
     */
    public static class ModernPopupMenuUI extends BasicPopupMenuUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernPopupMenuUI();
        }

        @Override
        public void installDefaults() {
            super.installDefaults();
            popupMenu.setBackground(Theme.CARD_BG);
            popupMenu.setOpaque(true);
            popupMenu.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.FLOATING_BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 2, 4, 2)));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            g.setColor(Theme.CARD_BG);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            super.paint(g, c);
        }
    }

    /**
     * Flat Modern MenuItemUI (#2B2D30 background, rounded #2E436E highlight on hover).
     */
    public static class ModernMenuItemUI extends BasicMenuItemUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernMenuItemUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            menuItem.setOpaque(false);
            menuItem.setBorderPainted(false);
            menuItem.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
            menuItem.setForeground(Theme.BLACK);
            menuItem.setBackground(Theme.CARD_BG);
            selectionBackground = Theme.ACTIVE_BG;
            selectionForeground = Theme.BLACK;
            acceleratorForeground = Theme.TEXT_MUTED;
            acceleratorSelectionForeground = Theme.BLACK;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            ButtonModel model = menuItem.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.CARD_BG);
            g2.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
            if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fill(new RoundRectangle2D.Float(3, 1, menuItem.getWidth() - 6, menuItem.getHeight() - 2, 6, 6));
            }
            g2.dispose();
        }
    }

    /**
     * Flat Modern CheckBoxMenuItemUI.
     */
    public static class ModernCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernCheckBoxMenuItemUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            menuItem.setOpaque(false);
            menuItem.setBorderPainted(false);
            menuItem.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
            menuItem.setForeground(Theme.BLACK);
            menuItem.setBackground(Theme.CARD_BG);
            selectionBackground = Theme.ACTIVE_BG;
            selectionForeground = Theme.BLACK;
            acceleratorForeground = Theme.TEXT_MUTED;
            acceleratorSelectionForeground = Theme.BLACK;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            ButtonModel model = menuItem.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.CARD_BG);
            g2.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
            if (model.isArmed()) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fill(new RoundRectangle2D.Float(3, 1, menuItem.getWidth() - 6, menuItem.getHeight() - 2, 6, 6));
            }
            g2.dispose();
        }
    }

    /**
     * Flat Modern TabbedPaneUI (IntelliJ / VS Code style editor tabs):
     * - Height = 36px
     * - Background = #1E1F22 (Theme.SURFACE)
     * - No 3D beveled borders
     * - 3px #3574F0 bottom indicator line under the active tab
     */
    public static class ModernTabbedPaneUI extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernTabbedPaneUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(0, 10, 0, 10);
            contentBorderInsets = new Insets(1, 0, 0, 0);
            tabInsets = new Insets(0, 16, 0, 16);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            if (tabPane != null) {
                tabPane.setBackground(Theme.SURFACE);
                tabPane.setForeground(Theme.BLACK);
                tabPane.setOpaque(true);
                tabPane.setBorder(BorderFactory.createEmptyBorder());
            }
        }

        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return 36;
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 8;
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, tabPane.getWidth(), calculateMaxTabHeight(tabPlacement) + 2);
            g.setColor(Theme.CARD_BORDER);
            int lineY = calculateMaxTabHeight(tabPlacement);
            g.drawLine(0, lineY, tabPane.getWidth(), lineY);
            super.paintTabArea(g, tabPlacement, selectedIndex);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fillRect(x, y, w, h);

            if (isSelected) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(x + 4, y + h - 3, w - 8, 3, 3, 3);
            }
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
            // No 3D beveled border
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            int tabAreaH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            g.setColor(Theme.CARD_BORDER);
            g.drawLine(0, tabAreaH - 1, tabPane.getWidth(), tabAreaH - 1);
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                           int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            // No focus rectangle
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                 int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font f = isSelected ? Theme.FONT_BOLD : Theme.FONT_PLAIN;
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            g2.setColor(isSelected ? Theme.BLACK : Theme.TEXT_MUTED);
            int tx = textRect.x;
            int ty = textRect.y + fm.getAscent();
            g2.drawString(title, tx, ty);
            g2.dispose();
        }
    }

    /**
     * Ghost Button UI for Toolbar JButtons (transparent default, #2E3035 hover, #2E436E pressed, arc=8).
     */
    public static class GhostButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent c) {
            return new GhostButtonUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = c.getWidth();
            int h = c.getHeight();
            if (model.isPressed() || model.isSelected()) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 8, 8));
            } else if (model.isRollover()) {
                g2.setColor(Theme.HOVER_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
            }
            g2.dispose();
            b.setForeground(Theme.BLACK);
            super.paint(g, c);
        }
    }

    /**
     * Ghost ToggleButton UI for Toolbar Tools (transparent default, #2E3035 hover, #2E436E / #3574F0 active, arc=8).
     */
    public static class GhostToggleButtonUI extends BasicToggleButtonUI {
        public static ComponentUI createUI(JComponent c) {
            return new GhostToggleButtonUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = c.getWidth();
            int h = c.getHeight();
            if (model.isSelected()) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 8, 8));
            } else if (model.isPressed()) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
            } else if (model.isRollover()) {
                g2.setColor(Theme.HOVER_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
            }
            g2.dispose();
            b.setForeground(model.isSelected() && !Theme.isDark() ? Theme.ACCENT : Theme.BLACK);
            super.paint(g, c);
        }
    }

    /**
     * IDE Run Button UI (#2EA043 green fill, #3FB950 hover, white text, arc = 8).
     */
    public static class RunButtonUI extends BasicButtonUI {
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = c.getWidth();
            int h = c.getHeight();
            Color bg = m.isPressed() ? Theme.RUN_GREEN.darker() : (m.isRollover() ? Theme.RUN_GREEN_HOVER : Theme.RUN_GREEN);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));
            g2.dispose();

            b.setForeground(Color.WHITE);
            super.paint(g, c);
        }
    }

    /**
     * Rounded Action Button UI (used for dialogs, headers, and inspector actions, arc = 8 or 10).
     */
    public static class RoundedActionButtonUI extends BasicButtonUI {
        private final boolean primary;
        private final int arc;

        public RoundedActionButtonUI() {
            this(false, 8);
        }

        public RoundedActionButtonUI(boolean primary, int arc) {
            this.primary = primary;
            this.arc = arc;
        }

        public static ComponentUI createUI(JComponent c) {
            return new RoundedActionButtonUI(false, 8);
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = c.getWidth();
            int h = c.getHeight();

            if (primary) {
                Color bg = m.isPressed() ? Theme.ACCENT.darker() : (m.isRollover() ? Theme.ACCENT_HOVER : Theme.ACCENT);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
                b.setForeground(Color.WHITE);
            } else {
                Color bg = m.isPressed() ? Theme.ACTIVE_BG : (m.isRollover() ? Theme.HOVER_BG : Theme.CARD_BG);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
                g2.setColor(m.isRollover() ? Theme.ACCENT : Theme.CARD_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, arc, arc));
                b.setForeground(Theme.BLACK);
            }
            g2.dispose();
            super.paint(g, c);
        }
    }

    /**
     * Modern iOS / JetBrains Pill Toggle Switch for State Inspector ("Inicial" & "Final").
     */
    public static class ModernSwitch extends JToggleButton {
        public ModernSwitch(String label) {
            super(label);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setForeground(Theme.BLACK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            setPreferredSize(new Dimension(100, 26));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int sw = 32;
            int sh = 18;
            int sx = 2;
            int sy = (getHeight() - sh) / 2;

            boolean sel = isSelected();
            boolean hover = getModel().isRollover();

            // Pill track
            if (sel) {
                g2.setColor(hover ? Theme.ACCENT_HOVER : Theme.ACCENT);
            } else {
                g2.setColor(hover ? Theme.SCROLL_THUMB_HOVER : Theme.CARD_BORDER);
            }
            g2.fillRoundRect(sx, sy, sw, sh, sh, sh);

            // Thumb circle
            int d = 14;
            int tx = sel ? (sx + sw - d - 2) : (sx + 2);
            int ty = sy + (sh - d) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(tx, ty, d, d);

            // Label text
            g2.setFont(getFont());
            g2.setColor(Theme.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            int textX = sx + sw + 8;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    /**
     * Modern Dark/Light Editable TextField with subtle 1px rounded border and #3574F0 focus highlight.
     */
    public static class ModernTextField extends JTextField {
        private final String placeholder;

        public ModernTextField(String placeholder) {
            super();
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setForeground(Theme.BLACK);
            setCaretColor(Theme.BLACK);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        public void updateTheme() {
            setForeground(Theme.BLACK);
            setCaretColor(Theme.BLACK);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.CARD_BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

            boolean focused = isFocusOwner();
            g2.setColor(focused ? Theme.ACCENT : Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.4f : 1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g2.dispose();

            super.paintComponent(g);

            if ((getText() == null || getText().isEmpty()) && !isFocusOwner() && placeholder != null) {
                Graphics2D gp = (Graphics2D) g.create();
                gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gp.setFont(getFont());
                gp.setColor(Theme.TEXT_MUTED);
                FontMetrics fm = gp.getFontMetrics();
                int py = (h - fm.getHeight()) / 2 + fm.getAscent();
                gp.drawString(placeholder, 8, py);
                gp.dispose();
            }
        }
    }

    /**
     * Layered Canvas Island container:
     * - Places the Automaton JScrollPane at DEFAULT_LAYER
     * - Centers the Floating Island Toolbar pill at PALETTE_LAYER (top-center, y = 14)
     * - Clips/masks outer corners with Theme.ISLAND_BASE_BG (#141517) and draws a 12px rounded border
     */
    public static class IslandCanvasContainer extends JLayeredPane {
        private final JComponent canvasScroll;
        private final JComponent floatingDock;
        private final int arc = 12;

        public IslandCanvasContainer(JComponent canvasScroll, JComponent floatingDock) {
            this.canvasScroll = canvasScroll;
            this.floatingDock = floatingDock;
            setOpaque(false);
            add(canvasScroll, JLayeredPane.DEFAULT_LAYER);
            add(floatingDock, JLayeredPane.PALETTE_LAYER);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();
            canvasScroll.setBounds(1, 1, Math.max(0, w - 2), Math.max(0, h - 2));
            if (floatingDock != null && floatingDock.isVisible()) {
                Dimension d = floatingDock.getPreferredSize();
                int dx = Math.max(8, (w - d.width) / 2);
                int dy = 12;
                floatingDock.setBounds(dx, dy, d.width, d.height);
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Mask outer corners with #141517 so the canvas is a true 12px rounded island
            Area outer = new Area(new Rectangle(0, 0, w, h));
            outer.subtract(new Area(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc)));
            g2.setColor(Theme.ISLAND_BASE_BG);
            g2.fill(outer);

            // Crisp 1px rounded island border (#393B40)
            g2.setColor(Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2f, h - 2f, arc, arc));
            g2.dispose();
        }
    }

    /**
     * Minimalist JetBrains ScrollBarUI: no arrows, flat background (#1E1F22),
     * 6px pill-rounded thumb in #4E5157.
     */
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernScrollBarUI();
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = Theme.SCROLL_THUMB;
            this.trackColor = Theme.SURFACE;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(10, 48)
                    : new Dimension(48, 10);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            Dimension zero = new Dimension(0, 0);
            btn.setPreferredSize(zero);
            btn.setMinimumSize(zero);
            btn.setMaximumSize(zero);
            btn.setFocusable(false);
            btn.setBorder(BorderFactory.createEmptyBorder());
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.SURFACE);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() || isDragging ? Theme.SCROLL_THUMB_HOVER : Theme.SCROLL_THUMB);

            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                int w = 6;
                int x = thumbBounds.x + (thumbBounds.width - w) / 2;
                int y = thumbBounds.y + 2;
                int h = Math.max(12, thumbBounds.height - 4);
                g2.fill(new RoundRectangle2D.Float(x, y, w, h, 6, 6));
            } else {
                int h = 6;
                int x = thumbBounds.x + 2;
                int y = thumbBounds.y + (thumbBounds.height - h) / 2;
                int w = Math.max(12, thumbBounds.width - 4);
                g2.fill(new RoundRectangle2D.Float(x, y, w, h, 6, 6));
            }
            g2.dispose();
        }
    }

    /**
     * Minimalist JetBrains SliderUI: 4px rounded track, #3574F0 active fill, 12px circular anti-aliased thumb.
     */
    public static class ModernSliderUI extends BasicSliderUI {
        public ModernSliderUI(JSlider b) {
            super(b);
        }

        public static ComponentUI createUI(JComponent c) {
            return new ModernSliderUI((JSlider) c);
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            c.setOpaque(false);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(14, 14);
        }

        @Override
        public void paintFocus(Graphics g) {
            // No dotted focus ring
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cy = trackRect.y + trackRect.height / 2;
            int tx = trackRect.x;
            int tw = trackRect.width;
            int th = 4;

            // Background track
            g2.setColor(Theme.CARD_BORDER);
            g2.fill(new RoundRectangle2D.Float(tx, cy - th / 2f, tw, th, 4, 4));

            // Filled active portion up to thumb center
            int fillW = Math.max(0, thumbRect.x + thumbRect.width / 2 - tx);
            g2.setColor(Theme.ACCENT);
            g2.fill(new RoundRectangle2D.Float(tx, cy - th / 2f, fillW, th, 4, 4));
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = 12;
            int x = thumbRect.x + (thumbRect.width - d) / 2;
            int y = thumbRect.y + (thumbRect.height - d) / 2;

            g2.setColor(Theme.ACCENT);
            g2.fillOval(x, y, d, d);
            g2.setColor(Theme.BLACK);
            g2.fillOval(x + 3, y + 3, d - 6, d - 6);
            g2.dispose();
        }
    }

    /**
     * Rounded container panel with smooth 1px border (or hover highlight).
     */
    public static class RoundedPanel extends JPanel {
        private final int arc;
        private Color customBg;
        private Color customBorder;
        private boolean hovered = false;

        public RoundedPanel(LayoutManager layout, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
        }

        public RoundedPanel(int arc) {
            super();
            this.arc = arc;
            setOpaque(false);
        }

        public void setHovered(boolean h) {
            this.hovered = h;
            repaint();
        }

        public void setCustomColors(Color bg, Color border) {
            this.customBg = bg;
            this.customBorder = border;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            Color fill = hovered ? Theme.HOVER_BG : (customBg != null ? customBg : Theme.SURFACE);
            Color stroke = hovered ? Theme.ACCENT : (customBorder != null ? customBorder : Theme.CARD_BORDER);

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc));
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(hovered ? 1.4f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2f, h - 2f, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Subtle JetBrains Badge (#2B2D30 / #1E1F22 fill, #3574F0 text, rounded corners).
     */
    public static class BadgeLabel extends JLabel {
        private final int arc;

        public BadgeLabel(String text, int arc) {
            super(text, JLabel.CENTER);
            this.arc = arc;
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Theme.ACCENT);
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.BADGE_BG);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
            g2.setColor(Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, arc, arc));
            g2.dispose();
            setForeground(Theme.ACCENT);
            super.paintComponent(g);
        }
    }

    /**
     * IntelliJ Project Tree row item: borderless by default, rounded #2E3035 background on hover.
     */
    public static class TreeItemPanel extends JPanel {
        private boolean hovered = false;
        private boolean active = false;

        public TreeItemPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        public void setHovered(boolean h) {
            this.hovered = h;
            repaint();
        }

        public void setActive(boolean a) {
            this.active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? Theme.TOOL_ACTIVE_BG : Theme.HOVER_BG);
                g2.fill(new RoundRectangle2D.Float(2, 1, getWidth() - 4, getHeight() - 2, 8, 8));
                if (active) {
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(3, 6, 3, getHeight() - 12, 3, 3);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Styles a JSplitPane with the minimalist 1px #393B40 divider and #4E5157 hover highlight,
     * completely eliminating the legacy Metal dotted grip.
     */
    public static void styleSplitPane(JSplitPane split) {
        if (split == null) {
            return;
        }
        split.setUI(new ModernSplitPaneUI());
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(5);
        split.setContinuousLayout(true);
        split.setBackground(Theme.SURFACE);
        split.setOpaque(true);
    }

    /**
     * Minimalist JetBrains SplitPaneUI: 1px #393B40 divider line, #4E5157 subtle hover highlight,
     * zero legacy Metal dots or 3D bevels.
     */
    public static class ModernSplitPaneUI extends BasicSplitPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernSplitPaneUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            if (splitPane != null) {
                splitPane.setBorder(BorderFactory.createEmptyBorder());
                splitPane.setDividerSize(5);
                splitPane.setContinuousLayout(true);
                splitPane.setBackground(Theme.SURFACE);
            }
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new ModernSplitPaneDivider(this);
        }
    }

    public static class ModernSplitPaneDivider extends BasicSplitPaneDivider {
        private boolean hovered = false;

        public ModernSplitPaneDivider(BasicSplitPaneUI ui) {
            super(ui);
            setBorder(null);
            setBackground(Theme.SURFACE);
            MouseAdapter hoverAdapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            };
            addMouseListener(hoverAdapter);
        }

        @Override
        public void setBorder(Border border) {
            // Never allow a 3D beveled border on the divider
            super.setBorder(null);
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, w, h);

            int orientation = (splitPane != null) ? splitPane.getOrientation() : JSplitPane.VERTICAL_SPLIT;
            if (orientation == JSplitPane.VERTICAL_SPLIT) {
                int cy = h / 2;
                if (hovered) {
                    g2.setColor(Theme.SCROLL_THUMB);
                    g2.fillRect(0, Math.max(0, cy - 1), w, 3);
                } else {
                    g2.setColor(Theme.CARD_BORDER);
                    g2.drawLine(0, cy, w, cy);
                }
            } else {
                int cx = w / 2;
                if (hovered) {
                    g2.setColor(Theme.SCROLL_THUMB);
                    g2.fillRect(Math.max(0, cx - 1), 0, 3, h);
                } else {
                    g2.setColor(Theme.CARD_BORDER);
                    g2.drawLine(cx, 0, cx, h);
                }
            }
            g2.dispose();
        }
    }

    /**
     * Styles the prominent circular/pill Auto-Run Play/Pause button on the Debugger Media Bar.
     */
    public static void styleMediaPlayButton(AbstractButton b, String tooltip) {
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setUI(new MediaPlayButtonUI());
    }

    public static class MediaPlayButtonUI extends BasicToggleButtonUI {
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = c.getWidth();
            int h = c.getHeight();
            int arc = Math.min(h - 2, 28); // Full pill / circular contour

            Color base = m.isSelected() ? Theme.RUN_GREEN : Theme.ACCENT;
            Color hover = m.isSelected() ? Theme.RUN_GREEN_HOVER : Theme.ACCENT_HOVER;
            Color bg = m.isPressed() ? base.darker() : (m.isRollover() ? hover : base);

            // Subtle outer glow
            g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 55));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc + 2, arc + 2));

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
            g2.dispose();

            b.setForeground(Color.WHITE);
            super.paint(g, c);
        }
    }

    /**
     * Automatically hooks any JDialog (including RuleTable, JOptionPane, About, etc.)
     * during JDialog.dialogInit() (before peer creation) so it is undecorated and
     * receives the coupled dark WindowChrome title bar.
     */
    public static class ModernRootPaneUI extends javax.swing.plaf.basic.BasicRootPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new ModernRootPaneUI();
        }

        @Override
        public void installUI(final JComponent c) {
            super.installUI(c);
            if (!(c instanceof javax.swing.JRootPane)) {
                return;
            }
            final javax.swing.JRootPane root = (javax.swing.JRootPane) c;
            if (Boolean.TRUE.equals(root.getClientProperty("WindowChrome.rootHooked"))) {
                return;
            }
            root.putClientProperty("WindowChrome.rootHooked", Boolean.TRUE);
            root.addHierarchyListener(new java.awt.event.HierarchyListener() {
                @Override
                public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                    java.awt.Container parent = root.getParent();
                    if (parent instanceof javax.swing.JDialog) {
                        javax.swing.JDialog dlg = (javax.swing.JDialog) parent;
                        if (!dlg.isDisplayable()) {
                            try {
                                dlg.setUndecorated(true);
                                AppIcon.apply(dlg);
                            } catch (Throwable ignored) {
                            }
                        } else if (!Boolean.TRUE.equals(root.getClientProperty("WindowChrome.dialogWrapped"))) {
                            Dimension oldSize = dlg.getSize();
                            WindowChrome.installDialog(dlg);
                            if (oldSize != null && oldSize.width > 0 && oldSize.height > 0) {
                                dlg.setSize(oldSize.width, oldSize.height + 34);
                            }
                            dlg.revalidate();
                        }
                    }
                }
            });
        }
    }
}
