package com.burpfox.ui;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import com.burpfox.core.DalfoxCommandBuilder;
import com.burpfox.core.DalfoxRunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration dialog for Dalfox scans.
 */
public class ScanConfigDialog extends JDialog {
    private final HttpRequestResponse requestResponse;
    private final BurpFoxTab mainTab;
    
    private final List<JCheckBox> paramCheckBoxes = new ArrayList<>();
    private final JTextArea commandPreviewArea;
    private final JComboBox<String> cmbScanMode;
    private final JLabel lblMethod;
    private final JTextField txtTriggerUrl;
    private final JPanel triggerLine;
    private final ScanOptionsPanel optionsPanel;

    public ScanConfigDialog(HttpRequestResponse requestResponse, BurpFoxTab mainTab) {
        this.requestResponse = requestResponse;
        this.mainTab = mainTab;

        setTitle("BurpFox Configuration");
        setSize(850, 850);
        setLocationRelativeTo(null);
        setModal(true);
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);
        
        // Target Line
        JPanel targetLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lblMethod = new JLabel(requestResponse.request().method());
        lblMethod.setFont(lblMethod.getFont().deriveFont(Font.BOLD));
        targetLine.add(new JLabel("Method: "));
        targetLine.add(lblMethod);
        targetLine.add(new JLabel("  URL: " + requestResponse.request().url())); 
        topPanel.add(targetLine, gbc);

        // Scan Mode
        gbc.gridy++;
        JPanel modeLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cmbScanMode = new JComboBox<>(new String[]{"Reflected XSS (url)", "Stored XSS (sxss)"});
        modeLine.add(new JLabel("Scan Mode: "));
        modeLine.add(cmbScanMode);
        topPanel.add(modeLine, gbc);

