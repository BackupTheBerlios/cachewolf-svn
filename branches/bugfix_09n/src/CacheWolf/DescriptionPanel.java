package CacheWolf;

import ewe.ui.*;
import ewe.fx.*;
import ewe.sys.*;

/**
*	This class shows the long description on a cache.
*/
public class DescriptionPanel extends CellPanel{
	HtmlDisplay disp = new HtmlDisplay();
	mButton btnPlus, btnMinus;
	CacheHolder currCache;
	
	CellPanel buttonP = new CellPanel();
	CellPanel descP = new CellPanel();
	
	public DescriptionPanel(){
		buttonP.addNext(btnPlus = new mButton("+"),CellConstants.HSTRETCH, (CellConstants.HFILL));
		buttonP.addLast(btnMinus = new mButton("-"),CellConstants.HSTRETCH, (CellConstants.HFILL));
		ScrollBarPanel sbp = new ScrollBarPanel(disp, ScrollBarPanel.NeverShowHorizontalScrollers);
		descP.addLast(sbp);
		this.addLast(descP);
		this.addLast(buttonP,CellConstants.HSTRETCH,CellConstants.HFILL);

	}
	
	/**
	*	Set the text to display. Text should be HTML formated.
	*/
	public void setText(CacheHolder cache){
		if (currCache != cache){
			Vm.showWait(true);
			if (cache.is_HTML)	disp.setHtml(cache.LongDescription);
			else				disp.setPlainText(cache.LongDescription);
			disp.scrollTo(0,false);
			Vm.showWait(false);
			currCache = cache;
		}
	}
	
	private void redraw() {
		int currLine;

		Vm.showWait(true);
		currLine = disp.getTopLine();
		if (currCache.is_HTML)	disp.setHtml(currCache.LongDescription);
		else				disp.setPlainText(currCache.LongDescription);
		disp.scrollTo(currLine,false);
		Vm.showWait(false);
	}
	
	/**
	 * Eventhandler
	 */
	public void onEvent(Event ev){
		
		if(ev instanceof ControlEvent && ev.type == ControlEvent.PRESSED){
			if (ev.target == btnPlus){
				Font currFont = disp.getFont();
				currFont = currFont.changeNameAndSize(null, currFont.getSize() + 2);
				disp.setFont(currFont);
				disp.displayPropertiesChanged();
				redraw();
			}

			if (ev.target == btnMinus){
				Font currFont = disp.getFont();
				currFont = currFont.changeNameAndSize(null, currFont.getSize() - 2);
				disp.setFont(currFont);
				disp.displayPropertiesChanged();
				redraw();
			}
		}
		super.onEvent(ev);
	}

}
