package simulation;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import astrotools.tools.Simulation;
import astrotools.tools.Transformations;

public class GradientSearch {
	
	static Scenario scenario = new Scenario();
	static LinkedHashMap<String,ArrayList<long[]>> chainAccess = new LinkedHashMap<String,ArrayList<long[]>>();
	static List<Double> MRT = new ArrayList<Double>();
	static List<Long> MCG = new ArrayList<Long>();
	static ArrayList<double[]> solutions = new ArrayList<double[]>();
	static LinkedHashMap <String,double[][]> solSpaceSat = new LinkedHashMap <String,double[][]>();
	private static Simulation sim = new Simulation();
	static List<Double> latsDone = new ArrayList<Double>();
	static final double RE = 6378135;
	
	static double satHeight = 700;		// Satellite height in Km
	static double MCGmax = 120;			// Maximum Coverage Gap in minutes
	static double latMax = 60;			// Maximum coverage latitude
	static double visTH = 40;			// Visibility TH in degrees
	
	static Random random = new Random();
	
	public static void main(String args[]){
		
        scenario.setTimePeriod("2020-03-10T11:00:00.000", "2020-03-10T23:00:00.000");
        scenario.setTimeStep(120);
        
        // Standard satellite elements
		double smAxis = satHeight*1000 + RE;
		double ecc = 0;
		double inc = 0;
		double argOfPerigee = 0;
		double RAAN = 0;
		double meanAnom = 0;
		
		double[] dummyElements = {smAxis,ecc,inc,argOfPerigee,RAAN,meanAnom};
		
		double tMax = Transformations.getMaxAccess(dummyElements, visTH);
		
		System.out.println("tMax: " + tMax);
		
		double lambda = getLambdaDeg(visTH,dummyElements);
		
		System.out.println("Lambda used: " + lambda);
		inc = getInclination(lambda,visTH,latMax);
		inc = Math.toDegrees(inc);
		System.out.println("Center inclination found: " + inc);
		
		/* You may change the maximum satellites at your criteria, otherwise
		 * the algorithm will populate satellites on the same plane until overlapping occurs */
		int maxSats = (int) Math.ceil(360/lambda);
		maxSats = 5;
		System.out.println("Maximum satellite per plane: " + maxSats);

		/* Simulated annealing variables */
		int itMax = 1200;
		double[][] graph = new double[itMax][3];
		double tInit = (-15)/Math.log(0.85);	// delta expected / log of probability of acceptance at beginning
		double tEnd = (-15)/Math.log(0.001);	// delta expected / log of probability of acceptance at end
		System.out.println("tInit: " + tInit + " tEnd " + tEnd);
		double ratio = Math.pow((tEnd/tInit), (1.0/(itMax-1)));	// fractional reduction every cycle
		System.out.println("Fractional reduction: " + ratio); 
		
		double[] profit = {0,scenario.getTimeSpan(),scenario.getTimeSpan()};
		
		double[][] sol = new double[3][4];	// Number of planes, Number of sats per plane, inclination, cost
		
		double[][] solSpace = {
				{5,6},
				{2,8},
				{inc-lambda,inc*1.3}
				};
		
		// Initial solution:
		int nPlanes = 2;
		int nSats = 1;
		
		double rstInc = inc;
		
		int discarted = 0;
		int discarted2 = 0;
		int discarted3 = 0;
		int discarted4 = 0;
		
		boolean go = true;
		boolean found = false;
		
		//while (tInit > tEnd){
			while (go){	
			
			scenario.clearAll();
			chainAccess.clear();
			MCG.clear(); MRT.clear();

	        // Populate facilities on eq
	        double gridRes = MCGmax*0.25;
	        int nFacs = (int) Math.round(360/gridRes);
	        System.out.println("Grid points: " + nFacs);
			for (int fac = 0; fac < nFacs; fac ++){
				Facility facility = new Facility("ge_"+fac,0.0,fac*gridRes,0.0,visTH);
				scenario.addFacility(facility);
			}
			
			// Populate facilities in middle:
	        gridRes = MCGmax*0.25/Math.cos(Math.toRadians(latMax/2));
	        nFacs = (int) Math.round(360/gridRes);
			for (int fac = 0; fac < nFacs; fac ++){
				Facility facility = new Facility("gm_"+fac,latMax/2,fac*gridRes,0.0,visTH);
				scenario.addFacility(facility);
			}
			
			// Populate facilities on LMax
	        gridRes = MCGmax*0.25/Math.cos(Math.toRadians(latMax));
	        nFacs = (int) Math.round(360/gridRes);
			for (int fac = 0; fac < nFacs; fac ++){
				Facility facility = new Facility("gp_"+fac,latMax,fac*gridRes,0.0,visTH);
				scenario.addFacility(facility);
			}

			double planePhase = 360/nPlanes;
			double satsPhase = 360/nSats;
			
			System.out.println(nPlanes + " planes with " + planePhase + " degrees of phase with " 
			+ nSats + " sats with " + satsPhase + " degress of phase and " + inc + " degrees of inclination");
			
			// Plane
			for (int plane = 0; plane < nPlanes; plane ++){
				// Satellites
				for (int sat = 0; sat < nSats; sat ++){
					double[] elements = {smAxis,ecc,inc,argOfPerigee,RAAN+plane*planePhase,meanAnom+sat*satsPhase-plane*(planePhase/nSats)};
					Satellite satellite = new Satellite(""+plane+"_"+sat,elements);
					scenario.addSat(satellite);
				}
			}

			// Evaluate metric
			sim.setScenario(scenario);
			chainAccess = getConstellationAccess();
			
			for (String key : chainAccess.keySet()){
				List<Long> gaps = getGaps(chainAccess.get(key));
				MRT.add(getMRT(gaps));
				MCG.add(getMCG(gaps));
			}
			
			profit[0] = (double) Collections.max(MCG)/(1000.0*60.0);
			
			System.out.println("Metric: " + profit[0]);

			int n = 2;
			latsDone.clear();
			
			// Make decision
			if (profit[0] <= MCGmax){

				System.out.println("Possible best solution. Testing with more granularity");
				scenario.setTimePeriod("2020-03-10T11:00:00.000", "2020-03-13T11:00:00.000");
				
				for (n = 1; n<=4; n++){
					
					double res=Math.pow(2, n);
					scenario.clearAll();
					chainAccess.clear();

			        double step;
			        double firstStep;
			        double lastStep;

		        	System.out.println("Granularity: " + res);
		        	
		        	step = latMax/res;
		        	lastStep = latMax - step;
		        	
		        	if (n==1){
		        		firstStep = 0;
		        	} else {
		        		firstStep = step;
		        	}
		        	
		    		// Grid
		    		for (double lat = firstStep; lat<=lastStep; lat+=step){
		    			if (!latsDone.contains(lat)){
		    				latsDone.add(lat);
			    			gridRes = MCGmax*0.25/Math.cos(Math.toRadians(lat));
			    			nFacs = (int) Math.round(360/gridRes);
			    			for (int fac = 0; fac < nFacs; fac ++){
			    				Facility facility = new Facility("g_"+fac+"_"+lat,lat,fac*gridRes,0.0,visTH);
			    				scenario.addFacility(facility);
			    			}
		    			}
		    		}
			        
					planePhase = 360/nPlanes;
					satsPhase = 360/nSats;
					
					// Plane
					for (int plane = 0; plane < nPlanes; plane ++){
						// Satellites
						for (int sat = 0; sat < nSats; sat ++){
							double[] elements = {smAxis,ecc,inc,argOfPerigee,RAAN+plane*planePhase,meanAnom+sat*satsPhase-plane*(planePhase/nSats)};
							Satellite satellite = new Satellite(""+plane+"_"+sat,elements);
							scenario.addSat(satellite);
						}
					}
					
					// Evaluate metric
					sim.setScenario(scenario);
					chainAccess = getConstellationAccess();
					
					for (String key : chainAccess.keySet()){
						List<Long> gaps = getGaps(chainAccess.get(key));
						MRT.add(getMRT(gaps));
						MCG.add(getMCG(gaps));
					}
					
					profit[0]=Collections.max(MCG)/(1000.0*60.0);
					System.out.println("Solution with increase granularity: " + profit[0]);
					
					if (profit[0]>MCGmax){
						break;
					}
					
					}
					// End for
				
					scenario.setTimePeriod("2020-03-10T11:00:00.000", "2020-03-10T23:00:00.000");
				
					if (profit[0]<=MCGmax){
						// If solution complies with requirements after granularity increased
						found = true;
						System.out.println("Req. compliant solution found!");
						System.out.println(profit[0] + "\t" + nPlanes + "\t" + nSats + "\t" + inc);
						double[] tempSol = {nPlanes,nSats,inc,profit[0]};
						solutions.add(tempSol);
					}

					else {
						
						switch (n){
						case(2):
							discarted2++;
							break;
						case(3):
							discarted3++;
							break;
						case(4):
							discarted4++;
							break;
						}
					}
						
						System.out.println("Rejecting move when granularity was increased");
					}
					
				else {
					discarted++;
				}
			
			inc = inc + 1.5;
			if (inc > 90 || found){
				inc = rstInc;
				
				nSats++;
				if (nSats > solSpace[1][1] || found){	// FIXME ojo aca le puse el found
					nSats = (int) solSpace[1][0];
					nPlanes++;
				}
				
				if (nPlanes > solSpace[0][1]){
					nPlanes = (int) solSpace[0][0];
					go = false;
				}
				
				found = false;
				
			}
			
		}
			
		System.out.println("Discarted solutions 1: " + discarted);
		System.out.println("Discarted solutions 2: " + discarted2);
		System.out.println("Discarted solutions 3: " + discarted3);
		System.out.println("Discarted solutions 4: " + discarted4);
			
		
		for (double[] s : solutions){
			System.out.println("Solution with MCG: " + s[3] + " , p: " + s[0] + " , s: "+ s[1] + " , inc: " + s[2]);
		}
		
	}
	
