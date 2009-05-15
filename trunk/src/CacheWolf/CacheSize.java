package CacheWolf;

public final class CacheSize {

	/*
	 * internal representation of cache sizes in CacheHolder we just made them
	 * up ;-)
	 */
	static final protected byte CW_SIZE_NOTCHOSEN = 0;
	static final protected byte CW_SIZE_OTHER = 1;
	static final protected byte CW_SIZE_MICRO = 2;
	static final protected byte CW_SIZE_SMALL = 3;
	static final protected byte CW_SIZE_REGULAR = 4;
	static final protected byte CW_SIZE_LARGE = 5;
	static final protected byte CW_SIZE_VERYLARGE = 6;
	static final protected byte CW_SIZE_NONE = 7;
	static final protected byte CW_SIZE_VIRTUAL = 8;

	/*
	 * geocaching.com size string as found by analyzing GPX files plus OC/TC
	 * Very large
	 */
	static final protected String GC_SIZE_MICRO = "Micro";
	static final protected String GC_SIZE_SMALL = "Small";
	static final protected String GC_SIZE_REGULAR = "Regular";
	static final protected String GC_SIZE_LARGE = "Large";
	static final protected String GC_SIZE_NOTCHOSEN = "Not chosen";
	static final protected String GC_SIZE_OTHER = "Other";
	static final protected String GC_SIZE_VIRTUAL = "Virtual";
	static final protected String OCTC_SIZE_VERYLARGE = "Very large";
	static final protected String OCTC_SIZE_NONE = "None";

	/*
	 * OpenCaching Size IDs see
	 * http://oc-server.svn.sourceforge.net/viewvc/oc-server/doc/sql/static-data/data.sql?view=markup
	 */
	static final protected String OC_SIZE_OTHER = "1";
	static final protected String OC_SIZE_MICRO = "2";
	static final protected String OC_SIZE_SMALL = "3";
	static final protected String OC_SIZE_NORMAL = "4";
	static final protected String OC_SIZE_LARGE = "5";
	static final protected String OC_SIZE_VERYLARGE = "6";
	static final protected String OC_SIZE_NONE = "7";

	/*
	 * TerraCaching Size IDs taken from old GPXimporter (?? reliable source ??)
	 */
	static final protected String TC_SIZE_MICRO = "1";
	static final protected String TC_SIZE_MEDIUM = "2";
	static final protected String TC_SIZE_REGULAR = "3";
	static final protected String TC_SIZE_LARGE = "4";
	static final protected String TC_SIZE_VERYLARGE = "4";

	/*
	 * images to show in CW index panel we use less images than sizes since all
	 * non physical caches are represented by the same symbol
	 */
	/** GUI image for micro caches */
	static final protected String CW_GUIIMG_MICRO = "sizeMicro.png";
	/** GUI image for small caches */
	static final protected String CW_GUIIMG_SMALL = "sizeSmall.png";
	/** GUI image for regular / normal caches */
	static final protected String CW_GUIIMG_NORMAL = "sizeReg.png";
	/** GUI image for large caches */
	static final protected String CW_GUIIMG_LARGE = "sizeLarge.png";
	/** GUI image for non physical caches */
	static final protected String CW_GUIIMG_NONPHYSICAL = "sizeNonPhysical.png";
	/** GUI image for very large caches */
	static final protected String CW_GUIIMG_VERYLARGE = "sizeVLarge.png";

	/*
	 * IDs for the sizePics[] array in TableModel therefore they must start with
	 * 0 and be consecutive
	 */
	static final protected byte CW_GUIIMGID_MICRO = 0;
	static final protected byte CW_GUIIMGID_SMALL = 1;
	static final protected byte CW_GUIIMGID_NORMAL = 2;
	static final protected byte CW_GUIIMGID_LARGE = 3;
	static final protected byte CW_GUIIMGID_NONPHYSICAL = 4;
	static final protected byte CW_GUIIMGID_VERYLARGE = 5;

	/*
	 * total number of different size images will be used to det the dimension
	 * of sizePics[] array in TableModel
	 */
	static final protected byte CW_TOTAL_SIZE_IMAGES = 6;

