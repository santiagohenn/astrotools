package simulation;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import astrotools.tools.Simulation;
import astrotools.tools.Transformations;

public class Optimization2 {
	
	static Scenario scenario = new Scenario();
	static LinkedHashMap<String,ArrayList<long[]>> chainAccess = new LinkedHashMap<String,ArrayList<long[]>>();
	
	static List<Double> MRT = new ArrayList<Double>();
	static List<Long> MCG = new ArrayList<Long>();
	
	LinkedHashMap <String,double[][]> solSpaceSat = new LinkedHashMap <String,double[][]>();
	private static Simulation sim = new Simulation();
	
	static final double RE = 6378135;
	
	static Random random = new Random();
	
	public static void main(String args[]){
		
        scenario.setTimePeriod("2020-03-10T11:00:00.000", "2020-03-11T11:00:00.000");
        scenario.setTimeStep(120);
        
        double MCGmax = 2*60;		// Maximum Coverage Gap in minutes
        double satHeight = 500;		// Satellite height in Km
        double visTH = 5;			// Visibility TH in degrees
        double latMax = 60;			// Maximum coverage latitude
        
        // Populate facilities on eq
        double gridRes = MCGmax*0.25;
        int nFacs = (int) Math.round(360/gridRes);

		scenario.printFacs();
        
        // Standard satellite elements
		double smAxis = satHeight*1000 + RE;
		double ecc = 0;
		double inc = 0;
		double argOfPerigee = 0;
		double RAAN = 0;
		double meanAnom = 0;
		
		double[] dummyElements = {smAxis,ecc,inc,argOfPerigee,RAAN,meanAnom};
		
		// double tMax = Transformations.getMaxAccess(dummyElements, visTH);
		
		double lambda = getLambdaDeg(visTH,dummyElements);
		System.out.println("Lambda found: " + lambda);
		
		inc = getInclination(lambda,visTH,latMax);
		inc = Math.toDegrees(inc);
		System.out.println("Center inclination found: " + inc);
		
		/* You may change the maximum satellites at your criteria, otherwise
		 * the algorithm will populate satellites on the same plane until overlapping occurs */
		int maxSats = (int) Math.ceil(360/lambda);
		maxSats = 6;
		System.out.println("Maximum satellite per plane: " + maxSats);

		/* Simulation variables */
		int itMax = 100;
		
		double[] profit = {0,-1,MCGmax};
		double[] mcgObtained = {MCGmax,MCGmax};
		
//		double[][] sol = new double[3][3];	// Number of planes, Number of sats per plane, inclination
		
		int[] satSS = {2,maxSats};
		int[] planeSS = {3,6};
		
		int nSats = satSS[0];
		int nPlanes = planeSS[0];
		
		//	double planePhase = 360/(Math.round(sol[0][0]));
		//	double satsPhase = 360/(Math.round(sol[0][1]));
		
		// Initial solution:
		
		int count = 0;
		double rstInc = inc;
		
		double longRes = MCGmax*0.25;
		double latRes = latMax;
		
		while (count < itMax){
			
			scenario.clearAll();
			chainAccess.clear();
			MCG.clear(); MRT.clear();
			
			// Grid
			for (double lat = 0; lat<=latMax; lat+=latRes){
				longRes = MCGmax*0.25/Math.cos(Math.toRadians(lat));
				nFacs = (int) Math.round(360/gridRes);
				for (int fac = 0; fac < nFacs; fac ++){
					Facility facility = new Facility("g_"+fac+"_"+lat,lat,fac*longRes,0.0,visTH);
					scenario.addFacility(facility);
				}
			}
			
			double planePhase = 360/nSats;
			double satsPhase = 360/nPlanes;
			// inc = sol[0][2];
			
			// Plane
			for (int plane = 0; plane < nPlanes; plane ++){
				// Satellites
				for (int sat = 0; sat < nSats; sat ++){
					double[] elements = {smAxis,ecc,inc,argOfPerigee,RAAN+plane*planePhase,meanAnom+sat*satsPhase-plane*(planePhase/nSats)};
					Satellite satellite = new Satellite(""+plane+"_"+sat,elements);
					scenario.addSat(satellite);
				}
			}
			
			// scenario.printSats();
		
			// Evaluate metric
			
			sim.setScenario(scenario);
			chainAccess = getConstellationAccess();
			
			for (String key : chainAccess.keySet()){
				List<Long> gaps = getGaps(chainAccess.get(key));
				MRT.add(getMRT(gaps));
				MCG.add(getMCG(gaps));
			}
			
			profit[0] = (double) Collections.max(MCG)/(1000.0*60.0);
			// System.out.println("MCG: " + profit[0] + " MCGmax: " + MCGmax);
			mcgObtained[0] = profit[0];
			// profit[0] = Math.abs(profit[0] - MCGmax);
			
			System.out.println("Metric: " + profit[0] + " - " + MCGmax);
			
			// Evaluate:
			if (profit[0]<MCGmax){
				
				// increase granularity
				System.out.println("Increasing granularity");
				latRes = latRes/2;
				profit[1]=profit[0];
				
				count++;
				
				if (count >= 4){
					break;
				}
				
			}
			// Take action:
			else {
				count = 0;
				latRes = latMax;
				
				inc = inc+1;
				
				if (inc > rstInc*1.3){
					inc = rstInc;
					
					nSats++;
					if (nSats>satSS[1]){
						nSats = satSS[0];
						nPlanes++;
					}
					
					if (nPlanes>planeSS[1]){
						nPlanes = planeSS[0];
						inc = inc+0.5;
					}
					
				}
				
				System.out.println("Taking action. Number of planes: " + nPlanes + " Number of sats:" + nSats + " Inclination: " + inc);

			} 
			
			// Iterate
			
		}
		
		System.out.println("Sol found with metric: " + profit[2] + " MCG: " + mcgObtained[1]);
		System.out.println("Sol found: " + nPlanes + " planes with " + nSats + " satellites at " 
				+ inc + " degrees of inclination");
		
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
			if (f2s.get(f2s.size()-1)[1] != tempVector[1]) f2s.add(tempVector);
			
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
		double lambdaMax = 90-th-Math.toDegrees(etaMax);
		
		return lambdaMax;
		
	}
    
    public static double getInclination(double lambda, double th, double latMax){
    	
    	double inc=55,pLo,pLm; // inclination, percentage at zero latitude, percentage at maximum latitude
    	// double lambda = getLambdaDeg(th,elements);
    	lambda = Math.toRadians(lambda);
    	
        double inc0 = 1;
        double inc1 = 89;
        double incx = 45, pOpt;
            
	    while (Math.abs(inc0-inc1) >= 0.001){
	
	        incx = (inc1+inc0)/2;
	        inc = Math.toRadians(incx);
	        pLm = Math.acos((-Math.sin(lambda)+Math.cos(inc)*Math.sin(latMax))/(Math.sin(inc)*Math.cos(latMax)))/Math.PI;
	        pLo = 1 - (2/Math.PI)*Math.acos((Math.sin(lambda))/Math.sin(inc));
	        pOpt = pLm-pLo;
	
	        if(pOpt==0) break;
	
	        if(pOpt<0)
	            inc0 = incx;
	        else if (pOpt>0)
	            inc1 = incx;
	        
	    }
	    
    	return inc;
    }

}
