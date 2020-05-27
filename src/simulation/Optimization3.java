package simulation;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import astrotools.tools.Simulation;
import astrotools.tools.Transformations;

public class Optimization3 {
	
	static Scenario scenario = new Scenario();
	static Simulation sim = new Simulation();
	
	static LinkedHashMap<String,ArrayList<long[]>> chainAccess = new LinkedHashMap<String,ArrayList<long[]>>();
	
	LinkedHashMap <String,double[][]> solSpaceSat = new LinkedHashMap <String,double[][]>();
	
	static final double RE = 6378135;
	
	static double MCGmax = 2*60;
	static double latMax = 60; 
	static double visTH = 5;
	
	static Random random = new Random();
	
	static Optimization3 test = new Optimization3();
	
	public static void main(String args[]){
		
        scenario.setTimePeriod("2020-03-10T11:00:00.000", "2020-03-11T11:00:00.000");
        scenario.setTimeStep(120);
        
        MCGmax = 2*60;				// Maximum Coverage Gap in minutes
        double satHeight = 700;		// Satellite height in Km
        visTH = 5;					// Visibility TH in degrees
        latMax = 60;				// Maximum coverage latitude
        
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
		maxSats = 5;
		System.out.println("Maximum satellite per plane: " + maxSats);

		/* Simulated annealing variables */
		int itMax = 100;
		double tInit = (-15)/Math.log(0.85);	// delta expected / log of probability of acceptance at beginning
		double tEnd = (-15)/Math.log(0.001);	// delta expected / log of probability of acceptance at end
		System.out.println("tInit: " + tInit + " tEnd " + tEnd);
		double ratio = Math.pow((tEnd/tInit), (1.0/(itMax-1)));	// fractional reduction every cycle
		System.out.println("Fractional reduction: " + ratio); 
		
		double[] profit = {0,-1,MCGmax};
		double[] mcgObtained = {MCGmax,MCGmax};
		double[][] sol = new double[3][3];	// Number of planes, Number of sats per plane, inclination
		
		double[][] solSpace = {
				{3,5},
				{3,6},
				{inc,inc*1.2}
				};
		
		// Initial solution:
		
		for (int p = 0; p<3; p++){
			sol[0][p] = solSpace[p][0] + (solSpace[p][1] - solSpace[p][0]) * random.nextDouble();
			sol[1][p] = sol[0][p];
			sol[2][p] = sol[0][p];
		}
		
		int param = 0;
		int count = 0;
		
		double delta = 0.0;
		
		while (tInit > tEnd){
			
			// Current solution = Accepted solution
			for (int p = 0; p<3; p++){
			sol[0][p] = sol[1][p];
			}

			// Find random solution within interval
			sol[0][param] = solSpace[param][0] + (solSpace[param][1] - solSpace[param][0]) * random.nextDouble();

			int nPlanes = (int) Math.round(sol[0][0]);
			int nSats = (int) Math.round(sol[0][1]);
			inc = sol[0][2];
			
			System.out.println("Move: " + nPlanes + "," + nSats + "," + inc);
			
			profit[0] = doProfit(1,nPlanes,nSats,inc,dummyElements);
			System.out.println("Cost: " + profit[0]);
			
			delta = delta + Math.abs(profit[1]-profit[0]);
			
			// Make decision
			if (profit[0]<profit[1]){
				
				System.out.println("Better move found. Testing with more granularity");
				double temp = profit[0];
				for (double res = 2; res<6; res++){
					profit[0] = doProfit(res,nPlanes,nSats,inc,dummyElements);
					if (profit[0]> temp){
						break;
					}
				}
				
				if (profit[0]<temp){
					profit[1]=profit[0];
					sol[1][param]=sol[0][param];
					if (profit[0]<profit[2]){
						profit[2]=profit[1];
						mcgObtained[1] = mcgObtained[0];
						sol[2][param]=sol[0][param];
					}
					param++;
				}
				else {
					System.out.println("Rejecting move when granularity was increased");
				}
			} else if (profit[1] == -1){
				profit[1]=profit[0];
				sol[1][param]=sol[0][param];
				if (profit[0]<profit[2]){
					profit[2]=profit[1];
					mcgObtained[1] = mcgObtained[0];
					sol[2][param]=sol[0][param];
				}
			}
			else {
				double diff = profit[1]-profit[0];
				if ((Math.exp(diff/tInit))>random.nextDouble()){
					System.out.println("delta: " + diff);
					System.out.println("Accepting move with probability: " + Math.exp(diff/(tInit)));
					profit[1]=profit[0];
					sol[1][param]=sol[0][param];
					param++;
				}
				else {
				System.out.println("Rejecting move");
				}
			} 
			
			if (param>2) param = 0;
			
			tInit = ratio*tInit;
			System.out.println("param " + param + " temparature: " + tInit);
			
			// Iterate
			count++;
			
		}
		
		if (profit[2]<=MCGmax){
			System.out.println("Sol found with metric: " + profit[2] + " MCG: " + mcgObtained[1]);
			System.out.println("Sol found: " + Math.round(sol[2][0]) + " planes with " + Math.round(sol[2][1]) + " satellites at " 
					+ sol[2][2] + " degrees of inclination");
		} else {
			System.out.println("N/S - Best metric: " + profit[2] + " MCG: " + mcgObtained[1]);
			System.out.println("N/S - Best option: " + Math.round(sol[2][0]) + " planes with " + Math.round(sol[2][1]) + " satellites at " 
					+ sol[2][2] + " degrees of inclination");			
		}
		
		System.out.println("Delta aprox: " + delta/count);
		
	}
	
