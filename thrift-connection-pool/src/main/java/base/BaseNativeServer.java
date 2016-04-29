package base;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.facebook.nifty.test.LogEntry;
import com.facebook.nifty.test.ResultCode;
import com.facebook.nifty.test.Scribe;

public class BaseNativeServer {
	private static final Logger log = LoggerFactory.getLogger(BaseNativeServer.class);

	public static void main(String[] args) throws Exception {
		final MetricRegistry registry = new MetricRegistry();
		final Meter requests = registry.meter(MetricRegistry.name(BaseNativeSelectorServer.class, "requests"));
		final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(1, TimeUnit.SECONDS);

	   // Create the handler
		Scribe.Iface serviceInterface = new Scribe.Iface() {
			public ResultCode log(List<LogEntry> messages) throws TException {
				requests.mark();
				for (LogEntry message : messages) {
					log.info("{}: {}", message.getCategory(),message.getMessage());
				}
				return ResultCode.OK;
			}
		};
		
		TServerSocket serverTransport = new TServerSocket(7911);
		Scribe.Processor<Scribe.Iface> processor = new Scribe.Processor<Scribe.Iface>(serviceInterface);
		
		final TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
		server.serve();

		//Arrange to stop the server at shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.stop();
			}
		});
	}
}
