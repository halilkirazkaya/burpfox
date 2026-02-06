package com.burpfox.menu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.burpfox.ui.BurpFoxTab;
import com.burpfox.ui.ScanConfigDialog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Context menu provider for BurpFox scan option.
 * Supports both message editor and proxy history selection.
 */
public class BurpFoxContextMenu implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final BurpFoxTab mainTab;

    public BurpFoxContextMenu(MontoyaApi api, BurpFoxTab mainTab) {
        this.api = api;
        this.mainTab = mainTab;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        
        // Get request from message editor (Repeater, Intruder, etc.)
        if (event.messageEditorRequestResponse().isPresent()) {
            JMenuItem burpFoxItem = new JMenuItem("BurpFox Scan");
            burpFoxItem.addActionListener(e -> {
                HttpRequestResponse requestResponse = event.messageEditorRequestResponse().get().requestResponse();
                new ScanConfigDialog(requestResponse, mainTab).setVisible(true);
            });
            menuItems.add(burpFoxItem);
        }
        // Also support selection from Proxy History, Site Map, etc.
        else if (event.selectedRequestResponses() != null && !event.selectedRequestResponses().isEmpty()) {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            
            if (selected.size() == 1) {
                // Single request - open config dialog
                JMenuItem burpFoxItem = new JMenuItem("BurpFox Scan");
                burpFoxItem.addActionListener(e -> {
                    new ScanConfigDialog(selected.get(0), mainTab).setVisible(true);
                });
                menuItems.add(burpFoxItem);
            } else {
                // Multiple requests - show submenu
                JMenu burpFoxMenu = new JMenu("BurpFox Scan (" + selected.size() + " requests)");
                
                JMenuItem scanAllItem = new JMenuItem("Scan All Selected");
                scanAllItem.addActionListener(e -> {
                    for (HttpRequestResponse req : selected) {
                        new ScanConfigDialog(req, mainTab).setVisible(true);
                    }
                });
                burpFoxMenu.add(scanAllItem);
                
                burpFoxMenu.addSeparator();
                
                // Individual items (limit to first 10)
                int count = 0;
                for (HttpRequestResponse req : selected) {
                    if (count >= 10) {
                        burpFoxMenu.add(new JMenuItem("... and " + (selected.size() - 10) + " more"));
                        break;
                    }
                    // Null safety check
                    if (req == null || req.request() == null) continue;
                    
                    String label = req.request().method() + " " + truncateUrl(req.request().url(), 50);
                    JMenuItem item = new JMenuItem(label);
                    item.addActionListener(evt -> {
                        new ScanConfigDialog(req, mainTab).setVisible(true);
                    });
                    burpFoxMenu.add(item);
                    count++;
                }
                
                menuItems.add(burpFoxMenu);
            }
        }
        
        return menuItems;
    }
    
    /**
     * Truncates URL for display in menu.
     */
    private String truncateUrl(String url, int maxLen) {
        if (url == null) return "";
        if (url.length() <= maxLen) return url;
        return url.substring(0, maxLen - 3) + "...";
    }
}
