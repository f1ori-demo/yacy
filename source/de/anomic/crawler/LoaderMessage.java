//plasmaCrawlLoaderMessage.java 
//------------------------
//part of YaCy
//(C) by Michael Peter Christen; mc@anomic.de
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004
//last major change: 21.04.2005 by Martin Thelian
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//Using this software in any meaning (reading, learning, copying, compiling,
//running) means that you agree that the Author(s) is (are) not responsible
//for cost, loss of data or any harm that may be caused directly or indirectly
//by usage of this softare or this documentation. The usage of this software
//is on your own risk. The installation and usage (starting/running) of this
//software may allow other people or application to access your computer and
//any attached devices and is highly dependent on the configuration of the
//software which must be done by the user of the software; the author(s) is
//(are) also not responsible for proper configuration and usage of the
//software, even if provoked by documentation provided together with
//the software.
//
//Any changes to this file according to the GPL as documented in the file
//gpl.txt aside this file in the shipment you received can be done to the
//lines that follows this copyright notice here, but changes must not be
//done inside the copyright notive above. A re-distribution must contain
//the intact and unchanged copyright notice.
//Contributions and changes to the program code must be marked as such.


package de.anomic.crawler;

import de.anomic.plasma.plasmaHTCache;
import de.anomic.server.serverSemaphore;
import de.anomic.yacy.yacyURL;

public final class LoaderMessage {
    public final int crawlingPriority;
    
    public final yacyURL url;
    public final String name;
    public final String referer;
    public final String initiator;
    public final int depth;
    public final CrawlProfile.entry profile;
    public final boolean acceptAllContent;
    public final int timeout;
    public final boolean keepInMemory;
    
    private serverSemaphore resultSync  = null;
    private plasmaHTCache.Entry result;
    private String errorMessage;
    
    // loadParallel(URL url, String referer, String initiator, int depth, plasmaCrawlProfile.entry profile) {
    public LoaderMessage(
            yacyURL url,
            String name,                       // the name of the url, from anchor tag <a>name</a>
            String referer, 
            String initiator, 
            int depth, 
            CrawlProfile.entry profile,
            int crawlingPriority,
            boolean acceptAllContent,
            int timeout,
            boolean keepInMemory
    ) {
        this.url = url;
        this.name = name;
        this.referer = referer;
        this.initiator = initiator;
        this.depth = depth;
        this.profile = profile;
        this.crawlingPriority = crawlingPriority;
        this.acceptAllContent = acceptAllContent;
        this.timeout = timeout;
        this.keepInMemory = keepInMemory;
        
        this.resultSync  = new serverSemaphore(0);
        this.result = null;
    } 
    
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getError() {
        return this.errorMessage;
    }
    
    public void setResult(plasmaHTCache.Entry theResult) {
        // store the result
        this.result = theResult;
        
        // notify blocking result readers
        this.resultSync.V();        
    }
    
    public plasmaHTCache.Entry waitForResult() throws InterruptedException {
        plasmaHTCache.Entry theResult = null;
        
        this.resultSync.P();
        /* =====> CRITICAL SECTION <======== */
        
            theResult = this.result;
        
        /* =====> CRITICAL SECTION <======== */         
        this.resultSync.V();
        
        return theResult;                
    }
}