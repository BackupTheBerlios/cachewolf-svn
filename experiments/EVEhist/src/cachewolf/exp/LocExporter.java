package cachewolf.exp;
import cachewolf.*;
import cachewolf.utils.Common;
import eve.io.File;
import ewesoft.xml.*;
import ewesoft.xml.sax.*;

/**
*	Class to export the cache database into an geocaching .loc file that may be exported
*	by GPSBabel to a Garmin GPS.
*
*	Now includes mapping of cachetypes to user defined icons (as defined in file garminmap.xml).
*/
public class LocExporter extends Exporter{
	public static final int MODE_AUTO = TMP_FILE;
	/**
	 * Defines how certain cachetypes are mapped to user icons
	 */
	private static GarminMap gm=null;
	
	public LocExporter(){
		super();
		this.setMask("*.loc");
		this.setHowManyParams(NO_PARAMS);
		if ((new File(File.getProgramDirectory()+"/garminmap.xml")).exists()) {
			gm=new GarminMap();
			gm.readGarminMap();
		}
	}
	
	public String header () {
		return "<?xml version=\"1.0\"?><loc version=\"1.0\" src=\"EasyGPS\">\r\n";
	}
	
	public String record(CacheHolderDetail chD){
		// filter out not valid coords
		if (!chD.pos.isValid()) return null;
		StringBuffer strBuf = new StringBuffer(200);
		strBuf.append("<waypoint>\r\n   <name id=\"");
		String wptName=simplifyString(chD.wayPoint);
		if (Global.getPref().garminMaxLen==0)
			strBuf.append(wptName);
		else {
			try {
				strBuf.append(wptName.substring(wptName.length()-Global.getPref().garminMaxLen));
			} catch (Exception ex){ pref.log("Invalid value for garmin.MaxWaypointLength"); }
		}
		strBuf.append("\"><![CDATA[");
		strBuf.append(simplifyString(chD.cacheName));
		strBuf.append("]]></name>\r\n   <coord lat=\"");
		strBuf.append(chD.pos.getLatDeg(CWPoint.DD));
		strBuf.append("\" lon=\"");
		strBuf.append(chD.pos.getLonDeg(CWPoint.DD));
		strBuf.append("\"/>\r\n   <type>");
		if (gm!=null) {
			strBuf.append(gm.getIcon(chD));
		} else {
			if (chD.is_found)
				strBuf.append("Geocache Found");
			else
				strBuf.append("Geocache");
		}
		strBuf.append("</type>\r\n</waypoint>\r\n");
		return strBuf.toString();
	}
	public String trailer(){
		return "</loc>\r\n";
	}
	
	/**
	 * This class implements user defined icons which depend on the cache type and the found status.
	 * See also http://www.geoclub.de/ftopic10413.html
	 * @author salzkammergut
	 *
	 */
	private class GarminMap extends MinML {
		
		private IconMap[] symbols=new IconMap[24];
		private int mapSize=0;
		
		String lastName;
		public void readGarminMap(){
			try{
				String datei = File.getProgramDirectory() + "/garminmap.xml";
				java.io.Reader r = new java.io.InputStreamReader(new java.io.FileInputStream(datei));
				parse(r);
				r.close();
			}catch(Exception e){
				if (e instanceof NullPointerException)
					Global.getPref().log("Error reading garminmap.xml: NullPointerException in Element "+lastName +". Wrong attribute?",e,true);
				else 
					Global.getPref().log("Error reading garminmap.xml: ", e);
			}
		}
		public void startElement(String name, AttributeList atts){
			lastName=name;
			if (name.equals("icon")) {
				symbols[mapSize]=new IconMap(Common.parseInt(atts.getValue("type")),atts.getValue("name"),atts.getValue("found"));
				mapSize++;
			}
		}		
		
		public String getIcon(CacheHolderDetail chD) {
			// First check if there is a mapping for "cache found"
			if (chD.is_found) {
				for (int i=0; i<mapSize; i++)
					if (symbols[i].onlyIfFound!=null && symbols[i].type==chD.type) return symbols[i].name;
			}
			// Now try mapping the cache irrespective of the "found" status
			for (int i=0; i<mapSize; i++)
				if (symbols[i].type==chD.type) return symbols[i].name;
		
			// If it is not a mapped type, just use the standard mapping
			if (chD.is_found)
				return "Geocache Found";
			return "Geocache";
		}
		
		private class IconMap {
			public int type;
			public Boolean onlyIfFound;
			public String name;
			
			IconMap(int type, String name, String onlyIfFound) {
				this.type=type;
				this.name=name;
				if (onlyIfFound!=null && onlyIfFound.equals("1"))
					this.onlyIfFound=Boolean.TRUE;
				else
					this.onlyIfFound=null;
			}
		}
	
	}
	
}
