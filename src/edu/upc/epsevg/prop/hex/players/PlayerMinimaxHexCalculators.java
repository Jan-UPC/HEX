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
import java.util.Random;

public class PlayerMinimaxHexCalculators implements IPlayer, IAuto {

    private final int INFINIT = Integer.MAX_VALUE;
    private final int MENYS_INFINIT = Integer.MIN_VALUE;

    private String _name;
    private PlayerType _Player;
    private int _colorPlayer;
    private int _profMax;
    private Dijkstra _dijkstra;
    int _profExpl;
    private TranspositionTable transpositionTable;
    private int _nPodas;

    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, int boardSize) {
        this._name = name;
        this._profMax = profunditatMaxima;
        this._profExpl = 0;
        this._nPodas = 0;
        this._dijkstra = new Dijkstra();
        this.transpositionTable = new TranspositionTable();
        ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        //System.out.println("=============================================");
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();

        long hash = ZobristHashing.calculateHash(s);
        TranspositionTable.TableEntry cachedEntry = transpositionTable.lookup(hash);
        
        // Si hay un valor exacto almacenado, usarlo directamente
        if (cachedEntry != null && cachedEntry.flag == TranspositionTable.EXACT) {
            return new PlayerMove(cachedEntry.bestMove, cachedEntry.value, _profMax, SearchType.MINIMAX);
        }
        
        // Ordenar los movimientos usando la heurística actual
        List<MoveNode> movimientos = ordenarMovimientos(s);
        int numMovimientosEvaluar = Math.min(movimientos.size(), 20); // Limitar a las 20 mejores jugadas
        Point mejorMovimiento = movimientos.get(movimientos.size()/2).getPoint();
        int mejorValor = MENYS_INFINIT;
        int profExpl = 0;

        for (int i = 0; i < numMovimientosEvaluar; i++) {
            _nPodas = 0;
            MoveNode movimiento = movimientos.get(i);
            
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

            long newHash = ZobristHashing.updateHash(hash, punto, 0, _colorPlayer);
            int valor = MIN(estadoAux, _profMax - 1, profExpl+1, MENYS_INFINIT, INFINIT, newHash);
            
            //System.out.println("movimiento con valor: " + valor);
            //System.out.println("numero de podas: " + _nPodas);
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        // Almacenar el mejor resultado en la tabla de transposición
        transpositionTable.store(hash, profExpl, mejorValor, TranspositionTable.EXACT, mejorMovimiento);
        
        return new PlayerMove(mejorMovimiento, mejorValor, profExpl, SearchType.MINIMAX);
    }

    //private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MIN(HexGameStatus estado, int profundidad, int nivelesExplorados,int alfa, int beta, long hash) {
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        
        if (entry != null && entry.depth >= profundidad) {
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    return entry.value;
                case TranspositionTable.LOWER_BOUND:
                    alfa = Math.max(alfa, entry.value);
                    break;
                case TranspositionTable.UPPER_BOUND:
                    beta = Math.min(beta, entry.value);
                    break;
                default:
                    break;
            }

            if (alfa >= beta) {
                return entry.value;
            }
        }
        
        
        /*if (estado.isGameOver()) {
            if(estado.GetWinner() == _Player){
                System.out.println("Casilla ganadora en MIN");
                return INFINIT;
            } else {
                System.out.println("Casilla perdedora en MIN");
                return MENYS_INFINIT;
            }
        } */

        if (profundidad == 0) {
            return heuristica(estado, _colorPlayer, nivelesExplorados);
        }

        int mejorValor = INFINIT;
        Point mejorPunto = null;

        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point punto = new Point(x, y);
                    estatAux.placeStone(punto);
                    
                    long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(x, y), _colorPlayer);
                    int valor = MAX(estatAux, profundidad-1, nivelesExplorados+1, alfa, beta, newHash);
                    
                    if(valor < mejorValor){
                        mejorValor = valor;
                        mejorPunto = punto;
                    }
                   
                    beta = Math.min(mejorValor, beta);
                    if (beta <= alfa) { // Fem poda
                        _nPodas++;
                        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.UPPER_BOUND, mejorPunto);
                        // return mejorValor;
                        break;
                    }
                    }
                }
            
            if (beta <= alfa) { // Fem poda
                _nPodas++;
                break;
            }
        }
        
        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.EXACT, mejorPunto);
        return mejorValor;
    }

    //private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MAX(HexGameStatus estado, int profundidad, int nivelesExplorados, int alfa, int beta, long hash){
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        
        if (entry != null && entry.depth >= profundidad) {
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    return entry.value;
                case TranspositionTable.LOWER_BOUND:
                    alfa = Math.max(alfa, entry.value);
                    break;
                case TranspositionTable.UPPER_BOUND:
                    beta = Math.min(beta, entry.value);
                    break;
                default:
                    break;
            }

            if (alfa >= beta) {
                return entry.value;
            }
        }
        
        /*if (estado.isGameOver()) {
            if(estado.GetWinner() == _Player){
                System.out.println("Casilla ganadora en MAX");
                return INFINIT;
            } else {
                System.out.println("Casilla perdedora en MAX");
                return MENYS_INFINIT;
            }
        } */
        
        if (profundidad == 0) return heuristica(estado, _colorPlayer, nivelesExplorados);

        int mejorValor = MENYS_INFINIT;
        Point mejorPunto = null;
        
        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point tirada = new Point(x, y);
                    estatAux.placeStone(tirada);
                    
                    long newHash = ZobristHashing.updateHash(hash, tirada, estado.getPos(x, y), _colorPlayer);
                    int valor = MIN(estatAux, profundidad-1, nivelesExplorados+1, alfa, beta, newHash);
                    
                    if(valor > mejorValor){
                        mejorValor = valor;
                        mejorPunto = tirada;
                    }
                    
                    beta = Math.max(mejorValor, beta);
                    if (beta <= alfa) { // Fem poda
                        _nPodas++;
                        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.LOWER_BOUND, mejorPunto);
                        //return mejorValor
                        break;
                    }
                }
            }
            if (beta <= alfa) { // Fem poda
                _nPodas++;
                break;
            }
        }

        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.EXACT, mejorPunto);
        return mejorValor;
    }

    /**
    * Ordena los movimientos según la heurística actual.
    */
   private List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
       List<MoveNode> movimientos = estado.getMoves();

       movimientos.sort((a, b) -> {
           HexGameStatus estadoA = new HexGameStatus(estado);
           HexGameStatus estadoB = new HexGameStatus(estado);

           estadoA.placeStone(a.getPoint());
           estadoB.placeStone(b.getPoint());

           int valorA = heuristica(estadoA, _colorPlayer, 0);
           int valorB = heuristica(estadoB, _colorPlayer, 0);

           return Integer.compare(valorB, valorA); // Ordenar de mayor a menor valor
       });

       return movimientos;
   }

    public int heuristica(HexGameStatus estado, int color, int nivelesExplorados) {
        Dijkstra result = _dijkstra.shortestPathWithVirtualNodes(estado, color);

        int caminoPropio = result.shortestPath;
        int caminosViables = result.viablePathsCount;
        int caminoEnemigo = result.enemyShortestPath;
        int caminosViablesEnemigo = result.viableEnemyPathsCount;

        if (caminoPropio == 0) return INFINIT;   // Victoria
        if (caminoEnemigo == 0) return MENYS_INFINIT; // Derrota inminente

        // Ponderar la heurística considerando los caminos viables del enemigo
        return (10 * (caminoEnemigo - caminoPropio)) 
             + (7 * caminosViables) 
             - (0 * caminosViablesEnemigo);
    }


    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void timeout() {
    }
}