	public static LinkedHashMap<String,ArrayList<long[]>> getConstellationAccess(){
		
		LinkedHashMap<String,ArrayList<long[]>> allHistograms = new LinkedHashMap<String,ArrayList<long[]>>();
		
		long[] tempVector = new long[2];
	    
		// For every facility
		for (Facility f : scenario.getFacList()){
			// Get chain access to the constellation (at least 1 member)
			
			ArrayList<long[]> join = new ArrayList<long[]>();
			ArrayList<long[]> f2s = new ArrayList<long[]>();
			
			// System.out.println("Getting constellation access for " + f.getId());
			
			for (Satellite s : scenario.getSatList()){
				sim.computeAccessBetween(f, s);
				if (sim.getIntervalCount() > 0) join.addAll(sim.getAccessIntervals());
			}
			
			// If there are no contacts
			if (join.isEmpty()){
				f2s.add(new long[]{0L,0L});
			}
			else {
			
			Collections.sort(join, new java.util.Comparator<long[]>(){
			    public int compare(long[] a, long[] b) {
			        return Double.compare(a[0], b[0]);
			    }
			    });
			
			tempVector = join.get(0);
			for (int idx = 1; idx<join.size(); idx++){
				if (join.get(idx)[0]<=tempVector[1] && join.get(idx)[1]>tempVector[1]){
				    tempVector[1] = join.get(idx)[1];}
				else if (join.get(idx)[0]>tempVector[1]){
					f2s.add(tempVector);
					tempVector = join.get(idx);}
			}
			
			// Add last constellation access if not already added
			if (f2s.size()>1 && f2s.get(f2s.size()-1)[1] != tempVector[1]) f2s.add(tempVector);
			
			}
			
			allHistograms.put(f.getId(),f2s);
			
		}
		
		return allHistograms;
	}
	
