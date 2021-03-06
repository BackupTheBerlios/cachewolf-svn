package CacheWolf;

import ewesoft.xml.*;
import ewesoft.xml.sax.*;
import ewe.io.*;
import ewe.sys.*;
import ewe.sys.Double;
import ewe.ui.*;
import ewe.util.*;
import ewe.data.PropertyList;
import ewe.net.*;
import java.lang.Integer.*;
import ewe.util.zip.*;

/**
*	Class to import Data from an GPX File. If cache data exists, the data from 
*	the GPX-File is ignored.
*	Class ID = 4000
*/
public class GPXImporter extends MinML {
	
	Vector cacheDB;
	CacheHolder holder;
	String strData, saveDir, logData, logIcon, logDate, logFinder;
	boolean inWpt, inCache, inLogs, inBug;
	public XMLElement document;
	private String file = new String();
	private boolean debugGPX = false; 
	InfoBox infB;
	static Preferences pref;
	boolean spiderOK = true;
	boolean doSpider = false;
	boolean fromOC = false;
	boolean nameFound = false;
	Locale l = Vm.getLocale();
	LocalResource lr = l.getLocalResource("cachewolf.Languages",true);
	int zaehlerGel = 0;
	Hashtable DBindex = new Hashtable();
	
	public GPXImporter(Vector DB, String f, Preferences p)
	{
		pref = p;
		cacheDB = DB;
		file = f;
		saveDir = pref.mydatadir;
		//msgA = msgArea;
		inWpt = false;
		inCache = false;
		inLogs = false;
		inBug =false;
		strData = new String();
		//index db for faster search
		CacheHolder ch;
		for(int i = 0; i<cacheDB.size();i++){
			ch = (CacheHolder)cacheDB.get(i);
			DBindex.put((String)ch.wayPoint, new Integer(i));
		}//for
	}
	
	public void doIt(){
		try{
			ewe.io.Reader r;
			InfoBox iB = new InfoBox("Spider?", "Spider Images?", InfoBox.CHECKBOX);
			iB.execute();
			doSpider = iB.mCB_state;
			//Vm.debug("State of: " + doSpider);
			Vm.showWait(true);
			//Test for zip.file
			if (file.indexOf(".zip") > 0){
				ZipFile zif = new ZipFile (file);
				ZipEntry zipEnt;
				Enumeration zipEnum = zif.entries();
				// there could be more than one file in the archive
				while (zipEnum.hasMoreElements())
				{
					zipEnt = (ZipEntry) zipEnum.nextElement();
					// skip over PRC-files
					if (zipEnt.getName().endsWith("gpx")){
						r = new ewe.io.InputStreamReader(zif.getInputStream(zipEnt));
						infB = new InfoBox(zipEnt.toString(),((String)lr.get(4000,"Loaded caches") + ":" + zaehlerGel));
						infB.show();
						parse(r);
						r.close();
						infB.close(0);
					}
				}
			}
			else {
				r = new ewe.io.InputStreamReader(new ewe.io.FileInputStream(file));
				infB = new InfoBox("Info",((String)lr.get(4000,"Loaded caches") + ":" + zaehlerGel));
				infB.show();
				parse(r);
				r.close();
				infB.close(0);
			}
			// save Index 
			CacheReaderWriter crw = new CacheReaderWriter();
			crw.saveIndex(cacheDB,saveDir);
			infB.close(0);
			Vm.showWait(false);
		}catch(Exception e){
			//Vm.debug(e.toString());
			Vm.showWait(false);
		}
	}
	public void startElement(String name, AttributeList atts){
		strData ="";
		if (name.equals("gpx")){
			// check for opencaching
			if (atts.getValue("creator").indexOf("opencaching")> 0) fromOC = true;
			else fromOC = false;
			zaehlerGel = 0;
		}
		if (name.equals("wpt")) {
			holder = new CacheHolder();
			holder.LatLon = latdeg2min(atts.getValue("lat")) + " " +londeg2min(atts.getValue("lon"));
			inWpt = true;
			inLogs = false;
			inBug = false;
			nameFound = false;
			return;
		}
		if (name.equals("groundspeak:cache")) {
			inCache = true;
			if (atts.getValue("available").equals("True"))
				holder.is_available = true;
			else 
				holder.is_available = false;
			if (atts.getValue("archived").equals("True"))
				holder.is_archived = true;
			else
				holder.is_archived = false;
			return;
		}

		if (name.equals("geocache")) {
			inCache=true;
			// get status
			String status = new String(atts.getValue("status"));
			holder.is_available = false;
			holder.is_archived = false;
			if (status.equals("Available")) holder.is_available = true;
			if (status.equals("Unavailable")) holder.is_available = false;
			if (status.equals("Draft")) holder.is_available = false;
			if (status.equals("Archived")) holder.is_archived = true;
			return;
		}
		
		if (name.equals("groundspeak:long_description")) {
			if (atts.getValue("html").toLowerCase().equals("true"))
				holder.is_HTML= true;
			else 
				holder.is_HTML = false;
			
		}
		if (name.equals("description")) {
			//set HTML always to true if from oc.de
			holder.is_HTML= true;
		}

		if (name.equals("groundspeak:logs") || name.equals("log")) {
			inLogs = true;
			return;
		}
		if (name.equals("groundspeak:travelbugs")) {
			inBug = true;
			return;
		}
		if (debugGPX){
			for (int i = 0; i < atts.getLength(); i++) {
				Vm.debug("Type: " + atts.getType(i) + " Name: " + atts.getName(i)+ " Value: "+atts.getValue(i));
			}
		}
	}
	
