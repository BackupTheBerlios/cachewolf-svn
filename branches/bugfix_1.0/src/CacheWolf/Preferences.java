package CacheWolf;
import utils.FileBugfix;
import ewe.io.*;
import ewe.sys.*;
import ewe.ui.*;
import ewesoft.xml.*;
import ewesoft.xml.sax.*;
import ewe.filechooser.*;
import ewe.util.*;
import ewe.util.Map.MapEntry;

/**
 *	A class to hold the preferences that were loaded upon start up of CacheWolf.
 *	This class is also capable of parsing the prefs.xml file as well as
 *	saving the current settings of preferences.
 */
public class Preferences extends MinML{

	public final int DEFAULT_MAX_LOGS_TO_SPIDER=250;
	public final int DEFAULT_LOGS_PER_PAGE=5;
	public final int DEFAULT_INITIAL_HINT_HEIGHT=10;

	//////////////////////////////////////////////////////////////////////////////////////
    // Constructor
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Singleton pattern - return reference to Preferences
	 * @return Singleton Preferences object
	 */
	public static Preferences getPrefObject() {
		if (_reference == null)
			// it's ok, we can call this constructor
			_reference = new Preferences();
		return _reference;
	}

	private static Preferences _reference;
	
	private String pathToConfigFile;
	
	/**
	 * Call this method to set the path of the config file <br>
	 * If you call it with null it defaults to [program-dir]/pref.xml
	 * if p is a directory "pref.xml" will automatically appended
	 * @param p
	 */
	public void setPathToConfigFile(String p) {
		String p_;
		if (p == null) {
			/*
			String test;
			test = Vm.getenv("APPDATA", "/"); // returns in java-vm on win xp: c:\<dokumente und Einstellungen>\<username>\<application data>
			log("Vm.getenv(APPDATA: " + test); // this works also in win32.exe (ewe-vm on win xp)
			test = Vm.getenv("HOME", "/"); // This should return on *nix system the home dir
			log("Vm.getenv(HOME: " + test);
			test = System.getProperty("user.dir"); // return in java-vm on win xp: <working dir> or maybe <program dir> 
			log("System.getProperty(user.dir: " + test); // in win32.exe -> null
			test = System.getProperty("user.home"); // in MS-java-VM env variable $HOME is ignored and always <windir>\java returned, see http://support.microsoft.com/kb/177181/en-us/
			log("System.getProperty(user.home: " + test); // in win32.exe -> null
			// "user.dir"              User's current working directory
			// "user.home"             User home directory (taken from http://scv.bu.edu/Doc/Java/tutorial/java/system/properties.html )
			 */
			p_ = FileBase.makePath(FileBase.getProgramDirectory(), "pref.xml");
		}
		else {
			if (new FileBugfix(p).isDirectory()) p_ = FileBase.makePath(p, "pref.xml");
			else p_ = p; 
		}
		pathToConfigFile = STRreplace.replace(p_, "//", "/"); // this is necessary in case that the root dir is the dir where the pref.xml is stored
		pathToConfigFile = pathToConfigFile.replace('\\', '/');
	}

	/**
	 * Constructor is private for a singleton object
	 */
	private Preferences(){
		mySPO.bits = 8;
		mySPO.parity = SerialPort.NOPARITY;
		mySPO.stopBits = 1;
		mySPO.baudRate = 4800;
		if ( ((ewe.fx.Rect) (Window.getGuiInfo(WindowConstants.INFO_SCREEN_RECT,null,new ewe.fx.Rect(),0))).height > 400) {
			if (Vm.getPlatform().equals("Unix"))
				fontSize = 12;
			else{
				// Default on VGA-PDAs: fontSize 21 + adjust ColWidth
				if (Vm.isMobile()){
					fontSize = 21;
					listColWidth="20,20,30,30,92,177,144,83,60,105,50,104,22,30,30";
				}
				else
					fontSize = 16;
			}
		} else 
			fontSize = 11;
	}

    //////////////////////////////////////////////////////////////////////////////////////
    // Public fields stored in pref.xml
	//////////////////////////////////////////////////////////////////////////////////////
	
