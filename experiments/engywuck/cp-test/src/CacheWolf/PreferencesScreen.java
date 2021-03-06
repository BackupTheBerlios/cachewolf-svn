package CacheWolf;

import utils.FileBugfix;
import ewe.ui.*;
import ewe.io.*;
import ewe.fx.*;
import ewe.filechooser.*;
import ewe.sys.*;

/**
*	This class displays a user interface allowing the user to change and set
*	preferences. It also provides a method to save the changed preferences that
*	are saved immediatly when the user presses "Apply".
*	Class ID=600
*/
public class PreferencesScreen extends Form {
	mButton cancelB, applyB, brwBt, gpsB,btnCentre;
	mChoice NS, EW, inpLanguage;
	mInput NSDeg, NSm, EWDeg, EWm, DataDir, Proxy, ProxyPort, Alias, nLogs, Browser, fontSize, inpGPS, 
	       inpLogsPerPage,inpMaxLogsToSpider,inpPassword;
	mCheckBox dif, ter, loc, own, hid, stat, dist, bear, chkAutoLoad, chkShowDeletedImg, chkMenuAtTop, 
	          chkTabsAtTop, chkShowStatus,chkHasCloseButton,chkSynthShort,chkProxyActive, chkDescShowImg;
	mTabbedPanel mTab;
	mChoice chcGarminPort;
	mLabel lblGarmin;
	TableColumnChooser tccBugs,tccList;
	
	Preferences pref;
	
	CellPanel pnlGeneral = new CellPanel();
	CellPanel pnlDisplay = new CellPanel();
	CellPanel pnlMore = new CellPanel();
	CellPanel pnlTB = new CellPanel();
	Frame frmGarmin = new Frame();
	ScrollBarPanel scp; // TODO not neede any more?
	String [] garminPorts= new String[]{"com1","com2","com3","com4","com5","com6","com7","usb"};
	
