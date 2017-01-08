import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import edu.rit.io.InStream;
import edu.rit.io.OutStream;
import edu.rit.pj2.Tuple;
import edu.rit.util.AList;

/**
 * Class TravellingPath contains a city tour list
 * 
 * @author Sahil Jasrotia, Lokesh Agrawal
 * 
 */
public class TravelingPath extends Tuple{

	private ArrayList<City> cityList 	= new ArrayList<City>(); // Stores the list of cities
	private AList<City> tupleCityList 	= new AList<City>();     // This list is needed to send the list to tuple space 
	private double fitness;					// Stores the fitness of the tour.	
	private double probability;				// Stores the probability
	private double euclideanDistance;		// Stores the total distance of the tour
	private int sampling;					// Stores the sampling of the tour
	
	/**
	 * Default constructor
	 * 
	 */
	public TravelingPath(){
		
	}
	
	/**
	 * Parameterized constructor, initializes the traveling path
	 * 
	 * @param cityList Contains the list of cities
	 */
	public TravelingPath(ArrayList<City> cityList){
		this.cityList = new ArrayList<City>();
		this.cityList.addAll(cityList);		
	}	
	
	/**
	 * Parameterized constructor for copying a traveling path
	 * 
	 * @param path object of the traveling path
	 */
	public TravelingPath(TravelingPath path) {	
		// Copies the city to the city list
		for (int i = 0; i < path.cityList.size(); i++) {
			addCity(path.cityList.get(i));
		}
		// Deep copies the parameters
		this.euclideanDistance = path.euclideanDistance;
		this.fitness = path.fitness;
		this.probability = path.probability;
		this.sampling = path.sampling;
	}
	
	/**
	 * This method shuffles the tour
	 * 
	 */
	public void shuffleTravellingPath(){
		Collections.shuffle(this.cityList,new Random(12345));		
	}	
	
	/**
	 * This method returns the citylist
	 * 
	 * @return List of cites in this tour
	 */
	public ArrayList<City> getCityList() {
		return cityList;
	}

	/**
	 * Sets the cities in the list
	 * 
	 * @param cityList list of cities
	 */
	public void setCityList(ArrayList<City> cityList) {
		this.cityList = cityList;
	}

	/**
	 * This method adds a city to the city list
	 * 
	 * @param city Contains the city
	 */
	public void addCity(City city){
		cityList.add(city);
	}
	
	/**
	 * This method calculates the euclidean distance for this traveling path
	 * 
	 */
	public void calculateDistance(){
		double x1,x2;
		double y1,y2;		
		
		// Loop through the citylist and calculate the euclidean distance
		for (int i = 0; i < cityList.size() - 1; i++){
			x1 = cityList.get(i).getX();
			y1 = cityList.get(i).getY();
			
			x2 = cityList.get(i+1).getX();
			y2 = cityList.get(i+1).getY();
			
			
			this.setEuclideanDistance(this.getEuclideanDistance() + Math.sqrt((Math.pow(x1 - x2 , 2) + Math.pow(y1 - y2 , 2))));
		}
		
		x1 = cityList.get(0).getX();
		y1 = cityList.get(0).getY();
		
		x2 = cityList.get(cityList.size()-1).getX();
		y2 = cityList.get(cityList.size()-1).getY();				
		this.setEuclideanDistance(this.getEuclideanDistance() + Math.sqrt((Math.pow(x1 - x2 , 2) + Math.pow(y1 - y2 , 2))));		
	}

	/**
	 * Calculated the fitness of the tour
	 * 
	 * @param maxDistance maximum distance of all the tours in the population list
	 */
	public void calculateFitness(double maxDistance){
		this.fitness = maxDistance - this.euclideanDistance;
	}
	
	/**
	 * Calculates the probability of the tour
	 * 
	 * @param overallPopulationFitness The overall fitness of the entire population
	 */
	public void calculateProbability(double overallPopulationFitness){
		this.probability = this.fitness/overallPopulationFitness;
	}
	
	/**
	 * Calculates the sampling 
	 * 
	 * @param populationSize Size of the population
	 */
	public void calculateSampling(int populationSize){
		this.setSampling((int)Math.round(this.probability*populationSize) + 1);
	}
	
	/**
	 * Returns the euclidean distance of this tour
	 * 
	 * @return euclidean distance
	 */
	public double getEuclideanDistance() {
		return euclideanDistance;
	}

	/**
	 * Sets the euclidean distance
	 * 
	 * @param euclideanDistance euclidean distance
	 */
	public void setEuclideanDistance(double euclideanDistance) {
		this.euclideanDistance = euclideanDistance;
	}

	/**
	 * Returns the fitness of this traveling path
	 * 
	 * @return fitness
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Sets the fitness for this traveling path
	 * 
	 * @param fitness fitness
	 */
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	/**
	 * Returns the probability
	 * 
	 * @return probability
	 */
	public double getProbability() {
		return probability;
	}
	
	/**
	 * Sets the probability for this traveling path
	 * 
	 * @param probability probability for this tour
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}

	/**
	 * Returns the sampling
	 * 
	 * @return sampling rate for this tour
	 */
	public int getSampling() {
		return sampling;
	}

	/**
	 * Sets the sampling rate for this traveling path
	 * 
	 * @param sampling Sampling rate
	 */
	public void setSampling(int sampling) {
		this.sampling = sampling;
	}	

	/**
	 * Reads the object parameters from the input stream
	 *
	 * @param inStream Input stream
	 * 
	 * @exception IOException is called to indicate that an object
	 * could not be or should not be read.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void readIn(InStream inStream) throws IOException {		
		tupleCityList.clear();
		tupleCityList = (AList<City>) inStream.readObject();
		for( int i = 0; i < tupleCityList.size(); i++ ){
			this.cityList.add(tupleCityList.get(i));
		}
		this.fitness 			= inStream.readDouble();
		this.probability 		= inStream.readDouble();
		this.euclideanDistance 	= inStream.readDouble();
		this.sampling 			= inStream.readInt();								
	}

	/**
	 * writes the object parameters to the out stream
	 *
	 * @param outStream output stream
	 * 
	 * @exception IOException is called to indicate that an object
	 * could not be or should not be read.
	 */

	@Override
	public void writeOut(OutStream outStream) throws IOException {
		tupleCityList.clear();
		for( int i = 0; i < cityList.size(); i++ ){
			tupleCityList.addLast( cityList.get(i) );
		}		
		outStream.writeObject(tupleCityList);
		outStream.writeDouble(fitness);
		outStream.writeDouble(probability);
		outStream.writeDouble(euclideanDistance);
		outStream.writeInt(sampling);					
	}				
}
