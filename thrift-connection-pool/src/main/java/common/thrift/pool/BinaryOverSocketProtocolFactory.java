package common.thrift.pool;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class BinaryOverSocketProtocolFactory implements ProtocolFactory {
	private String host;
	private int port;
	
	public BinaryOverSocketProtocolFactory(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public TProtocol make() {
		TTransport transport = new TSocket(host, port);
		try {
			transport.open();
		} catch (TTransportException e) {
			ThriftClientPool.LOGGER.warn("whut?", e);
			throw new ThriftClientException("Can not make protocol", e);
		}
		return new TBinaryProtocol(transport);
	}
}