/*
    CacheWolf is a software for PocketPC, Win and Linux that
    enables paperless caching.
    It supports the sites geocaching.com and opencaching.de

    Copyright (C) 2006  CacheWolf development team
    See http://developer.berlios.de/projects/cachewolf/
    for more information.
    Contact: 	bilbowolf@users.berlios.de
		kalli@users.berlios.de

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation version 2 of the License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
    */

package CacheWolf;
import ewe.net.*;
import ewe.io.*;
import ewe.sys.*;
import ewe.util.*;
import com.stevesoft.ewe_pat.*;
import ewe.ui.*;
import ewe.data.Property;
import ewe.data.PropertyList;

/**
*	Class to spider caches from gc.com
*/
public class SpiderGC{

	/**
	 * The maximum number of logs that will be stored
	 */
	public static int MAXLOGS=250; // Can be pre-set from preferences
	public static String passwort = ""; // Can be pre-set from preferences
	public static boolean loggedIn = false;

	private static int ERR_LOGIN = -10;
	private static Preferences pref;
	private Profile profile;
	private static String viewstate = "";
	private static String cookieID = "";
	private static String cookieSession = "";
	private static double distance = 0;
	private Regex inRex = new Regex();
	private Vector cacheDB;
	private Vector cachesToLoad = new Vector();
	private Hashtable indexDB;
	private InfoBox infB;
	private static myProperties p=null;

	public SpiderGC(Preferences prf, Profile profile, boolean bypass){
		this.profile=profile;
		this.cacheDB = profile.cacheDB;
		pref = prf;
		if (p==null) {
			pref.logInit();
			p=new myProperties();
		}
		MAXLOGS=pref.maxLogsToSpider;
	}

