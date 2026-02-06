package com.burpfox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.event.DocumentListener;

/**
 * Panel containing all Dalfox scan option checkboxes.
 * Uses proper encapsulation with getter methods.
 */
public class ScanOptionsPanel extends JPanel {
    
    // Configuration constants
    private static final int DEFAULT_WORKERS = 100;
    private static final int MAX_WORKERS = 500;
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int MAX_TIMEOUT = 120;
    private static final int MAX_DELAY = 10000;
    private static final int DEFAULT_SCAN_TIMEOUT = 30;
    private static final int MAX_SCAN_TIMEOUT = 120;
    
    // Detection
    private final JCheckBox cbContextAware;
    private final JCheckBox cbDeepDomXss;
    private final JCheckBox cbWafEvasion;
    private final JCheckBox cbFollowRedirects;
    private final JCheckBox cbFastScan;
    private final JCheckBox cbSkipDiscovery;
    private final JCheckBox cbSkipHeadless;
    
    // Mining
    private final JCheckBox cbMiningDict;
    private final JCheckBox cbMiningDom;
    private final JCheckBox cbSkipBav;
    private final JCheckBox cbSkipMiningAll;
    private final JCheckBox cbRemotePayloads;
    
    // Output
    private final JCheckBox cbNoColor;
    private final JCheckBox cbSilenceMode;
    private final JCheckBox cbReport;
    private final JComboBox<String> cmbPocType;
    
    // Advanced
    private final JSpinner spnWorkers;
    private final JSpinner spnTimeout;
    private final JSpinner spnDelay;
    private final JSpinner spnScanTimeout;
    private final JCheckBox cbProxy;
    private final JTextField txtProxy;
    private final JTextField txtIgnoreReturn;
    private final JCheckBox cbDebug;
    
    // Blind XSS
    private final JTextField txtBlindUrl;
    
