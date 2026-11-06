package gui.action;

import automata.fsa.FiniteStateAutomaton;
import automata.mealy.MealyMachine;
import automata.mealy.MooreMachine;
import automata.pda.PushdownAutomaton;
import automata.turing.TuringMachine;
import grammar.cfg.ContextFreeGrammar;
import grammar.lsystem.LSystem;
import gui.environment.EnvironmentFrame;
import gui.environment.FrameFactory;
import gui.environment.Universe;
import gui.menu.MenuBarCreator;
import gui.pumping.CFPumpingLemmaChooser;
import gui.pumping.RegPumpingLemmaChooser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import plus.JFLAPPlus;
import plus.JetBrainsUI;
import plus.Theme;
import plus.VSIcons;
import regular.RegularExpression;

public class NewAction extends RestrictedAction {
    private static NewDialog DIALOG = null;

    public NewAction() {
        super("New...", null);
        putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_N, MAIN_MENU_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Universe.numberOfFrames() > 0) {
            Component invoker = null;
            EnvironmentFrame active = null;
            EnvironmentFrame[] frames = Universe.frames();
            if (frames != null) {
                for (int i = frames.length - 1; i >= 0; i--) {
                    if (frames[i] != null && frames[i].isShowing()) {
                        active = frames[i];
                        break;
                    }
                }
                if (active == null && frames.length > 0) {
                    active = frames[frames.length - 1];
                }
            }
            if (active != null && active.getNewTabButton() != null && active.getNewTabButton().isShowing()) {
                invoker = active.getNewTabButton();
            } else if (e != null && e.getSource() instanceof Component && ((Component) e.getSource()).isShowing()) {
                invoker = (Component) e.getSource();
            } else if (active != null) {
                invoker = active.getRootPane();
            }
            if (invoker != null) {
                showNewMiniMenu(invoker);
                return;
            }
        }
        showNew();
    }

    /**
     * Shows a compact JetBrains-style floating dropdown mini-menu directly below the invoker button
     * to create a new model tab inside the active IDE window without opening the full main launcher.
     */
    public static JPopupMenu showNewMiniMenu(Component invoker) {
        JPopupMenu popup = createNewMiniMenu();
        if (invoker != null && invoker.isShowing()) {
            int y = invoker instanceof JButton ? invoker.getHeight() + 2 : 36;
            popup.show(invoker, 0, y);
        }
        return popup;
    }

    public static JPopupMenu createNewMiniMenu() {
        final JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Theme.CARD_BG);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JLabel header = new JLabel("NUEVO PROYECTO EN PESTAÑA");
        header.setFont(new Font("Segoe UI", Font.BOLD, 10));
        header.setForeground(Theme.TEXT_MUTED);
        header.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        popup.add(header);
        popup.addSeparator();

        popup.add(createMiniMenuItem("1 · Autómata Finito (AFD / AFND)", KeyEvent.VK_1, new Runnable() {
            @Override
            public void run() {
                createWindow(new FiniteStateAutomaton());
            }
        }));
        popup.add(createMiniMenuItem("2 · Máquina de Turing (1 Cinta)", KeyEvent.VK_2, new Runnable() {
            @Override
            public void run() {
                createWindow(new TuringMachine(1));
            }
        }));

        JMenu multiTmMenu = new JMenu("3 · Máquina de Turing (Multicinta)");
        multiTmMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        multiTmMenu.setForeground(Theme.BLACK);
        multiTmMenu.setBackground(Theme.CARD_BG);
        multiTmMenu.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        multiTmMenu.getPopupMenu().setBackground(Theme.CARD_BG);
        multiTmMenu.getPopupMenu().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        for (int t = 2; t <= 5; t++) {
            final int tapes = t;
            multiTmMenu.add(createMiniMenuItem(tapes + " Cintas (" + tapes + "-Tape TM)", 0, new Runnable() {
                @Override
                public void run() {
                    createWindow(new TuringMachine(tapes));
                }
            }));
        }
        popup.add(multiTmMenu);

        popup.add(createMiniMenuItem("4 · Autómata de Pila (AP / PDA)", KeyEvent.VK_4, new Runnable() {
            @Override
            public void run() {
                createWindow(new PushdownAutomaton());
            }
        }));
        popup.add(createMiniMenuItem("5 · Máquina de Mealy", KeyEvent.VK_5, new Runnable() {
            @Override
            public void run() {
                createWindow(new MealyMachine());
            }
        }));
        popup.add(createMiniMenuItem("6 · Máquina de Moore", KeyEvent.VK_6, new Runnable() {
            @Override
            public void run() {
                createWindow(new MooreMachine());
            }
        }));

        popup.addSeparator();

        popup.add(createMiniMenuItem("7 · Gramática Formal (GLC / Reg)", KeyEvent.VK_7, new Runnable() {
            @Override
            public void run() {
                createWindow(new ContextFreeGrammar());
            }
        }));
        popup.add(createMiniMenuItem("8 · Expresión Regular (Regex)", KeyEvent.VK_8, new Runnable() {
            @Override
            public void run() {
                createWindow(new RegularExpression());
            }
        }));
        popup.add(createMiniMenuItem("9 · Sistema L (Fractal)", KeyEvent.VK_9, new Runnable() {
            @Override
            public void run() {
                createWindow(new LSystem());
            }
        }));

        JMenu pumpingMenu = new JMenu("Lema de Bombeo (Pumping Lemma)");
        pumpingMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pumpingMenu.setForeground(Theme.BLACK);
        pumpingMenu.setBackground(Theme.CARD_BG);
        pumpingMenu.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        pumpingMenu.getPopupMenu().setBackground(Theme.CARD_BG);
        pumpingMenu.getPopupMenu().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        pumpingMenu.add(createMiniMenuItem("Lema de Bombeo Regular", 0, new Runnable() {
            @Override
            public void run() {
                createWindow(new RegPumpingLemmaChooser());
            }
        }));
        pumpingMenu.add(createMiniMenuItem("Lema de Bombeo Libre de Contexto", 0, new Runnable() {
            @Override
            public void run() {
                createWindow(new CFPumpingLemmaChooser());
            }
        }));
        popup.add(pumpingMenu);

        popup.addSeparator();

        popup.add(createMiniMenuItem("Abrir Archivo Existente (.jff)...", KeyEvent.VK_O, new Runnable() {
            @Override
            public void run() {
                new OpenAction().actionPerformed(new ActionEvent(popup, ActionEvent.ACTION_PERFORMED, "Open"));
            }
        }));

        return popup;
    }

    private static JMenuItem createMiniMenuItem(String label, int mnemonic, final Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.setForeground(Theme.BLACK);
        item.setBackground(Theme.CARD_BG);
        item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 14));
        if (mnemonic > 0) {
            item.setMnemonic(mnemonic);
        }
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return item;
    }

    public static void closeNew() {
        if (DIALOG != null) {
            DIALOG.dispose();
            DIALOG = null;
        }
    }

    public static void showNew() {
        if (DIALOG == null) {
            DIALOG = new NewDialog();
        } else {
            DIALOG.rebuildUI();
        }
        DIALOG.setVisible(true);
        DIALOG.toFront();
    }

    public static void hideNew() {
        if (DIALOG != null) {
            DIALOG.setVisible(false);
        }
    }

    public static void refreshThemeIfShowing() {
        if (DIALOG != null) {
            DIALOG.rebuildUI();
        }
    }

    private static void createWindow(Serializable object) {
        if (DIALOG != null) {
            DIALOG.setVisible(false);
        }
        FrameFactory.createFrame(object);
    }

    static class NewDialog extends JFrame {
        private JPanel filesListPanel;

        public NewDialog() {
            super("JFLAP Studio — Entorno de Autómatas y Lenguajes Formales");
            plus.AppIcon.apply(this);
            plus.WindowChrome.installFrame(this, new Runnable() {
                @Override
                public void run() {
                    if (Universe.numberOfFrames() > 0) {
                        NewDialog.this.setVisible(false);
                    } else {
                        QuitAction.beginQuit();
                    }
                }
            });
            initMenu();
            initComponents();
            setResizable(true);
            pack();
            setMinimumSize(new Dimension(960, 640));
            setLocationRelativeTo(null);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    if (Universe.numberOfFrames() > 0) {
                        NewDialog.this.setVisible(false);
                    } else {
                        QuitAction.beginQuit();
                    }
                }
            });
        }

        private void initMenu() {
            JMenuBar menuBar = new JMenuBar();
            menuBar.setBackground(Theme.SURFACE);
            menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));

            JMenu fileMenu = new JMenu("Archivo (File)");
            if (Universe.CHOOSER != null) {
                MenuBarCreator.addItem(fileMenu, new OpenAction());
            }
            try {
                MenuBarCreator.addItem(fileMenu, new QuitAction());
            } catch (SecurityException ignored) {
            }
            menuBar.add(fileMenu);

            JMenu batchMenu = new JMenu("Lotes (Batch)");
            MenuBarCreator.addItem(batchMenu, new TestAction());
            menuBar.add(batchMenu);

            JMenu prefMenu = new JMenu("Preferencias");
            JMenu tmPrefMenu = new JMenu("Preferencias de Máquina de Turing");
            tmPrefMenu.add(Universe.curProfile.getTuringFinalCheckBox());
            tmPrefMenu.add(Universe.curProfile.getAcceptByFinalStateCheckBox());
            tmPrefMenu.add(Universe.curProfile.getAcceptByHaltingCheckBox());
            tmPrefMenu.add(Universe.curProfile.getAllowStayCheckBox());
            MenuBarCreator.addItem(prefMenu, new EmptyStringCharacterAction());
            prefMenu.add(new SetUndoAmountAction());
            prefMenu.add(tmPrefMenu);
            prefMenu.addSeparator();

            final JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem("Tema JetBrains Island Dark (Ctrl+Shift+D)", Theme.isDark());
            darkItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Theme.toggle();
                    rebuildUI();
                }
            });
            prefMenu.add(darkItem);

            final JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Mostrar Cuadrícula de Puntos (Ctrl+G)", Theme.isShowGrid());
            gridItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Theme.toggleGrid();
                    gridItem.setSelected(Theme.isShowGrid());
                }
            });
            prefMenu.add(gridItem);

            final JCheckBoxMenuItem snapItem = new JCheckBoxMenuItem("Ajustar Estados a Cuadrícula (Ctrl+Shift+G)", Theme.isSnapToGrid());
            snapItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Theme.toggleSnapToGrid();
                    snapItem.setSelected(Theme.isSnapToGrid());
                }
            });
            prefMenu.add(snapItem);
            menuBar.add(prefMenu);

            JMenu helpMenu = new JMenu("Ayuda");
            JMenuItem shortcutsItem = new JMenuItem("Guía Rápida y Atajos (F1)");
            shortcutsItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFLAPPlus.showShortcutsHelp(NewDialog.this);
                }
            });
            helpMenu.add(shortcutsItem);
            MenuBarCreator.addItem(helpMenu, new NewHelpAction());
            MenuBarCreator.addItem(helpMenu, new AboutAction());
            menuBar.add(helpMenu);

            JetBrainsUI.styleMenuBar(menuBar);
            setJMenuBar(menuBar);
        }

        public void rebuildUI() {
            plus.WindowChrome.installFrame(this, null);
            getContentPane().removeAll();
            initMenu();
            initComponents();
            revalidate();
            repaint();
        }

        private void initComponents() {
            JPanel outer = new JPanel(new BorderLayout());
            outer.setBackground(Theme.SURFACE);

            // Top JetBrains New UI Header
            JPanel header = new JPanel(new BorderLayout(12, 6));
            header.setBackground(Theme.SURFACE);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(16, 24, 16, 24)));

            JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            brandRow.setOpaque(false);
            JLabel logoLbl = new JLabel(plus.AppIcon.getImageIcon(40));
            brandRow.add(logoLbl);

            JPanel titleBox = new JPanel();
            titleBox.setOpaque(false);
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("JFLAP Studio");
            title.setFont(new Font("Segoe UI", Font.BOLD, 21));
            title.setForeground(Theme.BLACK);
            JLabel sub = new JLabel("Entorno interactivo para autómatas, gramáticas formales y máquinas de Turing");
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            sub.setForeground(Theme.TEXT_MUTED);
            titleBox.add(title);
            titleBox.add(Box.createVerticalStrut(3));
            titleBox.add(sub);
            brandRow.add(titleBox);
            header.add(brandRow, BorderLayout.WEST);

            JPanel actionsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actionsBox.setOpaque(false);

            JButton openBtn = createHeaderButton("Abrir Archivo (.jff)", true, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new OpenAction().actionPerformed(e);
                }
            });
            JButton batchBtn = createHeaderButton("Pruebas por Lote", false, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new TestAction().actionPerformed(e);
                }
            });
            JButton themeBtn = createHeaderButton(Theme.isDark() ? "Tema: Island Dark" : "Tema: Island Light", false, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Theme.toggle();
                    rebuildUI();
                }
            });
            JButton helpBtn = createHeaderButton("Atajos (F1)", false, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFLAPPlus.showShortcutsHelp(NewDialog.this);
                }
            });

            actionsBox.add(openBtn);
            actionsBox.add(batchBtn);
            actionsBox.add(themeBtn);
            actionsBox.add(helpBtn);
            header.add(actionsBox, BorderLayout.EAST);

            outer.add(header, BorderLayout.NORTH);

            // Center body: Left = Project Tree Explorer Island, Right = Rounded Model Cards
            JPanel body = new JPanel(new BorderLayout(20, 0));
            body.setBackground(Theme.SURFACE);
            body.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Left column: Recent & Workspace .jff files (IntelliJ Project Tree style)
            JPanel sidePanel = new JPanel(new BorderLayout(0, 10));
            sidePanel.setOpaque(false);
            sidePanel.setPreferredSize(new Dimension(285, 450));

            JPanel sideHeader = new JPanel(new BorderLayout());
            sideHeader.setOpaque(false);
            sideHeader.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
            JLabel sideTitle = new JLabel("PROYECTOS RECIENTES (.JFF)");
            sideTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            sideTitle.setForeground(Theme.TEXT_MUTED);

            JButton refreshBtn = new JButton("Actualizar");
            JetBrainsUI.styleCompactButton(refreshBtn, "Volver a escanear carpeta Trabajos y recientes");
            refreshBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshRecentFiles();
                }
            });
            sideHeader.add(sideTitle, BorderLayout.WEST);
            sideHeader.add(refreshBtn, BorderLayout.EAST);
            sidePanel.add(sideHeader, BorderLayout.NORTH);

            filesListPanel = new JPanel();
            filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
            filesListPanel.setOpaque(false);
            filesListPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            JScrollPane scroll = new JScrollPane(filesListPanel);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            scroll.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            JetBrainsUI.RoundedPanel explorerIsland = new JetBrainsUI.RoundedPanel(new BorderLayout(), 12);
            explorerIsland.setCustomColors(Theme.CARD_BG, Theme.CARD_BORDER);
            explorerIsland.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            explorerIsland.add(scroll, BorderLayout.CENTER);
            sidePanel.add(explorerIsland, BorderLayout.CENTER);

            refreshRecentFiles();
            body.add(sidePanel, BorderLayout.WEST);

            // Right column: Create a new model (12 Rounded Cards, arc = 12px)
            JPanel gridWrapper = new JPanel(new BorderLayout(0, 10));
            gridWrapper.setOpaque(false);
            JLabel sectionTitle = new JLabel("  CREAR UN NUEVO MODELO   ·   Haz clic o presiona las teclas 1 a 9");
            sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            sectionTitle.setForeground(Theme.TEXT_MUTED);
            gridWrapper.add(sectionTitle, BorderLayout.NORTH);

            JPanel cardsGrid = new JPanel(new GridLayout(6, 2, 12, 10));
            cardsGrid.setOpaque(false);

            cardsGrid.add(createModuleCard("1", "Autómata Finito (AFD / AFND)",
                    "Finite Automaton · Rangos [a-z], [0-9], \\d", KeyEvent.VK_1, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new FiniteStateAutomaton());
                        }
                    }));
            cardsGrid.add(createModuleCard("2", "Máquina de Mealy",
                    "Mealy Machine · Transductor con salidas en aristas", KeyEvent.VK_2, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new MealyMachine());
                        }
                    }));
            cardsGrid.add(createModuleCard("3", "Máquina de Moore",
                    "Moore Machine · Transductor con salidas en estados", KeyEvent.VK_3, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new MooreMachine());
                        }
                    }));
            cardsGrid.add(createModuleCard("4", "Autómata de Pila (AP / PDA)",
                    "Pushdown Automaton · Lenguajes libres de contexto", KeyEvent.VK_4, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new PushdownAutomaton());
                        }
                    }));
            cardsGrid.add(createModuleCard("5", "Máquina de Turing (1 Cinta)",
                    "Turing Machine · Incluye Tabla de Reglas (Ctrl+Shift+T)", KeyEvent.VK_5, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new TuringMachine(1));
                        }
                    }));
            cardsGrid.add(createModuleCard("6", "Máquina de Turing (Multicinta)",
                    "Multi-Tape Turing Machine · De 2 a 5 cintas simultáneas", KeyEvent.VK_6, new Runnable() {
                        @Override
                        public void run() {
                            promptMultiTapeTM();
                        }
                    }));
            cardsGrid.add(createModuleCard("7", "Gramática Formal",
                    "Grammar · Libre de contexto, regular o sin restricciones", KeyEvent.VK_7, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new ContextFreeGrammar());
                        }
                    }));
            cardsGrid.add(createModuleCard("8", "Sistema L (L-System)",
                    "L-System · Reescritura paralela y gráficos fractales", KeyEvent.VK_8, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new LSystem());
                        }
                    }));
            cardsGrid.add(createModuleCard("9", "Expresión Regular",
                    "Regular Expression · Edición y conversión a autómata", KeyEvent.VK_9, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new RegularExpression());
                        }
                    }));
            cardsGrid.add(createModuleCard("R", "Lema de Bombeo Regular",
                    "Regular Pumping Lemma · Práctica interactiva paso a paso", KeyEvent.VK_R, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new RegPumpingLemmaChooser());
                        }
                    }));
            cardsGrid.add(createModuleCard("C", "Lema de Bombeo Libre de Contexto",
                    "Context-Free Pumping Lemma · Práctica para LLC", KeyEvent.VK_C, new Runnable() {
                        @Override
                        public void run() {
                            createWindow(new CFPumpingLemmaChooser());
                        }
                    }));
            cardsGrid.add(createModuleCard("O", "Examinar Archivo Existente (.jff)",
                    "Abrir selector de archivos en cualquier carpeta", KeyEvent.VK_O, new Runnable() {
                        @Override
                        public void run() {
                            new OpenAction().actionPerformed(new ActionEvent(NewDialog.this, ActionEvent.ACTION_PERFORMED, "Open"));
                        }
                    }));

            gridWrapper.add(cardsGrid, BorderLayout.CENTER);
            body.add(gridWrapper, BorderLayout.CENTER);

            outer.add(body, BorderLayout.CENTER);

            // IntelliJ IDEA Integrated Bottom Status Bar (#1E1F22, 1px top border #393B40, text #868A91)
            JPanel statusBar = new JPanel(new BorderLayout());
            statusBar.setBackground(Theme.STATUS_BAR_BG);
            statusBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                    BorderFactory.createEmptyBorder(5, 20, 5, 20)));

            JLabel statusLeft = new JLabel("Listo    |    Soporte activo: Rangos [a-z], [0-9], \\d    |    Tabla de Turing: Ctrl+Shift+T    |    Zoom con rueda del ratón");
            statusLeft.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            statusLeft.setForeground(Theme.TEXT_MUTED);

            JLabel statusRight = new JLabel(Theme.isDark() ? "JetBrains Island Dark" : "JetBrains Island Light");
            statusRight.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            statusRight.setForeground(Theme.TEXT_MUTED);

            statusBar.add(statusLeft, BorderLayout.WEST);
            statusBar.add(statusRight, BorderLayout.EAST);
            outer.add(statusBar, BorderLayout.SOUTH);

            setContentPane(outer);
        }

        private void promptMultiTapeTM() {
            Object[] options = new Object[]{2, 3, 4, 5};
            Object val = JOptionPane.showInputDialog(
                    NewDialog.this,
                    "¿Cuántas cintas deseas para la Máquina de Turing?",
                    "Número de Cintas (Number of Tapes)",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (val instanceof Integer) {
                createWindow(new TuringMachine(((Integer) val).intValue()));
            }
        }

        private JButton createHeaderButton(String text, boolean primary, ActionListener listener) {
            JButton btn = new JButton(text);
            JetBrainsUI.styleSecondaryButton(btn, primary, null);
            btn.addActionListener(listener);
            return btn;
        }

        private JPanel createModuleCard(String badge, String titleText, String descText, int keyCode, final Runnable action) {
            final JetBrainsUI.RoundedPanel card = new JetBrainsUI.RoundedPanel(new BorderLayout(12, 4), 12);
            card.setCustomColors(Theme.CARD_BG, Theme.CARD_BORDER);
            card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JetBrainsUI.BadgeLabel badgeLbl = new JetBrainsUI.BadgeLabel(badge, 10);

            JPanel badgeWrap = new JPanel(new GridLayout(1, 1));
            badgeWrap.setOpaque(false);
            badgeWrap.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            badgeWrap.add(badgeLbl);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            final JLabel titleLbl = new JLabel(titleText);
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLbl.setForeground(Theme.BLACK);

            JLabel subLbl = new JLabel(descText);
            subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subLbl.setForeground(Theme.TEXT_MUTED);

            textPanel.add(titleLbl);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(subLbl);

            card.add(badgeWrap, BorderLayout.WEST);
            card.add(textPanel, BorderLayout.CENTER);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setHovered(false);
                }
            };
            card.addMouseListener(adapter);
            badgeLbl.addMouseListener(adapter);
            titleLbl.addMouseListener(adapter);
            subLbl.addMouseListener(adapter);

            getRootPane().registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            }, KeyStroke.getKeyStroke(keyCode, 0), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

            return card;
        }

        public void refreshRecentFiles() {
            if (filesListPanel == null) {
                return;
            }
            filesListPanel.removeAll();
            List<File> jffFiles = JFLAPPlus.discoverJffFiles();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            if (jffFiles.isEmpty()) {
                JLabel empty = new JLabel("No se encontraron archivos .jff cercanos.");
                empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                empty.setForeground(Theme.TEXT_MUTED);
                empty.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
                filesListPanel.add(empty);
            } else {
                for (final File f : jffFiles) {
                    final JetBrainsUI.TreeItemPanel item = new JetBrainsUI.TreeItemPanel(new BorderLayout(10, 2));
                    item.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                    item.setPreferredSize(new Dimension(250, 44));
                    item.setMaximumSize(new Dimension(400, 44));
                    item.setMinimumSize(new Dimension(180, 44));
                    item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    JLabel iconLbl = new JLabel(VSIcons.get(VSIcons.FILE_JFF, 16, null));

                    JPanel infoBox = new JPanel(new BorderLayout(0, 1));
                    infoBox.setOpaque(false);

                    final JLabel nameLbl = new JLabel(f.getName());
                    nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    nameLbl.setForeground(Theme.BLACK);

                    String parentName = f.getParentFile() != null ? f.getParentFile().getName() : "";
                    JLabel metaLbl = new JLabel(parentName + "  ·  " + sdf.format(new Date(f.lastModified())));
                    metaLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    metaLbl.setForeground(Theme.TEXT_MUTED);

                    infoBox.add(nameLbl, BorderLayout.NORTH);
                    infoBox.add(metaLbl, BorderLayout.SOUTH);

                    item.add(iconLbl, BorderLayout.WEST);
                    item.add(infoBox, BorderLayout.CENTER);

                    MouseAdapter ma = new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                if (Universe.CHOOSER != null && f.getParentFile() != null) {
                                    Universe.CHOOSER.setCurrentDirectory(f.getParentFile());
                                }
                                hideNew();
                                OpenAction.openFile(f, OpenAction.makeFilters());
                            } catch (Throwable ex) {
                                JOptionPane.showMessageDialog(NewDialog.this,
                                        "Error al abrir " + f.getName() + ":\n" + ex.getMessage(),
                                        "Error al abrir archivo", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                            item.setHovered(true);
                            nameLbl.setForeground(Theme.ACCENT);
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            item.setHovered(false);
                            nameLbl.setForeground(Theme.BLACK);
                        }
                    };
                    item.addMouseListener(ma);
                    nameLbl.addMouseListener(ma);
                    metaLbl.addMouseListener(ma);

                    filesListPanel.add(item);
                    filesListPanel.add(Box.createVerticalStrut(2));
                }
            }
            filesListPanel.revalidate();
            filesListPanel.repaint();
        }
    }
}
