
import java.io.IOException;
import java.util.ArrayList;

import edu.rit.io.InStream;
import edu.rit.io.OutStream;
import edu.rit.pj2.Job;
import edu.rit.pj2.Loop;
import edu.rit.pj2.Task;
import edu.rit.pj2.Tuple;
import edu.rit.pj2.tuple.ObjectTuple;
import edu.rit.util.AList;
import edu.rit.util.Instance;

/**
 * Class SmpTSPMain is a parallel program to solve traveling sales man problem
 * 
 * * <P>
 * Usage: java pj2 jar={@literal <jarfile>} workers={@literal <k>} seqTSPMain {@literal "<ctor>" <populationsize> <GAiterations> <MigrationCount>} <br>
 * {@literal <jarfile>} = Name of the java archieve file containing all the java class files. <br>
 * {@literal <k>} = The number of worker tasks. <br>
 * {@literal <ctor>} = Constructor expression of the input graph.
 * {@literal <populationsize>} = The size of the population.
 * {@literal <GAiterations>} = The iterations required for genetic algorithm to run.
 * {@literal <MigrationCount>} = The number of migrations of best tours among Nodes in a cluster.
 * 
 * @author Sahil Jasrotia, Lokesh Agrawal
 * 
 */
public class SmpTSPMain extends Job {

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";	
	public static final String ANSI_CYAN  = "\u001B[36m";
	
	private TravelingPath initTour;
	PointGroup pg;		
	private int numCities;
	
	/**
	 * Job main program.
	 */
	public void main(String[] args) throws Exception {
				
		try{
			// Raise error if insufficient arguments.
			if(args.length != 4)
				usage();
			
			// verify parameters
			checkParameters(args);
						
			// Create an intial tour.
			createTravellingPath(args[0]);
						
			// put the initial tour into the tuple space.
			putTuple( new ObjectTuple<TravelingPath>(initTour) );			
			
			// Set up a task group of K worker tasks.
			rule().task(workers(), WorkerTask.class).args(Integer.toString(workers()),args[1],args[2],args[3]);
			
			// Set up reduction task.
			rule().atFinish().task(ReduceTask.class).runInJobProcess().args();
		}
		catch(Exception e){
			usage();
		}				
	}
	
	/**
	 * Print a usage message and exit.
	 */
	private static void usage() {
		System.err.println("Usage: java pj2 jar=<jarfile> workers=<k> seqTSPMain <ctor> <populationsize> <GAiterations>");
		System.err.println("<jar> Name of the java archieve file containing all the java class files. ");
		System.err.println("<workers> Number of workers.");
		System.err.println("<ctor> The constructor expression. The first argument to constructor expression is"
							+ "Number of cities.");
		System.err.println("<populationsize> The size of the population.");
		System.err.println("<GAiterations> Number of genetic algorithm iterations.");		
		System.err.println("<MigrationCount> The number of migrations of best tours among Nodes in a cluster.");
		terminate(1);
	}
	
	/**
	 * Check if the arguments 1 2 and 3 are integers.
	 * This method will throw an exception if the arguments are not integers.
	 * The exception will be captured by the main program which will exit the program.
	 * 
	 * @param args Command line arguments.
	 */
	private static void checkParameters(String args[]){
		Integer.parseInt(args[1]);
		Integer.parseInt(args[2]);
		Integer.parseInt(args[3]);
	}
	
	/**
	 * This method creates initial tour for the genetic algorithm
	 * 
	 * @param pointGroup Contains constructor for creating pointgroup instance
	 * @throws Exception Instance class throws the exception
	 */
	public void createTravellingPath(String pointGroup) throws Exception{	
		// create an instance of the point group.
		this.pg = (PointGroup) Instance.newInstance (pointGroup);
		
		// Get the number of cities that user has entered.
    	numCities = pg.N();
    	
    	// id used as city identifier
    	int id = 0;
    	
    	// Initial tour list
    	ArrayList<City> initTour = new ArrayList<City>();
    	City city = new City();    	
    	
    	// Get the points from the point group class and put it in points array
		for (int i = 0; i < numCities;  i++){
			pg.nextPoint(city);
			initTour.add(new City(city.x,city.y,id++));			
		}
		// initial tour initialization
		this.initTour = new TravelingPath(initTour);											
	}
	
	/**
	 * Class WorkerTask runs the genetic algorithm for the given number of iterations	 
	 * 
	 */
	private static class WorkerTask extends Task {

		private TravelingPath initTour;
		private int popSize;		
		private int iterGA;		
		private int migrationCount;
		private Population population[] = new Population[4];		
		int taskRank;
		int prevNodeRank;
		int numWorkers;
		int initializationCount = 0;
		private AList<TravelingPath> sendTopTours = new AList<TravelingPath>();
		private ArrayList<TravelingPath> receiveTopTours = new ArrayList<TravelingPath>();		
		
