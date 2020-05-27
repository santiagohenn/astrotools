package astrotools.structures;

import javax.vecmath.Vector3d;

public class OrbitalData {

	private String id = "";
	private long unixDate = 0;
	private double julianDate = 0;
    private int satNumber = -1;
    private double azimuth = 999.0;
    private double elevation = 99.0;
    private double range = 999.0;
    private double rangeRate = -999.0;
    private Vector3d posn = new Vector3d();	// TEME
    private Vector3d vel = new Vector3d();	// TEME
    private ECEF ecef = new ECEF(0.0,0.0,0.0);
    private LLA lla = new LLA(0.0,0.0,0.0);
    private double radiansToDegrees = 180.0/Math.PI;
    
    private String tle1 = "";
    private String tle2 = "";
    private double[] elements = new double[6];
    
    public OrbitalData(){
    	
    }
    
    public OrbitalData(String id){
        this.id = id;
    }
    
    public OrbitalData(int satNum){
        satNumber = satNum;
    }
    
    public OrbitalData(String id, String date, String card1, String card2){
        this.id = id;
        this.tle1 = card1;
        this.tle2 = card2;
    }
    
    public OrbitalData(String id, String date, double[] elements){
        this.id = id;
        this.elements = elements;
    }

    public void setTLE(String card1, String card2){
    	this.tle1 = card1;
    	this.tle2 = card2;
    }
    
    public String getTLE1(){
    	return this.tle1;
    }
    
    public String getTLE2(){
    	return this.tle2;
    }
    
    public void setElements(double[] elements){
    	this.elements = elements;
    }
    
    public double[] getElements(){
    	return this.elements;
    }
    
    public void setId(String id){
    	this.id = id;
    }
    
    public String getId(){
    	return this.id;
    }
    
    /**
     * Get the Azimuth (radians)
     * @return double
     */
    public double getAzimuth() {
        return azimuth;
    }
    /**
     * Get the Azimuth (degrees)
     * @return double
     */
    public double getAzimuthDegrees() {
        return azimuth*radiansToDegrees;
    }
    /**
     * Set the azimuth (radians)
     * @param azimuth
     */
    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }
    /**
     * Get the Elevation (radians)
     * @return
     */
    public double getElevation() {
        return elevation;
    }
    /**
     * Get the Elevation (degrees)
     * @return
     */
    public double getElevationDegrees() {
        return elevation*radiansToDegrees;
    }
    /**
     * Set the Elevation (radians)
     * @param double elevation
     */
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    /**
     * Get the Position
     * @return Vector3d
     */
    public Vector3d getPosn() {
        return posn;
    }
    /**
     * @param posn The posn to set.
     */
    public void setPosn(Vector3d posn) {
        this.posn = posn;
    }
    /**
     * Get the range (meters)
     * @return double
     */
    public double getRange() {
        return range;
    }
    /**
     * Get the range rate (meters/sec)
     * @return double
     */
    public double getRangeRate() {
        return rangeRate;
    }
    /**
     * @param range The range to set.
     */
    public void setRange(double range) {
        this.range = range;
    }
    /**
     * @return Returns the satNumber.
     */
    public int getSatNumber() {
        return satNumber;
    }
    /**
     * @param satNumber The satNumber to set.
     */
    public void setSatNumber(int satNumber) {
        this.satNumber = satNumber;
    }
    /**
     * @return Returns the x.
     */
    public double getX() {
        return posn.x;
    }
    /**
     * @param x The x to set.
     */
    public void setX(double x) {
        posn.x = x;
    }
    /**
     * @return Returns the y.
     */
    public double getY() {
        return posn.y;
    }
    /**
     * @param y The y to set.
     */
    public void setY(double y) {
        posn.y = y;
    }
    /**
     * @return Returns the z.
     */
    public double getZ() {
        return posn.z;
    }
    /**
     * @param z The z to set.
     */
    public void setZ(double z) {
        posn.z = z;
    }
    /**
     * @return Returns the vel.
     */
    public Vector3d getVel() {
        return vel;
    }
    /**
     * @param vel The vel to set.
     */
    public void setVel(Vector3d vel) {
        this.vel = vel;
    }
    /**
     * @return Returns the xdot.
     */
    public double getXdot() {
        return vel.x;
    }
    /**
     * @param xdot The xdot to set.
     */
    public void setXdot(double xdot) {
        vel.x = xdot;
    }
    /**
     * @return Returns the ydot.
     */
    public double getYdot() {
        return vel.y;
    }
    /**
     * @param ydot The ydot to set.
     */
    public void setYdot(double ydot) {
        vel.y = ydot;
    }
    /**
     * @return Returns the zdot.
     */
    public double getZdot() {
        return vel.z;
    }
    /**
     * @param zdot The zdot to set.
     */
    public void setZdot(double zdot) {
        vel.z = zdot;
    }
    
    public void setECEF(ECEF computedECEF){
    	this.ecef = computedECEF;
    }
    
    public ECEF getECEF(){
    	return this.ecef;
    }
    
    public void setLLA(LLA computedLLA){
    	this.lla = computedLLA;
    }
    
    public LLA getLLA(){
    	return this.lla;
    }
    
    public void setJulian(double julian){
    	this.julianDate = julian;
    }
    
    public double getJulian(){
    	return this.julianDate;
    }
    
    public void setUnix(long unix){
    	this.unixDate = unix;
    }
    
    public long getUnix(){
    	return this.unixDate;
    }
}