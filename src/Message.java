/**
 * Author  : Marc Pellet et David Villa
 * Project : AlgoElection
 * File    : Message.java
 * Date    : 6 janv. 2016
 * 
 * Type �num�r� pour d�finir les types de message
 * 
 * 	ELECTION  : Annonce qu'une election est en cours
 * 	RESULTAT  : Transmisson du r�sultat de l'�lection
 * 	BONJOUR   : Message transmis entre l'elu et l'application
 * 	QUITTANCE : Sert � quittancer la r�ception d'une election ou d'un resultat,
 * 				utilis� pour d�tecter les pannes des sites.
 *
 */
public enum Message {
	ELECTION, RESULTAT, BONJOUR, QUITTANCE
}
