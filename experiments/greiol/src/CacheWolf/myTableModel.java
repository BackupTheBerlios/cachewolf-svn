package CacheWolf;

import ewe.ui.*;
import ewe.sys.*;
import ewe.util.*;
import ewe.fx.*;


/**
*	Table model used to display the cache list.
* 	Used by the table control in the first panel of
*	CacheWolf.
* 20061212 salzkammergut, patch to speed up scrolling, Used MyLocale
*/
public class myTableModel extends TableModel{

	// Colors for Cache status (BG unless otherwise stated)
	private static final Color COLOR_FLAGED		= new Color(255,255,0);
	private static final Color COLOR_FOUND		= new Color(152,251,152);
	private static final Color COLOR_OWNED		= new Color(135,206,235);
	private static final Color COLOR_AVAILABLE	= new Color(255,128,0);
	private static final Color COLOR_ARCHIVED	= new Color(200,0,0);
	private static final Color COLOR_SELECTED	= new Color(141,141,141);
	private static final Color COLOR_DETAILS_LOADED		= new Color(229,206,235);
	private static final Color COLOR_WHITE   	= new Color(255,255,255);
	private Color lineColorBG                   = new Color(255,255,255);
	private Color lastColorBG                   = new Color(255,255,255);
	private Color lastColorFG                   = new Color(0,0,0);
	private static final Time lastSyncWorker = new Time();
	private int lastRow = -2;
	private CacheDB cacheDB;
	/** The max number of columns in the list view */
	public static final int N_COLUMNS = 20;
	/** How the columns are mapped onto the list view. If colMap[i]=j, it means that
	 * the element j (as per the list below) is visible in column i.
	 * [0]TickBox, [1]Type, [2]Distance, [3]Terrain, [4]waypoint, [5]name, [6]coordinates,
	 * [7]owner, [8]datehidden, [9]status, [10]distance, [11]bearing, [12] Size, [13] # of OC recommend.
	 * [14] OC index, [15] Solver exists, [16] Note exists, [17] # Additionals, [18] # DNF
	 * [19] Last Sync Date
	 *
	 * Attention: When adding columns here, also add a default width in Preferences.listColWidth
	 */
	private int[] colMap;
	/** The column widths corresponding to the list of columns above */
	private int[] colWidth;
	private String [] colName = {" ","?",MyLocale.getMsg(1000,"D"),MyLocale.getMsg(1001,"T"),
			MyLocale.getMsg(1002,"Waypoint"),"Name",MyLocale.getMsg(1004,"Location"),
			MyLocale.getMsg(1005,"Owner"),MyLocale.getMsg(1006,"Hidden"),MyLocale.getMsg(1007,"Status"),
			MyLocale.getMsg(1008,"Dist"),MyLocale.getMsg(1009,"Bear"),MyLocale.getMsg(1017,"S"),
			MyLocale.getMsg(1026,"#Rec"),MyLocale.getMsg(1027,"OC-IDX"),MyLocale.getMsg(1038,"S"),
			MyLocale.getMsg(1040,"N"),MyLocale.getMsg(1047,"A"),MyLocale.getMsg(1049,"DNF"),
			MyLocale.getMsg(1051,"Last synced")};

	private static Image noFindLogs[] = new Image[4];
	public static mImage red, blue, yellow; // skull, green
	private Image checkboxTicked,checkboxUnticked;
	private mImage bug;
	private boolean sortAsc = false;
	private int sortedBy = -1;
	private FontMetrics fm;
//	private mImage picSizeMicro,picSizeSmall,picSizeReg,picSizeLarge,picSizeVLarge,picSizeNonPhysical;
	private mImage picHasSolver,picHasNotes;
	private mImage[] sizePics = new mImage[CacheSize.CW_TOTAL_SIZE_IMAGES];
	/** This is the modifier (Shift & Control key status) for Pen Events
	 * it is set in myTableControl.onEvent */
	public int penEventModifiers;

//	private int lastRow=-1;
	private myTableControl tcControl;
	public boolean showExtraWptInfo=true;