	public void endElement(String name){
		//Vm.debug("Ende: " + name);
		
		// logs
		if (inLogs){
			if (name.equals("groundspeak:date")|| name.equals("time"))  {
				logDate = new String(strData.substring(0,10));
				return;
			}
			if (name.equals("groundspeak:type") || name.equals("type")){
				logIcon = new String(typeText2Image(strData));
				return;
			}
			if (name.equals("groundspeak:finder")|| name.equals("geocacher")){
				logFinder = new String(strData);
				return;
			}
			if (name.equals("groundspeak:text") || name.equals("text")){ 
				logData = new String(strData);
				return;
			}
			if (name.equals("groundspeak:log") || name.equals("log") ) {
				holder.CacheLogs.add(logIcon + logDate + " by " + logFinder + "</strong><br>" + logData + "<br>");
				return;
			}
		}
		
		if (name.equals("wpt")){
			// Add cache Data only, if waypoint not already in database
			//if (searchWpt(cacheDB, holder.wayPoint)== -1){
			int index;
			//Vm.debug("here ?!?!?");
			index = searchWpt(holder.wayPoint);
			CacheReaderWriter crw = new CacheReaderWriter();
			//Vm.debug("chould be new!!!!");
			if (index == -1){
				//Vm.debug("here A");
				String loganal = new String();
				//Check for number sukzessive DNF logs
				int z = 0;
				while(z < holder.CacheLogs.size() && z < 5){
					loganal = (String)holder.CacheLogs.get(z);
					if(loganal.indexOf("icon_sad")>0) {
						z++;
					}else break;
				}
				holder.noFindLogs = z;
				zaehlerGel++;
				infB.setInfo( ((String)lr.get(4000,"Loaded caches") + ":" + zaehlerGel));
				holder.is_new = true;
				cacheDB.add(holder);
				//Vm.debug("here B");
				//if(doSpider == true && fromOC == false){
				// don't spider additional waypoints, so check
				// if waypoint starts with "GC"
				if(doSpider == true && holder.wayPoint.startsWith("GC")){
					if(spiderOK == true && holder.is_archived == false){
						spiderImages();
						if(holder.LatLon.length() > 1){
							ParseLatLon pll = new ParseLatLon(holder.LatLon,".");
							pll.parse();
							MapLoader mpl = new MapLoader(pll.getLatDeg(),pll.getLonDeg(), pref.myproxy, pref.myproxyport);
							mpl.loadTo(pref.mydatadir + "/" + holder.wayPoint + "_map.gif", "3");
							mpl.loadTo(pref.mydatadir + "/" + holder.wayPoint + "_map_2.gif", "10");
						}
					
						//Rename image sources
						String text = new String();
						String orig = new String();
						String imgName = new String();
						orig = holder.LongDescription;
						Extractor ex = new Extractor(orig, "<img src=\"", ">", 0, false);
						text = ex.findNext();
						int num = 0;
						while(ex.endOfSearch() == false && spiderOK == true){
							//Vm.debug("Replacing: " + text);
							imgName = (String)holder.ImagesText.get(num);
							holder.LongDescription = replace(holder.LongDescription, text, "[[Image: " + imgName + "]]");
							num++;
							text = ex.findNext();
						}
						
					}
				}
				crw.saveCacheDetails(holder,saveDir);
				crw.saveIndex(cacheDB,saveDir);
			}
			//Update cache data
			else {
				//Vm.debug("it is not new!");
				CacheHolder oldCh = new CacheHolder();
				oldCh = (CacheHolder) cacheDB.get(index);
				try {
					//Vm.debug("Try to load");
					crw.readCache(oldCh, saveDir);
					//Vm.debug("Done loading");
				} catch (Exception e) {Vm.debug("Could not open file: " + e.toString());};
				oldCh.update(holder);
				crw.saveCacheDetails(oldCh,saveDir);
				crw.saveIndex(cacheDB,saveDir);
			}
			
			inWpt = false;
			return;
		}
		if (name.equals("sym")&& strData.endsWith("Found")) {
			holder.is_found = true;
			return;
		}
		if (name.equals("groundspeak:travelbugs")) {
			inBug = false;
			return;
		}
		if (name.equals("groundspeak:travelbug")) {
			holder.has_bug = true;
			return;
		}
		if (name.equals("groundspeak:name")&& inBug) {
			holder.Bugs += "<b>Name:</b> " + strData + "<br><hr>";  
			return;
		}
		
		if (name.equals("time") && inWpt) {
			String Date = new String();
			Date = strData.substring(5,7); // month
			Date += "/" + strData.substring(8,10); // day
			Date += "/" + strData.substring(0,4); // year
			holder.DateHidden = Date;
			return;
		}
		// cache information
		if (name.equals("groundspeak:cache") || name.equals("geocache")) {
			inCache = false;
		}
		
		if (name.equals("name") && inWpt && !inCache) {
			holder.wayPoint = strData;
			//msgA.setText("import " + strData);
			return;
		}
		//Vm.debug("Check: " + inWpt + " / " + fromOC);
		//if (name.equals("desc") && inWpt && fromOC) {
		// fill name with contents of <desc>, in case of gc.com the name is
		// later replaced by the contents of <groundspeak:name> which is shorter
		if (name.equals("desc")&& inWpt ) {
			holder.CacheName = strData;
			//Vm.debug("CacheName: " + strData);
			//msgA.setText("import " + strData);
			return;
		}
		if (name.equals("url")&& inWpt){
			holder.URL = strData;
			return;
		}
		
		// Text for additional waypoints, no HTML
		if (name.equals("cmt")&& inWpt){
			holder.LongDescription = strData;
			holder.is_HTML = false;
			return;
		}
		
		// aditional wapypoint
		if (name.equals("type")&& inWpt && !inCache && strData.startsWith("Waypoint")){
			holder.type= Common.typeText2Number(strData);
			holder.CacheSize = "None";
		}

		
		if (name.equals("groundspeak:name")&& inCache) {
			holder.CacheName = strData;
			return;
		}
		if (name.equals("groundspeak:owner") || name.equals("owner")) {
			holder.CacheOwner = strData;
			if(pref.myAlias.equals(strData)) holder.is_owned = true;
			return;
		}
		if (name.equals("groundspeak:difficulty") || name.equals("difficulty")) {
			holder.hard = strData.replace('.',',');
			return;
		}
		if (name.equals("groundspeak:terrain")|| name.equals("terrain")) {
			holder.terrain = strData.replace('.',',');
			return;
		}
		if ((name.equals("groundspeak:type") || name.equals("type"))&& inCache){
			holder.type= Common.typeText2Number(strData);
			return;
		}
		if (name.equals("groundspeak:container")|| name.equals("container")){
			holder.CacheSize = strData;
			return;
		}
		if (name.equals("groundspeak:short_description")|| name.equals("summary")) {
			holder.LongDescription =strData;
			return;
		}

		if (name.equals("groundspeak:long_description")|| name.equals("description")) {
			holder.LongDescription +=strData;
			return;
		}
		if (name.equals("groundspeak:encoded_hints") || name.equals("hints")) {
			holder.Hints = Common.rot13(strData);
			return;
		}


	}
	public void characters(char[] ch,int start,int length){
		String chars = new String(ch,start,length);
		strData += chars;
		if (debugGPX) Vm.debug("Char: " + chars);
	}
	
