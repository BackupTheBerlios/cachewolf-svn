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

package CacheWolf.imp;
import CacheWolf.CWPoint;
import CacheWolf.CacheDB;
import CacheWolf.CacheHolder;
import CacheWolf.CacheHolderDetail;
import CacheWolf.CacheImages;
import CacheWolf.CacheSize;
import CacheWolf.CacheTerrDiff;
import CacheWolf.CacheType;
import CacheWolf.Common;
import CacheWolf.DateFormat;
import CacheWolf.Extractor;
import CacheWolf.Global;
import CacheWolf.HttpConnection;
import CacheWolf.ImageInfo;
import CacheWolf.InfoBox;
import CacheWolf.Log;
import CacheWolf.LogList;
import CacheWolf.MyLocale;
import CacheWolf.Preferences;
import CacheWolf.Profile;
import CacheWolf.STRreplace;
import CacheWolf.SafeXML;
import CacheWolf.Travelbug;
import CacheWolf.navi.Metrics;
import CacheWolf.navi.TransformCoordinates;

import com.stevesoft.ewe_pat.Regex;

import ewe.data.Property;
import ewe.data.PropertyList;
import ewe.io.File;
import ewe.io.FileBase;
import ewe.io.FileInputStream;
import ewe.io.FileOutputStream;
import ewe.io.IOException;
import ewe.io.JavaUtf8Codec;
import ewe.net.Socket;
import ewe.net.URL;
import ewe.net.UnknownHostException;
import ewe.sys.Convert;
import ewe.sys.Double;
import ewe.sys.Time;
import ewe.sys.Vm;
import ewe.ui.FormBase;
import ewe.ui.MessageBox;
import ewe.util.ByteArray;
import ewe.util.CharArray;
import ewe.util.Enumeration;
import ewe.util.Hashtable;
import ewe.util.Properties;
import ewe.util.Utils;
import ewe.util.Vector;
import ewe.util.mString;

/**
*	Class to spider caches from gc.com
*/
public class SpiderGC{

	/**
	 * The maximum number of logs that will be stored
	 */
	public static String passwort = ""; // Can be pre-set from preferences
	public static boolean loggedIn = false;

	// Return values for spider action
	/** Ignoring a premium member cache when spidering from a non premium account */
	public static int SPIDER_IGNORE_PREMIUM = -2;
	/** Canceling spider process */
	public static int SPIDER_CANCEL = -1;
	/** Error occured while spidering */
	public static int SPIDER_ERROR = 0;
	/** Cache was spidered without problems */
	public static int SPIDER_OK = 1;
	/** no probs, but exmpl found und not want this*/
	public static int SPIDER_IGNORE = 2;

	private static int ERR_LOGIN = -10;
	private static Preferences pref;
	private Profile profile;

	private static String cookieID = "";
	private static String cookieSession = "";
	private static double minDistance = 0;
	private static double maxDistance = 0;
	private static String direction = "";
	private static String[] directions;
	private CacheDB cacheDB;
	private Vector cachesToLoad = new Vector();
	private InfoBox infB;
	private static SpiderProperties p=null;
	// following filled at doit
	private CWPoint origin;
	private double saveDistanceInMiles;
	private boolean doNotgetFound;
	private String cacheTypeRestriction;
	private boolean spiderAllFinds;
	private int page_number;
	private int found_on_page;
	private String htmlListPage;
	private int maxNew;
	private int maxUpdate;
	private boolean maxNumberAbort;

	private static String propFirstPage;
	private static String propFirstPage2;
	private static String propFirstPageFinds;
	private static String propFirstLine;
	private static String propFirstLine2;
	private static String propMaxDistance;
	private static String propShowOnlyFound;
	private static Regex RexPropListBlock;
	private static Regex RexPropLine;
	private static Regex RexNumFinds;
	private static Regex RexPropLogDate;
	private static String propAvailable;
	private static String propArchived;
	private static String propFound;
	private static String propPM;
	private static Regex RexPropDirection;
	private static Regex RexPropDistance;
	private static Regex RexPropWaypoint;
	private static Regex RexPropType;
	private static Regex RexPropSize;
	private static Regex RexPropDandT;
	private static Regex RexPropOwn;	
	private static Regex RexLogBlock;
	private static Extractor exSingleLog;
	private static Extractor exIcon;
	private static Extractor exNameTemp;
	private static Extractor exName;
	private static Extractor exDate;
	private static Extractor exLog;
	private static Extractor exLogId;
	private static String icon_smile;
	private static String icon_camera;
	private static String icon_attended;
	private static Regex RexCacheType;
	
	private int numFoundUpdates=0;
	private int numArchivedUpdates=0;
	private int numAvailableUpdates=0;
	private int numLogUpdates=0;
	private int numPrivate=0;

	public SpiderGC(Preferences prf, Profile profile, boolean bypass){
		this.profile=profile;
		this.cacheDB = profile.cacheDB;
		pref = prf;
		if (p == null) {
			pref.logInit();
			p=new SpiderProperties();
		}
		initialiseProperties();
	}

