package CacheWolf;

import CacheWolf.navi.GotoPanel;
import CacheWolf.navi.MapImage;
import CacheWolf.navi.MovingMap;
import CacheWolf.navi.Navigate;
import ewe.sys.Vm;
import ewe.ui.*;
import ewe.fx.*;

/**
 *	This class creates the tabbed panel and sets the tabs to the respective
 *	other panels. Important is to have a look at the event handler!<br>
 *	Class ID = 1200
 *	@see MainForm
 *	@see MainMenu
 */
public class MainTab extends mTabbedPanel {
	DescriptionPanel descP= new DescriptionPanel();
	HintLogPanel hintLP = new HintLogPanel();
	TablePanel tbP;
	CacheDB cacheDB;
	public DetailsPanel detP = new DetailsPanel();
	CalcPanel calcP;
	Preferences pref;
	Profile profile;
	GotoPanel gotoP; 
	RadarPanel radarP = new RadarPanel();
	ImagePanel imageP;
	SolverPanel solverP;
	String lastselected = "";
	public CacheHolder ch=null, chMain=null;
	CacheHolderDetail chD =null;
	MainMenu mnuMain;
	StatusBar statBar;
	public MovingMap mm;
	Navigate nav;
	public String mainCache="";
	int oldCard=0;
	boolean cacheDirty=false;
	
	public MainTab(MainMenu mainMenu,StatusBar statBar){
		Global.mainTab=this;
		mnuMain=mainMenu;
		pref = Global.getPref();
		profile=Global.getProfile();
		if (!pref.tabsAtTop) tabLocation=SOUTH;
		cacheDB = profile.cacheDB;
		this.statBar=statBar;
		MyLocale.setSIPButton();
		//Don't expand tabs if the screen is very narrow, i.e. HP IPAQ 65xx, 69xx
		int sw = MyLocale.getScreenWidth();
		if ( sw <= 240) this.dontExpandTabs=true;
		String imagesize="";
		if (Vm.isMobile() && sw >= 400) imagesize="_vga";  
		calcP = new CalcPanel(); // Init here so that Global.MainT is already set
		tbP = new TablePanel(pref, profile, statBar);
		Card c = this.addCard(new TableForm(tbP), MyLocale.getMsg(1200,"List"), null);
		
		c = this.addCard(detP, MyLocale.getMsg(1201,"Details"), null);
		c.iconize(new Image("details"+imagesize+".gif"),true);

		c = this.addCard(descP, MyLocale.getMsg(1202,"Description"), null);
		c.iconize(new Image("descr"+imagesize+".gif"),true);

		c = this.addCard(new MyScrollBarPanel(imageP = new ImagePanel()), MyLocale.getMsg(1203,"Images"), null);
		c.iconize(new Image("images"+imagesize+".gif"),true);

		c = this.addCard(hintLP, MyLocale.getMsg(1204,"Hints & Logs"), null);
		c.iconize(new Image("more"+imagesize+".gif"),true);

		c = this.addCard(solverP = new SolverPanel(pref, profile), MyLocale.getMsg(1205,"Solver"), null);
		c.iconize(new Image("solver"+imagesize+".gif"),true);

		c = this.addCard(calcP, MyLocale.getMsg(1206,"Calc"), null);
		mImage imgCalc=new mImage("projecttab"+imagesize+".gif"); imgCalc.transparentColor=new Color(0,255,0);
		c.iconize(imgCalc,true);

		nav = new Navigate();
		c = this.addCard(gotoP = new GotoPanel(nav), "Goto", null);
		c.iconize(new Image("goto"+imagesize+".gif"),true);
		nav.setGotoPanel(gotoP);

		c = this.addCard(radarP, "Radar", null);
		radarP.setMainTab(this);
		c.iconize(new Image("radar"+imagesize+".gif"),true);
		mnuMain.allowProfileChange(true);
//		if (pref.noTabs) top.modify(ShrinkToNothing,0);//TODO
	}

	public TablePanel getTablePanel(){
		return tbP;
	}

	public void selectAndActive(int rownum){// Called from myInteractivePanel.imageClicked
		tbP.selectRow(rownum);
		this.selectAndExpand(0);
	}