	public String latdeg2min(String lat){
		String res = new String();
		String deg = new String();
		String min = new String();
		Double minDouble = new Double();
		
		// Get degrees
		if (lat.indexOf('.') < 0) lat = lat + ".0";
		deg = lat.substring(0, lat.indexOf('.'));
		if (deg.substring(0,1).equals("-"))res = "S" + deg.substring(1)+ "� "; 
		else  res = "N " + deg + "� ";

		// Get minutes
		min = lat.substring(lat.indexOf('.')+1);
		minDouble.set(Common.parseDouble("0." +min)*60);
		minDouble.decimalPlaces = 3;
				
		// and back to string
		min = minDouble.toString().replace(',','.');
		// add leading '0'
		if (min.indexOf('.') == 1) min = "0" + min;
		// Build return string
		res += min;
		return res;
	}
	public String londeg2min(String lon){
		String res = new String();
		String deg = new String();
		String min = new String();
		Double minDouble = new Double();
		
		
		// Get degrees
		if (lon.indexOf('.') < 0) lon = lon + ".0";
		deg = lon.substring(0, lon.indexOf('.'));
		if (deg.substring(0,1).equals("-")){
			res = "W ";
			deg = replace(deg, "-","");
		} else  res = "E ";
		// fill up leading '0'
		for (int i=deg.length();i<3;i++)
			res += "0";
		res += deg + "� ";
		// Get minutes
		min = lon.substring(lon.indexOf('.')+1);
		minDouble.set(Common.parseDouble("0."+ min) * 60);
		minDouble.decimalPlaces = 3;
				
		// and back to string
		min = minDouble.toString().replace(',','.');
		// add leading '0'
		if (min.indexOf('.') == 1) min = "0" + min;
		// Build return string
		res += min;
		return res;
	}