	/**
	*	Method to start the spider for a search around the centre coordinates
	*/
	public void doIt(){
		doIt(false);
	}
	public void doIt(boolean _spiderAllFinds){
		spiderAllFinds=_spiderAllFinds;
		origin = pref.getCurCentrePt(); // No need to copy curCentrePt as it is only read and not written
		if ( !spiderAllFinds && !origin.isValid()) {
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5509,"Coordinates for centre must be set"), FormBase.OKB)).execute();
			return;
		}
		if (System.getProperty("os.name")!=null)pref.log("Operating system: "+System.getProperty("os.name")+"/"+System.getProperty("os.arch"));
		if (System.getProperty("java.vendor")!=null)pref.log("Java: "+System.getProperty("java.vendor")+"/"+System.getProperty("java.version"));
		CacheHolder ch;
		// Reset states for all caches when spidering (http://tinyurl.com/dzjh7p)
		for(int i = 0; i<cacheDB.size();i++){
			ch = cacheDB.get(i);
			if (ch.mainCache == null) ch.initStates(false);
		}

		if (!loggedIn || Global.getPref().forceLogin) {
			if(login() != FormBase.IDOK) return;
		}

		doNotgetFound = false;

		OCXMLImporterScreen options;
		if (spiderAllFinds) {
			options = new OCXMLImporterScreen(MyLocale.getMsg(5510,"Spider Options"),
					OCXMLImporterScreen.MAXNUMBER|
					OCXMLImporterScreen.MAXUPDATE|
					OCXMLImporterScreen.IMAGES|
					OCXMLImporterScreen.ISGC|
					OCXMLImporterScreen.MAXLOGS|
					OCXMLImporterScreen.TRAVELBUGS);

			options.maxNumberUpdates.setText("0"); // no updates for founds

			if (options.execute() == FormBase.IDCANCEL) {return; }
			maxDistance = 1;
			minDistance = 0;
			direction="";
		} else {
			options = new OCXMLImporterScreen(MyLocale.getMsg(5510,"Spider Options"),
					OCXMLImporterScreen.MAXNUMBER|
					OCXMLImporterScreen.MAXUPDATE|
					OCXMLImporterScreen.INCLUDEFOUND|
					OCXMLImporterScreen.MINDIST|
					OCXMLImporterScreen.DIST|
					OCXMLImporterScreen.DIRECTION|
					OCXMLImporterScreen.IMAGES|
					OCXMLImporterScreen.ISGC|
					OCXMLImporterScreen.TRAVELBUGS|
					OCXMLImporterScreen.MAXLOGS|
					OCXMLImporterScreen.TYPE);

			if (pref.spiderUpdates == Preferences.NO) {options.maxNumberUpdates.setText("0");} // no updates else all

			if (options.execute() == FormBase.IDCANCEL) {return; }

			String minDist = options.minDistanceInput.getText();
			if (minDist.length() == 0) minDist="0";
			minDistance = Common.parseDouble(minDist);
			Double distDouble = new Double();
			distDouble.value = minDistance;
			minDist = distDouble.toString(0, 1, 0).replace(',', '.');
			profile.setMinDistGC(minDist);

			String maxDist = options.maxDistanceInput.getText();
			if (maxDist.length() ==  0) return;
			maxDistance = Common.parseDouble(maxDist);
			//save last radius to profile
			distDouble.value = maxDistance;
			maxDist = distDouble.toString(0, 1, 0).replace(',', '.');
			profile.setDistGC(maxDist);

			direction=options.directionInput.getText().toUpperCase();
			direction=direction.replace(' ',','); // separator blank to ,
			direction=direction.replace(';',','); // separator ; to ,
			profile.setDirectionGC(direction);
			direction=direction.replace('O', 'E'); // synonym for East
			direction=direction.replace('Z', 'S'); // synonym for South
			direction=direction.replace('P', 'S'); // synonym for South

			doNotgetFound = options.foundCheckBox.getState();
		}
		directions=mString.split(direction, ',');

		maxNew = -1;
		String maxNumberString = options.maxNumberInput.getText();
		if (maxNumberString.length()!= 0) {
			maxNew = Common.parseInt(maxNumberString);
		}
		if (maxNew != pref.maxSpiderNumber) {
			pref.maxSpiderNumber = maxNew;
			pref.savePreferences();
		}

		maxUpdate = -1;
		String maxUpdateString = options.maxNumberUpdates.getText();
		if (maxUpdateString.length()!= 0) {
			maxUpdate = Common.parseInt(maxUpdateString);
		}
		// TODO maxUpdate in preferences ?

		if (maxNew == 0) return;
		if(maxNew == -1) maxNew=Integer.MAX_VALUE;
		if(maxUpdate == -1) maxUpdate=Integer.MAX_VALUE;

		boolean getImages = options.imagesCheckBox.getState();
		boolean getTBs = options.travelbugsCheckBox.getState();

		cacheTypeRestriction = options.getCacheTypeRestriction(p);
		byte restrictedCacheType = options.getRestrictedCacheType(p);
		options.close(0);

		//max distance in miles for URL, so we can get more than 80km
		saveDistanceInMiles = maxDistance;
		if ( Global.getPref().metricSystem != Metrics.IMPERIAL ) {
			saveDistanceInMiles = Metrics.convertUnit(maxDistance, Metrics.KILOMETER, Metrics.MILES);
		}
		// add a mile to be save from different distance calculations in CW and at GC
		saveDistanceInMiles = java.lang.Math.ceil(saveDistanceInMiles) + 1;

		Hashtable cachesToUpdate = new Hashtable(cacheDB.size());
		Hashtable cachesShouldUpdate = new Hashtable(cacheDB.size()); // for don't loose the already done work

		double distanceInKm = maxDistance;
		if ( Global.getPref().metricSystem == Metrics.IMPERIAL ) {
			distanceInKm = Metrics.convertUnit(maxDistance, Metrics.MILES, Metrics.KILOMETER);
		}
		// to get in meantime possibly archived caches
		for(int i = 0; i<cacheDB.size();i++){
			ch = cacheDB.get(i);
			if (spiderAllFinds) {
				if ( (ch.getWayPoint().substring(0,2).equalsIgnoreCase("GC"))
						&& !ch.is_black() ) {
					cachesToUpdate.put(ch.getWayPoint(), ch);
				}
			} else {
				if ( (!ch.is_archived())
						&& (ch.kilom <= distanceInKm)
						&& !(doNotgetFound && (ch.is_found() || ch.is_owned()))
						&& (ch.getWayPoint().substring(0,2).equalsIgnoreCase("GC"))
						&& ( (restrictedCacheType == CacheType.CW_TYPE_ERROR) || (ch.getType() == restrictedCacheType) )
						&& !ch.is_black() ) {
					cachesToUpdate.put(ch.getWayPoint(), ch);
				}
			}
		}

		int startSize=cachesToUpdate.size();
		
		//=======
		// Prepare list of all caches that are to be spidered
		//=======
		getFirstListPage();

		int numFinds=0; // spiderAllFinds : Number of GC-founds for this user
		int numFoundInDB=0; // Number of GC-founds already in this profile
		if (spiderAllFinds) {
			numFoundInDB=getFoundInDB();
			numFinds=getNumFound(htmlListPage);
			maxNew=java.lang.Math.min(numFinds-numFoundInDB,maxNew);
			if (maxUpdate == 0 && maxNew == 0) { Vm.showWait(false); infB.close(0); return; }
		}
		boolean loadAllLogs = (pref.maxLogsToSpider > 5) || spiderAllFinds;
		try {
			//Loop pages till maximum distance has been found or no more caches are in the list
			pref.log("Download properties : \n"
					+ (spiderAllFinds ? "all Finds (DB/GC)"+ numFoundInDB+"/"+numFinds : "new and update Caches") + "\n"
					+ "minDistance: " + minDistance + "\n"
					+ "maxDistance: " + maxDistance + "\n"
					+ "directions: " + direction + "\n"
					+ "maxNew: " + maxNew + "\n"
					+ "maxUpdate: " + maxUpdate + "\n"
					+ "maxLogs: " + (loadAllLogs ? "completepage " : "shortpage") + "save:" + pref.maxLogsToSpider + "\n"
					+ "with pictures     : " + (!getImages ? "no" : "yes")+ "\n"
					+ "with tb           : " + (!getTBs ? "no" : "yes") + "\n"
					+ "with Founds       : " + (doNotgetFound ? "no" : "yes") + "\n"
					+ "alias is premium m: " + (!pref.isPremium ? "no" : "yes") + "\n"
					+ "Update if new Logs: " + (!pref.checkLog ? "no" : "yes") + "\n"
					);
			while(maxDistance > 0){
				RexPropListBlock.search(htmlListPage);
				String tableOfHtmlListPage;
				if (RexPropListBlock.didMatch()) {
					tableOfHtmlListPage = RexPropListBlock.stringMatched(1);
				}
				else {
					pref.log("check listBlockRex in spider.def\n"+htmlListPage);
					tableOfHtmlListPage = "";
				}
				RexPropLine.search(tableOfHtmlListPage);
				while (maxDistance > 0){
					if (!RexPropLine.didMatch()) {
						if (page_number==1 && found_on_page==0) pref.log("check lineRex in spider.def");
						break;
					}
					found_on_page++;
					String CacheDescriptionGC=RexPropLine.stringMatched(1);
					double gotDistance=getDistGC(CacheDescriptionGC);
					String chWaypoint=getWP(CacheDescriptionGC);
					if(gotDistance <= maxDistance){
						ch=cacheDB.get(chWaypoint);
						if(ch == null){ // not in DB
							if ( gotDistance >= minDistance &&
									 directionOK(directions,getDirection(CacheDescriptionGC))  &&
									 doPMCache(CacheDescriptionGC) &&
									 cachesToLoad.size() < maxNew){
								if (CacheDescriptionGC.indexOf(propFound)!=-1) chWaypoint=chWaypoint+"found";
							cachesToLoad.add(chWaypoint);
							}
							else {
								// pref.log("no load of (Premium Cache/other direction/short Distance ?) " + chWaypoint);
								cachesToUpdate.remove( chWaypoint );
								}
						}
						else {
							if (maxUpdate>0) {
								if (doPMCache(CacheDescriptionGC) && updateExists(ch,CacheDescriptionGC)) {
									if (!ch.is_black() && (cachesShouldUpdate.size()<maxUpdate)) cachesShouldUpdate.put(chWaypoint, ch);
								}
								else {
									cachesToUpdate.remove( chWaypoint );
								}
							}
						}
						if(cachesToLoad.size() >= maxNew) {
							if(cachesShouldUpdate.size() >= maxUpdate) {
								maxDistance=0;
								cachesToUpdate.clear();
							} else {
								if ( cachesToUpdate.size() <= cachesShouldUpdate.size() ) {
									maxDistance=0;
								}
							}
						}
					} else maxDistance = 0; // finish listing
					// get next row of table (next Cache Description) of this htmlListPage
					RexPropLine.searchFrom(tableOfHtmlListPage, RexPropLine.matchedTo());
					if (infB.isClosed) break;
				} // next Cache
				infB.setInfo(MyLocale.getMsg(5521,"Page ") + page_number + "\n" +
							 MyLocale.getMsg(5511,"Found ") + cachesToLoad.size() + MyLocale.getMsg(5512," caches"));
				if(found_on_page < 20) {
					if (spiderAllFinds) {
						// check all pages ( seen a gc-account with found_on_page less 20 and not on end )
						if (((page_number-1)*20+found_on_page) >= numFinds) {
							maxDistance = 0;
						}
					}
					else maxDistance = 0; // last page (has less than 20 entries!?) to check reached
				}
				if(maxDistance > 0){getNextListPage();}
			} // loop pages
		} // try
		catch (Exception ex) {
			pref.log("Download error : ", ex);
			infB.close(0);
			Vm.showWait(false);
			return;
		}

		if (infB.isClosed) { Vm.showWait(false); return; }
		infB.setInfo(MyLocale.getMsg(5511,"Found ") + cachesToLoad.size() + MyLocale.getMsg(5512," caches"));

		pref.log("Checked " + page_number + " pages");
		pref.log("with " + ((page_number-1)*20+found_on_page) + " caches");
		pref.log("Found " + cachesToLoad.size() + " new caches");
		pref.log("Found " + cachesToUpdate.size() + "/" + cachesShouldUpdate.size() + " caches for update");
		if(spiderAllFinds){
			pref.log("Found " + numFoundUpdates + " caches with no found in profile.");
			pref.log("Found " + numArchivedUpdates + " caches with changed archived status.");
		}
		pref.log("Found " + numAvailableUpdates + " caches with changed available status.");
		pref.log("Found " + numLogUpdates + " caches with new found in log (inc. blacklisted");
		pref.log("Found " + (cachesToUpdate.size()-numAvailableUpdates-numLogUpdates) + " caches possibly archived.");
		pref.log("Found " + cachesShouldUpdate.size() + "?=" + (numFoundUpdates+numArchivedUpdates+numAvailableUpdates+numArchivedUpdates) + " caches to update.");
		pref.log("Found " + numPrivate + " Premium Caches (for non Premium Member.)");

		//=======
		// Now ready to spider each cache in the list
		//=======

		int spiderErrors = 0;
		if (cachesToUpdate.size() == startSize)
			cachesToUpdate.clear(); // there must be something wrong
		if (cachesToUpdate.size() == 0 || cachesToUpdate.size() > maxUpdate)
			cachesToUpdate=cachesShouldUpdate;

		if ( cachesToUpdate.size() > 0 ) {
			switch (pref.spiderUpdates) {
			case Preferences.NO:
				cachesToUpdate.clear();
				break;
			case Preferences.ASK:
				MessageBox mBox = new MessageBox(MyLocale.getMsg(5517,"Spider Updates?"), cachesToUpdate.size() + MyLocale.getMsg(5518," caches in database need an update. Update now?") , FormBase.IDYES |FormBase.IDNO);
				if (mBox.execute() != FormBase.IDOK){
					cachesToUpdate.clear();
				}
				break;
			}
		}

		int totalCachesToLoad = cachesToLoad.size() + cachesToUpdate.size();

		for(int i = 0; i<cachesToLoad.size(); i++){
			if (infB.isClosed) break;

			String wpt = (String)cachesToLoad.get(i);
			boolean is_found = wpt.indexOf("found")!=-1;
			if (is_found) wpt=wpt.substring(0,wpt.indexOf("found"));
			// Get only caches not already available in the DB
			if(cacheDB.getIndex(wpt) == -1){
				infB.setInfo(MyLocale.getMsg(5513,"Loading: ") + wpt +" (" + (i+1) + " / " + totalCachesToLoad + ")");
				CacheHolder holder = new CacheHolder();
				holder.setWayPoint(wpt);
				int test = getCacheByWaypointName(holder,false,getImages,getTBs,doNotgetFound,loadAllLogs||is_found);
				if (test == SPIDER_CANCEL) {
					infB.close(0);
					break;
				} else if (test == SPIDER_ERROR) {
					spiderErrors++;
				} else if (test == SPIDER_OK){
					cacheDB.add(holder);
					holder.save();
				} // For test == SPIDER_IGNORE_PREMIUM and SPIDER_IGNORE there is nothing to do
			}
		}

		if (!infB.isClosed) {
			int j = 1;
			for (Enumeration e = cachesToUpdate.elements() ; e.hasMoreElements() ; j++) {
				ch = (CacheHolder)e.nextElement();
				infB.setInfo(MyLocale.getMsg(5513,"Loading: ") + ch.getWayPoint() +" (" + (cachesToLoad.size()+j) + " / " + totalCachesToLoad + ")");
				int test = spiderSingle(cacheDB.getIndex(ch), infB,false,loadAllLogs);
				if (test == SPIDER_CANCEL) {
					break;
				} else if (test == SPIDER_ERROR) {
					spiderErrors++;
					Global.getPref().log("SpiderGC: could not spider "+ch.getWayPoint());
				} else {
					//profile.hasUnsavedChanges=true;
				}
			}
		}

		infB.close(0);
		Vm.showWait(false);
		if ( spiderErrors > 0) {
			new MessageBox(MyLocale.getMsg(5500,"Error"),spiderErrors + MyLocale.getMsg(5516," cache descriptions%0acould not be loaded."),FormBase.DEFOKB).execute();
		}
		if ( maxNumberAbort ) {
			new MessageBox(MyLocale.getMsg(5519,"Information"),MyLocale.getMsg(5520,"Only the given maximum of caches were loaded.%0aRepeat spidering later to load more caches.%0aNo already existing caches were updated."),FormBase.DEFOKB).execute();
		}
		Global.getProfile().restoreFilter();
		Global.getProfile().saveIndex(Global.getPref(),true);
	} // End of DoIt spider many / all finds

	/**
	 * Method to spider a single cache.
	 * It assumes a login has already been performed!
	 * @return 1 if spider was successful, -1 if spider was cancelled by closing the infobox, 0 error, but continue with next cache
	 */
	public int spiderSingle(int number, InfoBox pInfB, boolean forceLogin, boolean loadAllLogs){
		int ret=-1;
		this.infB = pInfB;
		CacheHolder ch = new CacheHolder(); // cacheDB.get(number);
		ch.setWayPoint(cacheDB.get(number).getWayPoint());
		if (ch.isAddiWpt()) return -1;  // No point re-spidering an addi waypoint, comes with parent

		// check if we need to login
		if (!loggedIn || forceLogin){
			if (this.login()!=FormBase.IDOK) return -1;
			// loggedIn is already set by this.login()
		}
		try{
			// Read the cache data from GC.COM and compare to old data
			ret=getCacheByWaypointName(ch,true,pref.downloadPics,pref.downloadTBs,false,loadAllLogs);
			// Save the spidered data
			if (ret == SPIDER_OK) {
				CacheHolder cacheInDB = cacheDB.get(number);
				cacheInDB.initStates(false);
				if (cacheInDB.is_found() && !ch.is_found() && ! loadAllLogs) {
					// If the number of logs to spider is 5 or less, then the "not found" information
					// of the spidered cache is not credible. In this case it should not overwrite
					// the "found" state of an existing cache.
					ch.setFound(true);
				}
				// preserve rating information
				ch.setNumRecommended(cacheInDB.getNumRecommended());
				if (pref.downloadPics) {
					// delete obsolete images when we have current set
					CacheImages.cleanupOldImages(cacheInDB.getCacheDetails(true).images, ch.getCacheDetails(false).images);
				} else {
					// preserve images if not downloaded
					ch.getCacheDetails(false).images = cacheInDB.getCacheDetails(true).images;
				}
				cacheInDB.update(ch);
				cacheInDB.save();
			}
		}catch(Exception ex){
			pref.log("Error spidering " + ch.getWayPoint() + " in spiderSingle");
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
			if (login()!=FormBase.IDOK) {
				return "";
			}
		}
		InfoBox localInfB = new InfoBox("Info", "Loading", InfoBox.PROGRESS_WITH_WARNINGS);
		localInfB.exec();
		try{
			String doc = p.getProp("waypoint") + wayPoint;
			pref.log("Fetching: " + wayPoint);
			completeWebPage = fetch(doc);
		}catch(Exception ex){
			localInfB.close(0);
			pref.log("Could not fetch " + wayPoint,ex);
			return "";
		}
		localInfB.close(0);
		try {
			return getLatLon(completeWebPage);
		} catch (Exception ex) {
			return "????";
		}
	} // getCacheCoordinates

	/**
	 * Method to login the user to gc.com
	 * It will request a password and use the alias defined in preferences
	 * If the login page cannot be fetched, the password is cleared.
	 * If the login fails, an appropriate message is displayed.
	 */
	private int login(){
		loggedIn = false;
		String loginPage,loginPageUrl,loginSuccess,nextPage;
		try {
			loginPageUrl=p.getProp("loginPage");
			loginSuccess=p.getProp("loginSuccess");
			nextPage=p.getProp("nextPage");
		} catch (Exception ex) { // Tag not found in spider.def
			return ERR_LOGIN;
		}
		//Get password
		InfoBox localInfB = new InfoBox(MyLocale.getMsg(5506,"Password"), MyLocale.getMsg(5505,"Enter Password"), InfoBox.INPUT);
		localInfB.feedback.setText(passwort); // Remember the PWD for next time
		localInfB.feedback.isPassword=true;
		int code=FormBase.IDOK;
		if (passwort.equals("")) {
			code = localInfB.execute();
			passwort = localInfB.getInput();
		}
		localInfB.close(0);
		if(code != FormBase.IDOK) return code;
		// Now start the login proper
		localInfB = new InfoBox(MyLocale.getMsg(5507,"Status"), MyLocale.getMsg(5508,"Logging in..."));
		localInfB.exec();
		try{
			pref.log("[login]:Fetching login page");
			//Access the page once to get a viewstate
			loginPage = fetch(loginPageUrl);   //http://www.geocaching.com/login/Default.aspx
			if (loginPage.equals("")) {
				localInfB.close(0);
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5499,"Error loading login page.%0aPlease check your internet connection."), FormBase.OKB)).execute();
				pref.log("[login]:Could not fetch: gc.com login page");
				return ERR_LOGIN;
			}
		} catch(Exception ex){
			localInfB.close(0);
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5499,"Error loading login page.%0aPlease check your internet connection."), FormBase.OKB)).execute();
			pref.log("[login]:Could not fetch: gc.com login page",ex);
			return ERR_LOGIN;
		}
		if (!localInfB.isClosed) { // If user has not aborted, we continue
			Regex rexCookieID = new Regex("(?i)Set-Cookie: userid=(.*?);.*");
			Regex rexViewstate = new Regex("id=\"__VIEWSTATE\" value=\"(.*?)\" />");
			// Regex rexViewstate1 = new Regex("id=\"__VIEWSTATE1\" value=\"(.*?)\" />");
			Regex rexEventvalidation = new Regex("id=\"__EVENTVALIDATION\" value=\"(.*?)\" />");
			Regex rexCookieSession = new Regex("(?i)Set-Cookie: ASP.NET_SessionId=(.*?);.*");
			String viewstate="";
			rexViewstate.search(loginPage);
			if(rexViewstate.didMatch()){
				viewstate = rexViewstate.stringMatched(1);
				//Vm.debug("ViewState: " + viewstate);
			} else {
				pref.log("[login]:rexViewstate not found before login\n");
			}

			if(loginPage.indexOf(loginSuccess) > 0)
				pref.log("[login]:Already logged in");
			else {
				rexEventvalidation.search(loginPage);
				if(rexEventvalidation.didMatch()){
					// eventvalidation = rexEventvalidation.stringMatched(1);
					//Vm.debug("EVENTVALIDATION: " + eventvalidation);
				} else
					pref.log("[login]:rexEventvalidation not found before login\n");
				//Ok now login!
				try{
					pref.log("[login]:Logging in as "+pref.myAlias);
					StringBuffer sb=new StringBuffer(1000);
					sb.append(URL.encodeURL("__VIEWSTATE",false));	sb.append("="); sb.append(URL.encodeURL(viewstate,false));
					sb.append("&ctl00%24ContentBody%24"); sb.append(URL.encodeURL("myUsername",false));
					sb.append("="); sb.append(encodeUTF8URL(Utils.encodeJavaUtf8String(pref.myAlias)));
					sb.append("&ctl00%24ContentBody%24"); sb.append(URL.encodeURL("myPassword",false));
					sb.append("="); sb.append(encodeUTF8URL(Utils.encodeJavaUtf8String(passwort)));
					sb.append("&ctl00%24ContentBody%24"); sb.append(URL.encodeURL("cookie",false));
					sb.append("="); sb.append(URL.encodeURL("on",false));
					sb.append("&ctl00%24ContentBody%24"); sb.append(URL.encodeURL("Button1",false));
					sb.append("="); sb.append(URL.encodeURL("Login",false));
//					sb.append("&"); sb.append(URL.encodeURL("__EVENTVALIDATION",false));
//					sb.append("="); sb.append(URL.encodeURL(eventvalidation,false));
					loginPage = fetch_post(loginPageUrl, sb.toString(), nextPage);  // /login/default.aspx
					if(loginPage.indexOf(loginSuccess) > 0)
						pref.log("[login]:Login successful");
					else {
						pref.log("[login]:Login failed. Wrong Account or Password?");
						if (pref.debug) {
							pref.log("[login.LoginUrl]:"+sb.toString());
							pref.log("[login.Answer]:"+loginPage);
						}
						localInfB.close(0);
						(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5501,"Login failed! Wrong account or password?"), FormBase.OKB)).execute();
						return ERR_LOGIN;
					}
				}catch(Exception ex){
					pref.log("[login]:Login failed with exception.", ex);
					localInfB.close(0);
					(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5501,"Login failed. Error loading page after login."), FormBase.OKB)).execute();
					return ERR_LOGIN;
				}
			}

			rexViewstate.search(loginPage);
			if (!rexViewstate.didMatch()) {
				pref.log("[login]:check rexViewstate in SpiderGC.java --> not found after login\n"+loginPage);
			}
			viewstate = rexViewstate.stringMatched(1);
			
			rexCookieID.search(loginPage);
			if (!rexCookieID.didMatch()) {
				pref.log("[login]:check rexCookieID in SpiderGC.java --> CookieID not found. Using old one.\n"+loginPage);
			} else
				cookieID = rexCookieID.stringMatched(1);
			//Vm.debug(cookieID);
			rexCookieSession.search(loginPage);
			if (!rexCookieSession.didMatch()) {
				pref.log("[login]:check rexCookieSession in SpiderGC.java --> CookieSession not found. Using old one.\n"+loginPage);
				//cookieSession="";
			} else
				cookieSession = rexCookieSession.stringMatched(1);
			//Vm.debug("cookieSession = " + cookieSession);

			/*
			String viewstate1;
			rexViewstate1.search(loginPage);
			if(rexViewstate1.didMatch()){
				viewstate1 = rexViewstate1.stringMatched(1);
			} else {
				viewstate1 = "";
				pref.log("[login]:check rexViewstate1 in SpiderGC.java --> not found after login\n"+loginPage);
			}
			*/
						
			/*
			rexEventvalidation.search(htmlPage);
			if(rexEventvalidation.didMatch()){
				eventvalidation = rexEventvalidation.stringMatched(1);
			} else {
				eventvalidation = "";
			}
			*/

			String strEnglishPage = "ctl00$uxLocaleList$uxLocaleList$ctl01$uxLocaleItem";
			String postStr = URL.encodeURL("__EVENTTARGET",false) +"="+ URL.encodeURL(strEnglishPage,false)
		    + "&" + URL.encodeURL("__EVENTARGUMENT",false) +"="+ URL.encodeURL("",false)