	public myTableModel(myTableControl tc, FontMetrics fm){
		super();
		cacheDB = Global.getProfile().cacheDB;
		fm = this.fm;
		tcControl = tc;
		setColumnNamesAndWidths();
		this.numRows = cacheDB.size();
		//Dimension selrow = new Dimension(-1,1);
		//this.cursorSize = selrow;
		noFindLogs[0] = new Image("no_1_log.png");
		noFindLogs[1] = new Image("no_2_log.png");
		noFindLogs[2] = new Image("no_3_log.png");
		noFindLogs[3] = new Image("no_4_log.png");
		red = new mImage("red.png"); red.transparentColor=Color.White;
		blue = new mImage("blue.png"); blue.transparentColor=Color.White;
		//green = new mImage("green.png");green.transparentColor=Color.White;
		yellow = new mImage("yellow.png");yellow.transparentColor=Color.White;
		//skull = new mImage("skull.png");skull.transparentColor=Color.DarkBlue;
		bug = new mImage("bug_table.png");bug.transparentColor=Color.DarkBlue;
		checkboxTicked = new Image("checkboxTicked.png");
		checkboxUnticked= new Image("checkboxUnticked.png");

//		picSizeMicro=new mImage("sizeMicro.png"); picSizeMicro.transparentColor=Color.White;
//		picSizeSmall=new mImage("sizeSmall.png"); picSizeSmall.transparentColor=Color.White;
//		picSizeReg=new mImage("sizeReg.png"); picSizeReg.transparentColor=Color.White;
//		picSizeLarge=new mImage("sizeLarge.png"); picSizeLarge.transparentColor=Color.White;
//		picSizeVLarge=new mImage("sizeVLarge.png"); picSizeVLarge.transparentColor=Color.White;
//		picSizeNonPhysical=new mImage("sizeNonPhysical.png"); picSizeNonPhysical.transparentColor=Color.White;

		for (byte i = 0; i < CacheSize.CW_TOTAL_SIZE_IMAGES; i++) {
			sizePics[i] = new mImage(CacheSize.sizeImageForId(i));
			sizePics[i].transparentColor=Color.White;
		}

		picHasSolver=new mImage("solver_exists.png"); picHasSolver.transparentColor=Color.White;
		picHasNotes=new mImage("notes_exist.png"); picHasNotes.transparentColor=Color.White;
//		updateRows();
	}

	/**
	 * Sets the column names and widths from preferences
	 *
	 */
	public void setColumnNamesAndWidths() {
		colMap=TableColumnChooser.str2Array(Global.getPref().listColMap,0,N_COLUMNS-1,0, -1);
		colWidth=TableColumnChooser.str2Array(Global.getPref().listColWidth,10,1024,50, colMap.length);
		numCols=colMap.length;
		clearCellAdjustments();
		// If the displayed columns include the checkbox, we use the full menu
		if ((","+Global.getPref().listColMap+",").indexOf(",0,")>=0)
			tcControl.setMenuFull();
		else
			tcControl.setMenuSmall();
	}

	/**
	 * Return the column widths as a comma delimited string for storing in the preferences
	 * @return
	 */
	public String getColWidths() {
		// Update the list with the current widths
		for (int col=0; col<numCols; col++) {
			colWidth[colMap[col]]=getColWidth(col);
		}
		clearCellAdjustments();
		// Convert to string
		StringBuffer sb=new StringBuffer(100);
		for (int i=0; i<N_COLUMNS; i++) {
			if (sb.length()!=0) sb.append(',');
			sb.append(colWidth[i]);
		}
		return sb.toString();
	}
	public void updateRows(){
		Vector sortDB = new Vector();
		Vector notVisibleDB = new Vector();
		CacheHolder ch, addiWpt;
		// sort cacheDB:
		// - addi wpts are listet behind the main cache
		// - filtered caches are moved to the end
		int size=cacheDB.size();
		for (int i=0; i<size; i++){
			ch = cacheDB.get(i);
			if (!ch.isVisible()) {
				notVisibleDB.add(ch);
			} else { // point is not filtered
				if (ch.isAddiWpt()){ // unfiltered Addi Wpt
					// check if main wpt is filtered
					if(ch.mainCache != null) { // parent exists
						if (! ch.mainCache.isVisible())
							sortDB.add(ch); // Unfiltered Addi Wpt with filtered Main Wpt, show it on its own
						// else Main cache is not filtered, Addi will be added below main cache further down
					} else { //Addi without main Cache
						sortDB.add(ch);
					}
				} else { // Main Wpt, not filtered. Check for Addis
					sortDB.add(ch);
					if (ch.hasAddiWpt()){
						for (int j=0; j<ch.addiWpts.getCount();j++){
							addiWpt = (CacheHolder)ch.addiWpts.get(j);
							if (addiWpt.isVisible()) sortDB.add(addiWpt);
						}
					}// if hasAddiWpt
				} // if AddiWpt
			} // if filtered
		}
		// rebuild database
		cacheDB.rebuild(sortDB, notVisibleDB);
		this.numRows = sortDB.getCount();
	}