	public String typeText2Image(String typeText){
		if (typeText.equals("Found it")||typeText.equals("Found")) return "<img src='icon_smile.gif'>&nbsp;";
		if (typeText.equals("Didn't find it")||typeText.equals("Not Found")) return "<img src='icon_sad.gif'>&nbsp;";
		if (typeText.equals("Write note")||typeText.equals("Note")
			||typeText.equals("Not Attempted")||typeText.equals("Other")) return "<img src='icon_note.gif'>&nbsp;";
		if (typeText.equals("Enable Listing")) return "<img src='icon_enabled.gif'>&nbsp;";
		if (typeText.equals("Temporarily Disable Listing")) return "<img src='icon_disabled.gif'>&nbsp;";
		if (typeText.equals("Webcam Photo Taken")) return "<img src='11.png'>&nbsp;";
		if (typeText.equals("Attended")) return "<img src='icon_attended.gif'>&nbsp;";
		if (typeText.equals("Publish Listing")) return "<img src='green.png'>&nbsp;";
		if (typeText.equals("Will Attend")) return "<img src='icon_rsvp.gif'>&nbsp;";
		if (typeText.equals("Post Reviewer Note")) return "<img src='big_smile.gif'>&nbsp;";
		if (typeText.equals("Unarchive")) return "<img src='traffic_cone.gif'>&nbsp;";
		if (typeText.equals("Archive (show)")) return "<img src='traffic_cone.gif'>&nbsp;";
		//Vm.debug("Unknown Log Type:" + typeText);
		return typeText +"&nbsp;";
	}

