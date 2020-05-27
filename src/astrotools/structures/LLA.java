package astrotools.structures;

public class LLA {
	
	private String id = "default";
    private double latitude;
    private double longitude;
    private double altitude;

    public LLA(double latitude, double longitude, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }
    
    public void set(double lat, double lon, double alt){
        this.longitude = lat;
        this.latitude = lon;
        this.altitude = alt;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getAltitude() {
        return altitude;
    }
    
    public void setId(String ID) {
        this.id = ID;
    }
    
    public String getId() {
        return this.id;
    }
}