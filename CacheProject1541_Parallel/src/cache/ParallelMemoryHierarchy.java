package cache;

import java.util.*;
import java.util.stream.IntStream;

public class ParallelMemoryHierarchy {
	private int layers; // # of Layers/Caches in memory hierarchy
	private int[] latencies; // Latencies of each cache layer
	private int policy; // Write/Allocate Policy of hierarchy: 0 for write-back and write-allocate; 1
						// for write-through and non-write-allocate
	private int blocksize; // Number of data locations within block
	private int outstandingMisses; // Number of Misses to be allowed to be in the buffer at one time
	private Cache[] caches; // Array of caches, larger the index, the deeper the cache in the hierarchy
	private int memAccess; // Number of Memory Access performed by the MemoryHierarchy
	private int requestIDs;

	/**
	 * Initialize the Memory Hierarchy
	 * 
	 * @param layers          -> # of Layers/Caches in memory hierarchy
	 * @param policy          -> Write/Allocate Policy of hierarchy
	 * @param blocksize       -> Number of data locations within block
	 * @param sizes           -> Array of sizes of the caches
	 * @param setAssociatives -> Array of # of sets within a cache
	 * @param latencies       -> Array of latencies of the caches
	 */
	public ParallelMemoryHierarchy(int layers, int policy, int blocksize, int outstandingMisses, int[] sizes,
			int[] setAssociatives, int[] latencies) {
		this.layers = layers;
		this.latencies = latencies;
		this.policy = policy;
		this.blocksize = blocksize;
		this.outstandingMisses = outstandingMisses;
		this.caches = new Cache[this.layers];
		this.memAccess = 1;
		this.requestIDs = 0;

		// Initialize each cache
		for (int i = 0; i < this.layers; i++) {
			this.caches[i] = new Cache(sizes[i], setAssociatives[i], latencies[i], blocksize, outstandingMisses);
		}
	}