	public void clearDetails() {
		imageP.clearImages(); // Remove all images
		descP.clear(); // write "loading ..."
		detP.clearAttributes(); // Clear only the attributes
		hintLP.clear(); // Remove the logs
		solverP.setInstructions("loading ...");
	}	
	
	public void onEvent(Event ev) {
		// This section clears old data when a new line is selected in the table
		if (ev instanceof TableEvent) {
			clearDetails();
		}
		if(ev instanceof MultiPanelEvent){
			// Check whether a profile change is allowed, if not disable the relevant options
			checkProfileChange();
			// Perform clean up actions for the panel we are leaving
			onLeavingPanel(oldCard);
			// Prepare actions for the panel we are about to enter
			onEnteringPanel(((MultiPanelEvent)ev).selectedIndex);
			oldCard=((MultiPanelEvent)ev).selectedIndex;
		}
		super.onEvent(ev); //Make sure you call this.
		// If we are in Listview update status
//		if (this.getSelectedItem()==0 && statBar!=null) statBar.updateDisplay();
	}

	/**
	 * Code to execute when leaving a panel (oldCard is the panel number)
	 *
	 */
	private void onLeavingPanel(int panelNo) {//Vm.debug("Leaving "+panelNo);
		if (panelNo==0) { // Leaving the list view
			// Get the cache for the current line (ch)
			// Get the details for the current line (chD)
			// If it is Addi get details of main Wpt (chMain)
			chMain=null;
			cacheDirty=false;
			if (tbP.getSelectedCache()>=Global.mainTab.tbP.myMod.numRows || tbP.getSelectedCache()<0) {
				ch=null;
				chD=null; 
				lastselected="";
			} else {
				ch = cacheDB.get(tbP.getSelectedCache());
				lastselected=ch.getWayPoint();  // Used in Parser.Skeleton
				chD = ch.getCacheDetails(true);
			}
		}
		if (panelNo==1) { // Leaving the Details Panel
			// Update chD with Details
			if(detP.isDirty()) {
				cacheDirty=true;
				boolean needTableUpdate = detP.needsTableUpdate();
				detP.saveDirtyWaypoint();
				if (needTableUpdate) {
					tbP.myMod.updateRows();// This sorts the waypoint (if it is new) into the right position
					tbP.selectRow(profile.getCacheIndex(detP.getDisplayedWaypoint()));
				}
				//was tbP.refreshTable();
				tbP.tc.update(true); // Update and repaint
				if (statBar!=null) statBar.updateDisplay();
			}
		}
		if (panelNo==5) { // Leaving the Solver Panel
			// Update chD or chMain with Solver
			// If chMain is set (i.e. if it is an addi Wpt) save it immediately
			if (chD!=null && solverP.isDirty()) {
				cacheDirty=true;
				if (chMain==null) {
					boolean oldHasSolver = chD.getParent().hasSolver();
					chD.setSolver(solverP.getInstructions());
					if (oldHasSolver != chD.getParent().hasSolver()) tbP.tc.update(true);
				} else {
					boolean oldHasSolver = chMain.hasSolver();
					chMain.getExistingDetails().setSolver(solverP.getInstructions());
					if (oldHasSolver != chMain.hasSolver()) tbP.tc.update(true);
					chMain.save();//Vm.debug("mainT:SaveCache "+chMain.wayPoint+"/S:"+chMain.Solver);
					chMain=null;
				}
			}
		}
	}

