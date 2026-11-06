package gui.action;

import gui.environment.Environment;
import gui.environment.tag.PermanentTag;
import gui.environment.tag.Tag;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plus.JetBrainsUI;
import plus.VSIcons;

public class CloseButton extends JButton {
    private Environment env;

    public CloseButton(Environment environment) {
        super();
        setDefaults();
        this.env = environment;
        this.env.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                checkEnabled();
            }
        });
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                env.remove(env.getActive());
            }
        });
        checkEnabled();
    }

    public void setDefaults() {
        setIcon(VSIcons.get(VSIcons.DELETE, 13, null));
        setText("Cerrar Pestaña");
        JetBrainsUI.styleGhostButton(this, "Cerrar pestaña activa (Dismiss Tab)");
        setPreferredSize(new Dimension(118, 28));
    }

    private void checkEnabled() {
        if (this.env == null || this.env.tabbed == null) {
            return;
        }
        Tag tag = this.env.getTag(this.env.getActive());
        boolean canDismiss = this.env.tabbed.getTabCount() > 1 && !(tag instanceof PermanentTag);
        setEnabled(canDismiss);
        setVisible(canDismiss);
    }
}