	/**
	 * Main Cache Cycle Update
	 * 
	 * Structure of Cycle 1. Go through each cache 2. Check for outstanding requests
	 * given to a cache from another process 3. Check for new requests. 4. Decrease
	 * timer on cache if busy. 5. Repeat til all caches are finished and there are
	 * no more requests.
	 * 
	 * @param request
	 * @param time
	 * @return
	 */
	public int cycle(Request request, int time) {
		Request outstandingRequest = null;
		boolean usedRequest = false;
		boolean[] finished = new boolean[this.layers];
		for (int f = 0; f < this.layers; f++)
			finished[f] = false;

		// System.out.println("Current Time: " + time + ", Cache Statuses: " +
		// this.caches[0].getStatus() + " | " + this.caches[1].getStatus() + " | " +
		// this.caches[2].getStatus());

		for (int c = 0; c < this.layers; c++) {
			Cache currCache = this.caches[c];
			// Add new Outstanding Misses
			if (outstandingRequest != null) {
				currCache.addOutstandingRequest(outstandingRequest);
				outstandingRequest = null;
			}

			// For the first cache, try sending new request if open. If it has a pending
			// write, do that first.
			if (currCache.getStatus() == 0 && c == 0
					&& (request != null || currCache.peekOutstandingRequest() != null)) {
				RequestResult result = null;
				// Run New Request if there is one, and if there is no outstandingRequests.
				if (request != null && (currCache.peekOutstandingRequest() == null
						|| currCache.peekOutstandingRequest().getTime() >= time)) {
					// System.out.println("New Request ID: " + request.getID());
					request.setStartTime(time);
					result = sendRequest(request, currCache);
					usedRequest = true;
				}
				// Otherwise, run outstanding requests.
				else if (currCache.peekOutstandingRequest().getTime() <= time) {
					// Run Outstanding Requests, only if the cache hasn't reach the max amount of misses
					if (currCache.getCurrMissesSize() >= this.outstandingMisses) {
						// Check if any outstanding misses have been fulfilled.
						Request oldMiss = currCache.selectOldMiss();
						if (oldMiss != null) {
							result = sendRequest(oldMiss, currCache);
						} else if(!currCache.getNotifyAtMaxMisses()){
							System.out.println("Cache " + c + " at max outstanding misses. Not fulfilling anymore requests til below max misses.");
							currCache.setNotifyAtMaxMisses(true);
						}
					} else {
						result = sendRequest(currCache.getOutstandingRequest(), currCache);
					}

					// Remove request from current misses.
					if (result != null && currCache.containsMiss(result.getRequest())) {
						currCache.removeCurrMiss(result.getRequest());
						currCache.setNotifyAtMaxMisses(false);
					}
					// If outstanding misses is at max, then wait until they are handled.
				}

				// What was the result of the request?
				if (result != null) {
					// Successful Read
					if (result.getResult() == 1) {
						// Update Upper Caches
						// Not needed for this cache
						int readTime = (time - request.getStartTime() + currCache.getLatency());
						System.out.println("Read Access complete in: " + readTime + " cycles. Request: "
								+ request.toString() + "\n");
					}
					// Unsuccessful Read
					else if (result.getResult() == -1) {
						outstandingRequest = result.getRequest();
						outstandingRequest.setTime(time + currCache.getLatency());
						currCache.addCurrMiss(outstandingRequest);
					}
					// Successful Write
					else if (result.getResult() == 2) {
						// Need to Write to Lower Levels
						outstandingRequest = result.getRequest();
						outstandingRequest.setTime(time + currCache.getLatency());
					}
					// Successful Write with Eviction
					else if (result.getResult() == -2) {
						// Row was Evicted, and needs to be sent to memory if lowest cache.
						// If not lowest Cache, do nothing

						// Need to Write to Lower Levels
						outstandingRequest = result.getRequest();
						outstandingRequest.setTime(time + currCache.getLatency());
					}
					// Successful Eviction
					else {
						// Take Evicted Row and return it to a lower cache.
						CacheRow evicted = result.getData();
						if (evicted.getDirty()) {
							int evictAddress = (evicted.getTag() << (int) Math.floor(currCache.getIndexSize() / 2) | evicted.getIndex());
							outstandingRequest = new Request(this.requestIDs++, 2, evictAddress, result.getRequest().getData(), time + currCache.getLatency(), time);
						}
						// Do Nothing if the evicted row is not dirty.
					}
				}
			}

			// If not first cache, check if cache has any jobs in queue waiting for it.
			else if (currCache.getStatus() == 0 && currCache.peekOutstandingRequest() != null) {
				RequestResult result = null;
				// Run Outstanding Requests, only if the cache hasn't reach the max amount of misses
				if (currCache.getCurrMissesSize() >= this.outstandingMisses) {
					// Check if any outstanding misses have been fulfilled.
					Request oldMiss = currCache.selectOldMiss();
					if (oldMiss != null) {
						result = sendRequest(oldMiss, currCache);
					} else if(!currCache.getNotifyAtMaxMisses()){
						System.out.println("Cache " + c + " at max outstanding misses. Not fulfilling anymore requests til below max misses.");
						currCache.setNotifyAtMaxMisses(true);	//Blocks all repeat alerts
					}
				} else {
					result = sendRequest(currCache.getOutstandingRequest(), currCache);
				}
				// Remove request from current misses.
				if (result != null && currCache.containsMiss(result.getRequest())) {
					currCache.removeCurrMiss(result.getRequest());
					currCache.setNotifyAtMaxMisses(false);
				}
				// If outstanding misses is at max, then wait until they are handled.
				// Check the result of the request.
				if (result != null) {
					// Successful Read
					if (result.getResult() == 1) {
						// Update Upper Caches
						int readTime = (time - result.getRequest().getStartTime() + currCache.getLatency()) + currCache.getLatency();
						System.out.println("Read Access complete in: " + readTime + " cycles. Request: " + result.getRequest().toString() + "\n");
						Request update = new Request(result.getRequest().getID(), 2, result.getRequest().getAddress(), result.getData().getBlockData(), time + currCache.getLatency(), time);
						for (int i = 0; i < c; i++) {
							this.caches[i].addOutstandingRequest(update);
						}
					}
					// Unsuccessful Read
					else if (result.getResult() == -1) {
						outstandingRequest = result.getRequest();
						outstandingRequest.setTime(time + currCache.getLatency());
						currCache.addCurrMiss(outstandingRequest);
					}
					// Successful Write
					else if (result.getResult() == 2) {
						// Need to Write to Lower Levels
						if (c != this.caches.length - 1) {
							outstandingRequest = result.getRequest();
							outstandingRequest.setTime(time + currCache.getLatency());
						} else if (policy == 1) {
							// Fake memory Write
							System.out.println("MEMORY WRITE!");
						}

					}
					// Successful Write with Eviction
					else if (result.getResult() == -2) {
						// Need to Write to Lower Levels, don't send outstanding request if last layer.
						if(c != this.layers-1) {
							outstandingRequest = result.getRequest();
							outstandingRequest.setTime(time + currCache.getLatency());
						}

						// Row was Evicted, and needs to be sent to memory if lowest cache.
						// If not lowest Cache, do nothing
						CacheRow evicted = result.getData();
						if (evicted.getDirty() && c == this.layers - 1) {
							// Fake memory Write
							System.out.println("MEMORY WRITE!");
						}
					}
					// Successful Eviction
					else {
						// Take Evicted Row and return it to a lower cache.
						CacheRow evicted = result.getData();
						if (evicted.getDirty() && c < this.layers - 1) {
							int evictAddress = (evicted.getTag() << (int) Math.floor(currCache.getIndexSize() / 2) | evicted.getIndex());
							outstandingRequest = new Request(this.requestIDs++, 2, evictAddress, evicted.getBlockData(), time + currCache.getLatency(), time);
						} else if (evicted.getDirty() && c == this.layers - 1) {
							// Fake memory Write
							System.out.println("MEMORY WRITE!");
						}
						// Do Nothing if the evicted row is not dirty.
					}
				}
			}

			// No requests/outstandingMisses atm.
			else if (currCache.getStatus() == 0) {
				// The cache is waiting for a request.
				finished[c] = true;
			}

			// Current Cache is busy, must let its latency timer tick down
			else {
				// Decrease timer.
				currCache.setStatus(currCache.getStatus() - 1);
			}
		}
		

		// Want to resolve any leftover outstandingMisses from the last cache (AKA memory accesses)
		if (outstandingRequest != null) {
			System.out.println("MEMORY READ ACCESS!");
			// Send timing of read access.
			int readTime = (time - outstandingRequest.getStartTime()) + (IntStream.of(this.latencies).sum() + 100 + this.caches[this.layers - 1].getLatency());
			System.out.println("Read Access complete in: " + (readTime) + " cycles. Request: " + outstandingRequest.toString() + "\n");

			// False Read Data
			int[] data = new int[this.blocksize];
			int block = (outstandingRequest.getAddress() % this.blocksize);
			for (int i = 0; i < this.blocksize; i++) {
				if (i == block)
					data[i] = this.memAccess;
				else
					data[i] = 0;
			}
			this.memAccess++;

			// Send an update request to lowest cache to write in the data from memory.
			Request evictRequest = new Request(outstandingRequest.getID(), 2, outstandingRequest.getAddress(), data, (time + 100 + this.caches[this.caches.length - 1].getLatency()), time);
			for (int i = 0; i < this.layers; i++) {
				this.caches[i].addOutstandingRequest(evictRequest);
			}
		}

		// Boolean check to see if the system is done with requests.
		boolean complete = true;
		for (int f = 0; f < this.layers; f++) {
			if (finished[f] == false)
				complete = false;
		}

		// Return Statements
		if (usedRequest)
			return 0;
		else if (complete)
			return -1;
		else
			return 1;
	}

