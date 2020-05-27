package astrotools.tools;

import java.text.ParseException;
import java.util.LinkedHashMap;

import astrotools.exceptions.SatElsetException;
import astrotools.structures.AER;
import astrotools.structures.Ephemerides;
import astrotools.structures.Ephemeris;
import astrotools.structures.LLA;
import astrotools.structures.OrbitalData;

public class AccessCalculator {
	
	String dateStart = "";
	String dateEnd = "";
	double timeStep = 60;
	public double[][] intervals = new double[1000][2];
	double[] durations;
	public int intervalCount = 0;
	public String status = "";
	
	// Default constructor
	public AccessCalculator(){
		
	}
	
	public static void main(String args[]){
		
		LLA gs = new LLA(0.0, 0.0, 0.0);
		OrbitalData sat = new OrbitalData();
		double [] elements = {7000000,0,65,0,0};
		sat.setElements(elements);
		
		double ap = getAccessPercentage(gs, sat, 5);
		System.out.println("Access percentage: " + ap);
		
	}
	
	public double[][] getIntervals(){
		return this.intervals;
	}
	
	public int getIntervalCount(){
		return this.intervalCount;
	}
	
	public double getStart(int idx){
		return this.intervals[idx][0];
	}
	
	public double getEnd(int idx){
		return this.intervals[idx][1];
	}
	
	// method 1 computes AER for every ephemeris, considering a WGS84 model, checking elevation over horizon
	public void method1(Ephemerides eph) throws ParseException{
		
		intervals = new double[eph.size()][2];
		intervalCount=0;
		boolean contact = false;
		double th = Math.toRadians(5);
		double t1 = 0;
		double t2 = 0;
		
		System.out.println("Performing method1");
		
		LinkedHashMap<String,AER> AERseries = eph.getAERseries();
		
		for (String key : AERseries.keySet()){
			
			double elevation = AERseries.get(key).getE();
			
			if(elevation>=th && contact==false){
				t1=Transformations.stamp2unix(key);
				intervals[intervalCount][0]=t1;
				contact=true;
			} else if (elevation<=th && contact==true){
				t2=Transformations.stamp2unix(key);
				contact=false;
				intervals[intervalCount][1]=t2;
				intervalCount++;
			}
		}
	}
	
	// method 2 its the same as method 1, extrapolating to get more precise access start and end
	// method 2 extrapolates to find the threshold where the contact begun, according to Az-El and WGS84
	// method 2 linear extrapolates
	public void method2(Ephemerides eph) throws ParseException{
		
		intervals = new double[eph.size()][2];
		intervalCount=0;
		boolean contact = false;
		double step = eph.getStep()*1000; // unix time is in milliseconds
		double th = Math.toRadians(5);
		double prevEl = 0;
		double upRate = 0;
		double downRate = 0;
		boolean getRate = false;
		int point = 0;
		
		System.out.println("Performing method2. Step: " + step);
		
		LinkedHashMap<String,AER> AERseries = eph.getAERseries();
		
		for (String key : AERseries.keySet()){
			
			double elevation = AERseries.get(key).getE();
			
			if (getRate){
				getRate = false;
				if (contact == true && point>1){
					upRate = ((th-prevEl)/(elevation-prevEl));
					System.out.println("previous el: " + Math.toDegrees(prevEl) + " new El: " + Math.toDegrees(elevation) + " Correcting start: " + key);
					intervals[intervalCount][0] = intervals[intervalCount][0] + (upRate*step);
					System.out.println("Corrected start: " + Transformations.unix2stamp((long)intervals[intervalCount][0]));
				} else {
					downRate = ((th-prevEl)/(elevation-prevEl));
					intervals[intervalCount-1][1] = intervals[intervalCount-1][1] + (downRate*step);
				}
			}
			
			if(elevation>=th && contact==false){
				intervals[intervalCount][0]=Transformations.stamp2unix(key);
				System.out.println("beggining of contact: " + key + " E: " + elevation);
				contact = true;
				getRate = true;
				prevEl = elevation;
			} else if (elevation<=th && contact==true){
				System.out.println("End of of contact: " + key + " E: " + elevation);
				intervals[intervalCount][1]=Transformations.stamp2unix(key);
				contact = false;
				getRate = true;
				prevEl = elevation;
				intervalCount++;
			} 
			
			point++;
			
			if (contact==true && point>=AERseries.size()){
				intervals[intervalCount][1]=Transformations.stamp2unix(key);
				intervalCount++;
			}
			
		}
	}

