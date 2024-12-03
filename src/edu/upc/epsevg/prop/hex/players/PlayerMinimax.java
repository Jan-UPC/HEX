package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.PlayerMove;
import java.awt.Point;

/**
 *
 * @author Equip (...)
 */
public class PlayerMinimax implements IPlayer {

    
    int INFINIT = Integer.MAX_VALUE;    // Número molt gran que representa el infinit

    int MENYS_INFINIT = Integer.MIN_VALUE;    // Número molt petit que representa el menys infinit
    
    private String _name; // Nom del jugador
    private int _colorPlayer; // Color del jugador, necessari per avaluar l'heurística
    private int _profMax; // Número de nivells que explorarà el jugador en l'algorisme Minimax
    private boolean _poda; // Si val true, el jugador realitzarà poda alfa-beta.
    private int _nJugades; // Nombre de jugades (crides a move) que ha realitzat el jugador
    private int _calculsHeuristica; // Nombre de vegades que s'ha calculat l'heurística
    

    public PlayerMinimax(String name, int profunditatMaxima, boolean poda) {
        this._name = name;
        _profMax = profunditatMaxima;
        _poda = poda;
        _nJugades = 0;
        _calculsHeuristica = 0;
    }
    
     /**
     * Decideix el moviment del jugador donat un tauler, que conté informació sobre un color de peça que
     * el color de peça que ha de posar.
     *
     * @param s Tauler i estat actual de joc.
     * @return El moviment que fa el jugador.
     */
    @Override
    public PlayerMove move(HexGameStatus s) {
        _colorPlayer = s.getCurrentPlayerColor();
        Point tiradaFinal = new Point(-1, -1);
        int hActual = MENYS_INFINIT;
        for (int i = 0; i < s.getSize(); ++i) {
            for (int j = 0; j < s.getSize(); ++j) {
                if (s.getPos(i, j) == 0) {
                    HexGameStatus estatAux = new HexGameStatus(s);
                    Point tiradaActual = new Point(i, j);
                    estatAux.placeStone(tiradaActual); // Aquesta funció canvia el color del current player d'estatAux
                    int hMin = MIN();
                    if (hMin > hActual) {
                        hActual = hMin;
                        tiradaFinal = tiradaActual;
                    }
                } 
            }
        }
        return null;
    }
    
    
    private int MIN() {
        return 0;
    }
    
    
    @Override
    public void timeout() {
        // A aquest jugador no li afecta el timeout, sempre explora una certa quantitat de nivells.
    }

    /**
     * Retorna el nom del jugador que s'utlilitza per visualització a la UI.
     *
     * @return Nom del jugador
     */
    @Override
    public String getName() {
        return "Player(" + _name + ")";
    }
}