	/**
	 * Send request to cache. The request type is determined by inner parameters.
	 * The result depends if there was a hit or not in the cache.
	 * 
	 * @param request
	 * @param c
	 * @return
	 */
	private RequestResult sendRequest(Request request, Cache c) {
		// Set cache to busy. Subtract 1 for the current cycle
		c.setStatus(c.getLatency() - 1);

		// Read request
		if (request.getReadWriteEvict() == 0) {
			ValidData cacheResult = c.readDataFromCache(request.getAddress());
			if (cacheResult.getValid()) {
				return new RequestResult(request, cacheResult.getData(), 1);
			}
			// Outstanding Miss to the next cache
			else {
				return new RequestResult(request, null, -1);
			}
		}
		// Write request
		else if (request.getReadWriteEvict() == 1) {
			// Write Back Policy
			if (this.policy == 0) {
				CacheRow evicted = c.writeBackData(request.getAddress(), request.getData());

				if (evicted == null)
					return new RequestResult(request, evicted, 2);
				else
					return new RequestResult(request, evicted, -2);
			}
			// Write Trough Policy
			else {
				c.writeThroughData(request.getAddress(), request.getData());
				return new RequestResult(request, null, 2);
			}
		}
		// Evict Request
		else {
			CacheRow evicted = c.evictRow(request.getAddress(), request.getBlockData());
			return new RequestResult(request, evicted, 0);
		}
	}

	/**
	 * Return the status of the Memory Hierarchy
	 */
	public String getStatus() {
		StringBuilder sb = new StringBuilder();

		// Get Latencies, Hit/Miss Rates, and Cache Print-outs
		int i = 0;
		for (Cache c : this.caches) {
			sb.append("\nCache " + i + " Total Latency: " + c.getTotalLatency());
			sb.append("\nCache " + i + " Hit Rate: " + c.getHitRate());
			sb.append("\nCache " + i + " Miss Rate: " + c.getMissRate());
			sb.append("\n" + c.toString());
			i++;
		}
		return sb.toString();
	}

}