	// method 3 its the same as method 1, interpolating to get more precise access start and end
	// method 3 interpolates to find the threshold where the contact begun, according to Az-El and WGS84
	// method 3 linear interpolates
	public void method3(Ephemerides eph) throws ParseException{
		
		intervals = new double[eph.size()][2];
		intervalCount=0;
		boolean contact = false;
		double step = eph.getStep()*1000; // unix time is in milliseconds
		double th = Math.toRadians(5);
		double prevEl = 0;
		double rate = 0;
		int point = 0;
		String lastKey = "";
		
		System.out.println("Performing method3. Step: " + step);
		
		LinkedHashMap<String,AER> AERseries = eph.getAERseries();
		
		for (String key : AERseries.keySet()){
			
			double elevation = AERseries.get(key).getE();
			
			if(elevation>=th && contact==false){
				
				// Start interval:
				intervals[intervalCount][0]=Transformations.stamp2unix(key);
				
				// Correction:
				prevEl = AERseries.get(lastKey).getE();
				
				if (point>1){	// If i've started the scenario on a contact, do not approximate
					rate = ((th-prevEl)/(elevation-prevEl));
					intervals[intervalCount][0] = intervals[intervalCount][0] - step + (rate*step);
				}
				
				System.out.println("previous el: " + Math.toDegrees(prevEl) + " new El: " 
				+ Math.toDegrees(elevation) + " From: " + lastKey + " to " + key + " new: " 
						+ Transformations.unix2stamp((long)intervals[intervalCount][0]));
				
				contact = true;
			} else if (elevation<=th && contact==true){
				
				// End of contact:
				intervals[intervalCount][1]=Transformations.stamp2unix(key);
				
				// Correction:
				prevEl = AERseries.get(lastKey).getE();
				rate = ((th-prevEl)/(elevation-prevEl));
				intervals[intervalCount][1] = intervals[intervalCount][1] - step + (rate*step);
				
				contact = false;
				intervalCount++;
			} 
			
			point++;
			
			lastKey = key;
			
			// If scenario ends within contact:
			if (contact==true && point>=AERseries.size()){
				intervals[intervalCount][1]=Transformations.stamp2unix(key);
				intervalCount++;
			}
			
		}
	}
	
	// method 4 its the same as method 1, but refines contacts through re propagation
	// method 4 refines the orbit propagation trough bi-section to find the start of the contact (this is what i think STK does)
	// method 4 refines through re-propagation (this is what i think STK does)
	public void method4(LLA gs, OrbitalData sat, String time1, String time2, double step) throws ParseException, SatElsetException{
		
		intervals = new double[1000][2]; // FIXME
		intervalCount=0;
		boolean contact = false;
		double startPeriod = Transformations.stamp2unix(time1);
		double endPeriod = Transformations.stamp2unix(time2);
		step = step*1000; // unix time is in milliseconds
		double th = Math.toRadians(5);
		double elevation = 0;
		double prevEl = 0;
		int point = 0;
		
		System.out.println("Performing method4. Step: " + step);
		
        Propagator propagator = new Propagator(sat,time1,time2,1,"SGP4");
        propagator.propagate();
        Ephemerides ephs = propagator.getEphemerides();
        
        //Transformations.getAER(gs,ephs);
		
		LinkedHashMap<String,AER> AERseries = ephs.getAERseries();
		
		double searchInterval = startPeriod;
		
		for (String key : AERseries.keySet()){
			
			elevation = AERseries.get(key).getE();
			
        	if(elevation>=th && contact==false){
    			if (searchInterval != startPeriod){
    				intervals[intervalCount][0] = getTreshold(gs, sat, searchInterval-step, searchInterval, prevEl, elevation, th);
    			} else {	// If i've started the scenario on a contact, do not approximate
    				intervals[intervalCount][0] = searchInterval;
    			}
    			contact = true;
    			
    		} else if (contact == true && elevation<=th){
    			
    			intervals[intervalCount][1] = getTreshold(gs, sat, searchInterval-step, searchInterval, prevEl, elevation, th);
    			contact = false;
    			intervalCount++;
    			break;
    		} 
			
        	searchInterval = searchInterval + step;
        	prevEl = elevation;
        	
        	//if (searchInterval > endPeriod) return;	// FIXME for throw exception
			
		}
	}
	
