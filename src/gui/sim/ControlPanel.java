package gui.sim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plus.JetBrainsUI;
import plus.Theme;
import plus.VSIcons;

public class ControlPanel extends JToolBar {
    private boolean blockStep = false;
    private ConfigurationController controller;
    private JToggleButton autoButton;
    private JButton stepBackBtn;
    private JSlider speedSlider;
    private JToggleButton[] speedPresetButtons;
    private JLabel statusBadgeLabel;
    private JLabel stepCountLabel;
    private Timer autoTimer;
    private boolean updatingSpeed = false;

    private static final String[] SPEED_LABELS = {"0.5x", "1x", "2x", "Max"};
    private static final int[] SPEED_DELAYS = {800, 400, 180, 45};

    public ControlPanel(ConfigurationController controller) {
        this.controller = controller;
        setFloatable(false);
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        initView();
    }

    public ConfigurationController getController() {
        return this.controller;
    }

    protected void initView() {
        removeAll();
        setLayout(new BorderLayout(12, 0));

        // 1. LEFT ZONE: Live Debugger Status Badge & Step Counter
        JPanel leftZone = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        leftZone.setOpaque(false);

        this.statusBadgeLabel = new JLabel("\u25CF Listo");
        this.statusBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.statusBadgeLabel.setForeground(Theme.ACCENT);

        this.stepCountLabel = new JLabel("Paso: 0");
        this.stepCountLabel.setFont(Theme.MONO_FONT);
        this.stepCountLabel.setForeground(Theme.TEXT_MUTED);

        leftZone.add(this.statusBadgeLabel);
        leftZone.add(JetBrainsUI.createToolbarSeparator());
        leftZone.add(this.stepCountLabel);

        // 2. CENTER ZONE: Centered Media Player Controls + Speed Selector
        JPanel centerZone = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        centerZone.setOpaque(false);

        // ⏮ Reiniciar (Reset)
        JButton resetBtn = new JButton("Reiniciar", VSIcons.get(VSIcons.MEDIA_RESET, 14, null));
        JetBrainsUI.styleGhostButton(resetBtn, "Reiniciar simulación al estado inicial (Reset)");
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAuto(null);
                controller.reset();
                refreshStatusUI();
            }
        });

        // ◀ Paso Atrás (Step Back)
        this.stepBackBtn = new JButton("Atrás", VSIcons.get(VSIcons.MEDIA_STEP_BACK, 14, null));
        JetBrainsUI.styleGhostButton(this.stepBackBtn, "Retroceder un paso en el historial (Step Back)");
        this.stepBackBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAuto(null);
                controller.stepBack();
                refreshStatusUI();
            }
        });

        // ▶ / ⏸ Reproducir / Pausa (Prominent Pill/Circular Auto Run Button)
        this.autoButton = new JToggleButton("Reproducir", VSIcons.get(VSIcons.MEDIA_PLAY, 14, Color.WHITE));
        JetBrainsUI.styleMediaPlayButton(this.autoButton, "Reproducir / Pausar ejecución automática (Auto Run)");

        // ▶❙ Paso Adelante (Step Forward)
        JButton stepForwardBtn = new JButton("Paso", VSIcons.get(VSIcons.MEDIA_STEP_FORWARD, 14, null));
        JetBrainsUI.styleGhostButton(stepForwardBtn, "Avanzar un paso de simulación (Step)");
        stepForwardBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAuto(null);
                controller.step(blockStep);
                refreshStatusUI();
            }
        });

        // ⏹ Detener (Stop)
        JButton stopBtn = new JButton("Detener", VSIcons.get(VSIcons.MEDIA_STOP, 14, new Color(229, 87, 101)));
        JetBrainsUI.styleGhostButton(stopBtn, "Detener ejecución automática (Stop)");
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAuto("Detenido");
            }
        });

        centerZone.add(resetBtn);
        centerZone.add(this.stepBackBtn);
        centerZone.add(this.autoButton);
        centerZone.add(stepForwardBtn);
        centerZone.add(stopBtn);
        centerZone.add(JetBrainsUI.createToolbarSeparator());

        // Speed Control: Quick Selector Pills (0.5x, 1x, 2x, Max) + Minimalist Slider
        JLabel speedTitle = new JLabel("Vel:");
        speedTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        speedTitle.setForeground(Theme.TEXT_MUTED);
        centerZone.add(speedTitle);

        this.speedPresetButtons = new JToggleButton[SPEED_LABELS.length];
        for (int i = 0; i < SPEED_LABELS.length; i++) {
            final int presetIdx = i;
            JToggleButton pb = new JToggleButton(SPEED_LABELS[i]);
            JetBrainsUI.styleGhostButton(pb, "Velocidad " + SPEED_LABELS[i] + " (" + SPEED_DELAYS[i] + " ms)");
            pb.setFont(new Font("Segoe UI", Font.BOLD, 11));
            pb.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
            if (i == 1) {
                pb.setSelected(true);
            }
            pb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applySpeedPreset(presetIdx);
                }
            });
            this.speedPresetButtons[i] = pb;
            centerZone.add(pb);
        }

        // Minimalist Slider (1..100 speed scale, mapped to delay 850ms..35ms)
        this.speedSlider = new JSlider(1, 100, 55);
        this.speedSlider.setUI(new JetBrainsUI.ModernSliderUI(this.speedSlider));
        this.speedSlider.setPreferredSize(new Dimension(84, 24));
        this.speedSlider.setOpaque(false);
        this.speedSlider.setToolTipText("Ajuste fino de velocidad de simulación");
        this.speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (updatingSpeed) {
                    return;
                }
                int val = speedSlider.getValue();
                int delay = Math.max(35, 850 - val * 8);
                autoTimer.setDelay(delay);
                autoTimer.setInitialDelay(delay);
                updatingSpeed = true;
                for (JToggleButton b : speedPresetButtons) {
                    b.setSelected(false);
                }
                updatingSpeed = false;
            }
        });
        centerZone.add(this.speedSlider);

        // 3. RIGHT ZONE: Grouped Secondary Actions (Trace, Freeze/Thaw, Focus Dropdown)
        JPanel rightZone = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightZone.setOpaque(false);

        JButton traceBtn = new JButton("Traza", VSIcons.get(VSIcons.MEDIA_TRACE, 14, null));
        JetBrainsUI.styleGhostButton(traceBtn, "Mostrar ventana de traza de la configuración (Trace)");
        traceBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.trace();
            }
        });
        rightZone.add(traceBtn);

        final JButton moreBtn = new JButton("Opciones", VSIcons.get(VSIcons.MEDIA_OPTIONS, 14, null));
        JetBrainsUI.styleGhostButton(moreBtn, "Acciones avanzadas de configuración (Focus, Freeze, Thaw, Remove)");
        moreBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu menu = new JPopupMenu();

                JMenuItem freezeItem = new JMenuItem("Congelar seleccionada (Freeze)",
                        VSIcons.get(VSIcons.MEDIA_FREEZE, 14, null));
                freezeItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        stopAuto(null);
                        controller.freeze();
                        refreshStatusUI();
                    }
                });
                menu.add(freezeItem);

                JMenuItem thawItem = new JMenuItem("Reanudar congelada (Thaw)",
                        VSIcons.get(VSIcons.MEDIA_PLAY, 14, Theme.RUN_GREEN));
                thawItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        stopAuto(null);
                        controller.thaw();
                        refreshStatusUI();
                    }
                });
                menu.add(thawItem);

                if (controller.isTuringMachine()) {
                    menu.addSeparator();
                    JMenuItem focusItem = new JMenuItem("Enfocar bloque (Focus)",
                            VSIcons.get(VSIcons.MEDIA_FOCUS, 14, null));
                    focusItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            stopAuto(null);
                            controller.focus();
                        }
                    });
                    menu.add(focusItem);

                    JMenuItem defocusItem = new JMenuItem("Salir de enfoque (Defocus)");
                    defocusItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            stopAuto(null);
                            controller.defocus();
                        }
                    });
                    menu.add(defocusItem);
                }

                menu.addSeparator();
                JMenuItem removeItem = new JMenuItem("Eliminar seleccionada (Remove)",
                        VSIcons.get(VSIcons.DELETE, 14, null));
                removeItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        stopAuto(null);
                        controller.remove();
                        refreshStatusUI();
                    }
                });
                menu.add(removeItem);

                JetBrainsUI.stylePopupMenu(menu);
                menu.show(moreBtn, 0, -menu.getPreferredSize().height - 4);
            }
        });
        rightZone.add(moreBtn);

        add(leftZone, BorderLayout.WEST);
        add(centerZone, BorderLayout.CENTER);
        add(rightZone, BorderLayout.EAST);

        // Auto-Run Timer (default 1x = 400ms)
        this.autoTimer = new Timer(400, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoTick();
            }
        });

        this.autoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoButton.isSelected()) {
                    autoButton.setText("Pausar");
                    autoButton.setIcon(VSIcons.get(VSIcons.MEDIA_PAUSE, 14, Color.WHITE));
                    statusBadgeLabel.setText("\u25B6 Ejecutando...");
                    statusBadgeLabel.setForeground(Theme.RUN_GREEN);
                    int status = controller.autoStatus();
                    if (status == 1) {
                        stopAuto("Aceptada");
                        return;
                    }
                    if (status == 2) {
                        stopAuto("Rechazada");
                        return;
                    }
                    autoTimer.start();
                } else {
                    stopAuto("Pausado");
                }
            }
        });

        this.controller.setStatusCallback(new Runnable() {
            @Override
            public void run() {
                refreshStatusUI();
            }
        });

        refreshStatusUI();
    }

    private void applySpeedPreset(int idx) {
        this.updatingSpeed = true;
        for (int i = 0; i < this.speedPresetButtons.length; i++) {
            this.speedPresetButtons[i].setSelected(i == idx);
            this.speedPresetButtons[i].repaint();
        }
        int delay = SPEED_DELAYS[idx];
        this.autoTimer.setDelay(delay);
        this.autoTimer.setInitialDelay(delay);
        int sliderVal = Math.max(1, Math.min(100, (850 - delay) / 8));
        this.speedSlider.setValue(sliderVal);
        this.updatingSpeed = false;
    }

    private void refreshStatusUI() {
        this.stepCountLabel.setText("Paso: " + this.controller.getStepCount());
        int status = this.controller.autoStatus();
        if (status == 1) {
            this.statusBadgeLabel.setText("\u2713 Aceptada");
            this.statusBadgeLabel.setForeground(Theme.RUN_GREEN);
        } else if (status == 2) {
            this.statusBadgeLabel.setText("\u2715 Rechazada");
            this.statusBadgeLabel.setForeground(new Color(229, 87, 101));
        } else if (!this.autoButton.isSelected()) {
            this.statusBadgeLabel.setText("\u25CF Listo");
            this.statusBadgeLabel.setForeground(Theme.ACCENT);
        }
    }

    private void stopAuto(String statusMessage) {
        if (this.autoTimer != null) {
            this.autoTimer.stop();
        }
        if (this.autoButton != null) {
            this.autoButton.setSelected(false);
            this.autoButton.setText("Reproducir");
            this.autoButton.setIcon(VSIcons.get(VSIcons.MEDIA_PLAY, 14, Color.WHITE));
        }
        this.stepCountLabel.setText("Paso: " + this.controller.getStepCount());
        if (statusMessage == null) {
            refreshStatusUI();
        } else if ("Aceptada".equals(statusMessage)) {
            this.statusBadgeLabel.setText("\u2713 Aceptada");
            this.statusBadgeLabel.setForeground(Theme.RUN_GREEN);
        } else if ("Rechazada".equals(statusMessage)) {
            this.statusBadgeLabel.setText("\u2715 Rechazada");
            this.statusBadgeLabel.setForeground(new Color(229, 87, 101));
        } else {
            this.statusBadgeLabel.setText("\u25CF " + statusMessage);
            this.statusBadgeLabel.setForeground(Theme.TEXT_MUTED);
        }
    }

    private void autoTick() {
        this.controller.step(this.blockStep);
        this.stepCountLabel.setText("Paso: " + this.controller.getStepCount());
        int status = this.controller.autoStatus();
        if (status == 1) {
            stopAuto("Aceptada");
        } else if (status == 2) {
            stopAuto("Rechazada");
        }
    }

    @Override
    public void removeNotify() {
        if (this.autoTimer != null) {
            this.autoTimer.stop();
        }
        super.removeNotify();
    }

    public void setBlock(boolean step) {
        this.blockStep = step;
    }
}
