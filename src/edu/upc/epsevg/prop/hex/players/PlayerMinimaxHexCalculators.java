package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.SearchType;
import java.awt.Point;
import java.util.PriorityQueue;

/**
 *
 * @author Equip Hex
 */
public class PlayerMinimaxHexCalculators implements IPlayer {

    
    int INFINIT = Integer.MAX_VALUE;    // Número molt gran que representa el infinit

    int MENYS_INFINIT = Integer.MIN_VALUE;    // Número molt petit que representa el menys infinit
    
    private String _name; // Nom del jugador
    private int _colorPlayer; // Color del jugador, necessari per avaluar l'heurística
    private int _profMax; // Número de nivells que explorarà el jugador en l'algorisme Minimax
    private boolean _poda; // Si val true, el jugador realitzarà poda alfa-beta.
    private int _nJugades; // Nombre de jugades (crides a move) que ha realitzat el jugador
    private long _calculsHeuristica; // Nombre de vegades que s'ha calculat l'heurística
    private int _profExplorada; // Nivell màxim de profunditat on s'ha arribat
    

    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, boolean poda) {
        this._name = name;
        _profMax = profunditatMaxima;
        _poda = poda;
        _nJugades = 0;
        _calculsHeuristica = 0;
        _profExplorada = 0;
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
        int alfa = MENYS_INFINIT;
        int beta = INFINIT;
        Point tiradaFinal = new Point(-1, -1);
        int hActual = MENYS_INFINIT;
        int profExpl = 0;
        
        for (int i = 0; i < s.getSize(); ++i) {
            // Bucle de les columnes
            for (int j = 0; j < s.getSize(); ++j) {
                // Bucle de les files
                if (s.getPos(i, j) == 0) {
                    HexGameStatus estatAux = new HexGameStatus(s);
                    Point tiradaActual = new Point(i, j);
                    estatAux.placeStone(tiradaActual); // Aquesta funció canvia el color del current player d'estatAux
                    int hMin = MIN(estatAux, _profMax-1, profExpl+1, i, j, alfa, beta);
                    if (hMin > hActual) {
                        hActual = hMin;
                        tiradaFinal = tiradaActual;
                    }
                }
            }
        }
        
        PlayerMove jugadaFinal = new PlayerMove(tiradaFinal, _calculsHeuristica, _profExplorada, SearchType.MINIMAX);
        
        return jugadaFinal;
    }
    
    
    private int MIN(HexGameStatus estat, int profunditat, int nivellsExplorats, int i, int j, int alpha, int beta) {
        int millor_valor = INFINIT;
        // int colorActual = estat.getCurrentPlayerColor();

        if (estat.isGameOver()) return millor_valor;
        else if (profunditat == 0) {
            return heuristica(estat, _colorPlayer, nivellsExplorats);
        } else {
            for (int ii = 0; ii < estat.getSize(); ii++) {
                // Bucle de les columnes
                for (int jj = 0; jj < estat.getSize(); jj++) {
                    // Bucle de les files
                    if (estat.getPos(i, j)==0) {
                        HexGameStatus estatAux = new HexGameStatus(estat);
                        Point tirada = new Point(ii, jj);
                        estatAux.placeStone(tirada);
                        int h_actual = MAX(estatAux, profunditat-1, nivellsExplorats+1, ii, jj, alpha, beta);
                        millor_valor = Math.min(millor_valor, h_actual);
                        if (_poda) {
                            beta = Math.min(millor_valor, beta);
                            if (beta <= alpha) { // Fem poda
                                break;
                            }
                        }
                    }
                }
                if (_poda) {
                    if (beta <= alpha) { // Fem poda
                        break;
                    }
                }
            }
        }
    return millor_valor;
    }
    
    private int MAX(HexGameStatus estat, int profunditat, int nivellsExplorats, int i, int j, int alpha, int beta) {
        int millor_valor = MENYS_INFINIT;
        // int colorActual = estat.getCurrentPlayerColor();

        if (estat.isGameOver()) return millor_valor;
        else if (profunditat == 0) {
            return heuristica(estat, _colorPlayer, nivellsExplorats);
        } else {
            for (int ii = 0; ii < estat.getSize(); ii++) {
                for (int jj = 0; jj < estat.getSize(); jj++) {
                    if (estat.getPos(i, j) == 0) {
                        HexGameStatus estatAux = new HexGameStatus(estat);
                        Point tirada = new Point(ii, jj);
                        estatAux.placeStone(tirada);
                        int h_actual = MIN(estatAux, profunditat-1, nivellsExplorats+1, ii, jj, alpha, beta);
                        millor_valor = Math.max(millor_valor, h_actual);
                        if (_poda) {
                            alpha = Math.max(millor_valor, alpha);
                            if (beta <= alpha) { // Fem poda
                                break;
                            }
                        }
                    }
                }
                if (_poda) {
                    if (beta <= alpha) { // Fem poda
                        break;
                    }
                }
            }
        }
        return millor_valor;
    }

    private int heuristica(HexGameStatus estat, int color, int nivellsExplorats) {
        if (nivellsExplorats > _profExplorada) _profExplorada = nivellsExplorats;
        int midaTauler = estat.getSize();
        int[] distancies = new int[midaTauler*midaTauler];
        PriorityQueue<Point> cua = new PriorityQueue<>((a,b)->distancies[a.x*midaTauler + a.y] - distancies[b.x*midaTauler + b.y]);
        
        // Inicialitzar vector de distàncies
        for (int i = 0; i < midaTauler*midaTauler; i++) {
            distancies[i] = INFINIT;
            // Aprofitem el bucle per afegir també les caselles inicials de l'algorisme i actualitzar el vector distàncies per aquelles caselles
            int columna = i/midaTauler; // també se li pot anomenar x
            int fila = i%midaTauler; // també se li pot anomenar y
            
            if (color == 1 && columna == 0) {
                // El jugador amb color "1" ha d'unir la banda esquerra (columna==0) amb la banda dreta (columna==midaTauler-1)
                int colorCasella = estat.getPos(columna, fila);
                if (colorCasella != -color) {
                    // La casella té una fitxa del jugador de color 1 o està buida
                    cua.add(new Point(columna, fila));
                    if (colorCasella == color) {
                        // Casella amb fitxa del jugador de color 1: té distància 0
                        distancies[i] = 0;
                    }
                    else if (colorCasella == 0) {
                        // Casella buida: té distància 1
                        distancies[i] = 1;
                    }
                }
            }
            
            if (color == -1 && fila == 0) {
                // El jugador amb color "-1" ha d'unir la banda superior (fila==0) amb la banda inferior (fila==midaTauler-1)
                int colorCasella = estat.getPos(columna, fila);
                if (colorCasella != -color) {
                    // La casella té una fitxa del jugador de color -1 o està buida
                    cua.add(new Point(columna, fila));
                    if (colorCasella == color) {
                        // Casella amb fitxa del jugador de color -1: té distància 0
                        distancies[i] = 0;
                    }
                    else if (colorCasella == 0) {
                        // Casella buida: té distància 1
                        distancies[i] = 1;
                    }
                }
            }
            
        }
        
        // Algorisme de Dijkstra
        
        
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
