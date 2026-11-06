package gui.environment;

import automata.Automaton;
import file.Codec;
import file.EncodeException;
import file.Encoder;
import file.ParseException;
import gui.action.NewAction;
import gui.action.OpenAction;
import gui.editor.EditBlockPane;
import gui.editor.EditorPane;
import gui.grammar.GrammarInputPane;
import gui.menu.MenuBarCreator;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import plus.AppIcon;
import plus.JetBrainsUI;
import plus.ModernFileChooser;
import plus.Theme;
import plus.VSIcons;

/**
 * Single-Window Multi-Project IDE Frame for JFLAP Studio.
 * Hosts multiple open Environments (.jff files / models) as modern IDE Document Tabs
 * inside a single OS window instead of spawning separate JFrames.
 */
public class EnvironmentFrame extends JFrame {
    private static final String DEFAULT_TITLE = "JFLAP Studio";

    private Environment environment;
    private int myNumber = 0xDEADCAFE;
    private final List<Environment> openEnvironments = new ArrayList<Environment>();
    private JPanel rootContentPanel;
    private JPanel projectHeaderRow;
    private JPanel projectTabBar;
    private JPanel workspaceToolsHolder;
    private JPanel environmentHostPanel;
    private JButton newTabButton;
    private int customMenuVariant = -1;

    public EnvironmentFrame(Environment environment) {
        this(environment, -1);
    }

