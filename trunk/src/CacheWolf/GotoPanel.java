package CacheWolf;

import ewe.ui.*;
import ewe.util.Vector;
import ewe.util.mString;
import ewe.fx.*;
import ewe.io.*;
import ewe.net.Socket;
//import ewe.io.IOException;
//import ewe.io.SerialPort;
//import ewe.io.SerialPortOptions;
import ewe.sys.*;
import ewe.sys.Double;


/**
 * Thread for reading data from COM-port
 *
 */
class SerialThread extends mThread{
	SerialPort comSp;   
	byte[] comBuff = new byte[1024];  
	int comLength = 0;
	CWGPSPoint myGPS;
	boolean run, tcpForward;
	Socket tcpConn;
	String lastError = new String();

	public SerialThread(SerialPortOptions spo, CWGPSPoint GPSPoint, String forwardIP) throws IOException {
		try{
			comSp = new SerialPort(spo);
		} catch (IOException e) {
			throw new IOException(spo.portName);
		} // catch (UnsatisfiedLinkError e) {} // TODO in original java-vm 
		if (forwardIP.length()>0) { 
			try {
				tcpConn = new Socket(forwardIP, 23);
				tcpForward = true;
			} catch (ewe.net.UnknownHostException e) { tcpForward = false; lastError = e.getMessage();
			} catch (IOException e) { tcpForward = false; lastError = e.getMessage(); 
			}
		}
		myGPS = GPSPoint;
	}

	public void run() {
		int noData = 0;
		int notinterpreted = 0;
		run = true;
		while (run){
			try {
				sleep(1000);
				//Vm.debug("Loop? " + noData);
				noData++;
				if (noData > 5) { myGPS.noDataError(); }
			} catch (InterruptedException e) {}
			if (comSp != null)	{
				comLength = comSp.nonBlockingRead(comBuff, 0 ,comBuff.length);
				//Vm.debug("Length: " + comBuff.length);
				if (comLength > 0)	{
					noData = 0;
					String str = mString.fromAscii(comBuff, 0, comLength); 
					if (tcpForward) {
						try {
							tcpConn.write(comBuff, 0, comLength);
						} catch (IOException e) { tcpForward = false; }
					}
					//Vm.debug(str);
					if (myGPS.examine(str)) notinterpreted = 0; else notinterpreted++;
					if (notinterpreted > 22) myGPS.noInterpretableData();
				}
			}
		} // while
		myGPS.noData();
		tcpConn.close();
	}

	public void stop() {
		run = false;
		if (comSp != null) comSp.close();
	}
}

/** 
 * Class for creating a new mThread to create timer ticks to be able to do form.close in the ticked-thread. 
 * Using the Vm.requestTimer-Method causes "ewe.sys.EventDirectionException: This task cannot be done within 
 * a Timer Tick." in the ewe-vm when form.close is called.  
 */

class UpdateThread extends mThread {
	public boolean run;
	public int calldelay;
	public GotoPanel ticked;

	public UpdateThread (GotoPanel gp, int cd) {
		ticked = gp;
		calldelay = cd;
	}

	public void run () {
		run = true;
		while (run) {
			try { sleep (calldelay);} catch (InterruptedException e) {}
			ticked.ticked();
		}
	}

	public void stop() {
		run = false;
	}
}


/**
 *	Class to create the panel which handles the connection to the GPS-device<br>
 *	Displays: current position,speed and bearing; relation to destination waypoint<br>
 *	Class ID: 1500
 */


public class GotoPanel extends CellPanel {

	public CWGPSPoint gpsPosition = new CWGPSPoint();
	public CWPoint toPoint = new CWPoint();

	mButton btnGPS, btnCenter,btnSave;
	mButton btnGoto, btnMap;
	mCheckBox chkDMM, chkDMS, chkDD, chkUTM;
	CheckBoxGroup chkFormat = new CheckBoxGroup();
	int currFormat;

	mLabel lblPosition, lblSats, lblSpeed, lblBearMov, lblBearWayP, lblDist, lblHDOP;
	mLabel lblSatsText, lblSpeedText, lblDirText, lblDistText, lblSunAzimut;
	mLabel lblGPS, lblDST, lblCurr, lblWayP;
	mLabel lblLog;
	mCheckBox chkLog;
	mInput inpLogSeconds;