	/**
	 * Method to set the row color of the table displaying the cache list, depending on different
	 * flags set to the cache.
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see ewe.ui.TableModel#getCellAttributes(int, int, boolean, ewe.ui.TableCellAttributes)
	 */
	public TableCellAttributes getCellAttributes(int row, int col, boolean isSelected,
	        TableCellAttributes ta) {
		ta = super.getCellAttributes(row, col, isSelected, ta);
		ta.alignment = CellConstants.LEFT;
		ta.anchor = CellConstants.LEFT;
		// The default color of a line is white
		lineColorBG.set(COLOR_WHITE);
		// Determination of colors is only done for first column. Other columns take same
		// color.
		if (row >= 0) {
			if (row==0 || row != lastRow) {
				try {
					// Now find out if the line should be painted in an other color.
					// Selected lines are not considered, so far
					CacheHolder ch = cacheDB.get(row);
					if (ch.is_owned())
						lineColorBG.set(COLOR_OWNED);
					else if (ch.is_found())
						lineColorBG.set(COLOR_FOUND);
					else if (ch.is_flaged)
						lineColorBG.set(COLOR_FLAGED);
					else if (Global.getPref().debug && ch.detailsLoaded())
						lineColorBG.set(COLOR_DETAILS_LOADED);

					if (ch.is_archived()) {
						if (lineColorBG.equals(COLOR_WHITE)) {
							lineColorBG.set(COLOR_ARCHIVED);
							ta.foreground = COLOR_WHITE;
						} else {
							ta.foreground = COLOR_ARCHIVED;
						}
					} else if (!ch.is_available()) {
						if (lineColorBG.equals(COLOR_WHITE)) {
							lineColorBG.set(COLOR_AVAILABLE);
						} else {
							ta.foreground = COLOR_AVAILABLE;
						}
					}

					// Now, if a line is selected, blend the determined color with the selection
					// color.
					if (isSelected)
						mergeColor(lineColorBG, lineColorBG, COLOR_SELECTED);
					ta.fillColor = lineColorBG;
					lastColorBG.set(ta.fillColor);
					lastColorFG.set(ta.foreground);
					lastRow = row;
				} catch (Exception e) {
					if (Global.getPref().debug) Global.getPref().log("Ignored Exception in myTableModel.TableCellAttributes()", e, true);
				};
			} else  {
				// Here: We already had this row.
				// Take color computed for last column
				ta.fillColor = lastColorBG;
				ta.foreground = lastColorFG;
			}
		} else if (row == -1 && colMap[col] == 0 && Global.getProfile().showBlacklisted()) {
			ta.fillColor = Color.Black;
			lastColorBG.set(ta.fillColor);
		}
		return ta;
	}

	/**
	 * Determines the arithmetic mean value of two colors and stores the result in the
	 * third color.
	 * @param colorMerged Resulting color
	 * @param colorA First color to merge. May be same object as <code>colorMerged</code>.
	 * @param colorB Second color to merge. May be same object as <code>colorMerged</code>.
	 */
	private void mergeColor(Color colorMerged, Color colorA, Color colorB) {
		colorMerged.set((colorA.getRed()+colorB.getRed())/2,
				         (colorA.getGreen()+colorB.getGreen())/2,
				         (colorA.getBlue()+colorB.getBlue())/2);
	}
	public int calculateRowHeight(int row){
		return java.lang.Math.max(18, charHeight+4);
	}

	public int calculateColWidth(int col){
		if(col == -1)
        	return 0;
        else if (col<numCols)
        	return colWidth[colMap[col]];
        else return 0;
	}

