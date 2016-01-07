import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;

import com.sun.glass.ui.View.Capability;

import javafx.util.Pair;


public class ElectionThread implements Runnable {
	private int elu;
	private boolean electionEnCours;
	private int id;
	private int aptitude;
	private List<Pair<InetAddress, Integer>> sites;
	private int next;
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
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];
			while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String message = new String( receivePacket.getData());
				String[] splitedMessage = message.split(":");
				Message type = Message.valueOf(splitedMessage[0]);
				
				switch (type) {
				case ELECTION:
					HashMap<Integer, Integer> aptitudes = byteArrayToHashMap(splitedMessage[1].getBytes());
					if(aptitudes.containsKey(id)){
						int maxValue = 0;
						int maxKey = 0;
						for(int i : aptitudes.keySet()){
							if(aptitudes.get(i) > maxValue){
								maxValue = aptitudes.get(i);
								maxKey = i;
							}
						}
						elu = maxKey;
						String reponse = Message.RESULTAT + ":" + id + ":" + elu;
						envoyerMessage(reponse, serverSocket, sites.get(next).getKey(), sites.get(next).getValue());
						electionEnCours = false;
						//TODO signal fin election
					}else{
						aptitudes.put(id, aptitude);
						String reponse = splitedMessage[0] + ":" + hashMapToByteArray(aptitudes);
						envoyerMessage(reponse, serverSocket, sites.get(next).getKey(), sites.get(next).getValue());
					}
					break;
				case RESULTAT:
					
					break;
				case BONJOUR:
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

	public void trouverNouvelElu(){
		electionEnCours = true;
		HashMap<Integer, Integer> aptitudes = new HashMap<Integer, Integer>();
		aptitudes.put(id, aptitude);
		String message = Message.ELECTION + ":" + hashMapToByteArray(aptitudes);
		envoyerMessage(message, serverSocket, sites.get(next).getKey(), sites.get(next).getValue());
	}

	private byte[] hashMapToByteArray(HashMap<Integer, Integer> map){
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(byteOut);
			out.writeObject(map);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return byteOut.toByteArray();
	}
	

	private HashMap<Integer, Integer> byteArrayToHashMap(byte[] bytes){
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
	    ObjectInputStream is;
	    HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
		try {
			is = new ObjectInputStream(in);
			temp = (HashMap<Integer, Integer>)is.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    return temp;
	}
	
	private void envoyerMessage(String message, DatagramSocket serverSocket, InetAddress addr, int port){
		byte[] sendData = new byte[1024];
		sendData = message.getBytes();
		DatagramPacket sendPacket =
		new DatagramPacket(sendData, sendData.length, addr, port);
		try {
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setSync(Object sync){
		this.sync = sync;
	}

	public void setRunning(boolean running){
		this.running = running;
	}
}
