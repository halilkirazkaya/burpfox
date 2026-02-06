package com.burpfox.core;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds Dalfox command arguments from request data and options.
 */
public class DalfoxCommandBuilder {
    
    private final HttpRequestResponse requestResponse;
    private final List<String> selectedParams;
    
    // Scan mode
    private boolean storedXssMode = false;
    private String triggerUrl = "";
    
    // Detection flags
    private boolean noColor = true;
    private boolean contextAware = false;
    private boolean deepDomXss = false;
    private boolean wafEvasion = false;
    private boolean followRedirects = false;
    private boolean fastScan = false;
    private boolean skipDiscovery = false;
    private boolean skipHeadless = false;
    
    // Mining flags
    private boolean skipBav = false;
    private boolean skipMiningAll = false;
    private boolean miningDict = false;
    private boolean miningDom = false;
    private boolean remotePayloads = false;
    
    // Output flags
    private boolean silenceMode = false;
    private boolean report = false;
    private boolean debug = false;
    private String pocType = "plain";
    
    // Advanced
    private int workers = 100;
    private int timeout = 10;
    private int delay = 0;
    private String proxy = "";
    private String ignoreReturn = "";
    
    // Blind XSS
    private String blindUrl = "";
    
    public DalfoxCommandBuilder(HttpRequestResponse requestResponse, List<String> selectedParams) {
        this.requestResponse = requestResponse;
        this.selectedParams = selectedParams;
    }
    
    // Fluent setters
    public DalfoxCommandBuilder storedXssMode(boolean value) { this.storedXssMode = value; return this; }
    public DalfoxCommandBuilder triggerUrl(String value) { this.triggerUrl = value; return this; }
    public DalfoxCommandBuilder noColor(boolean value) { this.noColor = value; return this; }
    public DalfoxCommandBuilder contextAware(boolean value) { this.contextAware = value; return this; }
    public DalfoxCommandBuilder deepDomXss(boolean value) { this.deepDomXss = value; return this; }
    public DalfoxCommandBuilder wafEvasion(boolean value) { this.wafEvasion = value; return this; }
    public DalfoxCommandBuilder followRedirects(boolean value) { this.followRedirects = value; return this; }
    public DalfoxCommandBuilder fastScan(boolean value) { this.fastScan = value; return this; }
    public DalfoxCommandBuilder skipDiscovery(boolean value) { this.skipDiscovery = value; return this; }
    public DalfoxCommandBuilder skipHeadless(boolean value) { this.skipHeadless = value; return this; }
    public DalfoxCommandBuilder skipBav(boolean value) { this.skipBav = value; return this; }
    public DalfoxCommandBuilder skipMiningAll(boolean value) { this.skipMiningAll = value; return this; }
    public DalfoxCommandBuilder miningDict(boolean value) { this.miningDict = value; return this; }
    public DalfoxCommandBuilder miningDom(boolean value) { this.miningDom = value; return this; }
    public DalfoxCommandBuilder remotePayloads(boolean value) { this.remotePayloads = value; return this; }
    public DalfoxCommandBuilder silenceMode(boolean value) { this.silenceMode = value; return this; }
    public DalfoxCommandBuilder report(boolean value) { this.report = value; return this; }
    public DalfoxCommandBuilder debug(boolean value) { this.debug = value; return this; }
    public DalfoxCommandBuilder pocType(String value) { this.pocType = value; return this; }
    public DalfoxCommandBuilder workers(int value) { this.workers = value; return this; }
    public DalfoxCommandBuilder timeout(int value) { this.timeout = value; return this; }
    public DalfoxCommandBuilder delay(int value) { this.delay = value; return this; }
    public DalfoxCommandBuilder proxy(String value) { this.proxy = value; return this; }
    public DalfoxCommandBuilder ignoreReturn(String value) { this.ignoreReturn = value; return this; }
    public DalfoxCommandBuilder blindUrl(String value) { this.blindUrl = value; return this; }
    
