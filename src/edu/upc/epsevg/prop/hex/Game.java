package edu.upc.epsevg.prop.hex;

import edu.upc.epsevg.prop.hex.players.HumanPlayer;
import edu.upc.epsevg.prop.hex.players.RandomPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.players.H_E_X_Player;
import edu.upc.epsevg.prop.hex.players.PlayerIDHexCalculators;
import edu.upc.epsevg.prop.hex.players.PlayerMinimaxHexCalculators;



import javax.swing.SwingUtilities;

/**
 * Checkers: el joc de taula.
 * @author bernat
 */
public class Game {
        /**
     * @param args
     */
    public static void main(String[] args) { 
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int midaTauler = 9;
                final int seg = 20;
                IPlayer player2 = new H_E_X_Player(2/*GB*/);
                IPlayer player1 = new PlayerMinimaxHexCalculators("MiniMaxHexCalculator", 6, midaTauler);
                //IPlayer player1 = new PlayerIDHexCalculators("IDS", midaTauler, seg);
                new Board(player1 , player2, midaTauler /*mida*/,  seg/*s*/, false);
             }
        });
    }
}
