import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.util.Pair;


public class Environnement {
	private final int NB_MAX_SITE = 4; // hypothèse 1
	private HashMap<Integer, Pair<InetAddress, Integer>> sites; // tableau liant un site (InetAddress/port) à un ID
	private int idSite;
	private int port;
	private List<Site> vm; // chaque site est un thread permettant ainsi de simuler une panne/reprise
	
	public Environnement(){
		idSite = 0;
		port = 5335;
	};
	
	public int init(int nbSite, String[] addressNames) throws UnknownHostException {
		// on s'assure que l'on ne crée pas plus de 4 sites
		if (nbSite > NB_MAX_SITE){
			return -1;
		} // on s'assure que l'on fournit un nombre de nom d'InetAddress suffisant
		else if (addressNames.length != nbSite){
			return -1;
		}
		
		Site courant;
		InetAddress addr;
		List<Site> liste = new ArrayList<Site>();
		sites = new HashMap<Integer, Pair<InetAddress, Integer>>();
		// creation des sites ainsi qu'une entré correspondante dans la hashmap
		for (int i = 0; i < nbSite; i++){
			addr = InetAddress.getByName(addressNames[i]);
			sites.put(new Integer(idSite), new Pair<InetAddress, Integer>(addr, new Integer(port)));
			courant = new Site(idSite, port, addr);
			liste.add(courant);
			idSite++;
			port++;
		}
		
		vm = new ArrayList<Site>();
		Thread t;
		// initialisation des sites avec la hashmap complete et demarrage de ces derniers (simulation VM)
		for (Site s : liste){
			s.init(sites);
			s.start();
			vm.add(s);
		}
		return 0;
	}
	
	public void simulerPanne(int siteID){
		// on kill les threads internes
		vm.get(siteID).panne();
	}
	
	public void reprisePanne(int siteID){
		// on restart le thread
		vm.get(siteID).start();
	}
	
	public void arretTotal(){
		for(Site s: vm){
			// on kill les threads internes
			s.safeStop();
			// puis on kill le thread principale
			s.stop();
		}
	}
}
