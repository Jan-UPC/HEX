package edu.upc.epsevg.prop.hex;

import edu.upc.epsevg.prop.hex.players.HumanPlayer;
import edu.upc.epsevg.prop.hex.players.RandomPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.players.H_E_X_Player;
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
                final int midaTauler = 5;
                
                IPlayer player2 = new H_E_X_Player(1/*GB*/);
                //IPlayer player2 = new RandomPlayer("Random");
                IPlayer player1 = new PlayerMinimaxHexCalculators("MiniMax", 5, true);
                // IPlayer player2 = new PlayerMinimax("HexCalculators", 4, false);
                new Board(player1 , player2, midaTauler /*mida*/,  10/*s*/, false);
             }
        });
    }
}
