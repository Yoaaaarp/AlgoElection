/**
 * Author  : Marc Pellet et David Villa
 * Project : AlgoElection
 * File    : Message.java
 * Date    : 6 janv. 2016
 * 
 * Dans cette classe, nous gérons toute l'élection en anneau avec panne. 
 * Ce thread gère tous les envoie de message qui y sont lié. En plus de 
 * cela, il peut répondre au messages envoyés par les application s'il
 * est l'élu.
 * Nous avons implémentés deux sockets: serverSocket et envoieSocket.
 * 
 * ServerSocket:
 * 
 * Ce socket gère la récéption des message, il est associé à un port connu
 * par tous les autres sites. Il lit les messages et les interprète en 
 * fonction du type de message et de l'état actuel.
 * 
 * EnvoieSocket:
 * 
 * Ce socket gère tous les envois depuis le thread. Il gère aussi la 
 * réception des quittances pour les message. Ceci à pour but de ne 
 * pas bloquer la réception de message en attendant une quittance.
 * 
 * Type de message:
 * 
 * ELECTION : ce message est reçu quand une élection est en cours.
 * il contient une HashMap associant les id des sites à leur aptitude.
 * quand ce message est reçu il y a deux cas possibles:
 * 	Le site apparait deja dans la liste:
 * 		Cela veut dire que le site a deja participé à l'election et
 * 		que le message a fait le tour des sites. Dans ce cas la, 
 * 		le site avec la capacité maximale est trouvé et défini comme
 * 		site élu. Après avoir déterminé l'élu, un message de type
 * 		RESULTAT contant l'élu est envoyé au porchain site dans
 * 		l'anneau
 * 	le site n'apparait pas encore dans la liste:
 * 		Dans ce cas, nous ajoutons simplement l'id du site et sa
 * 		capacité dans la liste et la transmettons au prochain site.
 * 		L'élection continuera donc son tour de l'anneau.
 * 
 * RESULTAT : ce message est reçu lorsqu'une élection est terminée pour
 * communiquer aux autres sites l'élu. Elle contient l'id de l'élu ainsi
 * qu'une liste contenant l'id des sites ayant deja reçu le résultat. Quand
 * ce message est reçu il y a de nouveau deux cas possibles:
 * 	L'id du site apparait dans la liste:
 * 		Cela veut dire que le message de resultat a fait le tour de l'anneau
 * 		et que l'election est donc bien terminée. Il n'y a donc rien à faire
 * 	L'id du site n'est pas encore dans la liste:
 * 		Cela signifie qu'on reçoit le résultat pour la première fois. Il faut 
 * 		donc récupérer l'id de l'elu et la stocker puis ajouter l'id du site 
 * 		à la liste avant de la transmettre à son voisin.
 * 
 * BONJOUR : Ce message représente la communication entre les applications et
 * l'élu. Nous y répondons par un simple BONJOUR.
 * 
 * QUITTANCE : Ce message est revoyé au site qui nous a transmis un message
 * de type RESULTAT ou ELECTION. Il a pour but de détecter une panne du site 
 * à qui le message est transmis. Après avoir envoyé un message, le site attend
 * une QUITTANCE de son voisin. Si après un certains timeout il ne l'a pas reçue
 * il considère que son voisin est en panne et transmet au suivant. 
 * 
 * Pour simplifier l'implémentation, nous avons décidé de ne pas garder en
 * mémoire le fait que nous ayons découvert qu'un site était en panne.
 * A chaque envoi de message, nous commençon par notre voisin direct même 
 * s'il était en panne la dernière fois que nous avons essayé.
 * 
 * 
 * 
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
		elu = -1;
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
						System.out.println("Site " + id + " fin d'élection, l'élu est le site " + elu);
						System.out.println("Début de transmission des résultats");
						
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
						electionEnCours = true;
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
					}else{
						System.out.println("Site " + id + ": fin de transmission des résultat");
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
	 * Retourne l'addresse est le port de l'elu s'il est connu
	 * ou null sinon
	 */
	public Pair<InetAddress, Integer> getElu() {
		if(elu == -1){
			return null;
		}else{
			return sites.get(elu);
		}
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
		System.out.println("Site " + id + ": Début d'une élection");
		//Pour simplifier on ne garde pas en mémoire qu'un site est en panne.
		next = (id + 1) % sites.size();
		envoyerElection(aptitudes);
	}

	/**
	 * @param obj Objet à transcrire en byte
	 * @return	tableau de byte correspondant
	 * 
	 * Transcrit un objet en tableau de byte
	 * 
	 */
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

	/**
	 * @param bytes tableau de byte à transcrire
	 * @return	Objet desérialisé
	 * 
	 * Retourne l'objet transcrit dans le tableau de byte passé en argument
	 * 
	 */
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

	/**
	 * @param message		message à envoyer
	 * @param serverSocket	socket sur lequel envoyer le message
	 * @param addr			addresse d'envoi
	 * @param port			port d'envoi
	 * 
	 * 
	 * Envoie le message à le destination spécifiée dans les arguments
	 * 
	 */
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

	/**
	 * 
	 * @param packet packet contenant les informations sur la personne ayant envoyé le message
	 * 
	 * Envoie un message pour quittancer la réception d'un message
	 * 
	 */
	private void envoyerQuittance(DatagramPacket packet){
		envoyerMessage(Message.QUITTANCE.toString(), envoieSocket, packet.getAddress(), packet.getPort());
	}

	/**
	 * @param aptitudes HashMap de sites et de peur capacités
	 * 
	 * Envoie un message de type ELECTION à son voisin, attent une quittance. Tant qu'il ne reçoit
	 * pas de quittance dans les temps, il envoie au site suivant dans la liste. 
	 */
	private void envoyerElection(HashMap<Integer, Integer> aptitudes){
		aptitudes.put(id, aptitude);
		String message = Message.ELECTION + ":" + objectToByteArray(aptitudes);
		envoyerMessage(message, serverSocket, sites.get(next).getKey(), sites.get(next).getValue());
		if(!attendreQuittance()){
			next = (next + 1) % sites.size();
			envoyerElection(aptitudes);
		}
	}

	/**
	 * @param ids liste des sites ayant deja reçu le resultat
	 * 
	 * Envoie un message de type RESULTAT à son voisin, attent une quittance. Tant qu'il ne reçoit
	 * pas de quittance dans les temps, il envoie au site suivant dans la liste.
	 * 
	 */
	private void envoyerResultat(List<Integer> ids){
		ids.add(id);
		String reponse = Message.RESULTAT + ":" + objectToByteArray(ids) + ":" + elu;
		envoyerMessage(reponse, envoieSocket, sites.get(next).getKey(), sites.get(next).getValue());
		if(!attendreQuittance()){
			next = (next + 1) % sites.size();
			envoyerResultat(ids);
		}
	}

	/** 
	 * @return boolean définnissant si la quittance a été reçue avant le timeout
	 * 
	 * Se met en attente d'une quittance, retourne false si la quittance n'as pas
	 * été reçue avant le timeout. 
	 * 
	 */
	private boolean attendreQuittance(){
		byte[] receiveData = new byte[300];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		String reponse;
		try {
			reponse = recevoirMessage(envoieSocket, receivePacket);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param socket socket de reception
	 * @param packet pakcet dans lequel écrire
	 * @return le message lu
	 * @throws IOException
	 * 
	 * Attend un message dans le socket et le transcrit en string
	 */
	private String recevoirMessage(DatagramSocket socket, DatagramPacket packet) throws IOException{

		socket.receive(packet);

		return new String( packet.getData());
	}




	public void setSync(Object sync){
		this.sync = sync;
	}



}
