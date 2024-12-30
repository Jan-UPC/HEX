package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.PlayerType;
import edu.upc.epsevg.prop.hex.SearchType;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerMinimaxHexCalculatorsSINOPTIMIZAR implements IPlayer, IAuto {

    private final int INFINIT = Integer.MAX_VALUE;
    private final int MENYS_INFINIT = Integer.MIN_VALUE;

    private String _name;
    private PlayerType _Player;
    private int _colorPlayer;
    private int _profMax;
    private boolean _poda;
    private Dijkstra _dijkstra;
    int _profExpl;
    private TranspositionTable transpositionTable;
    private int _nPodas;

    public PlayerMinimaxHexCalculatorsSINOPTIMIZAR(String name, int profunditatMaxima, boolean poda) {
        this._name = name;
        this._profMax = profunditatMaxima;
        this._poda = poda;
        this._profExpl = 0;
        this._nPodas = 0;
        this._dijkstra = new Dijkstra();
        //this.transpositionTable = new TranspositionTable();
        //ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        //System.out.println("=============================================");
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();

        // Ordenar los movimientos usando la heurística actual
        List<MoveNode> movimientos = ordenarMovimientos(s);
        int numMovimientosEvaluar = Math.min(movimientos.size(), 10); // Limitar a las 20 mejores jugadas
        Point mejorMovimiento = movimientos.get(movimientos.size()/2).getPoint();
        int mejorValor = MENYS_INFINIT;
        int profExpl = 0;
        //int hash = ZobristHashing.calculateHash(s);

        //for (MoveNode movimiento : movimientos) {
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            _nPodas = 0;
            //System.out.println("---------------------------------------------");
            //System.out.println("Analizando: " + movimiento.getPoint().x + " " + movimiento.getPoint().y);
            Point punto = movimiento.getPoint();
            
            HexGameStatus estadoAux = new HexGameStatus(s);
            estadoAux.placeStone(punto);
            
            // **Comprobación de si el movimiento es ganador**
            if (estadoAux.isGameOver() && estadoAux.GetWinner() == _Player) {
                //System.out.println("Casilla ganadora");
                return new PlayerMove(punto, INFINIT, _profMax, SearchType.MINIMAX);
            }

            int valor = MAX(estadoAux, _profMax - 1, profExpl+1, punto, MENYS_INFINIT, INFINIT);
            
            /*
            if (valorGuardado != null && valorGuardado.depth >= _profMax - 1) {
                valor = valorGuardado.value;
            } else {
                HexGameStatus estadoAux = new HexGameStatus(s);
                estadoAux.placeStone(punto);

                valor = MIN(estadoAux, _profMax - 1, MENYS_INFINIT, INFINIT, newHash);
                transpositionTable.store(newHash, _profMax - 1, valor, TranspositionTable.EXACT, punto);
            }*/

            //System.out.println("movimiento con valor: " + valor);
            //System.out.println("numero de podas: " + _nPodas);
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        return new PlayerMove(mejorMovimiento, mejorValor, _profExpl, SearchType.MINIMAX);
    }

    //private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MIN(HexGameStatus estado, int profundidad, int nivelesExplorados, Point p,int alfa, int beta) {
        /*if (estado.isGameOver()) {
            if(estado.GetWinner() == _Player){
                System.out.println("Casilla ganadora en MIN");
                return INFINIT;
            } else {
                System.out.println("Casilla perdedora en MIN");
                return MENYS_INFINIT;
            }
        } */
        //TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        //if (entry != null && entry.depth >= profundidad) return entry.value;

        if (profundidad == 0) {
            return heuristica0(estado, _colorPlayer, nivelesExplorados);
        }

        int mejorValor = INFINIT;

        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    //System.out.println("****MIN****");
                    //System.out.println("Analizando: " + x + " " + y);
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point tirada = new Point(x, y);
                    estatAux.placeStone(tirada);
                    int valor = MAX(estatAux, profundidad-1, nivelesExplorados+1, p, alfa, beta);
                    //System.out.println("Valor de jugada: " + x + " " + y + " = " + valor);
                    if(valor < mejorValor){
                        mejorValor = valor;
                    }
                    if (_poda) {
                        beta = Math.min(mejorValor, beta);
                        if (beta <= alfa) { // Fem poda
                            _nPodas++;
                            break;
                        }
                    }
                }
            }
            if (_poda) {
                    if (beta <= alfa) { // Fem poda
                        _nPodas++;
                        break;
                    }
                }
        }
        return mejorValor;
    }

    //private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MAX(HexGameStatus estado, int profundidad, int nivelesExplorados, Point p, int alfa, int beta){
        /*if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        } */
        //TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        //if (entry != null && entry.depth >= profundidad) return entry.value;
        
        if (profundidad == 0) return heuristica0(estado, _colorPlayer, nivelesExplorados);

        int mejorValor = MENYS_INFINIT;

        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    //System.out.println("****MAX****");
                    //System.out.println("Analizando: " + x + " " + y);
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point tirada = new Point(x, y);
                    estatAux.placeStone(tirada);
                    int valor = MIN(estatAux, profundidad-1, nivelesExplorados+1, p, alfa, beta);
                    //System.out.println("Valor de jugada: " + x + " " + y + " = " + valor);
                    if(valor > mejorValor){
                        mejorValor = valor;
                    }
                    if (_poda) {
                        beta = Math.max(mejorValor, beta);
                        if (beta <= alfa) { // Fem poda
                            _nPodas++;
                            break;
                        }
                    }
                }
            }
            if (_poda) {
                    if (beta <= alfa) { // Fem poda
                        _nPodas++;
                        break;
                    }
                }
        }

        return mejorValor;
    }

    /**
    * Ordena los movimientos según la heurística actual.
    */
   public List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
    List<MoveNode> movimientos = estado.getMoves();
    
    movimientos.sort((a, b) -> {
        // Crear copias independientes del estado
        HexGameStatus estadoA = new HexGameStatus(estado);
        HexGameStatus estadoB = new HexGameStatus(estado);

        // Aplicar los movimientos a los estados temporales
        estadoA.placeStone(a.getPoint());
        estadoB.placeStone(b.getPoint());
        
        // Calcular las heurísticas
        int valorA = heuristica0(estadoA, _colorPlayer, 0);
        int valorB = heuristica0(estadoB, _colorPlayer, 0);

        // Comparar para ordenar de mayor a menor valor heurístico
        return Integer.compare(valorB, valorA);
    });

    return movimientos;
}


    public int heuristica0(HexGameStatus estado, int color, int nivelesExplorados) {
        Dijkstra result = _dijkstra.shortestPathWithVirtualNodes(estado, color);

        int caminoPropio = result.shortestPath;
        int caminosViables = result.viablePathsCount;
        int caminoEnemigo = result.enemyShortestPath;
        int caminosViablesEnemigo = result.viableEnemyPathsCount;

        if (caminoPropio == 0) return INFINIT;   // Victoria
        if (caminoEnemigo == 0) return MENYS_INFINIT; // Derrota inminente

        // Ponderar la heurística considerando los caminos viables del enemigo
        return (7 * (estado.getSize() - caminoPropio)) 
             + (6 * caminosViables) 
             - (5 * (estado.getSize() - caminoEnemigo))
             - (4 * caminosViablesEnemigo);
    }


    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void timeout() {
    }
}