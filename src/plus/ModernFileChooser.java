package plus;

import gui.environment.Universe;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Modern JetBrains Island Dark File Chooser Dialog replacing the legacy 1990s Java Metal JFileChooser.
 * Supports quick sidebar locations, instant search filtering, .jff model type badges, and native OS fallback.
 */
public class ModernFileChooser extends JDialog {
    private static final Map<String, String> TYPE_CACHE = new HashMap<String, String>();

    private final boolean saveMode;
    private File currentDir;
    private File selectedFile;
    private File[] selectedFiles = new File[0];
    private boolean approved = false;

    private JetBrainsUI.ModernTextField pathField;
    private JetBrainsUI.ModernTextField searchField;
    private JetBrainsUI.ModernTextField fileNameField;
    private JPanel fileListContainer;
    private JLabel countStatusLabel;
    private final List<FileRowPanel> currentRows = new ArrayList<FileRowPanel>();

    public ModernFileChooser(Window owner, boolean saveMode, File initialDir, String defaultName) {
        super(owner, saveMode ? "Guardar Proyecto JFLAP (.jff)" : "Abrir Proyecto JFLAP (.jff)",
                Dialog.ModalityType.APPLICATION_MODAL);
        this.saveMode = saveMode;
        AppIcon.apply(this);

        if (initialDir != null && initialDir.exists()) {
            this.currentDir = initialDir.isDirectory() ? initialDir : initialDir.getParentFile();
        } else if (Universe.CHOOSER != null && Universe.CHOOSER.getCurrentDirectory() != null) {
            this.currentDir = Universe.CHOOSER.getCurrentDirectory();
        } else {
            this.currentDir = defaultStartDir();
        }

        buildUI(defaultName);
        WindowChrome.installDialog(this);
        navigateTo(this.currentDir);
        pack();
        setSize(new Dimension(780, 540));
        setLocationRelativeTo(owner);
    }

    private static File defaultStartDir() {
        String home = System.getProperty("user.home", ".");
        File trabajosJflap = new File(home, "Desktop\\Trabajos\\JFLAP");
        if (trabajosJflap.exists() && trabajosJflap.isDirectory()) return trabajosJflap;
        File trabajos = new File(home, "Desktop\\Trabajos");
        if (trabajos.exists() && trabajos.isDirectory()) return trabajos;
        File desktop = new File(home, "Desktop");
        if (desktop.exists() && desktop.isDirectory()) return desktop;
        return new File(home);
    }