	public PreferencesScreen (Preferences p){
		mTab=new mTabbedPanel();
		
		pref = p;
		this.title = MyLocale.getMsg(600,"Preferences");
		//this.resizable = false;
		//this.moveable = true;
		//this.windowFlagsToSet = Window.FLAG_MAXIMIZE;

		// set dialog-width according to fontsize
		int sw = MyLocale.getScreenWidth();
		int sh = MyLocale.getScreenHeight();
		if((pref.fontSize <= 13)||(sw <= 240)||(sh <= 240)){
			setPreferredSize(240,240);
		}
		else if(pref.fontSize <= 17){
			setPreferredSize(300,250);
		}
		else if(pref.fontSize <= 21){
			setPreferredSize(350,300);
		}
		else if(pref.fontSize <= 24){
			setPreferredSize(400,350);
		}
		else if(pref.fontSize <= 28){
			setPreferredSize(450,400);
		}
		else{
			setPreferredSize(500,450);
		}
		
		//scp = new ScrollBarPanel(pnlGeneral);
		
		/////////////////////////////////////////////////////////
		// First panel - General
		/////////////////////////////////////////////////////////
		Frame frmDataDir=new Frame();
		frmDataDir.borderStyle=UIConstants.BDR_RAISEDOUTER|UIConstants.BDR_SUNKENINNER|UIConstants.BF_BOTTOM;
		frmDataDir.addNext(new mLabel(MyLocale.getMsg(603,"Data Directory:")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		//frmDataDir.setTag(INSETS,new Insets(10,10,10,10));
		frmDataDir.addLast(brwBt = new mButton(MyLocale.getMsg(604,"Browse")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.EAST));
		DataDir = new mInput();
		DataDir.setText(pref.baseDir);
		frmDataDir.addLast(DataDir.setTag(CellConstants.SPAN, new Dimension(3,1)),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.EAST));
		frmDataDir.addLast(chkAutoLoad = new mCheckBox(MyLocale.getMsg(629,"Autoload last profile")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		if (pref.autoReloadLastProfile) chkAutoLoad.setState(true);
		chkAutoLoad.setTag(INSETS,new Insets(0,0,2,0));
		pnlGeneral.addLast(frmDataDir,HSTRETCH,HFILL);
		
		CellPanel pnlBrowser=new CellPanel();
		pnlBrowser.setTag(INSETS,new Insets(2,0,0,0));
		pnlBrowser.addNext(new mLabel("Browser:"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		Browser = new mInput();
		Browser.setText(pref.browser);
		pnlBrowser.addLast(Browser,CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));

		pnlBrowser.addNext(new mLabel(MyLocale.getMsg(601,"Your Alias:")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		Alias = new mInput();
		Alias.setText(pref.myAlias);
		pnlBrowser.addNext(Alias,CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		pnlBrowser.addNext(new mLabel(MyLocale.getMsg(594,"Pwd")));
		pnlBrowser.addLast(inpPassword=new mInput(pref.password),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		inpPassword.setToolTip(MyLocale.getMsg(593,"Password is optional here.\nEnter only if you want to store it in pref.xml"));
		inpPassword.isPassword=true;
		pnlGeneral.addLast(pnlBrowser,HSTRETCH,HFILL);
		
		pnlGeneral.addNext(gpsB = new mButton("GPS"),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		//content.addNext(Alias.setTag(Control.SPAN, new Dimension(3,1)),content.DONTSTRETCH, (content.HFILL|content.WEST));
		pnlGeneral.addLast(inpGPS=new mInput(""));
		inpGPS.modify(ControlConstants.Disabled|ControlConstants.NoFocus,0);
		inpGPS.setText(pref.mySPO.portName+"/"+pref.mySPO.baudRate);
		
		// Garmin and GPSBabel
		frmGarmin.addNext(lblGarmin=new mLabel(MyLocale.getMsg(173,"Garmin:  PC Port:")),DONTSTRETCH,LEFT);
		lblGarmin.setTag(INSETS,new Insets(4,0,0,0));
		frmGarmin.addNext(chcGarminPort=new mChoice(garminPorts,0),DONTSTRETCH,LEFT);
		chcGarminPort.setTag(INSETS,new Insets(4,0,0,0));
		chcGarminPort.selectItem(pref.garminConn);
		frmGarmin.addLast(chkSynthShort=new mCheckBox(MyLocale.getMsg(174,"Short Names")),STRETCH,RIGHT);
		chkSynthShort.setTag(INSETS,new Insets(4,0,0,0));
		chkSynthShort.setState(!pref.garminGPSBabelOptions.equals(""));
		frmGarmin.borderStyle=UIConstants.BDR_RAISEDOUTER|UIConstants.BDR_SUNKENINNER|UIConstants.BF_TOP;
		frmGarmin.setTag(INSETS,new Insets(4,0,0,0));
		pnlGeneral.addLast(frmGarmin);
		pnlGeneral.addLast(new mLabel(""));
		
		/////////////////////////////////////////////////////////
		// Second panel - Screen
		/////////////////////////////////////////////////////////
		
		Frame frmScreen=new Frame();
		frmScreen.borderStyle=UIConstants.BDR_RAISEDOUTER|UIConstants.BDR_SUNKENINNER;
		CellPanel pnlScreen=new CellPanel();
		pnlScreen.addNext(new mLabel(MyLocale.getMsg(625,"Screen (needs restart):")));
		pnlScreen.addNext(new mLabel("Font"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		pnlScreen.addLast(fontSize = new mInput(),CellConstants.DONTSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		fontSize.maxLength=2;
		fontSize.setPreferredSize(40,-1);
		frmScreen.addLast(pnlScreen,HSTRETCH,HFILL);
		fontSize.setText(Convert.toString(pref.fontSize));
		
		frmScreen.addLast(chkHasCloseButton=new mCheckBox(MyLocale.getMsg(631,"PDA has close Button")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));	
    	//lblTitle.setTag(INSETS,new Insets(2,0,0,0));
        chkHasCloseButton.setState(pref.hasCloseButton);
		frmScreen.addNext(chkMenuAtTop = new mCheckBox(MyLocale.getMsg(626,"Menu top")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		chkMenuAtTop.setTag(INSETS,new Insets(0,0,2,0));
		chkMenuAtTop.setState(pref.menuAtTop);
		frmScreen.addNext(chkTabsAtTop = new mCheckBox(MyLocale.getMsg(627,"Tabs top")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		chkTabsAtTop.setState(pref.tabsAtTop);
		chkTabsAtTop.setTag(INSETS,new Insets(0,0,2,0));
		frmScreen.addLast(chkShowStatus = new mCheckBox(MyLocale.getMsg(628,"Status")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		chkShowStatus.setState(pref.showStatus);
		chkShowStatus.setTag(INSETS,new Insets(0,0,2,0));
		pnlDisplay.addLast(frmScreen,CellConstants.HSTRETCH,CellConstants.FILL);
		
		Frame frmImages=new Frame();
		frmImages.borderStyle=UIConstants.BDR_RAISEDOUTER|UIConstants.BDR_SUNKENINNER|UIConstants.BF_TOP|UIConstants.BF_BOTTOM;
		//frmImages.addNext(new mLabel(MyLocale.getMsg(623,"Images:")),CellConstants.VSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		frmImages.addLast(chkShowDeletedImg = new mCheckBox(MyLocale.getMsg(624,"Show deleted images")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		chkShowDeletedImg.setTag(INSETS,new Insets(2,0,0,0));
		if (pref.showDeletedImages) chkShowDeletedImg.setState(true);
		//mLabel dummy;
		//frmImages.addNext(dummy=new mLabel(""),CellConstants.VSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST|CellConstants.NORTH));
		//dummy.setTag(INSETS,new Insets(0,0,2,0));
		frmImages.addLast(chkDescShowImg = new mCheckBox(MyLocale.getMsg(638,"Show pictures in description")),CellConstants.VSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST|CellConstants.NORTH));
		chkDescShowImg.setTag(INSETS,new Insets(0,0,2,0));
		if (pref.descShowImg) chkDescShowImg.setState(true);
		pnlDisplay.addLast(frmImages,CellConstants.STRETCH,CellConstants.FILL);

		Frame frmHintLog=new Frame();
		//frmHintLog.borderStyle=CellPanel.BDR_RAISEDOUTER|CellPanel.BDR_SUNKENINNER|CellPanel.BF_BOTTOM;
		frmHintLog.addNext(new mLabel(MyLocale.getMsg(630,"HintLogPanel:  Logs per page ")),CellConstants.DONTSTRETCH,CellConstants.DONTFILL);	
		frmHintLog.addLast(inpLogsPerPage=new mInput(),CellConstants.DONTSTRETCH,CellConstants.DONTFILL|CellConstants.EAST);
		inpLogsPerPage.setText(Convert.toString(pref.logsPerPage));
		inpLogsPerPage.setPreferredSize(40,-1);
		//inpLogsPerPage.setTag(INSETS,new Insets(0,0,2,0));
		//lblHlP.setTag(INSETS,new Insets(6,0,2,0));
		frmHintLog.addNext(new mLabel(MyLocale.getMsg(633,"Max. logs to spider")),CellConstants.DONTSTRETCH,CellConstants.DONTFILL);	
		frmHintLog.addLast(inpMaxLogsToSpider=new mInput(),CellConstants.DONTSTRETCH,CellConstants.DONTFILL|CellConstants.EAST);
		inpMaxLogsToSpider.setText(Convert.toString(pref.maxLogsToSpider));
		inpMaxLogsToSpider.setPreferredSize(40,-1);
		pnlDisplay.addLast(frmHintLog,CellConstants.STRETCH,CellConstants.FILL);

		/////////////////////////////////////////////////////////
		// Third panel - More
		/////////////////////////////////////////////////////////
		CellPanel pnlProxy=new CellPanel();
		pnlProxy.addNext(new mLabel("Proxy"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		pnlProxy.addLast(Proxy = new mInput(),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST)).setTag(SPAN,new Dimension(2,1));
		Proxy.setText(pref.myproxy);
		pnlProxy.addNext(new mLabel("Port"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		pnlProxy.addLast(ProxyPort = new mInput(),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		ProxyPort.setText(pref.myproxyport);
		pnlProxy.addNext(new mLabel(""),HSTRETCH,HFILL);
		pnlProxy.addLast(chkProxyActive=new mCheckBox(MyLocale.getMsg(634,"use Proxy")));
		chkProxyActive.setState(pref.proxyActive);
		pnlMore.addLast(pnlProxy,HSTRETCH,HFILL);
		pnlMore.addNext(new mLabel(MyLocale.getMsg(592,"Language (needs restart)")),DONTSTRETCH,DONTFILL|WEST);
		String[] tmp = (new FileBugfix(FileBase.getProgramDirectory()+"/languages").list("*.cfg", FileBase.LIST_FILES_ONLY)); //"*.xyz" doesn't work on some systems -> use FileBugFix
		if (tmp == null) tmp = new String[0];
		String [] langs = new String[tmp.length +1];
		langs[0] = "auto";
		int curlang = 0;
		for (int i = 0; i < tmp.length; i++) {
			langs[i+1] = tmp[i].substring(0, tmp[i].lastIndexOf('.'));
			if (langs[i+1].equalsIgnoreCase(MyLocale.language)) curlang = i+1 ;
		}
		//ewe.sys.Vm.copyArray(tmp, 0, langs, 1, tmp.length);
		pnlMore.addLast(inpLanguage=new mChoice(langs, curlang),DONTSTRETCH,DONTFILL|WEST);
		//inpLanguage.setPreferredSize(20,-1);
		inpLanguage.setToolTip(MyLocale.getMsg(591,"Select \"auto\" for system language or select your preferred language, e.g. DE or EN"));
		
		/////////////////////////////////////////////////////////
		// Fourth/Fifth panel - Listview and Travelbugs
		/////////////////////////////////////////////////////////

        mTab.addCard(pnlGeneral,MyLocale.getMsg(621,"General"),null);
		mTab.addCard(pnlDisplay,MyLocale.getMsg(622,"Screen"),null);
		mTab.addCard(pnlMore,MyLocale.getMsg(632,"More"),null);
		mTab.addCard(tccList=new TableColumnChooser(new String[] {
				MyLocale.getMsg(599,"checkbox"),
				MyLocale.getMsg(598,"type"),
				MyLocale.getMsg(606,"Difficulty"),
				MyLocale.getMsg(607,"Terrain"),
				MyLocale.getMsg(597,"waypoint"),
				MyLocale.getMsg(596,"name"),
				MyLocale.getMsg(608,"Location"),
				MyLocale.getMsg(609,"Owner"),
				MyLocale.getMsg(610,"Hidden"),
				MyLocale.getMsg(611,"Status"),
				MyLocale.getMsg(612,"Distance"),
				MyLocale.getMsg(613,"Bearing"),
				MyLocale.getMsg(635,"Size"),
				MyLocale.getMsg(636,"OC Empfehlungen"),
				MyLocale.getMsg(637,"OC Index")},pref.listColMap),MyLocale.getMsg(595,"List"),null);

		mTab.addCard(tccBugs=new TableColumnChooser(new String[] {
				MyLocale.getMsg(6000,"Guid"),
				MyLocale.getMsg(6001,"Name"),
				MyLocale.getMsg(6002,"track#"),
				MyLocale.getMsg(6003,"Mission"),
				MyLocale.getMsg(6004,"From Prof"),
				MyLocale.getMsg(6005,"From Wpt"),
				MyLocale.getMsg(6006,"From Date"),
				MyLocale.getMsg(6007,"From Log"),
				MyLocale.getMsg(6008,"To Prof"),
				MyLocale.getMsg(6009,"To Wpt"),
				MyLocale.getMsg(6010,"To Date"),
				MyLocale.getMsg(6011,"To Log")},pref.travelbugColMap),"T-bugs",null);
		
		this.addLast(mTab);
		addNext(cancelB = new mButton(MyLocale.getMsg(614,"Cancel")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		addLast(applyB = new mButton(MyLocale.getMsg(615,"Apply")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
	}
	
	public void onEvent(Event ev){
		if (ev instanceof ControlEvent && ev.type == ControlEvent.PRESSED){
			if (ev.target == cancelB){
				this.close(0);
			}
			if (ev.target == applyB){
				//if (pref.currProfile == 0){
					//pref.curCentrePt.set(btnCentre.getText());
					pref.baseDir = DataDir.getText();
				//}
				pref.fontSize = Convert.toInt(fontSize.getText());
				if (pref.fontSize<6) pref.fontSize=11;
				pref.logsPerPage=Common.parseInt(inpLogsPerPage.getText());
				if (pref.logsPerPage==0) pref.logsPerPage=pref.DEFAULT_LOGS_PER_PAGE;
				pref.maxLogsToSpider=Common.parseInt(inpMaxLogsToSpider.getText());
				if (pref.maxLogsToSpider==0) pref.maxLogsToSpider=pref.DEFAULT_MAX_LOGS_TO_SPIDER;
				
				Font defaultGuiFont = mApp.findFont("gui");
				int sz = (pref.fontSize);
				Font newGuiFont = new Font(defaultGuiFont.getName(), defaultGuiFont.getStyle(), sz); 
				mApp.addFont(newGuiFont, "gui"); 
				mApp.fontsChanged();
				mApp.mainApp.font = newGuiFont;
				
				pref.myAlias = Alias.getText().trim();
				SpiderGC.passwort=pref.password= inpPassword.getText().trim();
				MyLocale.saveLanguage(MyLocale.language=inpLanguage.getText().toUpperCase().trim());
				pref.browser = Browser.getText();
				//Vm.debug(myPreferences.browser);
				pref.myproxy = Proxy.getText();
				pref.myproxyport = ProxyPort.getText();
				pref.proxyActive=chkProxyActive.getState();
				HttpConnection.setProxy(pref.myproxy, Common.parseInt(pref.myproxyport), pref.proxyActive); // TODO generate an error message if proxy port is not a number
				//myPreferences.nLogs = Convert.parseInt(nLogs.getText());
				pref.autoReloadLastProfile=chkAutoLoad.getState();
				pref.showDeletedImages=chkShowDeletedImg.getState();
				pref.garminConn=chcGarminPort.getSelectedItem().toString();
				pref.garminGPSBabelOptions=chkSynthShort.state?"-s":"";
				pref.menuAtTop=chkMenuAtTop.getState();
				pref.tabsAtTop=chkTabsAtTop.getState();
				pref.showStatus=chkShowStatus.getState();
				pref.hasCloseButton=chkHasCloseButton.getState();
				pref.travelbugColMap=tccBugs.getSelectedCols();
				pref.listColMap=tccList.getSelectedCols();
				pref.descShowImg=chkDescShowImg.getState();
				Global.mainTab.tbP.myMod.setColumnNamesAndWidths();
				pref.savePreferences();
				pref.dirty = true; // Need to update table in case columns were enabled/disabled
				this.close(0);
			}
			if(ev.target == brwBt){
				FileChooser fc = new FileChooser(FileChooserBase.DIRECTORY_SELECT, pref.baseDir);
				fc.setTitle(MyLocale.getMsg(616,"Select directory"));
				if(fc.execute() != FormBase.IDCANCEL)	DataDir.setText(fc.getChosen()+"/");
			}
			if (ev.target == gpsB){
				GPSPortOptions spo = new GPSPortOptions();
				spo.portName = pref.mySPO.portName;
				spo.baudRate = pref.mySPO.baudRate;
				Editor s = spo.getEditor(SerialPortOptions.ADVANCED_EDITOR);
				spo.forwardGpsChkB.setState(pref.forwardGPS);
				spo.inputBoxForwardHost.setText(pref.forwardGpsHost);
				spo.logGpsChkB.setState(pref.logGPS);
				spo.inputBoxLogTimer.setText(pref.logGPSTimer);
				Gui.setOKCancel(s);
				if (s.execute()== FormBase.IDOK) {
					pref.mySPO.portName = spo.portName; 
					pref.mySPO.baudRate = spo.baudRate;
					pref.forwardGPS = spo.forwardGpsChkB.getState();
					pref.forwardGpsHost = spo.inputBoxForwardHost.getText();
					pref.logGPS = spo.logGpsChkB.getState();
					pref.logGPSTimer = spo.inputBoxLogTimer.getText();
					inpGPS.setText(pref.mySPO.portName+"/"+pref.mySPO.baudRate);
				}
			}
			// change destination waypoint
			/*if (ev.target == btnCentre){
				CoordsScreen cs = new CoordsScreen();
				cs.setFields(pref.curCentrePt, CWPoint.CW);
				if (cs.execute()== CoordsScreen.IDOK){
					pref.curCentrePt.set(cs.getCoords());
					btnCentre.setText(pref.curCentrePt.toString(CWPoint.CW));
				}
			}
			*/
		}
		super.onEvent(ev);
	}
	
}