	/*
	 * bit masks to be used with the filter function
	 */
	static final protected byte CW_FILTER_MICRO = 0x01 << 0;
	static final protected byte CW_FILTER_SMALL = 0x01 << 1;
	static final protected byte CW_FILTER_NORMAL = 0x01 << 2;
	static final protected byte CW_FILTER_LARGE = 0x01 << 3;
	static final protected byte CW_FILTER_VERYLARGE = 0x01 << 4;
	static final protected byte CW_FILTER_NONPHYSICAL = 0x01 << 5;
	static final protected byte CW_FILTER_ALL = CW_FILTER_MICRO
			| CW_FILTER_SMALL | CW_FILTER_NORMAL | CW_FILTER_LARGE
			| CW_FILTER_NONPHYSICAL | CW_FILTER_VERYLARGE;

	/**
	 * the constructor does nothing
	 */
	public CacheSize() {
		// do nothing
	}

	/**
	 * map filenames of images for the different sizes to the ids used array
	 * index for sizePics[] in TableModel
	 * 
	 * @param id
	 *            size identifier matching the CW_GUIIMGID_ constants
	 * @return filenam of image to be displayed for id
	 * @throws IllegalArgumentException
	 *             if there is no image associated to the id
	 */
	public static String sizeImageForId(byte id) {
		switch (id) {
			case CW_GUIIMGID_MICRO:
				return CW_GUIIMG_MICRO;
			case CW_GUIIMGID_SMALL:
				return CW_GUIIMG_SMALL;
			case CW_GUIIMGID_NORMAL:
				return CW_GUIIMG_NORMAL;
			case CW_GUIIMGID_LARGE:
				return CW_GUIIMG_LARGE;
			case CW_GUIIMGID_NONPHYSICAL:
				return CW_GUIIMG_NONPHYSICAL;
			case CW_GUIIMGID_VERYLARGE:
				return CW_GUIIMG_VERYLARGE;
			default:
				throw (new IllegalArgumentException("unmatched argument " + id + " in CacheSize cw2ExportString()"));
		}
	}

	/**
	 * convert the size info from a CacheHolder to a string suitable for GPX
	 * export
	 * 
	 * @param size
	 *            CW internal representation of cache size
	 * @return string representation of CacheWolf internal cache size
	 * @throws IllegalArgumentException
	 *             if cwsize can not be mapped to a CW_SIZE constant
	 */
	public static String cw2ExportString(byte size) {
		switch (size) {
			case CW_SIZE_MICRO:
				return GC_SIZE_MICRO;
			case CW_SIZE_SMALL:
				return GC_SIZE_SMALL;
			case CW_SIZE_REGULAR:
				return GC_SIZE_REGULAR;
			case CW_SIZE_LARGE:
				return GC_SIZE_LARGE;
			case CW_SIZE_NOTCHOSEN:
				return GC_SIZE_NOTCHOSEN;
			case CW_SIZE_OTHER:
				return GC_SIZE_OTHER;
			case CW_SIZE_VIRTUAL:
				return GC_SIZE_VIRTUAL;
			case CW_SIZE_VERYLARGE:
				return OCTC_SIZE_VERYLARGE;
			case CW_SIZE_NONE:
				return OCTC_SIZE_NONE;
			default:
				throw (new IllegalArgumentException("unmatched argument " + size + " in CacheSize cw2ExportString()"));
		}
	}

	/**
	 * convert the cache size information from a TerraCaching GPX import to
	 * internal representation
	 * 
	 * @param tcstring
	 *            size information extracted from a TC GPX inport
	 * @return CacheWolf internal representation of size information
	 * @throws IllegalArgumentException
	 *             if tcstring can not be mapped to internal representation
	 *             (CW_SIZE_*)
	 */

	public static byte tcGpxString2Cw(String tcstring) {
		if (tcstring.equals(TC_SIZE_MICRO)) {
			return CW_SIZE_MICRO;
		} else if (tcstring.equals(TC_SIZE_MEDIUM)) {
			return CW_SIZE_SMALL;
		} else if (tcstring.equals(TC_SIZE_REGULAR)) {
			return CW_SIZE_REGULAR;
		} else if (tcstring.equals(TC_SIZE_LARGE)) {
			return CW_SIZE_LARGE;
		} else if (tcstring.equals(TC_SIZE_VERYLARGE)) {
			return CW_SIZE_VERYLARGE;
		} else {
			throw (new IllegalArgumentException("unmatched argument " + tcstring + " in CacheSize tcGpxString2Cw()"));
		}
	}