	// method 5 uses approximations and assumptions to propagate only in possible times of access
	public void method5(LLA gs, OrbitalData sat, String time1, String time2, double step) throws ParseException, SatElsetException{
		
		intervals = new double[1000][2];	// FIXME
		intervalCount = 0;
		boolean contact = false;
		//double step = eph.getStep()*1000; // unix time is in milliseconds
		double th = Math.toRadians(5);
        double elevation;
		double prevEl = 0;
		double startPeriod = Transformations.stamp2unix(time1);
		double endPeriod = Transformations.stamp2unix(time2);
		step = step*1000; // 30 seconds
		double satPeriod = Transformations.getPeriodMinutes(sat)*60*1000; // min to ms
		double maxAccess = getMaxAccess(sat,th)*1000; // sec to ms
		int regions = getAccessRegions(gs,sat,th);
		
		System.out.println("Performing method 5");
        
        Propagator propagator = new Propagator(sat,"SGP4");
        Ephemeris eph = propagator.propagateAt(startPeriod);
        AER aer;
        //Transformations.getAER(gs, eph);
        
        // Find first contact (much like method 1):
        
        double searchInterval = startPeriod;
        
        while (intervalCount == 0){
        	
        	eph = propagator.propagateAt(searchInterval);
        	aer = Transformations.getAER(gs, eph);
        	elevation = aer.getE();
        	
        	if(elevation>=th && contact==false){
        		
    			if (searchInterval != startPeriod){
    				intervals[intervalCount][0] = getTreshold(gs, sat, searchInterval-step, searchInterval, prevEl, elevation, th);
    			} else {	// If i've started the scenario on a contact, do not approximate
    				intervals[intervalCount][0] = searchInterval;
    			}
    			contact = true;
    			
    		} else if (contact == true && elevation<=th){
    			
    			intervals[intervalCount][1] = getTreshold(gs, sat, searchInterval-step, searchInterval, prevEl, elevation, th);
    			contact = false;
    			intervalCount++;
    			break;
    		} 
        	searchInterval = searchInterval + step;
        	prevEl = elevation;
        	
        	if (searchInterval > endPeriod) return;	// FIXME for throw exception
        }
        
        double middleContact = intervals[0][1] + (intervals[0][1] - intervals[0][0])/2;
        
        // Now, I get the Ephemerides of the satellite only on possible access times and vicinities
        // Similar as method 5, refining through more precise propagations in thresholds
        
        maxAccess = maxAccess*1.1;	// 10% margin
        middleContact = middleContact + satPeriod;	// Search within next orbit
        
        while (middleContact-maxAccess<=endPeriod){
        	
        	propagator.setTimes(middleContact-maxAccess,middleContact+maxAccess,step/1000);
        	
        	if (middleContact+maxAccess > endPeriod){
        		propagator.setTimes(middleContact-maxAccess,endPeriod,step);
        		}

            propagator.propagate();
            Ephemerides ephs = propagator.getEphemerides();
            Transformations.getAER(gs,ephs);
            LinkedHashMap<String,AER> AERseries = ephs.getAERseries();
            
            for (String key : AERseries.keySet()){
    			
            	elevation = AERseries.get(key).getE();
            	searchInterval = Transformations.stamp2unix(key);
            	
            	if(elevation>=th && contact==false){
        			// Refine time(th) and save.
        			intervals[intervalCount][0] = getTreshold(gs, sat,searchInterval-step, searchInterval, prevEl, elevation, th);
        			contact = true;
        		} else if (elevation<=th && contact==true){
        			// Refine time(th) and save.
        			intervals[intervalCount][1] = getTreshold(gs, sat, searchInterval-step, searchInterval, prevEl, elevation, th);
        			middleContact = intervals[intervalCount][1] + (intervals[intervalCount][1] - intervals[intervalCount][0])/2;
        			contact = false;
        			intervalCount++;
        		}
            	
            	prevEl = elevation;
            	
    			// If scenario ends within contact:
    			if (contact==true && searchInterval>=endPeriod){
    				System.out.println("Scenario ends within contact!");
    				intervals[intervalCount][1]=Transformations.stamp2unix(key);
    				intervalCount++;
    				return;
    			}
    		}
            
            middleContact = middleContact + satPeriod/regions;
        }
	}
	
