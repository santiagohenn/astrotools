package simulation;

import java.util.List;

import astrotools.tools.Transformations;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Scenario {
	
	public String start = "";
	public String end = "";
	public double step = 1;
	public LinkedHashMap <String,Satellite> sats = new LinkedHashMap<String,Satellite>();
	public LinkedHashMap <String,Facility> facs = new LinkedHashMap<String,Facility>();
	
	public Scenario(){
		
	}
	
	public Scenario(String start, String end, double timeStep){
		this.start = start;
		this.end = end;
		this.step = timeStep;
	}
	
	public void setTimePeriod(String start, String end){
		this.start = start;
		this.end = end;
	}
	
	public void setTimeStep(double timeStep){
		this.step = timeStep;
	}
	
	public String getStart(){
		return this.start;
	}
	
	public String getEnd(){
		return this.end;
	}
	
	public double getStep(){
		return this.step;
	}
	
	public long getTimeSpan(){
		long ti = Transformations.stamp2unix(start);
		long tf = Transformations.stamp2unix(end);
		return tf - ti;
		
	}
	
 	public List<Satellite> getSatList(){
		List<Satellite> satList = new ArrayList<Satellite>(sats.values());
		return satList;
	}

 	public List<Facility> getFacList(){
		List<Facility> facList = new ArrayList<Facility>(facs.values());
		return facList;
	}
 	
 	public int getFacsCount(){
 		return this.facs.size();
 	}
	
 	public void addFacility(Facility fac){
 		facs.put(fac.getId(), fac);
 	}
 	
	public Facility getFacility(String name){
		return facs.get(name);
	}
	
	public void printFacs(){
		for (Facility fac : this.getFacList()){
			System.out.println(fac.getId() + " Lat: " + fac.getLat() + " Lon: " + fac.getLon());
		}
	}
	
	public void printSats(){
		for (Satellite sat : this.getSatList()){
			double[] el = sat.getElements();
			System.out.println(sat.getId() + "," + el[0] + "," + el[1] + "," + el[2] + "," + el[3] + "," + el[4] + "," + el[5]);
		}
	}
	
	public void addSat(Satellite sat){
		sats.put(sat.getId(), sat);
	}
	
	public Satellite getSat(String id){
		return sats.get(id);
	}
	
	public void clearSats(){
		this.sats.clear();
	}
	
	public void clearFacs(){
		this.facs.clear();
	}
	
	public void clearAll(){
		this.facs.clear();
		this.sats.clear();
	}
	
	public void getMatlabSTKapi(){
		
		// TODO
		
	}
	
	public void printSTK(){

		ArrayList<String> out = new ArrayList<String>();
		int nSats = sats.size();
		
		out.add("for n = 1:"+nSats);
		
		
//	for nAsset=1:nSM+nRelay
//	    parametrosTemp=Ps(1,:,nAsset);
//	    if nAsset<=nSM
//	        sat = scenario.Children.New('eSatellite',strcat('SM_',num2str(nAsset)));
//	    else
//	        sat = scenario.Children.New('eSatellite',strcat('RELAY_',num2str(nAsset-nSM)));
//	    end
//	    keplerian = sat.Propagator.InitialState.Representation.ConvertTo('eOrbitStateClassical');
//	    sat.Propagator.step=5; % 60 segundos step 
//	    %keplerian.SizeShapeType = 'eSizeShapeAltitude';
//	    keplerian.SizeShapeType = 'eSizeShapeSemimajorAxis';
//	    keplerian.LocationType = 'eLocationTrueAnomaly';
//	    %keplerian.LocationType = 'eLocationMeanAnomaly';
//	    if ((parametrosTemp(1)<=42164) && (parametrosTemp(1)>=42162))
//	        keplerian.Orientation.AscNodeType = 0;  % 0=LAN
//	        fprintf('_location_type_LAN_');
//	    else
//	        keplerian.Orientation.AscNodeType = 1;  % 1=RAAN
//	        fprintf('_location_type_RAAN_');
//	    end
//	    keplerian.SizeShape.SemiMajorAxis = parametrosTemp(1);
//	    keplerian.SizeShape.Eccentricity = parametrosTemp(2);
//	    keplerian.Orientation.Inclination = parametrosTemp(3);
//	    keplerian.Orientation.AscNode.Value = parametrosTemp(4);
//	    keplerian.Orientation.ArgOfPerigee = parametrosTemp(5);
//	    keplerian.Location.Value = parametrosTemp(6);
//	    sat.Propagator.InitialState.Representation.Assign(keplerian);
//	    sat.SetPropagatorType(1);   % 1 = J2 perturbation, 2 = J4, 7 = Two body.
//	    sat.Propagator.Propagate;
//	    accessConstraints = sat.AccessConstraints;
//	    minAngle = accessConstraints.AddConstraint('eCstrGroundElevAngle');
//	    minAngle.EnableMin = true;
//	    minAngle.Min = 5; % Grados
//	    minGrazingAlt = accessConstraints.AddConstraint('eCstrGrazingAlt');
//	    minGrazingAlt.EnableMin = true;
//	    minGrazingAlt.Min = 100; % Km
//	end
//	
//	for j=1:nFT
//	    facility = scenario.Children.New('eFacility',strcat('FT_',num2str(j)));
//	    facility.Position.AssignGeodetic(Pft(1,2,j),Pft(1,3,j),Pft(1,4,j));
//	end
		
	}
	
}
