package com.ariseontech.joindesk;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDateTime;

public class SystemInfo {
    public static final String apiPrefix = "";
    private final Runtime runtime = Runtime.getRuntime();

    public String Info() {
        return LocalDateTime.now() +
                "\n" +
                this.OsInfo() +
                "\n" +
                this.MemInfo();
    }

    public String OSname() {
        return System.getProperty("os.name");
    }

    public String OSversion() {
        return System.getProperty("os.version");
    }

    public String OsArch() {
        return System.getProperty("os.arch");
    }

    public long totalMem() {
        return Runtime.getRuntime().totalMemory();
    }

    public long usedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public String MemInfo() {
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        sb.append("Free memory: ");
        sb.append(format.format(freeMemory / 1024));
        sb.append(";");
        sb.append("Allocated memory: ");
        sb.append(format.format(allocatedMemory / 1024));
        sb.append(";");
        sb.append("Max memory: ");
        sb.append(format.format(maxMemory / 1024));
        sb.append(";");
        sb.append("Total free memory: ");
        sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
        sb.append(";");
        return sb.toString();

    }

    public String OsInfo() {
        return "OS: " +
                this.OSname() +
                "," +
                "Version: " +
                this.OSversion() +
                "," +
                ": " +
                this.OsArch() +
                "," +
                "Available processors (cores): " +
                runtime.availableProcessors();
    }

    public String DiskInfo() {
        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();
        StringBuilder sb = new StringBuilder();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            sb.append("File system root: ");
            sb.append(root.getAbsolutePath());
            sb.append(",");
            sb.append("Total space (bytes): ");
            sb.append(root.getTotalSpace());
            sb.append(",");
            sb.append("Free space (bytes): ");
            sb.append(root.getFreeSpace());
            sb.append(",");
            sb.append("Usable space (bytes): ");
            sb.append(root.getUsableSpace());
            sb.append(",");
        }
        return sb.toString();
    }
}