class TranspositionTable {
    static final int EXACT = 0;       // Valor exacto
    static final int LOWER_BOUND = 1; // Cota inferior
    static final int UPPER_BOUND = 2; // Cota superior 
    
    private HashMap<Long, TableEntry> table = new HashMap<>();

    public void store(long hash, int depth, int value, int flag, Point bestMove) {
        table.put(hash, new TableEntry(value, depth, flag, bestMove));
    }

    public TableEntry lookup(long hash) {
        return table.getOrDefault(hash, null);
    }

    static class TableEntry {
        int value;    // Valor de la posición
        int depth;    // Profundidad
        int flag;     // EXACTO, LOWER_BOUND, UPPER_BOUND
        Point bestMove;

        public TableEntry(int value, int depth, int flag, Point bestMove) {
            this.value = value;
            this.depth = depth;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }
}


class ZobristHashing {
    private static int boardSize = 11;
    private static long[][][] ZOBRIST_TABLE;

    public static void setBoardSize(int newSize) {
        boardSize = newSize;
        generateZobristTable();
    }

    private static void generateZobristTable() {
        ZOBRIST_TABLE = new long[boardSize][boardSize][3]; // Vacío, jugador 1, jugador 2
        Random random = new Random();
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                for (int k = 0; k < 3; k++) {
                    ZOBRIST_TABLE[x][y][k] = random.nextLong();
                }
            }
        }
    }

    public static long calculateHash(HexGameStatus estado) {
        long hash = 0;
        for (int x = 0; x < estado.getSize(); x++) {
            for (int y = 0; y < estado.getSize(); y++) {
                int piece = estado.getPos(x, y); // 0: vacío, 1: jugador 1, -1: jugador 2
                int index = (piece == 1) ? 1 : (piece == -1) ? 2 : 0;
                hash ^= ZOBRIST_TABLE[x][y][index];
            }
        }
        return hash;
    }

    public static long updateHash(long hash, Point move, int oldState, int newState) {
        int oldIndex = (oldState == 1) ? 1 : (oldState == -1) ? 2 : 0;
        int newIndex = (newState == 1) ? 1 : (newState == -1) ? 2 : 0;

        return hash ^ ZOBRIST_TABLE[move.x][move.y][oldIndex] ^ ZOBRIST_TABLE[move.x][move.y][newIndex];
    }
}

