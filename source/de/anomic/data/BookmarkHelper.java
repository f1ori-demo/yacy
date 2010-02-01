// BookmarkHelper.java 
// -------------------------------------
// part of YACY
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// Methods from this file has been originally contributed by Alexander Schier
// and had been refactored by Michael Christen for better a method structure 30.01.2010
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

package de.anomic.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.anomic.data.bookmarksDB.Bookmark;
import de.anomic.data.bookmarksDB.Tag;
import net.yacy.document.parser.html.ContentScraper;
import net.yacy.document.parser.html.TransformerWriter;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.data.word.Word;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.DateFormatter;
import net.yacy.kelondro.util.FileUtils;

public class BookmarkHelper {

    public static String cleanTagsString(String tagsString) {
        
        // get rid of heading, trailing and double commas since they are useless
        while (tagsString.length() > 0 && tagsString.charAt(0) == ',') {
            tagsString = tagsString.substring(1);
        }
        while (tagsString.endsWith(",")) {
            tagsString = tagsString.substring(0,tagsString.length() -1);
        }
        while (tagsString.contains(",,")){
            tagsString = tagsString.replaceAll(",,", ",");
        }
        // get rid of double and trailing slashes
        while (tagsString.endsWith("/")){
            tagsString = tagsString.substring(0, tagsString.length() -1);
        }
        while (tagsString.contains("/,")){
            tagsString = tagsString.replaceAll("/,", ",");
        }
        while (tagsString.contains("//")){
            tagsString = tagsString.replaceAll("//", "/");
        }
        // space characters following a comma are removed
        tagsString = tagsString.replaceAll(",\\s+", ","); 
        
        return tagsString;
    }
    

    /**
     * returns an object of type String that contains a tagHash
     * @param tagName an object of type String with the name of the tag. 
     *        tagName is converted to lower case before hash is generated!
     */
    public static String tagHash(final String tagName){
        return new String(Word.word2hash(tagName.toLowerCase()));
    }
    /*
    private static String tagHash(final String tagName, final String user){
        return new String(Word.word2hash(user+":"+tagName.toLowerCase()));
    }
    */
    


    // --------------------------------------
    // bookmarksDB's Import/Export functions
    // --------------------------------------
    
    public static int importFromBookmarks(bookmarksDB db, final DigestURI baseURL, final String input, final String tag, final boolean importPublic){
        try {
            // convert string to input stream
            final ByteArrayInputStream byteIn = new ByteArrayInputStream(input.getBytes("UTF-8"));
            final InputStreamReader reader = new InputStreamReader(byteIn,"UTF-8");
            
            // import stream
            return importFromBookmarks(db, baseURL, reader, tag, importPublic);
        } catch (final UnsupportedEncodingException e) { 
            return 0;
        }           
    }
    
    private static int importFromBookmarks(bookmarksDB db, final DigestURI baseURL, final InputStreamReader input, final String tag, final boolean importPublic){
            
        int importCount = 0;
        
        Map<DigestURI, String> links = new HashMap<DigestURI, String>();
        String title;
        DigestURI url;
        Bookmark bm;
        final Set<String> tags=listManager.string2set(tag); //this allow multiple default tags
        try {
            //load the links
            final ContentScraper scraper = new ContentScraper(baseURL);         
            //OutputStream os = new htmlFilterOutputStream(null, scraper, null, false);
            final Writer writer= new TransformerWriter(null,null,scraper, null, false);
            FileUtils.copy(input,writer);
            writer.close();
            links = scraper.getAnchors();           
        } catch (final IOException e) { Log.logWarning("BOOKMARKS", "error during load of links: "+ e.getClass() +" "+ e.getMessage());}
        for (Entry<DigestURI, String> link: links.entrySet()) {
            url= link.getKey();
            title=link.getValue();
            Log.logInfo("BOOKMARKS", "links.get(url)");
            if(title.equals("")){//cannot be displayed
                title=url.toString();
            }
            bm=db.new Bookmark(url.toString());
            bm.setProperty(Bookmark.BOOKMARK_TITLE, title);
            bm.setTags(tags);
            bm.setPublic(importPublic);
            db.saveBookmark(bm);
            
            importCount++;
        }
        
        return importCount;
    }
    
    
    public static int importFromXML(bookmarksDB db, final String input, final boolean importPublic){        
        try {
            // convert string to input stream
            final ByteArrayInputStream byteIn = new ByteArrayInputStream(input.getBytes("UTF-8"));
            
            // import stream
            return importFromXML(db, byteIn,importPublic);
        } catch (final UnsupportedEncodingException e) { 
            return 0;
        }       
    }
    
