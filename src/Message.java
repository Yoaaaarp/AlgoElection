/**
 * Author  : Marc Pellet et David Villa
 * Project : AlgoElection
 * File    : Message.java
 * Date    : 6 janv. 2016
 * 
 * Type énuméré pour définir les types de message
 * 
 * 	ELECTION  : Annonce qu'une election est en cours
 * 	RESULTAT  : Transmisson du résultat de l'élection
 * 	BONJOUR   : Message transmis entre l'elu et l'application
 * 	QUITTANCE : Sert à quittancer la réception d'une election ou d'un resultat,
 * 				utilisé pour détecter les pannes des sites.
 *
 */
public enum Message {
	ELECTION, RESULTAT, BONJOUR, QUITTANCE
}