	public long getTreshold(LLA gs, OrbitalData sat,String time1, String time2, double e1, double e2, double th) throws ParseException, SatElsetException{
		return getTreshold(gs,sat,Transformations.stamp2unix(time1),Transformations.stamp2unix(time2),e1,e2,th);
	}
	
	
	
	public long getTreshold(LLA gs, OrbitalData sat, double time1, double time2, double e1, double e2, double th) throws ParseException, SatElsetException {
		
		double t0=time1,t1=time2,tx=(time2-time1)/2;
		boolean gradient;
		
		if (e2<e1){
			gradient = true;	// raising
		} else {
			gradient = false;
		}
		
		Propagator prop = new Propagator(sat,"SGP4");
		Ephemeris eph = new Ephemeris();
		//String midpoint;
		AER aer;
		
		while(Math.abs(t0 - t1) >= 1){
			tx = (t1+t0)/2;
			//midpoint = Transformations.unix2stamp((long) tx);
			eph = prop.propagateAt(tx);
			aer = Transformations.getAER(gs, eph);
			
			double ex = aer.getE();
			
			if(ex==th) return (long) tx;
			
			if(ex<th){
				if(gradient) t1=tx;
				else t0=tx;
			} else {
				if(gradient) t0=tx;
				else t1=tx;
			}
		}
		
		return (long) tx;
	}	

	public double getMaxAccess(OrbitalData sat, double th) throws SatElsetException{
		
		double RE = 6378135;
		
        String card1 = sat.getTLE1();
        String card2 = sat.getTLE2();
        
        double[] params = Transformations.tle2elements(card1,card2);
        double Rp = params[0]*(1+params[1]);	// Get the radius of the apogee
        double Hmax = Rp-RE;
        
        double etaMax = Math.asin((RE*Math.cos(th))/(RE+Hmax));
        double Tseg = (params[6]/180)*(90-th-Math.toDegrees(etaMax));
        
        return Tseg;
		
	}
	
	
	public static double getAccessPercentage(LLA gs, OrbitalData sat, double th){
		
		final double RE = 6378135;
		double lat = Math.toRadians(gs.getLatitude());	// 50
		
		double inc = Math.toRadians(62.5);	// 62.5
		
		if (lat<0) lat = Math.abs(lat);
		
		double Hmax = 622000;
		
        double etaMax = Math.asin((RE*Math.cos(Math.toRadians(th)))/(RE+Hmax));
		double lambdaMax = 90-th-Math.toDegrees(etaMax);
		
		lambdaMax = Math.toRadians(lambdaMax);
		
		double phi1 = Math.acos((-Math.sin(lambdaMax) + Math.cos(inc)*Math.sin(lat))/(Math.sin(inc)*Math.cos(lat)));
		
		double per;
		
		if (lat >= (lambdaMax+inc)){
			System.out.println("No contact");
			per = 0;
		} else if ((inc + lambdaMax > lat) && (lat >= inc - lambdaMax )){
			per = phi1/Math.PI;
		} else {
			double phi2 = Math.acos((Math.sin(lambdaMax) + (Math.cos(inc)*Math.sin(lat)))/(Math.sin(inc)*Math.cos(lat)));
			per = (phi1 - phi2)/Math.PI;
		}
		
		if (Double.isNaN(per)){
			per = 1;
		}
		
		System.out.println("percentage: " + per);
		System.out.println("Hours needed: " + per*24);
		System.out.println("Minutes needed: " + per*24*60);
		
		return per;
		
	}
	
	public int getAccessRegions(LLA gs, OrbitalData sat, double th){

		final double RE = 6378135;
		double lat = Math.toRadians(gs.getLatitude());	// 50
		double inc = Math.toRadians(62.5);	// 62.5
		
		int regions = 0;
		
		if (lat<0) lat = Math.abs(lat);
		
		double Hmax = 622000;
		
        double etaMax = Math.asin((RE*Math.cos(Math.toRadians(th)))/(RE+Hmax));
		double lambdaMax = 90-th-Math.toDegrees(etaMax);
		
		lambdaMax = Math.toRadians(lambdaMax);

		if (lat >= (lambdaMax+inc)){
			regions = 0;
		} else if ((inc + lambdaMax > lat) && (lat >= inc - lambdaMax)){
			regions = 1;
		} else {
			regions = 2;
		}
		
		return regions;
	}
	
	
	
}
