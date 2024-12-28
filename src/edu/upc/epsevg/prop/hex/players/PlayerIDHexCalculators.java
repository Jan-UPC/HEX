package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.PlayerType;
import edu.upc.epsevg.prop.hex.SearchType;

import java.awt.Point;
import java.util.List;

public class PlayerIDHexCalculators implements IPlayer, IAuto {

    private final int INFINIT = Integer.MAX_VALUE;
    private final int MENYS_INFINIT = Integer.MIN_VALUE;

    private String _name;
    private PlayerType _Player;
    private int _colorPlayer;
    private int _profMax;
    private Dijkstra _dijkstra;
    private long timeoutLimit;
    private boolean timeoutTriggered;
    private TranspositionTable transpositionTable;
    private int _nPodas;

    public PlayerIDHexCalculators(String name, int boardSize) {
        this._name = name;
        this._profMax = 1; // Comenzar desde profundidad 1 para Iterative Deepening
        this._dijkstra = new Dijkstra();
        this.transpositionTable = new TranspositionTable();
        ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();

        // Configurar timeout
        timeoutTriggered = false;
        timeoutLimit = System.currentTimeMillis() + 4500; // Timeout en 4.5 segundos

        Point mejorMovimiento = null;
        int mejorValor = MENYS_INFINIT;

        // Hash inicial para el estado actual
        long hash = ZobristHashing.calculateHash(s);

        // Iterative Deepening
        for (int depth = 1; !timeoutTriggered; depth++) {
            _profMax = depth;
            try {
                Point movimientoActual = realizarBusqueda(s, hash, depth);
                mejorMovimiento = movimientoActual;
                mejorValor = heuristica(s, _colorPlayer, 0);
            } catch (TimeoutException e) {
                break;
            }
        }

        return new PlayerMove(mejorMovimiento, mejorValor, _profMax, SearchType.MINIMAX);
    }

    private Point realizarBusqueda(HexGameStatus s, long hash, int profundidad) throws TimeoutException {
        List<MoveNode> movimientos = ordenarMovimientos(s);
        Point mejorMovimiento = movimientos.get(0).getPoint();
        int mejorValor = MENYS_INFINIT;

        for (MoveNode movimiento : movimientos) {
            if (System.currentTimeMillis() >= timeoutLimit) {
                timeoutTriggered = true;
                throw new TimeoutException("Timeout alcanzado durante la búsqueda.");
            }

            Point punto = movimiento.getPoint();
            HexGameStatus estadoAux = new HexGameStatus(s);
            estadoAux.placeStone(punto);

            long newHash = ZobristHashing.updateHash(hash, punto, s.getPos(punto.x, punto.y), _colorPlayer);

            int valor = MIN(estadoAux, profundidad - 1, MENYS_INFINIT, INFINIT, newHash);

            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }
        return mejorMovimiento;
    }

    private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, long hash) throws TimeoutException {
        if (System.currentTimeMillis() >= timeoutLimit) {
            timeoutTriggered = true;
            throw new TimeoutException("Timeout alcanzado en MIN.");
        }

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
            }
            if (alfa >= beta) return entry.value;
        }

        if (profundidad == 0 || estado.isGameOver()) {
            int heuristica = heuristica(estado, _colorPlayer, 0);
            transpositionTable.store(hash, profundidad, heuristica, TranspositionTable.EXACT, null);
            return heuristica;
        }

        Point mejorMovimiento = null;
        int mejorValor = INFINIT;
        for (int x = 0; x < estado.getSize(); x++) {
            for (int y = 0; y < estado.getSize(); y++) {
                if (estado.getPos(x, y) == 0) {
                    HexGameStatus estadoAux = new HexGameStatus(estado);
                    Point punto = new Point(x, y);
                    estadoAux.placeStone(punto);

                    long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(x, y), estado.getCurrentPlayerColor());

                    int valor = MAX(estadoAux, profundidad - 1, alfa, beta, newHash);
                    if(valor < mejorValor){
                        mejorValor = valor;
                        mejorMovimiento = punto;
                    }

                    beta = Math.min(beta, mejorValor);
                    if (beta <= alfa) {
                        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.UPPER_BOUND, mejorMovimiento);
                        return mejorValor;
                    }
                }
            }
        }
        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.EXACT, mejorMovimiento);
        return mejorValor;
    }

    private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, long hash) throws TimeoutException {
        if (System.currentTimeMillis() >= timeoutLimit) {
            timeoutTriggered = true;
            throw new TimeoutException("Timeout alcanzado en MAX.");
        }

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
            }
            if (alfa >= beta) return entry.value;
        }

        if (profundidad == 0 || estado.isGameOver()) {
            int heuristica = heuristica(estado, _colorPlayer, 0);
            transpositionTable.store(hash, profundidad, heuristica, TranspositionTable.EXACT, null);
            return heuristica;
        }

        Point mejorMovimiento = null;
        int mejorValor = MENYS_INFINIT;
        for (int x = 0; x < estado.getSize(); x++) {
            for (int y = 0; y < estado.getSize(); y++) {
                if (estado.getPos(x, y) == 0) {
                    HexGameStatus estadoAux = new HexGameStatus(estado);
                    Point punto = new Point(x, y);
                    estadoAux.placeStone(punto);

                    long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(x, y), estado.getCurrentPlayerColor());

                    int valor = MIN(estadoAux, profundidad - 1, alfa, beta, newHash);
                    if(valor > mejorValor){
                        mejorValor = valor;
                        mejorMovimiento = punto;
                    }

                    alfa = Math.max(alfa, mejorValor);
                    if (alfa >= beta) {
                        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.LOWER_BOUND, mejorMovimiento);
                        return mejorValor;
                    }
                }
            }
        }
        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.EXACT, mejorMovimiento);
        return mejorValor;
    }

    private List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
        List<MoveNode> movimientos = estado.getMoves();

        movimientos.sort((a, b) -> {
            HexGameStatus estadoA = new HexGameStatus(estado);
            HexGameStatus estadoB = new HexGameStatus(estado);

            estadoA.placeStone(a.getPoint());
            estadoB.placeStone(b.getPoint());

            int valorA = heuristica(estadoA, _colorPlayer, 0);
            int valorB = heuristica(estadoB, _colorPlayer, 0);

            return Integer.compare(valorB, valorA); // Ordenar de mayor a menor
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
        timeoutTriggered = true;
    }
}


class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }
}
