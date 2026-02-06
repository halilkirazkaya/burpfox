package com.burpfox.core;

/**
 * Constants for Dalfox command-line flags.
 */
public final class DalfoxFlags {
    private DalfoxFlags() {}

    // Detection
    public static final String CONTEXT_AWARE = "--context-aware";
    public static final String DEEP_DOMXSS = "--deep-domxss";
    public static final String WAF_EVASION = "--waf-evasion";
    public static final String FAST_SCAN = "--fast-scan";
    public static final String SKIP_DISCOVERY = "--skip-discovery";
    public static final String SKIP_HEADLESS = "--skip-headless";

    // Mining
    public static final String SKIP_BAV = "--skip-bav";
    public static final String SKIP_MINING_ALL = "--skip-mining-all";
    public static final String SKIP_MINING_DICT = "--skip-mining-dict";
    public static final String SKIP_MINING_DOM = "--skip-mining-dom";

    // Remote & Network
    public static final String REMOTE_PAYLOADS = "--remote-payloads=portswigger,payloadbox";
    public static final String FOLLOW_REDIRECTS = "--follow-redirects";

    // Output
    public static final String NO_COLOR = "--no-color";
    public static final String SILENCE = "-S";
    public static final String REPORT = "--report";
    public static final String POC_TYPE = "--poc-type";
    public static final String ONLY_POC = "--only-poc";

    // Blind XSS
    public static final String BLIND = "--blind";

    // Scan modes
    public static final String MODE_URL = "url";
    public static final String MODE_SXSS = "sxss";

    // Options
    public static final String TRIGGER = "--trigger";
    public static final String METHOD = "-X";
    public static final String DATA = "-d";
    public static final String COOKIE = "-C";
    public static final String USER_AGENT = "--user-agent";
    public static final String HEADER = "-H";
    public static final String PARAM = "-p";
    
    // Advanced
    public static final String WORKER = "-w";
    public static final String TIMEOUT = "--timeout";
    public static final String DELAY = "--delay";
    public static final String PROXY = "--proxy";
    public static final String IGNORE_RETURN = "--ignore-return";
    public static final String IGNORE_PARAM = "--ignore-param";
    public static final String DEBUG = "--debug";
}