    private static int importFromXML(bookmarksDB db, final InputStream input, final boolean importPublic){
        final DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            final Document doc=builder.parse(input);
            return parseXMLimport(db, doc, importPublic);
        } catch (final ParserConfigurationException e) {  
        } catch (final SAXException e) {
        } catch (final IOException e) {
        }
        return 0;
        
    }
    
    private static int parseXMLimport(bookmarksDB db, final Node doc, final boolean importPublic){
        int importCount = 0;
        if (doc.getNodeName().equals("post")) {
            final NamedNodeMap attributes = doc.getAttributes();
            final String url=attributes.getNamedItem("href").getNodeValue();
            if(url.equals("")){
                return 0;
            }
            final Bookmark bm=db.new Bookmark(url);
            String tagsString="";
            String title="";
            String description="";
            String time="";
            if(attributes.getNamedItem("tag")!=null){
                tagsString=attributes.getNamedItem("tag").getNodeValue();
            }
            if(attributes.getNamedItem("description")!=null){
                title=attributes.getNamedItem("description").getNodeValue();
            }
            if(attributes.getNamedItem("extended")!=null){
                description=attributes.getNamedItem("extended").getNodeValue();
            }
            if(attributes.getNamedItem("time")!=null){
                time=attributes.getNamedItem("time").getNodeValue();
            }
            Set<String> tags=new HashSet<String>();
            
            if(title != null){
                bm.setProperty(Bookmark.BOOKMARK_TITLE, title);
            }
            if(tagsString!=null){
                tags = listManager.string2set(tagsString.replace(' ', ','));
            }
            bm.setTags(tags, true);
            if(time != null){
                
                Date parsedDate = null;
                try {
                    parsedDate = DateFormatter.parseISO8601(time);
                } catch (final ParseException e) {
                    parsedDate = new Date();
                }               
                bm.setTimeStamp(parsedDate.getTime());
            }
            if(description!=null){
                bm.setProperty(Bookmark.BOOKMARK_DESCRIPTION, description);
            }
            bm.setPublic(importPublic);
            db.saveBookmark(bm);
            
            importCount++;
        }
        final NodeList children=doc.getChildNodes();
        if(children != null){
            for (int i=0; i<children.getLength(); i++) {
                importCount += parseXMLimport(db, children.item(i), importPublic);
            }
        }
        
        return importCount;
    }
    
    public static Iterator<String> getFolderList(final String root, Iterator<Tag> tagIterator) {
        
        final Set<String> folders = new TreeSet<String>();
        String path = "";
        Tag tag;
        
        while (tagIterator.hasNext()) {
            tag=tagIterator.next();          
            if (tag.getFriendlyName().startsWith((root.equals("/") ? root : root+"/"))) {
                path = tag.getFriendlyName();
                path = BookmarkHelper.cleanTagsString(path);                  
                while(path.length() > 0 && !path.equals(root)){
                    folders.add(path);                  
                    path = path.replaceAll("(/.[^/]*$)", "");   // create missing folders in path
                }                   
            }
        }
        if (!root.equals("/")) { folders.add(root); }
        folders.add("\uffff");
        return folders.iterator();      
    }
}