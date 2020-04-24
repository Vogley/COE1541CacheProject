package cache;
import java.io.*;
import java.util.*; 

public class Main {
	private static ParallelMemoryHierarchy pmh;
	private static int iterID = 0;
	private static int iterData = 0;
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("_____________________________\n| *   *   *   *   *   *   * |\n|   *   *   *   *   *   *   |\n| Cache Project for COE1541 |\n|\t Tyler Vogel\t    |\n| *   *   *   *   *   *   * |\n|   *   *   *   *   *   *   |\n-----------------------------");
		
		System.out.println("Please Enter the Number of Cache Layers: ");
		int cacheNumber = Integer.parseInt(scanner.next());
		
		System.out.println("Please Enter the Policy (0 for write-back and write-allocate, 1 for write-through and non-write-allocate): ");
		int policy = Integer.parseInt(scanner.next());
		
		System.out.println("Please Enter the Blocksize: ");
		int blocksize = Integer.parseInt(scanner.next());
		
		System.out.println("Please Enter the Max Outstanding Misses: ");
		int outstandingMisses = Integer.parseInt(scanner.next());
		
		int[] sizes = new int[cacheNumber];
		int[] setAssociatives = new int[cacheNumber];
		int[] latencies = new int[cacheNumber];
		for(int i = 0; i < cacheNumber; i++) {
			System.out.println("\nPlease Enter the Size for Cache " + (i+1) + ": ");
			sizes[i] = Integer.parseInt(scanner.next());
			
			int setTemp = 1;
			do{
				if(sizes[i] % setTemp != 0 || !isPowerOfTwo(setTemp))
					System.out.println("ERROR: Set Associative Number must be a dividend of the cache size and also must be a power of 2.");
				System.out.println("Please Enter the Number of Sets for Cache " + (i+1) + ": ");
				setTemp = Integer.parseInt(scanner.next());
			}while(sizes[i] % setTemp != 0 || !isPowerOfTwo(setTemp));
			setAssociatives[i] = setTemp;
			
			System.out.println("Please Enter the Latency for Cache " + (i+1) + ": ");
			latencies[i] = Integer.parseInt(scanner.next());
		}
		
		pmh = new ParallelMemoryHierarchy(cacheNumber, policy, blocksize, outstandingMisses, sizes, setAssociatives, latencies);
		
		//pmh = new ParallelMemoryHierarchy(3, 1, 2, 5, new int[]{8, 16, 32}, new int[]{4, 4, 4}, new int[]{1, 5, 10});
		
		// Read File of Instructions
		boolean nofile = false;
		Queue<Request> requests = new LinkedList<>();
		do {
			System.out.println("\nPlease Enter the filename for Cache Accesses: ");
			String filename = scanner.next().trim();
			try {
				Scanner fs = new Scanner(new File(filename));
				System.out.println("\nStarting Instruction List...\n--------------------------");
				while (fs.hasNextLine()) {
					nofile = false;
					 requests.add(decode(fs.nextLine()));
				}
				fs.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				nofile = true;
			}
		}while(nofile);
		
		
		// Run requests
		Request currRequest = null;
		boolean newRequest = true;
		boolean finished = false;
		int status;
		int time = 0;
		// Continue running until all caches are finished and there are no more requests in the file.
		while(!(finished && requests.isEmpty())) {
			// Only send a new request if the caches ask for one, there is one to give, and the request's arrivial time is here.
			if(newRequest && !requests.isEmpty() && requests.peek().getTime() <= time) {
				currRequest = requests.remove(); 
				System.out.println("Request Sent! Request: " + currRequest);
				newRequest = false;
			}
			//The last request must be used before this can be set to null
			else if(newRequest){
				currRequest = null;
			}
			
			/**
			 * THIS IS THE MAIN CYCLE
			 */
			status = pmh.cycle(currRequest, time);
			
			// Status 0: Request was used up
			if(status == 0) {
				//System.out.println("Request Used! Request: " + currRequest);
				finished = false;
				newRequest = true;
			}
			// Status -1: All caches are done
			else if(status == -1) {
				finished = true;
			}
			// Other statuses mean the cache is busy.
			
			time++;
		}
		
		
		System.out.println("\nEnd of Instructions. Now Showing Memory Heirarchy Status.\n_______________________________________________________________\n");
		System.out.println("\n* * * * * * * * * * * * * * * \nCache Access Report\n* * * * * * * * * * * * * * * ");
		System.out.println("Total Latency: " + time);
		System.out.print(pmh.getStatus());
		
		
	}

	
	/**
	 * Form a request from a string. Incorrect formats won't be handled.
	 * @param nextLine
	 * @return
	 */
	private static Request decode(String nextLine) {
		String[] instruction = nextLine.split(" ");
		int id = -1000 + iterID;
		int data = 100 - iterData;
		int address = 0;
		
		if(Integer.parseInt(instruction[2]) == 10)
			address = Integer.parseInt(instruction[1]);
		else if(Integer.parseInt(instruction[2]) == 2)
			address = Integer.parseInt(instruction[1], 2);
		else if(Integer.parseInt(instruction[2]) == 16)
			address = Integer.parseInt(instruction[1], 16);
		else {
			System.out.println("Incorrect Instruction Format.");
			return null;
		}

		if(instruction[0].compareTo("r") == 0) {
			iterID++;
			return new Request(id, 0, address, 0, Integer.parseInt(instruction[3]), Integer.parseInt(instruction[3]));
		}
		else {
			iterID++;
			iterData++;
			return new Request(id, 1, address, data, Integer.parseInt(instruction[3]), Integer.parseInt(instruction[3]));
		}

	}
	
	
	/**
	 * Helper function to check if inputs are feasible.
	 * @param n
	 * @return
	 */
    private static boolean isPowerOfTwo(int n) 
    { 
        return (int)(Math.ceil((Math.log(n) / Math.log(2))))  
            == (int)(Math.floor(((Math.log(n) / Math.log(2))))); 
    } 
}
