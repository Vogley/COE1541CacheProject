package cache;

public class RequestResult {
	private Request request;
	private CacheRow data;
	private int result;

	public RequestResult(Request request, CacheRow data, int result) {
		this.request = request;
		this.data = data;
		this.result = result;
	}

	public Request getRequest() {
		return this.request;
	}

	public CacheRow getData() {
		return this.data;
	}

	public int getResult() {
		return this.result;
	}

}