    public ScanOptionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Group 1: Detection
        JPanel pnlDetection = new JPanel(new GridLayout(0, 3));
        pnlDetection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180)), "Detection"));
        pnlDetection.setBackground(new Color(240, 248, 255));
        
        cbContextAware = createCheckBox("Context Aware", new Color(240, 248, 255));
        cbDeepDomXss = createCheckBox("Deep DOM XSS", new Color(240, 248, 255));
        cbWafEvasion = createCheckBox("WAF Evasion", new Color(240, 248, 255));
        cbFollowRedirects = createCheckBox("Follow Redirects", new Color(240, 248, 255));
        cbFastScan = createCheckBox("Fast Scan", new Color(240, 248, 255));
        cbSkipDiscovery = createCheckBox("Skip Discovery", new Color(240, 248, 255));
        cbSkipHeadless = createCheckBox("Skip Headless", new Color(240, 248, 255));
        
        pnlDetection.add(cbContextAware);
        pnlDetection.add(cbDeepDomXss);
        pnlDetection.add(cbWafEvasion);
        pnlDetection.add(cbFollowRedirects);
        pnlDetection.add(cbFastScan);
        pnlDetection.add(cbSkipDiscovery);
        pnlDetection.add(cbSkipHeadless);
        
        // Group 2: Mining & Remote
        JPanel pnlMining = new JPanel(new GridLayout(0, 3));
        pnlMining.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 179, 113)), "Mining & Remote"));
        pnlMining.setBackground(new Color(240, 255, 240));
        
        cbMiningDict = createCheckBox("Mining Dict", new Color(240, 255, 240));
        cbMiningDom = createCheckBox("Mining DOM", new Color(240, 255, 240));
        cbSkipBav = createCheckBox("Skip BAV", new Color(240, 255, 240));
        cbSkipMiningAll = createCheckBox("Skip All Mining", new Color(240, 255, 240));
        cbRemotePayloads = createCheckBox("Remote Payloads", new Color(240, 255, 240));
        
        // Skip All Mining disables individual mining options
        cbSkipMiningAll.addActionListener(e -> {
            boolean skip = cbSkipMiningAll.isSelected();
            cbMiningDict.setEnabled(!skip);
            cbMiningDom.setEnabled(!skip);
            if (skip) {
                cbMiningDict.setSelected(false);
                cbMiningDom.setSelected(false);
            }
        });
        
        pnlMining.add(cbMiningDict);
        pnlMining.add(cbMiningDom);
        pnlMining.add(cbSkipBav);
        pnlMining.add(cbSkipMiningAll);
        pnlMining.add(cbRemotePayloads);
        
        // Group 3: Output
        JPanel pnlOutput = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        pnlOutput.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 140, 0)), "Output"));
        pnlOutput.setBackground(new Color(255, 250, 240));
        
        cbNoColor = createCheckBox("No Color", new Color(255, 250, 240));
        cbNoColor.setSelected(true);
        cbSilenceMode = createCheckBox("Silence", new Color(255, 250, 240));
        cbReport = createCheckBox("Report", new Color(255, 250, 240));
        
        cmbPocType = new JComboBox<>(new String[]{"plain", "curl", "httpie", "http-request"});
        JLabel lblPoc = new JLabel("PoC Type:");
        
        pnlOutput.add(cbNoColor);
        pnlOutput.add(cbSilenceMode);
        pnlOutput.add(cbReport);
        pnlOutput.add(lblPoc);
        pnlOutput.add(cmbPocType);
        
        // Group 4: Advanced
        JPanel pnlAdvanced = new JPanel(new GridBagLayout());
        pnlAdvanced.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(138, 43, 226)), "Advanced"));
        pnlAdvanced.setBackground(new Color(248, 240, 255));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: Workers, Timeout, Delay, Scan Timeout
        spnWorkers = new JSpinner(new SpinnerNumberModel(DEFAULT_WORKERS, 1, MAX_WORKERS, 10));
        spnTimeout = new JSpinner(new SpinnerNumberModel(DEFAULT_TIMEOUT, 1, MAX_TIMEOUT, 5));
        spnDelay = new JSpinner(new SpinnerNumberModel(0, 0, MAX_DELAY, 100));
        spnScanTimeout = new JSpinner(new SpinnerNumberModel(DEFAULT_SCAN_TIMEOUT, 1, MAX_SCAN_TIMEOUT, 5));
        
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0; pnlAdvanced.add(new JLabel("Workers:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.2; pnlAdvanced.add(spnWorkers, gbc);
        gbc.gridx = 2; gbc.weightx = 0; pnlAdvanced.add(new JLabel("Timeout(s):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.2; pnlAdvanced.add(spnTimeout, gbc);
        gbc.gridx = 4; gbc.weightx = 0; pnlAdvanced.add(new JLabel("Delay(ms):"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.2; pnlAdvanced.add(spnDelay, gbc);
        gbc.gridx = 6; gbc.weightx = 0; pnlAdvanced.add(new JLabel("Scan Timeout(m):"), gbc);
        gbc.gridx = 7; gbc.weightx = 0.2; pnlAdvanced.add(spnScanTimeout, gbc);
        
        // Row 2: Proxy toggle + text, Ignore Return, Debug
        cbProxy = createCheckBox("Proxy:", new Color(248, 240, 255));
        txtProxy = new JTextField("http://127.0.0.1:8080", 18);
        txtProxy.setEnabled(false);
        
        cbProxy.addActionListener(e -> txtProxy.setEnabled(cbProxy.isSelected()));
        
        txtIgnoreReturn = new JTextField(8);
        txtIgnoreReturn.setToolTipText("e.g. 302,403,404");
        cbDebug = createCheckBox("Debug", new Color(248, 240, 255));
        
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0; pnlAdvanced.add(cbProxy, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 0.5; pnlAdvanced.add(txtProxy, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 3; gbc.weightx = 0; pnlAdvanced.add(new JLabel("Ignore:"), gbc);
        gbc.gridx = 4; gbc.weightx = 0.3; pnlAdvanced.add(txtIgnoreReturn, gbc);
        gbc.gridx = 5; gbc.weightx = 0; pnlAdvanced.add(cbDebug, gbc);
        
        // Group 5: Blind XSS
        JPanel pnlBlind = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBlind.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 20, 60)), "Blind XSS"));
        pnlBlind.setBackground(new Color(255, 240, 245));
        txtBlindUrl = new JTextField(30);
        txtBlindUrl.setToolTipText("Your XSS Hunter or callback URL");
        pnlBlind.add(new JLabel("Callback URL:"));
        pnlBlind.add(txtBlindUrl);
        
        add(pnlDetection);
        add(pnlMining);
        add(pnlOutput);
        add(pnlAdvanced);
        add(pnlBlind);
    }
    
    private JCheckBox createCheckBox(String text, Color bg) {
        JCheckBox cb = new JCheckBox(text);
        cb.setBackground(bg);
        return cb;
    }
    
    // ========== GETTER METHODS (Encapsulation) ==========
    
    // Detection
    public boolean isContextAwareSelected() { return cbContextAware.isSelected(); }
    public boolean isDeepDomXssSelected() { return cbDeepDomXss.isSelected(); }
    public boolean isWafEvasionSelected() { return cbWafEvasion.isSelected(); }
    public boolean isFollowRedirectsSelected() { return cbFollowRedirects.isSelected(); }
    public boolean isFastScanSelected() { return cbFastScan.isSelected(); }
    public boolean isSkipDiscoverySelected() { return cbSkipDiscovery.isSelected(); }
    public boolean isSkipHeadlessSelected() { return cbSkipHeadless.isSelected(); }
    
    // Mining
    public boolean isMiningDictSelected() { return cbMiningDict.isSelected(); }
    public boolean isMiningDomSelected() { return cbMiningDom.isSelected(); }
    public boolean isSkipBavSelected() { return cbSkipBav.isSelected(); }
    public boolean isSkipMiningAllSelected() { return cbSkipMiningAll.isSelected(); }
    public boolean isRemotePayloadsSelected() { return cbRemotePayloads.isSelected(); }
    
    // Output
    public boolean isNoColorSelected() { return cbNoColor.isSelected(); }
    public boolean isSilenceModeSelected() { return cbSilenceMode.isSelected(); }
    public boolean isReportSelected() { return cbReport.isSelected(); }
    public boolean isDebugSelected() { return cbDebug.isSelected(); }
    public String getSelectedPocType() { return (String) cmbPocType.getSelectedItem(); }
    
    // Advanced
    public int getWorkersValue() { return (Integer) spnWorkers.getValue(); }
    public int getTimeoutValue() { return (Integer) spnTimeout.getValue(); }
    public int getDelayValue() { return (Integer) spnDelay.getValue(); }
    public int getScanTimeoutMinutes() { return (Integer) spnScanTimeout.getValue(); }
    public String getIgnoreReturnValue() { return txtIgnoreReturn.getText().trim(); }
    
    /**
     * Returns proxy value only if proxy is enabled.
     */
    public String getProxyValue() {
        if (cbProxy.isSelected()) {
            return txtProxy.getText().trim();
        }
        return "";
    }
    
    // Blind XSS
    public String getBlindUrlValue() { return txtBlindUrl.getText().trim(); }
    
    /**
     * Adds an ActionListener to all checkboxes.
     */
    public void addUpdateListener(ActionListener listener) {
        cbContextAware.addActionListener(listener);
        cbDeepDomXss.addActionListener(listener);
        cbWafEvasion.addActionListener(listener);
        cbFollowRedirects.addActionListener(listener);
        cbFastScan.addActionListener(listener);
        cbSkipDiscovery.addActionListener(listener);
        cbSkipHeadless.addActionListener(listener);
        cbMiningDict.addActionListener(listener);
        cbMiningDom.addActionListener(listener);
        cbSkipBav.addActionListener(listener);
        cbSkipMiningAll.addActionListener(listener);
        cbRemotePayloads.addActionListener(listener);
        cbNoColor.addActionListener(listener);
        cbSilenceMode.addActionListener(listener);
        cbReport.addActionListener(listener);
        cbDebug.addActionListener(listener);
        cbProxy.addActionListener(listener);
        cmbPocType.addActionListener(listener);
        spnWorkers.addChangeListener(e -> listener.actionPerformed(null));
        spnTimeout.addChangeListener(e -> listener.actionPerformed(null));
        spnDelay.addChangeListener(e -> listener.actionPerformed(null));
        spnScanTimeout.addChangeListener(e -> listener.actionPerformed(null));
    }
    
    /**
     * Adds DocumentListener to text fields.
     */
    public void addDocumentListener(DocumentListener listener) {
        txtProxy.getDocument().addDocumentListener(listener);
        txtIgnoreReturn.getDocument().addDocumentListener(listener);
        txtBlindUrl.getDocument().addDocumentListener(listener);
    }
}
