package plus;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Unified Dark IDE Window Chrome (Antigravity / VS Code / JetBrains New UI style).
 * Eliminates the native white OS titlebar and couples:
 * [App Icon] + [Menu Bar] + [Draggable Title Area] + [IDE Controls] + [ —  □  × ]
 * into a single dark header bar at the top of the window, with 8-way edge/corner resizing.
 */
public final class WindowChrome {
    public static final String NAME_APP_ICON = "CHROME_APP_ICON";
    public static final String NAME_TITLE_DRAG = "CHROME_TITLE_DRAG";
    public static final String NAME_WINDOW_CONTROLS = "CHROME_WINDOW_CONTROLS";

    private static final String PROP_ON_CLOSE = "WindowChrome.onClose";
    private static final String PROP_MAXIMIZED = "WindowChrome.maximized";
    private static final String PROP_NORMAL_BOUNDS = "WindowChrome.normalBounds";
    private static final String PROP_RESIZE_INSTALLED = "WindowChrome.resizeInstalled";
    private static final String PROP_DIALOG_WRAPPED = "WindowChrome.dialogWrapped";

    private static final Color CLOSE_HOVER_BG = new Color(0xC4, 0x2B, 0x1C);
    private static final int RESIZE_MARGIN = 6;

    private WindowChrome() {
    }

    /**
     * Configures a JFrame to be undecorated with a 1px dark border, 8-way resizing,
     * and custom close callback. Must be called before pack() / setVisible(true).
     */
    public static void installFrame(final JFrame frame, final Runnable onClose) {
        if (frame == null) {
            return;
        }
        if (!frame.isDisplayable()) {
            try {
                frame.setUndecorated(true);
            } catch (Throwable ignored) {
            }
        }
        JRootPane root = frame.getRootPane();
        if (root != null) {
            if (onClose != null) {
                root.putClientProperty(PROP_ON_CLOSE, onClose);
            }
            updateRootBorder(frame);
            if (!Boolean.TRUE.equals(root.getClientProperty(PROP_RESIZE_INSTALLED))) {
                root.putClientProperty(PROP_RESIZE_INSTALLED, Boolean.TRUE);
                installResizeHandler(frame);
            }
        }
    }

