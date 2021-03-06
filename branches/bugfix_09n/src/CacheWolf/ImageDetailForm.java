package CacheWolf;
import ewe.graphics.*;
import ewe.fx.*;
import ewe.ui.*;


/**
* Class creates a view on the image scaled
* to the application size, but only if the image is larger than
* the available app size.
*/
public class ImageDetailForm extends Form{
	String location = new String();
	int origH, origW;
	int state = 0; // 0 = nothing, -1 = scaled to app, 1 = scaled to original size
	int scaleX = 0, scaleY = 0;
	Preferences pref;
	ImageInteractivePanel ipp = new ImageInteractivePanel();
	AniImage ai;
	ScrollBarPanel scp;
	
	public ImageDetailForm(String imgLoc, Preferences p){
		scp = new ScrollBarPanel(ipp);
		setUp(imgLoc, p);
		this.title = "Image";
		this.setPreferredSize(pref.myAppWidth, pref.myAppHeight);
		this.addLast(scp.getScrollablePanel(), CellConstants.STRETCH, CellConstants.FILL);
	}
	
	public ImageDetailForm(){
	}

	/**
	 * Display Image.
	 * @param imgLoc path to the image file
	 * @param p Preferences.
	 * @throws IllegalArgumentException if file not found getcause() gives the name and path of the missing file.
	 */
	public void setUp(String imgLoc, Preferences p) throws IllegalArgumentException {
		pref = p;	
		location = imgLoc;
		try {
			mImage mI = new mImage(imgLoc);
			double scaleFactorX = 1, scaleFactorY = 1, scaleFactor = 1;
			origH = mI.getHeight();
			origW = mI.getWidth();
			if(origW >= pref.myAppWidth) scaleFactorX = pref.myAppWidth/(double)origW;
			if(origH >= pref.myAppHeight) scaleFactorY = pref.myAppHeight/(double)origH;
			if(scaleFactorX >= scaleFactorY) scaleFactor = scaleFactorY;
			if(scaleFactorY >= scaleFactorX) scaleFactor = scaleFactorX;
			state = -1;
			scaleX = (int)(origW*scaleFactor);
			scaleY = (int)(origH*scaleFactor);
			mI = mI.scale(scaleX, scaleY, null, 0);
			ai = new AniImage(mI);
			ai.setLocation(0,0);
			ipp.addImage(ai);
			ipp.setPreferredSize(origW, origH);
			ipp.setParams(state, scaleX, scaleY, origH, origW, scp, imgLoc);
		} catch (IllegalArgumentException e) {
			IllegalArgumentException t = new IllegalArgumentException(imgLoc);
			throw(t);
		}
	}
}

