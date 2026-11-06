using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Windows.Forms;

class JFLAPStudioLauncher {
    [DllImport("shell32.dll", SetLastError = true)]
    static extern void SetCurrentProcessExplicitAppUserModelID([MarshalAs(UnmanagedType.LPWStr)] string AppID);

    [STAThread]
    static void Main(string[] args) {
        try {
            SetCurrentProcessExplicitAppUserModelID("JFLAP.Studio.IDE.2026");
        } catch {}

        string appDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JFLAPStudio");
        try {
            Directory.CreateDirectory(appDir);
        } catch {}

        string extractedJar = Path.Combine(appDir, "JFLAP_Studio_Core.jar");
        bool usedEmbedded = false;

        try {
            Assembly asm = Assembly.GetExecutingAssembly();
            using (Stream resStream = asm.GetManifestResourceStream("JFLAP.jar")) {
                if (resStream != null) {
                    bool needExtract = !File.Exists(extractedJar) || (new FileInfo(extractedJar).Length != resStream.Length);
                    try {
                        DateTime exeTime = File.GetLastWriteTimeUtc(asm.Location);
                        if (File.Exists(extractedJar) && File.GetLastWriteTimeUtc(extractedJar) < exeTime) {
                            needExtract = true;
                        }
                    } catch {}

                    if (needExtract) {
                        string tempJar = extractedJar + ".tmp";
                        using (FileStream fs = new FileStream(tempJar, FileMode.Create, FileAccess.Write, FileShare.Read)) {
                            resStream.CopyTo(fs);
                        }
                        try {
                            if (File.Exists(extractedJar)) File.Delete(extractedJar);
                            File.Move(tempJar, extractedJar);
                        } catch {
                            // If another instance already has JFLAP_Studio_Core.jar open, use that existing file or tempJar
                            if (!File.Exists(extractedJar)) {
                                extractedJar = tempJar;
                            } else {
                                try { File.Delete(tempJar); } catch {}
                            }
                        }
                    }
                    usedEmbedded = File.Exists(extractedJar);
                }
            }
        } catch {}

        string jarPath = usedEmbedded ? extractedJar : null;
        if (jarPath == null) {
            string[] candidates = new string[] {
                Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "JFLAP.jar"),
                @"C:\Users\forma\Desktop\Trabajos\JFLAP\JFLAP.jar",
                @"C:\Users\forma\Desktop\JFLAP.jar",
                @"C:\Users\forma\Downloads\JFLAP.jar"
            };
            foreach (string c in candidates) {
                if (File.Exists(c)) { jarPath = c; break; }
            }
        }

        if (jarPath == null) {
            MessageBox.Show("No se encontró el núcleo de JFLAP Studio.", "JFLAP Studio", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return;
        }

        string javaw = "javaw.exe";
        string[] javaCandidates = new string[] {
            @"C:\Program Files\Java\jdk-22\bin\javaw.exe",
            @"C:\Program Files\Java\jdk-21\bin\javaw.exe",
            @"C:\Program Files\Java\jdk-17\bin\javaw.exe"
        };
        foreach (string jc in javaCandidates) {
            if (File.Exists(jc)) { javaw = jc; break; }
        }

        string workDir = @"C:\Users\forma\Desktop\Trabajos\JFLAP";
        if (!Directory.Exists(workDir)) {
            workDir = Path.GetDirectoryName(jarPath);
        }

        string argStr = "-Dsun.java2d.dpiaware=true -jar \"" + jarPath + "\"";
        if (args != null && args.Length > 0) {
            foreach (string a in args) {
                argStr += " \"" + a + "\"";
            }
        }

        ProcessStartInfo psi = new ProcessStartInfo(javaw, argStr);
        psi.WorkingDirectory = workDir;
        psi.UseShellExecute = false;
        Process.Start(psi);
    }
}