	/**
	 * Code to execute when entering a panel (getSelectedItem() is the panel number)
	 *
	 */
	private void onEnteringPanel(int panelNo) {//Vm.debug("Entering "+panelNo);
		switch (panelNo) {// Switch by panel number
		case 0:
			MyLocale.setSIPOff();
			// If Solver or Details has changed, save Cache
			updatePendingChanges();
			if (detP.hasBlackStatusChanged()) {
				tbP.refreshTable();
			}
			break;
		case 1:  // DetailsPanel
			if (chD==null) { // Empty DB - show a dummy detail
				newWaypoint(ch=new CacheHolder()); 
			}
			MyLocale.setSIPOff();
			detP.setDetails(ch);
			break;
		case 2: // Description Panel
			MyLocale.setSIPOff();
			descP.setText(ch);
			break;
		case 3: // Picture Panel
			MyLocale.setSIPOff();
			if (ch.isAddiWpt()) { 
				imageP.setImages(ch.mainCache.getCacheDetails(true));
			} else {
				imageP.setImages(chD);
			}
			break;
		case 4:  // Log Hint Panel
			MyLocale.setSIPOff();
			if (ch.isAddiWpt()) { 
				hintLP.setText(ch.mainCache.getCacheDetails(true));
			} else {
				hintLP.setText(chD);
			}
			break;
		case 5:  // Solver Panel
			MyLocale.setSIPOff();
			if (ch.isAddiWpt()) { 
				chMain=ch.mainCache;
				solverP.setInstructions(ch.mainCache);
			} else {
				solverP.setInstructions(ch);
			}
			break;
		case 6:  // CalcPanel
			MyLocale.setSIPOff();
			calcP.setFields(ch);
			break;
		case 7: // GotoPanel
			MyLocale.setSIPOff();
			break;
		case 8:  // Cache Radar Panel
			MyLocale.setSIPOff();
			radarP.setParam(pref, cacheDB, ch.getWayPoint());
			radarP.drawThePanel();
			break;
		}
	}
	
	/** Update the distances of all caches to the centre and display a message 
	 */
	public void updateBearDist(){// Called from DetailsPanel, GotoPanel and myTableControl
		MessageBox info = new MessageBox(MyLocale.getMsg(327,"Information"), MyLocale.getMsg(1024,"Entfernungen in der Listenansicht \n werden neu berechnet...").replace('~','\n'), 0);
		info.exec();
		info.waitUntilPainted(200);
		tbP.pref = pref;
		profile.updateBearingDistance();
		//tbP.refreshTable();
		info.close(0);
		tbP.tc.repaint();
	}

	public void openDescriptionPanel(CacheHolder chi) {
		MyLocale.setSIPOff();
		// To change cache we need to be in panel 0
		onLeavingPanel(oldCard);
		onEnteringPanel(0); oldCard=0;
		int row = profile.getCacheIndex(chi.getWayPoint());
		tbP.selectRow(row);
		//tbP.tc.scrollToVisible(row, 0);
		//tbP.selectRow(row);
		select(descP);
		//descP.setText(chi);
	}