	/**
	 * Need to override this method with a null return to avoid
	 * getCellData being called twice on each access to a cell.
	 * For further reference see the Ewe source code.
	 * @author skg
	 */
	public Object getCellText(int row, int col) {
		return null;
	}

	public Object getCellData(int row, int col){
		if(row == -1) return colName[colMap[col]];
		try { // Access to row can fail if many caches are deleted
			CacheHolder ch = cacheDB.get(row);
			if(ch!=null /*ch.isVisible()*/){ // Check of visibility needed here??
				switch(colMap[col]) { // Faster than using column names
					case 0: // Checkbox
						if (ch.is_Checked)
							return checkboxTicked;
						else
							return checkboxUnticked;
					case 1: // Type
						return GuiImageBroker.getTypeImage(ch.getType());
					case 2: // Difficulty;
						// FIXME Needs optimizing when code is stable
						if (ch.isAddiWpt() || ch.getType()==CacheType.CW_TYPE_CUSTOM) {
							return "";
						} else {
							return CacheTerrDiff.longDT(ch.getHard());
						}
					case 3: // Terrain
						// FIXME Needs optimizing when code is stable
						if (ch.isAddiWpt() || ch.getType()==CacheType.CW_TYPE_CUSTOM) {
							return "";
						} else {
							return CacheTerrDiff.longDT(ch.getTerrain());
						}
					case 4: // Waypoint
						if (showExtraWptInfo) {
							if(ch.is_incomplete()) return ch.getIconAndTextWP(4, fm);
							if(ch.is_new()       ) return ch.getIconAndTextWP(3, fm);
							if(ch.is_updated()    ) return ch.getIconAndTextWP(2, fm);
							if(ch.is_log_updated()) return ch.getIconAndTextWP(1, fm);
						}
						return ch.getWayPoint();
					case 5: // Cachename
						// Fast return for majority of case
						if (!showExtraWptInfo || (ch.has_bugs() == false && ch.getNoFindLogs()==0)) return ch.getCacheName();
						// Now need more checks
						IconAndText wpVal = new IconAndText();
						if(ch.has_bugs() == true) wpVal.addColumn(bug);
						if(ch.getNoFindLogs() > 0){
							if (ch.getNoFindLogs() > noFindLogs.length)
								wpVal.addColumn(noFindLogs[noFindLogs.length-1]);
							else
								wpVal.addColumn(noFindLogs[ch.getNoFindLogs()-1]);
						}
						wpVal.addColumn(ch.getCacheName());
						return wpVal;
					case 6: // Location
						return ch.LatLon;
					case 7: // Owner
						return ch.getCacheOwner();
					case 8: // Date hidden
						return ch.getDateHidden();
					case 9: // Status
						return ch.getCacheStatus();
					case 10: // Distance
						return ch.getDistance();
					case 11: // Bearing
						return ch.bearing;
					case 12: // Size
						if (ch.isAddiWpt()) {
							return "";
						} else {
							return sizePics[CacheSize.guiSizeImageId(ch.getCacheSize())];
						}
					case 13: // OC number of recommendations
						if (ch.isAddiWpt() || CacheType.CW_TYPE_CUSTOM == ch.getType()) return null;
						return Convert.formatInt(ch.getNumRecommended());
					case 14: // OC rating
						if (ch.getWayPoint().startsWith("OC"))
							return Convert.formatInt(ch.recommendationScore);
						return null;
					case 15: // Is solver filled?
					    if (ch.hasSolver()) return picHasSolver; else return null;
					case 16: // Does note exist?
						if (ch.hasNote()) return picHasNotes; else return null;
					case 17: // Number of Additional Waypoints;
						if (ch.mainCache == null && ch.addiWpts.size()>0) {
							return String.valueOf(ch.addiWpts.size());
						} else {
							return "";
						}
					case 18: // Number of DNF logs
						if (ch.getNoFindLogs()>0) {
							return String.valueOf(ch.getNoFindLogs());
						} else {
							return "";
						}
					case 19: // Last sync date
						if (!ch.getLastSync().equals("")) {
							try {
	                            lastSyncWorker.parse(ch.getLastSync(),"yyyyMMddHHmmss");
                            } catch (IllegalArgumentException e) {
                            	Global.getPref().log("Could not parse 'lastSyncDate': "+ch.getLastSync()+". Reset to empty.",e);
                            	ch.setLastSync("");
                            }
							return lastSyncWorker.format("yyyy-MM-dd HH:mm");
						} else {
							return "";
						}
				} // Switch
			} // if
		} catch (Exception e) {
			if (Global.getPref().debug) Global.getPref().log("Ignored Exception in myTableModel.getCellData()",e, true);
			return null;
		}
		return null;
	}