	/**
	* Method to iterate through cache database and look for waypoint.
	* Returns value >= 0 if waypoint is found, else -1
	*/
	private int searchWpt(Vector db, String wpt){
		if(wpt.length()>0){
			wpt = wpt.toUpperCase();
			CacheHolder ch = new CacheHolder();
			//Search through complete database
			for(int i = 0;i < db.size();i++){
				ch = (CacheHolder)db.get(i);
				if(ch.wayPoint.indexOf(wpt) >=0 ){
					return i;
				}
			} // for
		} // if
		return -1;
	}
	private int searchWpt(String wpt){
		Integer INTR = (Integer)DBindex.get(wpt);
		if(INTR != null){
			return INTR.intValue();
		} else return -1;
	}
	
	private void spiderImages(){
		//Vm.debug("Spider now: " + holder.wayPoint);
		Extractor cacheEx;
		String addr = new String();
		String longDesc = new String();
		String dummy = new String();
		String dummy2 = new String();
		String imageType = new String();
		String datei = new String();
		int imgCounter = 0;
		HttpConnection connImg;
		Socket sockImg;
		InputStream is;
		FileOutputStream fos;
		int bytes_read;
		byte[] buffer = new byte[9000];
		ByteArray daten;
		String cacheText = new String();
		
		addr = "http://www.geocaching.com/seek/cache_details.aspx?wp=" + holder.wayPoint ;
		Vm.debug("Address: " + addr);
		try{
			cacheText = fetch(addr);
		}catch(IOException iox){
			Vm.debug("Error fetching cache page from gc.com");
			spiderOK = false;
			cacheText = "";
		}
		cacheText = replace(cacheText, "<strong>", "<STRONG>");
		// Images in the cache long description
		cacheEx = new Extractor(cacheText, "<span id=\"ShortDescription\">", "<STRONG>Additional Hints&nbsp;",0,true);
		longDesc = cacheEx.findNext();
		//Vm.debug(longDesc);
		if(longDesc.length() == 0) {
			cacheEx = new Extractor(cacheText, "<span id=\"LongDescription\">", "<STRONG>Additional Hints&nbsp;",0,true);
			longDesc = cacheEx.findNext();
		}
		cacheEx = new Extractor(longDesc, "<img", ">", 0, true);
		String dummySrc = new String();
		dummySrc = cacheEx.findNext();
		dummySrc = replace(dummySrc, "SRC", "src");
		dummySrc =dummySrc.replace('\'', '\"');
		Extractor dummySrcExt;
		while(cacheEx.endOfSearch() == false){
			dummySrcExt = new Extractor(dummySrc, "src=\"", "\"",0,true);
			dummy = dummySrcExt.findNext();
			if(dummy.length() == 0){
				dummySrcExt = new Extractor(dummySrc, "src = \"", "\"",0,true);
				dummy = dummySrcExt.findNext();
			}
			//Vm.debug("Trying for: " + holder.wayPoint + " / " + dummySrc);
			//Vm.debug("Url = " + dummy);
			if(dummy.length()>0){
				if(pref.myproxy.length()>0){
					connImg = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), dummy);
				}else{
					connImg = new HttpConnection(dummy);
				}
				connImg.setRequestorProperty("Connection", "close");
				imageType = dummy.substring(dummy.lastIndexOf("."), dummy.lastIndexOf(".")+4);
				datei = pref.mydatadir + holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType;
				//if(imageType.equals(".png") || imageType.equals(".gif") || imageType.equals(".tif") || imageType.equals(".jpg")){
					try{
						sockImg = connImg.connect();
						daten = connImg.readData(connImg.connect());
						fos = new FileOutputStream(new File(datei));
						fos.write(daten.toBytes());
						fos.close();
						sockImg.close();
						holder.Images.add(holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType);
						//Vm.debug(holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType);
						holder.ImagesText.add(dummy.substring(dummy.lastIndexOf("/")+1, dummy.lastIndexOf("/")+1+dummy.length()-dummy.lastIndexOf("/")-1));
						//Vm.debug("adding...." +dummy.substring(dummy.lastIndexOf("/")+1, dummy.lastIndexOf("/")+1+dummy.length()-dummy.lastIndexOf("/")-1));
					} catch (UnknownHostException e) { 
						Vm.debug("Host not there...");
					}catch(IOException ioex){
						Vm.debug("File not found!");
					} catch (Exception ex){
						Vm.debug("Some kind of problem!");
					} finally {
						//Vm.debug("This is stupid!!");
					}
					imgCounter++;
			}
			dummySrc = cacheEx.findNext();
			//Vm.debug("Dummy SRC = " + dummySrc);
			dummySrc = replace(dummySrc, "SRC", "src");
			dummySrc =dummySrc.replace('\'', '\"');
		}
		
		// Images in the image span
		cacheEx = new Extractor(cacheText, "<span id=\"Images\"", "</span>",0,true);
		longDesc = cacheEx.findNext();
		//Vm.debug("In image span: " + longDesc);
		cacheEx = new Extractor(longDesc, ";<A HREF='http://img.geocaching.com/cache/", "' target='_blank'", 0, true);
		dummy = cacheEx.findNext();
		//Vm.debug("And this is the target: " + dummy);
		Extractor imgEx = new Extractor(longDesc, "style='text-decoration: underline;'>","</A>",0,true);
		dummy2 = imgEx.findNext();
		while(cacheEx.endOfSearch() == false){
			dummy = "http://img.geocaching.com/cache/"+dummy;
			//Vm.debug("Target= " +dummy);
			if(pref.myproxy.length()>0){
				connImg = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), dummy);
			}else{
				connImg = new HttpConnection(dummy);
			}
			connImg.setRequestorProperty("Connection", "close");
			imageType = dummy.substring(dummy.lastIndexOf("."), dummy.lastIndexOf(".")+4);
			datei = pref.mydatadir + holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType;
			try{
				sockImg = connImg.connect();
				daten = connImg.readData(sockImg);
				fos = new FileOutputStream(new File(datei));
				fos.write(daten.toBytes());
				fos.close();
				sockImg.close();
				holder.Images.add(holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType);
				holder.ImagesText.add(dummy2);
				//Vm.debug(holder.wayPoint + "_" + Convert.toString(imgCounter)+ imageType);
				//Vm.debug(dummy2);
			}catch(IOException ioex){
				Vm.debug("File not found!");
			}
			imgCounter++;
			dummy = cacheEx.findNext();
			dummy2 = imgEx.findNext();
		}
		
	}
	
	private static String fetch(String address) throws IOException
	   	{
			HttpConnection conn;
			if(pref.myproxy.length() > 0){
				conn = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), address);
				//Vm.debug(address);
			} else {
				conn = new HttpConnection(address);
			}
			conn.setRequestorProperty("USER_AGENT", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
			conn.setRequestorProperty("Connection", "close");
			conn.documentIsEncoded = true;
			Socket sock = conn.connect();
			ByteArray daten = conn.readData(sock);
			JavaUtf8Codec codec = new JavaUtf8Codec();
			CharArray c_data = codec.decodeText(daten.data, 0, daten.length, true, null);
			//Vm.debug(c_data.toString());
			sock.close();
			return c_data.toString();
		}
	
	public static String replace(String source, String pattern, String replace){
		if (source!=null)
		{
			final int len = pattern.length();
			StringBuffer sb = new StringBuffer();
			int found = -1;
			int start = 0;
		
			while( (found = source.indexOf(pattern, start) ) != -1) {
			    sb.append(source.substring(start, found));
			    sb.append(replace);
			    start = found + len;
			}
		
			sb.append(source.substring(start));
		
			return sb.toString();
		}
		else return "";
	}
}
