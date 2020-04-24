package cache;

public class MemoryHierarchy {
	private int layers;				// # of Layers/Caches in memory hierarchy
	private int policy;				// Write/Allocate Policy of hierarchy: 0 for write-back and write-allocate; 1 for write-through and non-write-allocate
	private int blocksize;			// Number of data locations within block
	private int outstandingMisses;	// Number of Misses to be allowed to be in the buffer at one time
	private Cache[] caches;			// Array of caches, larger the index, the deeper the cache in the hierarchy
	private int mem;
	private int memLatency;
	private int currLatency;
	
	/*
	 * TODO Add policy				- Done
	 * TODO Add latency Checkpoints - Done
	 * TODO Add memory				- Done
	 * TODO Try to make parallel
	 * TODO TEST
	 */
	
	/**
	 * Initialize the Memory Hierarchy
	 * 
	 * @param layers 			-> 		# of Layers/Caches in memory hierarchy
	 * @param policy 			-> 		Write/Allocate Policy of hierarchy
	 * @param blocksize 		-> 		Number of data locations within block
	 * @param sizes 			->		Array of sizes of the caches
	 * @param setAssociatives	->		Array of # of sets within a cache
	 * @param latencies			-> 		Array of latencies of the caches
	 */
	public MemoryHierarchy(int layers, int policy, int blocksize, int outstandingMisses, int[] sizes, int[] setAssociatives, int[] latencies) {
		this.layers = layers;
		this.policy = policy;
		this.blocksize = blocksize;
		this.outstandingMisses = outstandingMisses;
		this.caches = new Cache[this.layers];
		this.mem = 1;
		this.memLatency = 0;
		this.currLatency = 0;
		
		// Initialize each cache
		for(int i = 0; i < this.layers; i++) {
			this.caches[i] = new Cache(sizes[i], setAssociatives[i], latencies[i], blocksize);
		}
	}
	
	
	public void writeData(int address, int data) {
		System.out.println(data);
		// Write Data into each cache level IF there is a new row 
		for(int cache = 0; cache < this.layers; cache++) {
			System.out.println("Writing At Cache " + cache);
			//Write-Back/Allocate
			if(policy == 0) {
				CacheRow evictedRow = this.caches[cache].writeBackData(address, data);
				
				//Perfect write, no allocation needed. No copying needed.
				if(cache == 0 && evictedRow == null) break;
				
				System.out.println("Current Outstanding Misses: " + this.caches[cache].getCurrMisses());
				
				// We had to evict a row, now must write it back to memory (Write-Back Policy)
				// We do NOT have to write it to the other caches, since they are kept up to date.
				if(evictedRow != null && evictedRow.getDirty()) {
					// Fake memory Write
					System.out.println("MEMORY WRITE!");
					this.memLatency += 100 + this.caches[this.caches.length - 1].getLatency();
				}
				System.out.println("Current Outstanding Misses: " + this.caches[cache].getCurrMisses());
			}
			else {
				this.caches[cache].writeThroughData(address, data);
			}
		}
		if(this.policy == 1) {
			// Write into memory as well. Fake memory write
			System.out.println("MEMORY WRITE!");
			this.memLatency += 100 + this.caches[this.caches.length - 1].getLatency();
		}
	}
	
	public void readData(int address) {
		int[] data = new int[this.blocksize];
		boolean hit = false;
		
		int c;
		for(c = 0; c < this.layers; c++) {
			ValidData cacheResult = this.caches[c].readDataFromCache(address);
			
			System.out.println("Reading At Cache " + c);
			
			if(cacheResult.getValid()) {
				data = cacheResult.getData();
				hit = true;
				System.out.println("READ HIT!");
				break;
			}
			//System.out.println("Current Outstanding Misses: " + this.caches[c].getCurrMisses());
			// If a cache miss occurs, continue on to the next cache and update the top layer cache with LRU.
		}
		
		// Complete cache miss. Must read from Memory.
		if(!hit) {
			System.out.println("READ MISS!");
			this.memLatency += 100 + this.caches[this.caches.length - 1].getLatency();
			int block = (address % this.blocksize);
			for(int i = 0; i < this.blocksize; i++) {
				if(i == block)
					data[i] = this.mem;
				else
					data[i] = 0;
			}
			this.mem++;
		}
		
		//Update the layers of cache
		for(int i = 0; i < c; i++) {
			CacheRow evictedRow = this.caches[i].evictRow(address, data);
			//System.out.println("Current Outstanding Misses: " + this.caches[i].getCurrMisses());
			// Write-Back Action
			if(evictedRow.getDirty()) {
				int evictAddress = (evictedRow.getTag() << (int)Math.floor(this.caches[i].getIndexSize()/2) | evictedRow.getIndex());
				
				for(int j = i+1; j < this.layers; j++) {
					int block = (evictAddress % blocksize);
					CacheRow temp = this.caches[j].evictRow(evictAddress, evictedRow.getBlockData());
					
					
					//This evicted row should never be dirty since its within deeper caches, thus never needing to be saved.
					//Can be wrong about this TODO
					if(temp != null) System.out.println("OOOPS PLEASE FIX");
				}
			}
		}
	}
	
	
    /**
     * Get Caches
     */
	public Cache[] getCaches() {
		return this.caches;
	}
	
	/**
	 * Get Total Latency (Cache Accesses and Memory Access)
	 */
	public int getLatency() {
		int latency = 0;
		for(Cache c : this.getCaches()) {
			latency += c.getTotalLatency();
		}
		return latency + this.memLatency;
	}
	
	
	/**
	 * Get Latency of the last read access.
	 */
	public int getCurrentLatency() {
		int accessLatency = getLatency() - this.currLatency;
		this.currLatency += accessLatency;
		return accessLatency;
	}
	
	
	/**
	 * Return the status of the Memory Hierarchy
	 */
	public String getStatus() {
		StringBuilder sb = new StringBuilder();
		
		//Get Latencies, Hit/Miss Rates, and Cache Print-outs
		int i = 0;
		for(Cache c : this.getCaches()) {
			sb.append("\nCache " + i + " Total Latency: " + c.getTotalLatency());
			sb.append("\nCache " + i + " Hit Rate: " + c.getHitRate());
			sb.append("\nCache " + i + " Miss Rate: " + c.getMissRate());
			sb.append("\n" + c.toString());
			i++;
		}
		return sb.toString();
	}

}
