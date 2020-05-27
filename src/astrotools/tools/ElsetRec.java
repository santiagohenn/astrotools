package astrotools.tools;

/**
 * Data Class for SGP4
 * 
 * @author Joe Coughlin
 *
 */

import astrotools.structures.*;

public class ElsetRec {
    public int satnum;

    public int epochyr;
    public int init;
    public int epochtynumrev;

    public int error;

    public NearEarthType nevalues = new NearEarthType();

    public DeepSpaceType dsvalues = new DeepSpaceType();

    public double a;
    public double altp;
    public double alta;
    public double epochdays;
    public double mjdsatepoch;
    public double nddot;
    public double ndot;
    public double bstar;
    public double rcse;
    public double inclo;
    public double omegao;
    public double ecco;
    public double argpo;
    public double mo;
    public double no;
    public double eptime;
    public double srtime;
    public double sptime;
    public double deltamin;

    public double ep;
    public double xincp;
    public double omegap;
    public double argpp;
    public double mp;
    
    public int size = 3;

    public double[] r = new double[size];

    public double[] v = new double[size];

    /**
     * constructor
     * @return
     */
    public ElsetRec() {
    }
}
