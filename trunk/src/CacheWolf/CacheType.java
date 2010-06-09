package CacheWolf;

import ewe.fx.Image;

final class CTyp {
	public byte _cwMappedCType; // CW Cache Typ intern
	public byte _cwCType; // CW Cache Typ intern
	public char _cwCGroup; // Cache Typ Group intern
	public String _cwCTypeV1; // V1 Cache Typ 
	public String _gcCTypeSpider; // GC Type on Spider Import from GC.com 
	public String _ocCTypeXmlImport; // GC Type on Spider Import from GC.com 
	public byte _cwCTypeV2; // V2 Cache Typ 
	public char _gpxShortCType; // Short Typ (one char abbreviation)
	public String _imageName; // name of imageNameture for Icon, "showCacheInBrowser" and "KML Export"
	public String _gpxWptTypeTag; // gpx wpt <type> tag
	public String _gpxWptSymTag; // gpx wpt <sym> tag
	public String _gpxWptGCextensionTypTag; // gpx cache extension <groundspeak:type> tag
	public String _gpxAlternativeWptTypTags; // alternative typ - names for gpx from other sources 		
	public int _msgNrCTypeName; // message number for gui cache Typ name
	public int _GUIOrder; // sort Order in GUI selection //TODO more intelligent implementation (now manually change each line on new one)
	public int _FilterStringPos; // BitNr in Filter String (profile) 
	public int _FilterPattern; // 2**BitNr in Filter int (does not correspond with BitNr in String)
	public Image _iconImage; 
	public Image _mapImage;
	public CTyp(byte cwMappedCType, byte cwCType, char cwCGroup, String cwCTypeV1, String gcCTypeSpider, 
			String ocCTypeXmlImport, byte cwCTypeV2, char gpxShortCType, String imageName, 
			String[] gpx, int msgNrCTypeName, int gUIOrder, int filterStringPos, int filterPattern) {
		
		_cwMappedCType = cwMappedCType;
		_cwCType = cwCType;
		_cwCGroup = cwCGroup;
		_cwCTypeV1 = cwCTypeV1;
		_gcCTypeSpider = gcCTypeSpider;
		_ocCTypeXmlImport = ocCTypeXmlImport;
		_cwCTypeV2 = cwCTypeV2;
		_gpxShortCType = gpxShortCType;
		_imageName = imageName;
		_gpxWptTypeTag = gpx[0];
		_gpxWptSymTag = gpx[1];
		_gpxWptGCextensionTypTag = gpx[2];
		_gpxAlternativeWptTypTags = gpx[3];
		_msgNrCTypeName = msgNrCTypeName;
		_GUIOrder = gUIOrder;
		_FilterStringPos = filterStringPos;
		_FilterPattern = filterPattern;
		if (!_imageName.equals("")) {
			_iconImage=new Image(_imageName);
			_mapImage=_iconImage;
		}
	}
}


/**
 * Handles all aspects of converting cache type information 
 * from and to the various im- and exporters ...
 * converting legacy profiles to current standard
 *
 * Do not instantiate this class, only use it in a static way
 */
public final class CacheType {

	/** thou shallst not instantiate this object */
	private CacheType() {
		// Nothing to do
	}

