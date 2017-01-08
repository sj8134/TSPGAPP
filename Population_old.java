import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import edu.rit.util.AList;

/**
 * Class Population manages the population and performs the genetic algorithm on population
 * 
 * @author Sahil Jasrotia, Lokesh Agrawal
 * 
 */
public class Population {

	private int populationSize;							// To hold the population size.
	private ArrayList<TravelingPath>	populationList; // Contains the list of traveling paths
	private ArrayList<TravelingPath>   	newPopulation;  // Contains the population after crossover
	TravelingPath initTour;								// Contains the initial tour
	private double maxDistance = 0;
	private ArrayList<TravelingPath> matingPool = new ArrayList<TravelingPath>(); // mating pool for crossover
	
	/**
	 * Default constructor for initializing the population list
	 */
	public Population(){			
		this.populationList = new ArrayList<TravelingPath>();
		this.newPopulation  = new ArrayList<TravelingPath>();
	}
	
	/**
	 * Parameterized constructor to initialize tour and population list
	 * @param tour contains the initial tour
	 * @param populationSize the population size		 
	 */
	public Population(TravelingPath tour,int populationSize){
		this.initTour = new TravelingPath(tour.getCityList()); 
		this.populationSize = populationSize;					
		this.populationList = new ArrayList<TravelingPath>();
		this.newPopulation  = new ArrayList<TravelingPath>();
	}
	
	/**
	 * Constructor to initialize the population
	 * @param population contains the population
	 */
	public Population(Population population) {
		for (int i = 0; i < population.populationList.size(); i++) {
			this.populationList.add(new TravelingPath(population.populationList.get(i)));
		}
	}
	
	/**
	 * This method creates the initial population 
	 */
	public void createPopulation(){		    			
		for (int i = 0; i < populationSize; i++){			
			initTour.shuffleTravellingPath();						
			initTour.calculateDistance();
			if(initTour.getEuclideanDistance() > maxDistance)	maxDistance = initTour.getEuclideanDistance();
			populationList.add(initTour);
			calculatePopulationFitness();
		}											
	}
		
	/**
	 * This method finds the maximum distance from all the tours in the population list
	 * 
	 */
	private void findMaxDistance(){
		for (int i = 0; i < populationList.size(); i++)
			if(populationList.get(i).getEuclideanDistance() > maxDistance)	
				maxDistance = populationList.get(i).getEuclideanDistance();
	}
	
	/**
	 * This method is the core method which starts the GA process.
	 * 
	 * @param gaIterCount The iterations for genetic algorithm to run
	 */
	public void startGA(int gaIterCount){		
		// Perform GA for n times
		for(int i = 0; i < gaIterCount; i++){				
			calculateProbability();		
			calculateSampling();
			selection();
			crossover();
			mutation(10);
			copyAndReset();
			findMaxDistance();			 
			calculatePopulationFitness();
		}
	}
	
	/**
	 * This method copies the new enhanced population to the population list
	 * and clears the data structure for the next GA interation
	 */
	public void copyAndReset(){
		maxDistance = 0;			
		populationList.addAll(this.newPopulation);
		newPopulation.clear();
		matingPool.clear();
	}
	
	/**
	 * This method calculates the population fitness
	 * 
	 */
	public void calculatePopulationFitness(){
		for(int i = 0; i < populationList.size(); i++)		
			populationList.get(i).calculateFitness(maxDistance);
	}
	
	/**
	 * This method calculates the probability of each tour using the population fitness
	 * 
	 */
	public void calculateProbability(){
		double overallPopulationFitness = 0;
		
		// First calculate the overall fitness of the population
		for(int i = 0; i < populationList.size(); i++) 
			overallPopulationFitness += populationList.get(i).getFitness();
		
		// Using the overall fitness calculate the probability of each tour
		for(int i = 0; i < populationList.size(); i++) 
			populationList.get(i).calculateProbability(overallPopulationFitness);
	}
	
