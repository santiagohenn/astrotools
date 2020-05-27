package astrotools.structures;

public class TEME {
	
	    private double x;
	    private double y;
	    private double z;
	    private double xdot;
	    private double ydot;
	    private double zdot;

	    public TEME(double x, double y, double z) {
	        this.x = x;
	        this.y = y;
	        this.z = z;
	    }
	    
	    public void scale(double k){
	        this.x = k*x;
	        this.y = k*y;
	        this.z = k*z;
	    }

	    public double getX() {
	        return x;
	    }

	    public double getY() {
	        return y;
	    }

	    public double getZ() {
	        return z;
	    }
	    
	    public double getXdot() {
	        return xdot;
	    }

	    public double getYdot() {
	        return ydot;
	    }

	    public double getZdot() {
	        return zdot;
	    }
	}
	