	/** The base directory contains one subdirectory for each profile*/
	public String baseDir = "";  
	/** Name of last used profile */
	public String lastProfile=""; 
	/** If true, the last profile is reloaded automatically without a dialogue */
	public boolean autoReloadLastProfile=false; 
	/** This is the login alias for geocaching.com and opencaching.de */
	public String myAlias = "";
	/** Optional password */
	public String password="";
	/** This is an alternative alias used to identify found caches (i.e. if using multiple IDs) 
	 *  It is currently not used yet */
	public String myAlias2 = "";
	/** The path to the browser */
	public String browser = "";
	/** Name of proxy for spidering */
	public String myproxy = "";    
	/** Proxyport when spidering */
	public String myproxyport = "";
	/** Flag whether proxy is to be used */
	public boolean proxyActive=false;
	/** Serial port name and baudrate */
	public SerialPortOptions mySPO = new SerialPortOptions();
	/** True if the GPS data should be forwarded to an IP address */
	public boolean forwardGPS = false;
	/** IP address for forwarding GPS data */
	public String forwardGpsHost = "192.168.1.15";
	/** True if the GPS data should be logged to a file */
	public boolean logGPS = false;
	/** Timer for logging GPS data */
	public String logGPSTimer = "5";
	/** The default font size */
	public int fontSize = 11;
	// These settings govern where the menu and the tabs are displayed and whether the statusbas is shown
	/** True if the menu is to be displayed at the top of the screen */
	public boolean menuAtTop=true;
	/** True if the tabs are to be displayed at the top of the screen */
	public boolean tabsAtTop=true;
	/** True if the status bar is to be displayed (hidden if false) */
	public boolean showStatus=true;
	//public boolean noTabs=false;
	/** True if the application can be closed by clicking on the close button in the top line.
	 * This can be set to avoid accidental closing of the application */
	public boolean hasCloseButton=true;
	/** True if the SIP is always visible */
	public boolean fixSIP = false;
	/** The list of visible columns in the list view */
	public String listColMap="0,1,2,3,4,5,6,7,8,9,10,11,12";
	/** The widths for each column in list view */
	public String listColWidth="15,20,20,25,92,177,144,83,60,105,50,104,22,30,30";
	/** The columns which are to be displayed in TravelbugsJourneyScreen. See also TravelbugJourney */
	public String travelbugColMap="1,4,5,6,8,9,10,7";
	/** The column widths for the travelbug journeys. */
	public String travelbugColWidth="212,136,62,90,50,56,90,38,50,50,94,50";
	/** If this flag is true, only non-logged travelbug journeys will be shown */
	public boolean travelbugShowOnlyNonLogged=false;
	/** If this is true, deleted images are shown with a ? in the imagepanel */
	public boolean showDeletedImages=true; 
	/** This setting determines how many logs are shown per page of hintlogs (default 5) */
	public int logsPerPage=DEFAULT_LOGS_PER_PAGE;
	/** Initial height of hints field (set to 0 to hide them initially) */
	public int initialHintHeight=DEFAULT_INITIAL_HINT_HEIGHT; 
	/** Maximum logs to spider */ 
	public int maxLogsToSpider = DEFAULT_MAX_LOGS_TO_SPIDER;
	/** True if the Solver should ignore the case of variables */
	public boolean solverIgnoreCase=true;
	/** True if the solver expects arguments for trigonometric functions in degrees */
	public boolean solverDegMode=true;
	/** True if the description panel should show images */
	public boolean descShowImg=true;
	/** The type of connection which GPSBABEL uses: com1 OR usb. */
	public String garminConn="com1";  
	/** Additional options for GPSBabel, i.e. -s to synthethise short names */
	public String garminGPSBabelOptions=""; 
	/** Max. length for Garmin waypoint names (for etrex which can only accept 6 chars) */
	public int garminMaxLen=0;
	public boolean downloadPicsOC = true; //TODO Sollten die auch im Profil gespeichert werden mit Preferences als default Werte ?
	public boolean downloadMapsOC = true;
	public boolean downloadmissingOC = false;
	/** The currently used centre point, can be different from the profile's centrepoint. This is used
	 *  for spidering */
	public CWPoint curCentrePt=new CWPoint();
	/** True if a login screen is displayed on each spider operation */
	public boolean forceLogin=true;
	/** True if the goto panel is North centered */
	public boolean northCenteredGoto = true;
	/** If not null, a customs map path has been specified by the user */
	private String customMapsPath=null; 
	/** Number of CacheHolder details that are kept in memory */
	public int maxDetails=50;
	/** Number of details to delete when maxDetails have been stored in cachesWithLoadedDetails */
	public int deleteDetails=5;
	/** The locale code (DE, EN, ...) */
	public String language=""; 
	