	/**
	 * This method calculated the Sampling rate
	 * 
	 */
	public void calculateSampling(){
		for(int i = 0; i < populationList.size(); i++) 
			populationList.get(i).calculateSampling(populationSize);
	}
	
	/**
	 * This method selects the fitter tour more often for the crossover operation
	 * 
	 */
	public void selection(){
		// Sort the population first
		sortPopultaion();
		
		// Remove the costly paths found
		while(populationSize < populationList.size()){
			populationList.remove(populationList.size()-1);
		}							
		
		// Take the healthier population for mating. The sampling rate makes sure 
		// that the population with higher will be selected more often for the mating.
		for(int i = 0; i < populationList.size(); i++){
			int sampCount = this.populationList.get(i).getSampling();
			for(int j = 0 ; j < sampCount; j++)
				matingPool.add(this.populationList.get(i));
		}		
		
	}	
	
	/**
	 * This method performs the crossover operation.
	 * 
	 */
	public void crossover(){
		ArrayList<Integer> visited = new ArrayList<Integer>();
		int parent1_index = 0;
		int parent2_index = 0;
		
		// Randomly select parents from the mating pool and then perform the
		// ordered crossover.
		Random rand = new Random();
		for(int i = 0; i < populationList.size()/2; i++){
			parent1_index = rand.nextInt(matingPool.size()-1);
			parent2_index = rand.nextInt(matingPool.size()-1);			
			
			// Select only a parent that is not already taken for mating.
			boolean parent1Visited = visited.contains(parent1_index);
			boolean parent2Visited = visited.contains(parent2_index);
			
			while((parent1_index == parent2_index) || (parent1Visited && parent2Visited)){	
				parent1_index = rand.nextInt(matingPool.size()-1);
				parent2_index = rand.nextInt(matingPool.size()-1);
				parent1Visited = visited.contains(parent1_index);
				parent2Visited = visited.contains(parent2_index);
			}						
			
			// Using parents found in above step perform ordered crossover
			orderedCrossover(matingPool.get(parent1_index), matingPool.get(parent2_index));			
			if(!parent1Visited)
				visited.add(parent1_index);			
			if(!parent2Visited)
				visited.add(parent2_index);								
		}					
	}
	
	/**
	 * This method performs the ordered crossover.
	 * 
	 * @param parent1 First parent needed for crossover.
	 * @param parent2 Second parent needed for crossover.
	 */
	public void orderedCrossover(TravelingPath parent1, TravelingPath parent2){		
		Random findPivot = new Random();
		List<City> cityList1 = parent1.getCityList();
		List<City> cityList2 = parent2.getCityList();
		TravelingPath childTour1, childTour2;
		
		int cityList1Size = cityList1.size();						
		
		int pivot1 = findPivot.nextInt(cityList1Size);
		int pivot2 = findPivot.nextInt(cityList1Size);
		while(pivot1 == pivot2)	pivot2 = findPivot.nextInt(cityList1Size);
		
		//Swap if pivot1 is smaller than pivot2
		if(pivot1 > pivot2){
			pivot1 += pivot2;
			pivot2  = pivot1 - pivot2;
			pivot1  = pivot1 - pivot2;
		}		
		
		int iterationSize = cityList1Size;
		
		ArrayList<City> child1 = new ArrayList<City>();
		ArrayList<City> child2 = new ArrayList<City>();
		
		for(int i = 0;i < cityList1Size; i++){
			child1.add(null);
			child2.add(null);
		}
		for(int i = pivot1;i <= pivot2;i++){
			child1.set(i, parent2.getCityList().get(i));
			child2.set(i, parent1.getCityList().get(i));
		}
		
		int pointer1 = pivot2+1;
		int pointer2 = pivot2+1;
		int check1 = pivot2+1;
		int check2 = pivot2+1;
		
		while(iterationSize>0){
			if(pointer1 >= cityList1Size)	pointer1 =0;
			if(pointer2 >= cityList1Size)	pointer2 =0;
			if(check1 	>= cityList1Size)	check1	 =0;
			if(check2 	>= cityList1Size)	check2	 =0;
			
			if(!child1.contains(cityList1.get(check1))){
				child1.set(pointer1, cityList1.get(check1));
				pointer1++;
			}
			if(!child2.contains(cityList2.get(check2))){
				child2.set(pointer2, cityList2.get(check2));
				pointer2++;
			}
			check1++;
			check2++;
			iterationSize--;
		}
		childTour1 = new TravelingPath(child1);
		childTour1.calculateDistance();
		childTour2 = new TravelingPath(child2);
		childTour2.calculateDistance();
		this.newPopulation.add(childTour1);
		this.newPopulation.add(childTour2);
	}
	
