package de.anomic.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import net.yacy.kelondro.data.meta.DigestURI;

public class MimeTable {

    private static final Properties mimeTable = new Properties();
    
    public static void init(File mimeFile) {
        if (mimeTable.size() == 0) {
            // load the mime table
            BufferedInputStream mimeTableInputStream = null;
            try {
                mimeTableInputStream = new BufferedInputStream(new FileInputStream(mimeFile));
                mimeTable.load(mimeTableInputStream);
            } catch (final Exception e) {                
                e.printStackTrace();
            } finally {
                if (mimeTableInputStream != null) try { mimeTableInputStream.close(); } catch (final Exception e1) {}                
            }
        }
    }
    
    public static int size() {
        return mimeTable.size();
    }
    
    public static String ext2mime(String ext) {
        return mimeTable.getProperty(ext, "application/" + ext);
    }
    
    public static String ext2mime(String ext, String dfltMime) {
        return mimeTable.getProperty(ext, dfltMime);
    }
    
    public static String url2mime(DigestURI url, String dfltMime) {
        return ext2mime(url.getFileExtension(), dfltMime);
    }
    
    public static String url2mime(DigestURI url) {
        return ext2mime(url.getFileExtension());
    }
}