// ResourceObserver.java
// -----------------------
// part of YaCy
// (C) by Detlef Reichl; detlef!reichl()gmx!org
// Pforzheim, Germany, 2008
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.anomic.crawler;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.server.logging.serverLog;
import de.anomic.tools.diskUsage;

public final class ResourceObserver {
    // The minimal free space on every used volume, for now set to 100 MB.
    // TODO make it configurable
    private final static long MIN_FREE_DISK_SPACE = 100L /* MiB */ * 1024L * 1024L;
    // Unknown for now
    //private final static long MIN_FREE_MEMORY = 0;
    // We are called with the cleanup job every five minutes;
    // the disk usage should be checked with every run
    private final int CHECK_DISK_USAGE_FREQ = 1;
    // The memory usage should be checked on every run
    private final int CHECK_MEMORY_USAGE_FREQ = 1;
    
    private final serverLog log = new serverLog("RESOURCE OBSERVER");
    private final plasmaSwitchboard sb;

    private int checkDiskUsageCount;
    private int checkMemoryUsageCount;
    private boolean disksOK;
    private boolean memoryOK;
    
    public ResourceObserver(plasmaSwitchboard sb) {
        this.sb = sb;
        this.log.logInfo("initializing the resource observer");

        ArrayList<String> pathsToCheck = new ArrayList<String>();
        //  FIXME whats about the secondary path???
        //   = (getConfig(plasmaSwitchboard.INDEX_SECONDARY_PATH, "");
        final String[] pathes =  {plasmaSwitchboard.HTDOCS_PATH,        
                            plasmaSwitchboard.INDEX_PRIMARY_PATH,
                            plasmaSwitchboard.LISTS_PATH,
                            plasmaSwitchboard.PLASMA_PATH,
                            plasmaSwitchboard.RANKING_PATH,
                            plasmaSwitchboard.WORK_PATH};
        String path;
        for (final String element : pathes) {
            try {
                path = sb.getConfigPath(element, "").getCanonicalPath().toString();
                if (path.length() > 0) pathsToCheck.add(path);
            } catch (final IOException e) {}
        }
        
        diskUsage.init(pathsToCheck);
        
        if (!diskUsage.isUsable())
            this.log.logWarning("Disk usage returned: " + diskUsage.getErrorMessage());
        
        checkDiskUsageCount = 0;
        checkMemoryUsageCount = 0;
        disksOK = true;
        memoryOK = true;
    }

    public void resourceObserverJob() {
        checkDiskUsageCount++;
        checkMemoryUsageCount++;
        boolean tmpDisksOK = true;
        boolean tmpMemoryOK = true;
        if (checkDiskUsageCount >= CHECK_DISK_USAGE_FREQ) {
            checkDiskUsageCount = 0;
            tmpDisksOK = checkDisks();
            disksOK = tmpDisksOK;
        }
        if (checkMemoryUsageCount >= CHECK_MEMORY_USAGE_FREQ) {
            checkMemoryUsageCount = 0;
            tmpMemoryOK = checkMemory();
            memoryOK = tmpMemoryOK;
        }
        
        if (!tmpDisksOK || !tmpMemoryOK) {
            if (!sb.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL)) {
                this.log.logInfo("disabling local crawls");
                sb.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL);
            }
            if (!sb.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL)) {
                this.log.logInfo("disabling remote triggered crawls");
                sb.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
            }
        }
        else {
            if (diskUsage.isUsable())
                this.log.logInfo("run completed; everything in order");
            else
                this.log.logInfo("The observer is out of order: " + diskUsage.getErrorMessage());
        }
    }
    
    public boolean getDisksOK () {
        return disksOK;
    }
    
    public boolean getMemoryOK () {
        return memoryOK;
    }
    
    /**
     * @return amount of space that should be kept free
     */
    public long getMinFreeDiskSpace () {
        return MIN_FREE_DISK_SPACE;
    }
    
    /**
     * @return enough disk space available?
     */
    private boolean checkDisks() {
        boolean below = false;    
    
        if (!diskUsage.isUsable ())
            return true;
        
        final HashMap<String, long[]> usage = diskUsage.getDiskUsage();
        long[] val;
        for (Map.Entry<String, long[]> entry: usage.entrySet()) {
            val = entry.getValue();
            this.log.logInfo("df of Volume " + entry.getKey() + ": " + (val[1] / 1024 / 1024) + " MB");
            if (val[1] < MIN_FREE_DISK_SPACE) {
                this.log.logWarning("Volume " + entry.getKey() + ": free space (" + (val[1] / 1024 / 1024) + " MB) is too low (< " + (MIN_FREE_DISK_SPACE / 1024 / 1024) + " MB)");
                below = true;
            }
        }
        return !below;
    }
    
    private boolean checkMemory() {
        return true;
    }
}
