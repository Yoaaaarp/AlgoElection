/**
 * Author  : Marc Pellet et David Villa
 * Project : AlgoElection
 * File    : Message.java
 * Date    : 6 janv. 2016
 * 
 * Dans cette classe, nous g�rons toute l'�lection en anneau avec panne. 
 * Ce thread g�re tous les envoie de message qui y sont li�. En plus de 
 * cela, il peut r�pondre au messages envoy�s par les application s'il
 * est l'�lu.
 * Nous avons impl�ment�s deux sockets: serverSocket et envoieSocket.
 * 
 * ServerSocket:
 * 
 * Ce socket g�re la r�c�ption des message, il est associ� � un port connu
 * par tous les autres sites. Il lit les messages et les interpr�te en 
 * fonction du type de message et de l'�tat actuel.
 * 
 * EnvoieSocket:
 * 
 * Ce socket g�re tous les envois depuis le thread. Il g�re aussi la 
 * r�ception des quittances pour les message. Ceci � pour but de ne 
 * pas bloquer la r�ception de message en attendant une quittance.
 * 
 * Type de message:
 * 
 * ELECTION : ce message est re�u quand une �lection est en cours.
 * il contient une HashMap associant les id des sites � leur aptitude.
 * quand ce message est re�u il y a deux cas possibles:
 * 	Le site apparait deja dans la liste:
 * 		Cela veut dire que le site a deja particip� � l'election et
 * 		que le message a fait le tour des sites. Dans ce cas la, 
 * 		le site avec la capacit� maximale est trouv� et d�fini comme
 * 		site �lu. Apr�s avoir d�termin� l'�lu, un message de type
 * 		RESULTAT contant l'�lu est envoy� au porchain site dans
 * 		l'anneau
 * 	le site n'apparait pas encore dans la liste:
 * 		Dans ce cas, nous ajoutons simplement l'id du site et sa
 * 		capacit� dans la liste et la transmettons au prochain site.
 * 		L'�lection continuera donc son tour de l'anneau.
 * 
 * RESULTAT : ce message est re�u lorsqu'une �lection est termin�e pour
 * communiquer aux autres sites l'�lu. Elle contient l'id de l'�lu ainsi
 * qu'une liste contenant l'id des sites ayant deja re�u le r�sultat. Quand
 * ce message est re�u il y a de nouveau deux cas possibles:
 * 	L'id du site apparait dans la liste:
 * 		Cela veut dire que le message de resultat a fait le tour de l'anneau
 * 		et que l'election est donc bien termin�e. Il n'y a donc rien � faire
 * 	L'id du site n'est pas encore dans la liste:
 * 		Cela signifie qu'on re�oit le r�sultat pour la premi�re fois. Il faut 
 * 		donc r�cup�rer l'id de l'elu et la stocker puis ajouter l'id du site 
 * 		� la liste avant de la transmettre � son voisin.
 * 
 * BONJOUR : Ce message repr�sente la communication entre les applications et
 * l'�lu. Nous y r�pondons par un simple BONJOUR.
 * 
 * QUITTANCE : Ce message est revoy� au site qui nous a transmis un message
 * de type RESULTAT ou ELECTION. Il a pour but de d�tecter une panne du site 
 * � qui le message est transmis. Apr�s avoir envoy� un message, le site attend
 * une QUITTANCE de son voisin. Si apr�s un certains timeout il ne l'a pas re�ue
 * il consid�re que son voisin est en panne et transmet au suivant. 
 * 
 * Pour simplifier l'impl�mentation, nous avons d�cid� de ne pas garder en
 * m�moire le fait que nous ayons d�couvert qu'un site �tait en panne.
 * A chaque envoi de message, nous commen�on par notre voisin direct m�me 
 * s'il �tait en panne la derni�re fois que nous avons essay�.
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

					// si le site a deja particip� � l'�lection
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
						System.out.println("Site " + id + " fin d'�lection, l'�lu est le site " + elu);
						System.out.println("D�but de transmission des r�sultats");
						
						//envoi le r�sultat de l'election
						List<Integer> ids = new ArrayList<Integer>();
						next = (id + 1) % sites.size();
						envoyerResultat(ids);
						electionEnCours = false;
						//signal � l'application que l'election est finie
						synchronized (sync) {
							sync.notify();
						}
					}else{
						//Pour simplifier on ne garde pas en m�moire qu'un site est en panne.
						electionEnCours = true;
						next = (id + 1) % sites.size();
						envoyerElection(aptitudes);
					}
					break;
				case RESULTAT:
					envoyerQuittance(receivePacket);
					//r�cup�ration de la liste de id ayant re�u le r�sultat
					List<Integer> ids = (List<Integer>)byteArrayToObject(splitedMessage[1].getBytes());

					//transmission des r�sultats s'ils n'ont pas encore fait le tour
					if(!ids.contains(id)){
						electionEnCours = false;
						elu = Integer.valueOf(splitedMessage[2]);
						//signal � l'application que l'election est finie
						synchronized (sync) {
							sync.notify();
						}
						//Pour simplifier on ne garde pas en m�moire qu'un site est en panne.
						next = (id + 1) % sites.size();
						envoyerResultat(ids);
					}else{
						System.out.println("Site " + id + ": fin de transmission des r�sultat");
					}


					break;
				case BONJOUR:
					// communication en tant qu'�lu avec une application
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
		// fermture des sockets � l'arr�t du programme
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
	 * M�thode � appeler pour commencer une nouvelle �lection
	 */
	public void trouverNouvelElu(){
		electionEnCours = true;
		HashMap<Integer, Integer> aptitudes = new HashMap<Integer, Integer>();
		System.out.println("Site " + id + ": D�but d'une �lection");
		//Pour simplifier on ne garde pas en m�moire qu'un site est en panne.
		next = (id + 1) % sites.size();
		envoyerElection(aptitudes);
	}

	/**
	 * @param obj Objet � transcrire en byte
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
	 * @param bytes tableau de byte � transcrire
	 * @return	Objet des�rialis�
	 * 
	 * Retourne l'objet transcrit dans le tableau de byte pass� en argument
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
	 * @param message		message � envoyer
	 * @param serverSocket	socket sur lequel envoyer le message
	 * @param addr			addresse d'envoi
	 * @param port			port d'envoi
	 * 
	 * 
	 * Envoie le message � le destination sp�cifi�e dans les arguments
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
	 * @param packet packet contenant les informations sur la personne ayant envoy� le message
	 * 
	 * Envoie un message pour quittancer la r�ception d'un message
	 * 
	 */
	private void envoyerQuittance(DatagramPacket packet){
		envoyerMessage(Message.QUITTANCE.toString(), envoieSocket, packet.getAddress(), packet.getPort());
	}

	/**
	 * @param aptitudes HashMap de sites et de peur capacit�s
	 * 
	 * Envoie un message de type ELECTION � son voisin, attent une quittance. Tant qu'il ne re�oit
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
	 * @param ids liste des sites ayant deja re�u le resultat
	 * 
	 * Envoie un message de type RESULTAT � son voisin, attent une quittance. Tant qu'il ne re�oit
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
	 * @return boolean d�finnissant si la quittance a �t� re�ue avant le timeout
	 * 
	 * Se met en attente d'une quittance, retourne false si la quittance n'as pas
	 * �t� re�ue avant le timeout. 
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
	 * @param packet pakcet dans lequel �crire
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
