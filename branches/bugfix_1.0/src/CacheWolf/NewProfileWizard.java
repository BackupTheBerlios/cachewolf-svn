package CacheWolf;

import ewe.ui.FormBase;
import ewe.ui.Gui;

public class NewProfileWizard {

	static public boolean startNewProfileWizard(ewe.ui.Frame parent) {
		if (Global.mainTab != null) Global.mainTab.saveUnsavedChanges(true);
		Preferences pref = Global.getPref();
		NewProfileForm f=new NewProfileForm(pref.baseDir);
		int code=f.execute(parent, Gui.CENTER_FRAME);
		if (code==0) {
			Profile profile = Global.getProfile();
			profile.clearProfile(); 
			pref.lastProfile=profile.name=f.profileDir;
			pref.savePreferences(); // Remember that this was the last profile used
			profile.dataDir=pref.baseDir+f.profileDir+"/";
			
			CoordsScreen cs = new CoordsScreen();
			cs.setFields(new CWPoint(), CWPoint.CW);
			if (cs.execute() == FormBase.IDOK) {
				profile.centre.set(cs.getCoords());
				profile.hasUnsavedChanges=true;
			}
			Global.mainForm.setTitle("Cachewolf "+Version.getRelease()+" - "+profile.name);
		}
		f.close(0);
		return (code == 0);
	}
}
