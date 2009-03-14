// URLMetadata.java
// (C) 2006 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 2006 on http://www.anomic.de
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

package de.anomic.kelondro.text;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import de.anomic.crawler.CrawlEntry;
import de.anomic.kelondro.index.Row;
import de.anomic.kelondro.order.Base64Order;
import de.anomic.kelondro.order.Bitfield;
import de.anomic.kelondro.order.Digest;
import de.anomic.kelondro.order.NaturalOrder;
import de.anomic.kelondro.util.DateFormatter;
import de.anomic.kelondro.util.FileUtils;
import de.anomic.kelondro.util.kelondroException;
import de.anomic.plasma.plasmaSearchQuery;
import de.anomic.server.serverCharBuffer;
import de.anomic.server.serverCodings;
import de.anomic.tools.crypt;
import de.anomic.yacy.yacyURL;

public class MetadataRowContainer {

    // this object stores attributes for URL entries
    
    public static final Row rowdef = new Row(
        "String hash-12, " +            // the url's hash
        "String comp-360, " +           // components: the url, description, author and tags. As 5th element, an ETag is possible
        "Cardinal mod-4 {b256}, " +     // last-modified from the httpd
        "Cardinal load-4 {b256}, " +    // time when the url was loaded
        "Cardinal fresh-4 {b256}, " +   // time until this url is fresh
        "String referrer-12, " +        // (one of) the url's referrer hash(es)
        "byte[] md5-8, " +              // the md5 of the url content (to identify changes)
        "Cardinal size-6 {b256}, " +    // size of file in bytes
        "Cardinal wc-3 {b256}, " +      // size of file by number of words; for video and audio: seconds
        "byte[] dt-1, " +               // doctype, taken from extension or any other heuristic
        "Bitfield flags-4, " +          // flags; any stuff (see Word-Entity definition)
        "String lang-2, " +             // language
        "Cardinal llocal-2 {b256}, " +  // # of outlinks to same domain; for video and image: width 
        "Cardinal lother-2 {b256}, " +  // # of outlinks to outside domain; for video and image: height
        "Cardinal limage-2 {b256}, " +  // # of embedded image links
        "Cardinal laudio-2 {b256}, " +  // # of embedded audio links; for audio: track number; for video: number of audio tracks
        "Cardinal lvideo-2 {b256}, " +  // # of embedded video links
        "Cardinal lapp-2 {b256}",       // # of embedded links to applications
        Base64Order.enhancedCoder
    );      
    
    /* ===========================================================================
     * Constants to access the various columns of an URL entry
     * =========================================================================== */
    /** the url's hash */
    private static final int col_hash     =  0;
    /** components: the url, description, author and tags. As 5th element, an ETag is possible */
    private static final int col_comp     =  1;
    /** components: the url, description, author and tags. As 5th element, an ETag is possible */
    private static final int col_mod      =  2;
    /** time when the url was loaded */
    private static final int col_load     =  3;
    /** time until this url is fresh */
    private static final int col_fresh    =  4;
    /** time when the url was loaded */
    private static final int col_referrer =  5;
    /** the md5 of the url content (to identify changes) */
    private static final int col_md5      =  6;
    /** size of file in bytes */
    private static final int col_size     =  7;
    /** size of file by number of words; for video and audio: seconds */
    private static final int col_wc       =  8;
    /** doctype, taken from extension or any other heuristic */
    private static final int col_dt       =  9;
    /** flags; any stuff (see Word-Entity definition) */
    private static final int col_flags    = 10;
    /** language */
    private static final int col_lang     = 11;
    /** of outlinks to same domain; for video and image: width */
    private static final int col_llocal   = 12;
    /** of outlinks to outside domain; for video and image: height */
    private static final int col_lother   = 13;
    /** of embedded image links */
    private static final int col_limage   = 14;
    /** of embedded audio links; for audio: track number; for video: number of audio tracks */
    private static final int col_laudio   = 15;
    /** of embedded video links */
    private static final int col_lvideo   = 16;
    /** of embedded links to applications */
    private static final int col_lapp     = 17;
    
    private final Row.Entry entry;
    private final String snippet;
    private Reference word; // this is only used if the url is transported via remote search requests
    private final long ranking; // during generation of a search result this value is set
    
