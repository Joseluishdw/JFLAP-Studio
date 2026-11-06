package plus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public final class Theme {
    private static final String PREF_KEY = "jbNewUiDark";
    private static final String PREF_GRID = "showGrid";
    private static final String PREF_SNAP = "snapToGrid";
    private static final int GRID_SIZE = 20;

    private static volatile boolean dark = true;
    private static volatile boolean showGrid = true;
    private static volatile boolean snapToGrid = false;

    // Active simulation graph feedback state & transition sets
    private static final Set<Object> activeSimStates = Collections.synchronizedSet(new HashSet<Object>());
    private static final Set<Object> firedTransitions = Collections.synchronizedSet(new HashSet<Object>());

    // JetBrains New UI (Island Dark) Defaults
    public static volatile Color ISLAND_BASE_BG = new Color(20, 21, 23);      // #141517 Deep Island Margin Base
    public static volatile Color SURFACE = new Color(30, 31, 34);             // #1E1F22 Main & Island Sidebars
    public static volatile Color WHITE = new Color(43, 45, 48);               // #2B2D30 Canvas & Floating Dock
    public static volatile Color CARD_BG = new Color(43, 45, 48);             // #2B2D30 Cards & Dropdowns
    public static volatile Color BADGE_BG = new Color(30, 31, 34);            // #1E1F22 Subtle Badge Fill
    public static volatile Color HOVER_BG = new Color(46, 48, 53);            // #2E3035 Ghost Button & Tree Hover
    public static volatile Color ACTIVE_BG = new Color(46, 67, 110);          // #2E436E Active Tool Fill
    public static volatile Color TOOL_ACTIVE_BG = new Color(46, 67, 110);     // #2E436E Alias
    public static volatile Color ACCENT = new Color(53, 116, 240);            // #3574F0 JetBrains Blue
    public static volatile Color ACCENT_HOVER = new Color(70, 130, 250);      // #4682FA Hover Blue
    public static volatile Color RUN_GREEN = new Color(46, 160, 67);          // #2EA043 IDE Run Button Green
    public static volatile Color RUN_GREEN_HOVER = new Color(63, 185, 80);    // #3FB950 IDE Run Button Hover
    public static volatile Color DANGER = new Color(244, 71, 71);             // #F44747 Danger / Reject Red
    public static volatile Color SELECTION_COLOR = new Color(53, 116, 240);   // #3574F0
    public static volatile Color CARD_BORDER = new Color(57, 59, 64);         // #393B40 1px Subtle Borders
    public static volatile Color BORDER = new Color(57, 59, 64);              // #393B40 Alias
    public static volatile Color FLOATING_BORDER = new Color(67, 69, 74);     // #43454A Floating Dock & Popup Border
    public static volatile Color BLACK = new Color(223, 225, 229);            // #DFE1E5 Primary Text
    public static volatile Color TEXT_MUTED = new Color(134, 138, 145);       // #868A91 Secondary Text
    public static volatile Color SCROLL_THUMB = new Color(78, 81, 87);        // #4E5157 Minimalist Thumb
    public static volatile Color SCROLL_THUMB_HOVER = new Color(106, 110, 118);
    public static volatile Color GRID_COLOR = new Color(62, 65, 73);          // Dot Grid Minor
    public static volatile Color STATE_FILL = new Color(30, 31, 34);          // #1E1F22 Modern State Node Fill
    public static volatile Color STATE_BORDER = new Color(53, 116, 240);      // #3574F0 2px State Ring
    public static volatile Color LABEL_PILL_BG = new Color(30, 31, 34, 230);  // #1E1F22 90% Opacity Unified Container
    public static volatile Color INITIAL_BADGE = new Color(87, 150, 92);      // #57965C JetBrains Green
    public static volatile Color FINAL_RING = new Color(53, 116, 240);        // #3574F0
    public static volatile Color ACTIVITY_BAR_BG = new Color(30, 31, 34);     // #1E1F22
    public static volatile Color STATUS_BAR_BG = new Color(30, 31, 34);       // #1E1F22 IntelliJ Status Bar

    // Turing Tape & Simulation Colors
    public static volatile Color TAPE_CELL_BG = new Color(38, 40, 46);        // #26282E Card Cell Background
    public static volatile Color TAPE_BLANK_FG = new Color(90, 93, 99);       // #5A5D63 Muted Blank Symbol (□)
    public static volatile Color TAPE_HEAD_FILL = new Color(53, 116, 240, 51);// rgba(53, 116, 240, 0.2) Playhead Fill
    public static volatile Color FIRED_CYAN = new Color(76, 194, 255);        // #4CC2FF Active Fired Transition Cyan
    public static volatile Color FIRED_GREEN = new Color(60, 208, 112);       // #3CD070 Active Fired Transition Green

    public static final Font MONO_FONT;
    public static final Font TAPE_FONT;
    public static final Font FONT_PLAIN = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    static {
        Font resolvedMono = new Font("Monospaced", Font.BOLD, 12);
        Font resolvedTape = new Font("Monospaced", Font.BOLD, 16);
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            Preferences prefs = Preferences.userNodeForPackage(Theme.class);
            showGrid = prefs.getBoolean(PREF_GRID, true);
            snapToGrid = prefs.getBoolean(PREF_SNAP, false);

            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Set<String> set = new HashSet<String>(Arrays.asList(families));
            if (set.contains("JetBrains Mono")) {
                resolvedMono = new Font("JetBrains Mono", Font.BOLD, 12);
                resolvedTape = new Font("JetBrains Mono", Font.BOLD, 16);
            } else if (set.contains("Fira Code")) {
                resolvedMono = new Font("Fira Code", Font.BOLD, 12);
                resolvedTape = new Font("Fira Code", Font.BOLD, 16);
            } else if (set.contains("Consolas")) {
                resolvedMono = new Font("Consolas", Font.BOLD, 12);
                resolvedTape = new Font("Consolas", Font.BOLD, 16);
            }
        } catch (Throwable ignored) {
        }
        MONO_FONT = resolvedMono;
        TAPE_FONT = resolvedTape;
    }

    private Theme() {
    }

    public static void setActiveSimStates(Collection<?> states) {
        activeSimStates.clear();
        if (states != null) {
            activeSimStates.addAll(states);
        }
    }

    public static void clearActiveSimStates() {
        activeSimStates.clear();
    }

    public static boolean isActiveSimState(Object state) {
        return state != null && activeSimStates.contains(state);
    }

    public static void setFiredTransitions(Collection<?> transitions) {
        firedTransitions.clear();
        if (transitions != null) {
            firedTransitions.addAll(transitions);
        }
    }

    public static void clearFiredTransitions() {
        firedTransitions.clear();
    }

    public static boolean isTransitionFired(Object transition) {
        return transition != null && firedTransitions.contains(transition);
    }

    public static boolean isDark() {
        return dark;
    }

    public static boolean savedPreference() {
        try {
            return Preferences.userNodeForPackage(Theme.class).getBoolean(PREF_KEY, true);
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean isShowGrid() {
        return showGrid;
    }

    public static void setShowGrid(boolean show) {
        showGrid = show;
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean(PREF_GRID, show);
        } catch (Throwable ignored) {
        }
        repaintAllCanvases();
    }

    public static void toggleGrid() {
        setShowGrid(!showGrid);
    }

    public static boolean isSnapToGrid() {
        return snapToGrid;
    }

    public static void setSnapToGrid(boolean snap) {
        snapToGrid = snap;
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean(PREF_SNAP, snap);
        } catch (Throwable ignored) {
        }
        repaintAllCanvases();
    }

    public static void toggleSnapToGrid() {
        setSnapToGrid(!snapToGrid);
    }

    public static int getGridSize() {
        return GRID_SIZE;
    }

    public static int snap(int value) {
        if (!snapToGrid) {
            return value;
        }
        return Math.round((float) value / GRID_SIZE) * GRID_SIZE;
    }

    public static void repaintAllCanvases() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing()) {
                w.repaint();
            }
        }
    }

    public static void apply(boolean isDarkMode, boolean updateWindows) {
        dark = isDarkMode;
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean(PREF_KEY, isDarkMode);
        } catch (Throwable ignored) {
        }

        if (isDarkMode) {
            // JetBrains New UI — Island Dark Palette
            ISLAND_BASE_BG = new Color(20, 21, 23);    // #141517 Deep Island Margin
            SURFACE = new Color(30, 31, 34);           // #1E1F22 Main & Sidebars
            WHITE = new Color(43, 45, 48);             // #2B2D30 Canvas
            CARD_BG = new Color(43, 45, 48);           // #2B2D30 Lists, Cards & Popups
            BADGE_BG = new Color(30, 31, 34);          // #1E1F22 Subtle Badge
            HOVER_BG = new Color(46, 48, 53);          // #2E3035 Hover
            ACTIVE_BG = new Color(46, 67, 110);        // #2E436E Active Tool
            TOOL_ACTIVE_BG = ACTIVE_BG;
            ACCENT = new Color(53, 116, 240);          // #3574F0 JetBrains Blue
            ACCENT_HOVER = new Color(70, 130, 250);
            RUN_GREEN = new Color(46, 160, 67);        // #2EA043
            RUN_GREEN_HOVER = new Color(63, 185, 80);  // #3FB950
            DANGER = new Color(244, 71, 71);           // #F44747
            SELECTION_COLOR = new Color(53, 116, 240);
            CARD_BORDER = new Color(57, 59, 64);       // #393B40 1px Border
            BORDER = CARD_BORDER;
            FLOATING_BORDER = new Color(67, 69, 74);   // #43454A Floating Dock & Popup Border
            BLACK = new Color(223, 225, 229);          // #DFE1E5 Primary Text
            TEXT_MUTED = new Color(134, 138, 145);     // #868A91 Muted Text
            SCROLL_THUMB = new Color(78, 81, 87);      // #4E5157
            SCROLL_THUMB_HOVER = new Color(106, 110, 118);
            GRID_COLOR = new Color(62, 65, 73);        // Dot Grid
            STATE_FILL = new Color(30, 31, 34);        // #1E1F22 Modern Dark State
            STATE_BORDER = new Color(53, 116, 240);    // #3574F0 2px Ring
            LABEL_PILL_BG = new Color(30, 31, 34, 230);// #1E1F22 90% Opacity Unified Container
            INITIAL_BADGE = new Color(87, 150, 92);    // #57965C
            FINAL_RING = new Color(53, 116, 240);
            ACTIVITY_BAR_BG = new Color(30, 31, 34);
            STATUS_BAR_BG = new Color(30, 31, 34);     // #1E1F22 IntelliJ Status Bar
            TAPE_CELL_BG = new Color(38, 40, 46);      // #26282E Card Cell Background
            TAPE_BLANK_FG = new Color(90, 93, 99);     // #5A5D63 Muted Blank Symbol (□)
            TAPE_HEAD_FILL = new Color(53, 116, 240, 51);
            FIRED_CYAN = new Color(76, 194, 255);      // #4CC2FF
            FIRED_GREEN = new Color(60, 208, 112);     // #3CD070
            MetalLookAndFeel.setCurrentTheme(new DarkMetal());
        } else {
            // JetBrains New UI — Island Light Palette
            ISLAND_BASE_BG = new Color(225, 228, 234); // #E1E4EA Island Margin
            SURFACE = new Color(247, 248, 250);        // #F7F8FA
            WHITE = new Color(255, 255, 255);          // #FFFFFF Canvas
            CARD_BG = new Color(255, 255, 255);        // #FFFFFF Cards
            BADGE_BG = new Color(237, 241, 247);       // #EDF1F7
            HOVER_BG = new Color(235, 236, 240);       // #EBECF0
            ACTIVE_BG = new Color(212, 226, 255);      // #D4E2FF
            TOOL_ACTIVE_BG = ACTIVE_BG;
            ACCENT = new Color(53, 116, 240);          // #3574F0
            ACCENT_HOVER = new Color(40, 98, 214);
            RUN_GREEN = new Color(46, 160, 67);
            RUN_GREEN_HOVER = new Color(38, 138, 57);
            DANGER = new Color(217, 48, 37);
            SELECTION_COLOR = new Color(53, 116, 240);
            CARD_BORDER = new Color(218, 220, 226);    // #DADCE2
            BORDER = CARD_BORDER;
            FLOATING_BORDER = new Color(198, 202, 210);
            BLACK = new Color(43, 45, 48);             // #2B2D30
            TEXT_MUTED = new Color(108, 112, 126);     // #6C707E
            SCROLL_THUMB = new Color(194, 198, 206);
            SCROLL_THUMB_HOVER = new Color(157, 163, 174);
            GRID_COLOR = new Color(210, 214, 222);
            STATE_FILL = new Color(242, 246, 255);
            STATE_BORDER = new Color(53, 116, 240);
            LABEL_PILL_BG = new Color(255, 255, 255, 235);
            INITIAL_BADGE = new Color(54, 150, 76);
            FINAL_RING = new Color(53, 116, 240);
            ACTIVITY_BAR_BG = new Color(247, 248, 250);
            STATUS_BAR_BG = new Color(247, 248, 250);
            TAPE_CELL_BG = new Color(255, 255, 255);
            TAPE_BLANK_FG = new Color(165, 170, 182);
            TAPE_HEAD_FILL = new Color(53, 116, 240, 40);
            FIRED_CYAN = new Color(14, 148, 214);
            FIRED_GREEN = new Color(38, 166, 91);
            MetalLookAndFeel.setCurrentTheme(new LightMetal());
        }

        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Throwable ignored) {
        }

        // Register custom JetBrains UI delegates globally
        UIManager.put("RootPaneUI", JetBrainsUI.ModernRootPaneUI.class.getName());
        UIManager.put("ScrollBarUI", JetBrainsUI.ModernScrollBarUI.class.getName());
        UIManager.put("SliderUI", JetBrainsUI.ModernSliderUI.class.getName());
        UIManager.put("TabbedPaneUI", JetBrainsUI.ModernTabbedPaneUI.class.getName());
        UIManager.put("SplitPaneUI", JetBrainsUI.ModernSplitPaneUI.class.getName());
        UIManager.put("MenuBarUI", JetBrainsUI.ModernMenuBarUI.class.getName());
        UIManager.put("MenuUI", JetBrainsUI.ModernMenuUI.class.getName());
        UIManager.put("PopupMenuUI", JetBrainsUI.ModernPopupMenuUI.class.getName());
        UIManager.put("MenuItemUI", JetBrainsUI.ModernMenuItemUI.class.getName());
        UIManager.put("CheckBoxMenuItemUI", JetBrainsUI.ModernCheckBoxMenuItemUI.class.getName());
        UIManager.put("ScrollBar.width", Integer.valueOf(10));
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());

        ColorUIResource bg = new ColorUIResource(SURFACE);
        ColorUIResource fg = new ColorUIResource(BLACK);
        ColorUIResource fieldBg = new ColorUIResource(CARD_BG);
        ColorUIResource selBg = new ColorUIResource(ACTIVE_BG);
        ColorUIResource selFg = new ColorUIResource(BLACK);
        ColorUIResource grid = new ColorUIResource(CARD_BORDER);

        FontUIResource fontPlain = new FontUIResource(FONT_PLAIN);
        FontUIResource fontBold = new FontUIResource(FONT_BOLD);

        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = UIManager.get(key);
            if (val instanceof FontUIResource) {
                String k = key.toString();
                if (k.contains("Bold") || k.startsWith("TitledBorder")) {
                    UIManager.put(key, fontBold);
                } else {
                    UIManager.put(key, fontPlain);
                }
            }
        }

        String[] panelKeys = {
            "Panel", "Viewport", "ScrollPane", "SplitPane", "TabbedPane",
            "ToolBar", "MenuBar", "Menu", "OptionPane", "Label",
            "CheckBox", "RadioButton", "ToggleButton", "Button", "Slider",
            "TitledBorder", "RootPane"
        };
        for (String k : panelKeys) {
            UIManager.put(k + ".background", bg);
            UIManager.put(k + ".foreground", fg);
        }

        String[] menuPopupKeys = {
            "PopupMenu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"
        };
        for (String k : menuPopupKeys) {
            UIManager.put(k + ".background", fieldBg);
            UIManager.put(k + ".foreground", fg);
            UIManager.put(k + ".selectionBackground", selBg);
            UIManager.put(k + ".selectionForeground", selFg);
            UIManager.put(k + ".borderPainted", Boolean.FALSE);
        }
        UIManager.put("Menu.selectionBackground", new ColorUIResource(HOVER_BG));
        UIManager.put("Menu.selectionForeground", fg);
        UIManager.put("Menu.borderPainted", Boolean.FALSE);

        String[] inputKeys = {
            "TextField", "TextArea", "TextPane", "EditorPane",
            "FormattedTextField", "PasswordField", "Table", "List",
            "ComboBox", "Tree"
        };
        for (String k : inputKeys) {
            UIManager.put(k + ".background", fieldBg);
            UIManager.put(k + ".foreground", fg);
            UIManager.put(k + ".caretForeground", fg);
            UIManager.put(k + ".selectionBackground", new ColorUIResource(ACCENT));
            UIManager.put(k + ".selectionForeground", new ColorUIResource(Color.WHITE));
        }

        // Remove 3D beveled borders everywhere
        UIManager.put("MenuBar.border", BorderFactory.createEmptyBorder(0, 6, 0, 6));
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FLOATING_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 2, 4, 2)));
        UIManager.put("Separator.foreground", new ColorUIResource(FLOATING_BORDER));
        UIManager.put("Separator.background", fieldBg);
        UIManager.put("PopupMenuSeparator.margin", new Insets(3, 8, 3, 8));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        UIManager.put("Table.gridColor", grid);
        UIManager.put("TableHeader.background", bg);
        UIManager.put("TableHeader.foreground", fg);
        UIManager.put("ToolTip.background", new ColorUIResource(CARD_BG));
        UIManager.put("ToolTip.foreground", new ColorUIResource(BLACK));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FLOATING_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        UIManager.put("Button.select", new ColorUIResource(ACTIVE_BG));
        UIManager.put("ToggleButton.select", new ColorUIResource(ACTIVE_BG));
        UIManager.put("Button.margin", new Insets(5, 12, 5, 12));
        UIManager.put("ToggleButton.margin", new Insets(5, 12, 5, 12));
        UIManager.put("TabbedPane.selected", bg);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(0, 8, 0, 8));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(SCROLL_THUMB));
        UIManager.put("ScrollBar.track", bg);

        if (updateWindows) {
            try {
                gui.action.NewAction.refreshThemeIfShowing();
            } catch (Throwable ignored) {
            }
            for (Window w : Window.getWindows()) {
                AppIcon.apply(w);
                SwingUtilities.updateComponentTreeUI(w);
                if (w instanceof gui.environment.EnvironmentFrame) {
                    ((gui.environment.EnvironmentFrame) w).refreshProjectTabs();
                }
                if (w instanceof JFrame) {
                    WindowChrome.installFrame((JFrame) w, null);
                    JMenuBar mb = ((JFrame) w).getJMenuBar();
                    if (mb != null) {
                        JetBrainsUI.styleMenuBar(mb);
                    }
                }
                recolorTree(w);
                w.repaint();
            }
        }
    }

    public static void recolorTree(Component c) {
        if (c instanceof JScrollBar) {
            ((JScrollBar) c).setUI(new JetBrainsUI.ModernScrollBarUI());
        } else if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            if (sp.getVerticalScrollBar() != null) {
                sp.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            }
            if (sp.getHorizontalScrollBar() != null) {
                sp.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            }
        } else if (c instanceof JSlider) {
            ((JSlider) c).setUI(new JetBrainsUI.ModernSliderUI((JSlider) c));
        } else if (c instanceof JSplitPane) {
            JetBrainsUI.styleSplitPane((JSplitPane) c);
        } else if (c instanceof JTabbedPane) {
            ((JTabbedPane) c).setUI(new JetBrainsUI.ModernTabbedPaneUI());
            c.setBackground(SURFACE);
        } else if (c instanceof JMenuBar) {
            JetBrainsUI.styleMenuBar((JMenuBar) c);
        } else if (c instanceof gui.editor.EditorPane) {
            ((gui.editor.EditorPane) c).applyThemeColors();
        } else if (c instanceof gui.editor.ToolBar) {
            ((gui.editor.ToolBar) c).reapplyGhostStyles();
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                recolorTree(child);
            }
        }
    }

    public static void toggle() {
        apply(!dark, true);
    }

    private static final class DarkMetal extends DefaultMetalTheme {
        private final ColorUIResource p1 = new ColorUIResource(53, 116, 240);
        private final ColorUIResource p2 = new ColorUIResource(46, 67, 110);
        private final ColorUIResource p3 = new ColorUIResource(70, 130, 250);
        private final ColorUIResource s1 = new ColorUIResource(57, 59, 64);
        private final ColorUIResource s2 = new ColorUIResource(43, 45, 48);
        private final ColorUIResource s3 = new ColorUIResource(30, 31, 34);
        private final ColorUIResource w = new ColorUIResource(43, 45, 48);
        private final ColorUIResource b = new ColorUIResource(223, 225, 229);
        private final FontUIResource fontPlain = new FontUIResource("Segoe UI", Font.PLAIN, 13);
        private final FontUIResource fontBold = new FontUIResource("Segoe UI", Font.BOLD, 13);

        @Override
        protected ColorUIResource getPrimary1() { return p1; }
        @Override
        protected ColorUIResource getPrimary2() { return p2; }
        @Override
        protected ColorUIResource getPrimary3() { return p3; }
        @Override
        protected ColorUIResource getSecondary1() { return s1; }
        @Override
        protected ColorUIResource getSecondary2() { return s2; }
        @Override
        protected ColorUIResource getSecondary3() { return s3; }
        @Override
        protected ColorUIResource getWhite() { return w; }
        @Override
        protected ColorUIResource getBlack() { return b; }
        @Override
        public FontUIResource getControlTextFont() { return fontPlain; }
        @Override
        public FontUIResource getSystemTextFont() { return fontPlain; }
        @Override
        public FontUIResource getUserTextFont() { return fontPlain; }
        @Override
        public FontUIResource getMenuTextFont() { return fontPlain; }
        @Override
        public FontUIResource getWindowTitleFont() { return fontBold; }
        @Override
        public FontUIResource getSubTextFont() { return fontPlain; }
    }

    private static final class LightMetal extends DefaultMetalTheme {
        private final ColorUIResource p1 = new ColorUIResource(53, 116, 240);
        private final ColorUIResource p2 = new ColorUIResource(212, 226, 255);
        private final ColorUIResource p3 = new ColorUIResource(235, 236, 240);
        private final ColorUIResource s1 = new ColorUIResource(218, 220, 226);
        private final ColorUIResource s2 = new ColorUIResource(235, 236, 240);
        private final ColorUIResource s3 = new ColorUIResource(247, 248, 250);
        private final ColorUIResource w = new ColorUIResource(255, 255, 255);
        private final ColorUIResource b = new ColorUIResource(43, 45, 48);
        private final FontUIResource fontPlain = new FontUIResource("Segoe UI", Font.PLAIN, 13);
        private final FontUIResource fontBold = new FontUIResource("Segoe UI", Font.BOLD, 13);

        @Override
        protected ColorUIResource getPrimary1() { return p1; }
        @Override
        protected ColorUIResource getPrimary2() { return p2; }
        @Override
        protected ColorUIResource getPrimary3() { return p3; }
        @Override
        protected ColorUIResource getSecondary1() { return s1; }
        @Override
        protected ColorUIResource getSecondary2() { return s2; }
        @Override
        protected ColorUIResource getSecondary3() { return s3; }
        @Override
        protected ColorUIResource getWhite() { return w; }
        @Override
        protected ColorUIResource getBlack() { return b; }
        @Override
        public FontUIResource getControlTextFont() { return fontPlain; }
        @Override
        public FontUIResource getSystemTextFont() { return fontPlain; }
        @Override
        public FontUIResource getUserTextFont() { return fontPlain; }
        @Override
        public FontUIResource getMenuTextFont() { return fontPlain; }
        @Override
        public FontUIResource getWindowTitleFont() { return fontBold; }
        @Override
        public FontUIResource getSubTextFont() { return fontPlain; }
    }
}
