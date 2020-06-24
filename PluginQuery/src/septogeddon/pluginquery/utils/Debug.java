package septogeddon.pluginquery.utils;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import septogeddon.pluginquery.PluginQuery;
import septogeddon.pluginquery.api.QueryConnection;
import septogeddon.pluginquery.api.QueryContext;
import septogeddon.pluginquery.api.QueryFuture;
import septogeddon.pluginquery.api.QueryMessenger;
import septogeddon.pluginquery.channel.QueryDecryptor;
import septogeddon.pluginquery.channel.QueryDeflater;
import septogeddon.pluginquery.channel.QueryEncryptor;
import septogeddon.pluginquery.channel.QueryInflater;

public class Debug {

	public static boolean ENABLED = true;
	public static void debug(Object obj) {
		System.out.println("DEBUG: "+obj);
	}
	public static void main(String[]args) {
		System.out.println(Byte.MAX_VALUE);
	}
	public static void main2(String[]args) throws Throwable {
		PluginQuery.initializeDefaultMessenger();
		QueryMessenger messenger = PluginQuery.getMessenger();
//		QueryConnection connection = messenger.newConnection(new InetSocketAddress("131.153.48.90", 25619));
		QueryConnection connection = messenger.newConnection(new InetSocketAddress("localhost", 25565));
		EncryptionToolkit toolkit = new EncryptionToolkit(EncryptionToolkit.readKey(new File("D:\\TestServer\\plugins\\PluginQuery\\secret.key")));
		messenger.getPipeline().addLast(
				new QueryDecryptor(toolkit.getDecryptor()),
				new QueryDeflater(),
				new QueryInflater(),
				new QueryEncryptor(toolkit.getEncryptor())
				);
		QueryFuture<QueryConnection> future = connection.connect();
		future.addListener(fut->{
			if (fut.isSuccess()) {
				debug("Connected to localhost:25569");
			} else {
				debug("Failed to connect");
				fut.getCause().printStackTrace();
			}
		});
		connection.getEventBus().registerListener(conn->{
			if (conn.isConnected()) {
				System.out.println("Connected!");
			} else {
				System.out.println("Disconnected!");
			}
		});
		AtomicInteger count = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();
		connection.getEventBus().registerListener((conn, channel, message)->{
			DataBuffer buf = new DataBuffer(message);
			String command = buf.readUTF();
			if (QueryContext.COMMAND_VERSION_CHECK.equals(command)) {
				String version = buf.readUTF();
				String source = buf.readUTF();
				debug(source+": "+version+" ("+count.getAndIncrement()+"/"+failCount.get()+")");
			}
		});
		for (int i = 0; i < 5; i++) {
			DataBuffer buffer = new DataBuffer();
			buffer.writeUTF(QueryContext.COMMAND_VERSION_CHECK);
			buffer.writeUTF("Standalone non-bungee");
			buffer.writeUTF("testServer");
			QueryFuture<QueryConnection> bytes = connection.sendQuery(QueryContext.PLUGIN_MESSAGING_CHANNEL, buffer.toByteArray());
			bytes.addListener(futured->{
				if (!futured.isSuccess()) {
					futured.getCause().printStackTrace();
					failCount.getAndIncrement();
				}
			});
		}
	}
	
}