    /**
     * Builds and returns command arguments as a List (safe for ProcessBuilder).
     */
    public List<String> buildArgs() {
        List<String> args = new ArrayList<>();
        
        String targetUrl = requestResponse.request().url();
        String method = requestResponse.request().method();
        String body = requestResponse.request().bodyToString();
        
        String cookie = null;
        String userAgent = null;
        List<String> otherHeaders = new ArrayList<>();
        
        for (var header : requestResponse.request().headers()) {
            String name = header.name();
            String value = header.value();
            
            if (name.equalsIgnoreCase("Cookie")) {
                cookie = value;
            } else if (name.equalsIgnoreCase("User-Agent")) {
                userAgent = value;
            } else if (shouldIncludeHeader(name, value)) {
                otherHeaders.add(name + ": " + value);
            }
        }
        
        // Dalfox binary path
        args.add(DalfoxRunner.getDalfoxPath());
        
        // Scan Mode
        if (storedXssMode) {
            args.add(DalfoxFlags.MODE_SXSS);
        } else {
            args.add(DalfoxFlags.MODE_URL);
        }
        
        // Target URL
        args.add(targetUrl);
        
        // Trigger URL (Stored XSS)
        if (storedXssMode && triggerUrl != null && !triggerUrl.isEmpty()) {
            args.add(DalfoxFlags.TRIGGER);
            args.add(triggerUrl);
        }
        
        // Output flags
        if (noColor) args.add(DalfoxFlags.NO_COLOR);
        if (silenceMode) args.add(DalfoxFlags.SILENCE);
        if (report) args.add(DalfoxFlags.REPORT);
        if (debug) args.add(DalfoxFlags.DEBUG);
        if (pocType != null && !pocType.equals("plain")) {
            args.add(DalfoxFlags.POC_TYPE);
            args.add(pocType);
        }
        
        // Blind XSS
        if (blindUrl != null && !blindUrl.isEmpty()) {
            args.add(DalfoxFlags.BLIND);
            args.add(blindUrl);
        }
        
        // Method
        if (!method.equalsIgnoreCase("GET")) {
            args.add(DalfoxFlags.METHOD);
            args.add(method);
        }
        
        // Body
        if (body != null && !body.isEmpty()) {
            args.add(DalfoxFlags.DATA);
            args.add(body);
        }
        
        // Cookie
        if (cookie != null && !cookie.isEmpty()) {
            args.add(DalfoxFlags.COOKIE);
            args.add(cookie);
        }
        
        // User-Agent
        if (userAgent != null && !userAgent.isEmpty()) {
            args.add(DalfoxFlags.USER_AGENT);
            args.add(userAgent);
        }
        
        // Other Headers
        for (String h : otherHeaders) {
            args.add(DalfoxFlags.HEADER);
            args.add(h);
        }
        
        // Parameters - each needs its own -p flag
        for (String param : selectedParams) {
            args.add(DalfoxFlags.PARAM);
            args.add(param);
        }
        
        // Detection flags
        if (contextAware) args.add(DalfoxFlags.CONTEXT_AWARE);
        if (deepDomXss) args.add(DalfoxFlags.DEEP_DOMXSS);
        if (wafEvasion) args.add(DalfoxFlags.WAF_EVASION);
        if (followRedirects) args.add(DalfoxFlags.FOLLOW_REDIRECTS);
        if (fastScan) args.add(DalfoxFlags.FAST_SCAN);
        if (skipDiscovery) args.add(DalfoxFlags.SKIP_DISCOVERY);
        if (skipHeadless) args.add(DalfoxFlags.SKIP_HEADLESS);
        
        // Mining flags
        if (skipBav) args.add(DalfoxFlags.SKIP_BAV);
        if (skipMiningAll) args.add(DalfoxFlags.SKIP_MINING_ALL);
        if (!miningDict) args.add(DalfoxFlags.SKIP_MINING_DICT);
        if (!miningDom) args.add(DalfoxFlags.SKIP_MINING_DOM);
        
        // Remote
        if (remotePayloads) args.add(DalfoxFlags.REMOTE_PAYLOADS);
        
        // Advanced options
        if (workers != 100) {
            args.add(DalfoxFlags.WORKER);
            args.add(String.valueOf(workers));
        }
        if (timeout != 10) {
            args.add(DalfoxFlags.TIMEOUT);
            args.add(String.valueOf(timeout));
        }
        if (delay > 0) {
            args.add(DalfoxFlags.DELAY);
            args.add(String.valueOf(delay));
        }
        if (proxy != null && !proxy.isEmpty()) {
            args.add(DalfoxFlags.PROXY);
            args.add(proxy);
        }
        if (ignoreReturn != null && !ignoreReturn.isEmpty()) {
            args.add(DalfoxFlags.IGNORE_RETURN);
            args.add(ignoreReturn);
        }
        
        return args;
    }
    
    /**
     * Returns command as a single string for display purposes only.
     * For actual execution, use buildArgs() instead.
     */
    public String buildPreviewString() {
        List<String> args = buildArgs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(" ");
            String arg = args.get(i);
            // Quote args with spaces for display
            if (arg.contains(" ") || arg.contains("&") || arg.contains(";")) {
                sb.append("'").append(arg.replace("'", "'\\''")).append("'");
            } else {
                sb.append(arg);
            }
        }
        return sb.toString();
    }
    
    /**
     * Determines if a header should be included in the Dalfox command.
     * Filters out headers that cause parsing issues or are not relevant for XSS testing.
     */
    private boolean shouldIncludeHeader(String name, String value) {
        // Skip standard headers that are handled separately or not needed
        if (name.equalsIgnoreCase("Host") || 
            name.equalsIgnoreCase("Content-Length") ||
            name.equalsIgnoreCase("Connection")) {
            return false;
        }
        
        // Skip Sec-Ch-* headers (contain quotes that break Dalfox parsing)
        if (name.toLowerCase().startsWith("sec-ch-")) {
            return false;
        }
        
        // Skip other Sec-Fetch-* headers (not needed for XSS testing)
        if (name.toLowerCase().startsWith("sec-fetch-")) {
            return false;
        }
        
        // Skip headers with values containing quotes (cause parsing errors)
        if (value.contains("\"")) {
            return false;
        }
        
        // Skip encoding headers (can cause issues)
        if (name.equalsIgnoreCase("Accept-Encoding")) {
            return false;
        }
        
        return true;
    }
}