	public boolean penPressed(Point onTable,Point cell){
		boolean retval = false;
		if (cell==null) return false;
		try{
			// Check whether the click is on the checkbox image
			if (cell.y>=0 && colMap[cell.x]==0) {
				Global.getProfile().selectionChanged = true;
				if ((penEventModifiers & IKeys.SHIFT)>0) {
					if (tcControl.cursor.y >= 0) { // Second row being marked with shift key pressed
						if (tcControl.cursor.y<cell.y)
							toggleSelect(tcControl.cursor.y+1,cell.y,cell.x);
						else
							toggleSelect(cell.y,tcControl.cursor.y-1,cell.x);
					} else { // Remember this row as start of range, but don't toggle yet
					}
				} else { // Single row marked
					toggleSelect(cell.y,cell.y,cell.x);
				}
			}
			if(cell.y == -1){ // Hit a header => sort the table accordingly
				CacheHolder ch=null;
				// cell.x is the physical column but we have to sort by the
				// column it is mapped into
				int mappedCol=colMap[cell.x];
				if (mappedCol==0) { // Click on Tickbox header
					// Hide/unhide the additional information about a waypoint such as
					// travelbugs/number of notfound logs/yellow circle/red circle etc.
					// This helps on small PDA screens
					showExtraWptInfo=!showExtraWptInfo;
					this.table.repaint();
					return true;
				}
				Vm.showWait(true);
				Point a = tcControl.getSelectedCell(null);
				if((a != null) && (a.y >= 0) && (a.y < cacheDB.size())) ch = cacheDB.get(a.y);
				if (mappedCol == sortedBy) sortAsc=!sortAsc;
				else sortAsc = false;
				sortedBy = mappedCol;
				cacheDB.sort(new MyComparer(cacheDB, mappedCol,numRows), sortAsc);
				updateRows();
				if(a != null && ch!=null){
					int rownum = Global.getProfile().getCacheIndex(ch.getWayPoint());
					if(rownum >= 0){
						tcControl.cursorTo(rownum, 0, true);
	/*					tcControl.scrollToVisible(rownum, 0);
						tcControl.clearSelectedCells(new Vector());
						for(int i= 0; i < MAXCOLUMNS; i++){
							tcControl.addToSelection(rownum,i);
						}
		*/			}
				}
				Vm.showWait(false);
				tcControl.update(true);
				retval = true;
			}
		} catch(NullPointerException npex){
			Global.getPref().log("NPE in myTableModel.Penpressed");
			Vm.showWait(false);
			}
		return retval;
	}

	/** Toggle the select status for a group of caches
	 * If from==to, the addi Waypoints are also toggled if the cache is a main waypoint
	 * If from!=to, each cache is toggled irrespective of its type (main or addi)
	 * @param from index of first cache to toggle
	 * @param to index of last cache to toggle
	 * @param x is column of checkbox (does not have to be 0)
	 */
	void toggleSelect(int from, int to, int x) {
		CacheHolder ch;
		boolean singleRow= from == to;
		for (int j=from; j<=to; j++) {
			ch=cacheDB.get(j);
			ch.is_Checked= !ch.is_Checked;
			tcControl.repaintCell(j, x);
			// set the ceckbox also for addi wpts
			if (ch.hasAddiWpt() && singleRow){
				CacheHolder addiWpt;
				int addiCount=ch.addiWpts.getCount();
				for (int i=0;i<addiCount;i++){
					addiWpt = (CacheHolder)ch.addiWpts.get(i);
					addiWpt.is_Checked = ch.is_Checked;
					if (addiWpt.isVisible()){
						tcControl.repaintCell(cacheDB.getIndex(addiWpt), x);
					}
				}

			}
		}
	}
	public void select(int row,int col,boolean selectOn) {
		//super.select(row, col, selectOn);
		tcControl.cursorTo(row, col, true);
	}

}