	//////////////////////////////////////////////
	/** The debug switch (Can be used to activate dormant code) by adding
	 * the line: <pre><debug value="true"></pre>
	 * to the pref.xml file.
	 */
	public boolean debug = false;
	//////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////
    // Public fields not stored in pref.xml
	//////////////////////////////////////////////////////////////////////////////////////
	
	/** The height of the application */
	public int myAppHeight = 0;
	/** The width of the application */
	public int myAppWidth = 0;
	/** True if the preferences were changed and need to be saved */
	public boolean dirty = false;
	
    //////////////////////////////////////////////////////////////////////////////////////
    // Read pref.xml file
	//////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Method to open and parse the config file (pref.xml). Results are stored in the
	 * public variables of this class.
	 * If you want to specify a non default config file call setPathToConfigFile() first.
	 */
	public void readPrefFile(){
		if (pathToConfigFile == null) setPathToConfigFile(null); // this sets the default value 
		try{
			ewe.io.Reader r = new ewe.io.InputStreamReader(new ewe.io.FileInputStream(pathToConfigFile));
			parse(r);
			r.close();
		}catch(IOException e){
			log("IOException reading config file: " + pathToConfigFile, e, true);
			(new MessageBox(MyLocale.getMsg(327, "Information"), MyLocale.getMsg(176, "First start - using default preferences \n For experts only: \n Could not read preferences file:\n") + pathToConfigFile, MessageBox.OKB)).execute();
		}catch(Exception e){
			if (e instanceof NullPointerException)
				log("Error reading pref.xml: NullPointerException in Element "+lastName +". Wrong attribute?",e,true);
			else 
				log("Error reading pref.xml: ", e);
		}
	}

	/** Helper variables for XML parser */ 
	private StringBuffer collectElement=null; 
	private String lastName; // The string to the last XML that was processed

