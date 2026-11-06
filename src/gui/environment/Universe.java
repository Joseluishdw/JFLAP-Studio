package gui.environment;

import file.CodecRegistry;
import file.XMLCodec;
import gui.action.NewAction;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import plus.ModernFileChooser;

public class Universe {
    private static Map<Environment, EnvironmentFrame> environmentToFrame = new HashMap<Environment, EnvironmentFrame>();
    private static Map<String, EnvironmentFrame> fileToFrame = new HashMap<String, EnvironmentFrame>();
    public static JFileChooser CHOOSER = null;
    private static int numberRegistered = 0;

    private static FileChangeListener FILE_LISTENER = new FileChangeListener() {
        @Override
        public void fileChanged(FileChangeEvent e) {
            File oldFile = e.getOldFile();
            EnvironmentFrame frame = Universe.frameForEnvironment((Environment) e.getSource());
            if (oldFile != null) {
                fileToFrame.remove(getPath(oldFile));
            }
            Environment env = (Environment) e.getSource();
            File newFile = env.getFile();
            if (newFile == null) {
                return;
            }
            fileToFrame.put(getPath(newFile), frame);
        }
    };

    public static final CodecRegistry CODEC_REGISTRY = new CodecRegistry();
    public static Profile curProfile = new Profile();

    static {
        try {
            CHOOSER = new ModernFileChooser.ModernJFileChooser(System.getProperties().getProperty("user.dir"));
        } catch (SecurityException ignored) {
        }
        XMLCodec xc = new XMLCodec();
        CODEC_REGISTRY.add(xc);
    }

    private Universe() {
    }

    private static String getPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static int registerFrame(EnvironmentFrame frame) {
        Environment env = frame.getEnvironment();
        environmentToFrame.put(env, frame);
        File file = env.getFile();
        if (file != null) {
            fileToFrame.put(getPath(file), frame);
        }
        env.addFileChangeListener(FILE_LISTENER);
        NewAction.hideNew();
        return ++numberRegistered;
    }

    public static void unregisterFrame(EnvironmentFrame frame) {
        try {
            fileToFrame.remove(getPath(frame.getEnvironment().getFile()));
        } catch (NullPointerException ignored) {
        }
        environmentToFrame.remove(frame.getEnvironment());
        if (numberOfFrames() == 0) {
            NewAction.showNew();
        }
    }

    public static EnvironmentFrame frameForFile(File file) {
        if (file == null) {
            return null;
        }
        return fileToFrame.get(getPath(file));
    }

    public static EnvironmentFrame frameForEnvironment(Environment environment) {
        return environmentToFrame.get(environment);
    }

    public static EnvironmentFrame[] frames() {
        return environmentToFrame.values().toArray(new EnvironmentFrame[0]);
    }

    public static int numberOfFrames() {
        return environmentToFrame.size();
    }
}
