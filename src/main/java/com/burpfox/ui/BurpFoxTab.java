package com.burpfox.ui;

import com.burpfox.core.DalfoxRunner;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main tab component for BurpFox extension.
 */
public class BurpFoxTab {
    private final JTabbedPane tabbedPane;
    private int scanCounter = 0;
    
    // Track active runners
    private final List<DalfoxRunner> activeRunners = new ArrayList<>();
    
    // Colors
    private static final Color HEADER_BG = new Color(45, 45, 45);
    private static final Color HEADER_FG = new Color(255, 120, 50);
    private static final Color REQUEST_BG = new Color(250, 250, 250);
    private static final Color REQUEST_FG = new Color(30, 30, 30);
    private static final Color OUTPUT_BG = new Color(30, 30, 35);
    private static final Color OUTPUT_FG = new Color(200, 200, 200);
    private static final Color VULN_COLOR = new Color(255, 85, 85);
    private static final Color INFO_COLOR = new Color(100, 200, 255);
    private static final Color SUCCESS_COLOR = new Color(80, 200, 120);

    public BurpFoxTab() {
        tabbedPane = new JTabbedPane();
    }

    public Component getUiComponent() {
        return tabbedPane;
    }

    /**
     * Stops all active scans. Called when extension is unloaded.
     */
    public void stopAllScans() {
        synchronized (activeRunners) {
            for (DalfoxRunner runner : activeRunners) {
                runner.stop();
            }
            activeRunners.clear();
        }
    }

    /**
     * Creates a new scan tab and returns a DalfoxRunner.
     * Uses argument list instead of shell command string for security.
     */
    public DalfoxRunner initScan(String title, String requestDetails, boolean noColor, 
                                  List<String> commandArgs, String method, String url, 
                                  int timeoutMinutes) {
        scanCounter++;
        String fullTitle = "Scan " + scanCounter;
        
        // --- UI Components ---
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_BG);
        headerPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        
        JLabel lblTitle = new JLabel("BurpFox Scan #" + scanCounter);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(HEADER_FG);
        
        JButton btnStop = new JButton("Stop Scan");
        btnStop.setForeground(Color.RED);
        btnStop.setFocusPainted(false);
        
        JLabel lblStatus = new JLabel("Running...");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblStatus.setForeground(new Color(150, 150, 150));
        
        JPanel paramsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        paramsRight.setOpaque(false);
        paramsRight.add(lblStatus);
        paramsRight.add(btnStop);
        
        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(paramsRight, BorderLayout.EAST);
        
        // Left: Request
        JTextArea requestArea = new JTextArea(requestDetails);
        requestArea.setEditable(false);
        requestArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        requestArea.setBackground(REQUEST_BG);
        requestArea.setForeground(REQUEST_FG);
        requestArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane requestScroll = new JScrollPane(requestArea);
        requestScroll.setBorder(BorderFactory.createTitledBorder("Request"));

        // Right: Output
        JTextPane outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputPane.setBackground(OUTPUT_BG);
        outputPane.setForeground(OUTPUT_FG);
        outputPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));
        
        // Split
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestScroll, outputScroll);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(5);
        
        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // --- Runner & Thread Safety ---
        
        // Logger logic
        java.util.function.Consumer<String> logger = text -> SwingUtilities.invokeLater(() -> {
            javax.swing.text.StyledDocument doc = outputPane.getStyledDocument();
            javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();
            Color textColor = OUTPUT_FG;
            
            if (!noColor) {
                if (text.contains("[POC]") || text.contains("[V]") || text.contains("VULN")) {
                    textColor = VULN_COLOR;
                } else if (text.contains("[I]") || text.contains("[*]")) {
                    textColor = INFO_COLOR;
                } else if (text.contains("Finished") || text.contains("SUCCESS")) {
                    textColor = SUCCESS_COLOR;
                    lblStatus.setText("Completed");
                    lblStatus.setForeground(SUCCESS_COLOR);
                    btnStop.setEnabled(false);
                } else if (text.contains("ERROR") || text.contains("[E]")) {
                    textColor = VULN_COLOR;
                    lblStatus.setText("Error");
                    lblStatus.setForeground(VULN_COLOR);
                    btnStop.setEnabled(false);
                }
            } else {
                if (text.contains("Finished") || text.contains("SUCCESS") || text.contains("ERROR")) {
                    lblStatus.setText("Finished");
                    btnStop.setEnabled(false);
                }
            }
            
            javax.swing.text.StyleConstants.setForeground(attrs, textColor);
            try {
                doc.insertString(doc.getLength(), text + "\n", attrs);
                outputPane.setCaretPosition(doc.getLength());
            } catch (Exception e) {}
        });
        
        // Create Runner with argument list and timeout
        DalfoxRunner runner = new DalfoxRunner(logger, commandArgs, method, url, timeoutMinutes);
        
        // Track the runner
        synchronized (activeRunners) {
            activeRunners.add(runner);
        }
        
        // Action Listeners
        btnStop.addActionListener(e -> {
            runner.stop();
            lblStatus.setText("Cancelled");
            btnStop.setEnabled(false);
            synchronized (activeRunners) {
                activeRunners.remove(runner);
            }
        });
        
        // Add tab
        SwingUtilities.invokeLater(() -> {
            tabbedPane.addTab(fullTitle, mainPanel);
            tabbedPane.setSelectedComponent(mainPanel);
            
            int idx = tabbedPane.indexOfComponent(mainPanel);
            if (idx >= 0) {
                tabbedPane.setTabComponentAt(idx, createTabComponent(fullTitle, mainPanel, runner));
            }
        });
        
        return runner;
    }
    
    private JPanel createTabComponent(String title, JPanel panel, DalfoxRunner runner) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl.setOpaque(false);
        
        JLabel lbl = new JLabel(title);
        lbl.setBorder(new EmptyBorder(0, 0, 0, 5));
        
        JButton btnClose = new JButton("x");
        btnClose.setMargin(new Insets(0, 2, 0, 2));
        btnClose.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setForeground(Color.GRAY);
        
        btnClose.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnClose.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e) { btnClose.setForeground(Color.GRAY); }
        });
        
        btnClose.addActionListener(e -> {
            runner.stop(); // Kill process
            synchronized (activeRunners) {
                activeRunners.remove(runner);
            }
            tabbedPane.remove(panel);
        });
        
        pnl.add(lbl);
        pnl.add(btnClose);
        return pnl;
    }
}