    public MetadataRowContainer(
            final yacyURL url,
            final String dc_title,
            final String dc_creator,
            final String dc_subject,
            final String ETag,
            final Date mod,
            final Date load,
            final Date fresh,
            final String referrer,
            final byte[] md5,
            final long size,
            final int wc,
            final char dt,
            final Bitfield flags,
            final String lang,
            final int llocal,
            final int lother,
            final int laudio,
            final int limage,
            final int lvideo,
            final int lapp) {
        // create new entry and store it into database
        this.entry = rowdef.newEntry();
        this.entry.setCol(col_hash, url.hash(), null);
        this.entry.setCol(col_comp, encodeComp(url, dc_title, dc_creator, dc_subject, ETag));
        encodeDate(col_mod, mod);
        encodeDate(col_load, load);
        encodeDate(col_fresh, fresh);
        try {
			this.entry.setCol(col_referrer, (referrer == null) ? null : referrer.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			this.entry.setCol(col_referrer, (referrer == null) ? null : referrer.getBytes());
		}
        this.entry.setCol(col_md5, md5);
        this.entry.setCol(col_size, size);
        this.entry.setCol(col_wc, wc);
        this.entry.setCol(col_dt, new byte[]{(byte) dt});
        this.entry.setCol(col_flags, flags.bytes());
        try {
			this.entry.setCol(col_lang, lang.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			this.entry.setCol(col_lang, lang.getBytes());
		}
        this.entry.setCol(col_llocal, llocal);
        this.entry.setCol(col_lother, lother);
        this.entry.setCol(col_limage, limage);
        this.entry.setCol(col_laudio, laudio);
        this.entry.setCol(col_lvideo, lvideo);
        this.entry.setCol(col_lapp, lapp);
        //System.out.println("===DEBUG=== " + load.toString() + ", " + decodeDate(col_load).toString());
        this.snippet = null;
        this.word = null;
        this.ranking = 0;
    }

    private void encodeDate(final int col, final Date d) {
        // calculates the number of days since 1.1.1970 and returns this as 4-byte array
        this.entry.setCol(col, NaturalOrder.encodeLong(d.getTime() / 86400000, 4));
    }

    private Date decodeDate(final int col) {
        return new Date(86400000 * this.entry.getColLong(col));
    }
    
    public static byte[] encodeComp(final yacyURL url, final String dc_title, final String dc_creator, final String dc_subject, final String ETag) {
        final serverCharBuffer s = new serverCharBuffer(200);
        s.append(url.toNormalform(false, true)).append(10);
        s.append(dc_title).append(10);
        s.append(dc_creator).append(10);
        s.append(dc_subject).append(10);
        s.append(ETag).append(10);
        try {
			return s.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s.toString().getBytes();
		}
    }
    
    public MetadataRowContainer(final Row.Entry entry, final Reference searchedWord, final long ranking) {
        this.entry = entry;
        this.snippet = null;
        this.word = searchedWord;
        this.ranking = ranking;
    }

    public MetadataRowContainer(final Properties prop) {
        // generates an plasmaLURLEntry using the properties from the argument
        // the property names must correspond to the one from toString
        //System.out.println("DEBUG-ENTRY: prop=" + prop.toString());
        yacyURL url;
        try {
            url = new yacyURL(crypt.simpleDecode(prop.getProperty("url", ""), null), prop.getProperty("hash"));
        } catch (final MalformedURLException e) {
            url = null;
        }
        String descr = crypt.simpleDecode(prop.getProperty("descr", ""), null); if (descr == null) descr = "";
        String dc_creator = crypt.simpleDecode(prop.getProperty("author", ""), null); if (dc_creator == null) dc_creator = "";
        String tags = crypt.simpleDecode(prop.getProperty("tags", ""), null); if (tags == null) tags = "";
        String ETag = crypt.simpleDecode(prop.getProperty("ETag", ""), null); if (ETag == null) ETag = "";
        
        this.entry = rowdef.newEntry();
        this.entry.setCol(col_hash, url.hash(), null); // FIXME potential null pointer access
        this.entry.setCol(col_comp, encodeComp(url, descr, dc_creator, tags, ETag));
        try {
            encodeDate(col_mod, DateFormatter.parseShortDay(prop.getProperty("mod", "20000101")));
        } catch (final ParseException e) {
            encodeDate(col_mod, new Date());
        }
        try {
            encodeDate(col_load, DateFormatter.parseShortDay(prop.getProperty("load", "20000101")));
        } catch (final ParseException e) {
            encodeDate(col_load, new Date());
        }
        try {
            encodeDate(col_fresh, DateFormatter.parseShortDay(prop.getProperty("fresh", "20000101")));
        } catch (final ParseException e) {
            encodeDate(col_fresh, new Date());
        }
        try {
			this.entry.setCol(col_referrer, prop.getProperty("referrer", "").getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			this.entry.setCol(col_referrer, prop.getProperty("referrer", "").getBytes());
		}
        this.entry.setCol(col_md5, Digest.decodeHex(prop.getProperty("md5", "")));
        this.entry.setCol(col_size, Integer.parseInt(prop.getProperty("size", "0")));
        this.entry.setCol(col_wc, Integer.parseInt(prop.getProperty("wc", "0")));
        this.entry.setCol(col_dt, new byte[]{(byte) prop.getProperty("dt", "t").charAt(0)});
        final String flags = prop.getProperty("flags", "AAAAAA");
        this.entry.setCol(col_flags, (flags.length() > 6) ? plasmaSearchQuery.empty_constraint.bytes() : (new Bitfield(4, flags)).bytes());
        try {
			this.entry.setCol(col_lang, prop.getProperty("lang", "uk").getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			this.entry.setCol(col_lang, prop.getProperty("lang", "uk").getBytes());
		}
        this.entry.setCol(col_llocal, Integer.parseInt(prop.getProperty("llocal", "0")));
        this.entry.setCol(col_lother, Integer.parseInt(prop.getProperty("lother", "0")));
        this.entry.setCol(col_limage, Integer.parseInt(prop.getProperty("limage", "0")));
        this.entry.setCol(col_laudio, Integer.parseInt(prop.getProperty("laudio", "0")));
        this.entry.setCol(col_lvideo, Integer.parseInt(prop.getProperty("lvideo", "0")));
        this.entry.setCol(col_lapp, Integer.parseInt(prop.getProperty("lapp", "0")));
        this.snippet = crypt.simpleDecode(prop.getProperty("snippet", ""), null);
        this.word = null;
        if (prop.containsKey("word")) throw new kelondroException("old database structure is not supported");
        if (prop.containsKey("wi")) {
            this.word = new ReferenceRow(Base64Order.enhancedCoder.decodeString(prop.getProperty("wi", ""), "de.anomic.index.indexURLEntry.indexURLEntry()"));
        }
        this.ranking = 0;
    }

    public static MetadataRowContainer importEntry(final String propStr) {
        if (propStr == null || !propStr.startsWith("{") || !propStr.endsWith("}")) {
            return null;
        }
        try {
            return new MetadataRowContainer(serverCodings.s2p(propStr.substring(1, propStr.length() - 1)));
        } catch (final kelondroException e) {
                // wrong format
                return null;
        }
    }

    private StringBuilder corePropList() {
        // generate a parseable string; this is a simple property-list
        final URLMetadata metadata = this.metadata();
        final StringBuilder s = new StringBuilder(300);
        //System.out.println("author=" + comp.author());
        try {
            s.append("hash=").append(hash());
            s.append(",url=").append(crypt.simpleEncode(metadata.url().toNormalform(false, true)));
            s.append(",descr=").append(crypt.simpleEncode(metadata.dc_title()));
            s.append(",author=").append(crypt.simpleEncode(metadata.dc_creator()));
            s.append(",tags=").append(crypt.simpleEncode(metadata.dc_subject()));
            s.append(",ETag=").append(crypt.simpleEncode(metadata.ETag()));
            s.append(",mod=").append(DateFormatter.formatShortDay(moddate()));
            s.append(",load=").append(DateFormatter.formatShortDay(loaddate()));
            s.append(",fresh=").append(DateFormatter.formatShortDay(freshdate()));
            s.append(",referrer=").append(referrerHash());
            s.append(",md5=").append(md5());
            s.append(",size=").append(size());
            s.append(",wc=").append(wordCount());
            s.append(",dt=").append(doctype());
            s.append(",flags=").append(flags().exportB64());
            s.append(",lang=").append(language());
            s.append(",llocal=").append(llocal());
            s.append(",lother=").append(lother());
            s.append(",limage=").append(limage());
            s.append(",laudio=").append(laudio());
            s.append(",lvideo=").append(lvideo());
            s.append(",lapp=").append(lapp());
            
            if (this.word != null) {
                // append also word properties
                s.append(",wi=").append(Base64Order.enhancedCoder.encodeString(word.toPropertyForm()));
            }
            return s;

        } catch (final Exception e) {
            //          serverLog.logFailure("plasmaLURL.corePropList", e.getMessage());
            //          if (moddate == null) serverLog.logFailure("plasmaLURL.corePropList", "moddate=null");
            //          if (loaddate == null) serverLog.logFailure("plasmaLURL.corePropList", "loaddate=null");
            e.printStackTrace();
            return null;
        }
    }

    public Row.Entry toRowEntry() {
        return this.entry;
    }

    public String hash() {
        // return a url-hash, based on the md5 algorithm
        // the result is a String of 12 bytes within a 72-bit space
        // (each byte has an 6-bit range)
        // that should be enough for all web pages on the world
        return this.entry.getColString(col_hash, null);
    }

    public long ranking() {
    	return this.ranking;
    }
    
    public URLMetadata metadata() {
        final ArrayList<String> cl = FileUtils.strings(this.entry.getCol("comp", null), "UTF-8");
        return new URLMetadata(
                (cl.size() > 0) ? (cl.get(0)).trim() : "",
                hash(),
                (cl.size() > 1) ? (cl.get(1)).trim() : "",
                (cl.size() > 2) ? (cl.get(2)).trim() : "",
                (cl.size() > 3) ? (cl.get(3)).trim() : "",
                (cl.size() > 4) ? (cl.get(4)).trim() : "");
    }
    
    public Date moddate() {
        return decodeDate(col_mod);
    }

    public Date loaddate() {
        return decodeDate(col_load);
    }

    public Date freshdate() {
        return decodeDate(col_fresh);
    }

    public String referrerHash() {
        // return the creator's hash
        return entry.getColString(col_referrer, null);
    }

    public String md5() {
        // returns the md5 in hex representation
        return Digest.encodeHex(entry.getColBytes(col_md5));
    }

    public char doctype() {
        return (char) entry.getColByte(col_dt);
    }

    public String language() {
        return this.entry.getColString(col_lang, null);
    }

    public int size() {
        return (int) this.entry.getColLong(col_size);
    }

    public Bitfield flags() {
        return new Bitfield(this.entry.getColBytes(col_flags));
    }

    public int wordCount() {
        return (int) this.entry.getColLong(col_wc);
    }

    public int llocal() {
        return (int) this.entry.getColLong(col_llocal);
    }

    public int lother() {
        return (int) this.entry.getColLong(col_lother);
    }

    public int limage() {
        return (int) this.entry.getColLong(col_limage);
    }

    public int laudio() {
        return (int) this.entry.getColLong(col_laudio);
    }

    public int lvideo() {
        return (int) this.entry.getColLong(col_lvideo);
    }

    public int lapp() {
        return (int) this.entry.getColLong(col_lapp);
    }
    
    public String snippet() {
        // the snippet may appear here if the url was transported in a remote search
        // it will not be saved anywhere, but can only be requested here
        return snippet;
    }

    public Reference word() {
        return word;
    }

    public boolean isOlder(final MetadataRowContainer other) {
        if (other == null) return false;
        final Date tmoddate = moddate();
        final Date omoddate = other.moddate();
        if (tmoddate.before(omoddate)) return true;
        if (tmoddate.equals(omoddate)) {
            final Date tloaddate = loaddate();
            final Date oloaddate = other.loaddate();
            if (tloaddate.before(oloaddate)) return true;
            if (tloaddate.equals(oloaddate)) return true;
        }
        return false;
    }

    public String toString(final String snippet) {
        // add information needed for remote transport
        final StringBuilder core = corePropList();
        if (core == null)
            return null;

        core.ensureCapacity(core.length() + snippet.length() * 2);
        core.insert(0, "{");
        core.append(",snippet=").append(crypt.simpleEncode(snippet));
        core.append("}");

        return new String(core);
        //return "{" + core + ",snippet=" + crypt.simpleEncode(snippet) + "}";
    }

    public CrawlEntry toBalancerEntry(final String initiatorHash) {
        return new CrawlEntry(
                initiatorHash, 
                metadata().url(), 
                referrerHash(), 
                metadata().dc_title(),
                null,
                loaddate(), 
                null,
                0, 
                0, 
                0);
    }
    
    /**
     * @return the object as String.<br> 
     * This e.g. looks like this:
     * <pre>{hash=jmqfMk7Y3NKw,referrer=------------,mod=20050610,load=20051003,size=51666,wc=1392,cc=0,local=true,q=AEn,dt=h,lang=uk,url=b|aHR0cDovL3d3dy50cmFuc3BhcmVuY3kub3JnL3N1cnZleXMv,descr=b|S25vd2xlZGdlIENlbnRyZTogQ29ycnVwdGlvbiBTdXJ2ZXlzIGFuZCBJbmRpY2Vz}</pre>
     */
    public String toString() {
        final StringBuilder core = corePropList();
        if (core == null) return null;

        core.insert(0, "{");
        core.append("}");

        return new String(core);
        //return "{" + core + "}";
    }
    
}