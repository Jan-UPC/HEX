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

public class PlayerMinimaxHexCalculators implements IPlayer, IAuto {

    private final int INFINIT = Integer.MAX_VALUE;
    private final int MENYS_INFINIT = Integer.MIN_VALUE;

    private String _name;
    private int _colorPlayer;
    private int _profMax;
    private boolean _poda;
    private Dijkstra dijkstra;

    private TranspositionTable transpositionTable;

    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, boolean poda, int boardSize) {
        this._name = name;
        this._profMax = profunditatMaxima;
        this._poda = poda;
        this.dijkstra = new Dijkstra(boardSize);
        this.transpositionTable = new TranspositionTable();
        ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        _colorPlayer = s.getCurrentPlayerColor();

        Point mejorMovimiento = null;
        int mejorValor = MENYS_INFINIT;
        int hash = ZobristHashing.calculateHash(s);

        ArrayList<MoveNode> movimientos = ordenarMovimientos(s, hash);

        for (MoveNode movimiento : movimientos) {
            Point punto = movimiento.getPoint();
            
            // **Comprobación de si el movimiento es ganador**
            if (esMovimientoGanador(s, punto)) {
                return new PlayerMove(punto, INFINIT, _profMax, SearchType.MINIMAX);
            }

            int newHash = ZobristHashing.updateHash(hash, punto, _colorPlayer);
            TranspositionTable.TableEntry valorGuardado = transpositionTable.lookup(newHash);

            int valor;
            if (valorGuardado != null && valorGuardado.depth >= _profMax - 1) {
                valor = valorGuardado.value;
            } else {
                HexGameStatus estadoAux = new HexGameStatus(s);
                estadoAux.placeStone(punto);

                valor = MIN(estadoAux, _profMax - 1, MENYS_INFINIT, INFINIT, newHash);
                transpositionTable.store(newHash, _profMax - 1, valor, TranspositionTable.EXACT, punto);
            }

            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        if (mejorMovimiento == null && !movimientos.isEmpty()) {
            mejorMovimiento = movimientos.get(0).getPoint();
        }

        return new PlayerMove(mejorMovimiento, mejorValor, _profMax, SearchType.MINIMAX);
    }

    private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        if (entry != null && entry.depth >= profundidad) return entry.value;

        if (estado.isGameOver()) {
            return (estado.GetWinner() == estado.getCurrentPlayer()) ? MENYS_INFINIT : INFINIT;
        }

        if (profundidad == 0) return heuristica(estado, -_colorPlayer);

        int mejorValor = INFINIT;

        for (MoveNode movimiento : estado.getMoves()) {
            Point punto = movimiento.getPoint();
            int newHash = ZobristHashing.updateHash(hash, punto, -_colorPlayer);

            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(punto);

            mejorValor = Math.min(mejorValor, MAX(estadoAux, profundidad - 1, alfa, beta, newHash));
            beta = Math.min(beta, mejorValor);

            if (_poda && beta <= alfa) break;
        }

        return mejorValor;
    }

    private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, int hash) {
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        if (entry != null && entry.depth >= profundidad) return entry.value;

        if (estado.isGameOver()) {
            return (estado.GetWinner() == estado.getCurrentPlayer()) ? INFINIT : MENYS_INFINIT;
        }

        if (profundidad == 0) return heuristica(estado, _colorPlayer);

        int mejorValor = MENYS_INFINIT;

        for (MoveNode movimiento : estado.getMoves()) {
            Point punto = movimiento.getPoint();
            int newHash = ZobristHashing.updateHash(hash, punto, _colorPlayer);

            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(punto);

            mejorValor = Math.max(mejorValor, MIN(estadoAux, profundidad - 1, alfa, beta, newHash));
            alfa = Math.max(alfa, mejorValor);

            if (_poda && alfa >= beta) break;
        }

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

    public int heuristica(HexGameStatus estado, int color) {
        int distanciaPropia = dijkstra.shortestPathWithVirtualNodes(estado, color);
        int distanciaOponente = dijkstra.shortestPathWithVirtualNodes(estado, -color);

        if (distanciaPropia == 0) return INFINIT;
        if (distanciaOponente == 0) return MENYS_INFINIT;

        //return 10 * (distanciaOponente - distanciaPropia);
        return 10 * (distanciaOponente - distanciaPropia) + 2 * (estado.getSize() - distanciaPropia);

    }

    private boolean esMovimientoGanador(HexGameStatus estado, Point movimiento) {
        HexGameStatus estadoAux = new HexGameStatus(estado);
        estadoAux.placeStone(movimiento);
        return estadoAux.isGameOver() && estadoAux.GetWinner() == estadoAux.getCurrentPlayer();
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
