package common.thrift.pool;

import org.apache.thrift.protocol.TProtocol;

public interface ProtocolFactory {
	TProtocol make();
}