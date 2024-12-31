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
    private int _totalTime;
    int _colorPlayer;
    private int _nMoves;
    private long _hashTableroVacio;
    int _nNodes;
    private int _profMax;
    private Dijkstra _dijkstra;
    int _profExpl;
    private TranspositionTable transpositionTable;

    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, int boardSize) {
        init(name, profunditatMaxima, boardSize);
    }
    
    private void init(String name, int profunditatMaxima, int boardSize){
        System.out.println("==========================================");
        this._name = name;
        this._profMax = profunditatMaxima;
        this._profExpl = 0;
        this._nMoves = 0;
        this._nNodes = 0;
        this._totalTime = 0;
        this._dijkstra = new Dijkstra();
        this.transpositionTable = new TranspositionTable();
        HexGameStatus s = new HexGameStatus(boardSize);
        ZobristHashing.setBoardSize(boardSize);
        this._hashTableroVacio = ZobristHashing.calculateHash(s);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        _nNodes = 0;
        _nMoves++;
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();
        long initialTime = System.currentTimeMillis();
        // Calculamos el hash inicial
        long hash = ZobristHashing.calculateHash(s);
        // Comprobamos que no hayamos empezado otra partida para reiniciar todos los valores
        if(_hashTableroVacio==hash && _nMoves!=1){
            /*double estadistica = (_totalTime)/_nMoves;
            System.out.println("Tiempo total del juego en ms: " + _totalTime);
            System.out.println("Numero total de movimientos: " + _nMoves);
            System.out.println("Estadistica ms/moves: " + estadistica);*/
            init(_name, _profMax, s.getSize());
            _nMoves++;
        }
             
        // Ordenar los movimientos usando la heurística
        List<MoveNode> movimientos;
        if(_nMoves < 3){
            movimientos = ordenarMovimientosRapido(s);
        } else {
            movimientos = ordenarMovimientos(s);
        }
        int numMovimientosEvaluar = Math.min(movimientos.size(), 30);
        Point mejorMovimiento = movimientos.get(movimientos.size()/2).getPoint();
        int mejorValor = MENYS_INFINIT;
        int profExpl = 0;

        for (int i = 0; i < numMovimientosEvaluar; i++) {
            _nNodes++;
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();
            
            HexGameStatus estadoAux = new HexGameStatus(s);
            long newHash = ZobristHashing.updateHash(hash, punto, s.getPos(punto.x, punto.y), _colorPlayer);
            estadoAux.placeStone(punto);
            
            // **Comprobación de si el movimiento es ganador**
            if (estadoAux.isGameOver() && estadoAux.GetWinner() == _Player) {
                long finalTime = System.currentTimeMillis();
                long realTime = finalTime - initialTime;
                _totalTime += realTime;
                /*double estadistica = (_totalTime)/_nMoves;
                System.out.println("Tiempo total del movimiento en ms: " + realTime);
                System.out.println("Tiempo total del juego en ms: " + _totalTime);
                System.out.println("Numero total de movimientos: " + _nMoves);
                System.out.println("Estadistica ms/moves: " + estadistica);*/
                return new PlayerMove(punto, _nNodes, _profExpl, SearchType.MINIMAX);
            }
            
            int valor = MIN(estadoAux, _profMax - 1, profExpl+1, MENYS_INFINIT, INFINIT, newHash);
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        // Almacenar el mejor resultado en la tabla de transposición
        transpositionTable.store(hash, profExpl, mejorValor, TranspositionTable.EXACT, mejorMovimiento);
        long finalTime = System.currentTimeMillis();
        long realTime = finalTime - initialTime;
        _totalTime += realTime;
        /*double estadistica = (_totalTime)/_nMoves;
        System.out.println("Tiempo total del movimiento en ms: " + realTime);
        System.out.println("Tiempo total del juego en ms: " + _totalTime);
        System.out.println("Numero total de movimientos: " + _nMoves);
        System.out.println("Estadistica ms/moves: " + estadistica);*/
        return new PlayerMove(mejorMovimiento, _nNodes, _profExpl, SearchType.MINIMAX);
    }

    //private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MIN(HexGameStatus estado, int profundidad, int nivelesExplorados,int alfa, int beta, long hash) {  
        _nNodes++;
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        }
        
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        Point mejorPunto = null;
        if (entry != null) {
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    if(entry.depth >= profundidad){
                        return entry.value;
                    } else {
                        break;
                    }                    
                case TranspositionTable.alfa:
                    if(entry.value > alfa){
                        alfa = entry.value;
                        mejorPunto = entry.bestMove;
                    }                    
                    break;
                case TranspositionTable.beta:
                    if(entry.value < beta){
                        beta = entry.value;
                        mejorPunto = entry.bestMove;
                    }
                    break;
                default:
                    break;
            }

            if (alfa >= beta) {
                return entry.value;
            }
        }

        if (profundidad == 0) {
            _profExpl = nivelesExplorados;
            return heuristica(estado, _colorPlayer);
        }

        int mejorValor = INFINIT;
        
        // Ordenar movimientos por heurística 
        List<MoveNode> movimientos;
        movimientos = ordenarMovimientosRapido(estado);
        int numMovimientosEvaluar = Math.min(movimientos.size(), 30);
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            int valor = MAX(estadoAux, profundidad - 1, nivelesExplorados + 1, alfa, beta, newHash);
            mejorValor = Math.min(mejorValor, valor);
            beta = Math.min(beta, mejorValor);
            if (beta <= alfa) {
                transpositionTable.store(hash, profundidad, beta, TranspositionTable.beta, mejorPunto);
                return mejorValor; // Poda
            }
        }

        return mejorValor;
    }

    private int MAX(HexGameStatus estado, int profundidad, int nivelesExplorados, int alfa, int beta, long hash){
        _nNodes++;
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        } 
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        Point mejorPunto = null;
        if (entry != null) {
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    if(entry.depth >= profundidad){
                        return entry.value;
                    } else {
                        break;
                    }
                case TranspositionTable.alfa:
                    alfa = Math.max(alfa, entry.value);
                    mejorPunto = entry.bestMove;
                    break;
                case TranspositionTable.beta:
                    beta = Math.min(beta, entry.value);
                    mejorPunto = entry.bestMove;
                    break;
                default:
                    break;
            }

            if (alfa >= beta) {
                return entry.value;
            }
        }
        
        if (profundidad == 0){
            _profExpl = nivelesExplorados;
            return heuristica(estado, _colorPlayer);
        }
        int mejorValor = MENYS_INFINIT;
        // Ordenar movimientos por heurística
        List<MoveNode> movimientos;
        movimientos = ordenarMovimientosRapido(estado);
        int numMovimientosEvaluar = Math.min(movimientos.size(), 30);
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            int valor = MIN(estadoAux, profundidad - 1, nivelesExplorados + 1, alfa, beta, newHash);
            mejorValor = Math.max(mejorValor, valor);
            alfa = Math.max(alfa, mejorValor);
            if (beta <= alfa) {
                transpositionTable.store(hash, profundidad, alfa, TranspositionTable.alfa, mejorPunto);
                return mejorValor; // Poda
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
           int valorA = heuristica(estadoA, _colorPlayer);
           int valorB = heuristica(estadoB, _colorPlayer);
           
           // Comparar para ordenar de mayor a menor valor heurístico
           return Integer.compare(valorB, valorA); // Ordenar de mayor a menor valor
       });

       return movimientos;
   }

    public int heuristica(HexGameStatus estado, int color) {
        Dijkstra result = _dijkstra.shortestPathWithVirtualNodes(estado, color);

        int caminoPropio = result.shortestPath;
        int caminosViables = result.viablePathsCount;
        int caminoEnemigo = result.enemyShortestPath;
        int caminosViablesEnemigo = result.viableEnemyPathsCount;
        /*System.out.println("Camino propio: " + caminoPropio +
                " Camino enemigo: " + caminoEnemigo +
                " caminos vialbles: " + caminosViables +
                " caminos viables enemigo " + caminosViablesEnemigo);*/
        if (caminoPropio == 0) return INFINIT;   // Victoria
        if (caminoEnemigo == 0) return MENYS_INFINIT; // Derrota 

        return (10 * (estado.getSize() - caminoPropio)) 
             + (3 * caminosViables) 
             - (7 * (estado.getSize() - caminoEnemigo))
             - (3 * caminosViablesEnemigo);
    }

    public List<MoveNode> ordenarMovimientosRapido(HexGameStatus estado) {
        List<MoveNode> movimientos = estado.getMoves();

        movimientos.sort((a, b) -> {
            int valorA = heuristicaRapida(estado, a.getPoint());
            int valorB = heuristicaRapida(estado, b.getPoint());
            return Integer.compare(valorB, valorA); // Ordenar de mayor a menor
        });

        return movimientos;
    }

    public int heuristicaRapida(HexGameStatus estado, Point punto) {
        // Aproximación rápida basada en proximidad al centro y valor de los vecinos 
        int distanciaCentro = Math.abs(punto.x - estado.getSize() / 2) + Math.abs(punto.y - estado.getSize() / 2);
        // Evaluar vecinos
        int puntuacionVecinos = 0;
        for (Point vecino : estado.getNeigh(punto)) {
            int estadoVecino = estado.getPos(vecino.x, vecino.y);
            if (estadoVecino == 0) { // Vecino vacío
                puntuacionVecinos += 1;
            } else if (estadoVecino == estado.getCurrentPlayerColor()) { // Vecino nuestro
                puntuacionVecinos += 2;
            } else { // Vecino del enemigo
                puntuacionVecinos -= 1;
            }
        }
        return -distanciaCentro + puntuacionVecinos;
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
    static final int EXACT = 0; 
    static final int alfa = 1; 
    static final int beta = 2; 
    
    private HashMap<Long, TableEntry> table = new HashMap<>();

    public void store(long hash, int depth, int value, int flag, Point bestMove) {
        table.put(hash, new TableEntry(value, depth, flag, bestMove));
    }

    public TableEntry lookup(long hash) {
        TableEntry entry = table.get(hash);
        return entry;
    }

    static class TableEntry {
        int value;   
        int depth;    
        int flag;     
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
        ZOBRIST_TABLE = new long[boardSize][boardSize][3];
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
                int piece = estado.getPos(x, y); 
                int index = (piece == 1) ? 1 : (piece == -1) ? 2 : 0;
                hash ^= ZOBRIST_TABLE[x][y][index];
            }
        }
        return hash;
    }

    public static long updateHash(long hash, Point move, int oldState, int newState) {
        int oldIndex = 0;
        int newIndex = (newState == 1) ? 1 : (newState == -1) ? 2 : 0;
        long updatedHash = hash ^ ZOBRIST_TABLE[move.x][move.y][oldIndex] ^ ZOBRIST_TABLE[move.x][move.y][newIndex];
        return updatedHash;
    }
}

