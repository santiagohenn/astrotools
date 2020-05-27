package astrotools.testbench;

import java.text.ParseException;

import astrotools.exceptions.SatElsetException;
import astrotools.structures.LLA;
import astrotools.structures.OrbitalData;
import astrotools.tools.Simulation;
import astrotools.tools.Transformations;

public class Bench {
	
	public static void main(String[] args) throws ParseException, SatElsetException{

        test1();
		
	}
	
	public static void test1(){
		
        String card1 = "1 43641U 18076A   20080.10203740 -.00000150  00000-0 -12340-4 0  9999";
        String card2 = "2 43641  97.8877 267.4896 0001478  80.1454 279.9910 14.82153417 78367";
        	
        String time1 = "2020-03-20T11:00:00.000";
        String time2 = "2020-03-30T11:00:00.000";
        
        double elements[] = {7000672.074930292,1.478E-4,97.8877,80.1454,267.4896,279.9910};
        
        try {
			Transformations.tle2elements(card1, card2);
		} catch (SatElsetException e) {
			e.printStackTrace();
		}
        
        double step = 60;
        double th = Math.toRadians(5);
        LLA gs = new LLA(25.0,45.0,788.0);
        
        //OrbitalData satellite = new OrbitalData("SM_1",time1,card1,card2);
        OrbitalData satellite = new OrbitalData("SM_1",time1,elements);
        
        Simulation access = new Simulation(time1,time2,step,th);
        //AccessCalculator access = new AccessCalculator(gs,satellite,time1,time2,step,th);
        
        long t1 = System.currentTimeMillis();
        access.computeAccess();
        t1 = System.currentTimeMillis() - t1;
        
        access.printAccessReport();
        //access.printIntervalsUnix();
        System.out.println("Total access: " + (long)(access.getTotalAccess()));
        //System.out.println("Elapsed time: " + t1);
		
	}

}
