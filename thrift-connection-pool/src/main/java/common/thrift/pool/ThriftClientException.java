package common.thrift.pool;

public class ThriftClientException extends RuntimeException {
	
	private static final long serialVersionUID = -2275296727467192665L;
	public ThriftClientException(String message, Exception e) {
		super(message, e);
	}
}