	/** custom waypoint */
	public static final byte CW_TYPE_CUSTOM = 0;
	/** traditional cache (GC,OC) */
	public static final byte CW_TYPE_TRADITIONAL = 2;
	/** multi cache (GC,OC) */
	public static final byte CW_TYPE_MULTI = 3;
	/** virtual cache (GC,OC) */
	public static final byte CW_TYPE_VIRTUAL = 4;
	/** letterbox cache (GC) */
	public static final byte CW_TYPE_LETTERBOX = 5;
	/** event cache (GC,OC) */
	public static final byte CW_TYPE_EVENT = 6;
	/** unknown cache - Mystery (GC) */
	public static final byte CW_TYPE_UNKNOWN = 8;
	/** drive in cache (OC) */
	public static final byte CW_TYPE_DRIVE_IN = 10;
	/** webcam cache (GC,OC) */
	public static final byte CW_TYPE_WEBCAM = 11;
	/** locationless cache (GC) */
	public static final byte CW_TYPE_LOCATIONLESS = 12;
	/** CITO cache (GC)*/
	public static final byte CW_TYPE_CITO = 13;
	/** Mega Event Cache (GC) */
	public static final byte CW_TYPE_MEGA_EVENT = 100;
	/** WhereIGo Cache (GC) */
	public static final byte CW_TYPE_WHEREIGO = 101;
	/** Earth Cache (GC) */
	public static final byte CW_TYPE_EARTH = 104;
	/** Additional Waypoint Parking (GC) */
	public static final byte CW_TYPE_PARKING = 50;
	/** Additional Waypoint Stage of a Multi (GC) */
	public static final byte CW_TYPE_STAGE = 51;
	/** Additional Waypoint Question to answer (GC) */
	public static final byte CW_TYPE_QUESTION = 52;
	/** Additional Waypoint Final (GC) */
	public static final byte CW_TYPE_FINAL = 53;
	/** Additional Waypoint Trailhead (GC) */
	public static final byte CW_TYPE_TRAILHEAD = 54;
	/** Additional Waypoint Reference (GC) */
	public static final byte CW_TYPE_REFERENCE = 55;
	/** unrecognized cache type or missing information */
	public static final byte CW_TYPE_ERROR = -1;
	String[] ggpx={"Geocache|Custom","Custom","Custom",""};
	private static final CTyp[] cTypRef = {
		// custom waypoints
		new CTyp((byte) 0,(byte) 0,'P',"0","","",(byte) -128,'C',"typeCustom.png",new String[] {"Geocache|Custom","Custom","Custom",""},1,0,10,0x000100),
		// Cache waypoints
		new CTyp((byte) 2,(byte) 2,'C',"2","2","2",(byte) -126,'T',"typeTradi.png",new String[] {"Geocache|Traditional Cache","Geocache","Traditional Cache","Traditional|Classic"},2,1,0,0x000001),
		new CTyp((byte) 3,(byte) 3,'C',"3","3","3",(byte) -125,'M',"typeMulti.png",new String[] {"Geocache|Multi-cache","Geocache","Multi-cache","Multi|Offset"},3,2,1,0x000002),
		new CTyp((byte) 4,(byte) 4,'C',"4","4","4",(byte) -124,'V',"typeVirtual.png",new String[] {"Geocache|Virtual Cache","Geocache","Virtual Cache","Virtual"},4,3,2,0x000004),
		new CTyp((byte) 5,(byte) 5,'C',"5","5","",(byte) -123,'L',"typeLetterbox.png",new String[] {"Geocache|Letterbox Hybrid","Geocache","Letterbox Hybrid","Letterbox"},5,4,3,0x000008),
		new CTyp((byte) 6,(byte) 6,'C',"6","6","6",(byte) -122,'X',"typeEvent.png",new String[] {"Geocache|Event Cache","Geocache","Event Cache","Event"},6,5,4,0x000010),
		new CTyp((byte) 100,(byte) 100,'C',"453","453","",(byte) 101,'X',"typeMegaevent.png",new String[] {"Geocache|Mega-Event Cache","Geocache","Mega-Event Cache","Mega"},14,6,9,0x000200),
		new CTyp((byte) 11,(byte) 11,'C',"11","11","5",(byte) -117,'W',"typeWebcam.png",new String[] {"Geocache|Webcam Cache","Geocache","Webcam Cache","Webcam"},11,7,5,0x000020),
		new CTyp((byte) 8,(byte) 8,'C',"8","8","",(byte) -120,'U',"typeUnknown.png",new String[] {"Geocache|Unknown Cache","Geocache","Unknown Cache","Mystery"},8,8,6,0x000040),
		new CTyp((byte) 12,(byte) 12,'C',"12","12","",(byte) -116,'O',"typeLocless.png",new String[] {"Geocache|Locationless new CTyp(Reverse) Cache","Geocache","Locationless new CTyp(Reverse) Cache","Locationless"},12,9,8,0x000080),
		new CTyp((byte) 13,(byte) 13,'C',"13","13","",(byte) -115,'X',"typeCito.png",new String[] {"Geocache|Cache In Trash Out Event","Geocache","Cache In Trash Out Event","CITO"},13,10,17,0x020000),
		new CTyp((byte) 104,(byte) 104,'C',"137","137","",(byte) 9,'E',"typeEarth.png",new String[] {"Geocache|Earthcache","Geocache","Earthcache","Earth"},18,11,7,0x000400),
		new CTyp((byte) 101,(byte) 101,'C',"1858","1858","",(byte) 100,'G',"typeWhereigo.png",new String[] {"Geocache|Wherigo Cache","Geocache","Wherigo Cache","Wherigo"},15,12,18,0x040000),
		// additional waypoints
		new CTyp((byte) 50,(byte) 50,'A',"50","","",(byte) -78,'P',"typeParking.png",new String[] {"Waypoint|Parking Area","Parking Area","Parking Area",""},50,13,11,0x000800),
		new CTyp((byte) 51,(byte) 51,'A',"51","","",(byte) -77,'S',"typeStage.png",new String[] {"Waypoint|Stages of a Multicache","Stages of a Multicache","Stages of a Multicache",""},51,14,12,0x001000),
		new CTyp((byte) 52,(byte) 52,'A',"52","","",(byte) -76,'Q',"typeQuestion.png",new String[] {"Waypoint|Question to Answer","Question to Answer","Question to Answer",""},52,15,13,0x002000),
		new CTyp((byte) 53,(byte) 53,'A',"53","","",(byte) -75,'F',"typeFinal.png",new String[] {"Waypoint|Final Location","Final Location","Final Location",""},53,16,14,0x004000),
		new CTyp((byte) 54,(byte) 54,'A',"54","","",(byte) -74,'H',"typeTrailhead.png",new String[] {"Waypoint|Trailhead","Trailhead","Trailhead",""},54,17,15,0x008000),
		new CTyp((byte) 55,(byte) 55,'A',"55","","",(byte) -73,'R',"typeReference.png",new String[] {"Waypoint|Reference Point","Reference Point","Reference Point",""},55,18,16,0x010000),
		// error on waypoint
		new CTyp((byte) -1,(byte) -1,'E',"","","",(byte) -1,'-',"guiError.png",new String[] {"","","",""},49,-1,-1,0),
        // mapped types (recognized on input from gpx or download-spider / or cw - version)                 
		new CTyp((byte) 8,(byte) 1,'C',"","","1",(byte) -1,'U',"",new String[] {"Geocache|Other","Geocache","Other","Other"},21,-1,-1,0),
		new CTyp((byte) 8,(byte) 7,'C',"7","","7",(byte) -121,'U',"",new String[] {"Geocache|Quiz","Geocache","Quiz","Quiz"},7,-1,-1,0),
		new CTyp((byte) 8,(byte) 9,'C',"9","","9",(byte) -119,'U',"",new String[] {"Geocache|Moving","Geocache","Moving","Moving"},9,-1,-1,0),
		new CTyp((byte) 2,(byte) 10,'C',"10","","10",(byte) -118,'U',"",new String[] {"Geocache|DriveIn","Geocache","DriveIn","DriveIn"},10,-1,-1,0),
		new CTyp((byte) 6,(byte) 14,'C',"","3653","",(byte) -1,'X',"",new String[] {"Geocache|Lost and Found Event Cache","Geocache","Lost and Found Event Cache",""},6,-1,-1,0),		         
		new CTyp((byte) 2,(byte) 102,'C',"","9","",(byte) -1,'T',"",new String[] {"Geocache|Project APE Cache","Geocache","Project APE Cache","APE"},16,-1,-1,0),
		new CTyp((byte) 6,(byte) 103,'C',"","1304","",(byte) -1,'X',"",new String[] {"Geocache|GPS Adventures Exhibit","Geocache","GPS Adventures Exhibit","MAZE"},17,-1,-1,0),
		new CTyp((byte) 8,(byte) 108,'C',"","","8",(byte) -1,'U',"",new String[] {"only on OC download","","",""},19,-1,-1,0),
		new CTyp((byte) 101,(byte) 15,'C',"","","",(byte) -62,'G',"",new String[] {"Hack for V2 Typ","","",""},-1,-1,-1,0),
	};
	// public static final int anzCacheTyps=cTypRef.length;
	public static final byte maxCWCType=110;
    static final byte[] Ref_Index = new byte[maxCWCType];
	static {
		// +1 cause error is -1 and array starts at 0
	  for (byte i = (byte) (cTypRef.length - 1); i>=0; i--) {
		  Ref_Index[1 + cTypRef[i]._cwCType]= i;
	   }
	}
	public static byte Ref_Index(final byte type) {
		byte ret=Ref_Index[cTypRef[Ref_Index[type+1]]._cwMappedCType + 1];
		return ret;
	}
	
	
	
