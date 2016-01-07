import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import javafx.util.Pair;


public class Site extends Thread {
	private ApplicationThread app;
	private Thread threadApp;
	private ElectionThread election;
	private Thread threadElection;
	private int id;
	private int port;
	private InetAddress addr;
	private HashMap<Integer, Pair<InetAddress, Integer>> sites;
	private int aptitude;
	
	public Site(int id, int port, InetAddress addr){
		this.id = id;
		this.port = port;
		this.addr = addr;
		aptitude = 0;
		for(byte b : addr.getAddress()){
			aptitude *= 10;
			aptitude += (int)b;
		}
		aptitude += port;
	}
	
	public void init(HashMap<Integer, Pair<InetAddress, Integer>> sites){
		// TODO
		this.sites = sites;
		
	}
	
	public int getAptitude() throws UnknownHostException {
		int aptitude = addr.getAddress()[3] + port;
		return aptitude;
	}

	@Override
	public void run() {
		// TODO faire un truc qui correspond à la donnée...
		
		election = new ElectionThread(id, aptitude);
		app = new ApplicationThread(election);
		threadApp = new Thread(app);
		threadApp.start();
		threadElection = new Thread(election);
		threadElection.start();
	}
	
	public void panne() {
		// on se permet d'utiliser cette methode déprécié car c'est le propre d'une panne
		// que de ne pas se fermer de manière sécuritaire
		threadApp.stop();
		threadElection.stop();
	}

	public void safeStop() {
		app.setRunning(false);
	}
}