		/**
		 * Worker task main program
		 */
		public void main(String[] args) throws Exception {
			
			// read the tours 
			initTour = readTuple(new ObjectTuple<TravelingPath>()).item;
			
			numWorkers = Integer.parseInt(args[0]);
			// number of generations for genetic algorithm 
			iterGA = Integer.parseInt(args[2]);			
			
			// Get the population size
			popSize = Integer.parseInt(args[1]);
			
			// Get the migration rate
			migrationCount = Integer.parseInt(args[3]);
			
			// Get the task rank
			taskRank = taskRank();								
			
			// Get the previous neighboring node task rank
			prevNodeRank = taskRank - 1;
			
			if( prevNodeRank == -1 ) {
				prevNodeRank = numWorkers - 1;
			}						
			
			for( int iter = 0; iter < migrationCount; iter++ ) {				
				// run genetic algorithm in all cores independently
				parallelFor(0,3).exec( new Loop() {
									
					int rank;		// To store thread rank
					int nextThread; // To store neighboring thread rank
								
					public void start( ) throws Exception {
				
						// rank of this core
						rank = rank();						
						// rank of neighbor core
						nextThread = (rank + 1) % 4;							
															
						if( initializationCount <= 3 ) {	
							initializationCount++;
							population[rank] = new Population (initTour,popSize);								
							population[rank].createPopulation();																														
						}																		
					}

					public void run(int args) throws Exception {
						
						// Check if we have received the top tours from neighboring node
						if( !receiveTopTours.isEmpty() ) {
							population[rank].receiveMigratingTours(receiveTopTours);							
						}			
						// Run genetic algorithm
						population[rank].startGA(iterGA);						
						
						// Thread 0 will send the migrating tours to the 3rd thread of the neighboring node.
						// Migration is done in anti clock wise fashion.
						if(rank == 0) {																														
							sendTopTours = population[0].getBestPopulation(10);							
							putTuple(1, new PathTuple(sendTopTours,prevNodeRank,3));							
						}
						
						// All other threads except 3rd thread will do intra node migration.
						if( rank!= 3 ){
							
							population[rank].threadMigration( population[nextThread].getPopulationList());							
						}
						else{
							// Thread 3 will get the migrating tours from the 0th thread of the neighboring node.
							// Migration is done in anti clock wise fashion. 
							PathTuple template = new PathTuple();
							PathTuple pathTuple = null;
							template.taskRank = taskRank;							
							template.threadRank = rank;
						
							pathTuple = takeTuple(template);														
							pathTuple.getTourList(receiveTopTours);
						}						
					}									
			
				} );																		
				// Clear the structures after each migration								
				sendTopTours.clear();
				receiveTopTours.clear();
				
				// We have done the migration in this node, now get the best tour from the four cores,
				// and put the best tour out of the four core to the result tuple to do inter node reduction.
				if( iter == migrationCount - 1 ) {		
					TravelingPath bestTour = new TravelingPath(population[0].getPopulationList().get(0));					
					for( int i = 1; i < population.length; i++ ) {
						if(bestTour.getEuclideanDistance() < population[i].getPopulationList().get(0).getEuclideanDistance())
							bestTour = population[i].getPopulationList().get(0);
					}																									
					putTuple( new ResultTuple(bestTour));
					
				}					
			}																								
		}
	}
	
	/**
	 * Class PathTuple is used to send best tour in tuple space from a node
	 * to the neighboring node in the cluster
	 * 
	 * @author Sahil Jasrotia, Lokesh Agrawal
	 * 
	 */
	private static class PathTuple extends Tuple {

		AList<TravelingPath> tourList = new AList<TravelingPath>(); // Tour list.
		int taskRank; 		// Worker task rank
		int threadRank;     // Thread rank of the core
		
		/**
		 * Default constructor to create path tuple object
		 */
		public PathTuple() {
			
		}
		
		/**
		 * parameterized constructor initialize the PathTuple object
		 * 
		 * @param tourList AList of best tours
		 * @param taskRank taskRank of the node
		 * @param rank thread rank of the core
		 */
		public PathTuple( AList<TravelingPath> tourList , int taskRank, int rank ) {
			this.tourList = tourList;
			this.taskRank = taskRank;
			this.threadRank = rank;
		}
		
		/**
		 * This overridden method matches the content of the this tuple with the target tuple
		 * 
		 * @param target to be matched with this tuple
		 */
		public boolean matchContent(Tuple target) {
			PathTuple tourTuple = (PathTuple) target;
			return this.taskRank == tourTuple.taskRank && this.threadRank == tourTuple.threadRank;
		}
		