	MainTab mainT;
	Vector cacheDB;
	DetailsPanel detP;

	Preferences pref;
	Profile profile;
	// different panels to avoid spanning
	CellPanel FormatP = new CellPanel();
	CellPanel ButtonP = new CellPanel();
	CellPanel CoordsP = new CellPanel();
	CellPanel roseP = new CellPanel();
	CellPanel GotoP = new CellPanel();
	CellPanel LogP = new CellPanel();

	SerialThread serThread;
	UpdateThread tickerThread;

	ImageControl ic; 

	static Color RED = new Color(255,0,0);
	static Color YELLOW = new Color(255,255,0);
	static Color GREEN = new Color(0,255,0);
	static Color BLUE = new Color(0,255,255);

	static Font BOLD = new Font("Arial", Font.BOLD, 14);

	int centerX, centerY;

	int ticker = 0;

	boolean mapsLoaded = false;
	public boolean runMovingMap = false;
	Vector availableMaps = new Vector();
	MapInfoObject tempMIO = new MapInfoObject();
	MovingMap mmp;
	Track currTrack;

	/**
	 * Create GotoPanel 
	 * @param Preferences 	global preferences
	 * @param MainTab		reference to MainTable
	 * @param DetailsPanel 	reference to DetailsPanel
	 * @param Vector		cacheDB
	 */
	public GotoPanel(Preferences p, Profile prof, MainTab mt, DetailsPanel dp)
	{
		pref = p;
		profile=prof;
		mainT = mt;
		detP = dp;
		cacheDB = profile.cacheDB;

		// Button
		ButtonP.addNext(btnGPS = new mButton("Start"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		ButtonP.addNext(btnCenter = new mButton(MyLocale.getMsg(309,"Center")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		ButtonP.addNext(btnSave = new mButton(MyLocale.getMsg(311,"Create Waypoint")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		ButtonP.addLast(btnMap = new mButton("Map"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));

		//Format selection for coords
		FormatP.addNext(chkDD =new mCheckBox("d.d�"),CellConstants.DONTSTRETCH, CellConstants.WEST);
		FormatP.addNext(chkDMM =new mCheckBox("d�m.m\'"),CellConstants.DONTSTRETCH, CellConstants.WEST);
		FormatP.addNext(chkDMS =new mCheckBox("d�m\'s\""),CellConstants.DONTSTRETCH,CellConstants.WEST);
		FormatP.addLast(chkUTM =new mCheckBox("UTM"),CellConstants.DONTSTRETCH, CellConstants.WEST);

		chkDD.setGroup(chkFormat);
		chkDMM.setGroup(chkFormat);
		chkDMS.setGroup(chkFormat);
		chkUTM.setGroup(chkFormat);
		currFormat = CWPoint.DMM;
		chkFormat.selectIndex(currFormat);

		//Coords
		CoordsP.addNext(lblGPS = new mLabel("GPS: "),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		lblGPS.backGround = RED;
		CoordsP.addLast(lblPosition = new mLabel(gpsPosition.toString(currFormat)),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		CoordsP.addNext(lblDST = new mLabel(MyLocale.getMsg(1500,"DST:")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		lblDST.backGround = BLUE;
		CoordsP.addLast(btnGoto = new mButton(toPoint.toString(currFormat)),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));

		//Rose for bearing
		Image img = new Image("rose.png");
		ic = new ImageControl(img);
		centerY = img.getHeight() / 2;
		centerX = img.getWidth() / 2;
		roseP.addLast(ic,CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.NORTH));

		//Goto
		//things from GPS
		GotoP.addLast(lblCurr = new mLabel(MyLocale.getMsg(1501,"Current")),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		lblCurr.backGround = RED;
		lblCurr.font = BOLD;

		//GotoP.addNext(lblSatsText = new mLabel("Sats: "),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		//lblSatsText.font = BOLD;
		GotoP.addLast(lblSats = new mLabel("Sats: " + Convert.toString(gpsPosition.getSats())),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblSats.font = BOLD;
		GotoP.addLast(lblHDOP = new mLabel("HDOP: " + Convert.toString(gpsPosition.getHDOP())),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblHDOP.font = BOLD;


		GotoP.addLast(lblSpeed = new mLabel(Convert.toString(gpsPosition.getSpeed())),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblSpeed.font = BOLD;

		GotoP.addLast(lblBearMov = new mLabel("0"),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblBearMov.font = BOLD;

		//things about destination
		GotoP.addLast(lblWayP = new mLabel("WayPoint"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		lblWayP.backGround = BLUE;
		lblWayP.font = BOLD;
		GotoP.addLast(lblBearWayP = new mLabel("0"),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblBearWayP.font = BOLD;

		GotoP.addLast(lblDist = new mLabel("0"),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.WEST));
		lblDist.font = BOLD;

		LogP.addNext(lblLog = new mLabel("Log "),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		LogP.addNext(chkLog = new mCheckBox(),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		LogP.addNext(inpLogSeconds = new mInput("10"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		LogP.addLast(new mLabel("sec"),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));

		chkLog.useCross = true;
		chkLog.setState(false);
		inpLogSeconds.columns = 5;

		LogP.addNext(lblGPS = new mLabel("Sonne: "),CellConstants.DONTSTRETCH, (CellConstants.DONTFILL|CellConstants.WEST));
		lblGPS.backGround = YELLOW;
		lblGPS.setTag(SPAN, new Dimension(2,1));

		LogP.addLast(lblSunAzimut = new mLabel("---"),CellConstants.HSTRETCH, (CellConstants.HFILL|CellConstants.NORTH));
		lblSunAzimut.setText("---");
		lblSunAzimut.font = BOLD;


		//add Panels
		this.addLast(ButtonP,CellConstants.DONTSTRETCH, CellConstants.DONTFILL|CellConstants.WEST).setTag(SPAN,new Dimension(2,1));
		this.addLast(FormatP,CellConstants.DONTSTRETCH, CellConstants.DONTFILL|CellConstants.WEST).setTag(SPAN,new Dimension(2,1));
		this.addLast(CoordsP,CellConstants.HSTRETCH, CellConstants.HFILL|CellConstants.NORTH).setTag(SPAN,new Dimension(2,1));
		this.addNext(roseP,CellConstants.DONTSTRETCH, CellConstants.DONTFILL|CellConstants.WEST).setTag(SPAN,new Dimension(1,1));
		this.addLast(GotoP,CellConstants.HSTRETCH, CellConstants.HFILL|CellConstants.NORTHWEST).setTag(SPAN,new Dimension(1,2));
		this.addLast(LogP,CellConstants.DONTSTRETCH, CellConstants.DONTFILL|CellConstants.NORTHWEST).setTag(SPAN,new Dimension(1,1));

		// for debuging
		/*		CWGPSPoint myGPS;
		myGPS = new CWGPSPoint();
		String ex = new String();
		boolean test = false;
		try { FileReader f = new FileReader("c:\\kw\\20060928_1051.log");
		ex = f.readAll();
		test = myGPS.examine(ex);
		f.close();
		} catch (IOException e ) {
			Vm.debug(e.toString());
		}
		test = myGPS.examine("$GPGLL,5226.8935,N,01338.5327,E,084635.00,A,D*6E");
		test = myGPS.examine("$GPGSA,A,3,10,28,26,29,09,,,,,,,,04.1,02.6,03.2*0F");
		test = myGPS.examine("$GPGSV,4,1,13,08,40,072,28,10,29,201,47,27,15,079,29,28,62,102,44*7E");
		test = myGPS.examine("$GPGSV,4,2,13,29,72,289,38,26,63,296,41,09,12,259,35,18,14,324,*79");
		test = myGPS.examine("$GPGSV,4,3,13,19,09,025,,17,06,138,,21,06,300,,37,29,171,40*7A");
		test = myGPS.examine("$GPGSV,4,4,13,39,29,166,38*40");
		 */

		//while (true){
//		int notinterpreted = 0;
//		if (myGPS.examine("@��H @��H�@��HH�������H")) { notinterpreted = 0;} else notinterpreted++;
//		if (notinterpreted > 5) myGPS.noInterpretableData();
//		// myGPS.noInterpretableData();


//		}	
	}

	/**
	 * draw arrows for the directions of movement and destination waypoint
	 * @param ctrl the control to paint on
	 * @param moveDir degrees of movement
	 * @param destDir degrees of destination waypoint
	 */

	private void drawArrows(Control ctrl,double moveDir, double destDir, double sunAziumt){
		Graphics g = ctrl.getGraphics();

		if (g != null) {
			ctrl.repaintNow();
			// draw only valid arrows
			if (moveDir < 360 && moveDir > -360) drawArrow(g, moveDir, RED);
			if (destDir < 360 && destDir > -360) drawArrow(g, destDir, BLUE);
			if (sunAziumt < 360 && sunAziumt > -360) drawArrow(g, sunAziumt, YELLOW);
			g.free();
		}

	}

	/**
	 * draw single arrow 
	 * @param g handle for drawing
	 * @param angle angle of arrow
	 * @param col color of arrow
	 */
	private void drawArrow(Graphics g, double angle, Color col) {
		double angleRad;
		int x, y;

		angleRad = angle * java.lang.Math.PI / 180;
		x = centerX + new Float(centerX * java.lang.Math.sin(angleRad)).intValue();
		y = centerY - new Float(centerY * java.lang.Math.cos(angleRad)).intValue();
		g.setPen(new Pen(col,Pen.SOLID,3));
		g.drawLine(centerX,centerY,x,y);

	}

	/**
	 * set the coords of the destination  
	 * @param dest destination
	 */ 
	public void setDestination(CWPoint dest){
		toPoint.set(dest);
		btnGoto.setText(toPoint.toString(currFormat));
		mainT.select(this);
	}

	/**
	 * set the coords of the destination  
	 * @param LatLon destination
	 */ 
	public void setDestination(String LatLon) {
		toPoint.set(LatLon,CWPoint.CW);
		btnGoto.setText(toPoint.toString(currFormat));
		mainT.select(this);
	}

	/**
	 * method which is called if a timer is set up  
	 */ 
	public void ticked() {
		Double bearMov = new Double();
		Double bearWayP = new Double();
		Double dist = new Double();
		Double speed = new Double();
		Double sunAzimut = new Double();
		Vm.debug("ticked: voher");

		//		Vm.debug("ticked");
		int fix = gpsPosition.getFix();
		lblSats.setText("Sats: " + Convert.toString(gpsPosition.getSats()));
		lblHDOP.setText("HDOP: " + Convert.toString(gpsPosition.getHDOP()));
		// display values only, if signal good
		if ((fix > 0) && (gpsPosition.getSats()>= 0)) {
			//gpsPosition.printAll();
			//Vm.debug("currTrack.add: voher");
			try {
				currTrack.add(gpsPosition);
			} catch (IndexOutOfBoundsException e) { // track full -> create a new one
				currTrack = new Track(RED); 
				currTrack.add(gpsPosition);
				if (mmp != null) mmp.addTrack(currTrack); // TODO maybe gotoPanel should also hold a list of Tracks, because otherwise they will be destroyed if not saved in mmp before
			}
			//Vm.debug("currTrack.add: nachher");
			lblPosition.setText(gpsPosition.toString(currFormat));
			speed.set(gpsPosition.getSpeed());
			lblSpeed.setText(MyLocale.formatDouble(speed,"0.0") + " km/h");
			try { 
				sunAzimut.set(getSunAzimut(gpsPosition.Time, gpsPosition.Date, gpsPosition.latDec, gpsPosition.lonDec));
				lblSunAzimut.setText(MyLocale.formatDouble(sunAzimut,"0.0") + " Grad");
			} catch (NumberFormatException e) { 
				// irgendeine Info zu Berechnung des Sonnenaziumt fehlt (insbesondere Datum und Uhrzeit sind nicht unbedingt gleichzeitig verf�gbar wenn es einen Fix gibt)
				sunAzimut.set(500); // any value out of range (bigger than 360) will prevent drawArrows from drawing it 
				lblSunAzimut.setText("---");
			}//sunAzimut.set(getSunAzimut("141303","130906", 50.744, 7.0935));

			bearMov.set(gpsPosition.getBear());
			lblBearMov.setText(bearMov.toString(0,0,0) + " Grad");
			bearWayP.set(gpsPosition.getBearing(toPoint));
			lblBearWayP.setText(bearWayP.toString(0,0,0) + " Grad");

			dist.set(gpsPosition.getDistance(toPoint));

			if (dist.value >= 1){
				lblDist.setText(MyLocale.formatDouble(dist,"0.000")+ " km");
			}
			else {
				dist.set(dist.value * 1000);
				lblDist.setText(dist.toString(3,0,0) + " m");
			}

			drawArrows(ic,bearMov.value,bearWayP.value, sunAzimut.value);

			// Set background to signal quality
			lblSats.backGround = GREEN;
		}

		// receiving data, but signal ist not good
		if ((fix == 0) && (gpsPosition.getSats()>= 0)) {
			lblSats.backGround = YELLOW;
		}
		// receiving no data
		if (fix == -1) {
			if (lblSats.backGround != RED) (new MessageBox("Error", "No data from GPS\nConnection to serial port closed",MessageBox.OKB)).exec();
			lblSats.backGround = RED;
			stopGPS();
		}
		// cannot interprete data
		if (fix == -2) {
			if (lblSats.backGround != RED) (new MessageBox("Error", "Cannot interpret data from GPS\n possible reasons:\n wrong Port,\n wrong Baudrate,\n not NMEA-Protocol\nConnection to serial port closed\nLast String tried to interprete:\n "+gpsPosition.lastStrExamined, MessageBox.OKB)).exec();
			lblSats.backGround = RED;
			stopGPS();
		}

		// In moving map mode
		if (mmp != null && runMovingMap ) { // neccessary in case of multi-threaded Java-VM: ticked could be called during load of mmp 
			if ((fix > 0) && (gpsPosition.getSats()>= 0)) {
				Vm.debug("ShowLastAddedPoint: voher");
				mmp.ShowLastAddedPoint(currTrack);
				Vm.debug("ShowLastAddedPoint: nachher");
				mmp.updatePosition(gpsPosition.latDec, gpsPosition.lonDec);
				mmp.setGpsStatus(MovingMap.gotFix);
			}
			if ((fix == 0) && (gpsPosition.getSats()== 0)) {
				mmp.setGpsStatus(MovingMap.lostFix);
			}
			if (fix < 0 ) {
				mmp.setGpsStatus(MovingMap.noGPSData);
			}
		}
	}

	public void startDisplayTimer() {
		tickerThread = new UpdateThread(this, 1000);
		tickerThread.start();
	}

	public void stopDisplayTimer(){
		if (tickerThread != null) tickerThread.stop();
	}

	/**
	 * @param utc in the format as it comes from gps DDMMYY
	 * @param datum in the format as it comes from gps HHMMSS
	 * @param lat in degrees in WGS84
	 * @param lon in degrees in WGS84
	 * @return Azimut of the sun in degrees from north
	 * @throws NumberFormatException when utc / datum could not be interpreted
	 */
	public double getSunAzimut (String utc, String datum, double lat, double lon) {
		//	(new MessageBox("test", "utc:"+utc+" datum: "+datum+", lat: "+lat+", len: "+lon, MessageBox.OKB)).exec();
		try {
			int tag, monat, jahr, stunde, minute, sekunde;
			tag = Convert.parseInt(datum.substring(0, 2));
			monat = Convert.parseInt(datum.substring(2, 4));
			jahr = Convert.parseInt(datum.substring(4, 6)) + 2000;
			stunde=Convert.parseInt(utc.substring(0, 2));
			minute=Convert.parseInt(utc.substring(2, 4));
			sekunde=Convert.parseInt(utc.substring(4, 6)); // Kommastellen werden abgeschnitten
			// julianisches "Datum" jd berechnen (see http://de.wikipedia.org/wiki/Julianisches_Datum )
			if (monat<2) {jahr--; monat+=12;} // verlegung des Jahres Endes auf Feb macht Berechnung von SChaltjahren einfacher
			double a = (int)java.lang.Math.floor((double)jahr/100.); // Alle hundert Jahre kein Schlatjahr (abrunden)
			double b = 2 - a + java.lang.Math.floor((double)a/4.);
			double jd = java.lang.Math.floor(365.25*(jahr + 4716.)) + java.lang.Math.floor(30.6001*((double)monat+1.)) + (double)tag + (double)stunde/24 + (double)minute/1440 + (double)sekunde/86400 + b - 1524.5;
			double jd0 = java.lang.Math.floor(365.25*(jahr + 4716.)) + java.lang.Math.floor(30.6001*((double)monat+1.)) +(double)tag + b - 1524.5;
			// Ekliptikalkoordinaten der Sonne berechnen (see http://de.wikipedia.org/wiki/Sonnenstand )
			double n = jd - 2451545.0;
			double l = 280.46 + 0.9856474 * n;
			double g = 357.528 + 0.9856003 * n;
			double d = l + 1.915*java.lang.Math.sin(g/180*java.lang.Math.PI) + 0.02 * java.lang.Math.sin(2*g/180*java.lang.Math.PI);
			// Rektaszension alpha und Deklination delta der Sonne berechnen
			double e = 23.439 -0.0000004 * n;
			double alphaNenner = java.lang.Math.cos(d/180*java.lang.Math.PI);
			double alpha = 180/java.lang.Math.PI*java.lang.Math.atan(java.lang.Math.cos(e/180*java.lang.Math.PI)*java.lang.Math.sin(d/180*java.lang.Math.PI)/alphaNenner);
			double delta = 180/java.lang.Math.PI*java.lang.Math.asin(java.lang.Math.sin(e/180*java.lang.Math.PI)*java.lang.Math.sin(d/180*java.lang.Math.PI) );
			if (alphaNenner<0) {alpha +=180;}
			// Azimut
			double t0 = (jd0 - 2451545.)/36525.; // schon in t0 bzw jd0 richtig berechnet?
			double thetaHG = 6.697376 + 2400.05134 * t0 + 1.002738 * ((double)stunde + (double)minute/60.);
			double theta = thetaHG * 15. + lon;
			double azimutNenner = java.lang.Math.cos((theta-alpha)/180*java.lang.Math.PI)*java.lang.Math.sin(lat/180*java.lang.Math.PI)-
			java.lang.Math.tan(delta/180*java.lang.Math.PI)*java.lang.Math.cos(lat/180*java.lang.Math.PI);
			double azimut = java.lang.Math.atan(java.lang.Math.sin((theta-alpha)/180*java.lang.Math.PI)/
					azimutNenner);
			azimut = azimut * 180. / java.lang.Math.PI;
			if (azimutNenner<0) azimut +=180.;
			// null = Sueden auf Null = Norden umrechnen
			azimut +=180.;
			if (azimut >360.) azimut -=360.;
			return azimut;
		} catch (IndexOutOfBoundsException e) {
			// wird von substring geworfen wenn datum / utc nicht genug Ziffern haben
			// NumberFormatException wird au�erdem von Convert.ParseInt direkt geworfen wenn
			// nicht in Int konvertiert werden kann
			throw new NumberFormatException();
		}
	}

	private void stopGPS() {
		serThread.stop();
		stopDisplayTimer();
		btnGPS.setText("Start");
		gpsPosition.stopLog();
		lblSats.backGround = this.backGround;
		if (mmp != null) mmp.setGpsStatus(MovingMap.noGPS);
		this.repaintNow(); // without this the change in the background color will not be displayed
		chkLog.modify(0,ControlConstants.Disabled);
	}

	public void startGps() {
		if (serThread != null) if (serThread.isAlive()) return;
		try {
			serThread = new SerialThread(pref.mySPO, gpsPosition, (pref.forwardGPS ? pref.forwardGpsHost : ""));
			if (pref.forwardGPS && !serThread.tcpForward) {
				(new MessageBox("Warning", "Ignoring error:\n could not forward GPS data to host:\n"+pref.forwardGpsHost+"\n"+serThread.lastError+"\nstop and start GPS to retry",MessageBox.OKB)).exec();
			}
			if (gpsPosition.latDec == 0 && gpsPosition.lonDec == 0) {
				gpsPosition.latDec = toPoint.latDec; // setze Zielpunkt als Ausgangspunkt
				gpsPosition.lonDec = toPoint.lonDec;
			}
			serThread.start();
			startDisplayTimer();
			if (chkLog.getState()){
				gpsPosition.startLog(profile.dataDir, Convert.toInt(inpLogSeconds.getText()), CWGPSPoint.LOGALL);
			}
			chkLog.modify(ControlConstants.Disabled,0);
			btnGPS.setText("Stop");
		} catch (IOException e) {
			(new MessageBox("Error", "Could not connect to GPS-receiver.\n Error while opening serial Port " + e.getMessage()+"\npossible reasons:\n Another (GPS-)program is blocking the port\nwrong port\nOn Loox: active infra-red port is blocking GPS", MessageBox.OKB)).execute(); 
		}
		currTrack = new Track(RED);
	}


	/**
	 * Eventhandler
	 */

	public void onEvent(Event ev){

		if(ev instanceof ControlEvent && ev.type == ControlEvent.PRESSED){
			// display coords in another format
			if (ev.target == chkFormat){
				currFormat = chkFormat.getSelectedIndex();
				lblPosition.setText(gpsPosition.toString(currFormat));
				btnGoto.setText(toPoint.toString(currFormat));
			}

			// start/stop GPS connection
			if (ev.target == btnGPS){
				if (btnGPS.getText().equals("Start")) startGps();
				else stopGPS();
			}

			// set current position as center and recalculate distance of caches in MainTab 
			if (ev.target == btnCenter){
				Vm.showWait(true);
				pref.curCentrePt.set(gpsPosition);
				mainT.updateBearDist();
				Vm.showWait(false);
				(new MessageBox("Info", "Entfernungen in der Listenansicht \nvom aktuellen Standpunkt aus \nneu berechnet", MessageBox.OKB)).execute();
			}
			//Start moving map
			if (ev.target == btnMap){
				runMovingMap = true;
				if (mmp != null && mmp.mmp.mapImage != null) {
					if (serThread == null || !serThread.isAlive() ) {
						// setze Zielpunkt als Ausgangspunkt, wenn GPS aus ist und lade entsprechende Karte
						mmp.ignoreGps = false;
						mmp.updatePosition(toPoint.latDec, toPoint.lonDec);
						mmp.ignoreGps = true;
					}
					if (currTrack != null) mmp.addTrack(currTrack);
					mmp.setGotoPosition(toPoint.latDec, toPoint.lonDec);
					mmp.exec();
				}
				else {
					if(mapsLoaded == false){
						Vm.showWait(true);
						InfoBox inf = new InfoBox("Info", "Loading list of maps...");
						inf.exec();
						String dateien[];
						String mapsPath = new String();
						mapsPath = File.getProgramDirectory() + "/maps/";
						File files = new File(mapsPath);
						Extractor ext;
						String rawFileName = new String();
						dateien = files.list("*.png", File.LIST_FILES_ONLY);
						for(int i = 0; i < dateien.length;i++){
							ext = new Extractor(dateien[i], "", ".", 0, true);
							rawFileName = ext.findNext();
							try {
								tempMIO = new MapInfoObject();
								tempMIO.loadwfl(mapsPath, rawFileName);
								availableMaps.add(tempMIO);
								mapsLoaded = true;
							}catch(IOException ex){ } // TODO etwas genauer auch Fehlermeldung ausgeben? Bei vorhandenen .wfl-Datei mit ung�ltigen Werten Fehler ausgeben oder wie jetz einfach ignorieren?
						}
						inf.close(0);
					} // if(mapsLoaded == false)
					mmp = new MovingMap(pref, availableMaps, this, cacheDB);
					Vm.showWait(false);
					if (serThread == null || !serThread.isAlive() ) {
						// setze Zielpunkt als Ausgangspunkt, wenn GPS aus ist und lade entsprechende Karte
						mmp.loadMap(toPoint.latDec, toPoint.lonDec);
					} else
						mmp.loadMap(gpsPosition.latDec, gpsPosition.lonDec);
					if (currTrack != null) mmp.addTrack(currTrack);
					mmp.setGotoPosition(toPoint.latDec, toPoint.lonDec);
					mmp.exec();
				}

			} // if (ev.target == btnMap
			// create new waypoint with current GPS-position
			if (ev.target == btnSave){
				CacheHolder ch = new CacheHolder();
				ch.LatLon = gpsPosition.toString();
				detP.newWaypoint(ch,mainT, pref, profile);
			}
			// change destination waypoint
			if (ev.target == btnGoto){
				CoordsScreen cs = new CoordsScreen();
				cs.setFields(toPoint, currFormat);
				if (cs.execute()== CoordsScreen.IDOK){
					toPoint = cs.getCoords();
					btnGoto.setText(toPoint.toString(currFormat));
				}
			}
		}
		super.onEvent(ev);
	}
}
