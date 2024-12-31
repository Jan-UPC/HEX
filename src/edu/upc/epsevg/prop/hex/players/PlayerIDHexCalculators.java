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
    private int _profTotal;
    private int _nMoves;
    private int _timeout;
    int _nNodes;
    private Dijkstra _dijkstra;
    private long timeoutLimit;
    private boolean timeoutTriggered;
    private TranspositionTable transpositionTable;
    private int _nPodas;

    public PlayerIDHexCalculators(String name, int boardSize, int timeout) {
        this._name = name;
        this._profTotal = 0; 
        this._timeout = timeout*1000;
        this._nMoves = 0;
        this._dijkstra = new Dijkstra();
        this.transpositionTable = new TranspositionTable();
        ZobristHashing.setBoardSize(boardSize);
    }

    @Override
    public PlayerMove move(HexGameStatus s) {
        _nMoves++;
        _nNodes = 0;
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();

        // Configurar timeout
        timeoutTriggered = false;
        timeoutLimit = System.currentTimeMillis() + _timeout; 

        Point mejorMovimiento = null;

        // Hash inicial para el estado actual
        long hash = ZobristHashing.calculateHash(s);

        // Iterative Deepening
        int Depth = 0;
        for (int depth = 1; !timeoutTriggered; depth++) {
            try {
                Point movimientoActual = realizarBusqueda(s, hash, depth);
                mejorMovimiento = movimientoActual;
                Depth = depth;
            } catch (TimeoutException e) {
                break;
            }
        }
        _profTotal += Depth;

        double estadistica = _profTotal/_nMoves;
        System.out.println("Profundidad conseguida con exito: " + _profTotal);
        System.out.println("Numero total de movimientos: " + _nMoves);
        System.out.println("Estadistica ProfundidadTotal/Moves: " + estadistica);
        return new PlayerMove(mejorMovimiento, _nNodes, Depth, SearchType.MINIMAX_IDS);
    }

    private Point realizarBusqueda(HexGameStatus s, long hash, int profundidad) throws TimeoutException {
        List<MoveNode> movimientos = null;
        if(_nMoves < 2){
            movimientos = ordenarMovimientosRapido(s);
        } else {
            movimientos = ordenarMovimientos(s);
        }
        Point mejorMovimiento = movimientos.get(0).getPoint();
        int mejorValor = MENYS_INFINIT;
        //int numMovimientosEvaluar = Math.min(movimientos.size(), Math.max(20, (150/s.getSize())));
        int numMovimientosEvaluar = s.getSize();
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i); 
            if (System.currentTimeMillis() >= timeoutLimit) {
                timeoutTriggered = true;
                throw new TimeoutException("Timeout alcanzado durante la búsqueda.");
            }
            _nNodes++;
            Point punto = movimiento.getPoint();
            HexGameStatus estadoAux = new HexGameStatus(s);

            long newHash = ZobristHashing.updateHash(hash, punto, s.getPos(punto.x, punto.y), estadoAux.getCurrentPlayerColor());
            
            estadoAux.placeStone(punto);            

            if (s.isGameOver() && s.GetWinner() == _Player) {
                return punto;
            }
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
        _nNodes++;
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        }
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        Point mejorPunto = null;
        if (entry != null) {
            //System.out.println("Entrada encontrada " + entry.flag);
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    if(entry.depth >= profundidad){
                        return entry.value;
                    } else {
                        break;
                    }                    
                case TranspositionTable.alfa:
                    //System.out.println("Cogiendo alfa haseada");
                    if(entry.value > alfa){
                        alfa = entry.value;
                        mejorPunto = entry.bestMove;
                    }                    
                    break;
                case TranspositionTable.beta:
                    //System.out.println("Cogiendo beta haseada");
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

        if (profundidad == 0 || estado.isGameOver()) {
            int heuristica = heuristica(estado, _colorPlayer);
            transpositionTable.store(hash, profundidad, heuristica, TranspositionTable.EXACT, mejorPunto);
            return heuristica;
        }
        int mejorValor = INFINIT;
        // Ordenar movimientos por heurística 
        List<MoveNode> movimientos = ordenarMovimientosRapido(estado);
        //int numMovimientosEvaluar = Math.min(movimientos.size(), Math.max(20, (150/estado.getSize()))); // Solo los 20 mejores
        int numMovimientosEvaluar = estado.getSize();
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            int valor = MAX(estadoAux, profundidad - 1, alfa, beta, newHash);
            mejorValor = Math.min(mejorValor, valor);

            beta = Math.min(beta, mejorValor);
            if (beta <= alfa) {
                _nPodas++;
                transpositionTable.store(hash, profundidad, beta, TranspositionTable.beta, mejorPunto);
                return mejorValor; // Poda
            }
        }
        transpositionTable.store(hash, profundidad, beta, TranspositionTable.beta, mejorPunto);
        return mejorValor;
    }

    private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, long hash) throws TimeoutException {
        if (System.currentTimeMillis() >= timeoutLimit) {
            timeoutTriggered = true;
            throw new TimeoutException("Timeout alcanzado en MAX.");
        }
        _nNodes++;
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        } 
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        Point mejorPunto = null;
        if (entry != null) {
            //System.out.println("Entrada encontrada en MIN para hash: " + hash + ", valor: " + entry.value);
            switch (entry.flag) {
                case TranspositionTable.EXACT:
                    if(entry.depth >= profundidad){
                        return entry.value;
                    } else {
                        break;
                    }
                case TranspositionTable.alfa:
                    //System.out.println("Cogiendo alfa haseada");
                    alfa = Math.max(alfa, entry.value);
                    mejorPunto = entry.bestMove;
                    break;
                case TranspositionTable.beta:
                    //System.out.println("Cogiendo beta haseada");
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

        if (profundidad == 0 || estado.isGameOver()) {
            int heuristica = heuristica(estado, _colorPlayer);
            transpositionTable.store(hash, profundidad, heuristica, TranspositionTable.EXACT, mejorPunto);
            return heuristica;
        }

        int mejorValor = MENYS_INFINIT;
        // Ordenar movimientos por heurística
        List<MoveNode> movimientos = ordenarMovimientosRapido(estado);
        //int numMovimientosEvaluar = Math.min(movimientos.size(), Math.max(20, (150/estado.getSize()))); // Solo los 20 mejores
        int numMovimientosEvaluar = estado.getSize();
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            int valor = MIN(estadoAux, profundidad - 1, alfa, beta, newHash);
            mejorValor = Math.max(mejorValor, valor);

            alfa = Math.max(alfa, mejorValor);
            if (beta <= alfa) {
                _nPodas++;
                transpositionTable.store(hash, profundidad, alfa, TranspositionTable.alfa, mejorPunto);
                return mejorValor; // Poda
            }
        }
        transpositionTable.store(hash, profundidad, mejorValor, TranspositionTable.alfa, mejorPunto);
        return mejorValor;
    }

    private List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
        List<MoveNode> movimientos = estado.getMoves();

        movimientos.sort((a, b) -> {
            HexGameStatus estadoA = new HexGameStatus(estado);
            HexGameStatus estadoB = new HexGameStatus(estado);

            estadoA.placeStone(a.getPoint());
            estadoB.placeStone(b.getPoint());

            int valorA = heuristica(estadoA, _colorPlayer);
            int valorB = heuristica(estadoB, _colorPlayer);

            return Integer.compare(valorB, valorA); // Ordenar de mayor a menor
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
        if (caminoEnemigo == 0) return MENYS_INFINIT; // Derrota inminente

        // Ponderar la heurística considerando los caminos viables del enemigo
        return (10 * (estado.getSize() - caminoPropio)) 
             + (3 * caminosViables) 
             - (7 * (estado.getSize() - caminoEnemigo))
             - (3 * caminosViablesEnemigo);
        /*return (3*(caminoEnemigo - caminoPropio))
                + ((caminosViables - caminosViablesEnemigo));*/
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
        // Aproximación rápida basada en proximidad al centro y número de vecinos libres
        int distanciaCentro = Math.abs(punto.x - estado.getSize() / 2) + Math.abs(punto.y - estado.getSize() / 2);
        //int puntuacionVecinos = (int) estado.getNeigh(punto).stream().filter(p -> estado.getPos(p.x, p.y) == 0).count();
        // Evaluar vecinos
        int puntuacionVecinos = 0;
        for (Point vecino : estado.getNeigh(punto)) {
            int estadoVecino = estado.getPos(vecino.x, vecino.y);
            if (estadoVecino == 0) { // Vecino vacío
                puntuacionVecinos += 1;
            } else if (estadoVecino == _colorPlayer) { // Vecino nuestro
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
        timeoutTriggered = true;
    }
}

class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }
}
