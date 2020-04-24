package cache;

public class ValidData {
	private boolean valid;
	private int[] data;
	
	public ValidData(boolean v, int[] d){
		this.valid = v;
		this.data = d;
	}
	
    /**
     * Set valid
     */
	public void setValid(boolean v) {
		this.valid = v;
	}
	
    /**
     * Set data
     */
	public void setData(int[] d) {
		this.data = d;
	}
	
    /**
     * Get valid
     */
	public boolean getValid() {
		return this.valid;
	}
	
    /**
     * Get data
     */
	public int[] getData() {
		return this.data;
	}
}
