package simulation;

import astrotools.structures.*;

public class Facility {
	
	String id = "";
	LLA lla = new LLA(0,0,0);
	double visibilityTH = 0;
	
	public Facility(){
		this.id = "";
		this.lla = new LLA(0,0,0);
		this.visibilityTH = 0;
	}
	
	public Facility(String id, LLA pos, double th){
		this.id = id;
		this.lla = pos;
		this.visibilityTH = Math.toRadians(th);
	}
	
	public Facility(String id, double latDeg, double lonDeg, double alt, double visTH){
		this.id = id;
		this.lla = new LLA(latDeg,lonDeg,alt);
		this.visibilityTH = visTH;
	}
	
	public String getId(){
		return this.id;
	}
	
	public LLA getLLA(){
		return this.lla;
	}
	
	public double getLat(){
		return this.lla.getLatitude();
	}
	
	public double getLon(){
		return this.lla.getLongitude();
	}
	
	public double getLatRad(){
		return Math.toRadians(this.lla.getLatitude());
	}
	
	public double getLonRad(){
		return Math.toRadians(this.lla.getLongitude());
	}
	
	public double getAlt(){
		return this.lla.getAltitude();
	}
	
	public double getTH(){
		return this.visibilityTH;
	}
	
	public double getTHrad(){
		return Math.toRadians(this.visibilityTH);
	}

}