	/**
	 * Method to login the user to gc.com
	 * It will request a password and use the alias defined in preferences
	 * If the login page cannot be fetched, the password is cleared.
	 * If the login fails, an appropriate message is displayed.
	 */
	public int login(){
		loggedIn = false;
		String start,doc,loginPage,loginSuccess,nextPage;
		try {
			loginPage=p.getProp("loginPage");
			loginSuccess=p.getProp("loginSuccess");
			nextPage=p.getProp("nextPage");
		} catch (Exception ex) { // Tag not found in spider.def
			return ERR_LOGIN;
		}
		//Get password
		InfoBox infB = new InfoBox(MyLocale.getMsg(5506,"Password"), MyLocale.getMsg(5505,"Enter Password"), InfoBox.INPUT);
		infB.feedback.setText(passwort); // Remember the PWD for next time
		infB.feedback.isPassword=true;
		int code = infB.execute();
		passwort = infB.getInput();
		infB.close(0);
		if(code != Form.IDOK) return code;

		// Now start the login proper
		infB = new InfoBox(MyLocale.getMsg(5507,"Status"), MyLocale.getMsg(5508,"Logging in..."));
		infB.exec();
		try{
			pref.log("Fetching login page");
			//Access the page once to get a viewstate
			start = fetch(loginPage);   //http://www.geocaching.com/login/Default.aspx
		} catch(Exception ex){
			infB.close(0);
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5499,"Error loading login page"), MessageBox.OKB)).execute();
			pref.log("Could not fetch: gc.com login page",ex);
			passwort="";
			return ERR_LOGIN;
		}
		if (!infB.isClosed) { // If user has not aborted, we continue
			Regex rexCookieID = new Regex("(?i)Set-Cookie: userid=(.*?);.*");
			Regex rex = new Regex("name=\"__VIEWSTATE\" value=\"(.*?)\" />");
			Regex rexCookieSession = new Regex("(?i)Set-Cookie: ASP.NET_SessionId=(.*?);.*");
			rex.search(start);
			if(rex.didMatch()){
				viewstate = rex.stringMatched(1);
				//Vm.debug("ViewState: " + viewstate);
			} else
				pref.log("Viewstate not found before login");
			//Ok now login!
			try{
				pref.log("Logging in as "+pref.myAlias);
				doc = URL.encodeURL("__VIEWSTATE",false) +"="+ URL.encodeURL(viewstate,false)
					+ "&" + URL.encodeURL("myUsername",false) +"="+ encodeUTF8(new String(Utils.encodeJavaUtf8String(pref.myAlias)))
				    + "&" + URL.encodeURL("myPassword",false) +"="+ encodeUTF8(new String(Utils.encodeJavaUtf8String(passwort)))
				    + "&" + URL.encodeURL("cookie",false) +"="+ URL.encodeURL("on",false)
				    + "&" + URL.encodeURL("Button1",false) +"="+ URL.encodeURL("Login",false);
				start = fetch_post(loginPage, doc, nextPage);  // /login/default.aspx
				if(start.indexOf(loginSuccess) > 0)
					pref.log("Login successful");
				else {
					pref.log("Login failed. Wrong Account or Password?");
					infB.close(0);
				    (new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5501,"Login failed! Wrong account or password?"), MessageBox.OKB)).execute();
					return ERR_LOGIN;
				}
			}catch(Exception ex){
				pref.log("Login failed.", ex);
				infB.close(0);
			    (new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5501,"Login failed. Error loading page after login."), MessageBox.OKB)).execute();
				return ERR_LOGIN;
			}

			rex.search(start);
			if (!rex.didMatch()) {
				pref.log("Viewstate not found");
			}
			viewstate = rex.stringMatched(1);
			rexCookieID.search(start);
			if (!rexCookieID.didMatch()) {
				pref.log("CookieID not found");
			}
			cookieID = rexCookieID.stringMatched(1);
			//Vm.debug(cookieID);
			rexCookieSession.search(start);
			if (!rexCookieSession.didMatch()) {
				pref.log("CookieSession not found Using old one.");
				//cookieSession="";
			} else
				cookieSession = rexCookieSession.stringMatched(1);
			//Vm.debug("cookieSession = " + cookieSession);
		}
		boolean loginAborted=infB.isClosed;
		infB.close(0);
		if (loginAborted)
			return Form.IDCANCEL;
		else {
			loggedIn = true;
			return Form.IDOK;
		}
	}

	/**
	 * Method to spider a single cache.
	 * It assumes a login has already been performed!
	 * @return True if spider was successful, false if spider was cancelled by closing the infobox
	 */
	public boolean spiderSingle(int number, InfoBox infB){
		boolean ret=false;
		this.infB = infB;
		CacheHolder ch = (CacheHolder)cacheDB.get(number);
		if (ch.isAddiWpt()) return false;  // No point re-spidering an addi waypoint, comes with parent

		// check if we need to login
		if (!loggedIn){
			if (this.login()!=Form.IDOK) return false;
			// loggedIn is already set by this.login()
		}
		CacheHolderDetail chD=new CacheHolderDetail(ch);
		try{
			// Get all existing details of the cache
			try {
				chD.readCache(profile.dataDir);
			} catch (IOException ioex) {
				pref.log("No .XML file found for cache "+chD.wayPoint);
			};
			// Read the cache data from GC.COM and compare to old data
			ret=getCacheByWaypointName(chD,true,true,false,true);
			// Save the spidered data
			if (ret) {
				pref.log("Saving to:" + profile.dataDir);
				chD.saveCacheDetails(profile.dataDir);
				((CacheHolder) cacheDB.get(number)).update(chD);
			}
		}catch(Exception ex){
			pref.log("Error spidering " + chD.wayPoint + " in spiderSingle");
		}
		return ret;
	} // spiderSingle

	/**
	 * Fetch the coordinates of a waypoint from GC
	 * @param wayPoint the name of the waypoint
	 * @return the cache coordinates
	 */
	public String getCacheCoordinates(String wayPoint) {
		String completeWebPage;
		// Check whether spider definitions could be loaded, if not issue appropriate message and terminate
		// Try to login. If login fails, issue appropriate message and terminate
		if (!loggedIn || Global.getPref().forceLogin) {
			if (login()!=Form.IDOK) {
				return "";
			}
		}
		InfoBox infB = new InfoBox("Info", "Loading", InfoBox.PROGRESS_WITH_WARNINGS);
		infB.exec();
		try{
			String doc = p.getProp("waypoint") + wayPoint;
			pref.log("Fetching: " + wayPoint);
			completeWebPage = fetch(doc);
		}catch(Exception ex){
			infB.close(0);
			pref.log("Could not fetch " + wayPoint,ex);
			return "";
		}
		infB.close(0);
		try {
			return getLatLon(completeWebPage);
		} catch (Exception ex) {
			return "????";
		}
	}

	/**
	*	Method to start the spider for a search around the center coordinates
	*/
	public void doIt(){
		String postStr, dummy, ln, wpt;
		Regex lineRex;
		CacheHolderDetail chD;
		CWPoint origin = pref.curCentrePt; // No need to copy curCentrePt as it is only read and not written
		if (!origin.isValid()) {
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5509,"Coordinates for center must be set"), MessageBox.OKB)).execute();
			return;
		}
		// Prepare an index of caches for faster searching
		indexDB = new Hashtable(cacheDB.size());
		CacheHolder ch;
		//index the database for faster searching!
		for(int i = 0; i<cacheDB.size();i++){
			ch = (CacheHolder)cacheDB.get(i);
			indexDB.put((String)ch.wayPoint, new Integer(i));
			ch.is_new = false;
		}
		String start = "";
		Regex rex = new Regex("name=\"__VIEWSTATE\" value=\"(.*)\" />");
		String doc = "";

		if (!loggedIn || Global.getPref().forceLogin) {
			if(login() != Form.IDOK) return;
		}

		OCXMLImporterScreen options = new OCXMLImporterScreen(MyLocale.getMsg(5510,"Spider Options"),	OCXMLImporterScreen.INCLUDEFOUND | OCXMLImporterScreen.DIST| OCXMLImporterScreen.IMAGES);
		options.distanceInput.setText("");
		if (options.execute() == OCXMLImporterScreen.IDCANCEL) {return; }
		String dist = options.distanceInput.getText();
		if (dist.length()== 0) return;
		distance = Convert.toDouble(dist);
		boolean doNotgetFound = options.foundCheckBox.getState();
		boolean getImages = options.imagesCheckBox.getState();
		options.close(0);

		//=======
		// Prepare list of all caches that are to be spidered
		//=======
		Vm.showWait(true);
		infB = new InfoBox("Status", MyLocale.getMsg(5502,"Fetching first page..."));
		infB.exec();
		//Get first page
		try{
			ln = p.getProp("firstPage") + origin.getLatDeg(CWPoint.DD) + p.getProp("firstPage2") +origin.getLonDeg(CWPoint.DD);
			if(doNotgetFound) ln = ln + "&f=1";
			pref.log("Getting first page: "+ln);
			start = fetch(ln);
			pref.log("Got first page");
		}catch(Exception ex){
			pref.log("Error fetching first list page",ex,true);
			Vm.showWait(false);
			infB.close(0);
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5503,"Error fetching first list page."), MessageBox.OKB)).execute();
			return;
		}
		dummy = "";
		//String lineBlck = "";
		int page_number = 4;
		try  {
			lineRex = new Regex(p.getProp("lineRex")); //"<tr bgcolor=((?s).*?)</tr>"
		} catch (Exception ex) {
			infB.close(0);
			Vm.showWait(false);
			return;
		}
		int found_on_page = 0;
		try {
			//Loop till maximum distance has been found or no more caches are in the list
			while(distance > 0){
				if (infB.isClosed) break;
				rex.search(start);
				viewstate = rex.stringMatched(1);
				//Vm.debug("In loop");
				Regex listBlockRex = new Regex(p.getProp("listBlockRex")); // "<table id=\"dlResults\"((?s).*?)</table>"
				listBlockRex.search(start);
				dummy = listBlockRex.stringMatched(1);
				try{
					lineRex.search(dummy);
				}catch(NullPointerException nex){}
				while(lineRex.didMatch()){
					//Vm.debug(getDist(lineRex.stringMatched(1)) + " / " +getWP(lineRex.stringMatched(1)));
					found_on_page++;
					if(getDist(lineRex.stringMatched(1)) <= distance){
						if(indexDB.get((String)getWP(lineRex.stringMatched(1))) == null){
							cachesToLoad.add(getWP(lineRex.stringMatched(1)));
						} else pref.log(getWP(lineRex.stringMatched(1))+" already in DB");
					} else distance = 0;
					lineRex.searchFrom(dummy, lineRex.matchedTo());
				}
				infB.setInfo(MyLocale.getMsg(5511,"Found ") + cachesToLoad.size() + MyLocale.getMsg(5512," caches"));
				if(found_on_page < 20) distance = 0;
				postStr = p.getProp("firstLine") + origin.getLatDeg(CWPoint.DD) + "&" + origin.getLonDeg(CWPoint.DD);
				if(doNotgetFound) postStr = postStr + p.getProp("showOnlyFound");
				if(distance > 0){
					page_number++;
					if(page_number >= 15) page_number = 5;
					doc = URL.encodeURL("__VIEWSTATE",false) +"="+ URL.encodeURL(viewstate,false)
					//if(doNotgetFound) doc += "&f=1";
					    + "&" + URL.encodeURL("__EVENTTARGET",false) +"="+ URL.encodeURL("ResultsPager:_ctl"+page_number,false)
					    + "&" + URL.encodeURL("__EVENTARGUMENT",false) +"="+ URL.encodeURL("",false);
					try{
						start = "";
						pref.log("Fetching next list page:" + doc);
						start = fetch_post(postStr, doc, p.getProp("nextListPage"));
					}catch(Exception ex){
						//Vm.debug("Couldn't get the next page");
						pref.log("Error getting next page");
					}finally{
					}
				}
				//Vm.debug("Distance is now: " + distance);
				found_on_page = 0;
			}
		} catch (Exception ex) { // Some tag missing from spider.def
			infB.close(0);
			Vm.showWait(false);
			return;
		}
		pref.log("Found " + cachesToLoad.size() + " caches");
		if (!infB.isClosed) infB.setInfo(MyLocale.getMsg(5511,"Found ") + cachesToLoad.size() + MyLocale.getMsg(5512," caches"));

		//=======
		// Now ready to spider each cache in the list
		//=======
		for(int i = 0; i<cachesToLoad.size(); i++){
			if (infB.isClosed) break;

			wpt = (String)cachesToLoad.get(i);
			// Get only caches not already available in the DB
			if(searchWpt(wpt) == -1){
				infB.setInfo(MyLocale.getMsg(5513,"Loading: ") + wpt +" (" + (i+1) + " / " + cachesToLoad.size() + ")");
				chD = new CacheHolderDetail();
				chD.wayPoint=wpt;
				if (!getCacheByWaypointName(chD,false,getImages,doNotgetFound,true)) break;
				if (!chD.is_found || !doNotgetFound ) {
					chD.saveCacheDetails(profile.dataDir);
					cacheDB.add(new CacheHolder(chD)); // TODO Could copy into existing object
				}
			}
		}
		infB.close(0);
		Vm.showWait(false);
		Global.getProfile().saveIndex(Global.getPref(),true);
	}

	/**
	 * Read a complete cachepage from geocaching.com including all logs. This is used both when
	 * updating already existing caches (via spiderSingle) and when spidering around a centre. It
	 * is also used when reading a GPX file and fetching the images.
	 *
	 * This is the workhorse function of the spider.
	 *
	 * @param CacheHolderDetail chD The element wayPoint must be set to the name of a waypoint
	 * @param boolean isUpdate True if an existing cache is being updated, false if it is a new cache
	 * @param boolean fetchImages True if the pictures are to be fetched
	 * @param boolean doNotGetFound True if the cache is not to be spidered if it has already been found
	 * @param boolean fetchAllLogs True if all logs are to be fetched (by adding option '&logs=y' to command line).
	 *     This is normally false when spidering from GPXImport as the logs are part of the GPX file, and true otherwise
	 * @return false if the infoBox was closed
	 */
	private boolean getCacheByWaypointName(CacheHolderDetail chD, boolean isUpdate, boolean fetchImages, boolean doNotGetFound, boolean fetchAllLogs) {
		String completeWebPage;
		try{
			String doc = p.getProp("getPageByName") + chD.wayPoint +(fetchAllLogs?p.getProp("fetchAllLogs"):"");
			pref.log("Fetching: " + chD.wayPoint);
			completeWebPage = fetch(doc);
		}catch(Exception ex){
			pref.log("Could not fetch " + chD.wayPoint,ex);
			chD.is_incomplete = true;
			return !infB.isClosed; // Only return false (which terminates the loop over all caches) if infB is closed
		}
		// Only analyse the cache data and fetch pictures if user has not closed the progress window
		if (!infB.isClosed) {
			try{
				chD.is_new = !isUpdate;
				chD.is_update = false;
				chD.is_HTML = true;
				chD.is_available = true;
				chD.is_archived = false;
				chD.is_incomplete = false;
				// Save size of logs to be able to check whether any new logs were added
				int logsz = chD.CacheLogs.size();
				chD.CacheLogs.clear();
				chD.addiWpts.clear();
				chD.Images.clear();
				chD.ImagesText.clear();

				if(completeWebPage.indexOf(p.getProp("cacheUnavailable")) >= 0) chD.is_available = false;
				if(completeWebPage.indexOf(p.getProp("cacheArchived")) >= 0) chD.is_archived = true;
				//==========
				// General Cache Data
				//==========
				chD.setLatLon(getLatLon(completeWebPage));
				if (pref.debug) pref.log("LatLon: " + chD.LatLon);

				pref.log("Trying description");
				chD.setLongDescription(getLongDesc(completeWebPage));
				pref.log("Got description");

				pref.log("Getting cache name");
				chD.CacheName = SafeXML.cleanback(getName(completeWebPage));
				pref.log("Got cache name");
				if (pref.debug) pref.log("Name: " + chD.CacheName);

				pref.log("Trying owner");
				chD.CacheOwner = SafeXML.cleanback(getOwner(completeWebPage)).trim();
				if(chD.CacheOwner.equals(pref.myAlias) || (pref.myAlias2.length()>0 && chD.CacheOwner.equals(pref.myAlias2))) chD.is_owned = true;
				pref.log("Got owner");
				if (pref.debug) pref.log("Owner: " + chD.CacheOwner +"; is_owned = "+chD.is_owned+";  alias1,2 = ["+pref.myAlias+"|"+pref.myAlias2+"]");

				pref.log("Trying date hidden");
				chD.DateHidden = DateFormat.MDY2YMD(getDateHidden(completeWebPage));
				pref.log("Got date hidden");
				if (pref.debug) pref.log("Hidden: " + chD.DateHidden);

				pref.log("Trying hints");
				chD.setHints(getHints(completeWebPage));
				pref.log("Got hints");
				if (pref.debug) pref.log("Hints: " + chD.Hints);

				pref.log("Trying size");
				chD.CacheSize = getSize(completeWebPage);
				pref.log("Got size");
				if (pref.debug) pref.log("Size: " + chD.CacheSize);

				pref.log("Trying difficulty");
				chD.hard = getDiff(completeWebPage);
				pref.log("Got difficulty");
				if (pref.debug) pref.log("Hard: " + chD.hard);

				pref.log("Trying terrain");
				chD.terrain = getTerr(completeWebPage);
				pref.log("Got terrain");
				if (pref.debug) pref.log("Terr: " + chD.terrain);

				pref.log("Trying cache type");
				chD.type = getType(completeWebPage);
				pref.log("Got cache type");
				if (pref.debug) pref.log("Type: " + chD.type);

				//==========
				// Logs
				//==========
				pref.log("Trying logs");
				chD.setCacheLogs(getLogs(completeWebPage, chD));
				pref.log("Found logs");

				// If the switch is set to not store found caches and we found the cache => return
				if (chD.is_found && doNotGetFound) return !infB.isClosed;

				//==========
				// Bugs
				//==========
				// As there may be several bugs, we check whether the user has aborted
				if (!infB.isClosed) getBugs(chD,completeWebPage);
				chD.has_bug = chD.Travelbugs.size()>0;

				//==========
				// Images
				//==========
				if(fetchImages){
					pref.log("Trying images");
					getImages(completeWebPage, chD);
					pref.log("Got images");
				}
				//==========
				// Addi waypoints
				//==========

				pref.log("Getting additional waypoints");
				getAddWaypoints(completeWebPage, chD.wayPoint, chD.is_found);
				pref.log("Got additional waypoints");

				//==========
				// Attributes
				//==========
				pref.log("Getting attributes");
				getAttributes(completeWebPage, chD);
				pref.log("Got attributes");
				if (chD.is_new) chD.is_update=false;
			}catch(Exception ex){
				pref.log("Error reading cache: "+chD.wayPoint);
				pref.log("Exception in getCacheByWaypointName: ",ex);
			}
			finally{}
		}
		boolean ret=!infB.isClosed; // If the infoBox was closed before getting here, we return false
		return ret;
	} // getCacheByWaypointName

	/**
	 * Check whether a waypoint is in the database
	 * @param wpt Name of waypoint to check
	 * @return index of waypoint in database, -1 if it does not exist
	 */
	private int searchWpt(String wpt){
		Integer INTR = (Integer)indexDB.get(wpt);
		if(INTR != null){
			return INTR.intValue();
		} else return -1;
	}

	/**
	 * Get the Distance to the centre
	 * @param doc A previously fetched cachepage
	 * @return Distance
	 */
	private double getDist(String doc) throws Exception {
		inRex = new Regex(p.getProp("distRex"));
		inRex.search(doc);
		if(doc.indexOf("Here") >= 0) return(0);
		if (!inRex.didMatch()) return 0;
		if(pref.digSeparator.equals(",")) return Convert.toDouble(inRex.stringMatched(1).replace('.',','));
		return Convert.toDouble(inRex.stringMatched(1));
	}

	/**
	 * Get the waypoint name
	 * @param doc A previously fetched cachepage
	 * @return Name of waypoint to add to list
	 */
	private String getWP(String doc) throws Exception {
		inRex = new Regex(p.getProp("waypointRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "???";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the coordinates of the cache
	 * @param doc A previously fetched cachepage
	 * @return Cache coordinates
	 */
	private String getLatLon(String doc) throws Exception{
		inRex = new Regex(p.getProp("latLonRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "???";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the long description
	 * @param doc A previously fetched cachepage
	 * @return the long description
	 */
	private String getLongDesc(String doc) throws Exception{
		String res = "";
		inRex = new Regex(p.getProp("shortDescRex"));
		Regex rex2 = new Regex(p.getProp("longDescRex"));
		inRex.search(doc);
		rex2.search(doc);
		res = ((inRex.stringMatched(1)==null)?"":inRex.stringMatched(1)) + "<br>";
		res += rex2.stringMatched(1);
		return res; // SafeXML.cleanback(res);
	}

	/**
	 * Get the cache name
	 * @param doc A previously fetched cachepage
	 * @return the name of the cache
	 */
	private String getName(String doc) throws Exception{
		inRex = new Regex(p.getProp("cacheNameRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "???";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the cache owner
	 * @param doc A previously fetched cachepage
	 * @return the cache owner
	 */
	private String getOwner(String doc) throws Exception{
		inRex = new Regex(p.getProp("cacheOwnerRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "???";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the date when the cache was hidden
	 * @param doc A previously fetched cachepage
	 * @return Hidden date
	 */
	private String getDateHidden(String doc) throws Exception{
		inRex = new Regex(p.getProp("dateHiddenRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "???";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the hints
	 * @param doc A previously fetched cachepage
	 * @return Cachehints
	 */
	private String getHints(String doc) throws Exception{
		inRex = new Regex(p.getProp("hintsRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) return "";
		return inRex.stringMatched(1);
	}

	/**
	 * Get the cache size
	 * @param doc A previously fetched cachepage
	 * @return Cache size
	 */
	private String getSize(String doc) throws Exception{
		inRex = new Regex(p.getProp("sizeRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else return "None";
	}

	/**
	 * Get the Difficulty
	 * @param doc A previously fetched cachepage
	 * @return The cache difficulty
	 */
	private String getDiff(String doc) throws Exception{
		inRex = new Regex(p.getProp("difficultyRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else return "";
	}

	/**
	 * Get the terrain rating
	 * @param doc A previously fetched cachepage
	 * @return Terrain rating
	 */
	private String getTerr(String doc) throws Exception{
		inRex = new Regex(p.getProp("terrainRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else return "";
	}

	/**
	 * Get the waypoint type
	 * @param doc A previously fetched cachepage
	 * @return the waypoint type (Tradi, Multi, etc.)
	 */
	private String getType(String doc) throws Exception{
		inRex = new Regex(p.getProp("cacheTypeRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else return "";
	}

	/**
	 * Get the logs
	 * @param doc A previously fetched cachepage
	 * @param chD Cache Details
	 * @return A HTML string containing the logs
	 */
	private Vector getLogs(String doc, CacheHolderDetail chD) throws Exception{
		String icon = "";
		String name = "";
		Vector reslts = new Vector();
		Regex blockRex = new Regex(p.getProp("blockRex"));
		blockRex.search(doc);
		doc = blockRex.stringMatched(1);
		//Vm.debug("Log Block: " + doc);
		/*
		Vm.debug("Setting log regex");
		inRex = new Regex("<STRONG><IMG SRC='http://www.geocaching.com/images/icons/((?s).*?)'((?s).*?)&nbsp;((?s).*?)<A NAME=\"((?s).*?)'text-decoration: underline;'>((?s).*?)<A HREF=\"((?s).*?)'text-decoration: underline;'>((?s).*?)</A></strong>((?s).*?)\\[<A href=");
		inRex.optimize();
		inRex.search(doc);
		Vm.debug("Log regex run...");
		while(inRex.didMatch()){
			Vm.debug("Logs:" + inRex.stringMatched(1) + " / " + inRex.stringMatched(3)+ " / " + inRex.stringMatched(7)+ " / " + inRex.stringMatched(8));
			//<img src='icon_smile.gif'>&nbsp;
			reslts.add("<img src='"+ inRex.stringMatched(1) +"'>&nbsp;" + inRex.stringMatched(3)+ inRex.stringMatched(7)+ inRex.stringMatched(8));
			inRex.searchFrom(doc, inRex.matchedTo());
		}
		*/
		String singleLog = "";
		Extractor exSingleLog = new Extractor(doc,p.getProp("singleLogExStart"), p.getProp("singleLogExEnd"), 0, false); // maybe here is some change neccessary because findnext now gives the whole endstring back???
		singleLog = exSingleLog.findNext();
		Extractor exIcon = new Extractor(singleLog,p.getProp("iconExStart"), p.getProp("iconExEnd"), 0, true);
		Extractor exNameTemp = new Extractor(singleLog,p.getProp("nameTempExStart"), p.getProp("nameTempExEnd"), 0 , true);
		String nameTemp = "";
		nameTemp = exNameTemp.findNext();
		Extractor exName = new Extractor(nameTemp, p.getProp("nameExStart"), p.getProp("nameExEnd"), 0 , true);
		Extractor exDate = new Extractor(singleLog,p.getProp("dateExStart"), p.getProp("dateExEnd"), 0 , true);
		Extractor exLog = new Extractor(singleLog, p.getProp("logExStart"), p.getProp("logExEnd"), 0, true);
		//Vm.debug("Log Block: " + singleLog);
		int nLogs=0;
		while(exSingleLog.endOfSearch() == false){
			nLogs++;
			//Vm.debug("--------------------------------------------");
			//Vm.debug("Log Block: " + singleLog);
			//Vm.debug("Icon: "+exIcon.findNext());
			//Vm.debug(exName.findNext());
			//Vm.debug(exDate.findNext());
			//Vm.debug(exLog.findNext());
			//Vm.debug("--------------------------------------------");
			icon = exIcon.findNext();
			name = exName.findNext();
			String d=DateFormat.logdate2YMD(exDate.findNext());
			if((icon.equals(p.getProp("icon_smile")) || icon.equals(p.getProp("icon_camera"))) &&
				(name.equals(pref.myAlias) || (pref.myAlias2.length()>0 && name.equals(pref.myAlias2))) )  {
				chD.is_found = true;
				chD.CacheStatus = d;
			}
			if (nLogs<=MAXLOGS) reslts.add("<img src='"+ icon +"'>&nbsp;" + d + " " + name +"<br>"+ exLog.findNext());

			singleLog = exSingleLog.findNext();
			exIcon.setSource(singleLog);
			exNameTemp.setSource(singleLog);
			nameTemp = exNameTemp.findNext();
			exName.setSource(nameTemp);
			exDate.setSource(singleLog);
			exLog.setSource(singleLog);
			// We cannot simply stop if we have reached MAXLOGS just in case we are waiting for
			// a log by our alias that happened earlier.
			if (nLogs>=MAXLOGS && chD.is_found) break;
		}
		if (nLogs>MAXLOGS) {
			reslts.add("<br>More than "+MAXLOGS+" logs.<br>");
		}
		return reslts;
	}

	/**
	 * Read the travelbug names from a previously fetched Cache page and then
	 * read the travelbug purpose for each travelbug
	 * @param doc The previously fetched cachepage
	 * @return A HTML formatted string with bug names and there purpose
	 */
	public void getBugs(CacheHolderDetail chD, String doc) throws Exception{
		Extractor exBlock = new Extractor(doc,p.getProp("blockExStart"),p.getProp("blockExEnd") ,0,Extractor.EXCLUDESTARTEND);
		String bugBlock = exBlock.findNext();
		//Vm.debug("Bugblock: "+bugBlock);
		Extractor exBug = new Extractor(bugBlock,p.getProp("bugExStart"),p.getProp("bugExEnd"),0,Extractor.EXCLUDESTARTEND);
		String link,bug,linkPlusBug,bugDetails;
		String oldInfoBox=infB.getInfo();
		chD.Travelbugs.clear();
		while(exBug.endOfSearch() == false){
			if (infB.isClosed) break; // Allow user to cancel by closing progress form
			linkPlusBug= exBug.findNext();
			int idx=linkPlusBug.indexOf("'>");
			if (idx<0) break; // No link/bug pair found
			link=linkPlusBug.substring(0,idx);
			bug=linkPlusBug.substring(idx+2);
			if(bug.length()>0) { // Found a bug, get its details
				Travelbug tb=new Travelbug(bug);
				try{
					infB.setInfo(oldInfoBox+MyLocale.getMsg(5514,"\nGetting bug: ")+SafeXML.cleanback(bug));
					pref.log("Fetching bug details: "+bug);
					bugDetails = fetch(link);
					Extractor exDetails = new Extractor(bugDetails,p.getProp("bugDetailsStart"),p.getProp("bugDetailsEnd"),0,Extractor.EXCLUDESTARTEND);
					tb.setMission(exDetails.findNext());
					Extractor exGuid = new Extractor(bugDetails,"details.aspx?guid=","\" id=\"Form1",0,Extractor.EXCLUDESTARTEND); // TODO Replace with spider.def see also further down
					tb.setGuid(exGuid.findNext());
					chD.Travelbugs.add(tb);
				}catch(Exception ex){
					pref.log("Could not fetch bug details");
				}
			}
			//Vm.debug("B: " + bug);
			//Vm.debug("End? " + exBug.endOfSearch());
		}
		infB.setInfo(oldInfoBox);
	}

	/**
	 * Get the images for a previously fetched cache page. Images are extracted
	 * from two areas: The long description and the pictures section (including
	 * the spoiler)
	 * @param doc The previously fetched cachepage
	 * @param chD The Cachedetails
	 */
	public void getImages(String doc, CacheHolderDetail chD){
		int imgCounter = 0;
		String imgName, oldImgName, imgType, imgUrl;
		Vector spideredUrls=new Vector(15);
		Extractor exImgBlock;
		int idxUrl; // Index of already spidered Url in list of spideredUrls
		//========
		//In the long description
		//========
		String longDesc = "";
		try {
			if (chD.wayPoint.startsWith("TC")) longDesc = doc;
			else
				longDesc = getLongDesc(doc);
			longDesc = STRreplace.replace(longDesc, "<img", "<IMG");
			longDesc = STRreplace.replace(longDesc, "src=", "SRC=");
			longDesc = STRreplace.replace(longDesc, "'", "\"");
			exImgBlock = new Extractor(longDesc,p.getProp("imgBlockExStart"),p.getProp("imgBlockExEnd"), 0, false);
		} catch (Exception ex) {//Missing property in spider.def
			return;
		}
		//Vm.debug("In getImages: Have longDesc" + longDesc);
		String tst;
		tst = exImgBlock.findNext();
		//Vm.debug("Test: \n" + tst);
		Extractor exImgSrc = new Extractor(tst, "http://", "\"", 0, true);
		while(exImgBlock.endOfSearch() == false){
			imgUrl = exImgSrc.findNext();
			//Vm.debug("Img Url: " +imgUrl);
			if(imgUrl.length()>0){
				imgUrl = "http://" + imgUrl;
				try{
					imgType = (imgUrl.substring(imgUrl.lastIndexOf(".")).toLowerCase()+"    ").substring(0,4).trim();
					// imgType is now max 4 chars, starting with .
					if(!imgType.startsWith(".com") && !imgType.startsWith(".php") && !imgType.startsWith(".exe")){
						// Check whether image was already spidered for this cache
						idxUrl=spideredUrls.find(imgUrl);
						imgName = chD.wayPoint + "_" + Convert.toString(imgCounter);
						if (idxUrl<0) { // New image
							pref.log("Loading image: " + imgUrl+" as "+imgName);
							spiderImage(imgUrl, imgName+imgType);
							chD.Images.add(imgName+imgType);
							spideredUrls.add(imgUrl);
						} else { // Image already spidered as wayPoint_'idxUrl'
							pref.log("Already loaded image: " + imgUrl);
							oldImgName = chD.wayPoint + "_" + Convert.toString(idxUrl);
							chD.Images.add(oldImgName+imgType); // Store name of old image as image to load
						}
						chD.ImagesText.add(imgName); // Keep the image name
						imgCounter++;
					}
				} catch (IndexOutOfBoundsException e) {
					//Vm.debug("IndexOutOfBoundsException not in image span"+e.toString()+"imgURL:"+imgUrl);
					pref.log("Problem loading image. imgURL:"+imgUrl);
				}
				}
			exImgSrc.setSource(exImgBlock.findNext());
		}
		//========
		//In the image span
		//========
		Extractor spanBlock,exImgName;
		try {
			spanBlock = new Extractor(doc,p.getProp("imgSpanExStart"),p.getProp("imgSpanExEnd"), 0 , true);
			tst = spanBlock.findNext();
			exImgName = new Extractor(tst,p.getProp("imgNameExStart"),p.getProp("imgNameExEnd"), 0 , true);
			exImgSrc = new Extractor(tst,p.getProp("imgSrcExStart"),p.getProp("imgSrcExEnd"), 0, true);
		} catch (Exception ex) { // Missing property in spider .def
			return;
		}
		while(exImgSrc.endOfSearch() == false){
			imgUrl = exImgSrc.findNext();
			//Vm.debug("Img Url: " +imgUrl);
			if(imgUrl.length()>0){
				imgUrl = "http://" + imgUrl;
				try{
					imgType = (imgUrl.substring(imgUrl.lastIndexOf(".")).toLowerCase()+"    ").substring(0,4).trim();
					// imgType is now max 4 chars, starting with .
					if(!imgType.startsWith(".com") && !imgType.startsWith(".php") && !imgType.startsWith(".exe")){
						// Check whether image was already spidered for this cache
						idxUrl=spideredUrls.find(imgUrl);
						imgName = chD.wayPoint + "_" + Convert.toString(imgCounter);
						if (idxUrl<0) { // New image
							pref.log("Loading image: " + imgUrl);
							spiderImage(imgUrl, imgName+imgType);
							chD.Images.add(imgName+imgType);
						} else { // Image already spidered as wayPoint_ 'idxUrl'
							pref.log("Already loaded image: " + imgUrl);
							oldImgName = chD.wayPoint + "_" + Convert.toString(idxUrl);
							chD.Images.add(oldImgName+imgType); // Store name of old image as image to load
						}
						chD.ImagesText.add(exImgName.findNext()); // Keep the image description
						imgCounter++;
					}
				} catch (IndexOutOfBoundsException e) {
					pref.log("IndexOutOfBoundsException in image span. imgURL:"+imgUrl,e);
				}
			}
		}
	}

	/**
	 * Read an image from the server
	 * @param imgUrl The Url of the image
	 * @param target The bytes of the image
	 */
	private void spiderImage(String imgUrl, String target){
		HttpConnection connImg;
		Socket sockImg;
		//InputStream is;
		FileOutputStream fos;
		//int bytes_read;
		//byte[] buffer = new byte[9000];
		ByteArray daten;
		String datei = "";
		datei = profile.dataDir + target;
		if(pref.myproxy.length()>0){
			connImg = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), imgUrl);
		}else{
			connImg = new HttpConnection(imgUrl);
		}
		connImg.setRequestorProperty("Connection", "close");
		try{
			pref.log("Trying to fetch image from: " + imgUrl);
			sockImg = connImg.connect();
			daten = connImg.readData(connImg.connect());
			fos = new FileOutputStream(new File(datei));
			fos.write(daten.toBytes());
			fos.close();
			sockImg.close();
		} catch (UnknownHostException e) {
			pref.log("Host not there...");
		}catch(IOException ioex){
			pref.log("File not found!");
		} catch (Exception ex){
			pref.log("Some other problem while fetching image",ex);
		} finally {
			//Continue with the spider
		}
	}

	/**
	 * Read all additional waypoints from a previously fetched cachepage.
	 * @param doc The previously fetched cachepage
	 * @param wayPoint The name of the cache
	 * @param is_found Found status of the cached (is inherited by the additional waypoints)
	 */
	public void getAddWaypoints(String doc, String wayPoint, boolean is_found) throws Exception{
		Extractor exWayBlock = new Extractor(doc,p.getProp("wayBlockExStart"),p.getProp("wayBlockExEnd"), 0, false);
		String wayBlock = "";
		String rowBlock = "";
		wayBlock = exWayBlock.findNext();
		Regex nameRex = new Regex(p.getProp("nameRex"));
		Regex koordRex = new Regex(p.getProp("koordRex"));
		Regex descRex = new Regex(p.getProp("descRex"));
		Regex typeRex = new Regex(p.getProp("typeRex"));
		int counter = 0;
		if(exWayBlock.endOfSearch() == false && wayBlock.indexOf("No additional waypoints to display.")<0){
			Extractor exRowBlock = new Extractor(wayBlock,p.getProp("rowBlockExStart"),p.getProp("rowBlockExEnd"), 0, false);
			rowBlock = exRowBlock.findNext();
			rowBlock = exRowBlock.findNext();
			while(exRowBlock.endOfSearch()==false){
				CacheHolderDetail cxD = new CacheHolderDetail();
				cxD.wayPoint = MyLocale.formatLong(counter, "00") + wayPoint.substring(2);
				counter++;
				try{ // If addi exists, try to read it to preserve the notes
					cxD.readCache(profile.dataDir);
				} catch (Exception ex) {};
				nameRex.search(rowBlock);
				koordRex.search(rowBlock);
				typeRex.search(rowBlock);
				cxD.CacheName = nameRex.stringMatched(1);
				if(koordRex.didMatch()) cxD.setLatLon(koordRex.stringMatched(1));
				if(typeRex.didMatch()) cxD.type = CacheType.typeText2Number("Waypoint|"+typeRex.stringMatched(1));
				rowBlock = exRowBlock.findNext();
				descRex.search(rowBlock);
				cxD.setLongDescription(descRex.stringMatched(1));
				cxD.is_found = is_found;
				cxD.saveCacheDetails(profile.dataDir);

				int idx=profile.getCacheIndex(cxD.wayPoint);
				if (idx<0){
					cacheDB.add(new CacheHolder(cxD));
				}else {
					CacheHolder cx=(CacheHolder) cacheDB.get(idx);
					if (cx.is_Checked && // Only re-spider existing addi waypoints that are ticked
				 	   !cx.is_filtered) // and are visible (i.e.  not filtered)
					   cx.update(cxD);
				}
				rowBlock = exRowBlock.findNext();
			}
		}
	}

	private void getAttributes(String doc, CacheHolderDetail chD) throws Exception {
		Extractor attBlock = new Extractor(doc,p.getProp("attBlockExStart"),p.getProp("attBlockExEnd"), 0 , true);
		String atts = attBlock.findNext();
		Extractor attEx = new Extractor(atts,p.getProp("attExStart"),p.getProp("attExEnd"), 0 , true);
		String attribute=attEx.findNext();
		chD.attributes.clear();
		while (attEx.endOfSearch()==false) {
			chD.attributes.add(attribute);
			attribute=attEx.findNext();
		}
	}


	/**
	*	Performs an initial fetch to a given address. In this case
	*	it will be a gc.com address. This method is used to obtain
	*	the result of a search for caches screen.
	*/
	public static String fetch(String address)
	   	{
			CharArray c_data;
			String data="";
			try{
				//Vm.debug(address);
				HttpConnection conn;
				if(pref.myproxy.length() > 0){
					pref.log("Using proxy: " + pref.myproxy + " / " +pref.myproxyport);
					conn = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), address);
					//Vm.debug(address);
				} else {
					conn = new HttpConnection(address);
				}
				conn.setRequestorProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
				if(cookieSession.length()>0){
					conn.setRequestorProperty("Cookie", "ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
					pref.log("Cookie Zeug: " + "Cookie: ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
				} else
					pref.log("No Cookie found");
				conn.setRequestorProperty("Connection", "close");
				conn.documentIsEncoded = true;
				pref.log("Connecting");
				Socket sock = conn.connect();
				pref.log("Connect ok!");
				ByteArray daten = conn.readData(sock);
				pref.log("Read socket ok");
				JavaUtf8Codec codec = new JavaUtf8Codec();
				c_data = codec.decodeText(daten.data, 0, daten.length, true, null);

				/*
				 * prepend the response headers to the document
				 * like non-linux Ewe does
				 *
				 * @TODO directly use the properties in the
				 * code which calls this method instead
				 */
				PropertyList pl = conn.documentProperties;
				if (pl != null) {
					StringBuffer sb = new StringBuffer();
					boolean gotany = false;

					for (int i = 0; i < pl.size(); i++) {
						Property p = (Property)pl.get(i);
						if (p.value != null) {
							sb.append(p.name + ": " + p.value + "\r\n");
							gotany = true;
						}
					}
					if (gotany)
						data += sb.toString() + "\r\n";
				}
				data += c_data.toString();
				//Vm.debug("SpiderGC.fetch() result = " + data);
				sock.close();
			}catch(IOException ioex){
				pref.log("IOException in fetch", ioex);
			}finally{
				//continue
			}
			return data;
		}

	/**
	*	After a fetch to gc.com the next fetches have to use the post method.
	*	This method does exactly that. Actually this method is generic in the sense
	*	that it can be used to post to a URL using http post.
	*/
	private static String fetch_post(String address, String document, String path) throws IOException
	   	{

			//String line = "";
			String totline = "";
			if(pref.myproxy.length()==0){
				try {
					/*
					// Create a socket to the host
					String hostname = "www.geocaching.com";
					int port = 80;
					InetAddress addr = InetAddress.getByName(hostname);
					Socket socket = new Socket(hostname, port);
					// Send header
					//String path = "/seek/nearest.aspx";
					BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
					BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					wr.write("POST "+path+" HTTP/1.1\r\n");
					wr.write("Host: www.geocaching.com\r\n");
					if(cookieSession.length()>0){
						wr.write("Cookie: ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
					}
					Vm.debug("Doc length: " + document.length());
					wr.write("Content-Length: "+document.length()+"\r\n");
					wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
					wr.write("Connection: close\r\n");
					wr.write("\r\n");
					// Send data
					wr.write(document);
					wr.write("\r\n");
					wr.flush();
					//Vm.debug("Sent the data!");
					// Get response
					while ((line = rd.readLine()) != null) {
						totline += line + "\n";
					}
					wr.close();
					rd.close();
					*/
					HttpConnection conn;
					conn = new HttpConnection(address);
					JavaUtf8Codec codec = new JavaUtf8Codec();
					conn.documentIsEncoded = true;
					//Vm.debug(address + " / " + document);
					//document = document + "\r\n";
					//conn.setPostData(document.toCharArray());
					conn.setRequestorProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
					conn.setPostData(codec.encodeText(document.toCharArray(),0,document.length(),true,null));
					conn.setRequestorProperty("Content-Type", "application/x-www-form-urlencoded");
					if(cookieSession.length()>0){
						if (cookieSession!=null) conn.setRequestorProperty("Cookie", "ASP.NET_SessionId="+cookieSession+"; userid="+cookieID);
					}
					conn.setRequestorProperty("Connection", "close");
					Socket sock = conn.connect();

					//Vm.debug("getting stuff!");
					ByteArray daten = conn.readData(sock);
					//Vm.debug("coming back!");
					CharArray c_data = codec.decodeText(daten.data, 0, daten.length, true, null);
					sock.close();
					//Vm.debug(c_data.toString());
					totline = "";

					/*
					 * prepend the response headers to the document
					 * like non-linux Ewe does
					 *
					 * @TODO directly use the properties in the
					 * code which calls this method instead
					 */
					PropertyList pl = conn.documentProperties;
					if (pl != null) {
						StringBuffer sb = new StringBuffer();
						boolean gotany = false;

						for (int i = 0; i < pl.size(); i++) {
							Property p = (Property)pl.get(i);
							if (p.value != null) {
								sb.append(p.name + ": " + p.value + "\r\n");
								gotany = true;
							}
						}
						if (gotany)
							totline += sb.toString() + "\r\n";
					}
					totline += c_data.toString();
				} catch (Exception e) {
				}
			} else {
				HttpConnection conn;
				conn = new HttpConnection(pref.myproxy, Convert.parseInt(pref.myproxyport), address);
				JavaUtf8Codec codec = new JavaUtf8Codec();
				conn.documentIsEncoded = true;
				//Vm.debug(address + " / " + document);
				//document = document + "\r\n";
				//conn.setPostData(document.toCharArray());
				conn.setRequestorProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
				conn.setPostData(codec.encodeText(document.toCharArray(),0,document.length(),true,null));
				conn.setRequestorProperty("Content-Type", "application/x-www-form-urlencoded");
				if(cookieSession.length()>0){
					conn.setRequestorProperty("Cookie", "ASP.NET_SessionId="+cookieSession+"; userid="+cookieID);
				}
				conn.setRequestorProperty("Connection", "close");
				Socket sock = conn.connect();

				//Vm.debug("getting stuff!");
				ByteArray daten = conn.readData(sock);
				//Vm.debug("coming back!");
				CharArray c_data = codec.decodeText(daten.data, 0, daten.length, true, null);
				sock.close();
				//Vm.debug(c_data.toString());
				totline = "";

				/*
				 * prepend the response headers to the document
				 * like non-linux Ewe does
				 *
				 * @TODO directly use the properties in the
				 * code which calls this method instead
				 */
				PropertyList pl = conn.documentProperties;
				if (pl != null) {
					StringBuffer sb = new StringBuffer();
					boolean gotany = false;

					for (int i = 0; i < pl.size(); i++) {
						Property p = (Property)pl.get(i);
						if (p.value != null) {
							sb.append(p.name + ": " + p.value + "\r\n");
							gotany = true;
						}
					}
					if (gotany)
						totline += sb.toString() + "\r\n";
				}
				totline += c_data.toString();
			}
			return totline;
		}

	final static String hex = ewe.util.TextEncoder.hex;

	public String encodeUTF8(String username) {
		char [] what = ewe.sys.Vm.getStringChars(username);
		int max = what.length;
		char [] dest = new char[6*max]; // Assume each char is a UTF char and encoded into 6 chars
		char d = 0;
		for (int i = 0; i<max; i++){
			char c = what[i];
			if (c == ' ') c = '+';
			else if (c<='\u00FF') {
				if(c <= ' ' || c == '+' || c == '&' || c == '%' || c == '=' ||
				   c == '|' || c == '{' || c == '}' || c>'\u007F'){
					dest[d++] = '%';
					dest[d++] = hex.charAt((c >> 4) & 0xf);
					dest[d++] = hex.charAt(c & 0xf);
					continue;
				}

			} else {
				dest[d++] = '%';
				dest[d++] = hex.charAt((c >> 12) & 0xf);
				dest[d++] = hex.charAt((c >> 8) & 0xf);
				dest[d++] = '%';
				dest[d++] = hex.charAt((c >> 4) & 0xf);
				dest[d++] = hex.charAt(c & 0xf);
				continue;
			}
			dest[d++] = c;
		}
		return new String(dest,0,d);
	}

	/**
	 * Load the bug id for a given name. This method is not ideal, as there are
	 * sometimes several bugs with identical names but different IDs. Normally
	 * the bug GUID is used which can be obtained from the cache page.<br>
	 * Note that each bug has both an ID and a GUID.
	 * @param name The name (or partial name) of a travelbug
	 * @return the id of the bug
	 */
	public String getBugId (String name) {
		String bugList;
		try{
			//infB.setInfo(oldInfoBox+"\nGetting bug: "+bug);
			pref.log("Fetching bugId: "+name);
			bugList = fetch(p.getProp("getBugByName")+STRreplace.replace(SafeXML.clean(name)," ","+"));
		}catch(Exception ex){
			pref.log("Could not fetch bug list");
			bugList="";
		}
		try {
			if (bugList.equals("") || bugList.indexOf(p.getProp("bugNotFound"))>=0) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6020,"Travelbug not found."), MessageBox.OKB)).execute();
				return "";
			}
			if (bugList.indexOf(p.getProp("bugTotalRecords"))<0) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6021,"More than one travelbug found. Specify name more precisely."), MessageBox.OKB)).execute();
				return "";
			}
			Extractor exGuid = new Extractor(bugList,p.getProp("bugGuidExStart"),p.getProp("bugGuidExEnd"),0,Extractor.EXCLUDESTARTEND); // TODO Replace with spider.def
			return exGuid.findNext();
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Fetch a bug's mission for a given GUID or ID. If the guid String is longer
	 * than 10 characters it is assumed to be a GUID, otherwise it is an ID.
	 * @param guid the guid or id of the travelbug
	 * @return The mission
	 */
	public String getBugMissionByGuid(String guid) {
		String bugDetails;
		try{
			//infB.setInfo(oldInfoBox+"\nGetting bug: "+bug);
			pref.log("Fetching bug detailsById: "+guid);
			if (guid.length()>10)
				bugDetails = fetch(p.getProp("getBugByGuid")+guid);
			else
				bugDetails = fetch(p.getProp("getBugById")+guid);
		}catch(Exception ex){
			pref.log("Could not fetch bug details");
			bugDetails="";
		}
		try {
			if (bugDetails.indexOf(p.getProp("bugNotFound"))>=0) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6020,"Travelbug not found."), MessageBox.OKB)).execute();
				return "";
			}
			Extractor exDetails = new Extractor(bugDetails,p.getProp("bugDetailsStart"),p.getProp("bugDetailsEnd"),0,Extractor.EXCLUDESTARTEND);
			return exDetails.findNext();
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Fetch a bug's mission for a given tracking number
	 * @param trackNr the tracking number of the travelbug
	 * @return The mission
	 */
	public String getBugMissionByTrackNr(String trackNr) {
		String bugDetails;
		try{
			pref.log("Fetching bug detailsByTrackNr: "+trackNr);
			bugDetails = fetch(p.getProp("getBugByTrackNr")+trackNr);
		}catch(Exception ex){
			pref.log("Could not fetch bug details");
			bugDetails="";
		}
		try {
			if (bugDetails.indexOf(p.getProp("bugNotFound"))>=0) {
//				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6020,"Travelbug not found."), MessageBox.OKB)).execute();
				return "";
			}
			Extractor exDetails = new Extractor(bugDetails,p.getProp("bugDetailsStart"),p.getProp("bugDetailsEnd"),0,Extractor.EXCLUDESTARTEND);
			return exDetails.findNext();
		} catch (Exception ex) {
			return "";
		}
	}

	private class myProperties extends Properties {
		myProperties() {
			super();
			try {
				load(new FileInputStream(File.getProgramDirectory()+"/spider.def"));
			} catch (Exception ex) {
				pref.log("Failed to load spider.def",ex);
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5504,"Could not load 'spider.def'"), MessageBox.OKB)).execute();
			}
		}
		public String getProp(String key) throws Exception {
			String s=super.getProperty(key);
			if (s==null) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5497,"Error missing tag in spider.def") + ": "+key, MessageBox.OKB)).execute();
				throw new Exception("Missing tag in spider.def: "+key);
			}
			return s;
		}

	}
}
