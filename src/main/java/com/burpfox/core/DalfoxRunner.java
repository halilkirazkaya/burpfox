package com.burpfox.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Handles Dalfox process execution with cancellation support.
 * Uses argument array instead of shell command to prevent shell injection.
 */
public class DalfoxRunner {
    
    private static final String DALFOX_PATH = resolveDalfoxPath();
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    
    private static String resolveDalfoxPath() {
        String propPath = System.getProperty("dalfox.path");
        if (propPath != null && !propPath.isEmpty()) return propPath;
        
        String[] commonPaths = {
            "/usr/local/bin/dalfox", "/usr/bin/dalfox",
            System.getProperty("user.home") + "/go/bin/dalfox",
            System.getProperty("user.home") + "/.local/bin/dalfox"
        };
        
        for (String path : commonPaths) {
            if (new java.io.File(path).exists()) return path;
        }
        return "dalfox";
    }
    
    private final Consumer<String> logger;
    private final List<String> commandArgs;
    private final String method;
    private final String url;
    private final int timeoutMinutes;
    
    private Process process;
    private boolean isCancelled = false;
    
    /**
     * Creates a new DalfoxRunner with default timeout.
     */
    public DalfoxRunner(Consumer<String> logger, List<String> commandArgs, String method, String url) {
        this(logger, commandArgs, method, url, DEFAULT_TIMEOUT_MINUTES);
    }
    
    /**
     * Creates a new DalfoxRunner with custom timeout.
     */
    public DalfoxRunner(Consumer<String> logger, List<String> commandArgs, String method, String url, int timeoutMinutes) {
        this.logger = logger;
        this.commandArgs = commandArgs;
        this.method = method;
        this.url = url;
        this.timeoutMinutes = timeoutMinutes;
    }
    
    public static String getDalfoxPath() {
        return DALFOX_PATH;
    }
    
    public void start() {
        new Thread(this::runInternal).start();
    }
    
    public void stop() {
        isCancelled = true;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            logger.accept("\n[!] Scan cancelled by user.");
        }
    }
    
    /**
     * Returns the command as a display string for logging.
     */
    public String getCommandString() {
        return String.join(" ", commandArgs);
    }
    
    private void runInternal() {
        logger.accept("--------------------------------------------------");
        logger.accept("SCAN STARTED: " + method + " " + url);
        logger.accept("COMMAND: " + getCommandString());
        logger.accept("--------------------------------------------------");
        
        try {
            if (!checkDalfoxExists()) {
                logger.accept("[ERROR] Dalfox not found at: " + DALFOX_PATH);
                return;
            }
            
            // Use argument array directly - no shell injection possible
            ProcessBuilder pb = new ProcessBuilder(commandArgs);
            pb.redirectErrorStream(true);
            process = pb.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isCancelled) break;
                    logger.accept(line);
                }
            }
            
            if (!isCancelled) {
                boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    logger.accept("\n[!] TIMEOUT: Scan exceeded " + timeoutMinutes + " minutes.");
                } else {
                    logger.accept("\n[!] Finished. Exit Code: " + process.exitValue());
                }
            }
            
        } catch (Exception e) {
            if (!isCancelled) {
                logger.accept("[ERROR] " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if Dalfox binary exists.
     * Uses local variable to avoid overwriting the main process reference.
     */
    private boolean checkDalfoxExists() {
        try {
            ProcessBuilder pb = new ProcessBuilder(DALFOX_PATH, "version");
            // FIX: Use local variable instead of overwriting class field
            Process checkProcess = pb.start();
            return checkProcess.waitFor(5, TimeUnit.SECONDS) && checkProcess.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
