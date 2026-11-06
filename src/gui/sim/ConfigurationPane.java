package gui.sim;

import automata.Automaton;
import automata.Configuration;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import plus.Theme;

public class ConfigurationPane extends JPanel implements ActionListener {
    private Automaton automaton;
    private HashMap<Configuration, ConfigurationButton> configurationToButtonMap = new LinkedHashMap<Configuration, ConfigurationButton>();
    private HashSet<Configuration> selected = new HashSet<Configuration>();
    private transient HashSet<ConfigurationSelectionListener> selectionListeners = new HashSet<ConfigurationSelectionListener>();

    public ConfigurationPane(Automaton automaton) {
        this.automaton = automaton;
        setBackground(Theme.SURFACE);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setLayout(new java.awt.GridLayout(0, 1, 10, 8));
    }

    private void updateGridColumns() {
        int count = this.configurationToButtonMap.size();
        int desiredCols = (this.automaton instanceof automata.turing.TuringMachine || count <= 1) ? 1 : 2;
        if (getLayout() instanceof java.awt.GridLayout) {
            java.awt.GridLayout gl = (java.awt.GridLayout) getLayout();
            if (gl.getColumns() != desiredCols) {
                setLayout(new java.awt.GridLayout(0, desiredCols, 10, 8));
            }
        }
    }

    public void add(Configuration configuration) {
        add(configuration, ConfigurationButton.NORMAL);
    }

    public void add(Configuration configuration, int state) {
        if (contains(configuration)) {
            return;
        }
        ConfigurationButton button = new ConfigurationButton(configuration, state);
        this.configurationToButtonMap.put(configuration, button);
        updateGridColumns();
        add(button);
        button.addActionListener(this);
    }

    public ConfigurationButton getButton(Configuration configuration) {
        return this.configurationToButtonMap.get(configuration);
    }

    public int getState(Configuration configuration) {
        ConfigurationButton btn = this.configurationToButtonMap.get(configuration);
        return btn != null ? btn.state : ConfigurationButton.NORMAL;
    }

    public boolean contains(Configuration configuration) {
        return this.configurationToButtonMap.containsKey(configuration);
    }

    public void setReject(Configuration configuration) {
        ConfigurationButton button = this.configurationToButtonMap.get(configuration);
        if (button == null) {
            return;
        }
        if (button.state == ConfigurationButton.NORMAL) {
            button.setState(ConfigurationButton.REJECT);
        }
        button.doClick();
    }

    public void setFrozen(Configuration configuration) {
        ConfigurationButton button = this.configurationToButtonMap.get(configuration);
        if (button == null) {
            return;
        }
        if (button.state == ConfigurationButton.NORMAL) {
            button.setState(ConfigurationButton.FREEZE);
        }
        button.doClick();
    }

    public void setFocused(Configuration configuration) {
        ConfigurationButton button = this.configurationToButtonMap.get(configuration);
        if (button == null) {
            return;
        }
        if (button.state == ConfigurationButton.NORMAL) {
            button.setState(ConfigurationButton.FOCUSED);
            button.doClick();
        }
    }

    public void defocus(Configuration configuration) {
        setNormal(configuration);
        Configuration curr = configuration;
        curr.setFocused(false);
        while (curr.getParent() != null) {
            curr = curr.getParent();
            curr.setFocused(false);
        }
    }

    public void setNormal(Configuration configuration) {
        ConfigurationButton button = this.configurationToButtonMap.get(configuration);
        if (button == null) {
            return;
        }
        if (button.state == ConfigurationButton.FREEZE || button.state == ConfigurationButton.FOCUSED) {
            button.setState(ConfigurationButton.NORMAL);
        }
        button.doClick();
    }

    public void remove(Configuration configuration) {
        Component comp = this.configurationToButtonMap.remove(configuration);
        if (comp == null) {
            return;
        }
        this.selected.remove(configuration);
        remove(comp);
    }

    public void clear() {
        this.configurationToButtonMap.clear();
        this.selected.clear();
        super.removeAll();
    }

    public void deselectAll() {
        for (ConfigurationButton btn : this.configurationToButtonMap.values()) {
            btn.setSelected(false);
        }
        this.selected.clear();
    }

    public Configuration[] getSelected() {
        return this.selected.toArray(new Configuration[0]);
    }

    public Configuration[] getConfigurations() {
        return this.configurationToButtonMap.keySet().toArray(new Configuration[0]);
    }

    public Configuration[] getValidConfigurations() {
        ArrayList<Configuration> list = new ArrayList<Configuration>();
        Iterator<ConfigurationButton> it = this.configurationToButtonMap.values().iterator();
        while (it.hasNext()) {
            ConfigurationButton button = it.next();
            if (button.state == ConfigurationButton.NORMAL || button.state == ConfigurationButton.FOCUSED) {
                list.add(button.getConfiguration());
            }
        }
        return list.toArray(new Configuration[0]);
    }

    public void clearFinal() {
        ArrayList<ConfigurationButton> list = new ArrayList<ConfigurationButton>();
        list.addAll(this.configurationToButtonMap.values());
        for (ConfigurationButton button : list) {
            if (button.state == ConfigurationButton.ACCEPT || button.state == ConfigurationButton.REJECT) {
                remove(button.getConfiguration());
            }
        }
    }

    public void clearThawed() {
        ArrayList<ConfigurationButton> list = new ArrayList<ConfigurationButton>();
        list.addAll(this.configurationToButtonMap.values());
        for (ConfigurationButton button : list) {
            if (button.state != ConfigurationButton.FREEZE) {
                remove(button.getConfiguration());
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ConfigurationButton button;
        try {
            button = (ConfigurationButton) e.getSource();
        } catch (ClassCastException ex) {
            return;
        }
        Configuration config = button.getConfiguration();
        if (!this.configurationToButtonMap.containsKey(config)) {
            return;
        }
        if (button.isSelected()) {
            this.selected.add(config);
        } else {
            this.selected.remove(config);
        }
        distributeSelectionEvent(new ConfigurationSelectionEvent(this));
    }

    public void addSelectionListener(ConfigurationSelectionListener listener) {
        this.selectionListeners.add(listener);
    }

    public void removeSelectionListener(ConfigurationSelectionListener listener) {
        this.selectionListeners.remove(listener);
    }

    void distributeSelectionEvent(ConfigurationSelectionEvent event) {
        for (ConfigurationSelectionListener listener : this.selectionListeners) {
            listener.configurationSelectionChange(event);
        }
    }
}
