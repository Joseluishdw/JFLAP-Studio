package gui.action;

import automata.turing.TuringMachine;
import file.Codec;
import file.DataException;
import file.ParseException;
import gui.environment.Environment;
import gui.environment.EnvironmentFrame;
import gui.environment.FrameFactory;
import gui.environment.Universe;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import plus.ModernFileChooser;

public class OpenAction extends RestrictedAction {
    private static boolean openOrRead = false;
    private JFileChooser fileChooser;
    private static Serializable lastObject = null;
    private static File lastFile = null;
    private static boolean lastFileOpened = false;

    public OpenAction() {
        super("Open...", null);
        putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_O, MAIN_MENU_MASK));
        this.fileChooser = Universe.CHOOSER;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Component source = null;
        lastFileOpened = false;
        try {
            if (e != null && e.getSource() instanceof Component) {
                source = (Component) e.getSource();
            }
        } catch (Throwable ignored) {
        }

        File tempFile = this.fileChooser != null ? this.fileChooser.getCurrentDirectory() : null;
        if (lastFile != null && lastFile.getParentFile() != null) {
            tempFile = lastFile.getParentFile();
        }
        Codec[] codecs = makeFilters();

        File[] files = ModernFileChooser.showOpenFilesDialog(source, tempFile);
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!openOrRead) {
                EnvironmentFrame existingFrame = Universe.frameForFile(file);
                if (existingFrame != null) {
                    Environment existingEnv = existingFrame.findEnvironmentForFile(file);
                    if (existingEnv != null) {
                        existingFrame.switchToEnvironment(existingEnv);
                        existingFrame.toFront();
                        lastFile = file;
                        lastFileOpened = true;
                        continue;
                    }
                }
            }
            try {
                openFile(file, codecs);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(source, ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
            } catch (DataException ex) {
                JOptionPane.showMessageDialog(source, ex.getMessage(), "Data Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (Universe.CHOOSER != null) {
            Universe.CHOOSER.resetChoosableFileFilters();
        }
        lastFileOpened = true;
    }

    public static Serializable readFileAndCodecs(File file) {
        setOpenOrRead(true);
        Codec[] codecs = makeFilters();
        openFile(file, codecs);
        setOpenOrRead(false);
        return getLastObjectOpened();
    }

    public static Codec[] makeFilters() {
        if (Universe.CHOOSER != null) {
            Universe.CHOOSER.resetChoosableFileFilters();
        }
        List decoders = Universe.CODEC_REGISTRY.getDecoders();
        Iterator it = decoders.iterator();
        while (it.hasNext()) {
            FileFilter filter = (FileFilter) it.next();
            if (Universe.CHOOSER != null) {
                Universe.CHOOSER.addChoosableFileFilter(filter);
            }
        }
        if (Universe.CHOOSER != null) {
            Universe.CHOOSER.setFileFilter(Universe.CHOOSER.getAcceptAllFileFilter());
        }
        return (Codec[]) decoders.toArray(new Codec[0]);
    }

    public static void openFile(File file, Codec[] codecs) {
        if (!openOrRead && file != null) {
            EnvironmentFrame[] frames = Universe.frames();
            if (frames != null) {
                for (EnvironmentFrame ef : frames) {
                    if (ef != null) {
                        Environment existingEnv = ef.findEnvironmentForFile(file);
                        if (existingEnv != null) {
                            ef.switchToEnvironment(existingEnv);
                            ef.toFront();
                            lastFile = file;
                            lastObject = existingEnv.getObject();
                            return;
                        }
                    }
                }
            }
        }

        ParseException lastException = null;
        for (int i = 0; i < codecs.length; i++) {
            try {
                Serializable object = codecs[i].decode(file, null);
                if (openOrRead && !(object instanceof TuringMachine)) {
                    JOptionPane.showMessageDialog(null,
                            "Only Turing Machine files can be added as building blocks.",
                            "Wrong File Type", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                lastObject = object;
                lastFile = file;
                if (!openOrRead) {
                    EnvironmentFrame frame = FrameFactory.createFrame(object);
                    if (frame == null) {
                        return;
                    }
                    frame.getEnvironment().setFile(file);
                    frame.getEnvironment().setEncoder(codecs[i].correspondingEncoder());
                    frame.refreshProjectTabs();
                }
                return;
            } catch (ParseException e) {
                lastException = e;
            }
        }
        if (codecs.length != 1) {
            lastException = new ParseException("No format could read the file!");
        }
        throw lastException;
    }

    public static boolean isApplicable(Object object) {
        return true;
    }

    public static Serializable getLastObjectOpened() {
        return lastObject;
    }

    public static File getLastFileOpened() {
        return lastFile;
    }

    public static boolean isOpened() {
        return lastFileOpened;
    }

    public static void setOpenOrRead(boolean flag) {
        openOrRead = flag;
    }
}
