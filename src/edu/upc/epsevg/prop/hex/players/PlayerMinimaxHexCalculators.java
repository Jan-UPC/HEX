package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.SearchType;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerMinimaxHexCalculators implements IPlayer, IAuto {

    private final int INFINIT = Integer.MAX_VALUE;
    private final int MENYS_INFINIT = Integer.MIN_VALUE;

    private String _name;
    private int _colorPlayer;
    private int _profMax;
    private boolean _poda;
    private Dijkstra dijkstra;
    int _profExpl;
    private TranspositionTable transpositionTable;

    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, boolean poda) {
        this._name = name;
        this._profMax = profunditatMaxima;
        this._poda = poda;
        this._profExpl = 0;
        this.dijkstra = new Dijkstra();
        //this.transpositionTable = new TranspositionTable();
        //ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        _colorPlayer = s.getCurrentPlayerColor();

        List<MoveNode> movimientos = s.getMoves();
        Point mejorMovimiento = movimientos.get(movimientos.size()/2).getPoint();
        int mejorValor = MENYS_INFINIT;
        int profExpl = 0;
        //int hash = ZobristHashing.calculateHash(s);

        //ArrayList<MoveNode> movimientos = ordenarMovimientos(s, hash);
        //for (MoveNode movimiento : movimientos) {
        for(MoveNode movimiento : movimientos){
            System.out.println("Analizando: " + movimiento.getPoint().x + " " + movimiento.getPoint().y);
            Point punto = movimiento.getPoint();
            
            HexGameStatus estadoAux = new HexGameStatus(s);
            estadoAux.placeStone(punto);
            
            // **Comprobación de si el movimiento es ganador**
            if (estadoAux.isGameOver() && estadoAux.GetWinner() == estadoAux.getCurrentPlayer()) {
                return new PlayerMove(punto, INFINIT, _profMax, SearchType.MINIMAX);
            }

            //int newHash = ZobristHashing.updateHash(hash, punto, _colorPlayer);
            //TranspositionTable.TableEntry valorGuardado = transpositionTable.lookup(newHash);

            int valor = MIN(estadoAux, _profMax - 1, profExpl+1, punto, MENYS_INFINIT, INFINIT);
            
            /*
            if (valorGuardado != null && valorGuardado.depth >= _profMax - 1) {
                valor = valorGuardado.value;
            } else {
                HexGameStatus estadoAux = new HexGameStatus(s);
                estadoAux.placeStone(punto);

                valor = MIN(estadoAux, _profMax - 1, MENYS_INFINIT, INFINIT, newHash);
                transpositionTable.store(newHash, _profMax - 1, valor, TranspositionTable.EXACT, punto);
            }*/

            System.out.println("movimiento con valor: " + valor);
            
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        return new PlayerMove(mejorMovimiento, mejorValor, _profExpl, SearchType.MINIMAX);
    }

    //private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MIN(HexGameStatus estado, int profundidad, int nivelesExplorados, Point p,int alfa, int beta) {
        //TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        //if (entry != null && entry.depth >= profundidad) return entry.value;

        if (profundidad == 0) return heuristica(estado, -_colorPlayer, nivelesExplorados);

        int mejorValor = INFINIT;

        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point tirada = new Point(x, y);
                    estatAux.placeStone(tirada);
                    int valor = MAX(estatAux, profundidad-1, nivelesExplorados+1, p, alfa, beta);
                    if(valor < mejorValor){
                        mejorValor = valor;
                    }
                    if (_poda) {
                        beta = Math.min(mejorValor, beta);
                        if (beta <= alfa) { // Fem poda
                            break;
                        }
                    }
                }
            }
            if (_poda) {
                    if (beta <= alfa) { // Fem poda
                        break;
                    }
                }
        }
        /*
        for (MoveNode movimiento : estado.getMoves()) {
            Point punto = movimiento.getPoint();
            int newHash = ZobristHashing.updateHash(hash, punto, -_colorPlayer);

            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(punto);

            int valor = MAX(estadoAux, profundidad - 1, alfa, beta);
            if(valor < mejorValor){
                mejorValor = valor;
            }
            mejorValor = Math.min(mejorValor, MAX(estadoAux, profundidad - 1, alfa, beta, newHash));
            beta = Math.min(beta, mejorValor);

            if (_poda && beta <= alfa) break;
        } */
        return mejorValor;
    }

    //private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
    private int MAX(HexGameStatus estado, int profundidad, int nivelesExplorados, Point p, int alfa, int beta){
        //TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        //if (entry != null && entry.depth >= profundidad) return entry.value;

        if (profundidad == 0) return heuristica(estado, _colorPlayer, nivelesExplorados);

        int mejorValor = MENYS_INFINIT;

        for(int x = 0; x < estado.getSize(); x++){
            for (int y = 0; y < estado.getSize(); y++){
                // Bucle de les files
                if (estado.getPos(x, y)==0) {
                    HexGameStatus estatAux = new HexGameStatus(estado);
                    Point tirada = new Point(x, y);
                    estatAux.placeStone(tirada);
                    int valor = MIN(estatAux, profundidad-1, nivelesExplorados+1, p, alfa, beta);
                    if(valor > mejorValor){
                        mejorValor = valor;
                    }
                    if (_poda) {
                        beta = Math.max(mejorValor, beta);
                        if (beta <= alfa) { // Fem poda
                            break;
                        }
                    }
                }
            }
            if (_poda) {
                    if (beta <= alfa) { // Fem poda
                        break;
                    }
                }
        }
        
        /*for (MoveNode movimiento : estado.getMoves()) {
            Point punto = movimiento.getPoint();
            int newHash = ZobristHashing.updateHash(hash, punto, _colorPlayer);

            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(punto);

            mejorValor = Math.max(mejorValor, MIN(estadoAux, profundidad - 1, alfa, beta, newHash));
            alfa = Math.max(alfa, mejorValor);

            if (_poda && alfa >= beta) break;
        }*/

        return mejorValor;
    }

    private ArrayList<MoveNode> ordenarMovimientos(HexGameStatus estado, int hash) {
        ArrayList<MoveNode> movimientos = new ArrayList<>(estado.getMoves());
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);

        movimientos.sort((a, b) -> {
            if (entry != null && entry.bestMove != null) {
                if (a.getPoint().equals(entry.bestMove)) return -1;
                if (b.getPoint().equals(entry.bestMove)) return 1;
            }
            return 0;
        });

        return movimientos;
    }

    public int heuristica(HexGameStatus estado, int color, int nivelesExplorados) {
        if (nivelesExplorados > _profExpl) _profExpl = nivelesExplorados;
        int distanciaPropia = dijkstra.shortestPathWithVirtualNodes(estado, color);
        int distanciaOponente = dijkstra.shortestPathWithVirtualNodes(estado, -color);

        if (distanciaPropia == 0) return INFINIT;
        if (distanciaOponente == 0) return MENYS_INFINIT;

        
        //return 10 * (distanciaPropia - distanciaOponente);
        //return 10 * (distanciaOponente - distanciaPropia) + 2 * (estado.getSize() - distanciaPropia);
        //return (estado.getSize() - distanciaPropia);
        return distanciaPropia;

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

    private HashMap<Integer, TableEntry> table = new HashMap<>();

    public void store(int hash, int depth, int value, int type, Point bestMove) {
        table.put(hash, new TableEntry(value, depth, type, bestMove));
    }

    public TableEntry lookup(int hash) {
        return table.get(hash);
    }

    static class TableEntry {
        int value, depth, type;
        Point bestMove;

        TableEntry(int value, int depth, int type, Point bestMove) {
            this.value = value;
            this.depth = depth;
            this.type = type;
            this.bestMove = bestMove;
        }
    }
}

class ZobristHashing {
    private static int boardSize = 11;
    private static int[][][] ZOBRIST_TABLE;

    public static void setBoardSize(int newSize) {
        boardSize = newSize;
        generateZobristTable();
    }

    private static void generateZobristTable() {
        ZOBRIST_TABLE = new int[boardSize][boardSize][3];
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                for (int k = 0; k < 3; k++)
                    ZOBRIST_TABLE[i][j][k] = random.nextInt();
    }

    public static int calculateHash(HexGameStatus estado) {
        int hash = 0;
        for (int i = 0; i < estado.getSize(); i++)
            for (int j = 0; j < estado.getSize(); j++) {
                int pos = estado.getPos(i, j);
                int index = (pos == 1) ? 1 : (pos == -1) ? 2 : 0;
                hash ^= ZOBRIST_TABLE[i][j][index];
            }
        return hash;
    }

    public static int updateHash(int hash, Point move, int color) {
        int index = (color == 1) ? 1 : 2;
        return hash ^ ZOBRIST_TABLE[move.x][move.y][0] ^ ZOBRIST_TABLE[move.x][move.y][index];
    }
}