	/**
	 * Method that gets called when a new element has been identified in pref.xml
	 */
	public void startElement(String name, AttributeList atts){
		//Vm.debug("name = "+name);
		lastName=name;
		String tmp;
		if(name.equals("browser")) browser = atts.getValue("name");
		else if(name.equals("fixedsip")) {
			if(atts.getValue("state").equals("true")) {
				fixSIP = true;
			}
		}
		else if(name.equals("font")) fontSize = Convert.toInt(atts.getValue("size"));
		else if(name.equals("alias")) {
			myAlias = SafeXML.cleanback(atts.getValue("name"));
			tmp = SafeXML.cleanback(atts.getValue("password"));
			if (tmp != null) password=tmp;
			SpiderGC.passwort=password;
		}
		else if(name.equals("alias2")) SafeXML.cleanback(myAlias2 = atts.getValue("name"));
		else if(name.equals("location")){
			curCentrePt.set(atts.getValue("lat")+" "+atts.getValue("long"));
		}
		else if(name.equals("port")){
			mySPO.portName = atts.getValue("portname");
			mySPO.baudRate = Convert.toInt(atts.getValue("baud"));
		}
		else if(name.equals("portforward")) {
			forwardGPS = Convert.toBoolean(atts.getValue("active"));
			forwardGpsHost = atts.getValue("destinationHost");
		}
		else if(name.equals("portlog")) {
			logGPS = Convert.toBoolean(atts.getValue("active"));
			logGPSTimer = atts.getValue("logTimer");
		}
		else if (name.equals("lastprofile")) {
			collectElement=new StringBuffer(50);
			if (atts.getValue("autoreload").equals("true")) autoReloadLastProfile=true;
		}

		else if(name.equals("basedir")) {
			baseDir = atts.getValue("dir");
		}
		else if (name.equals("opencaching")) {
			downloadPicsOC = Boolean.valueOf(atts.getValue("downloadPics")).booleanValue();
			downloadMapsOC = Boolean.valueOf(atts.getValue("downloadMaps")).booleanValue();
			downloadmissingOC = Boolean.valueOf(atts.getValue("downloadmissing")).booleanValue();

		}
		else if (name.equals("listview")) {
			listColMap=atts.getValue("colmap");
			listColWidth=atts.getValue("colwidths")+",30,30"; // append default values for older versions	
			if((new StringTokenizer(listColWidth,",")).countTokens()<15) listColWidth+=",30,30"; // for older versions
		}
		else if(name.equals("proxy")) {
			myproxy = atts.getValue("prx");
			myproxyport = atts.getValue("prt");
			tmp = atts.getValue("active");
			if (tmp != null) proxyActive=Boolean.valueOf(tmp).booleanValue();
		}
		else if (name.equals("garmin")) {
			garminConn=atts.getValue("connection");
			tmp = atts.getValue("GPSBabelOptions");
			if (tmp != null) garminGPSBabelOptions=tmp;
			tmp = atts.getValue("MaxWaypointLength");
			if (tmp != null) garminMaxLen=Convert.toInt(atts.getValue("MaxWaypointLength"));
		}
		else if (name.equals("imagepanel")) {
			showDeletedImages = Boolean.valueOf(atts.getValue("showdeletedimages")).booleanValue();
		}
		else if (name.equals("descpanel")) {
			descShowImg = Boolean.valueOf(atts.getValue("showimages")).booleanValue();
		} 
		else if (name.equals("screen")) {
			menuAtTop=Boolean.valueOf(atts.getValue("menuattop")).booleanValue();
			tabsAtTop=Boolean.valueOf(atts.getValue("tabsattop")).booleanValue();
			showStatus=Boolean.valueOf(atts.getValue("showstatus")).booleanValue();
			if (atts.getValue("hasclosebutton")!=null)
				hasCloseButton=Boolean.valueOf(atts.getValue("hasclosebutton")).booleanValue();
		}
		else if (name.equals("hintlogpanel")) {
			logsPerPage = Convert.parseInt(atts.getValue("logsperpage"));
			String strInitialHintHeight=atts.getValue("initialhintheight");
			if (strInitialHintHeight!=null) initialHintHeight=Convert.parseInt(strInitialHintHeight);
			String strMaxLogsToSpider=atts.getValue("maxspiderlogs");
			if (strMaxLogsToSpider!=null) maxLogsToSpider=Convert.parseInt(strMaxLogsToSpider);
		}
		else if (name.equals("solver")) {
			solverIgnoreCase=Boolean.valueOf(atts.getValue("ignorevariablecase")).booleanValue();
			tmp = atts.getValue("degMode");
			if (tmp != null) solverDegMode=Boolean.valueOf(tmp).booleanValue();
		}
		else if (name.equals("mapspath")) {
			customMapsPath=atts.getValue("dir").replace('\\', '/');
		}
		else if (name.equals("debug")) debug=Boolean.valueOf(atts.getValue("value")).booleanValue();
		
		else if (name.equals("expPath")){
			exporterPaths.put(atts.getValue("key"),atts.getValue("value"));
		}
		else if (name.equals("travelbugs")) {
			travelbugColMap=atts.getValue("colmap");
			travelbugColWidth=atts.getValue("colwidths");	
			travelbugShowOnlyNonLogged=Boolean.valueOf(atts.getValue("shownonlogged")).booleanValue();
		}
		else if (name.equals("gotopanel")) {
			northCenteredGoto = Boolean.valueOf(atts.getValue("northcentered")).booleanValue();
		}
		else if (name.equals("spider")) {
			forceLogin = Boolean.valueOf(atts.getValue("forcelogin")).booleanValue();
		}
		else if (name.equals("details")) {
			maxDetails=Common.parseInt(atts.getValue("cacheSize"));
			deleteDetails=Common.parseInt(atts.getValue("delete"));
			if (maxDetails<2) maxDetails=2;
			if (deleteDetails<1) deleteDetails=1;
		}
		else if (name.equals("locale")) {
			language = atts.getValue("language");
		}
	}

	public void characters( char ch[], int start, int length ) {
		if (collectElement!=null) {
			collectElement.append(ch,start,length); // Collect the name of the last profile
		}
	}	

	/**
	 * Method that gets called when the end of an element has been identified in pref.xml
	 */
	public void endElement(String tag){
		if (tag.equals("lastprofile")) {
			if (collectElement!=null) lastProfile=collectElement.toString();
		}
		collectElement=null;
	}

