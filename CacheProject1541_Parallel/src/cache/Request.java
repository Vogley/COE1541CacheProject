package cache;

public class Request {
	private int id;
	private int readWriteEvict;
	private int address;
	private int data;
	private int[] block;
	private int time;
	private int startTime;

	public Request(int id, int readWriteEvict, int address, int data, int time, int startTime) {
		this.id = id;
		this.readWriteEvict = readWriteEvict;
		this.address = address;
		this.data = data;
		this.time = time;
		this.startTime = startTime;
	}

	public Request(int id, int readWriteEvict, int evictAddress, int[] blockData, int time, int startTime) {
		this.id = id;
		this.readWriteEvict = readWriteEvict;
		this.address = evictAddress;
		this.block = blockData;
		this.time = time;
		this.startTime = startTime;
	}

	public int getID() {
		return this.id;
	}

	public int getReadWriteEvict() {
		return this.readWriteEvict; // 0 for Read, 1 for Write, 2 for Evict
	}

	public int getAddress() {
		return this.address;
	}

	public int getData() {
		return this.data;
	}

	public int[] getBlockData() {
		return this.block;
	}

	public int getTime() {
		return this.time;
	}

	public void setTime(int t) {
		this.time = t;
	}

	public int getStartTime() {
		return this.startTime;
	}

	public void setStartTime(int t) {
		this.startTime = t;
	}

	public String toString() {
		return String.format("ID: %d\t| Read(0)/Write(1)/Evict(2) %d | Address: %d\t| Data: %d\t| Time: %d |", id, readWriteEvict, address,
				data, time);
	}

}
