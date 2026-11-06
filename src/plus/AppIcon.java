package plus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Generates the modern JetBrains-style "JFLAP Studio" vector icon in multiple resolutions
 * (16, 24, 32, 48, 64, 128, 256) for JFrames, JDialogs, the OS Taskbar, and .ico export.
 */
public final class AppIcon {
    private static List<Image> cachedIcons;
    private static boolean taskbarApplied = false;

    private AppIcon() {
    }

    public static synchronized List<Image> getIcons() {
        if (cachedIcons == null) {
            int[] sizes = new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256};
            cachedIcons = new ArrayList<Image>(sizes.length);
            for (int sz : sizes) {
                cachedIcons.add(renderIcon(sz));
            }
        }
        return cachedIcons;
    }

    public static BufferedImage getIcon(int size) {
        return renderIcon(size);
    }

    public static ImageIcon getImageIcon(int size) {
        return new ImageIcon(renderIcon(size));
    }

    /**
     * Applies the JFLAP Studio multi-resolution icon to any Window (JFrame, JDialog)
     * and sets the Windows/macOS taskbar icon.
     */
    public static void apply(Window window) {
        if (window != null) {
            try {
                window.setIconImages(getIcons());
            } catch (Throwable ignored) {
            }
        }
        if (!taskbarApplied) {
            taskbarApplied = true;
            try {
                Class<?> taskbarCls = Class.forName("java.awt.Taskbar");
                Method isSupported = taskbarCls.getMethod("isTaskbarSupported");
                if (Boolean.TRUE.equals(isSupported.invoke(null))) {
                    Object taskbar = taskbarCls.getMethod("getTaskbar").invoke(null);
                    Method setIcon = taskbarCls.getMethod("setIconImage", Image.class);
                    setIcon.invoke(taskbar, renderIcon(256));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Renders a crisp JetBrains Fleet / IntelliJ New UI style icon for JFLAP Studio:
     * - Rounded squircle background (#181A1F -> #232730) with vibrant #3574F0 -> #4CC2FF accent badge
     * - Connected automaton state graph (initial arrow -> q0 -> final state q1)
     */
    public static BufferedImage renderIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float s = size;
        float pad = Math.max(1.0f, s * 0.05f);
        float box = s - pad * 2.0f;
        float arc = box * 0.26f;

        // 1. Outer vibrant gradient squircle
        GradientPaint outerGrad = new GradientPaint(
                pad, pad, new Color(0x35, 0x74, 0xF0),
                pad + box, pad + box, new Color(0x22, 0xB8, 0xCF));
        g2.setPaint(outerGrad);
        g2.fill(new RoundRectangle2D.Float(pad, pad, box, box, arc, arc));

        // 2. Inner dark island squircle (with glowing border effect)
        float borderW = Math.max(1.2f, s * 0.065f);
        float inX = pad + borderW;
        float inY = pad + borderW;
        float inBox = box - borderW * 2.0f;
        float inArc = Math.max(2.0f, arc - borderW * 0.8f);

        GradientPaint innerGrad = new GradientPaint(
                inX, inY, new Color(0x1E, 0x20, 0x26),
                inX, inY + inBox, new Color(0x14, 0x15, 0x19));
        g2.setPaint(innerGrad);
        g2.fill(new RoundRectangle2D.Float(inX, inY, inBox, inBox, inArc, inArc));

        // 3. Subtle top-left blue glow inside the dark squircle
        GradientPaint glow = new GradientPaint(
                inX, inY, new Color(53, 116, 240, 75),
                inX + inBox * 0.7f, inY + inBox * 0.7f, new Color(53, 116, 240, 0));
        g2.setPaint(glow);
        g2.fill(new RoundRectangle2D.Float(inX, inY, inBox, inBox, inArc, inArc));

        // 4. Draw stylized automaton transition curve + nodes
        float c1x = s * 0.33f;
        float c1y = s * 0.62f;
        float c2x = s * 0.69f;
        float c2y = s * 0.36f;
        float r1 = s * 0.135f;
        float r2 = s * 0.155f;

        // Connecting curved transition arc
        g2.setColor(new Color(0x4C, 0xC2, 0xFF));
        g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.065f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        CubicCurve2D.Float curve = new CubicCurve2D.Float(
                c1x, c1y,
                s * 0.32f, s * 0.30f,
                s * 0.50f, s * 0.32f,
                c2x, c2y);
        g2.draw(curve);

        // Node 1 (Initial State on bottom-left)
        g2.setColor(new Color(0x26, 0x29, 0x32));
        g2.fill(new Ellipse2D.Float(c1x - r1, c1y - r1, r1 * 2, r1 * 2));
        g2.setColor(new Color(0x35, 0x74, 0xF0));
        g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.055f)));
        g2.draw(new Ellipse2D.Float(c1x - r1, c1y - r1, r1 * 2, r1 * 2));

        // Node 2 (Accepting Final State on top-right with double ring in cyan/green)
        g2.setColor(new Color(0x20, 0x3A, 0x52));
        g2.fill(new Ellipse2D.Float(c2x - r2, c2y - r2, r2 * 2, r2 * 2));
        g2.setColor(new Color(0x4C, 0xC2, 0xFF));
        g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.058f)));
        g2.draw(new Ellipse2D.Float(c2x - r2, c2y - r2, r2 * 2, r2 * 2));

        if (size >= 24) {
            float innerR2 = r2 * 0.64f;
            g2.setColor(new Color(0x3C, 0xD0, 0x70));
            g2.setStroke(new BasicStroke(Math.max(1.0f, s * 0.042f)));
            g2.draw(new Ellipse2D.Float(c2x - innerR2, c2y - innerR2, innerR2 * 2, innerR2 * 2));
        }

        // Bottom-right "JF" badge for sizes >= 32
        if (size >= 32) {
            Font f = new Font("Segoe UI", Font.BOLD, Math.max(9, Math.round(s * 0.22f)));
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String txt = "JF";
            int tw = fm.stringWidth(txt);
            int tx = Math.round(s * 0.68f - tw / 2.0f);
            int ty = Math.round(s * 0.80f);
            g2.setColor(new Color(0xDF, 0xE1, 0xE5));
            g2.drawString(txt, tx, ty);
        }

        g2.dispose();
        return img;
    }

    /**
     * Writes a standard multi-resolution Windows .ico file containing PNG frames (16, 32, 48, 64, 256).
     */
    public static void writeIcoFile(File outFile) throws Exception {
        int[] sizes = new int[]{16, 32, 48, 64, 256};
        byte[][] pngData = new byte[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            BufferedImage img = renderIcon(sizes[i]);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            pngData[i] = baos.toByteArray();
        }

        FileOutputStream fos = new FileOutputStream(outFile);
        try {
            // ICONDIR header (6 bytes)
            writeLE16(fos, 0); // Reserved
            writeLE16(fos, 1); // Type = 1 (ICO)
            writeLE16(fos, sizes.length); // Count

            int offset = 6 + sizes.length * 16;
            for (int i = 0; i < sizes.length; i++) {
                int sz = sizes[i];
                int dimByte = (sz >= 256) ? 0 : sz;
                fos.write(dimByte); // Width
                fos.write(dimByte); // Height
                fos.write(0);       // ColorCount
                fos.write(0);       // Reserved
                writeLE16(fos, 1);  // Planes
                writeLE16(fos, 32); // BitCount
                writeLE32(fos, pngData[i].length); // BytesInRes
                writeLE32(fos, offset);            // ImageOffset
                offset += pngData[i].length;
            }
            for (int i = 0; i < sizes.length; i++) {
                fos.write(pngData[i]);
            }
        } finally {
            fos.close();
        }
    }

    private static void writeLE16(FileOutputStream fos, int val) throws Exception {
        fos.write(val & 0xFF);
        fos.write((val >>> 8) & 0xFF);
    }

    private static void writeLE32(FileOutputStream fos, int val) throws Exception {
        fos.write(val & 0xFF);
        fos.write((val >>> 8) & 0xFF);
        fos.write((val >>> 16) & 0xFF);
        fos.write((val >>> 24) & 0xFF);
    }
}