	/* Gaps of connectivity
	 * 
	 * */
	public static List<Long> getGaps(ArrayList<long[]> intervals){
		
		List<Long> gaps = new ArrayList<Long>();
		
		if (intervals.size() == 0){
			gaps.add(scenario.getTimeSpan());
			return gaps;
		}
		
		for (int idx=1; idx<intervals.size(); idx++){
			gaps.add(intervals.get(idx)[0]-intervals.get(idx-1)[1]);
		}
		
		return gaps;
	}
	
	/* Mean Response Time 
	 * 
	 * */
	public static double getMRT(List<Long> gaps){
		
		if (gaps.size() == 0) return 0;
		
		double gapTimer = 0;
		
		for (long gap : gaps){
			gapTimer += gap;
		}
		
		return gapTimer / gaps.size();	
	}
	
	/* Max Coverage Gap
	 * 
	 * */
	public static long getMCG(List<Long> gaps){
		if (gaps.size() > 0){
			return Collections.max(gaps);
		}
		else{
			return scenario.getTimeSpan();
		}
	}
	
	/* Total Access Time
	 * 
	 * */	
	public static long getTotalAccess(ArrayList<long[]> intervals){
		
		if (intervals.size() == 0)	return 0;

		long access = 0;
		for (long[] i : intervals){
			access+= i[1] - i[0];
		}
		
		return access;

	}
	