//		    + "&" + URL.encodeURL("__VIEWSTATEFIELDCOUNT",false) +"=2"
		    + "&" + URL.encodeURL("__VIEWSTATE",false) +"="+ URL.encodeURL(viewstate,false);
//		    + "&" + URL.encodeURL("__VIEWSTATE1",false) +"="+ URL.encodeURL(viewstate1,false);
//		    + "&" + URL.encodeURL("__EVENTVALIDATION",false) +"="+ URL.encodeURL(eventvalidation,false);
			try{
				pref.log("Switching to English:" + postStr);
				loginPage = fetch_post(loginPageUrl, postStr, nextPage);
			}catch(Exception ex){
				pref.log("Error switching to English");
			}
		}
		boolean loginAborted=localInfB.isClosed;
		localInfB.close(0);
		if (loginAborted)
			return FormBase.IDCANCEL;
		else {
			loggedIn = true;
			return FormBase.IDOK;
		}
	}
	/*
	 *
	 */
	private void initialiseProperties() {
		try {
			propFirstPage=p.getProp("firstPage");
			propFirstPage2=p.getProp("firstPage2");
			propFirstPageFinds=p.getProp("firstPageFinds");
			propFirstLine=p.getProp("firstLine");
			propFirstLine2=p.getProp("firstLine2");
			propMaxDistance=p.getProp("maxDistance");
			propShowOnlyFound=p.getProp("showOnlyFound");
			RexPropListBlock = new Regex(p.getProp("listBlockRex"));
			RexPropLine = new Regex(p.getProp("lineRex"));
			RexNumFinds = new Regex("Total Records: <b>(.*?)</b>");
			RexPropLogDate = new Regex(p.getProp("logDateRex"));
			propAvailable=p.getProp("availableRex");
			propArchived=p.getProp("archivedRex");
			propFound=p.getProp("found");		
			propPM=p.getProp("PMRex");
			RexPropDirection=new Regex(p.getProp("directionRex"));
			RexPropDistance = new Regex(p.getProp("distRex"));
			RexPropWaypoint = new Regex(p.getProp("waypointRex"));
			RexPropType = new Regex(p.getProp("TypeRex"));
			RexPropSize = new Regex(p.getProp("SizeRex"));
			RexPropDandT = new Regex(p.getProp("DandTRex"));
			RexPropOwn = new Regex(p.getProp("own"));
			RexLogBlock = new Regex(p.getProp("blockRex"));
			exSingleLog = new Extractor("",p.getProp("singleLogExStart"), p.getProp("singleLogExEnd"), 0, false); 
			exIcon = new Extractor("",p.getProp("iconExStart"), p.getProp("iconExEnd"), 0, true);
			exNameTemp = new Extractor("",p.getProp("nameTempExStart"), p.getProp("nameTempExEnd"), 0 , true);
			exName = new Extractor("", p.getProp("nameExStart"), p.getProp("nameExEnd"), 0 , true);
			exDate = new Extractor("",p.getProp("dateExStart"), p.getProp("dateExEnd"), 0 , true);
			exLog = new Extractor("", p.getProp("logExStart"), p.getProp("logExEnd"), 0, true);
			exLogId = new Extractor("", p.getProp("logIdExStart"), p.getProp("logIdExEnd"), 0, true);
			icon_smile=p.getProp("icon_smile");
			icon_camera=p.getProp("icon_camera");
			icon_attended=p.getProp("icon_attended");
			RexCacheType = new Regex(p.getProp("cacheTypeRex"));
		}catch(Exception ex){
		}
	}

	/*
	 *
	 */
	private void getFirstListPage() {
		Vm.showWait(true);
		infB = new InfoBox("Status", MyLocale.getMsg(5502,"Fetching first page..."));
		infB.exec();
		//Get first page

		String url;
		if (spiderAllFinds) {
			url = propFirstPageFinds + encodeUTF8URL(Utils.encodeJavaUtf8String(pref.myAlias));
		} else {
			url = propFirstPage + origin.getLatDeg(TransformCoordinates.DD) + propFirstPage2 + origin.getLonDeg(TransformCoordinates.DD)
		                              + propMaxDistance + Integer.toString( (int)saveDistanceInMiles );
			if(doNotgetFound) url = url + propShowOnlyFound;
		}
		url = url + cacheTypeRestriction;
		pref.log("Getting first page: "+url);
		try{
			htmlListPage = fetch(url);
			pref.log("Got first page");
		}catch(Exception ex){
			pref.log("Error fetching first list page",ex,true);
			Vm.showWait(false);
			infB.close(0);
			(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5503,"Error fetching first list page."), FormBase.OKB)).execute();
			return;
		}
		page_number = 1;
		found_on_page = 0;
	}

	/**
	 * in: ...
	 * out: page_number,htmlPage
	 */
	private void getNextListPage() {
		String postStr;
		if (spiderAllFinds) {
			postStr = propFirstLine;
		} else {
			postStr = propFirstLine + origin.getLatDeg(TransformCoordinates.DD) + propFirstLine2 + origin.getLonDeg(TransformCoordinates.DD)
					                             + propMaxDistance + Integer.toString( (int)saveDistanceInMiles );
			if(doNotgetFound) postStr = postStr + propShowOnlyFound;
		}
		postStr = postStr + cacheTypeRestriction;
		
		Regex rexViewstate = new Regex("id=\"__VIEWSTATE\" value=\"(.*?)\" />");
		String viewstate;
		rexViewstate.search(htmlListPage);
		if(rexViewstate.didMatch()){
			viewstate = rexViewstate.stringMatched(1);
		} else {
			viewstate = "";
			pref.log("[getNextListPage]:check rexViewstate in SpiderGC.java\n"+htmlListPage);
		}
		
		Regex rexViewstate1 = new Regex("id=\"__VIEWSTATE1\" value=\"(.*?)\" />");
		String viewstate1;
		rexViewstate1.search(htmlListPage);
		if(rexViewstate1.didMatch()){
			viewstate1 = rexViewstate1.stringMatched(1);
		} else {
			viewstate1 = "";
			pref.log("[getNextListPage]:check rexViewstate1 in SpiderGC.java\n"+htmlListPage);
		}

		/*
		rexEventvalidation.search(htmlPage);
		if(rexEventvalidation.didMatch()){
			eventvalidation = rexEventvalidation.stringMatched(1);
		} else {
			eventvalidation = "";
		}
		*/

		String strNextPage = "ctl00$ContentBody$pgrTop$ctl08";
		String url = URL.encodeURL("__EVENTTARGET",false) +"="+ URL.encodeURL(strNextPage,false)
	    + "&" + URL.encodeURL("__EVENTARGUMENT",false) +"="+ URL.encodeURL("",false)
	    + "&" + URL.encodeURL("__VIEWSTATEFIELDCOUNT",false) +"=2"
	    + "&" + URL.encodeURL("__VIEWSTATE",false) +"="+ URL.encodeURL(viewstate,false)
	    + "&" + URL.encodeURL("__VIEWSTATE1",false) +"="+ URL.encodeURL(viewstate1,false);
//	    + "&" + URL.encodeURL("__EVENTVALIDATION",false) +"="+ URL.encodeURL(eventvalidation,false);
		try{
			pref.log("Fetching next list page:" + url);
			htmlListPage = fetch_post(postStr, url, p.getProp("nextListPage"));
		}catch(Exception ex){
			pref.log("Error getting next page");
		}
		page_number++;
		found_on_page = 0;
	}

	/**
	 * check if new Update exists
	 * @param ch CacheHolder
	 * @param CacheDescription A previously fetched cachepage
	 * @return true if new Update exists else false
	 */
	private boolean updateExists(CacheHolder ch, String CacheDescription) {
		boolean ret = false;
		boolean save = false;
		boolean is_archived_GC=false;
		boolean is_found_GC=false;
		CacheHolderDetail chd = ch.getCacheDetails(false);
		if (spiderAllFinds) {
			if(!ch.is_found()) { ch.setFound(true); save=true; numFoundUpdates+=1; ret=true;}
			is_archived_GC=CacheDescription.indexOf(propArchived) != -1;
			if (is_archived_GC!=ch.is_archived()) { ch.setArchived(is_archived_GC); save=true; numArchivedUpdates+=1; ret=true;}
		} else if (!doNotgetFound){ // there could be a found or own ...
			is_found_GC=CacheDescription.indexOf(propFound)!=-1;
			if (is_found_GC!=ch.is_found()) {ch.setFound(is_found_GC); save=true; ret=true;}
		}
		if (ch.is_found() && chd.OwnLogId.equals("")) {ret=true;} //missing ownLogID
		boolean is_available_GC=!is_archived_GC && CacheDescription.indexOf(propAvailable) == -1;
		if (is_available_GC != ch.is_available()) {
			ch.setAvailable(is_available_GC); save=true; numAvailableUpdates+=1; ret=true;}
		if (typeChanged(ch,CacheDescription)) { save=true; ret=true;}
		if (sizeChanged(ch,CacheDescription)) { save=true; ret=true;}
		if (difficultyOrTerrainChanged(ch,CacheDescription)) {save=true; ret=true;}
		if (newFoundExists(ch,CacheDescription)) {
			numLogUpdates++; ret=true;
			}
		if (save) ch.save();
		return ret;
	}
	/**
	 * Get num found
	 * @param doc A previously fetched cachepage
	 * @return numFound
	 */
	private int getNumFound(String doc) {
		RexNumFinds.search(doc);
		if (RexNumFinds.didMatch()) {
			 return Convert.toInt(RexNumFinds.stringMatched(1));}
		else {
			pref.log("check RexNumFinds in SpiderGC.java / initialiseProperties\n"+doc);
			return 0;
		}
	}
	private int getFoundInDB() {
		CacheHolder ch;
		int counter = 0;
		for(int i = 0; i<cacheDB.size();i++){
			ch = cacheDB.get(i);
			if(ch.is_found()) {
				if(ch.getWayPoint().startsWith("GC") ) counter++;
			}
		}
		return counter;
	}
	/**
	 * Get the Distance to the centre
	 * @param doc A previously fetched cachepage
	 * @return Distance
	 */
	private double getDistGC(String doc) throws Exception {
		if(doc.indexOf("Here") >= 0) {
			return(0);
		}
		else {
			RexPropDistance.search(doc);
			if (!RexPropDistance.didMatch()) {
				pref.log("check distRex in spider.def\n"+doc);
				return 0;
			}
			if(MyLocale.getDigSeparator().equals(",")) return Convert.toDouble(RexPropDistance.stringMatched(1).replace('.',','));
			return Convert.toDouble(RexPropDistance.stringMatched(1));
		}
	}
	/**
	 * Get the waypoint name
	 * @param doc A previously fetched cachepage
	 * @return Name of waypoint to add to list
	 */
	private String getWP(String doc) throws Exception {
		RexPropWaypoint.search(doc);
		if (!RexPropWaypoint.didMatch()) {
			pref.log("check waypointRex in spider.def\n"+doc);
			return "???";
		}
		return "GC"+RexPropWaypoint.stringMatched(1);
	}

	/**
	 * check for Premium Member Cache
	 */
	private boolean doPMCache(String toCheck) {
		if (pref.isPremium) return true;
		if (toCheck.indexOf(propPM) <= 0) {
			return true;
		}
		else {
			numPrivate=numPrivate+1;
			return false;			
		}
	}
	/*
	 * check for changed Cachetype
	 */
	private boolean typeChanged(CacheHolder ch, String toCheck){
		RexPropType.search(toCheck);
		if(RexPropType.didMatch()) {
			String stmp=RexPropType.stringMatched(1);
			if (Common.parseInt(stmp) == 0){
				if (stmp.equalsIgnoreCase("EarthCache")) stmp="137";
			}
			if(ch.getType() == CacheType.gcSpider2CwType(stmp))
				return false;
				else {
					ch.setType(CacheType.gcSpider2CwType(stmp));
					return true;
				}
		}
		pref.log("check TypeRex in spider.def\n"+toCheck);
		return false;
	}
	/*
	 * check for changed CacheSize
	 */
	private boolean sizeChanged(CacheHolder ch, String toCheck) {
		RexPropSize.search(toCheck);
		if(RexPropSize.didMatch()){
			String stmp=RexPropSize.stringMatched(1);
			if(ch.getCacheSize() == CacheSize.gcSpiderString2Cw(stmp))
				return false;
				else {
					ch.setCacheSize(CacheSize.gcSpiderString2Cw(stmp));
					return true;
				}
		}
		pref.log("check SizeRex in spider.def\n"+toCheck);
		return false;
	}
	/*
	 * check for changed Difficulty or Terrain
	 */
	private boolean difficultyOrTerrainChanged(CacheHolder ch, String toCheck) {
		boolean ret = false;
		RexPropDandT.search(toCheck);	
		if(RexPropDandT.didMatch()){
			String stmp=RexPropDandT.stringMatched(1);
			if(!(ch.getHard() == CacheTerrDiff.v1Converter(stmp))) {
				ch.setHard(CacheTerrDiff.v1Converter(stmp));
				ret=true;
			}
			stmp=RexPropDandT.stringMatched(2);
			if(!(ch.getTerrain() == CacheTerrDiff.v1Converter(stmp))) {
				ch.setTerrain(CacheTerrDiff.v1Converter(stmp));
				ret=true;
			}
		}
		else {
			pref.log("check DandTRex in spider.def\n"+toCheck);
		}
		return ret;
	}
	
	/**
	 * Get the direction
	 * @param doc A previously fetched cachepage
	 * @return direction String
	 */
	private String getDirection(String doc) throws Exception {
		RexPropDirection.search(doc);
		if (!RexPropDirection.didMatch()) {
			pref.log("check directionRex in spider.def\n"+doc);
			return "";
		}
		return RexPropDirection.stringMatched(1);
	}

	/*
	 * if cache lies in the desired direction
	 */
	private boolean directionOK(String[] directions, String gotDirection) {
		if (directions.length == 0) return true; // nothing means all
		for (int i = 0; i < directions.length; i++) {
			if (directions[i].equals(gotDirection)) {
				return true;
			}
			int j=directions[i].indexOf("*");
			if (j>0){
				if (gotDirection.indexOf(directions[i].substring(0, 1))>-1) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * @param CacheHolder ch
	 * @param String cacheDescGC
	 * @return boolean newLogExists
	 */
	private boolean newFoundExists(CacheHolder ch, String cacheDescrition) {
		if(!pref.checkLog) return false;
		// String[] CacheDesc=mString.split(cacheDescrition,'\n');
		Time lastLogCW = new Time();
		Log lastLog = ch.getCacheDetails(true).CacheLogs.getLog(0);
		if (lastLog == null) return true;
		String slastLogCW=lastLog.getDate();
		if (slastLogCW.equals("")) return true; // or check cacheDescGC also no log?
		lastLogCW.parse(slastLogCW,"yyyy-MM-dd");

		Time lastLogGC = new Time(); // is current time
		lastLogGC.hour=0;
		lastLogGC.minute=0;
		lastLogGC.second=0;
		lastLogGC.millis=0;
		String[] SDate;
		String stmp="";
		RexPropLogDate.search(cacheDescrition);
		if (RexPropLogDate.didMatch()) {
			stmp=RexPropLogDate.stringMatched(1);
		}
		else {
			pref.log("check logDateRex in spider.def\n"+cacheDescrition);
			return false;
		}
		if (stmp.indexOf("day")>0) {
			lastLogGC.day=java.lang.Math.max(1, lastLogGC.day-7); // simplyfied (update if not newer than last week)
		}
		else if (stmp.equals("")) {
			Vm.debug("no log yet");
			return false; // no log yet
		}
		else {
			final String monthNames[] = { "January", "February", "March", "April", "May",
					"June", "July", "August", "September", "October", "November",
					"December" };
			SDate=mString.split(stmp,' ');
			lastLogGC.day=Integer.parseInt(SDate[0]);
			for (int m = 0; m < 12; m++) {
				if (monthNames[m].startsWith(SDate[1])) {
					lastLogGC.month=m+1;
					m=12;
				}
			}
			lastLogGC.year=2000+Integer.parseInt(SDate[2].substring(0,2));
		}
		// compare
		// Vm.debug("CW:"+lastLogCW.toString()+" GC:"+lastLogGC.toString());
		return lastLogCW.compareTo(lastLogGC) < 0;
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
	 * @param boolean fetchTBs True if the TBs are to be fetched
	 * @param boolean doNotGetFound True if the cache is not to be spidered if it has already been found
	 * @param boolean fetchAllLogs True if all logs are to be fetched (by adding option '&logs=y' to command line).
	 *     This is normally false when spidering from GPXImport as the logs are part of the GPX file, and true otherwise
	 * @return -1 if the infoBox was closed (cancel spidering), 0 if there was an error (continue with next cache), 1 if everything ok
	 */
	private int getCacheByWaypointName(CacheHolder ch, boolean isUpdate, boolean fetchImages, boolean fetchTBs, boolean doNotGetFound, boolean fetchAllLogs) {
		int ret = SPIDER_OK; // initialize value;
		while (true) { // retry even if failure
			pref.log(""); // new line for more overview
			String completeWebPage;
			int spiderTrys=0;
			int MAX_SPIDER_TRYS=3;
			while (spiderTrys++<MAX_SPIDER_TRYS) {
				ret = SPIDER_OK; // initialize value;
				try{
					String doc = p.getProp("getPageByName") + ch.getWayPoint() +((fetchAllLogs||ch.is_found())?p.getProp("fetchAllLogs"):"");
					pref.log("Fetching: " + ch.getWayPoint());
					completeWebPage = fetch(doc);
					if	( completeWebPage.equals("")) {
						pref.log("Could not fetch " + ch.getWayPoint());
						if (!infB.isClosed) {
							continue;
						} else {
							ch.setIncomplete(true);
							return SPIDER_CANCEL;
						}
					}
				}catch(Exception ex){
					pref.log("Could not fetch " + ch.getWayPoint(),ex);
					if (!infB.isClosed) {
						continue;
					} else {
						ch.setIncomplete(true);
						return SPIDER_CANCEL;
					}
				}
				// Only analyse the cache data and fetch pictures if user has not closed the progress window
				if (!infB.isClosed) {
					try{
						ch.initStates(!isUpdate);

						//first check if coordinates are available to prevent deleting existing coorinates
						String latLon = getLatLon(completeWebPage);
						if (latLon.equals("???")) {
							if (completeWebPage.indexOf(p.getProp("premiumCachepage"))>0) {
								// Premium cache spidered by non premium member
								pref.log("Ignoring premium member cache: "+ch.getWayPoint());
								spiderTrys = MAX_SPIDER_TRYS;
								ret = SPIDER_IGNORE_PREMIUM;
								continue;
							} else {
								pref.log(">>>> Failed to spider Cache. Retry.");
								ret = SPIDER_ERROR;
								continue; // Restart the spider
							}
						}

						ch.setHTML(true);
						ch.setIncomplete(true);
						// Save size of logs to be able to check whether any new logs were added
						// int logsz = chD.CacheLogs.size();
						//chD.CacheLogs.clear();
						ch.addiWpts.clear();
						ch.getCacheDetails(false).images.clear();

						ch.setAvailable(!(completeWebPage.indexOf(p.getProp("cacheUnavailable")) >= 0));
						ch.setArchived(completeWebPage.indexOf(p.getProp("cacheArchived")) >= 0);
						//==========
						// Logs first (for check early for break)
						//==========
						ch.getCacheDetails(false).setCacheLogs(getLogs(completeWebPage, ch.getCacheDetails(false)));
						pref.log("Got logs");
						// If the switch is set to not store found caches and we found the cache => return
						if (ch.is_found() && doNotGetFound) {
							if (infB.isClosed) {
								return SPIDER_CANCEL;
							} else {
								return SPIDER_IGNORE;
							}
						}
						//==========
						// General Cache Data
						//==========
						ch.setLatLon(latLon);
						pref.log("LatLon: " + ch.getLatLon());

						ch.getCacheDetails(false).setLongDescription(getLongDesc(completeWebPage));
						pref.log("Got description");

						ch.setCacheName(SafeXML.cleanback(getName(completeWebPage)));
						pref.log("Name: " + ch.getCacheName());

						String location = getLocation(completeWebPage);
						if (location.length() != 0) {
							int countryStart = location.indexOf(",");
							if (countryStart > -1) {
								ch.getCacheDetails(false).Country = SafeXML.cleanback(location.substring(countryStart + 1).trim());
								ch.getCacheDetails(false).State = SafeXML.cleanback(location.substring(0, countryStart).trim());
							} else {
								ch.getCacheDetails(false).Country = location.trim();
								ch.getCacheDetails(false).State = "";
							}
							pref.log("Got location (country/state)");
						} else {
							ch.getCacheDetails(false).Country = "";
							ch.getCacheDetails(false).State = "";
							pref.log("No location (country/state) found");
						}

						ch.setCacheOwner(SafeXML.cleanback(getOwner(completeWebPage)).trim());
						if(ch.getCacheOwner().equals(pref.myAlias) || (pref.myAlias2.length()>0 && ch.getCacheOwner().equals(pref.myAlias2))) ch.setOwned(true);
						pref.log("Owner: " + ch.getCacheOwner() +"; is_owned = "+ch.is_owned()+";  alias1,2 = ["+pref.myAlias+"|"+pref.myAlias2+"]");

						ch.setDateHidden(DateFormat.MDY2YMD(getDateHidden(completeWebPage)));
						pref.log("Hidden: " + ch.getDateHidden());

						ch.getCacheDetails(false).setHints(getHints(completeWebPage));
						pref.log("Hints: " + ch.getCacheDetails(false).Hints);

						ch.setCacheSize(CacheSize.gcSpiderString2Cw(getSize(completeWebPage)));
						pref.log("Size: " + ch.getCacheSize());

						ch.setHard(CacheTerrDiff.v1Converter(getDiff(completeWebPage)));
						pref.log("Hard: " + ch.getHard());

						ch.setTerrain(CacheTerrDiff.v1Converter(getTerr(completeWebPage)));
						pref.log("Terr: " + ch.getTerrain());

						ch.setType(getType(completeWebPage));
						pref.log("Type: " + ch.getType());
						//==========
						// Bugs
						//==========
						if (fetchTBs) getBugs(ch.getCacheDetails(false),completeWebPage);
						ch.setHas_bugs(ch.getCacheDetails(false).Travelbugs.size()>0);
						pref.log("Got TBs");
						//==========
						// Images
						//==========
						if(fetchImages){
							getImages(completeWebPage, ch.getCacheDetails(false));
							pref.log("Got images");
						}
						//==========
						// Addi waypoints
						//==========
						getAddWaypoints(completeWebPage, ch.getWayPoint(), ch.is_found());
						pref.log("Got additional waypoints");
						//==========
						// Attributes
						//==========
						getAttributes(completeWebPage, ch.getCacheDetails(false));
						pref.log("Got attributes");
						//==========
						// Last sync date
						//==========
						ch.setLastSync((new Time()).format("yyyyMMddHHmmss"));
						ch.setIncomplete(false);
						pref.log("ready " + ch.getWayPoint() + " : "+ ch.getLastSync());
						break;
					}catch(Exception ex){
						pref.log("Error reading cache: "+ch.getWayPoint());
						pref.log("Exception in getCacheByWaypointName: ",ex);
					}
				} else {
					break;
				}
			} // spiderTrys
			if ( ( spiderTrys >= MAX_SPIDER_TRYS ) && ( ret == SPIDER_OK ) ) {
				pref.log(">>> Failed to spider cache. Number of retrys exhausted.");
				int decision = (new MessageBox(MyLocale.getMsg(5500,"Error"),MyLocale.getMsg(5515,"Failed to load cache.%0aPleas check your internet connection.%0aRetry?"),FormBase.DEFOKB|FormBase.NOB|FormBase.CANCELB)).execute();
				if ( decision == FormBase.IDOK ) {
					continue; // retry even if failure
				} else if ( decision == FormBase.IDNO ){
					ret = SPIDER_ERROR;
				} else {
					ret = SPIDER_CANCEL;
				}
			}
			break;
		}//while(true) // retry even if failure
		if (infB.isClosed) {// If the infoBox was closed before getting here, we return -1
			return SPIDER_CANCEL;
		}
		return ret;
	} // getCacheByWaypointName


	/**
	 * Get the coordinates of the cache
	 * @param doc A previously fetched cachepage
	 * @return Cache coordinates
	 */
	private String getLatLon(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("latLonRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check latLonRex in spider.def\n"+doc);
			return "???";
		}
		return inRex.stringMatched(1);
	}

	boolean shortDescRex_not_yet_found=true;
	/**
	 * Get the long description
	 * @param doc A previously fetched cachepage
	 * @return the long description
	 */
	private String getLongDesc(String doc) throws Exception{
		String res = "";
		Regex shortDescRex = new Regex(p.getProp("shortDescRex"));
		Regex longDescRex = new Regex(p.getProp("longDescRex"));
		shortDescRex.search(doc);
		if (!shortDescRex.didMatch()) {
			if (shortDescRex_not_yet_found) pref.log("no shortDesc or check shortDescRex in spider.def\n"+doc);
		}
		else {
			res = shortDescRex.stringMatched(1);
			shortDescRex_not_yet_found=false;
		}
		res += "<br>";
		longDescRex.search(doc);
		if (!longDescRex.didMatch()) {
			pref.log("check longDescRex in spider.def\n"+doc);
		}
		else {
			res += longDescRex.stringMatched(1);
		}
		int spanEnd = res.lastIndexOf("</span>");
		if (spanEnd >= 0) {
			res = res.substring(0, spanEnd);
		}
		return SafeXML.cleanback(res); // since internal viewer doesn't show html-entities that are now in cacheDescription
	}

	/**
	 * Get the cache location (country and state)
	 * @param doc A previously fetched cachepage
	 * @return the location (country and state) of the cache
	 */
	private String getLocation(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("cacheLocationRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check cacheLocationRex in spider.def\n"+doc);
			return "";
		}
		return inRex.stringMatched(1);
	}

	/**
	 * Get the cache name
	 * @param doc A previously fetched cachepage
	 * @return the name of the cache
	 */
	private String getName(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("cacheNameRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check cacheNameRex in spider.def\n"+doc);
			return "???";
		}
		return inRex.stringMatched(1);
	}

	/**
	 * Get the cache owner
	 * @param doc A previously fetched cachepage
	 * @return the cache owner
	 */
	private String getOwner(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("cacheOwnerRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check cacheOwnerRex in spider.def\n"+doc);
			return "???";
		}
		return inRex.stringMatched(1);
	}

	/**
	 * Get the date when the cache was hidden
	 * @param doc A previously fetched cachepage
	 * @return Hidden date
	 */
	private String getDateHidden(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("dateHiddenRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check dateHiddenRex in spider.def\n"+doc);
			return "???";
		}
		return inRex.stringMatched(1);
	}

	/**
	 * Get the hints
	 * @param doc A previously fetched cachepage
	 * @return Cachehints
	 */
	private String getHints(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("hintsRex"));
		inRex.search(doc);
		if (!inRex.didMatch()) {
			pref.log("check hintsRex in spider.def\n"+doc);
			return "";
		}
		return inRex.stringMatched(1);
	}

	/**
	 * Get the cache size
	 * @param doc A previously fetched cachepage
	 * @return Cache size
	 */
	private String getSize(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("sizeRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else {
			pref.log("check sizeRex in spider.def\n"+doc);
			return "None";
		}
	}

	/**
	 * Get the Difficulty
	 * @param doc A previously fetched cachepage
	 * @return The cache difficulty
	 */
	private String getDiff(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("difficultyRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else {
			pref.log("check difficultyRex in spider.def\n"+doc);
			return "-1";
		}
	}

	/**
	 * Get the terrain rating
	 * @param doc A previously fetched cachepage
	 * @return Terrain rating
	 */
	private String getTerr(String doc) throws Exception{
		Regex inRex = new Regex(p.getProp("terrainRex"));
		inRex.search(doc);
		if(inRex.didMatch()) return inRex.stringMatched(1);
		else {
			pref.log("check terrainRex in spider.def\n"+doc);
			return "-1";
		}
	}

	/**
	 * Get the waypoint type
	 * @param doc A previously fetched cachepage
	 * @return the waypoint type (Tradi, Multi, etc.)
	 */
	private byte getType(String doc){
		RexCacheType.search(doc);
		if(RexCacheType.didMatch()) return CacheType.gcSpider2CwType(RexCacheType.stringMatched(1));
		else {
			pref.log("check cacheTypeRex in spider.def\n"+doc);
			return 0;
		}
	}

	/**
	 * Get the logs
	 * @param doc A previously fetched cachepage
	 * @param chD Cache Details
	 * @return A HTML string containing the logs
	 */
	private LogList getLogs(String completeWebPage, CacheHolderDetail chD) throws Exception {
		String icon = "";
		String name = "";
		String logText = "";
		String logId = "";
		String singleLog = "";
		LogList reslts = new LogList();
		RexLogBlock.search(completeWebPage);
		if (!RexLogBlock.didMatch()) {
			pref.log("check blockRex in spider.def\n"+completeWebPage);			
		}
		String LogBlock = RexLogBlock.stringMatched(1);
		exSingleLog.setSource(LogBlock);
		singleLog = exSingleLog.findNext();
		exIcon.setSource(singleLog);
		exNameTemp.setSource(singleLog);
		exName.setSource(exNameTemp.findNext());
		exDate.setSource(singleLog);
		exLog.setSource(singleLog);
		exLogId.setSource(singleLog);
		int nLogs=0;
		boolean foundown = false;
		while(!exSingleLog.endOfSearch()){
			//pref.log(singleLog);
			nLogs++;
			icon = exIcon.findNext();
			name = exName.findNext();
			logText = exLog.findNext();
			logId = exLogId.findNext();
			String d=DateFormat.logdate2YMD(exDate.findNext());
			//pref.log(Integer.toString(nLogs)+":"+icon+"|logger:"+name+"|id:"+logId+"|"+d);
			// if this log says this Cache is found by me
			if((icon.equals(icon_smile) || icon.equals(icon_camera) || icon.equals(icon_attended)) &&
				(name.equalsIgnoreCase(SafeXML.clean(pref.myAlias)) || 
				(pref.myAlias2.length()>0 && name.equalsIgnoreCase(SafeXML.clean(pref.myAlias2)))) )  {
				chD.getParent().setFound(true);
				chD.getParent().setCacheStatus(d);
				chD.OwnLogId = logId;
				chD.OwnLog = new Log(icon,d,name,logText);
				reslts.add(new Log(icon,d,name,logText));
				foundown=true;
				//pref.log("own log detected!");
			}
			if (nLogs<=pref.maxLogsToSpider) {reslts.add(new Log(icon,d,name,logText));}
			else {if (foundown){break;}}
			singleLog = exSingleLog.findNext();
			exIcon.setSource(singleLog);
			exNameTemp.setSource(singleLog);
			exName.setSource(exNameTemp.findNext());
			exDate.setSource(singleLog);
			exLog.setSource(singleLog);
			exLogId.setSource(singleLog);
		}
		if (nLogs>pref.maxLogsToSpider) {
			reslts.add(Log.maxLog());
			//pref.log("MAXLOGS reached ("+pref.maxLogsToSpider+")");
		} 
		//pref.log(nLogs+" checked!");
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
		while(!exBug.endOfSearch()){
			if (infB.isClosed) break; // Allow user to cancel by closing progress form
			linkPlusBug= exBug.findNext();
			int idx=linkPlusBug.indexOf(p.getProp("bugLinkEnd"));
			if (idx<0) break; // No link/bug pair found
			link=linkPlusBug.substring(0,idx);
			Extractor exBugName = new Extractor(linkPlusBug,p.getProp("bugNameExStart"),p.getProp("bugNameExEnd"),0,Extractor.EXCLUDESTARTEND);
			bug=exBugName.findNext();
			if(bug.length()>0) { // Found a bug, get its details
				Travelbug tb=new Travelbug(bug);
				try{
					infB.setInfo(oldInfoBox+MyLocale.getMsg(5514,"\nGetting bug: ")+SafeXML.cleanback(bug));
					pref.log("Fetching bug details: "+bug);
					bugDetails = fetch(link);
					Extractor exDetails = new Extractor(bugDetails,p.getProp("bugDetailsStart"),p.getProp("bugDetailsEnd"),0,Extractor.EXCLUDESTARTEND);
					tb.setMission(exDetails.findNext());
					Extractor exGuid = new Extractor(bugDetails,"details.aspx?guid=","\" id=\"aspnetForm",0,Extractor.EXCLUDESTARTEND); // TODO Replace with spider.def see also further down
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
		int spiderCounter = 0;
		String fileName, imgName, imgType, imgUrl, imgComment;
		Vector spideredUrls=new Vector(15);
		ImageInfo imageInfo=null;
		Extractor exImgBlock,exImgComment;
		int idxUrl; // Index of already spidered Url in list of spideredUrls
		CacheImages lastImages=null;

		// First: Get current image object of waypoint before spidering images.
		CacheHolder oldCh = Global.getProfile().cacheDB.get(chD.getParent().getWayPoint());
		if (oldCh != null) {
			lastImages = oldCh.getCacheDetails(false).images;
		}

		//========
		//In the long description
		//========
		String longDesc = "";
		try {
			if (chD.getParent().getWayPoint().startsWith("TC")) longDesc = doc;
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
				// Optimize: img.groundspeak.com -> img.geocaching.com (for better caching purposes)
				imgUrl = CacheImages.optimizeLink("http://" + imgUrl);
				try{
					imgType = (imgUrl.substring(imgUrl.lastIndexOf('.')).toLowerCase()+"    ").substring(0,4).trim();
					// imgType is now max 4 chars, starting with .
					if(imgType.startsWith(".png") || imgType.startsWith(".jpg") || imgType.startsWith(".gif")){
						// Check whether image was already spidered for this cache
						idxUrl=spideredUrls.find(imgUrl);
						imgName = chD.getParent().getWayPoint() + "_" + Convert.toString(imgCounter);
						imageInfo = null;
						if (idxUrl<0) { // New image
							fileName = chD.getParent().getWayPoint().toLowerCase() + "_" + Convert.toString(spiderCounter);
							if (lastImages != null) {
								imageInfo = lastImages.needsSpidering(imgUrl, fileName+imgType);
							}
							if (imageInfo == null) {
								imageInfo = new ImageInfo();
								pref.log("Loading image: " + imgUrl+" as "+fileName+imgType);
								spiderImage(imgUrl, fileName+imgType);
								imageInfo.setFilename(fileName+imgType);
								imageInfo.setURL(imgUrl);
							} else {
								pref.log("Already exising image: " + imgUrl+" as "+imageInfo.getFilename());
							}
							spideredUrls.add(imgUrl);
							spiderCounter++;
						} else { // Image already spidered as wayPoint_'idxUrl'
							fileName = chD.getParent().getWayPoint().toLowerCase() + "_" + Convert.toString(idxUrl);
							pref.log("Already loaded image: " + imgUrl+" as "+fileName+imgType);
							imageInfo = new ImageInfo();
							imageInfo.setFilename(fileName+imgType);
							imageInfo.setURL(imgUrl);
						}
						imageInfo.setTitle(imgName);
						imageInfo.setComment(null);
						imgCounter++;
						chD.images.add(imageInfo);
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
			exImgComment = new Extractor(tst,p.getProp("imgCommentExStart"),p.getProp("imgCommentExEnd"), 0, true);
		} catch (Exception ex) { // Missing property in spider .def
			return;
		}
		while(!exImgSrc.endOfSearch()){
			imgUrl = exImgSrc.findNext();
			imgComment = exImgComment.findNext();
			//Vm.debug("Img Url: " +imgUrl);
			if(imgUrl.length()>0){
				imgUrl = "http://" + imgUrl;
				try{
					imgType = (imgUrl.substring(imgUrl.lastIndexOf('.')).toLowerCase()+"    ").substring(0,4).trim();
					// imgType is now max 4 chars, starting with .
					if(imgType.startsWith(".png") || imgType.startsWith(".jpg") || imgType.startsWith(".gif")){
						// Check whether image was already spidered for this cache
						idxUrl=spideredUrls.find(imgUrl);
						imgName = chD.getParent().getWayPoint() + "_" + Convert.toString(imgCounter);
						imageInfo = null;
						if (idxUrl<0) { // New image
							fileName = chD.getParent().getWayPoint().toLowerCase() + "_" + Convert.toString(spiderCounter);
							if (lastImages != null) {
								imageInfo = lastImages.needsSpidering(imgUrl, fileName+imgType);
							}
							if (imageInfo == null) {
								imageInfo = new ImageInfo();
								pref.log("Loading image: " + imgUrl+" as "+fileName+imgType);
								spiderImage(imgUrl, fileName+imgType);
								imageInfo.setFilename(fileName+imgType);
								imageInfo.setURL(imgUrl);
							} else {
								pref.log("Already exising image: " + imgUrl+" as "+imageInfo.getFilename());
							}
							spideredUrls.add(imgUrl);
							spiderCounter++;
						} else { // Image already spidered as wayPoint_'idxUrl'
							fileName = chD.getParent().getWayPoint().toLowerCase() + "_" + Convert.toString(idxUrl);
							pref.log("Already loaded image: " + imgUrl+" as "+fileName+imgType);
							imageInfo = new ImageInfo();
							imageInfo.setFilename(fileName+imgType);
							imageInfo.setURL(imgUrl);
						}
						imageInfo.setTitle(exImgName.findNext());
						while (imgComment.startsWith("<br />")) imgComment=imgComment.substring(6);
						while (imgComment.endsWith("<br />")) imgComment=imgComment.substring(0,imgComment.length()-6);
						imageInfo.setComment(imgComment);
						chD.images.add(imageInfo);
					}
				} catch (IndexOutOfBoundsException e) {
					pref.log("IndexOutOfBoundsException in image span. imgURL:"+imgUrl,e);
				}
			}
		}
		//========
		//Final sweep to check for images in hrefs
		//========
		Extractor exFinal = new Extractor(longDesc, "http://", "\"", 0, true);
		while(!exFinal.endOfSearch()){
			imgUrl = exFinal.findNext();
			if(imgUrl.length()>0){
				// Optimize: img.groundspeak.com -> img.geocaching.com (for better caching purposes)
				imgUrl = CacheImages.optimizeLink("http://" + imgUrl);
				try{
					imgType = (imgUrl.substring(imgUrl.lastIndexOf('.')).toLowerCase()+"    ").substring(0,4).trim();
					// imgType is now max 4 chars, starting with . Delete characters in URL after the image extension
					imgUrl=imgUrl.substring(0,imgUrl.lastIndexOf('.')+imgType.length());
					if( imgType.startsWith(".jpg") || imgType.startsWith(".bmp") || imgType.startsWith(".png") || imgType.startsWith(".gif")){
						// Check whether image was already spidered for this cache
						idxUrl=spideredUrls.find(imgUrl);
						if (idxUrl<0) { // New image
							imgName = chD.getParent().getWayPoint() + "_" + Convert.toString(imgCounter);
							fileName = chD.getParent().getWayPoint().toLowerCase() + "_" + Convert.toString(spiderCounter);
							if (lastImages != null) {
								imageInfo = lastImages.needsSpidering(imgUrl, fileName+imgType);
							}
							if (imageInfo==null) {
								imageInfo = new ImageInfo();
								pref.log("Loading image: " + imgUrl+" as "+fileName+imgType);
								spiderImage(imgUrl, fileName+imgType);
								imageInfo.setFilename(fileName+imgType);
								imageInfo.setURL(imgUrl);
							} else {
								pref.log("Already exising image: " + imgUrl+" as "+imageInfo.getFilename());
							}
							spideredUrls.add(imgUrl);
							spiderCounter++;
							imageInfo.setTitle(imgName);
							imgCounter++;
							chD.images.add(imageInfo);
						}
					}
				} catch (IndexOutOfBoundsException e) {
					pref.log("Problem loading image. imgURL:"+imgUrl);
				}
			}
		}
	}


	/**
	 * Read an image from the server
	 * @param imgUrl The Url of the image
	 * @param target The bytes of the image
	 */
	private void spiderImage(String imgUrl, String target){ // TODO implement a fetch(URL, filename) in HttpConnection and use that one
		HttpConnection connImg;
		Socket sockImg;
		//InputStream is;
		FileOutputStream fos;
		//int bytes_read;
		//byte[] buffer = new byte[9000];
		ByteArray daten;
		String datei = "";
		datei = profile.dataDir + target;
		connImg = new HttpConnection(imgUrl);
		if (imgUrl.indexOf('%')>=0) connImg.documentIsEncoded=true;
		connImg.setRequestorProperty("Connection", "close");
		//connImg.setRequestorProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.12) Gecko/20080201 Firefox/2.0.0.12");
		//connImg.setRequestorProperty("Accept","text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		try{
			pref.log("Trying to fetch image from: " + imgUrl);
			String redirect=null;
			do {
				sockImg = connImg.connect();
				redirect=connImg.getRedirectTo();
				if (redirect!=null) {
					connImg=connImg.getRedirectedConnection(redirect);
					pref.log("Redirect to "+redirect);
				}
			} while(redirect!=null); // TODO this can end up in an endless loop if trying to load from a malicous site
			daten = connImg.readData(sockImg);
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
	boolean koords_not_yet_found = true; 
	private void getAddWaypoints(String doc, String wayPoint, boolean is_found) throws Exception{
		Extractor exWayBlock = new Extractor(doc,p.getProp("wayBlockExStart"),p.getProp("wayBlockExEnd"), 0, false);
		String wayBlock = "";
		String rowBlock = "";
		wayBlock = exWayBlock.findNext();
		Regex nameRex = new Regex(p.getProp("nameRex"));
		Regex koordRex = new Regex(p.getProp("koordRex"));
		Regex descRex = new Regex(p.getProp("descRex"));
		Regex typeRex = new Regex(p.getProp("typeRex"));
		int counter = 0;
		if(!exWayBlock.endOfSearch() && wayBlock.indexOf("No additional waypoints to display.")<0){
			Extractor exRowBlock = new Extractor(wayBlock,p.getProp("rowBlockExStart"),p.getProp("rowBlockExEnd"), 0, false);
			rowBlock = exRowBlock.findNext();
			rowBlock = exRowBlock.findNext();
			while(!exRowBlock.endOfSearch()){
				CacheHolder hd = null;

/*
				String[] AddiBlock=mString.split(rowBlock,'\n');
				int linePrefix=8;
				if(AddiBlock.length < linePrefix + 1) {
					(new MessageBox(MyLocale.getMsg(5500,"Error"), "GC changed table output \nCW must be changed too!", FormBase.OKB)).execute();
					break;
				}				
				String prefix=AddiBlock[linePrefix].trim();
*/

//				Extractor exPrefix=new Extractor(AddiBlock[linePrefix].trim(),p.getProp("prefixExStart"),p.getProp("prefixExEnd"),0,true);

				Extractor exPrefix=new Extractor(rowBlock,p.getProp("prefixExStart"),p.getProp("prefixExEnd"),0,true);
				String prefix=exPrefix.findNext();

				String adWayPoint;
				if (prefix.length() == 2)
					adWayPoint=prefix+wayPoint.substring(2);
				else
				    adWayPoint = MyLocale.formatLong(counter, "00") + wayPoint.substring(2);
				counter++;
				int idx=profile.getCacheIndex(adWayPoint);
				if (idx>=0) {
					// Creating new CacheHolder, but accessing old cache.xml file
					hd=new CacheHolder();
					hd.setWayPoint(adWayPoint);
					hd.getCacheDetails(true); // Accessing Details reads file if not yet done
				} else {
					hd=new CacheHolder();
					hd.setWayPoint(adWayPoint);
				}
				hd.initStates(idx<0);
				nameRex.search(rowBlock);
				if (nameRex.didMatch()) {
					hd.setCacheName(nameRex.stringMatched(1));
				}
				else {
					pref.log("check nameRex in spider.def\n"+rowBlock);			
				}
				koordRex.search(rowBlock);
				typeRex.search(rowBlock);
				if(koordRex.didMatch()) {
					hd.setLatLon(koordRex.stringMatched(1));
					koords_not_yet_found = false;
				}
				else {
					if (koords_not_yet_found) pref.log("check koordRex in spider.def\n"+rowBlock);			
				}
				if(typeRex.didMatch()) {
					hd.setType(CacheType.gpxType2CwType("Waypoint|"+typeRex.stringMatched(1)));
				}
				else {
					pref.log("check typeRex in spider.def\n"+rowBlock);			
				}
				rowBlock = exRowBlock.findNext();
				descRex.search(rowBlock);
				if (descRex.didMatch()) {
					hd.getCacheDetails(false).setLongDescription(descRex.stringMatched(1).trim());
				}
				else {
					pref.log("check descRex in spider.def\n"+rowBlock);			
				}
				hd.setFound(is_found);
				hd.setCacheSize(CacheSize.CW_SIZE_NOTCHOSEN);
				hd.setHard(CacheTerrDiff.CW_DT_UNSET);
				hd.setTerrain(CacheTerrDiff.CW_DT_UNSET);
				if (idx<0){
					cacheDB.add(hd);
					hd.save();
				}else {
					CacheHolder cx=cacheDB.get(idx);
					if (cx.is_Checked && // Only re-spider existing addi waypoints that are ticked
				 	   cx.isVisible()) { // and are visible (i.e.  not filtered)
					   cx.initStates(false);
					   cx.update(hd);
					   cx.is_Checked=true;
					   cx.save();
					}
				}
				rowBlock = exRowBlock.findNext();

			}
		}
	}

	public void getAttributes(String doc, CacheHolderDetail chD) throws Exception {
		Extractor attBlock = new Extractor(doc,p.getProp("attBlockExStart"),p.getProp("attBlockExEnd"), 0 , true);
		String atts = attBlock.findNext();
		Extractor attEx = new Extractor(atts,p.getProp("attExStart"),p.getProp("attExEnd"), 0 , true);
		String attribute=attEx.findNext();
		chD.attributes.clear();
		while (!attEx.endOfSearch()) {
			chD.attributes.add(attribute);
			attribute=attEx.findNext();
		}
		chD.getParent().setAttribsAsBits(chD.attributes.getAttribsAsBits());
	}


	/**
	*	Performs an initial fetch to a given address. In this case
	*	it will be a gc.com address. This method is used to obtain
	*	the result of a search for caches screen.
	*/
	public static String fetch(String address) {
		CharArray c_data;
		try{
			HttpConnection conn;
			if(pref.myproxy.length() > 0 && pref.proxyActive){
				pref.log("[fetch]:Using proxy: " + pref.myproxy + " / " +pref.myproxyport);
			}
			conn = new HttpConnection(address);
			conn.setRequestorProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
			if(cookieSession.length()>0){
				conn.setRequestorProperty("Cookie", "ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
				pref.log("[fetch]:Cookie Zeug: " + "Cookie: ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
			} else
				pref.log("[fetch]:No Cookie found");
			conn.setRequestorProperty("Connection", "close");
			conn.documentIsEncoded = true;
			if (pref.debug) pref.log("[fetch]:Connecting "+address);
			Socket sock = conn.connect();
			if (pref.debug) pref.log("[fetch]:Connect ok! "+address);
			// ByteArray daten = conn.readData(sock);
			JavaUtf8Codec codec = new JavaUtf8Codec();
			// c_data = codec.decodeText(daten.data, 0, daten.length, true, null);
			c_data = conn.readText(sock, codec);
			sock.close();
			if (pref.debug) pref.log("[fetch]:Read data ok "+address);
			return getResponseHeaders(conn)+ c_data.toString();
		}catch(IOException ioex){
			pref.log("IOException in fetch", ioex);
		}finally{
			//continue
		}
		return "";
	}

	/**
	*	After a fetch to gc.com the next fetches have to use the post method.
	*	This method does exactly that. Actually this method is generic in the sense
	*	that it can be used to post to a URL using http post.
	*/
	private static String fetch_post(String address, String document, String path) {
		HttpConnection conn;
		try {
			conn = new HttpConnection(address);
			JavaUtf8Codec codec = new JavaUtf8Codec();
			conn.documentIsEncoded = true;
			conn.setRequestorProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20041107 Firefox/1.0");
			conn.setPostData(codec.encodeText(document.toCharArray(),0,document.length(),true,null));
			conn.setRequestorProperty("Content-Type", "application/x-www-form-urlencoded");
			if(cookieSession.length()>0){
				conn.setRequestorProperty("Cookie", "ASP.NET_SessionId="+cookieSession+"; userid="+cookieID);
				pref.log("[fetch_post]:Cookie Zeug: " + "Cookie: ASP.NET_SessionId="+cookieSession +"; userid="+cookieID);
			} else {
				pref.log("[fetch_post]:No Cookie found");
			}
			conn.setRequestorProperty("Connection", "close");
			if (pref.debug) pref.log("[fetch_post]:Connecting "+address);
			Socket sock = conn.connect();
			if (pref.debug) pref.log("[fetch_post]:Connect ok! "+address);
			// ByteArray daten = conn.readData(sock);
			CharArray c_data = conn.readText(sock, codec);
			if (pref.debug) pref.log("[fetch_post]:Read data ok "+address);
			// CharArray c_data = codec.decodeText(daten.data, 0, daten.length, true, null);
			sock.close();
			return getResponseHeaders(conn)+c_data.toString();
		} catch (Exception e) {
			Global.getPref().log("Ignored Exception", e, true);
		}
		return "";
	}

	private static String getResponseHeaders(HttpConnection conn) {
		PropertyList pl = conn.documentProperties;
		if (pl != null) {
			StringBuffer sb = new StringBuffer(1000);
			boolean gotany = false;

			for (int i = 0; i < pl.size(); i++) {
				Property currProp = (Property)pl.get(i);
				if (currProp.value != null) {
					sb.append(currProp.name).append(": ").append(currProp.value).append("\r\n");
					gotany = true;
				}
			}
			if (gotany)
				return sb.toString() + "\r\n";
		}
		return "";
	}

	final static String hex = ewe.util.TextEncoder.hex;
	public String encodeUTF8URL(byte[] what) {
		int max = what.length;
		char [] dest = new char[6*max]; // Assume each char is a UTF char and encoded into 6 chars
		char d = 0;
		for (int i = 0; i<max; i++){
			char c = (char) what[i];
			if (c <= ' ' || c == '+' || c == '&' || c == '%' || c == '=' ||
				   c == '|' || c == '{' || c == '}' || c>0x7f ){
					dest[d++] = '%';
					dest[d++] = hex.charAt((c >> 4) & 0xf);
					dest[d++] = hex.charAt(c & 0xf);
			} else dest[d++] = c;
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
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6020,"Travelbug not found."), FormBase.OKB)).execute();
				return "";
			}
			if (bugList.indexOf(p.getProp("bugTotalRecords"))<0) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6021,"More than one travelbug found. Specify name more precisely."), FormBase.OKB)).execute();
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
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(6020,"Travelbug not found."), FormBase.OKB)).execute();
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

	/**
	 * Fetch a bug's mission and namefor a given tracking number
	 * @param TB the travelbug
	 * @return true if suceeded
	 */
	public boolean getBugMissionAndNameByTrackNr(Travelbug TB) {
		String bugDetails;
		String trackNr = TB.getTrackingNo();
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
				return false;
			}
			Extractor exDetails = new Extractor(bugDetails,p.getProp("bugDetailsStart"),p.getProp("bugDetailsEnd"),0,Extractor.EXCLUDESTARTEND);
			TB.setMission( exDetails.findNext() );
			Extractor exName = new Extractor(bugDetails,p.getProp("bugNameStart"),p.getProp("bugNameEnd"),0,Extractor.EXCLUDESTARTEND);
			TB.setName( exName.findNext() );
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public class SpiderProperties extends Properties {
		SpiderProperties() {
			super();
			try {
				load(new FileInputStream(FileBase.getProgramDirectory()+"/spider.def"));
			} catch (Exception ex) {
				pref.log("Failed to load spider.def",ex);
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5504,"Could not load 'spider.def'"), FormBase.OKB)).execute();
			}
		}

		/**
		 * Gets an entry in spider.def by its key (tag)
		 * @param key The key which is attributed to a specific entry
		 * @return The value for the key
		 * @throws Exception When a key is requested which doesn't exist
		 */
		public String getProp(String key) throws Exception {
			String s=super.getProperty(key);
			if (s == null) {
				(new MessageBox(MyLocale.getMsg(5500,"Error"), MyLocale.getMsg(5497,"Error missing tag in spider.def") + ": "+key, FormBase.OKB)).execute();
				throw new Exception("Missing tag in spider.def: "+key);
			}
			return s;
		}

	}
}