		/**
		 * Reads the object parameters from the input stream
		 *
		 * @param inStream Input stream
		 * 
		 * @exception IOException Throws the IO exception if there is an error		 
		 */

		@SuppressWarnings("unchecked")
		public void readIn(InStream inStream) throws IOException {
			tourList = (AList<TravelingPath>) inStream.readObject();
			taskRank = inStream.readInt();
			threadRank = inStream.readInt();
		}

		/**
		 * Writes the object parameters to the out stream
		 *
		 * @param outStream output stream
		 * 
		 * @exception IOException Throws the IO exception if there is an error		 
		 */
		public void writeOut(OutStream outStream) throws IOException {
			outStream.writeObject(tourList);
			outStream.writeInt(taskRank);
			outStream.writeInt(threadRank);
		}
		
		/**
		 * This method returns the best tour list.
		 * 
		 * @param getTourList get migrated tour		 
		 */
		public void getTourList(ArrayList<TravelingPath> receiveTopTours ) {
			
			for( int i = 0; i < tourList.size(); i++ ) {
				receiveTopTours.add(tourList.get(i));				
			}								
		}		
	}
	
	/**
	 * Class ResultTuple Contains the intermediate and final results after reduction. 
	 * 
	 * @author Sahil Jasrotia, Lokesh Agrawal
	 * 
	 */
	private static class ResultTuple extends Tuple {
		
		public TravelingPath bestTour = new TravelingPath();		
		
		/**
		 * Default constructor to create bestTour
		 * 
		 */
		public ResultTuple() {
		
		}
		
		/**
		 * Parameterized constructor to create bestTour
		 * 
		 * @param bestTour tour that is best of all tours 		 
		 */
		public ResultTuple( TravelingPath bestPath) {
			bestTour = bestPath;							
		}

		/**
		 * This is the reduction method to reduce the best tours depending on the 
		 * distance between a tour
		 * 
		 * @param resultTuple the result tuple object
		 */
		public void reduce(ResultTuple resultTuple) {					
			if( bestTour.getEuclideanDistance() == 0 ) {
				bestTour = resultTuple.bestTour;								
			}
			else {
				if( resultTuple.bestTour.getEuclideanDistance() < bestTour.getEuclideanDistance() ) {
					bestTour = resultTuple.bestTour;					
				}
			}
		}
		
		/**
		 * Read the fields of this tuple from the given input stream
		 * 
		 * @param in The input stream
		 * 
		 * @exception  IOException Throws the IO exception if there is an error
		 */
		public void readIn(InStream inStream) throws IOException {
			bestTour = (TravelingPath) inStream.readObject();			
		}

		/**
		 * Write the fields of this tuple to the out stream
		 * 
		 * @param out The output stream
		 * 
		 * @exception  IOException Throws the IO exception if there is an error
		 */
		public void writeOut(OutStream outStream) throws IOException {
			outStream.writeObject(bestTour);			
		}
		
		/**
		 * Print the result 
		 * 		 
		 */
		public void printResults() {					
			
			// Get the citylist from the best tour we have
			ArrayList<City> cityList  = bestTour.getCityList();			
			
			// Print the city id to identify city in a tour.
			for( int i = 0; i < cityList.size(); i++ ) {
				System.out.print(ANSI_CYAN + cityList.get(i).id + ANSI_RESET);
				System.out.print(ANSI_CYAN + "-->" + ANSI_RESET);
			}
			System.out.println(ANSI_CYAN + cityList.get(0).id +"\n"+ ANSI_RESET);
			
			// Print the optimal distance.
			System.out.println(ANSI_GREEN + "OPTIMAL DISTANCE: " + ANSI_RESET);
			System.out.printf(ANSI_CYAN);
			System.out.printf ("%.3f", bestTour.getEuclideanDistance());
			System.out.printf ("\n" + ANSI_RESET);
			
		}				
	}
	
	/**
	 * The ReduceTask class is used to get results from all the nodes and then reduces
	 * the result to get the final result.	  
	 * 
	 * @author Lokesh Agrawal, Sahil Jasrotia
	 * 
	 */
	private static class ReduceTask extends Task { 
		
		/** 
		 * Reduce task main program.
		 */
		public void main(String[] args) throws Exception {
						
			ResultTuple template = new ResultTuple();
			ResultTuple resultTuple = new ResultTuple();
			ResultTuple result = null;
			
			// Try to take result tuple from the tuple space
			while( ( result = tryToTakeTuple(template) ) != null )
			{
				// Reduce the result to get the final tour with minimum traveling distance.
				resultTuple.reduce(result);				
			}
			
			// print the results.
			System.out.println(ANSI_GREEN + "OPTIMAL PATH: " + ANSI_RESET);
			resultTuple.printResults();			
		}		
	}
}