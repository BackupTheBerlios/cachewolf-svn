    /*
    GNU General Public License
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
    the Free Software Foundation; version 2 of the License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
    */
package CacheWolf.navi;

import CacheWolf.CWPoint;
import ewe.sys.Convert;


public final class UTMProjection extends Projection {
	Ellipsoid ellip;
	
	public UTMProjection(Ellipsoid ellip_) {
		ellip = ellip_;
		zoneSeperately = true;
	}
	public double getEasting(ProjectedPoint pp) {
		return pp.easting + 500000;
	}

	public double getNorthing(ProjectedPoint pp) {
		return (pp.northing >=0 ? pp.northing : pp.northing + 10000000);
	}
	
	public String getZone(ProjectedPoint pp) {
		int zoneletter = (int)Math.floor(pp.zone / 200);
		return Convert.formatInt(pp.zone - zoneletter * 200 +1) + getZoneLetter(zoneletter);
	}
	
	private char getZoneLetter(int number){
		if (((char)(number)) >= 'i' - 'a') number++; // skip I
		if (((char)(number)) >= 'o' - 'a') number++; // skip O
		char ret = (char)( number  + (int)'A' -1);
		return ret;
	}

	public ProjectedPoint project(CWPoint wgs84, ProjectedPoint pp) {
		int stripe = (int) Math.floor((wgs84.lonDec - 180) / 6); // we start with stripe 0, but officially this is stripe 1
		if (stripe < 0) stripe += 60;
		GkProjection.project(wgs84, ellip, 6, (stripe >= 30 ? stripe -30 : stripe +30), 3, 0.9996, pp);
		pp.zone = stripe;
		pp.zone += (int) (Math.floor((wgs84.latDec)/8)+13) * 200; // zone letter
		return pp;
	}
	
	public String toHumanReadableString(ProjectedPoint pp) {
	 	return pp.toString(0, "", " ");
	}

	public ProjectedPoint project(CWPoint ll, ProjectedPoint pp, int epsg) {
		throw new UnsupportedOperationException("UTMProjection: prject by epsg-code not supported");
	}

	public ProjectedPoint set(double northing, double easting, ProjectedPoint pp) {
		throw new UnsupportedOperationException("UTMProjection.set: set() requires zone, use set with 1 more parameter");
	}
	
	public ProjectedPoint set(double northing, double easting, String zone, ProjectedPoint pp) {
		pp.easting = easting - 500000;
		if (northing > 10000000) pp.northing = northing - 10000000;
		else pp.northing = northing;
		if (zone.length() < 1) throw new IllegalArgumentException("UTMProjection.set: zone must be set");
		if (zone.length() > 3) throw new IllegalArgumentException("UTMProjection.set: zone must not have more than 3 letters");
		char lastletter = zone.charAt(zone.length()-1); 
		int zoneletter = -1;
		if ( (lastletter > 'a') && ( lastletter < 'z')) zoneletter = lastletter - 'a';   
		if ( (lastletter > 'A') && ( lastletter < 'Z')) zoneletter = lastletter - 'A';
		if (zoneletter > 'i' - 'a') zoneletter--;
		if (zoneletter > 'o' - 'a') zoneletter--;
		int zonenumer = -1;
		if (zoneletter == -1) {
			zoneletter = 'n' - 'a'; // default to northern hemisphere
			zonenumer = Convert.parseInt(zone);
		} else {
			zonenumer = Convert.parseInt(zone.substring(0, zone.length()-1));
		}
		if (zonenumer == -1) throw new IllegalArgumentException("UTMProjection.set: could not parse zone number");
		pp.zone = zonenumer -1 + zoneletter * 200; // internally zone number starts with 0
		return pp;
	}

	public CWPoint unproject(ProjectedPoint pp) {
		int stripe = pp.zone - (int)Math.floor(pp.zone / 200) * 200;
		int stripelon = stripe * 6 -177;
		return GkProjection.unproject(pp, stripelon, ellip, 0.9996);

	}

}