	/**
	 * This method performs the mutation
	 * 
	 * @param mutationRate This tells the mutation rate
	 */
	public void mutation(int mutationRate){
		int size = newPopulation.size();
		int pivot1 = 0, pivot2 = 0;
		City temp;
		Random generateNumber = new Random();		
		int mutationSize = size * mutationRate/100;
		int citySize = newPopulation.get(0).getCityList().size();
		int pathNumber = 0;
		
		for(int i = 0;i < mutationSize; i++){
			pathNumber = generateNumber.nextInt(size);
			pivot1 = generateNumber.nextInt(citySize);
			pivot2 = generateNumber.nextInt(citySize);
			
			while(pivot1 == pivot2)	pivot2 = generateNumber.nextInt(citySize);
			
			temp = newPopulation.get(pathNumber).getCityList().get(pivot1);
			newPopulation.get(pathNumber).getCityList().set(pivot1, newPopulation.get(pathNumber).getCityList().get(pivot2));
			newPopulation.get(pathNumber).getCityList().set(pivot2, temp);
		}
	}
	
	/**
	 * This method receives the tour migrated from the peer node
	 * 
	 * @param migrationPopulation Contains the migrated population
	 */
	public void receiveMigratingTours(ArrayList<TravelingPath> migrationPopulation){		
		for (int i = 0; i < 10; i++){			
			this.populationList.add(new TravelingPath(migrationPopulation.get(i).getCityList()));
		}
		// We have added new population to our population list so we need to calculate the maxdistance again
		findMaxDistance();
	}
	
	/**
	 * This method does the thread level migration of the population
	 * 
	 * @param migrationPopulation contains the migrated population
	 */
	public void threadMigration(ArrayList<TravelingPath> migrationPopulation){		
		receiveMigratingTours(migrationPopulation);
	}	
	
	/**
	 * This method returns the fitter population depending on the migrating rate 
	 * 
	 * @param migratingRate Tells the function how many population need to be extracted
	 * @return Returns the population that needs to be migrated
	 */
	public AList<TravelingPath> getBestPopulation(int migratingRate) {		
		//this.calculatePopulationFitness();
		// Sort the population first so that the fitter population is at the top of the list
		sortPopultaion();		
		AList<TravelingPath> migratingList = new AList<TravelingPath>();
		
		// Add the most fitter population to the migrating list
		for( int i = 0; i < migratingRate; i++ ) {
			TravelingPath tour = new TravelingPath(populationList.get(i));
			migratingList.addLast(tour);
		}
		
		return migratingList;
	}

	/**
	 * This method sorts the population making the fitter population at the top of the list
	 */
	public void sortPopultaion(){
		Collections.sort(this.populationList, new Comparator<TravelingPath>(){
		     public int compare(TravelingPath o1, TravelingPath o2){
		         if(o1.getFitness() == o2.getFitness())
		             return 0;
		         return o1.getFitness() < o2.getFitness() ? 1 : -1;
		     }
		});		
	}
	
	/**
	 * This method returns the population list
	 * 
	 * @return population list
	 */
	public ArrayList<TravelingPath> getPopulationList() {
		return populationList;
	}
}
