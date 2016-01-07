import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.util.Pair;


public class ElectionThread implements Runnable {
	private int elu;
	private boolean electionEnCours;
	private int id;
	private int aptitude;
	private List<Pair<InetAddress, Integer>> sites;
	private int next;


	private DatagramSocket envoieSocket;
	private DatagramSocket serverSocket;
	private Object sync;

	private boolean running;

	public ElectionThread(int id, int aptitude, List<Pair<InetAddress, Integer>> sites){
		this.id = id;
		this.aptitude = aptitude;
		this.sites = sites;
		next = (id + 1)% sites.size();
	}

	@Override
	public void run() {
		running = true;
		
		try {
			serverSocket = new DatagramSocket(sites.get(id).getValue());
			envoieSocket = new DatagramSocket();
			envoieSocket.setSoTimeout(5000);
			running = true;
			while(running)
			{
				byte[] receiveData = new byte[300];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				String message = recevoirMessage(serverSocket,receivePacket);
				String[] splitedMessage = message.split(":");
				Message type = Message.valueOf(splitedMessage[0]);
				
				
				switch (type) {
				case ELECTION:
					envoyerQuittance(receivePacket);
					HashMap<Integer, Integer> aptitudes = (HashMap<Integer, Integer>)byteArrayToObject(splitedMessage[1].getBytes());
					
					// si le site a deja participé à l'élection
					if(aptitudes.containsKey(id)){
						//trouver l'elu
						int maxValue = 0;
						int maxKey = 0;
						for(int i : aptitudes.keySet()){
							if(aptitudes.get(i) > maxValue){
								maxValue = aptitudes.get(i);
								maxKey = i;
							}
						}
						elu = maxKey;
						//envoi le résultat de l'election
						List<Integer> ids = new ArrayList<Integer>();
						next = (id + 1) % sites.size();
						envoyerResultat(ids);
						electionEnCours = false;
						//signal à l'application que l'election est finie
						synchronized (sync) {
							sync.notify();
						}
					}else{
						//Pour simplifier on ne garde pas en mémoire qu'un site est en panne.
						next = (id + 1) % sites.size();
						envoyerElection(aptitudes);
					}
					break;
				case RESULTAT:
					envoyerQuittance(receivePacket);
					//récupération de la liste de id ayant reçu le résultat
					List<Integer> ids = (List<Integer>)byteArrayToObject(splitedMessage[1].getBytes());
					
					//transmission des résultats s'ils n'ont pas encore fait le tour
					if(!ids.contains(id)){
						electionEnCours = false;
						elu = Integer.valueOf(splitedMessage[2]);
						//signal à l'application que l'election est finie
						synchronized (sync) {
							sync.notify();
						}
						//Pour simplifier on ne garde pas en mémoire qu'un site est en panne.
						next = (id + 1) % sites.size();
						envoyerResultat(ids);
					}
						
					
					break;
				case BONJOUR:
					// communication en tant qu'élu avec une application
					String reponse = Message.BONJOUR.toString();
					envoyerMessage(reponse, serverSocket, receivePacket.getAddress(), receivePacket.getPort());
					break;
				default:
					break;
				}
				
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		// fermture des sockets à l'arrêt du programme
		serverSocket.close();
		envoieSocket.close();
	}

	/**
	 * @return the elu
	 */
	public Pair<InetAddress, Integer> getElu() {
		return sites.get(elu);
	}

	/**
	 * @return the electionEnCours
	 */
	public boolean isElectionEnCours() {
		return electionEnCours;
	}
	
	

	/**
	 * @param running the running to set
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * Méthode à appeler pour commencer une nouvelle élection
	 */
	public void trouverNouvelElu(){
		electionEnCours = true;
		HashMap<Integer, Integer> aptitudes = new HashMap<Integer, Integer>();
		//Pour simplifier on ne garde pas en mémoire qu'un site est en panne.
		next = (id + 1) % sites.size();
		envoyerElection(aptitudes);
	}

	private byte[] objectToByteArray(Object obj){
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(byteOut);
			out.writeObject(obj);
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return byteOut.toByteArray();
	}
	

	private Object byteArrayToObject(byte[] bytes){
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
	    ObjectInputStream is;
	    Object temp = new HashMap<Integer, Integer>();
		try {
			is = new ObjectInputStream(in);
			temp = is.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    return temp;
	}
	
	private void envoyerMessage(String message, DatagramSocket serverSocket, InetAddress addr, int port){
		byte[] sendData = new byte[1024];
		sendData = message.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, port);
		try {
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void envoyerQuittance(DatagramPacket packet){
		envoyerMessage(Message.QUITTANCE.toString(), envoieSocket, packet.getAddress(), packet.getPort());
	}
	
	private void envoyerElection(HashMap<Integer, Integer> aptitudes){
		aptitudes.put(id, aptitude);
		String message = Message.ELECTION + ":" + objectToByteArray(aptitudes);
		envoyerMessage(message, serverSocket, sites.get(next).getKey(), sites.get(next).getValue());
		if(!attendreQuittance()){
			next = (next + 1) % sites.size();
			envoyerElection(aptitudes);
		}
	}
	
	private void envoyerResultat(List<Integer> ids){
		ids.add(id);
		String reponse = Message.RESULTAT + ":" + objectToByteArray(ids) + ":" + elu;
		envoyerMessage(reponse, envoieSocket, sites.get(next).getKey(), sites.get(next).getValue());
		if(!attendreQuittance()){
			next = (next + 1) % sites.size();
			envoyerResultat(ids);
		}
	}
	
	private boolean attendreQuittance(){
		byte[] receiveData = new byte[300];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		String reponse;
		try {
			reponse = recevoirMessage(envoieSocket, receivePacket);
		} catch (IOException e) {
			return false;
		}
		
		if(reponse != Message.QUITTANCE.toString()){
			return false;
		}
		
		return true;
	}
	
	private String recevoirMessage(DatagramSocket socket, DatagramPacket packet) throws IOException{
		
		socket.receive(packet);
		
		return new String( packet.getData());
	}
	
	


	public void setSync(Object sync){
		this.sync = sync;
	}



}