    public EnvironmentFrame(Environment environment, int multiple) {
        super();
        this.customMenuVariant = multiple >= 0 ? 0 : -1;
        AppIcon.apply(this);
        plus.WindowChrome.installFrame(this, new Runnable() {
            @Override
            public void run() {
                close();
            }
        });

        this.rootContentPanel = new JPanel(new BorderLayout(0, 0));
        this.rootContentPanel.setBackground(Theme.ISLAND_BASE_BG);

        this.projectHeaderRow = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        this.projectHeaderRow.setBackground(Theme.ISLAND_BASE_BG);

        this.projectTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        this.projectTabBar.setOpaque(false);
        this.projectTabBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));

        this.workspaceToolsHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        this.workspaceToolsHolder.setOpaque(false);
        this.workspaceToolsHolder.setBorder(BorderFactory.createEmptyBorder(1, 4, 2, 8));

        this.projectHeaderRow.add(this.projectTabBar, BorderLayout.CENTER);
        this.projectHeaderRow.add(this.workspaceToolsHolder, BorderLayout.EAST);

        this.environmentHostPanel = new JPanel(new BorderLayout());
        this.environmentHostPanel.setBackground(Theme.ISLAND_BASE_BG);

        this.rootContentPanel.add(this.projectHeaderRow, BorderLayout.NORTH);
        this.rootContentPanel.add(this.environmentHostPanel, BorderLayout.CENTER);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(this.rootContentPanel, BorderLayout.CENTER);

        addOrSwitchEnvironment(environment);

        addWindowListener(new Listener());
        setLocation(50, 50);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        resizeWatcher();
    }

    public void setWorkspaceTools(Component tools) {
        if (this.workspaceToolsHolder == null) {
            return;
        }
        if (tools != null && tools.getParent() == this.workspaceToolsHolder) {
            return;
        }
        this.workspaceToolsHolder.removeAll();
        if (tools != null) {
            this.workspaceToolsHolder.add(tools);
        }
        this.projectHeaderRow.revalidate();
        this.projectHeaderRow.repaint();
    }

    /**
     * Adds a new Environment to this single IDE window (or switches to it if already open).
     */
    public void addOrSwitchEnvironment(final Environment env) {
        if (env == null) return;
        if (!this.openEnvironments.contains(env)) {
            this.openEnvironments.add(env);
            this.environment = env;
            int regNum = Universe.registerFrame(this);
            if (this.myNumber == 0xDEADCAFE) {
                this.myNumber = regNum;
            }
            env.addFileChangeListener(new FileChangeListener() {
                @Override
                public void fileChanged(FileChangeEvent e) {
                    refreshTitle();
                    refreshProjectTabs();
                    EditorPane ep = findEditor(env);
                    if (ep != null) {
                        ep.refreshExplorerFiles();
                    }
                }
            });
        }
        switchToEnvironment(env);
    }

    /**
     * Switches the active workspace inside this window to the specified Environment.
     */
    public void switchToEnvironment(Environment targetEnv) {
        if (targetEnv == null) return;
        if (!this.openEnvironments.contains(targetEnv)) {
            addOrSwitchEnvironment(targetEnv);
            return;
        }
        this.environment = targetEnv;
        if (targetEnv.getObject() instanceof Automaton) {
            ((Automaton) targetEnv.getObject()).setEnvironmentFrame(this);
        }
        initMenuBar();

        this.environmentHostPanel.removeAll();
        this.environmentHostPanel.add(targetEnv, BorderLayout.CENTER);

        EditorPane ep = findEditor(targetEnv);
        if (ep != null) {
            ep.installIntoWindowAndTabHierarchy();
            ep.refreshExplorerFiles();
        }

        refreshTitle();
        refreshProjectTabs();
        this.rootContentPanel.revalidate();
        this.rootContentPanel.repaint();
    }

    public Environment findEnvironmentForFile(File f) {
        if (f == null) return null;
        String targetPath = canonicalOrAbs(f);
        for (Environment env : this.openEnvironments) {
            File ef = env.getFile();
            if (ef != null && canonicalOrAbs(ef).equalsIgnoreCase(targetPath)) {
                return env;
            }
        }
        return null;
    }

    public List<Environment> getOpenEnvironments() {
        return this.openEnvironments;
    }

    private static String canonicalOrAbs(File f) {
        try {
            return f.getCanonicalPath();
        } catch (Throwable t) {
            return f.getAbsolutePath();
        }
    }

    private EditorPane findEditor(Component c) {
        if (c instanceof EditorPane) return (EditorPane) c;
        if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents()) {
                EditorPane found = findEditor(ch);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Rebuilds the top IDE Project Document Tab Bar showing all open .jff files / models.
     */
    public void refreshProjectTabs() {
        if (this.projectTabBar == null) return;
        if (this.projectHeaderRow != null) {
            this.projectHeaderRow.setBackground(Theme.ISLAND_BASE_BG);
        }
        this.projectTabBar.setBackground(Theme.ISLAND_BASE_BG);
        this.projectTabBar.removeAll();

        JLabel sectionBadge = new JLabel("PROYECTOS");
        sectionBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sectionBadge.setForeground(Theme.TEXT_MUTED);
        sectionBadge.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        this.projectTabBar.add(sectionBadge);

        for (int i = 0; i < this.openEnvironments.size(); i++) {
            final Environment env = this.openEnvironments.get(i);
            final boolean isActive = (env == this.environment);
            String title = getTabTitle(env, i + 1);
            ProjectTabChip chip = new ProjectTabChip(env, title, isActive);
            this.projectTabBar.add(chip);
        }

        this.projectTabBar.add(Box.createHorizontalStrut(4));

        final JButton newTabBtn = new JButton("+ Nuevo");
        this.newTabButton = newTabBtn;
        JetBrainsUI.styleCompactButton(newTabBtn, "Crear un nuevo modelo en una pestaña (Ctrl+N)");
        newTabBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewAction.showNewMiniMenu(newTabBtn);
            }
        });
        this.projectTabBar.add(newTabBtn);

        JButton openTabBtn = new JButton("Abrir...");
        JetBrainsUI.styleCompactButton(openTabBtn, "Abrir otro archivo .jff en esta misma ventana (Ctrl+O)");
        openTabBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new OpenAction().actionPerformed(e);
            }
        });
        this.projectTabBar.add(openTabBtn);

        this.projectTabBar.revalidate();
        this.projectTabBar.repaint();
    }

    public JButton getNewTabButton() {
        return this.newTabButton;
    }

    private String getTabTitle(Environment env, int index) {
        if (env == null) return "Proyecto " + index;
        File f = env.getFile();
        String base;
        if (f != null) {
            base = f.getName();
        } else {
            Serializable obj = env.getObject();
            if (obj instanceof automata.turing.TuringMachine) {
                base = "Turing-" + index + ".jff";
            } else if (obj instanceof automata.pda.PushdownAutomaton) {
                base = "PDA-" + index + ".jff";
            } else if (obj instanceof automata.fsa.FiniteStateAutomaton) {
                base = "AFD-" + index + ".jff";
            } else {
                base = "Proyecto-" + index + ".jff";
            }
        }
        return env.isDirty() ? base + " •" : base;
    }

    protected void initMenuBar() {
        JMenuBar mb = (this.customMenuVariant >= 0)
                ? MenuBarCreator.getMenuBar(this, this.customMenuVariant)
                : MenuBarCreator.getMenuBar(this);
        JetBrainsUI.styleMenuBar(mb);
        setJMenuBar(mb);
    }

    public String getDescription() {
        if (this.environment == null) {
            return "<untitled>";
        }
        if (this.environment.getFile() == null) {
            return "<untitled" + this.myNumber + ">";
        }
        if (this.environment.myObjects != null && this.environment.getActive() != null
                && (this.environment.getActive() instanceof EditorPane
                || this.environment.getActive() instanceof GrammarInputPane)) {
            String name = this.environment.getActive().getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "(" + this.environment.getFile().getName() + ")";
    }

    protected void refreshTitle() {
        String title = DEFAULT_TITLE + " — " + getDescription();
        setTitle(title);
        if (getJMenuBar() != null) {
            getJMenuBar().repaint();
        }
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public boolean save(boolean saveAs) {
        File file = saveAs ? null : this.environment.getFile();
        Codec codec = (Codec) this.environment.getEncoder();
        Serializable object = this.environment.getObject();
        if (this.environment.myObjects != null && this.environment.getActive() != null
                && (this.environment.getActive() instanceof EditorPane)) {
            EditorPane ep = (EditorPane) this.environment.getActive();
            File expected = new File(ep.getAutomaton().getFilePath() + ep.getAutomaton().getFileName());
            file = saveAs ? null : expected;
            object = ep.getAutomaton();
        } else if (this.environment.myObjects != null && this.environment.getActive() != null
                && (this.environment.getActive() instanceof GrammarInputPane)) {
            GrammarInputPane ep = (GrammarInputPane) this.environment.getActive();
            File expected = new File(ep.getGrammar().getFilePath() + ep.getGrammar().getFileName());
            file = saveAs ? null : expected;
            object = ep.getGrammar();
        }

        boolean blockEdit = false;
        if (this.environment.getActive() instanceof EditBlockPane) {
            EditBlockPane newPane = (EditBlockPane) this.environment.getActive();
            object = newPane.getAutomaton();
            blockEdit = true;
        }

        if (file != null && (codec == null || !codec.canEncode(object))) {
            JOptionPane.showMessageDialog(this,
                    "No se puede guardar esta estructura en el mismo formato en que fue leída.\nUsa Guardar Como para elegir un nuevo formato.",
                    "Error de E/S", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        List encoders = Universe.CODEC_REGISTRY.getEncoders(object);
        if (codec == null || !codec.canEncode(object)) {
            if (encoders != null && !encoders.isEmpty()) {
                codec = (Codec) encoders.get(0);
            }
        }

        if (file != null && file.getName().isEmpty()) {
            file = null;
        }

        while (file == null) {
            String defaultName = this.environment.getFile() != null ? this.environment.getFile().getName() : "proyecto.jff";
            File initialDir = this.environment.getFile() != null ? this.environment.getFile().getParentFile() : null;
            file = ModernFileChooser.showSaveDialog(this, initialDir, defaultName);
            if (file == null) {
                return false;
            }
            if (codec != null) {
                file = new File(codec.proposeFilename(file.getAbsolutePath(), object));
            }
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "¿Sobrescribir " + file.getName() + "?");
                if (result == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
                if (result == JOptionPane.NO_OPTION) {
                    file = null;
                }
            }
        }

        if (Universe.CHOOSER != null) {
            Universe.CHOOSER.resetChoosableFileFilters();
        }
        try {
            codec.encode(object, file, null);
            if (!blockEdit) {
                this.environment.setFile(file);
            }
            this.environment.setEncoder((Encoder) codec);
            this.environment.clearDirty();
            refreshTitle();
            refreshProjectTabs();
            return true;
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Escritura", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (EncodeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Escritura", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean closeEnvironment(Environment envToClose) {
        if (envToClose == null) return true;
        Environment prev = this.environment;
        this.environment = envToClose;
        if (envToClose.isDirty()) {
            File file = envToClose.getFile();
            String title = (file == null) ? "sin título" : file.getName();
            int result = JOptionPane.showConfirmDialog(this, "¿Guardar " + title + " antes de cerrar?");
            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                this.environment = prev;
                return false;
            }
            if (result == JOptionPane.YES_OPTION) {
                if (!save(false)) {
                    this.environment = prev;
                    return false;
                }
            }
        }

        int idx = this.openEnvironments.indexOf(envToClose);
        this.openEnvironments.remove(envToClose);
        Universe.unregisterFrame(this);

        if (this.openEnvironments.isEmpty()) {
            dispose();
            return true;
        }

        // Re-register remaining environments in Universe so Universe knows this frame is still active
        for (Environment remaining : this.openEnvironments) {
            this.environment = remaining;
            Universe.registerFrame(this);
        }

        int nextIdx = Math.max(0, Math.min(idx, this.openEnvironments.size() - 1));
        switchToEnvironment(this.openEnvironments.get(nextIdx));
        return true;
    }

    public boolean close() {
        // Check all open tabs before closing the IDE window
        List<Environment> snapshot = new ArrayList<Environment>(this.openEnvironments);
        for (Environment env : snapshot) {
            if (env.isDirty()) {
                switchToEnvironment(env);
                File file = env.getFile();
                String title = (file == null) ? "sin título" : file.getName();
                int result = JOptionPane.showConfirmDialog(this, "¿Guardar " + title + " antes de cerrar?");
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                    return false;
                }
                if (result == JOptionPane.YES_OPTION) {
                    if (!save(false)) {
                        return false;
                    }
                }
                env.clearDirty();
            }
        }
        for (Environment env : snapshot) {
            this.environment = env;
            Universe.unregisterFrame(this);
        }
        this.openEnvironments.clear();
        dispose();
        return true;
    }

    public void resizeWatcher() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (environment != null) {
                    environment.resizeSplit();
                }
            }
        });
    }

    @Override
    public String toString() {
        return getDescription();
    }

    private class Listener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent event) {
            close();
        }
    }

    /**
     * Modern VS Code / IntelliJ Document Tab Chip for switching between open .jff files in-place.
     */
    private class ProjectTabChip extends JPanel {
        private final Environment targetEnv;
        private final boolean active;
        private boolean hovered = false;
        private boolean closeHovered = false;

        ProjectTabChip(final Environment targetEnv, String title, boolean active) {
            super(new BorderLayout(8, 0));
            this.targetEnv = targetEnv;
            this.active = active;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel icon = new JLabel(VSIcons.get(VSIcons.FILE_JFF, 14, null));
            JLabel text = new JLabel(title);
            text.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
            text.setForeground(active ? Theme.BLACK : Theme.TEXT_MUTED);

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            left.setOpaque(false);
            left.add(icon);
            left.add(text);

            final JLabel closeBtn = new JLabel("×");
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            closeBtn.setForeground(Theme.TEXT_MUTED);
            closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
            closeBtn.setToolTipText("Cerrar este proyecto");

            closeBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    e.consume();
                    closeEnvironment(targetEnv);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    closeHovered = true;
                    closeBtn.setForeground(Theme.DANGER);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    closeHovered = false;
                    closeBtn.setForeground(Theme.TEXT_MUTED);
                    repaint();
                }
            });

            add(left, BorderLayout.CENTER);
            add(closeBtn, BorderLayout.EAST);

            MouseAdapter switchMa = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON2) {
                        closeEnvironment(targetEnv);
                    } else {
                        switchToEnvironment(targetEnv);
                    }
                }

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
            addMouseListener(switchMa);
            left.addMouseListener(switchMa);
            text.addMouseListener(switchMa);
            icon.addMouseListener(switchMa);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(Theme.SURFACE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                // Top 2px JetBrains #3574F0 active indicator line
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(6, 1, w - 12, 3, 2, 2);
            } else if (hovered || closeHovered) {
                g2.setColor(Theme.HOVER_BG);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            } else {
                g2.setColor(Theme.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