	/**
	 * check if a given waypoint type is an additional waypoint
	 * @param type waypoint type to check
	 * @return true if it is an additional waypoint, false otherwise
	 */
	public static boolean isAddiWpt(final byte type) {
		return cTypRef[Ref_Index(type)]._cwCGroup == 'A';
	}
	/**
	 * check if a given waypoint type is an cache waypoint
	 * @param type waypoint type to check
	 * @return true if it is an Cache waypoint, false otherwise
	 */
	public static boolean isCacheWpt(final byte type) {
		return cTypRef[Ref_Index(type)]._cwCGroup == 'C';
	}
	/**
	 * check if a given waypoint type is an Custom waypoint
	 * @param type waypoint type to check
	 * @return true if it is an Custom waypint, false otherwise
	 */
	public static boolean isCustomWpt(final byte type) {
		return cTypRef[Ref_Index(type)]._cwCGroup == 'P';
	}
	
	
	// done for DetailsPanel.java and KML- and TomTom-Exporter
	/**
	 * create list of cache types to be shown in GUI drop down lists
	 * @return list of cache types to be shown in GUI drop down list
	 * @see guiSelect2Cw
	 * @see cw2GuiSelect
	 */
	public static String[] guiTypeStrings() {
		int j = 0;
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._GUIOrder > j) {
				j=cTypRef[i]._GUIOrder;
			}
		}
		String[] ret = new String[j+1];
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._GUIOrder > -1) {
				ret[cTypRef[i]._GUIOrder]=MyLocale.getMsg(cTypRef[i]._msgNrCTypeName,"");
			}
		}
		return ret;
	}
	/**
	 * translate GUI drop down index selection back to internally stored type
	 * @param selection index value from drop down list
	 * @return internal type
	 * @throws IllegalArgumentException if <code>selection</code> can not be matched
	 * @see guiTypeStrings
	 * @see cw2GuiSelect
	 */
	public static byte guiSelect2Cw(final int selection) {
		for (byte i=0; i<cTypRef.length; i++) {
			if (cTypRef[i]._GUIOrder == selection) {return cTypRef[i]._cwCType;};
		}
		return -1;
	}
	/**
	 * translate cache type to position of index to highlight in GUI cache type drop down list
	 * @param typeId internal id of cache type
	 * @return index of the cache type in GUI list
	 * @throws IllegalArgumentException if <code>id</code> can not be matched
	 * @see guiTypeStrings
	 * @see guiSelect2Cw
	 */
	public static int cw2GuiSelect(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._GUIOrder;
	}

	
	
	/**
	 * convert the strings found in import of GPX from GC, OC or TC to internal cache type
	 * @param gpxType type information found in GPX
	 * @return internal cache type
	 */
	public static byte gpxType2CwType(final String gpxType) throws IllegalArgumentException {
		for (byte i=0; i<cTypRef.length; i++) {
			if (cTypRef[i]._gpxWptTypeTag.equals(gpxType)) {return cTypRef[i]._cwMappedCType;};
		}
		for (byte i=0; i<cTypRef.length; i++) {
			if (cTypRef[i]._gpxWptGCextensionTypTag.equals(gpxType)) {return cTypRef[i]._cwMappedCType;};
		}
		for (byte i=0; i<cTypRef.length; i++) {
			if (cTypRef[i]._gpxAlternativeWptTypTags.indexOf(gpxType) != -1) {
				return cTypRef[i]._cwMappedCType;
			};
		}
		// TODO extend definition of _gpxAlternativeWptTypTags for all cases of Mystery
		// old code was : if (!(gpxType.indexOf("Mystery")==-1)) return CW_TYPE_UNKNOWN; 
		return -1;
	}

	/**
	 * convert the cache type information from an OC XML import to internal cache type
	 * @param ocType cache type found in OC XML
	 * @return internal cache type
	 * @throws IllegalArgumentException if <code>ocType</code> can not be matched
	 */
	public static byte ocType2CwType(final String ocType) {
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._ocCTypeXmlImport.equals(ocType)) {
				return cTypRef[i]._cwMappedCType;
			}
		}
		return -1;
	}
	/**
	 * convert type information discovered by GC spider to internal type information
	 * @param gcType type information from GC spider
	 * @return internal representation of cache type
	 * @throws IllegalArgumentException if <code>gcType</code> can not be matched
	 */
	public static byte gcSpider2CwType(final String gcType) {
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._gcCTypeSpider.equals(gcType)) {
				return cTypRef[i]._cwMappedCType;
			}
		}
		return -1;
	}
	/**
	 * convert version1 type information to current values
	 * @param type version1 cache type information
	 * @return current version cache type information or -1
	 * @deprecated remove once v1 file version compatibility is abandoned
	 */
	public static byte v1Converter(final String type) {
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._cwCTypeV1.equals(type)) {
				return cTypRef[i]._cwMappedCType;
			}
		}
		return -1;
	}
	/**
	 * convert version2 type information to current values
	 * @param type version2 cache type information
	 * @return current version cache type information or -1
	 * @deprecated remove once v2 file version compatibility is abandoned
	 */
	public static byte v2Converter(final byte type) {
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._cwCTypeV2 == type) {
				return cTypRef[i]._cwMappedCType;
			}
		}
		return -1;
	}

	
	
	/**
	 * translate cache type to a short version for compact exporters or "smart" cache names.
	 * @param typeId CacheWolf internal type information
	 * @return abbreviation of cache type
	 */
	public static String getExportShortId(final byte typeId) {
		return ""+cTypRef[Ref_Index(typeId)]._gpxShortCType;
	}
	/**
	 * map cache types to images
	 * @param typeId internal cache type id
	 * @return non qualified name of image
	 */
	public static String typeImageForId(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._imageName;
	}
	/**
	 * generate type description matching those of GC for GPX export
	 * @param typeId internal type id
	 * @return type information in GC.com <type> GPX format
	 */
	public static String type2TypeTag(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._gpxWptTypeTag;
	}
	/**
	 * generate type description matching those of GC for GPX export
	 * @param typeId internal type id
	 * @return symb information in GC.com <sym> GPX format
	 */
	public static String type2SymTag(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._gpxWptSymTag;
	}
	/**
	 * generate type description matching those of GC for GPX export
	 * @param typeId internal type id
	 * @return type information in GC.com <groundspeak:type> GPX format
	 */
	public static String type2GSTypeTag(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._gpxWptGCextensionTypTag;
	}
	/**
	 * generate type description matching those of GC for GPX export
	 * @param typeId internal type id
	 * @return Gui - string for type
	 */
	public static String type2Gui(final byte typeId) {
		return MyLocale.getMsg(cTypRef[Ref_Index(typeId)]._msgNrCTypeName,"");
	}
	/**
	 * select image to be displayed for a given cache type
	 * @param typeId internal cache type id
	 * @return <code>Image</code> object to be displayed
	 */
	public static Image getTypeImage(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._iconImage;
	}
	/**
	 * select image to be displayed for a given cache type
	 * @param typeId internal cache type id
	 * @return <code>Image</code> object to be displayed
	 */
	public static Image getMapImage(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._mapImage;
	}
	/**
	 * select image to be displayed for a given cache type
	 * @param typeId internal cache type id
	 * @param Image object to be displayed
	 */
	public static void setTypeImage(final byte id, final Image iconImage) {
		if (cTypRef[Ref_Index(id)]._iconImage != cTypRef[Ref_Index(id)]._mapImage) 
			cTypRef[Ref_Index(id)]._iconImage.free();
		cTypRef[Ref_Index(id)]._iconImage=iconImage;
	}
	/**
	 * select image to be displayed for a given cache type
	 * @param typeId internal cache type id
	 * @param Image object to be displayed
	 */
	public static void setMapImage(final byte id, final Image mapImage) {
		if (cTypRef[Ref_Index(id)]._iconImage != cTypRef[Ref_Index(id)]._mapImage) 
			cTypRef[Ref_Index(id)]._mapImage.free();
		cTypRef[Ref_Index(id)]._mapImage=mapImage;
	}
	
	
	// TODO do it better in Version 4
	public static int getCacheTypePattern(final byte typeId) {
		return cTypRef[Ref_Index(typeId)]._FilterPattern;
	}
	public static int Type_FilterString2Type_FilterPattern(final String Type_FilterString) {
		int typeMatchPattern = 0;
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._FilterStringPos > -1) {
				if (Type_FilterString.charAt(cTypRef[i]._FilterStringPos) == '1') {
					typeMatchPattern|=cTypRef[i]._FilterPattern;
				}
			}
		}
		return typeMatchPattern;
	}
	public static boolean hasTypeMatchPattern(final int typeMatchPattern) {
		return typeMatchPattern != 0;
	}
	public static boolean hasMainTypeMatchPattern(final int typeMatchPattern) {
		int TYPE_MAIN = 0;
		for (int i = 0; i < cTypRef.length; i++) {
			if (cTypRef[i]._cwCGroup == 'C' || cTypRef[i]._cwCGroup == 'P' ) {
				TYPE_MAIN|=cTypRef[i]._FilterPattern;
			}
		}
		return (typeMatchPattern & TYPE_MAIN) != 0;
	}
	
	// TODO it for OCXMLImporterScreen and FilterScreen ?
}
