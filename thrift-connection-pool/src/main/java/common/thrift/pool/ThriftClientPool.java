package common.thrift.pool;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThriftClientPool<T extends TServiceClient> implements AutoCloseable {
	static final Logger LOGGER = LoggerFactory.getLogger(ThriftClientPool.class);
	
	private final GenericObjectPool<T> internalPool;

	public ThriftClientPool(ClientFactory<T> clientFactory, GenericObjectPool.Config poolConfig, String host, int port) {
		this(clientFactory, new BinaryOverSocketProtocolFactory(host, port), poolConfig);
	}

	public ThriftClientPool(ClientFactory<T> clientFactory, ProtocolFactory protocolFactory, GenericObjectPool.Config poolConfig) {
		this.internalPool = new GenericObjectPool<T>(new ThriftClientFactory(clientFactory, protocolFactory), poolConfig);
	}

	class ThriftClientFactory extends BasePoolableObjectFactory<T> {
		private ClientFactory<T> clientFactory;	
		private ProtocolFactory protocolFactory;

		public ThriftClientFactory(ClientFactory<T> clientFactory, ProtocolFactory protocolFactory) {
			this.clientFactory = clientFactory;
			this.protocolFactory = protocolFactory;
		}

		@Override
		public T makeObject() throws Exception {
			try {
				TProtocol protocol = protocolFactory.make();
				return clientFactory.make(protocol);
			} catch (Exception e) {
				LOGGER.warn("whut?", e);
				throw new ThriftClientException("Can not make a new object for pool", e);
			}
		}
		
		@Override
		public void destroyObject(T obj) throws Exception {
			if (obj.getOutputProtocol().getTransport().isOpen()) {
				obj.getOutputProtocol().getTransport().close();
			}
			if (obj.getInputProtocol().getTransport().isOpen()) {
				obj.getInputProtocol().getTransport().close();
			}
		}
	}

	public T getResource() {
		try {
			return internalPool.borrowObject();
		} catch (Exception e) {
			throw new ThriftClientException("Could not get a resource from the pool", e);
		}
	}

	public void returnResourceObject(T resource) {
		try {
			internalPool.returnObject(resource);
		} catch (Exception e) {
			throw new ThriftClientException("Could not return the resource to the pool", e);
		}
	}

	public void returnBrokenResource(T resource) {
		returnBrokenResourceObject(resource);
	}

	public void returnResource(T resource) {
		returnResourceObject(resource);
	}

	protected void returnBrokenResourceObject(T resource) {
		try {
			internalPool.invalidateObject(resource);
		} catch (Exception e) { 
			throw new ThriftClientException("Could not return the resource to the pool", e);
		}
	}

	public void destroy() {
		close();
	}

	public void close() {
		try {
			internalPool.close();
		} catch (Exception e) {
			throw new ThriftClientException("Could not destroy the pool", e);
		}
	}
}