	public static double doProfit(double latRes, int nPlanes, int nSats, double inc, double[] elem){
		
		List<Double> MRT = new ArrayList<Double>();
		List<Long> MCG = new ArrayList<Long>();
		scenario.clearAll();
		chainAccess.clear();
		
//		MCG.clear(); MRT.clear();
		
        double gridRes;
        int nFacs;
        double step;
        double lastStep;
        
        if (latRes == 1){
        	
        	System.out.println("Testing extremes. LatMax: " + latMax);
        	
		// Grid
		for (double lat = 0; lat<=latMax; lat+=latMax){
			gridRes = MCGmax*0.25/Math.cos(Math.toRadians(lat));
			nFacs = (int) Math.round(360/gridRes);
			for (int fac = 0; fac < nFacs; fac ++){
				Facility facility = new Facility("g_"+fac+"_"+lat,lat,fac*gridRes,0.0,visTH);
				scenario.addFacility(facility);
			}
		}
		
		scenario.printFacs();
		
        }
        else {
        	
        	System.out.println("Testing with granularity: " + latRes);
        	
        	step = latMax/latRes;
        	lastStep = latMax - step;
        	
    		// Grid
    		for (double lat = step; lat<=lastStep; lat+=step){
    			gridRes = MCGmax*0.25/Math.cos(Math.toRadians(lat));
    			nFacs = (int) Math.round(360/gridRes);
    			for (int fac = 0; fac < nFacs; fac ++){
    				Facility facility = new Facility("g_"+fac+"_"+lat,lat,fac*gridRes,0.0,visTH);
    				scenario.addFacility(facility);
    			}
    		}
    		
    		scenario.printFacs();
        }
        
		double planePhase = 360/nPlanes;
		double satsPhase = 360/nSats;
		
		System.out.println("Doing: " + nPlanes + " planes with " + planePhase + " degrees of phase with " 
		+ nSats + " sats with " + satsPhase + " degress of phase");
		
		// Plane
		for (int plane = 0; plane < nPlanes; plane ++){
			// Satellites
			for (int sat = 0; sat < nSats; sat ++){
				double[] elements = {elem[0],elem[1],elem[2],elem[3],elem[4]+plane*planePhase,elem[5]+sat*satsPhase-plane*(planePhase/nSats)};
				Satellite satellite = new Satellite(""+plane+"_"+sat,elements);
				scenario.addSat(satellite);
			}
		}
		
		
		
		// Evaluate metric
		sim.setScenario(scenario);
		chainAccess = getConstellationAccess();
		
		System.out.println("Intervals: " + chainAccess.size());
//		
//		for (String key : chainAccess.keySet()){
//			ArrayList<long[]> itv = chainAccess.get(key);
//			System.out.println(key + " " + itv.get(0)[0] + " - " + itv.get(0)[1]);
//		}
		
		System.out.println("Chain access size: " + chainAccess.size());
		
		for (String key : chainAccess.keySet()){
			List<Long> gaps = getGaps(chainAccess.get(key));
			MRT.add(getMRT(gaps));
			MCG.add(getMCG(gaps));
		}
		
		System.out.println("MCG size: " + MCG.size());
		System.out.println("MRT size: " + MRT.size());
		
		for (long l : MCG){
			System.out.println("MCG_ "+l);
		}
		
		return ((double) Collections.max(MCG)/(1000.0*60.0));
		
	}
	
	public static LinkedHashMap<String,ArrayList<long[]>> getConstellationAccess(){
		
		LinkedHashMap<String,ArrayList<long[]>> allHistograms = new LinkedHashMap<String,ArrayList<long[]>>();
		
		long[] tempVector = new long[2];
	    
		System.out.println("Scenario check: " + scenario.getFacsCount() + " - " + scenario.getSatList().size());
		
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
			if (f2s.size() > 1 && f2s.get(f2s.size()-1)[1] != tempVector[1]) f2s.add(tempVector);
			
			}
			
			allHistograms.put(f.getId(),f2s);
			
		}
		
		System.out.println("Histograms size: " + allHistograms.size());
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
