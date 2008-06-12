// kelondroBLOB.java
// (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 08.06.2008 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
// $LastChangedRevision: 1986 $
// $LastChangedBy: orbiter $
//
// LICENSE
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

package de.anomic.kelondro;

import java.io.IOException;

public interface kelondroBLOB {
    
    public int keylength();
    public void clear() throws IOException;
    public int size();
    public kelondroCloneableIterator<String> keys(boolean up, boolean rotating) throws IOException;
    public byte[] get(String key, int pos, int len) throws IOException;
    public void put(String key, int pos, byte[] b, int off, int len) throws IOException;
    public void remove(String key) throws IOException;
    public boolean exist(String key) throws IOException;
    public kelondroRA getRA(String filekey);
    public void close();
    
}