    public static File[] showOpenFilesDialog(Component parent, File initialDir) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        ModernFileChooser dlg = new ModernFileChooser(owner, false, initialDir, "");
        dlg.setVisible(true);
        if (dlg.approved && dlg.selectedFiles != null && dlg.selectedFiles.length > 0) {
            if (Universe.CHOOSER != null && dlg.currentDir != null) {
                Universe.CHOOSER.setCurrentDirectory(dlg.currentDir);
            }
            return dlg.selectedFiles;
        }
        return null;
    }

    public static File showOpenDialog(Component parent, File initialDir) {
        File[] files = showOpenFilesDialog(parent, initialDir);
        return (files != null && files.length > 0) ? files[0] : null;
    }

    public static File showSaveDialog(Component parent, File initialDir, String defaultFileName) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        ModernFileChooser dlg = new ModernFileChooser(owner, true, initialDir, defaultFileName);
        dlg.setVisible(true);
        if (dlg.approved && dlg.selectedFile != null) {
            if (Universe.CHOOSER != null && dlg.currentDir != null) {
                Universe.CHOOSER.setCurrentDirectory(dlg.currentDir);
            }
            return dlg.selectedFile;
        }
        return null;
    }

    private void buildUI(String defaultName) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.ISLAND_BASE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Top Navigation & Search Island
        JetBrainsUI.RoundedPanel topBar = new JetBrainsUI.RoundedPanel(new BorderLayout(8, 0), 10);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        navLeft.setOpaque(false);

        JButton upBtn = new JButton("Subir", createFolderUpIcon());
        JetBrainsUI.styleCompactButton(upBtn, "Ir a la carpeta superior (Backspace)");
        upBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentDir != null && currentDir.getParentFile() != null) {
                    navigateTo(currentDir.getParentFile());
                }
            }
        });
        navLeft.add(upBtn);

        this.pathField = new JetBrainsUI.ModernTextField("Ruta de carpeta...");
        this.pathField.setFont(new Font("Consolas", Font.PLAIN, 12));
        this.pathField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = new File(pathField.getText().trim());
                if (f.exists() && f.isDirectory()) {
                    navigateTo(f);
                } else if (f.exists() && f.isFile()) {
                    selectAndApprove(f);
                }
            }
        });

        JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
        searchWrap.setOpaque(false);
        searchWrap.setPreferredSize(new Dimension(210, 28));
        this.searchField = new JetBrainsUI.ModernTextField("Filtrar por nombre...");
        this.searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshFileList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshFileList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshFileList();
            }
        });
        searchWrap.add(this.searchField, BorderLayout.CENTER);

        topBar.add(navLeft, BorderLayout.WEST);
        topBar.add(this.pathField, BorderLayout.CENTER);
        topBar.add(searchWrap, BorderLayout.EAST);

        // 2. Center Split: Left Quick Locations Island + Right File Table Island
        JPanel centerArea = new JPanel(new BorderLayout(8, 0));
        centerArea.setOpaque(false);
        centerArea.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Left Quick Places Island
        JetBrainsUI.RoundedPanel placesIsland = new JetBrainsUI.RoundedPanel(new BorderLayout(0, 8), 10);
        placesIsland.setPreferredSize(new Dimension(185, 300));
        placesIsland.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel placesTitle = new JLabel("UBICACIONES");
        placesTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        placesTitle.setForeground(Theme.TEXT_MUTED);
        placesIsland.add(placesTitle, BorderLayout.NORTH);

        JPanel placesList = new JPanel();
        placesList.setLayout(new BoxLayout(placesList, BoxLayout.Y_AXIS));
        placesList.setOpaque(false);

        Map<String, File> places = getQuickPlaces();
        for (final Map.Entry<String, File> entry : places.entrySet()) {
            final JetBrainsUI.TreeItemPanel item = new JetBrainsUI.TreeItemPanel(new BorderLayout(8, 0));
            item.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            item.setPreferredSize(new Dimension(160, 32));
            item.setMaximumSize(new Dimension(200, 32));
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel icon = new JLabel(VSIcons.get(VSIcons.EXPLORER, 14, Theme.ACCENT));
            final JLabel text = new JLabel(entry.getKey());
            text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            text.setForeground(Theme.BLACK);

            item.add(icon, BorderLayout.WEST);
            item.add(text, BorderLayout.CENTER);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigateTo(entry.getValue());
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    item.setHovered(true);
                    text.setForeground(Theme.ACCENT);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    item.setHovered(false);
                    text.setForeground(Theme.BLACK);
                }
            };
            item.addMouseListener(ma);
            text.addMouseListener(ma);
            placesList.add(item);
            placesList.add(Box.createVerticalStrut(2));
        }

        placesIsland.add(placesList, BorderLayout.CENTER);

        // Native OS Dialog Fallback Button at bottom of sidebar
        JButton nativeBtn = new JButton("Explorador Windows...");
        JetBrainsUI.styleCompactButton(nativeBtn, "Abrir el diálogo nativo de Windows");
        nativeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openNativeWindowsDialog();
            }
        });
        placesIsland.add(nativeBtn, BorderLayout.SOUTH);

        // Right File List Island
        JetBrainsUI.RoundedPanel filesIsland = new JetBrainsUI.RoundedPanel(new BorderLayout(0, 4), 10);
        filesIsland.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(2, 10, 6, 12)));
        JLabel colName = new JLabel("Nombre de Archivo / Carpeta");
        colName.setFont(new Font("Segoe UI", Font.BOLD, 11));
        colName.setForeground(Theme.TEXT_MUTED);
        JLabel colMeta = new JLabel("Tipo de Modelo  ·  Modificado");
        colMeta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        colMeta.setForeground(Theme.TEXT_MUTED);
        headerRow.add(colName, BorderLayout.WEST);
        headerRow.add(colMeta, BorderLayout.EAST);
        filesIsland.add(headerRow, BorderLayout.NORTH);

        this.fileListContainer = new JPanel();
        this.fileListContainer.setLayout(new BoxLayout(this.fileListContainer, BoxLayout.Y_AXIS));
        this.fileListContainer.setBackground(Theme.SURFACE);

        JScrollPane scroll = new JScrollPane(this.fileListContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(Theme.SURFACE);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new JetBrainsUI.ModernScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        filesIsland.add(scroll, BorderLayout.CENTER);

        centerArea.add(placesIsland, BorderLayout.WEST);
        centerArea.add(filesIsland, BorderLayout.CENTER);

        // 3. Bottom Action Bar Island
        JetBrainsUI.RoundedPanel bottomBar = new JetBrainsUI.RoundedPanel(new BorderLayout(12, 0), 10);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel fileInputBox = new JPanel(new BorderLayout(8, 0));
        fileInputBox.setOpaque(false);
        JLabel fnLabel = new JLabel("Archivo:");
        fnLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fnLabel.setForeground(Theme.BLACK);

        this.fileNameField = new JetBrainsUI.ModernTextField("Selecciona o escribe un archivo .jff...");
        if (defaultName != null && !defaultName.isEmpty()) {
            this.fileNameField.setText(defaultName);
        }
        this.fileNameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmSelection();
            }
        });
        fileInputBox.add(fnLabel, BorderLayout.WEST);
        fileInputBox.add(this.fileNameField, BorderLayout.CENTER);

        JPanel buttonsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsBox.setOpaque(false);

        this.countStatusLabel = new JLabel("");
        this.countStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        this.countStatusLabel.setForeground(Theme.TEXT_MUTED);
        buttonsBox.add(this.countStatusLabel);

        JButton cancelBtn = new JButton("Cancelar");
        JetBrainsUI.styleCompactButton(cancelBtn, "Cerrar sin seleccionar (Esc)");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approved = false;
                dispose();
            }
        });

        JButton okBtn = new JButton(this.saveMode ? "Guardar" : "Abrir Proyecto");
        JetBrainsUI.styleMediaPlayButton(okBtn, this.saveMode ? "Guardar archivo .jff" : "Abrir el archivo seleccionado en esta ventana");
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmSelection();
            }
        });

        buttonsBox.add(cancelBtn);
        buttonsBox.add(okBtn);

        bottomBar.add(fileInputBox, BorderLayout.CENTER);
        bottomBar.add(buttonsBox, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);
        root.add(centerArea, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);

        // Escape key closes dialog
        KeyAdapter escListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    approved = false;
                    dispose();
                }
            }
        };
        addKeyListener(escListener);
        this.searchField.addKeyListener(escListener);
        this.fileNameField.addKeyListener(escListener);
    }

    private Map<String, File> getQuickPlaces() {
        Map<String, File> map = new LinkedHashMap<String, File>();
        String home = System.getProperty("user.home", ".");
        File jflapDir = new File(home, "Desktop\\Trabajos\\JFLAP");
        if (jflapDir.exists()) map.put("Proyectos JFLAP", jflapDir);
        File trabajosDir = new File(home, "Desktop\\Trabajos");
        if (trabajosDir.exists()) map.put("Trabajos", trabajosDir);
        File desktopDir = new File(home, "Desktop");
        if (desktopDir.exists()) map.put("Escritorio", desktopDir);
        File downloadsDir = new File(home, "Downloads");
        if (downloadsDir.exists()) map.put("Descargas", downloadsDir);
        File docsDir = new File(home, "Documents");
        if (docsDir.exists()) map.put("Documentos", docsDir);
        File homeDir = new File(home);
        if (homeDir.exists()) map.put("Carpeta Personal", homeDir);
        return map;
    }

    private void navigateTo(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        this.currentDir = dir;
        this.pathField.setText(dir.getAbsolutePath());
        refreshFileList();
    }

    private void refreshFileList() {
        if (this.fileListContainer == null || this.currentDir == null) return;
        this.fileListContainer.removeAll();
        this.currentRows.clear();

        String filter = this.searchField != null ? this.searchField.getText().trim().toLowerCase() : "";
        File[] children = this.currentDir.listFiles();
        List<File> dirs = new ArrayList<File>();
        List<File> files = new ArrayList<File>();

        if (children != null) {
            for (File f : children) {
                if (f.isHidden()) continue;
                String name = f.getName();
                if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) {
                    continue;
                }
                if (f.isDirectory()) {
                    dirs.add(f);
                } else if (isSupportedFile(name)) {
                    files.add(f);
                }
            }
        }

        Comparator<File> alpha = new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        };
        dirs.sort(alpha);
        files.sort(new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });

        for (File d : dirs) {
            FileRowPanel row = new FileRowPanel(d, true);
            this.currentRows.add(row);
            this.fileListContainer.add(row);
        }
        for (File f : files) {
            FileRowPanel row = new FileRowPanel(f, false);
            this.currentRows.add(row);
            this.fileListContainer.add(row);
        }

        if (this.countStatusLabel != null) {
            this.countStatusLabel.setText(files.size() + " archivos .jff  ·  " + dirs.size() + " carpetas   ");
        }

        this.fileListContainer.revalidate();
        this.fileListContainer.repaint();
    }

    private boolean isSupportedFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jff") || lower.endsWith(".txt") || lower.endsWith(".xml");
    }

    private void selectRow(FileRowPanel row) {
        selectRow(row, false);
    }

    private void selectRow(FileRowPanel row, boolean toggleMulti) {
        if (toggleMulti && !this.saveMode && row != null && !row.isDirectory) {
            row.setSelected(!row.selected);
            List<File> selList = new ArrayList<File>();
            StringBuilder names = new StringBuilder();
            for (FileRowPanel r : this.currentRows) {
                if (r.selected && !r.isDirectory) {
                    selList.add(r.file);
                    if (names.length() > 0) names.append(", ");
                    names.append(r.file.getName());
                }
            }
            this.selectedFiles = selList.toArray(new File[selList.size()]);
            this.selectedFile = selList.isEmpty() ? null : selList.get(0);
            this.fileNameField.setText(names.toString());
            return;
        }
        for (FileRowPanel r : this.currentRows) {
            r.setSelected(r == row);
        }
        if (row != null && !row.isDirectory) {
            this.selectedFile = row.file;
            this.selectedFiles = new File[]{row.file};
            this.fileNameField.setText(row.file.getName());
        }
    }

    private void selectAndApprove(File f) {
        this.selectedFile = f;
        this.selectedFiles = new File[]{f};
        this.approved = true;
        dispose();
    }

    private void confirmSelection() {
        if (!this.saveMode && this.selectedFiles != null && this.selectedFiles.length > 1) {
            this.approved = true;
            dispose();
            return;
        }
        String typed = this.fileNameField.getText().trim();
        if (!typed.isEmpty()) {
            File candidate = new File(typed);
            if (!candidate.isAbsolute()) {
                candidate = new File(this.currentDir, typed);
            }
            if (candidate.exists() && candidate.isDirectory()) {
                navigateTo(candidate);
                return;
            }
            if (this.saveMode) {
                if (!candidate.getName().contains(".")) {
                    candidate = new File(candidate.getParentFile(), candidate.getName() + ".jff");
                }
                selectAndApprove(candidate);
                return;
            } else if (candidate.exists() && candidate.isFile()) {
                selectAndApprove(candidate);
                return;
            }
        }
        if (this.selectedFile != null && (this.saveMode || this.selectedFile.exists())) {
            selectAndApprove(this.selectedFile);
        }
    }

    private void openNativeWindowsDialog() {
        Window owner = getOwner();
        FileDialog fd;
        if (owner instanceof Frame) {
            fd = new FileDialog((Frame) owner, getTitle(), this.saveMode ? FileDialog.SAVE : FileDialog.LOAD);
        } else if (owner instanceof Dialog) {
            fd = new FileDialog((Dialog) owner, getTitle(), this.saveMode ? FileDialog.SAVE : FileDialog.LOAD);
        } else {
            fd = new FileDialog((Frame) null, getTitle(), this.saveMode ? FileDialog.SAVE : FileDialog.LOAD);
        }
        if (this.currentDir != null) {
            fd.setDirectory(this.currentDir.getAbsolutePath());
        }
        fd.setFile("*.jff");
        fd.setVisible(true);
        if (fd.getFile() != null) {
            File chosen = new File(fd.getDirectory(), fd.getFile());
            selectAndApprove(chosen);
        }
    }

    private static String detectJffType(File f) {
        String key = f.getAbsolutePath() + ":" + f.lastModified();
        String cached = TYPE_CACHE.get(key);
        if (cached != null) return cached;

        String result = "Proyecto JFLAP";
        if (f.getName().toLowerCase().endsWith(".jff")) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(f));
                char[] buf = new char[512];
                int n = br.read(buf);
                if (n > 0) {
                    String head = new String(buf, 0, n);
                    int s = head.indexOf("<type>");
                    int e = head.indexOf("</type>");
                    if (s >= 0 && e > s + 6) {
                        String t = head.substring(s + 6, e).trim();
                        if ("turing".equalsIgnoreCase(t) || "tm".equalsIgnoreCase(t)) {
                            result = "Máquina de Turing";
                        } else if ("fa".equalsIgnoreCase(t)) {
                            result = "Autómata Finito";
                        } else if ("pda".equalsIgnoreCase(t)) {
                            result = "Autómata de Pila";
                        } else if ("grammar".equalsIgnoreCase(t)) {
                            result = "Gramática";
                        } else if ("re".equalsIgnoreCase(t)) {
                            result = "Expresión Regular";
                        } else if ("mealy".equalsIgnoreCase(t)) {
                            result = "Máquina de Mealy";
                        } else if ("moore".equalsIgnoreCase(t)) {
                            result = "Máquina de Moore";
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        } else {
            result = "Archivo de Texto";
        }
        TYPE_CACHE.put(key, result);
        return result;
    }

    private class FileRowPanel extends JPanel {
        final File file;
        final boolean isDirectory;
        boolean hovered = false;
        boolean selected = false;

        FileRowPanel(final File file, final boolean isDirectory) {
            super(new BorderLayout(10, 0));
            this.file = file;
            this.isDirectory = isDirectory;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 12));
            setPreferredSize(new Dimension(480, 34));
            setMaximumSize(new Dimension(2000, 34));
            setMinimumSize(new Dimension(200, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            Icon ic = isDirectory ? createFolderIcon() : VSIcons.get(VSIcons.FILE_JFF, 16, null);
            JLabel iconLbl = new JLabel(ic);

            JLabel nameLbl = new JLabel(file.getName());
            nameLbl.setFont(new Font("Segoe UI", isDirectory ? Font.PLAIN : Font.BOLD, 13));
            nameLbl.setForeground(Theme.BLACK);

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            left.setOpaque(false);
            left.add(iconLbl);
            left.add(nameLbl);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            right.setOpaque(false);

            if (!isDirectory) {
                String modelType = detectJffType(file);
                JLabel badge = new JLabel(modelType);
                badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                badge.setForeground(modelType.contains("Turing") ? Theme.FIRED_CYAN : Theme.ACCENT);
                right.add(badge);
            }

            String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(file.lastModified()));
            JLabel dateLbl = new JLabel(isDirectory ? "Carpeta" : dateStr);
            dateLbl.setFont(new Font("Consolas", Font.PLAIN, 11));
            dateLbl.setForeground(Theme.TEXT_MUTED);
            right.add(dateLbl);

            add(left, BorderLayout.WEST);
            add(right, BorderLayout.EAST);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isDirectory) {
                        if (e.getClickCount() >= 1) {
                            navigateTo(file);
                        }
                    } else {
                        boolean toggle = e.isControlDown() || e.isMetaDown() || e.isShiftDown();
                        selectRow(FileRowPanel.this, toggle);
                        if (e.getClickCount() >= 2) {
                            selectAndApprove(file);
                        }
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
            addMouseListener(ma);
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected || hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? Theme.TOOL_ACTIVE_BG : Theme.HOVER_BG);
                g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 8, 8);
                if (selected) {
                    g2.setColor(Theme.ACCENT);
                    g2.drawRoundRect(4, 2, getWidth() - 9, getHeight() - 5, 8, 8);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static Icon createFolderIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE0, 0xA8, 0x4E));
                g2.fillRoundRect(x + 1, y + 4, 14, 10, 3, 3);
                g2.fillRoundRect(x + 1, y + 2, 6, 4, 2, 2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private static Icon createFolderUpIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 7, y + 11, x + 7, y + 3);
                g2.drawLine(x + 3, y + 7, x + 7, y + 3);
                g2.drawLine(x + 11, y + 7, x + 7, y + 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 14;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };
    }

    /**
     * Drop-in JFileChooser replacement for Universe.CHOOSER so every action in JFLAP
     * (including TestAction / Batch Testing, MultipleSimulateAction, SaveAction, etc.)
     * opens our JetBrains Island Dark ModernFileChooser instead of the old Java Metal JFileChooser.
     */
    public static class ModernJFileChooser extends javax.swing.JFileChooser {
        public ModernJFileChooser() {
            super();
        }

        public ModernJFileChooser(String currentDirectoryPath) {
            super(currentDirectoryPath);
        }

        public ModernJFileChooser(File currentDirectory) {
            super(currentDirectory);
        }

        @Override
        public int showOpenDialog(Component parent) {
            File[] files = ModernFileChooser.showOpenFilesDialog(parent, getCurrentDirectory());
            if (files != null && files.length > 0) {
                setSelectedFiles(files);
                setSelectedFile(files[0]);
                if (files[0].getParentFile() != null) {
                    setCurrentDirectory(files[0].getParentFile());
                }
                return APPROVE_OPTION;
            }
            return CANCEL_OPTION;
        }

        @Override
        public int showSaveDialog(Component parent) {
            String defName = getSelectedFile() != null ? getSelectedFile().getName() : "";
            File saved = ModernFileChooser.showSaveDialog(parent, getCurrentDirectory(), defName);
            if (saved != null) {
                setSelectedFile(saved);
                setSelectedFiles(new File[]{saved});
                if (saved.getParentFile() != null) {
                    setCurrentDirectory(saved.getParentFile());
                }
                return APPROVE_OPTION;
            }
            return CANCEL_OPTION;
        }

        @Override
        public int showDialog(Component parent, String approveButtonText) {
            if (approveButtonText != null && approveButtonText.toLowerCase().contains("save")) {
                return showSaveDialog(parent);
            }
            return showOpenDialog(parent);
        }
    }
}
