package cache;
import java.io.*;
import java.util.*; 

public class Main {
	private static MemoryHierarchy mh;
	private static int iter = 0;

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
		
		mh = new MemoryHierarchy(cacheNumber, policy, blocksize, outstandingMisses, sizes, setAssociatives, latencies);
		
		//mh = new MemoryHierarchy(3, 0, 2, 5, new int[]{8, 16, 32}, new int[]{4, 4, 4}, new int[]{1, 10 , 100});
		
		// Read File of Instructions
		boolean nofile = false;
		do {
			System.out.println("\nPlease Enter the filename for Cache Accesses: ");
			String filename = scanner.next().trim();
			try {
				Scanner fs = new Scanner(new File(filename));
				System.out.println("\nStarting Instruction List...\n--------------------------");
				while (fs.hasNextLine()) {
					nofile = false;
					decode(fs.nextLine());
					System.out.println("Latency of Cache Access: " + mh.getCurrentLatency() + "\n");
				}
				fs.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				nofile = true;
			}
		}while(nofile);
		
		
		System.out.println("\nEnd of Instructions. Now Showing Memory Heirarchy Status.\n_______________________________________________________________\n");
		System.out.println("\n* * * * * * * * * * * * * * * \nCache Access Report\n* * * * * * * * * * * * * * * ");
		System.out.println("Total Latency: " + mh.getLatency());
		System.out.print(mh.getStatus());
		
		
	}

	private static void decode(String nextLine) {
		String[] instruction = nextLine.split(" ");
		int data = 100 - iter;
		int address = 0;
		
		if(Integer.parseInt(instruction[2]) == 10)
			address = Integer.parseInt(instruction[1]);
		else if(Integer.parseInt(instruction[2]) == 2)
			address = Integer.parseInt(instruction[1], 2);
		else if(Integer.parseInt(instruction[2]) == 16)
			address = Integer.parseInt(instruction[1], 16);
		else {
			System.out.println("Incorrect Instruction Format.");
			return;
		}

		if(instruction[0].compareTo("r") == 0) {
			mh.readData(address); 
		}
		else {
			mh.writeData(address, data);
			iter++;
		}

	}
	
    private static boolean isPowerOfTwo(int n) 
    { 
        return (int)(Math.ceil((Math.log(n) / Math.log(2))))  
            == (int)(Math.floor(((Math.log(n) / Math.log(2))))); 
    } 
}
