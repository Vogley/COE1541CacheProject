package cache;
import java.util.Arrays;

public class CacheRow {
	private boolean valid;	// Valid bit, 0 for uninitialized
	private boolean dirty;	// Dirty bit to notify of an update in write-back policy, 0 for clean, 1 for dirty.
	private int set;		// Set or Way the row is located in.
	private int index;		// The index within the set the row is at.
	private int blocksize;	// The size of each block in the row (2 = 2 data locations)
	private int LRU;		// Least Recently Used, the last used set in a specific index will be replaced on eviction. Only for Set-Associatice.
	private int tag;		// The memory tag associated with the data.
	private int[] data;		
	
    /**
     * Initialize a Blank Row
     */
	public CacheRow(int set, int index, int blocksize){
		this.valid = false;
		this.dirty = false;
		this.set = set;
		this.index = index;
		this.blocksize = blocksize;
		this.LRU = set; 	// When initializing, the LRU is just the set since none have been used yet. 
		this.tag = 0;
		this.data = new int[blocksize];
		for(int i = 0; i < blocksize; i++)
			this.data[i] = 0;
	}
	
	// Setter Functions
	
	/**
     * Set valid bit from row
     */
	public void setValid(boolean v) {
		this.valid = v;
	}
	
    /**
     * Set dirty bit from row
     */
	public void setDirty(boolean d) {
		this.dirty = d;
	}
	
    /**
     * Set LRU from row
     */
	public void setLRU(int lru) {
		this.LRU = lru;
	}
	
    /**
     * Set tag from row
     */
	public void setTag(int t) {
		this.tag = t;
	}
	
    /**
     * Set specific block of data from row
     */
	public void setData(int d, int block) {
		this.data[block] = d;
	}
	
    /**
     * Set entire block of data from row
     */
	public void setBlockData(int[] d) {
		this.data = d;
	}
	
	// Getter Functions
	
    /**
     * Get valid bit from row
     */
	public boolean getValid() {
		return this.valid;
	}
	
    /**
     * Get dirty bit from row
     */
	public boolean getDirty() {
		return this.dirty;
	}
	
    /**
     * Get Set from row
     */
	public int getSet() {
		return this.set;
	}
	
    /**
     * Get Index from row
     */
	public int getIndex() {
		return this.index;
	}
	
    /**
     * Get LRU from row
     */
	public int getLRU() {
		return this.LRU;
	}
	
    /**
     * Get tag from row
     */
	public int getTag() {
		return this.tag;
	}
	
    /**
     * Get specific block of data from row
     */
	public int getData(int block) {
		return this.data[block];
	}
	
    /**
     * Get entire block of data from row
     */
	public int[] getBlockData() {
		return this.data;
	}
	
	
    /**
     * ToString Function
     */
	public String toString() {
		StringBuilder dataArray = new StringBuilder();
		for(int d: this.data) {
			dataArray.append('[');
			dataArray.append(d);
			dataArray.append("]\t");
		}
		return String.format("%b\t| %b\t| %d \t| %d \t| %d \t| %d \t| %s \t\n", this.valid, this.dirty, this.set, this.index, this.LRU, this.tag, dataArray);
	}
}