	/**
	 * this is called from goto / MovingMap / CalcPanel / DetailsPanel and so on to 
	 * offer the user the possibility of entering an new waypoint
	 * at a given position. pCh must already been preset with a valid
	 * CacheHolder object
	 * 
	 * @param pCh
	 */
	public void newWaypoint(CacheHolder pCh){
		//When creating a new waypoint, simulate a change to the list view
		//if we are currently NOT in the list view
		if (oldCard != 0)
		{
			onLeavingPanel(oldCard);
		}
		updatePendingChanges(); // was: onEnteringPanel(0); oldCard=0;
		
		mainCache=lastselected;
		int selectedIndex = profile.getCacheIndex( lastselected );
		if (selectedIndex >= 0) {
			CacheHolder selectedCache = profile.cacheDB.get( selectedIndex );
			if ( selectedCache.isAddiWpt() ) {
				mainCache = selectedCache.mainCache.getWayPoint();
			}			
		}
		if (CacheType.isAddiWpt(pCh.getType()) && mainCache!=null && mainCache.length()>2) {
			pCh.setWayPoint(profile.getNewAddiWayPointName(mainCache));
			profile.setAddiRef(pCh);
		} else { 
			pCh.setWayPoint(profile.getNewWayPointName());
			pCh.setType(CacheType.CW_TYPE_CUSTOM);
			pCh.setHard(CacheTerrDiff.CW_DT_UNSET);
			pCh.setTerrain(CacheTerrDiff.CW_DT_UNSET);
			pCh.setCacheSize(CacheSize.CW_SIZE_NOTCHOSEN);
			lastselected=pCh.getWayPoint();
		}
		pCh.setCacheSize(CacheSize.CW_SIZE_NOTCHOSEN);
		chD = pCh.getCacheDetails(true);
		this.ch = pCh;
		cacheDB.add(pCh);
		Global.getProfile().notifyUnsavedChanges(true); // Just to be sure 
		tbP.myMod.numRows++;
		detP.setDetails(pCh);
		oldCard=1;
		if (this.cardPanel.selectedItem != 1) select(detP);
		solverP.setInstructions(pCh);
		detP.setNeedsTableUpdate(true);
		//tbP.refreshTable(); // moved this instruction to onLeavingPanel

	}

	
	/**
	 * sets posCircle Lat/Lon to centerTo
	 * 
	 * @param centerTo true: centers centerTo on the screen and disconnects MovingMap from GPS if Gps-pos is not on the loaded map
	 * @param forceCenter
	 */
	public void SwitchToMovingMap(CWPoint centerTo, boolean forceCenter) {
		try {
			if (!centerTo.isValid()) {
				(new MessageBox("Error", "No valid coordinates", FormBase.OKB)).execute();
				return;
			}
			if (mm == null) {
				mm = new MovingMap(nav, profile.cacheDB);
				nav.setMovingMap(mm);
			} 
			if (forceCenter) mm.setGpsStatus(MovingMap.noGPS); // disconnect movingMap from GPS TODO only if GPS-pos is not on the screen
			mm.updatePosition(centerTo);
			mm.myExec();
			if (forceCenter) {
				try {
					int i = 0;
					while (MapImage.screenDim.width == 0 && i < 10*60) { i++; ewe.sys.mThread.sleep(100);} // wait until the window size of the moving map is known note: ewe.sys.sleep() will pause the whole vm - no other thread will run
					if (i >= 10*60) {(new MessageBox("Error", "MovingMap cannot be displayed - this is most likely a bug - plaese report it on www.geoclub.de", FormBase.OKB)).execute(); return;}
					mm.setCenterOfScreen(centerTo, false); // this can only be executed if mm knows its window size that's why myExec must be executed before
					mm.updatePosition(centerTo);
					/*			if(!mm.posCircle.isOnScreen()) { // TODO this doesn't work because lat lon is set to the wished pos and not to gps anymore
				mm.setGpsStatus(MovingMap.noGPS); // disconnect movingMap from GPS if GPS-pos is not on the screen
				mm.setResModus(MovingMap.HIGHEST_RESOLUTION);
				mm.updatePosition(centerTo.latDec, centerTo.lonDec);
				mm.setCenterOfScreen(centerTo, true); 
			}
					 */			//TODO what to do, if there is a map at centerTo, but it is not loaded because of mapSwitchMode == dest & cuurpos und daf�r gibt es keine Karte 
				}catch (InterruptedException e) {
					Global.getPref().log("Error starting mavoing map (1): " + e.getMessage(), e, true);
					(new MessageBox("Error", "This must not happen please report to pfeffer how to produce this error message", FormBase.OKB)).execute(); } 
			}
		} catch (Exception e) { // TODO swith waiting indication clock off
			Global.getPref().log("Error starting moving map (2): " + e.getMessage(), e, true);
			(new MessageBox("Error", "Error starting moving map: " + e.getMessage(), FormBase.OKB)).execute(); }
	}

	void updatePendingChanges() {
		if (cacheDirty) {
			if (chD != null)
				chD.getParent().save();
			cacheDirty = false;
		}
	}
	
	/** Save the index file
	 * 
	 * @param askForConfirmation is ignored, old: If true, the save can be cancelled by user
	 */
	public void saveUnsavedChanges(boolean askForConfirmation) {
		if (oldCard!=0) {
			onLeavingPanel(oldCard);
			onEnteringPanel(0);
			oldCard=0;
		}
		updatePendingChanges();
		if (profile.hasUnsavedChanges()) profile.saveIndex(Global.getPref(),true);
	    this.tbP.saveColWidth(pref);
		Global.getPref().savePreferences();
	}
	
	private void checkProfileChange() {
		// A panel is selected. Could be the same panel twice
		mnuMain.allowProfileChange(false);	  
		if(this.getSelectedItem() == 0){// List view selected
			mnuMain.allowProfileChange(true);	  
			MyLocale.setSIPOff();
		}
	}
}
// 




