package base;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.Scribe;
import com.facebook.nifty.test.Scribe.Client;

import common.thrift.pool.ClientFactory;
import common.thrift.pool.ThriftClientPool;

public class BaseNativeClient {
	private static final Logger log = LoggerFactory.getLogger(BaseNativeClient.class);
	public static void main(String[] args) throws Exception {
		
		Config poolConfig = new Config();
		poolConfig.maxActive = 10;
		poolConfig.minIdle = 5;
		poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
		poolConfig.testOnBorrow = true;
		poolConfig.testWhileIdle = true;
		poolConfig.numTestsPerEvictionRun = 10;
		poolConfig.maxWait = 3000;

		final ThriftClientPool<Scribe.Client> pool = new ThriftClientPool<Scribe.Client>(new ClientFactory<Scribe.Client>() {
			
			@Override
			public Client make(TProtocol tProtocol) {
				return new Scribe.Client(tProtocol);
			}
		}, poolConfig, "localhost", 8888);

		ExecutorService executor = Executors.newFixedThreadPool(10);

		for (int i = 0; i < 10; i++) {			
			executor.submit(new Runnable() {
				public void run() {

					Scribe.Client resource = pool.getResource();
					try {
						for (int i = 0; i < 10000; i++) {
							try {
								resource.Log(Collections.singletonList(new LogEntry("cat1","test" + i)));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						pool.returnResource(resource);
					} catch (Exception e) {
						pool.returnBrokenResource(resource);
						log.warn("whut?", e);
					}
				}
			});
		}
		
		Thread.sleep(3000);
		pool.close();
	}
}