	public static void getTAG(){
		
	}
	
	public static void printIntervals(ArrayList<long[]> intervals){
			for (long[] i : intervals){
			//System.out.print(i[0] + " -> " + i[1]);
	    	System.out.println(Transformations.unix2stamp(i[0]) + 
	    			" -> " + Transformations.unix2stamp(i[1]));
		}
	}
	
    public static double getLambdaDeg(double th, double[] elements){
    	
		double Hmax = ((1+elements[1])*elements[0])-RE;
		
        double etaMax = Math.asin((RE*Math.cos(Math.toRadians(th)))/(RE+Hmax));
        
        System.out.println("ETA max: " + Math.toDegrees(etaMax));
        
		double lambdaMax = 90-th-Math.toDegrees(etaMax);
		
		return lambdaMax;
		
	}
    
    public static double getInclination(double lambda, double th, double latMax){
    	
    	double inc=55,pLo,pLm; // inclination, percentage at zero latitude, percentage at maximum latitude
    	// double lambda = getLambdaDeg(th,elements);
    	
    	double lam = Math.toRadians(lambda);
    	double lat = Math.toRadians(latMax);
    	
        double inc0 = 1;
        double inc1 = 89;
        double incx = 45, pOpt;
        int wdt = 0;
            
	    while (Math.abs(inc0-inc1) >= 0.01){
	
	        incx = (inc1+inc0)/2;
	        inc = Math.toRadians(incx);
	        pLm = Math.acos((-Math.sin(lam)+Math.cos(inc)*Math.sin(lat))/(Math.sin(inc)*Math.cos(lat)))/Math.PI;
	        pLo = 1 - (2/Math.PI)*Math.acos((Math.sin(lam))/Math.sin(inc));
	        
	        if (Double.isNaN(pLo)){
	        	pLo = 1;
	        } else if (Double.isNaN(pLm)){
	        	pLm = 0;
	        }
	        
	        pOpt = pLm-pLo;
	
	        if(pOpt==0) break;
	
	        if(pOpt<0)
	            inc0 = incx;
	        else if (pOpt>0)
	            inc1 = incx;
	        
	        wdt++;
	        if (wdt>1000) break;
	        
	    }
	    
    	return inc;
    }

}
