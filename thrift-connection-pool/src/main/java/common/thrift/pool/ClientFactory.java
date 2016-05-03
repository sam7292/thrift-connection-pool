package common.thrift.pool;

import org.apache.thrift.protocol.TProtocol;

public interface ClientFactory<T> {	
	T make(TProtocol tProtocol);
}