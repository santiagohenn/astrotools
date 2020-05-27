package astrotools.structures;

import java.util.LinkedHashMap;

public class Ephemerides {
	
	private LinkedHashMap<String,Ephemeris> ephemerides = new LinkedHashMap<String,Ephemeris>();
	private double step = 1;
	
	public Ephemerides(){
		
	}
	
	public void setStep(double s){
		this.step = s;
	}
	public double getStep(){
		return this.step;
	}
	
	public int size(){
		return ephemerides.size();
	}
	
	public LinkedHashMap<String,Ephemeris> getMap(){
		return this.ephemerides;
	}
	
	public Ephemeris getEphemerisAt(String key){
		return ephemerides.get(key);
	}
	
	public void setEphemerisAt(String key,Ephemeris eph){
		ephemerides.put(key, eph);
	}
	
	public void clear(){
		if (!ephemerides.isEmpty()) ephemerides.clear();
	}
	
	public LinkedHashMap<String,TEME> getTEMEseries(){
		LinkedHashMap<String,TEME> temeSeries = new LinkedHashMap<String,TEME>();
		for (String key : ephemerides.keySet()){
			temeSeries.put(key, ephemerides.get(key).getTEME());
		}
		return temeSeries;
	}
	
	public LinkedHashMap<String,ECEF> getECEFseries(){
		LinkedHashMap<String,ECEF> temeSeries = new LinkedHashMap<String,ECEF>();
		for (String key : ephemerides.keySet()){
			temeSeries.put(key, ephemerides.get(key).getECEF());
		}
		return temeSeries;
	}
	
	public LinkedHashMap<String,LLA> getLLAseries(){
		LinkedHashMap<String,LLA> temeSeries = new LinkedHashMap<String,LLA>();
		for (String key : ephemerides.keySet()){
			temeSeries.put(key, ephemerides.get(key).getLLA());
		}
		return temeSeries;
	}
	
	public LinkedHashMap<String,AER> getAERseries(){
		LinkedHashMap<String,AER> temeSeries = new LinkedHashMap<String,AER>();
		for (String key : ephemerides.keySet()){
			temeSeries.put(key, ephemerides.get(key).getAER());
		}
		return temeSeries;
	}

}
