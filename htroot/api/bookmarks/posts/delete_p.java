
import java.net.MalformedURLException;

import de.anomic.http.httpRequestHeader;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacyURL;

public class delete_p {
    public static serverObjects respond(final httpRequestHeader header, final serverObjects post, final serverSwitch<?> env) {
        // return variable that accumulates replacements
        final plasmaSwitchboard switchboard = (plasmaSwitchboard) env;
        final serverObjects prop = new serverObjects();
        final boolean isAdmin=switchboard.verifyAuthentication(header, true);        
        if(post!= null){
    		if(!isAdmin){
    			// force authentication if desired
        			if(post.containsKey("login")){
        				prop.put("AUTHENTICATE","admin log-in");
        			}
        			return prop;
    		} 
        	try {
                if( post.containsKey("url") && switchboard.bookmarksDB.removeBookmark((new yacyURL(post.get("url", "nourl"), null)).hash())) {
                	prop.put("result", "1");
                }else if(post.containsKey("urlhash") && switchboard.bookmarksDB.removeBookmark(post.get("urlhash", "nohash"))){
                	prop.put("result", "1");
                }else{
                	prop.put("result", "0");
                }
            } catch (final MalformedURLException e) {
                prop.put("result", "0");
            }
        }else{
        	prop.put("result", "0");
        }        
        // return rewrite properties
        return prop;
    }    
}