    //////////////////////////////////////////////////////////////////////////////////////
    // Write pref.xml file
	//////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Method to save current preferences in the pref.xml file
	 */
	public void savePreferences(){
		if (pathToConfigFile == null) setPathToConfigFile(null); // this sets the default value 
		try{
			PrintWriter outp =  new PrintWriter(new BufferedWriter(new FileWriter(pathToConfigFile)));
			outp.print("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
			outp.print("<preferences>\n");
			outp.print("    <locale language=\"" + SafeXML.strxmlencode(language) + "\"/>\n");
			outp.print("	<basedir dir = \"" + SafeXML.strxmlencode(baseDir) + "\"/>\n");
			outp.print("    <lastprofile autoreload=\"" + SafeXML.strxmlencode(autoReloadLastProfile) + "\">" + SafeXML.strxmlencode(lastProfile) + "</lastprofile>\n"); //RB
			outp.print("	<alias name =\""+ SafeXML.clean(myAlias) +"\" password=\""+SafeXML.clean(password)+"\" />\n");
			outp.print("	<alias2 name =\""+ SafeXML.clean(myAlias2) +"\"/>\n");
			outp.print("	<browser name = \"" + SafeXML.strxmlencode(browser) + "\"/>\n");
			outp.print("	<proxy prx = \"" + SafeXML.strxmlencode(myproxy) + "\" prt = \"" + SafeXML.strxmlencode(myproxyport) + "\" active = \"" + SafeXML.strxmlencode(proxyActive) + "\" />\n");
			outp.print("	<port portname = \"" + SafeXML.strxmlencode(mySPO.portName) + "\" baud = \"" + SafeXML.strxmlencode(mySPO.baudRate) + "\"/>\n");
			outp.print("	<portforward active= \"" + SafeXML.strxmlencode(Convert.toString(forwardGPS)) + "\" destinationHost = \"" + SafeXML.strxmlencode(forwardGpsHost) + "\"/>\n");
			outp.print("	<portlog active= \"" + SafeXML.strxmlencode(Convert.toString(logGPS)) + "\" logTimer = \"" + SafeXML.strxmlencode(logGPSTimer) + "\"/>\n");
			outp.print("    <font size =\"" + SafeXML.strxmlencode(fontSize) + "\"/>\n");
			outp.print("    <screen menuattop=\"" + SafeXML.strxmlencode(menuAtTop) + "\" tabsattop=\"" + SafeXML.strxmlencode(tabsAtTop) + "\" showstatus=\"" + SafeXML.strxmlencode(showStatus) + "\" hasclosebutton=\"" + SafeXML.strxmlencode(hasCloseButton) + "\"/>\n");
			outp.print("    <fixedsip state = \"" + SafeXML.strxmlencode(fixSIP) + "\"/>\n");
			outp.print("    <listview colmap=\"" + SafeXML.strxmlencode(listColMap) + "\" colwidths=\"" + SafeXML.strxmlencode(listColWidth) + "\" />\n");
			outp.print("    <travelbugs colmap=\"" + SafeXML.strxmlencode(travelbugColMap) + "\" colwidths=\"" + SafeXML.strxmlencode(travelbugColWidth) + "\" shownonlogged=\"" + SafeXML.strxmlencode(travelbugShowOnlyNonLogged) + "\" />\n");
			outp.print("    <descpanel showimages=\"" + SafeXML.strxmlencode(descShowImg) + "\" />\n");
			outp.print("    <imagepanel showdeletedimages=\"" + SafeXML.strxmlencode(showDeletedImages) + "\"/>\n");
			outp.print("    <hintlogpanel logsperpage=\"" + SafeXML.strxmlencode(logsPerPage) + "\" initialhintheight=\"" + SafeXML.strxmlencode(initialHintHeight) + "\"  maxspiderlogs=\"" + SafeXML.strxmlencode(maxLogsToSpider) + "\" />\n");
			outp.print("    <solver ignorevariablecase=\"" + SafeXML.strxmlencode(solverIgnoreCase) + "\" degMode=\"" + SafeXML.strxmlencode(solverDegMode) + "\" />\n");
			outp.print("    <garmin connection = \"" + SafeXML.strxmlencode(garminConn) + "\" GPSBabelOptions = \"" + SafeXML.strxmlencode(garminGPSBabelOptions) + "\" MaxWaypointLength = \"" + SafeXML.strxmlencode(garminMaxLen) + "\" />\n");
			outp.print("    <opencaching downloadPicsOC=\"" + SafeXML.strxmlencode(downloadPicsOC) + "\" downloadMaps=\"" + SafeXML.strxmlencode(downloadMapsOC) + "\" downloadMissing=\"" + SafeXML.strxmlencode(downloadmissingOC) + "\"/>\n");
			outp.print("	<location lat = \"" + SafeXML.strxmlencode(curCentrePt.getLatDeg(CWPoint.DD)) + "\" long = \"" + SafeXML.strxmlencode(curCentrePt.getLonDeg(CWPoint.DD)) + "\"/>\n");
			outp.print("    <spider forcelogin=\"" + SafeXML.strxmlencode(forceLogin) + "\"/>\n");
			outp.print("    <gotopanel northcentered=\"" + SafeXML.strxmlencode(northCenteredGoto) + "\" />\n");
			outp.print("    <details cacheSize=\"" + SafeXML.strxmlencode(maxDetails) + "\" delete=\"" + SafeXML.strxmlencode(deleteDetails) + "\"/>\n");
			if (customMapsPath!=null) outp.print("	<mapspath dir = \"" + SafeXML.strxmlencode(customMapsPath.replace('\\','/')) + "\"/>\n");
			if (debug) outp.print("    <debug value=\"true\" />\n"); // Keep the debug switch if it is set
			// save last path of different exporters
			Iterator itPath = exporterPaths.entries();
			MapEntry entry;
			while(itPath.hasNext()){
				entry = (MapEntry) itPath.next();
				outp.print("    <expPath key = \"" + SafeXML.strxmlencode(entry.getKey().toString()) + "\" value = \"" + SafeXML.strxmlencode(entry.getValue().toString().replace('\\', '/')) + "\"/>\n");
			}
			outp.print("</preferences>");
			outp.close();
		} catch (Exception e) {
			log("Problem saving: " +pathToConfigFile,e,true);
		}
	}

    //////////////////////////////////////////////////////////////////////////////////////
    // Maps
	//////////////////////////////////////////////////////////////////////////////////////

	private static final String mapsPath = "maps/standard";
	
	/**
	 * custom = set by the user
	 * @return custom Maps Path, null if not set
	 */
	public String getCustomMapsPath() {
	   return customMapsPath;	
	}
	
	public void saveCustomMapsPath(String mapspath_) {
		if (customMapsPath == null || !customMapsPath.equals(mapspath_)) {
			customMapsPath=new String(mapspath_).replace('\\', '/');
			savePreferences();
		}
	}
	
	/**
	 * gets the path to the calibrated maps
	 * it first tries if there are manually imported maps
	 * in <baseDir>/maps/standard then it tries 
	 * the legacy dir: <program-dir>/maps
	 * In case in both locations are no .wfl-files
	 * it returns  <baseDir>/maps/expedia - the place where
	 * the automatically downloaded maps are placed.
	 * 
	 * 
	 */
	public String getMapLoadPath() {
		saveCustomMapsPath(getMapLoadPathInternal());
		return getCustomMapsPath();
	}
	private String getMapLoadPathInternal() {
		// here could also a list of map-types displayed...
		// standard dir
		String ret = getCustomMapsPath(); 
		if (ret != null) return ret; 
		ret = getMapManuallySavePath(false);
		File t = new FileBugfix(ret);
		String[] f = t.list("*.wfl", FileBase.LIST_FILES_ONLY);
		if (f != null && f.length > 0) return  baseDir + mapsPath;
		f = t.list("*.wfl", FileBase.LIST_DIRECTORIES_ONLY | FileBase.LIST_ALWAYS_INCLUDE_DIRECTORIES);
		if (f != null && f.length > 0) { // see if in a subdir of <baseDir>/maps/standard are .wfl files
			String[] f2;
			for (int i = 0; i< f.length; i++) {
				t.set(null, ret+"/"+f[i]);
				f2 = t.list("*.wfl", FileBase.LIST_FILES_ONLY);
				if (f2 != null && f2.length > 0) return  ret;
			}
		}
		// lagacy dir 
		ret = FileBase.getProgramDirectory() + "/maps";
		t.set(null, ret);
		f = t.list("*.wfl", FileBase.LIST_FILES_ONLY);
		if (f != null && f.length > 0) {
			MessageBox inf = new MessageBox("Information", "The directory for calibrated maps \nhas moved in this program version\n to '<profiles directory>/maps/standard'\n Do you want to move your calibrated maps there now?", FormBase.YESB | FormBase.NOB);
			if (inf.execute() == FormBase.IDYES) {
				String sp = getMapManuallySavePath(false);
				FileBugfix spF = new FileBugfix(sp);
				if (!spF.exists()) spF.mkdirs();
				String image;
				String lagacypath = ret;
				for (int i=0; i<f.length; i++) {
					t.set(null, lagacypath+f[i]);
					spF.set(null, sp+"/"+f[i]);
					t.move(spF);
					image = Common.getImageName(lagacypath+f[i].substring(0, f[i].lastIndexOf(".")));
					t.set(null, image);
					spF.set(null, sp+"/"+t.getFileExt());
					t.move(spF);
				}
				t.set(null, lagacypath);
				t.delete();
				return sp;
			}
			else return  ret;
		}
		// expedia dir
		// return getMapExpediaLoadPath();
		
		//whole maps directory
		return Global.getPref().baseDir.replace('\\', '/') + "maps";
	}

	/**
	 * @param create if true the directory if it doesn't exist will be created
	 * @return the path where manually imported maps should be stored
	 * this should be adjustable in preferences...
	 */
	public String getMapManuallySavePath(boolean create) {
		String mapsDir = baseDir + mapsPath;
		if (create && !(new FileBugfix(mapsDir).isDirectory())) { // dir exists? 
			if (new FileBugfix(mapsDir).mkdirs() == false) {// dir creation failed?
				(new MessageBox(MyLocale.getMsg(321,"Error"), MyLocale.getMsg(172,"Error: cannot create maps directory: \n")+mapsDir, FormBase.OKB)).exec();
				return null;
			}
		}
		return mapsDir;
	}

	/**
	 * to this path the automatically downloaded maps are saved
	 */
	public String getMapDownloadSavePath(String mapkind) {
		String subdir = Global.getProfile().dataDir.substring(Global.getPref().baseDir.length()).replace('\\', '/');
		String mapsDir = Global.getPref().baseDir + "maps/" + Common.ClearForFileName(mapkind)+ "/" + subdir;
		if (!(new FileBugfix(mapsDir).isDirectory())) { // dir exists? 
			if (new FileBugfix(mapsDir).mkdirs() == false) // dir creation failed?
			{(new MessageBox(MyLocale.getMsg(321,"Error"), MyLocale.getMsg(172,"Error: cannot create maps directory: \n")+new FileBugfix(mapsDir).getParentFile(), FormBase.OKB)).exec();
			return null;
			}
		}
		return mapsDir;
	}

	public String getMapExpediaLoadPath() {
		return Global.getPref().baseDir.replace('\\', '/') + "maps/expedia"; // baseDir has trailing /
	}
	
    //////////////////////////////////////////////////////////////////////////////////////
    // Profile Selector
	//////////////////////////////////////////////////////////////////////////////////////
	
	static protected final int PROFILE_SELECTOR_FORCED_ON=0;
	static protected final int PROFILE_SELECTOR_FORCED_OFF=1;
	static protected final int PROFILE_SELECTOR_ONOROFF=2;

	/**
	 * Open Profile selector screen 
	 * @param prof
	 * @param showProfileSelector
	 * @return True if a profile was selected
	 */
	public boolean selectProfile(Profile prof, int showProfileSelector, boolean hasNewButton) {
		// If datadir is empty, ask for one
		if (baseDir.length()==0 || !(new FileBugfix(baseDir)).exists()) {
			do {
				FileChooser fc = new FileChooser(FileChooserBase.DIRECTORY_SELECT,"/");
				fc.title = MyLocale.getMsg(170,"Select base directory for cache data");
				// If no base directory given, terminate
				if (fc.execute() == FormBase.IDCANCEL) ewe.sys.Vm.exit(0);
				baseDir = fc.getChosenFile().toString();
			}while (!(new FileBugfix(baseDir)).exists());
		}
		baseDir=baseDir.replace('\\','/');
		if (!baseDir.endsWith("/")) baseDir+="/";
		boolean profileExists=true;  // Assume that the profile exists
		do {	
			if(!profileExists || (showProfileSelector==PROFILE_SELECTOR_FORCED_ON) || 
					(showProfileSelector==PROFILE_SELECTOR_ONOROFF && !autoReloadLastProfile)){ // Ask for the profile
				ProfilesForm f = new ProfilesForm(baseDir,lastProfile,!profileExists || hasNewButton);
				int code = f.execute();
				// If no profile chosen (includes a new one), terminate
				if (code==-1) return false; // Cancel pressed
				CWPoint savecenter = new CWPoint(prof.centre);
				prof.clearProfile();
				prof.centre = savecenter;
				prof.hasUnsavedChanges = true;
				//curCentrePt.set(0,0); // No centre yet
				lastProfile=f.newSelectedProfile;
			}
			profileExists=(new FileBugfix(baseDir+lastProfile)).exists();
			if (!profileExists) (new MessageBox(MyLocale.getMsg(144,"Warning"),
					           MyLocale.getMsg(171,"Profile does not exist: ")+lastProfile,FormBase.MBOK)).execute();
		} while (profileExists==false);
		// Now we are sure that baseDir exists and basDir+profile exists
		prof.name=lastProfile;
		prof.dataDir=baseDir+lastProfile;
		prof.dataDir=prof.dataDir.replace('\\','/');
		if (!prof.dataDir.endsWith("/")) prof.dataDir+='/';
		savePreferences();
		return true;
	}

    //////////////////////////////////////////////////////////////////////////////////////
    // Log functions
	//////////////////////////////////////////////////////////////////////////////////////

	/** Log file is in program directory and called log.txt */
	private final String LOGFILENAME=FileBase.getProgramDirectory()+"/log.txt";
	
	/**
	 * Method to delete an existing log file. Called on every SpiderGC.
	 * The log file is also cleared when Preferences is created and the filesize > 60KB
	 */
	public void logInit(){
		File logFile = new FileBugfix(LOGFILENAME);
		logFile.delete();
		log("CW Version "+Version.getReleaseDetailed());
	}
	
	/**
	 * Method to log messages to a file called log.txt
	 * It will always append to an existing file.
	 * To show the message on the console, the global variable debug must be set.
	 * This can be done by adding
	 * <pre><debug value="true"></pre>
	 * to the pref.xml file
	 * @param text to log
	 */
	public void log(String text){
		Time dtm = new Time();
		dtm.getTime();
		dtm.setFormat("dd.MM.yyyy'/'HH:mm");
		text = dtm.toString()+ ": "+ text;
		if (debug) Vm.debug(text);
		text=text+"\n";
		FileWriter logFile = null;
		try{
			logFile = new FileWriter(LOGFILENAME, true);
			//Stream strout = null;
			//strout = logFile.toWritableStream(true);
			logFile.println(text);
			//Vm.debug(text); Not needed - put <debug value="true"> into pref.xml
		}catch(Exception ex){
			Vm.debug("Error writing to log file!");
		}finally{
			if (logFile != null) try {logFile.close(); } catch (IOException ioe) {}
		}
	}

	/** Log an exception to the log file with or without a stack trace
	 * 
	 * @param text Optional message (Can be empty string)
	 * @param e The exception
	 * @param withStackTrace If true and the debug switch is true, the stack trace is appended to the log
	 * The debug switch can be set by including the line <i>&lt;debug value="true"&gt;&lt;/debug&gt;</i> in the pref.xml file
	 * or by manually setting it (i.e. in BE versions or RC versions) by including the line
	 * <pre>Global.getPref().debug=true;</pre>
	 * in Version.getRelease()
	 */
	public void log(String text,Throwable e, boolean withStackTrace) {
		String msg;
		if (text.equals("")) msg=text; else msg=text+"\n";
		if (e!=null) {
			if (withStackTrace && debug) 
				msg+=ewe.sys.Vm.getAStackTrace(e);
			else
				msg+=e.toString();
		}
		log(msg);
	}
	
	/** Log an exception to the log file without a stack trace, i.e.
	 * where a stack trace is not needed because the location/cause of the error is clear 
	 * 
	 * @param message Optional message (Can be empty string)
	 * @param e The exception
	 */
	public void log(String message,Exception e) {
		log (message,e,false);
	}
		
    //////////////////////////////////////////////////////////////////////////////////////
    // Exporter path functions
	//////////////////////////////////////////////////////////////////////////////////////

	/** Hashtable for storing the last export path */
	private Hashtable exporterPaths = new Hashtable();

	public void setExportPath(String exporter,String path){
		exporterPaths.put(exporter, path);
		savePreferences();
	}

	public void setExportPathFromFileName(String exporter,String filename){
		File tmpfile = new FileBugfix (filename);
		exporterPaths.put(exporter, tmpfile.getPath());
		savePreferences();
	}

	public String getExportPath(String exporter){
		String dir = (String) exporterPaths.get(exporter);
		if (dir == null){
			dir = Global.getProfile().dataDir;
		}
		return dir;
	}

}