        // Trigger URL
        gbc.gridy++;
        triggerLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        txtTriggerUrl = new JTextField(30);
        triggerLine.add(new JLabel("Trigger URL: "));
        triggerLine.add(txtTriggerUrl);
        triggerLine.setVisible(false);
        topPanel.add(triggerLine, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        topPanel.add(new JLabel("Select parameters to scan:"), gbc);
        
        add(topPanel, BorderLayout.NORTH);

        // Listeners
        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateCommandPreview(); }
            public void removeUpdate(DocumentEvent e) { updateCommandPreview(); }
            public void changedUpdate(DocumentEvent e) { updateCommandPreview(); }
        };
        txtTriggerUrl.getDocument().addDocumentListener(docListener);
        
        cmbScanMode.addActionListener(e -> {
            boolean isStored = cmbScanMode.getSelectedIndex() == 1;
            triggerLine.setVisible(isStored);
            topPanel.revalidate();
            topPanel.repaint();
            updateCommandPreview();
        });

        // Parameters Panel
        JPanel paramsPanel = new JPanel();
        paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
        paramsPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        List<ParsedHttpParameter> parameters = requestResponse.request().parameters();
        boolean hasParams = false;
        
        for (ParsedHttpParameter param : parameters) {
            if(param.type().name().equals("URL") || param.type().name().equals("BODY")) {
                JCheckBox cb = new JCheckBox(param.name() + " (" + param.type().name() + ")");
                cb.setSelected(true);
                cb.putClientProperty("paramName", param.name());
                cb.addActionListener(e -> updateCommandPreview());
                paramCheckBoxes.add(cb);
                paramsPanel.add(cb);
                hasParams = true;
            }
        }
        
        if (!hasParams) {
            paramsPanel.add(new JLabel("<html><i>No parameters found. Dalfox will scan the full URL.</i></html>"));
        }

        JScrollPane scrollParams = new JScrollPane(paramsPanel);
        
        // Options Panel
        optionsPanel = new ScanOptionsPanel();
        optionsPanel.addUpdateListener(e -> updateCommandPreview());
        optionsPanel.addDocumentListener(docListener);

        // Command Preview
        commandPreviewArea = new JTextArea(5, 40);
        commandPreviewArea.setLineWrap(true);
        commandPreviewArea.setWrapStyleWord(true);
        commandPreviewArea.setEditable(false);
        commandPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollCmd = new JScrollPane(commandPreviewArea);
        scrollCmd.setBorder(BorderFactory.createTitledBorder("Command Preview"));
        
        // Bottom Container
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(optionsPanel, BorderLayout.NORTH);
        bottomContainer.add(scrollCmd, BorderLayout.CENTER);
        
        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollParams, bottomContainer);
        splitPane.setResizeWeight(0.25);
        add(splitPane, BorderLayout.CENTER);

        // Initial Update
        updateCommandPreview();

        // Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Cancel");
        JButton btnScan = new JButton("Start Scan");

        btnCancel.addActionListener(e -> dispose());
        btnScan.addActionListener(e -> {
            // Validate: Stored XSS requires Trigger URL
            if (cmbScanMode.getSelectedIndex() == 1 && txtTriggerUrl.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Stored XSS mode requires a Trigger URL.\nPlease enter the URL where the XSS payload will be triggered.",
                    "Trigger URL Required",
                    JOptionPane.WARNING_MESSAGE);
                txtTriggerUrl.requestFocus();
                return;
            }
            startScan();
            dispose();
        });

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnScan);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void startScan() {
        String targetUrl = requestResponse.request().url();
        String method = requestResponse.request().method();

        StringBuilder requestDetails = new StringBuilder();
        requestDetails.append(method).append(" ").append(targetUrl).append("\n\n");
        for (var header : requestResponse.request().headers()) {
            requestDetails.append(header.name()).append(": ").append(header.value()).append("\n");
        }
        requestDetails.append("\n").append(requestResponse.request().bodyToString());

        boolean noColor = optionsPanel.isNoColorSelected();
        int scanTimeoutMinutes = optionsPanel.getScanTimeoutMinutes();
        
        // Build command args using builder
        List<String> commandArgs = createBuilder().buildArgs();
        
        // Start via Tab (which handles Runner creation and references)
        DalfoxRunner runner = mainTab.initScan(
            "Scan", 
            requestDetails.toString(), 
            noColor, 
            commandArgs, 
            method, 
            targetUrl,
            scanTimeoutMinutes
        );
        
        runner.start();
    }

    /**
     * Creates a configured DalfoxCommandBuilder based on current UI state.
     * Single source of truth - used by both command execution and preview.
     */
    private DalfoxCommandBuilder createBuilder() {
        List<String> selectedParams = paramCheckBoxes.stream()
                .filter(JCheckBox::isSelected)
                .map(cb -> (String) cb.getClientProperty("paramName"))
                .collect(Collectors.toList());

        return new DalfoxCommandBuilder(requestResponse, selectedParams)
                .storedXssMode(cmbScanMode.getSelectedIndex() == 1)
                .triggerUrl(txtTriggerUrl.getText().trim())
                // Output
                .noColor(optionsPanel.isNoColorSelected())
                .silenceMode(optionsPanel.isSilenceModeSelected())
                .report(optionsPanel.isReportSelected())
                .pocType(optionsPanel.getSelectedPocType())
                // Detection
                .contextAware(optionsPanel.isContextAwareSelected())
                .deepDomXss(optionsPanel.isDeepDomXssSelected())
                .wafEvasion(optionsPanel.isWafEvasionSelected())
                .followRedirects(optionsPanel.isFollowRedirectsSelected())
                .fastScan(optionsPanel.isFastScanSelected())
                .skipDiscovery(optionsPanel.isSkipDiscoverySelected())
                .skipHeadless(optionsPanel.isSkipHeadlessSelected())
                // Mining
                .skipBav(optionsPanel.isSkipBavSelected())
                .skipMiningAll(optionsPanel.isSkipMiningAllSelected())
                .miningDict(optionsPanel.isMiningDictSelected())
                .miningDom(optionsPanel.isMiningDomSelected())
                .remotePayloads(optionsPanel.isRemotePayloadsSelected())
                // Advanced
                .workers(optionsPanel.getWorkersValue())
                .timeout(optionsPanel.getTimeoutValue())
                .delay(optionsPanel.getDelayValue())
                .proxy(optionsPanel.getProxyValue())
                .ignoreReturn(optionsPanel.getIgnoreReturnValue())
                .debug(optionsPanel.isDebugSelected())
                // Blind XSS
                .blindUrl(optionsPanel.getBlindUrlValue());
    }

    private void updateCommandPreview() {
        commandPreviewArea.setText(createBuilder().buildPreviewString());
    }
}

