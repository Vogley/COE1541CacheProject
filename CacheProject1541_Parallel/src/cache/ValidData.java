package cache;

public class ValidData {
	private boolean valid;
	private CacheRow data;
	
	public ValidData(boolean v, CacheRow d){
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
	public void setData(CacheRow d) {
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
	public CacheRow getData() {
		return this.data;
	}
}
