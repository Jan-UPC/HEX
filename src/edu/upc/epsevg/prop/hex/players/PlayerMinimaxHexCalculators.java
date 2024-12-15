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

        ArrayList<MoveNode> movimientos = ordenarMovimientos(s);

        for (MoveNode movimiento : movimientos) {
            HexGameStatus estadoAux = new HexGameStatus(s);
            estadoAux.placeStone(movimiento.getPoint());

            int hash = ZobristHashing.calculateHash(estadoAux);
            Integer valorGuardado = transpositionTable.lookup(hash, _profMax);
            if (valorGuardado != null) {
                return new PlayerMove(movimiento.getPoint(), valorGuardado, _profMax, SearchType.MINIMAX);
            }

            int valor = MIN(estadoAux, _profMax - 1, MENYS_INFINIT, INFINIT);
            transpositionTable.store(hash, _profMax, valor);

            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = movimiento.getPoint();
            }
        }

        if (mejorMovimiento == null && !movimientos.isEmpty()) {
            mejorMovimiento = movimientos.get(0).getPoint();
        }

        return new PlayerMove(mejorMovimiento, mejorValor, _profMax, SearchType.MINIMAX);
    }

    private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta) {
        int hash = ZobristHashing.calculateHash(estado);
        Integer valorGuardado = transpositionTable.lookup(hash, profundidad);
        if (valorGuardado != null) return valorGuardado;

        if (estado.isGameOver()) return (estado.GetWinner() == estado.getCurrentPlayer()) ? MENYS_INFINIT : INFINIT;
        if (profundidad == 0) return heuristica(estado, -_colorPlayer);

        int mejorValor = INFINIT;

        for (MoveNode movimiento : estado.getMoves()) {
            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(movimiento.getPoint());
            mejorValor = Math.min(mejorValor, MAX(estadoAux, profundidad - 1, alfa, beta));
            beta = Math.min(beta, mejorValor);
            if (_poda && beta <= alfa) break;
        }

        transpositionTable.store(hash, profundidad, mejorValor);
        return mejorValor;
    }

    private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta) {
        int hash = ZobristHashing.calculateHash(estado);
        Integer valorGuardado = transpositionTable.lookup(hash, profundidad);
        if (valorGuardado != null) return valorGuardado;

        if (estado.isGameOver()) return (estado.GetWinner() == estado.getCurrentPlayer()) ? INFINIT : MENYS_INFINIT;
        if (profundidad == 0) return heuristica(estado, _colorPlayer);

        int mejorValor = MENYS_INFINIT;

        for (MoveNode movimiento : estado.getMoves()) {
            HexGameStatus estadoAux = new HexGameStatus(estado);
            estadoAux.placeStone(movimiento.getPoint());
            mejorValor = Math.max(mejorValor, MIN(estadoAux, profundidad - 1, alfa, beta));
            alfa = Math.max(alfa, mejorValor);
            if (_poda && alfa >= beta) break;
        }

        transpositionTable.store(hash, profundidad, mejorValor);
        return mejorValor;
    }

    public int heuristica(HexGameStatus estado, int color) {
        int distanciaPropia = dijkstra.shortestPathWithVirtualNodes(estado, color);
        int distanciaOponente = dijkstra.shortestPathWithVirtualNodes(estado, -color);

        if (distanciaPropia == 0) return INFINIT;
        if (distanciaOponente == 0) return MENYS_INFINIT;

        //return 10 * (distanciaOponente - distanciaPropia);
        return 10 * (distanciaOponente - distanciaPropia) + (estado.getSize() - distanciaPropia);

    }

    private ArrayList<MoveNode> ordenarMovimientos(HexGameStatus estado) {
        ArrayList<MoveNode> movimientos = new ArrayList<>(estado.getMoves());

        movimientos.sort((a, b) -> {
            int heurA = evaluarMovimiento(estado, a.getPoint());
            int heurB = evaluarMovimiento(estado, b.getPoint());
            return Integer.compare(heurB, heurA);
        });

        return movimientos;
    }

    private int evaluarMovimiento(HexGameStatus estado, Point movimiento) {
        HexGameStatus estadoAux = new HexGameStatus(estado);
        estadoAux.placeStone(movimiento);

        int distanciaPropia = dijkstra.shortestPathWithVirtualNodes(estadoAux, _colorPlayer);
        int distanciaOponente = dijkstra.shortestPathWithVirtualNodes(estadoAux, -_colorPlayer);

        //return 10 * (distanciaOponente - distanciaPropia);
        return 10 * (distanciaOponente - distanciaPropia) + (estado.getSize() - distanciaPropia);
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
    private HashMap<Integer, Integer> table = new HashMap<>();

    public void store(int hash, int depth, int value) {
        table.put(hash ^ depth, value);
    }

    public Integer lookup(int hash, int depth) {
        return table.getOrDefault(hash ^ depth, null);
    }
}

class ZobristHashing {

    private static int boardSize = 11; // Valor por defecto
    private static int[][][] ZOBRIST_TABLE;

    // Método para actualizar boardSize y regenerar ZOBRIST_TABLE
    public static void setBoardSize(int newSize) {
        boardSize = newSize;
        generateZobristTable();
    }

    // Método que inicializa la tabla ZOBRIST_TABLE
    private static void generateZobristTable() {
        ZOBRIST_TABLE = new int[boardSize][boardSize][3];
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                for (int k = 0; k < 3; k++) {
                    ZOBRIST_TABLE[i][j][k] = random.nextInt();
                }
            }
        }
    }

    // Constructor estático: inicializa ZOBRIST_TABLE con el tamaño inicial
    static {
        generateZobristTable();
    }

    // Cálculo del hash del estado actual
    public static int calculateHash(HexGameStatus estado) {
        int size = estado.getSize();
        int hash = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int pos = estado.getPos(i, j); // Obtener el valor de la casilla: 0, 1 o -1
                int zobristIndex = (pos == 1) ? 1 : (pos == -1) ? 2 : 0; // 0 = vacío, 1 = jugador 1, 2 = jugador 2
                hash ^= ZOBRIST_TABLE[i][j][zobristIndex]; // Actualización XOR del hash
            }
        }

        return hash;
    }
}