	/**
	 * convert the cache size information from a GC GPX import to internal
	 * representation
	 * 
	 * @param gcstring
	 *            size information extracted from a GPX inport
	 * @return CacheWolf internal representation of size information
	 * @throws IllegalArgumentException
	 *             if gcstring can not be mapped to internal representation
	 *             (CW_SIZE_*)
	 */

	public static byte gcGpxString2Cw(String gcstring) {
		if (gcstring.equals(GC_SIZE_MICRO)) {
			return CW_SIZE_MICRO;
		} else if (gcstring.equals(GC_SIZE_SMALL)) {
			return CW_SIZE_SMALL;
		} else if (gcstring.equals(GC_SIZE_REGULAR)) {
			return CW_SIZE_REGULAR;
		} else if (gcstring.equals(GC_SIZE_LARGE)) {
			return CW_SIZE_LARGE;
		} else if (gcstring.equals(GC_SIZE_NOTCHOSEN)) {
			return CW_SIZE_NOTCHOSEN;
		} else if (gcstring.equals(GC_SIZE_OTHER)) {
			return CW_SIZE_OTHER;
		} else if (gcstring.equals(GC_SIZE_VIRTUAL)) {
			return CW_SIZE_VIRTUAL;
		} else {
			throw (new IllegalArgumentException("unmatched argument " + gcstring + " in CacheSize gcGpxString2Cw()"));
		}
	}

	/**
	 * convert the cache size information from GCSpider to internal
	 * representation for CacheHolder
	 * 
	 * @param spiderstring
	 *            string identified by the spider as containing size iformation
	 * @return CacheWolf internal representation of size information
	 * @throws IllegalArgumentException
	 *             if spiderstring can not be mapped to internal representation
	 *             (CW_SIZE_*)
	 */
	public static byte gcSpiderString2Cw(String spiderstring) {
		// at the moment both sources use the same strings
		return gcGpxString2Cw(spiderstring);
	}

	/**
	 * map information from an Opencaching XML cache description suitable for
	 * CacheHolder
	 * 
	 * @param ocxmlstring
	 *            string extracted from OC-XML attribute size
	 * @return CacheWolf internal representation of size information
	 * @trows IllegalArgumentException if ocxmlstring can not be mapped to a
	 *        CW_SIZE_*
	 */
	public static byte ocXmlString2Cw(String ocxmlstring) {
		if (ocxmlstring.equals(OC_SIZE_OTHER)) {
			return CW_SIZE_OTHER;
		} else if (ocxmlstring.equals(OC_SIZE_MICRO)) {
			return CW_SIZE_MICRO;
		} else if (ocxmlstring.equals(OC_SIZE_SMALL)) {
			return CW_SIZE_SMALL;
		} else if (ocxmlstring.equals(OC_SIZE_NORMAL)) {
			return CW_SIZE_REGULAR;
		} else if (ocxmlstring.equals(OC_SIZE_LARGE)) {
			return CW_SIZE_LARGE;
		} else if (ocxmlstring.equals(OC_SIZE_VERYLARGE)) {
			return CW_SIZE_VERYLARGE;
		} else if (ocxmlstring.equals(OC_SIZE_NONE)) {
			return CW_SIZE_NOTCHOSEN;
		} else {
			throw (new IllegalArgumentException("unmatched argument " + ocxmlstring + " in CacheSize ocXmlString2Cw()"));
		}
	}

	/**
	 * get name of the image to be displayed in CW index panel
	 * 
	 * @param size
	 *            CW internal representation of cache size
	 * @return filename of image to be displayed in main panel as size icon
	 * @throws IllegalArgumentException
	 *             if size can not be mapped
	 */
	public static byte guiSizeImageId(byte size) {
		switch (size) {
			case CW_SIZE_MICRO:
				return CW_GUIIMGID_MICRO;
			case CW_SIZE_SMALL:
				return CW_GUIIMGID_SMALL;
			case CW_SIZE_REGULAR:
				return CW_GUIIMGID_NORMAL;
			case CW_SIZE_LARGE:
				return CW_GUIIMGID_LARGE;
			case CW_SIZE_NOTCHOSEN:
				return CW_GUIIMGID_NONPHYSICAL;
			case CW_SIZE_OTHER:
				return CW_GUIIMGID_NONPHYSICAL;
			case CW_SIZE_VIRTUAL:
				return CW_GUIIMGID_NONPHYSICAL;
			case CW_SIZE_VERYLARGE:
				return CW_GUIIMGID_VERYLARGE;
			case CW_SIZE_NONE:
				return CW_GUIIMGID_NONPHYSICAL;
			default:
				throw (new IllegalArgumentException("unmatched argument " + size + " in CacheSize guiSizeImage()"));
		}
	}

