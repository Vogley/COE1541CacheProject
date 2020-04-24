package cache;

import java.util.Arrays;

public class Cache {
	private int size;			// Total Size of Cache
	private int numSets;		// # of Sets in Cache
	private int latency;		// Latency of the Cache
	private int blocksize;		// Block size of Cache
	private int currMisses;		// Current Number of Misses
	private double accesses;	// Number of accesses
	private double misses;		// Number of misses
	private CacheRow[] rows;	// Contains the content of the Cache
	
	private int indexSize;		// # of index locations in one set.

	/** - - - - - - - - - - - - - - - - - - - - - - 
	 * 		Cache Format
	 * 
	 * 		Valid		Index	Tag			Data
	 * 		  x			xx		xxxx	xxxxxxxx
	 * 	 	  x			xx		xxxx	xxxxxxxx
	 * 	 	  x			xx		xxxx	xxxxxxxx
	 * 	  	  x			xx		xxxx	xxxxxxxx
	 * 	  	  x			xx		xxxx	xxxxxxxx
	 * 		 ...	    ...		...		...
	  - - - - - - - - - - - - - - - - - - - - - - */
	
	
	/**
	 * Initialize Blank Cache
	 * 
	 * @param size
	 * @param numSets
	 * @param latency
	 */
	public Cache(int size, int numSets, int latency, int blocksize) {
		this.size = size;
		this.numSets = numSets;
		this.latency = latency;
		this.blocksize = blocksize;
		this.currMisses = 0;
		this.accesses = 0;
		this.misses = 0;
		this.rows = new CacheRow[this.size];
		
		// Creating each row of the cache
		int setSize = size/numSets;
		int set = 0;
		
		int i;
		for(i = 0; i < this.size;  i++) {
			this.rows[i] = new CacheRow(set, i % setSize, this.blocksize);
			if(i % setSize == setSize - 1)
				set++;
		}
		
		// Record the number of index addresses needed
		this.indexSize = setSize;
	}
	
	
	/**
	 * Write data into location in cache with the write-back policy.
	 * 
	 * @param address
	 * @param data
	 * @return 
	 */
	public CacheRow writeBackData(int address, int data) {
		int indexMask = (int) Math.pow(2, log2(this.indexSize)) - 1;
		int blockMask = (int) Math.pow(2, log2(this.blocksize)) - 1;
		int tag = (address >> (int) (log2(this.blocksize))) >> (int) (log2(this.indexSize));
		int index = ((address >> (int) (log2(this.blocksize))) & indexMask);
		int block = (address & blockMask);
		this.accesses++;
		
		// Needed for LRU updates
		CacheRow[] set = new CacheRow[this.numSets];
		int oldLRU = -1;
		
		boolean hit = false;
		for(int i = 0; i < this.numSets; i++) {
			CacheRow currRow = this.rows[index + i*this.indexSize];
			set[i] = currRow;
			if(currRow.getValid() && currRow.getTag() == tag) {
				hit = true;
				System.out.println("WRITE HIT!");
				oldLRU = currRow.getLRU();
				currRow.setLRU(0);
				currRow.setDirty(true);
				currRow.setData(data, block);
				set[i] = null;
			}
		}

		// Update all LRUs due to write hit
		if(hit) {
			for(CacheRow s: set) {
				if(s != null && s.getLRU() < oldLRU)
					s.setLRU(s.getLRU() + 1);
			}
		}
		// Invalid location or Tag was not found
		else {
			this.currMisses++;
			int[] dataArray = new int[this.blocksize];
			for(int i = 0; i < this.blocksize;i++) {
				dataArray[i] = data;
			}
			CacheRow evictedRow = evictRow(address, dataArray);
			this.misses++;
			return evictedRow;
		}
		
		return null;
	}
	
	
	/**
	 * Write data into location in cache with write through policy.
	 * 
	 * @param address
	 * @param data
	 * @return 
	 */
	public void writeThroughData(int address, int data) {
		int indexMask = (int) Math.pow(2, log2(this.indexSize)) - 1;
		int blockMask = (int) Math.pow(2, log2(this.blocksize)) - 1;
		int tag = (address >> (int) (log2(this.blocksize))) >> (int) (log2(this.indexSize));
		int index = ((address >> (int) (log2(this.blocksize))) & indexMask);
		int block = (address & blockMask);
		this.accesses++;
		
		// Needed for LRU updates
		CacheRow[] set = new CacheRow[this.numSets];
		int oldLRU = -1;
		
		boolean hit = false;
		for(int i = 0; i < this.numSets; i++) {
			CacheRow currRow = this.rows[index + i*this.indexSize];
			set[i] = currRow;
			if(currRow.getValid() && currRow.getTag() == tag) {
				hit = true;
				System.out.println("WRITE HIT!");
				oldLRU = currRow.getLRU();
				currRow.setLRU(0);
				currRow.setDirty(true);
				currRow.setData(data, block);
				set[i] = null;
			}
		}
		
		// Update all LRUs due to write hit
		if(hit) {
			for(CacheRow s: set) {
				if(s != null && s.getLRU() < oldLRU)
					s.setLRU(s.getLRU() + 1);
			}
		}
		
		// Invalid location or Tag was not found
		// Do Nothing
	}

	
	/**
	 * Read memory from a location in the cache
	 * 
	 * @param address
	 * @return A object with a boolean of hit/miss, and the data.
	 */
	public ValidData readDataFromCache(int address) {
		int indexMask = (int) Math.pow(2, log2(this.indexSize)) - 1;
		int blockMask = (int) Math.pow(2, log2(this.blocksize)) - 1;
		int tag = (address >> (int) (log2(this.blocksize))) >> (int) (log2(this.indexSize));
		int index = ((address >> (int) (log2(this.blocksize))) & indexMask);
		int block = (address & blockMask);
		this.accesses++;
		
		//System.out.println(String.format("blockMask: %d, indexMask: %d", blockMask, indexMask));
		//System.out.println(String.format("blocksize: %d, indexsize: %d", (int) (log2(this.blocksize)), (int) (log2(this.indexSize))));
		//System.out.println(String.format("Tag: %d, Index: %d, block: %d", tag, index, block));
		
		// Search through sets to see if Tag and Valid bit match.
		ValidData target = new ValidData(false, new int[this.blocksize]);
		for(int i = 0; i < this.numSets; i++) {
			CacheRow currRow = this.rows[index + i*this.indexSize];
			if(currRow.getValid() && currRow.getTag() == tag) {
				target.setData(this.rows[index + i*this.indexSize].getBlockData());
				target.setValid(true);
			}
		}
		
		// If we return a miss, we need to update the location, by grabbing data from the lower memory source
		// and updating the cache. This is done in the memory hierarchy. 
		if(!target.getValid()) {
			this.misses++;
			this.currMisses++;
		}
		return target;
	}
	
	
	/**
	 * Very similar to writeData, except it will evict a data slot using LRU.
	 * 
	 * @param address
	 * @param data
	 * @return evicted row
	 */
	public CacheRow evictRow(int address, int[] data) {
		int indexMask = (int) Math.pow(2, log2(this.indexSize)) - 1;
		int blockMask = (int) Math.pow(2, log2(this.blocksize)) - 1;
		int tag = (address >> (int) (log2(this.blocksize))) >> (int) (log2(this.indexSize));
		int index = ((address >> (int) (log2(this.blocksize))) & indexMask);
		int block = (address & blockMask);
		CacheRow evictedRow = null;
		
		// Search through sets to see if Tag and Valid bit match.
		for(int i = 0; i < this.numSets; i++) {
			CacheRow currRow = this.rows[index + i*this.indexSize];
			if(currRow.getLRU() == this.numSets-1) {
				// Grab row with highest LRU
				// Evict LRU row using the cache's write policy. This is done in the memory hierarchy.
				evictedRow = new CacheRow(currRow.getSet(), currRow.getIndex(), this.blocksize);
				evictedRow.setLRU(currRow.getLRU());
				evictedRow.setTag(currRow.getTag());
				evictedRow.setBlockData(currRow.getBlockData());
				evictedRow.setDirty(currRow.getDirty());
				evictedRow.setValid(currRow.getValid());
				
				currRow.setLRU(0);
				currRow.setTag(tag);
				currRow.setBlockData(data);
				currRow.setDirty(false);
				currRow.setValid(true);
			}
			else {
				currRow.setLRU(currRow.getLRU()+1);
			}
		}
		this.currMisses--;
		return evictedRow;
	}
	
	
    /**
     * Get Size of Index (For writing purposes)
     */
	public int getIndexSize() {
		return this.indexSize;
	}
	
	
    /**
     * Get Latency
     */
	public int getLatency() {
		return this.latency;
	}
	
	
    /**
     * Get Total Latency
     */
	public int getTotalLatency() {
		return (int) (this.latency*this.accesses);
	}
	
	
    /**
     * Get Hit Rate
     */
	public double getHitRate() {
		return (this.accesses - this.misses)/this.accesses;
	}
	
	
    /**
     * Get Miss Rate
     */
	public double getMissRate() {
		return this.misses/this.accesses;
	}
	
	
    /**
     * Get Current Amount of Misses
     */
	public int getCurrMisses() {
		return this.currMisses;
	}
	
	/**
	 * Helper Function to get the base2 logarithm
	 */
	private static double log2(int x)
	{
	    return Math.ceil((Math.log(x) / Math.log(2)));
	}
	
	
	/**
	 * ToString Function
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Valid\t| Dirty\t| Set\t| Index\t| LRU\t| Tag\t| DataBlock\n------------------------------------------------------------\n");
		
		for(int i = 0; i < this.size;  i++) {
			sb.append(this.rows[i]);
		}
		sb.append("\n");
		
		return sb.toString();
	}
}