    /**
     * Configures a JDialog to be undecorated and wraps its contentPane with a sleek
     * coupled 34px dark IDE titlebar ([AppIcon] + Title + [ × ]) and 8-way resizing.
     * Call after setting contentPane and before pack() / setVisible(true).
     */
    public static void installDialog(final JDialog dialog) {
        if (dialog == null) {
            return;
        }
        if (!dialog.isDisplayable()) {
            try {
                dialog.setUndecorated(true);
            } catch (Throwable ignored) {
            }
        }
        JRootPane root = dialog.getRootPane();
        if (root == null) {
            return;
        }
        root.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1));
        if (!Boolean.TRUE.equals(root.getClientProperty(PROP_RESIZE_INSTALLED))) {
            root.putClientProperty(PROP_RESIZE_INSTALLED, Boolean.TRUE);
            installResizeHandler(dialog);
        }
        if (Boolean.TRUE.equals(root.getClientProperty(PROP_DIALOG_WRAPPED))) {
            return;
        }
        root.putClientProperty(PROP_DIALOG_WRAPPED, Boolean.TRUE);

        Container oldContent = dialog.getContentPane();
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(Theme.SURFACE);

        JPanel headerBar = new JPanel(new BorderLayout(0, 0));
        headerBar.setBackground(Theme.ISLAND_BASE_BG);
        headerBar.setPreferredSize(new Dimension(100, 34));
        headerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));

        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
        leftBrand.setOpaque(false);
        JLabel iconLbl = new JLabel(AppIcon.getImageIcon(16));
        final JLabel titleLbl = new JLabel(dialog.getTitle() != null ? dialog.getTitle() : "JFLAP Studio");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(Theme.BLACK);
        leftBrand.add(iconLbl);
        leftBrand.add(titleLbl);
        headerBar.add(leftBrand, BorderLayout.WEST);

        JComponent dragSpacer = new JComponent() {
        };
        dragSpacer.setOpaque(false);
        headerBar.add(dragSpacer, BorderLayout.CENTER);

        installWindowDrag(headerBar, dialog, true);
        installWindowDrag(leftBrand, dialog, true);
        installWindowDrag(dragSpacer, dialog, true);

        JPanel controls = new JPanel(new GridLayout(1, 1, 0, 0));
        controls.setOpaque(false);
        JButton closeBtn = createWindowControlButton(2, dialog, new Runnable() {
            @Override
            public void run() {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                dialog.dispose();
            }
        });
        controls.add(closeBtn);
        headerBar.add(controls, BorderLayout.EAST);

        wrapper.add(headerBar, BorderLayout.NORTH);
        wrapper.add(oldContent, BorderLayout.CENTER);
        dialog.setContentPane(wrapper);
    }

    /**
     * Integrates the App Icon (left), Draggable Title Region (center), and Window Controls
     * ([ — ] [ □ ] [ × ] on the far right) directly into the JMenuBar, coupling the menu bar
     * and window title bar into a single Antigravity/VS Code-style header bar.
     */
    public static void integrateMenuBar(final JMenuBar menuBar) {
        if (menuBar == null) {
            return;
        }
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));
        menuBar.setPreferredSize(new Dimension(menuBar.getPreferredSize().width, 36));
        if (!Boolean.TRUE.equals(menuBar.getClientProperty("WindowChrome.dragInstalled"))) {
            menuBar.putClientProperty("WindowChrome.dragInstalled", Boolean.TRUE);
            installLazyFrameDrag(menuBar);
        }

        Component existingIcon = null;
        Component existingTitleDrag = null;
        Component existingIdeHeader = null;
        Component existingWinControls = null;

        for (int i = menuBar.getComponentCount() - 1; i >= 0; i--) {
            Component c = menuBar.getComponent(i);
            String name = c.getName();
            if (NAME_APP_ICON.equals(name)) {
                existingIcon = c;
                menuBar.remove(i);
            } else if (NAME_TITLE_DRAG.equals(name)) {
                existingTitleDrag = c;
                menuBar.remove(i);
            } else if ("IDE_HEADER_PANEL".equals(name)) {
                existingIdeHeader = c;
                menuBar.remove(i);
            } else if (NAME_WINDOW_CONTROLS.equals(name)) {
                existingWinControls = c;
                menuBar.remove(i);
            }
        }

        // 1. Far-left App Icon (18x18)
        if (existingIcon == null) {
            JLabel appIconLabel = new JLabel(AppIcon.getImageIcon(18));
            appIconLabel.setName(NAME_APP_ICON);
            appIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
            appIconLabel.setToolTipText("JFLAP Studio");
            installLazyFrameDrag(appIconLabel);
            existingIcon = appIconLabel;
        }
        menuBar.add(existingIcon, 0);

        // 2. Center Flexible Draggable Title Area (placed right after JMenus)
        if (existingTitleDrag == null) {
            JComponent titleDrag = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(120, 34);
                }

                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(24, 34);
                }

                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Short.MAX_VALUE, 36);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (!(w instanceof JFrame)) {
                        return;
                    }
                    String title = ((JFrame) w).getTitle();
                    if (title == null || title.isEmpty()) {
                        return;
                    }
                    int wPx = getWidth();
                    if (wPx < 70) {
                        return;
                    }
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2.setColor(Theme.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    String display = clipText(title, fm, wPx - 20);
                    if (!display.isEmpty()) {
                        int tx = Math.max(8, (wPx - fm.stringWidth(display)) / 2);
                        int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(display, tx, ty);
                    }
                    g2.dispose();
                }
            };
            titleDrag.setName(NAME_TITLE_DRAG);
            titleDrag.setOpaque(false);
            installLazyFrameDrag(titleDrag);
            existingTitleDrag = titleDrag;
        }
        menuBar.add(existingTitleDrag);

        // 3. Re-add IDE_HEADER_PANEL (if present) before window control buttons
        if (existingIdeHeader != null) {
            menuBar.add(existingIdeHeader);
        }

        // 4. Far-right Window Control Buttons: [ — ] [ □ ] [ × ]
        if (existingWinControls == null) {
            JPanel winControls = new JPanel(new GridLayout(1, 3, 0, 0));
            winControls.setName(NAME_WINDOW_CONTROLS);
            winControls.setOpaque(false);
            winControls.setPreferredSize(new Dimension(126, 34));
            winControls.setMinimumSize(new Dimension(126, 34));
            winControls.setMaximumSize(new Dimension(126, 36));

            JButton minBtn = createLazyFrameControlButton(0, "Minimizar");
            JButton maxBtn = createLazyFrameControlButton(1, "Maximizar / Restaurar");
            JButton closeBtn = createLazyFrameControlButton(2, "Cerrar");

            winControls.add(minBtn);
            winControls.add(maxBtn);
            winControls.add(closeBtn);
            existingWinControls = winControls;
        }
        menuBar.add(existingWinControls);

        installCoupledHeaderLayout(menuBar);
        menuBar.revalidate();
        menuBar.repaint();
    }

    /**
     * Custom LayoutManager for the coupled JMenuBar header that guarantees:
     * - App Icon + JMenus are placed on the left
     * - Window controls ([ — ] [ □ ] [ × ]) are ALWAYS pinned flush at the far-right edge
     * - IDE_HEADER_PANEL (if present) sits immediately to the left of the window controls
     * - CHROME_TITLE_DRAG fills the exact space between the menus and right controls
     */
    private static void installCoupledHeaderLayout(final JMenuBar menuBar) {
        menuBar.setLayout(new java.awt.LayoutManager() {
            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                int w = 0;
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    Component c = parent.getComponent(i);
                    if (c.isVisible() && !NAME_TITLE_DRAG.equals(c.getName())) {
                        Dimension d = c.getPreferredSize();
                        if (d != null) {
                            w += d.width;
                        }
                    }
                }
                return new Dimension(Math.max(w + 40, 640), 36);
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return new Dimension(480, 36);
            }

            @Override
            public void layoutContainer(Container parent) {
                int totalW = parent.getWidth();
                int totalH = parent.getHeight();
                int innerH = Math.max(1, totalH - 1); // leave 1px bottom border visible

                Component winControls = null;
                Component ideHeader = null;
                Component titleDrag = null;

                int leftX = 2;
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    Component c = parent.getComponent(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    String name = c.getName();
                    if (NAME_WINDOW_CONTROLS.equals(name)) {
                        winControls = c;
                    } else if ("IDE_HEADER_PANEL".equals(name)) {
                        ideHeader = c;
                    } else if (NAME_TITLE_DRAG.equals(name)) {
                        titleDrag = c;
                    } else {
                        Dimension d = c.getPreferredSize();
                        int cw = (d != null) ? d.width : 50;
                        c.setBounds(leftX, 0, cw, innerH);
                        leftX += cw;
                    }
                }

                int rightX = totalW;
                if (winControls != null) {
                    int wcW = 126;
                    rightX -= wcW;
                    winControls.setBounds(rightX, 0, wcW, innerH);
                }

                if (ideHeader != null) {
                    Dimension d = ideHeader.getPreferredSize();
                    int ideW = (d != null) ? d.width : 300;
                    int maxAvailable = Math.max(0, rightX - leftX - 8);
                    int actualIdeW = Math.min(ideW, maxAvailable);
                    rightX -= (actualIdeW + 4);
                    int ideY = Math.max(0, (innerH - (d != null ? d.height : innerH)) / 2);
                    int ideH = (d != null) ? Math.min(innerH, d.height) : innerH;
                    ideHeader.setBounds(rightX, ideY, actualIdeW, ideH);
                }

                if (titleDrag != null) {
                    int dragW = Math.max(0, rightX - leftX);
                    titleDrag.setBounds(leftX, 0, dragW, innerH);
                }
            }
        });
    }

    private static String clipText(String text, FontMetrics fm, int maxWidth) {
        if (maxWidth <= 20) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellW = fm.stringWidth(ellipsis);
        for (int i = text.length() - 1; i > 0; i--) {
            if (fm.stringWidth(text.substring(0, i)) + ellW <= maxWidth) {
                return text.substring(0, i) + ellipsis;
            }
        }
        return "";
    }

    public static boolean isMaximized(JFrame frame) {
        if (frame == null) {
            return false;
        }
        if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            return true;
        }
        JRootPane root = frame.getRootPane();
        return root != null && Boolean.TRUE.equals(root.getClientProperty(PROP_MAXIMIZED));
    }

    public static void toggleMaximize(JFrame frame) {
        if (frame == null || !frame.isResizable()) {
            return;
        }
        JRootPane root = frame.getRootPane();
        if (root == null) {
            return;
        }
        if (isMaximized(frame)) {
            root.putClientProperty(PROP_MAXIMIZED, Boolean.FALSE);
            frame.setExtendedState(Frame.NORMAL);
            Object saved = root.getClientProperty(PROP_NORMAL_BOUNDS);
            if (saved instanceof Rectangle) {
                frame.setBounds((Rectangle) saved);
            } else {
                frame.setSize(1080, 720);
                frame.setLocationRelativeTo(null);
            }
        } else {
            root.putClientProperty(PROP_NORMAL_BOUNDS, frame.getBounds());
            root.putClientProperty(PROP_MAXIMIZED, Boolean.TRUE);
            Rectangle usable = getUsableScreenBounds(frame);
            frame.setBounds(usable);
        }
        updateRootBorder(frame);
        frame.revalidate();
        frame.repaint();
    }

    private static Rectangle getUsableScreenBounds(Window window) {
        try {
            GraphicsConfiguration gc = window != null ? window.getGraphicsConfiguration() : null;
            if (gc != null) {
                Rectangle bounds = gc.getBounds();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                return new Rectangle(
                        bounds.x + insets.left,
                        bounds.y + insets.top,
                        bounds.width - insets.left - insets.right,
                        bounds.height - insets.top - insets.bottom);
            }
        } catch (Throwable ignored) {
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    private static void updateRootBorder(JFrame frame) {
        if (frame == null || frame.getRootPane() == null) {
            return;
        }
        if (isMaximized(frame)) {
            frame.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        } else {
            frame.getRootPane().setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1));
        }
    }

    private static void triggerFrameClose(JFrame frame) {
        if (frame == null) {
            return;
        }
        JRootPane root = frame.getRootPane();
        if (root != null) {
            Object cb = root.getClientProperty(PROP_ON_CLOSE);
            if (cb instanceof Runnable) {
                ((Runnable) cb).run();
                return;
            }
        }
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    private static JButton createLazyFrameControlButton(final int type, String tooltip) {
        final JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isArmed() && getModel().isPressed();

                if (hover || pressed) {
                    if (type == 2) {
                        g2.setColor(pressed ? CLOSE_HOVER_BG.darker() : CLOSE_HOVER_BG);
                    } else {
                        g2.setColor(pressed ? Theme.ACTIVE_BG : Theme.HOVER_BG);
                    }
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                Color iconColor = (type == 2 && (hover || pressed)) ? Color.WHITE : Theme.BLACK;
                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(1.2f));

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if (type == 0) {
                    // Minimize: crisp horizontal bar
                    g2.drawLine(cx - 5, cy, cx + 5, cy);
                } else if (type == 1) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    boolean max = (w instanceof JFrame) && isMaximized((JFrame) w);
                    if (max) {
                        // Restore: two overlapping squares
                        g2.drawRect(cx - 3, cy - 5, 8, 8);
                        g2.setColor((hover || pressed) ? Theme.HOVER_BG : Theme.SURFACE);
                        g2.fillRect(cx - 5, cy - 3, 8, 8);
                        g2.setColor(iconColor);
                        g2.drawRect(cx - 5, cy - 3, 8, 8);
                    } else {
                        // Maximize: single crisp 10x10 square
                        g2.drawRect(cx - 5, cy - 5, 10, 10);
                    }
                } else {
                    // Close: crisp 10x10 '×'
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(44, 34));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window w = SwingUtilities.getWindowAncestor(btn);
                if (w instanceof JFrame) {
                    JFrame frame = (JFrame) w;
                    if (type == 0) {
                        frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
                    } else if (type == 1) {
                        toggleMaximize(frame);
                    } else if (type == 2) {
                        triggerFrameClose(frame);
                    }
                }
            }
        });
        return btn;
    }

    private static JButton createWindowControlButton(final int type, final Window window, final Runnable action) {
        final JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isArmed() && getModel().isPressed();

                if (hover || pressed) {
                    if (type == 2) {
                        g2.setColor(pressed ? CLOSE_HOVER_BG.darker() : CLOSE_HOVER_BG);
                    } else {
                        g2.setColor(pressed ? Theme.ACTIVE_BG : Theme.HOVER_BG);
                    }
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                Color iconColor = (type == 2 && (hover || pressed)) ? Color.WHITE : Theme.BLACK;
                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(1.2f));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setToolTipText("Cerrar (Esc)");
        btn.setPreferredSize(new Dimension(44, 34));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (action != null) {
                    action.run();
                } else if (window != null) {
                    window.dispose();
                }
            }
        });
        return btn;
    }

    private static void installLazyFrameDrag(final Component dragComp) {
        MouseAdapter adapter = new MouseAdapter() {
            private Point pressScreen;
            private Point startWindowLoc;

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Window w = SwingUtilities.getWindowAncestor(dragComp);
                    if (w != null) {
                        pressScreen = e.getLocationOnScreen();
                        startWindowLoc = w.getLocation();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    Window w = SwingUtilities.getWindowAncestor(dragComp);
                    if (w instanceof JFrame) {
                        toggleMaximize((JFrame) w);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressScreen == null) {
                    return;
                }
                Window w = SwingUtilities.getWindowAncestor(dragComp);
                if (w == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                if (w instanceof JFrame && isMaximized((JFrame) w)) {
                    JFrame frame = (JFrame) w;
                    if (Math.abs(now.x - pressScreen.x) > 6 || Math.abs(now.y - pressScreen.y) > 6) {
                        double ratioX = (double) e.getXOnScreen() / Math.max(1, frame.getWidth());
                        toggleMaximize(frame);
                        int restoredW = frame.getWidth();
                        int newX = now.x - (int) (restoredW * ratioX);
                        int newY = Math.max(0, now.y - 16);
                        frame.setLocation(newX, newY);
                        pressScreen = now;
                        startWindowLoc = frame.getLocation();
                    }
                    return;
                }
                if (startWindowLoc != null) {
                    w.setLocation(
                            startWindowLoc.x + (now.x - pressScreen.x),
                            startWindowLoc.y + (now.y - pressScreen.y));
                }
            }
        };
        dragComp.addMouseListener(adapter);
        dragComp.addMouseMotionListener(adapter);
    }

    private static void installWindowDrag(final Component dragComp, final Window window, final boolean allowDoubleClickMax) {
        MouseAdapter adapter = new MouseAdapter() {
            private Point pressScreen;
            private Point startWindowLoc;

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && window != null) {
                    pressScreen = e.getLocationOnScreen();
                    startWindowLoc = window.getLocation();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressScreen != null && startWindowLoc != null && window != null) {
                    Point now = e.getLocationOnScreen();
                    window.setLocation(
                            startWindowLoc.x + (now.x - pressScreen.x),
                            startWindowLoc.y + (now.y - pressScreen.y));
                }
            }
        };
        dragComp.addMouseListener(adapter);
        dragComp.addMouseMotionListener(adapter);
    }

    /**
     * Installs 8-directional border/corner mouse resizing on an undecorated Window.
     */
    private static void installResizeHandler(final Window window) {
        final JRootPane root = (window instanceof JFrame) ? ((JFrame) window).getRootPane()
                : (window instanceof JDialog) ? ((JDialog) window).getRootPane() : null;
        if (root == null) {
            return;
        }
        MouseAdapter resizer = new MouseAdapter() {
            private int resizeDir = Cursor.DEFAULT_CURSOR;
            private Point startScreen;
            private Rectangle startBounds;

            @Override
            public void mouseMoved(MouseEvent e) {
                if ((window instanceof JFrame && (!((JFrame) window).isResizable() || isMaximized((JFrame) window)))
                        || (window instanceof JDialog && !((JDialog) window).isResizable())) {
                    if (root.getCursor().getType() != Cursor.DEFAULT_CURSOR) {
                        root.setCursor(Cursor.getDefaultCursor());
                    }
                    return;
                }
                int dir = getCursorDirection(e.getPoint(), root.getWidth(), root.getHeight());
                root.setCursor(Cursor.getPredefinedCursor(dir));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (startScreen == null) {
                    root.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if ((window instanceof JFrame && (!((JFrame) window).isResizable() || isMaximized((JFrame) window)))
                        || (window instanceof JDialog && !((JDialog) window).isResizable())) {
                    return;
                }
                resizeDir = getCursorDirection(e.getPoint(), root.getWidth(), root.getHeight());
                if (resizeDir != Cursor.DEFAULT_CURSOR) {
                    startScreen = e.getLocationOnScreen();
                    startBounds = window.getBounds();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                startScreen = null;
                startBounds = null;
                resizeDir = Cursor.DEFAULT_CURSOR;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startScreen == null || startBounds == null || resizeDir == Cursor.DEFAULT_CURSOR) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                int dx = now.x - startScreen.x;
                int dy = now.y - startScreen.y;

                Rectangle b = new Rectangle(startBounds);
                Dimension min = window.getMinimumSize();
                int minW = (min != null && min.width > 120) ? min.width : 420;
                int minH = (min != null && min.height > 100) ? min.height : 300;

                if (resizeDir == Cursor.E_RESIZE_CURSOR || resizeDir == Cursor.NE_RESIZE_CURSOR || resizeDir == Cursor.SE_RESIZE_CURSOR) {
                    b.width = Math.max(minW, startBounds.width + dx);
                }
                if (resizeDir == Cursor.S_RESIZE_CURSOR || resizeDir == Cursor.SE_RESIZE_CURSOR || resizeDir == Cursor.SW_RESIZE_CURSOR) {
                    b.height = Math.max(minH, startBounds.height + dy);
                }
                if (resizeDir == Cursor.W_RESIZE_CURSOR || resizeDir == Cursor.NW_RESIZE_CURSOR || resizeDir == Cursor.SW_RESIZE_CURSOR) {
                    int newW = Math.max(minW, startBounds.width - dx);
                    b.x = startBounds.x + (startBounds.width - newW);
                    b.width = newW;
                }
                if (resizeDir == Cursor.N_RESIZE_CURSOR || resizeDir == Cursor.NE_RESIZE_CURSOR || resizeDir == Cursor.NW_RESIZE_CURSOR) {
                    int newH = Math.max(minH, startBounds.height - dy);
                    b.y = startBounds.y + (startBounds.height - newH);
                    b.height = newH;
                }
                window.setBounds(b);
                window.validate();
            }

            private int getCursorDirection(Point p, int w, int h) {
                boolean left = p.x < RESIZE_MARGIN;
                boolean right = p.x >= w - RESIZE_MARGIN;
                boolean top = p.y < RESIZE_MARGIN;
                boolean bottom = p.y >= h - RESIZE_MARGIN;

                if (top && left) return Cursor.NW_RESIZE_CURSOR;
                if (top && right) return Cursor.NE_RESIZE_CURSOR;
                if (bottom && left) return Cursor.SW_RESIZE_CURSOR;
                if (bottom && right) return Cursor.SE_RESIZE_CURSOR;
                if (top) return Cursor.N_RESIZE_CURSOR;
                if (bottom) return Cursor.S_RESIZE_CURSOR;
                if (left) return Cursor.W_RESIZE_CURSOR;
                if (right) return Cursor.E_RESIZE_CURSOR;
                return Cursor.DEFAULT_CURSOR;
            }
        };
        root.addMouseListener(resizer);
        root.addMouseMotionListener(resizer);
    }
}