	/**
	 * convert an "old style" size string to the new internal representation
	 * 
	 * @param v1Size
	 *            old size string
	 * @return CW internal representation of cache size
	 * @throws if v1Size can not be mapped
	 * @deprecated remove once v1 file version compatibility is abandoned
	 */
	public static final byte v1Converter(String v1Size) {
		if (v1Size.equals(GC_SIZE_MICRO)) {
			return CW_SIZE_MICRO;
		} else if (v1Size.equals(GC_SIZE_SMALL)) {
			return CW_SIZE_SMALL;
		} else if (v1Size.equals(GC_SIZE_REGULAR)) {
			return CW_SIZE_REGULAR;
		} else if (v1Size.equals(GC_SIZE_LARGE)) {
			return CW_SIZE_LARGE;
		} else if (v1Size.equals(GC_SIZE_NOTCHOSEN)) {
			return CW_SIZE_NOTCHOSEN;
		} else if (v1Size.equals(GC_SIZE_OTHER)) {
			return CW_SIZE_OTHER;
		} else if (v1Size.equals(GC_SIZE_VIRTUAL)) {
			return CW_SIZE_VIRTUAL;
		} else if (v1Size.equals(OCTC_SIZE_NONE)) {
			return CW_SIZE_NONE;
		} else if (v1Size.equals(OCTC_SIZE_VERYLARGE)) {
			return CW_SIZE_VERYLARGE;
		} else if (v1Size.equals("")) {
			return CW_SIZE_NOTCHOSEN;
		} else if (v1Size.equals(null)) {
			return CW_SIZE_NOTCHOSEN;
		} else {
			throw (new IllegalArgumentException("unmatched argument " + v1Size + " in v1Converter()"));
		}
	}
	
	public static final byte v2Converter(byte v2Size) {
		switch(v2Size) {
			case CW_SIZE_MICRO: // fall through
			case CW_SIZE_SMALL: // fall through
			case CW_SIZE_REGULAR: // fall through
			case CW_SIZE_LARGE: // fall through
			case CW_SIZE_NOTCHOSEN: // fall through
			case CW_SIZE_OTHER: // fall through
			case CW_SIZE_VIRTUAL: // fall through
			case CW_SIZE_NONE: // fall through
			case CW_SIZE_VERYLARGE: return v2Size;
			case -1: return CW_SIZE_NOTCHOSEN; // -1 was catch all in v2
			default:
				throw (new IllegalArgumentException("unmatched argument " + v2Size + " in v2Converter()"));
		}
	}

	/**
	 * return a bit mask representing the caches size for use in the Filter
	 * 
	 * @param size
	 *            CW internal representation of cache size
	 * @return a bit mask for the filter function
	 * @throws IllegalArgumentException
	 *             if size can not be mapped to a bit mask
	 */

	public static byte getFilterPattern(byte size) {
		switch (size) {
			case CW_SIZE_MICRO:
				return CW_FILTER_MICRO;
			case CW_SIZE_SMALL:
				return CW_FILTER_SMALL;
			case CW_SIZE_REGULAR:
				return CW_FILTER_NORMAL;
			case CW_SIZE_LARGE:
				return CW_FILTER_LARGE;
			case CW_SIZE_NOTCHOSEN:
				return CW_FILTER_NONPHYSICAL;
			case CW_SIZE_OTHER:
				return CW_FILTER_NONPHYSICAL;
			case CW_SIZE_VIRTUAL:
				return CW_FILTER_NONPHYSICAL;
			case CW_SIZE_VERYLARGE:
				return CW_FILTER_VERYLARGE;
			case CW_SIZE_NONE:
				return CW_FILTER_NONPHYSICAL;
			default:
				throw (new IllegalArgumentException("unmatched argument " + size + " in CacheSize getFilterPattern()"));
		}
	}

	/**
	 * provides abbreviated representations of CacheSize for compact exporters
	 * 
	 * @param size
	 *            CW internal representation of cache size
	 * @return a one letter String for cache size
	 * @throws IllegalArgumentException
	 *             if size can not be mapped
	 */

	public static String getExportShortId(byte size) {
		switch (size) {
			case CW_SIZE_MICRO:
				return "m";
			case CW_SIZE_SMALL:
				return "s";
			case CW_SIZE_REGULAR:
				return "r";
			case CW_SIZE_LARGE:
				return "l";
			case CW_SIZE_NOTCHOSEN:
				return "n";
			case CW_SIZE_OTHER:
				return "n";
			case CW_SIZE_VIRTUAL:
				return "n";
			case CW_SIZE_VERYLARGE:
				return "v";
			case CW_SIZE_NONE:
				return "n";
			default:
				throw (new IllegalArgumentException("unmatched argument " + size + " in CacheSize getExportShortId()"));
		}
	}

	/**
	 * generate a string array suitable to be used in DetalsPanel drop down list
	 * 
	 * @return strings to be displayed in the DetailsPanel Size DropDown
	 * @see guiSizeStrings2CwSize
	 * @see cwSizeId2GuiSizeId
	 */

	public static String[] guiSizeStrings() {
		// make sure strings appear in ascending order for CW_SIZE_*
		String ret[] = new String[] { GC_SIZE_NOTCHOSEN, GC_SIZE_OTHER,
				GC_SIZE_MICRO, GC_SIZE_SMALL, GC_SIZE_REGULAR, GC_SIZE_LARGE,
				OCTC_SIZE_VERYLARGE, OCTC_SIZE_NONE, GC_SIZE_VIRTUAL };
		return ret;
	}

	/**
	 * map a string chosen from the DetailsPanel Size drop down list back to
	 * internal representation
	 * 
	 * @param id
	 * @return
	 * @throws IllegalArgumentException
	 *             if id can not be mapped
	 * @see cwSizeId2GuiSizeId
	 * @see guiSizeStrings
	 */
	public static byte guiSizeStrings2CwSize(String id) {
		// map the strings in guiSizeStrings() back to cw byte types
		if (id.equals(GC_SIZE_NOTCHOSEN)) {
			return CW_SIZE_NOTCHOSEN;
		} else if (id.equals(GC_SIZE_OTHER)) {
			return CW_SIZE_OTHER;
		} else if (id.equals(GC_SIZE_SMALL)) {
			return CW_SIZE_SMALL;
		} else if (id.equals(GC_SIZE_REGULAR)) {
			return CW_SIZE_REGULAR;
		} else if (id.equals(GC_SIZE_LARGE)) {
			return CW_SIZE_LARGE;
		} else if (id.equals(OCTC_SIZE_VERYLARGE)) {
			return CW_SIZE_VERYLARGE;
		} else if (id.equals(OCTC_SIZE_NONE)) {
			return CW_SIZE_NONE;
		} else if (id.equals(GC_SIZE_MICRO)) {
			return CW_SIZE_MICRO;
		} else {
			throw (new IllegalArgumentException("unmatched argument " + id + " in guiSizeStrings2CwSize()"));
		}
	}

	/**
	 * map internal representation to index used in the the DetailsPanel Size
	 * drop down list
	 * 
	 * @param id
	 *            internal id to be mapped
	 * @return index of internal size in array
	 * @throws IllegalArgumentException
	 *             if id can not be mapped
	 * @see guiSizeStrings2CwSize
	 * @see cwSizeId2GuiSizeId
	 */
	public static int cwSizeId2GuiSizeId(byte id) {
		switch (id) {
		case CW_SIZE_NOTCHOSEN:
			return 0;
		case CW_SIZE_OTHER:
			return 1;
		case CW_SIZE_MICRO:
			return 2;
		case CW_SIZE_SMALL:
			return 3;
		case CW_SIZE_REGULAR:
			return 4;
		case CW_SIZE_LARGE:
			return 5;
		case CW_SIZE_VERYLARGE:
			return 6;
		case CW_SIZE_NONE:
			return 7;
		case CW_SIZE_VIRTUAL:
			return 8;
		default:
			throw (new IllegalArgumentException("unmatched argument " + id + " in CacheSize ()"));
		}

	}
}