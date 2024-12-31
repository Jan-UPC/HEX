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

/**
 * Classe que implementa un jugador utilitzant l'algoritme Minimax amb millores com taules de transposició i heurístiques personalitzades.
 */
public class PlayerMinimaxHexCalculators implements IPlayer, IAuto {
    /** 
     * Valor infinit utilitzat per representar el millor resultat possible en l'algoritme Minimax.
     */
    private final int INFINIT = Integer.MAX_VALUE;
    /** 
     * Valor menys infinit utilitzat per representar el pitjor resultat possible en l'algoritme Minimax.
     */
    private final int MENYS_INFINIT = Integer.MIN_VALUE;
    /**
     * Nom del jugador.
     */
    private String _name;
    /**
     * El tipus del jugador actual (PLAYER1 o PLAYER2).
     */
    private PlayerType _Player;
    /**
     * Temps total acumulat en mil·lisegons per a calcular tots els moviments realitzats pel jugador.
     */
    private int _totalTime;
    /**
     * Color assignat al jugador.
     */
    int _colorPlayer;
    /**
     * Nombre total de moviments realitzats pel jugador fins al moment.
     */
    private int _nMoves;
    /**
     * Hash inicial del tauler buit, per poder detectar quan comença una partida continuada.
     */
    private long _hashTableroVacio;
    /**
     * Nombre de nodes explorats durant l'execució de l'algoritme Minimax per al moviment actual.
     */
    int _nNodes;
    /**
     * Profunditat màxima permesa en la recerca amb l'algoritme Minimax.
     */
    private int _profMax;
    /**
     * Algoritme de Dijkstra utilitzat per calcular camins més curts i heurístiques.
     */
    private Dijkstra _dijkstra;
    /**
     * Profunditat explorada durant la recerca actual amb l'algoritme Minimax.
     */
    int _profExpl;
    /**
     * Taula de transposició utilitzada per guardar i reutilitzar resultats d'estats ja explorats.
     */
    private TranspositionTable transpositionTable;

    /**
    * Constructor de la classe PlayerMinimaxHexCalculators.
    * Inicialitza el jugador amb un nom, una profunditat màxima per a l'algoritme Minimax i la mida del tauler.
    *
    * @param name Nom del jugador.
    * @param profunditatMaxima Profunditat màxima per a la recerca amb Minimax.
    * @param boardSize Mida del tauler.
    */
    public PlayerMinimaxHexCalculators(String name, int profunditatMaxima, int boardSize) {
        init(name, profunditatMaxima, boardSize);
    }
    
    /**
    * Mètode d'inicialització que configura les variables necessàries per al funcionament del jugador.
    *
    * @param name Nom del jugador.
    * @param profunditatMaxima Profunditat màxima per a la recerca amb Minimax.
    * @param boardSize Mida del tauler.
    */
    private void init(String name, int profunditatMaxima, int boardSize){
        //System.out.println("==========================================");
        // Assignació del nom del jugador
        this._name = name;

        // Assignació de la profunditat màxima per a l'algoritme Minimax
        this._profMax = profunditatMaxima;

        // Inicialització de variables per al recompte i estadístiques
        this._profExpl = 0;
        this._nMoves = 0;
        this._nNodes = 0;
        this._totalTime = 0;

        // Inicialització de l'algoritme de Dijkstra
        this._dijkstra = new Dijkstra();

        // Inicialització de la taula de transposició per optimitzar càlculs
        this.transpositionTable = new TranspositionTable();

        // Creació d'un estat inicial buit del tauler
        HexGameStatus s = new HexGameStatus(boardSize);

        // Configuració del hashing Zobrist segons la mida del tauler
        ZobristHashing.setBoardSize(boardSize);

        // Càlcul del hash inicial del tauler buit
        this._hashTableroVacio = ZobristHashing.calculateHash(s);
    }

    /**
    * Realitza el moviment del jugador utilitzant l'algoritme Minimax amb poda alfa-beta.
    * Aquest mètode és cridat per cada torn del joc i selecciona el millor moviment segons les heurístiques i estratègies.
    *
    * @param s Estat actual del tauler del joc.
    * @return PlayerMove amb la millor jugada calculada.
    */
    @Override
    public PlayerMove move(HexGameStatus s) {
        // Inicialitza el comptador de nodes explorats i augmenta el número de moviments jugats
        _nNodes = 0;
        _nMoves++;
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();
        long initialTime = System.currentTimeMillis();
        
        
        // Calcula el hash inicial de l'estat del tauler
        long hash = ZobristHashing.calculateHash(s);
        
        // Comprova si el tauler ha estat reiniciat (nova partida)
        if(_hashTableroVacio==hash && _nMoves!=1){
            /*double estadistica = (double)_totalTime/_nMoves;
            System.out.println("Tiempo total del juego en ms: " + _totalTime);
            System.out.println("Numero total de movimientos: " + _nMoves);
            System.out.println("Estadistica ms/moves: " + estadistica);*/
            init(_name, _profMax, s.getSize());
            _nMoves++;
        }
             
        // Ordena els moviments segons la heurística apropiada
        List<MoveNode> movimientos;
        if(_nMoves < 3){
            movimientos = ordenarMovimientosRapido(s); // Utilitza una heurística ràpida al començament
        } else {
            movimientos = ordenarMovimientos(s); // Utilitza una heurística més precisa després
        }
        
        // Determina el número màxim de moviments a avaluar
        int numMovimientosEvaluar = Math.min(movimientos.size(), (150/_profMax));
        Point mejorMovimiento = movimientos.get(movimientos.size()/2).getPoint();
        int mejorValor = MENYS_INFINIT;
        int profExpl = 0;

        // Itera pels moviments ordenats per trobar el millor
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            _nNodes++;
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();
            
            // Simula el moviment al tauler auxiliar
            HexGameStatus estadoAux = new HexGameStatus(s);
            long newHash = ZobristHashing.updateHash(hash, punto, s.getPos(punto.x, punto.y), _colorPlayer);
            estadoAux.placeStone(punto);
            
            // Comprova si el moviment actual és guanyador
            if (estadoAux.isGameOver() && estadoAux.GetWinner() == _Player) {
                long finalTime = System.currentTimeMillis();
                long realTime = finalTime - initialTime;
                _totalTime += realTime;
                /*double estadistica = (double)_totalTime/_nMoves;
                System.out.println("======== Min-Max =========");
                System.out.println("Tiempo total del movimiento en ms: " + realTime);
                System.out.println("Tiempo total del juego en ms: " + _totalTime);
                System.out.println("Numero total de movimientos: " + _nMoves);
                System.out.println("Estadistica ms/moves: " + estadistica);*/
                return new PlayerMove(punto, _nNodes, _profExpl, SearchType.MINIMAX);
            }
            
            // Avalua el moviment amb l'algoritme Minimax
            int valor = MIN(estadoAux, _profMax - 1, profExpl+1, MENYS_INFINIT, INFINIT, newHash);
            if (valor > mejorValor) {
                mejorValor = valor;
                mejorMovimiento = punto;
            }
        }

        // Desa el millor resultat a la taula de transposició
        transpositionTable.store(hash, profExpl, mejorValor, TranspositionTable.beta, mejorMovimiento);
        long finalTime = System.currentTimeMillis();
        long realTime = finalTime - initialTime;
        _totalTime += realTime;
        /*double estadistica = (double)_totalTime/_nMoves;
                System.out.println("======== Min-Max =========");
                System.out.println("Tiempo total del movimiento en ms: " + realTime);
                System.out.println("Tiempo total del juego en ms: " + _totalTime);
                System.out.println("Numero total de movimientos: " + _nMoves);
                System.out.println("Estadistica ms/moves: " + estadistica);*/
        return new PlayerMove(mejorMovimiento, _nNodes, _profExpl, SearchType.MINIMAX);
    }

    /**
    * Mètode MIN de l'algoritme Minimax amb poda alfa-beta.
    * Avalua el moviment del jugador oponent per trobar el valor mínim possible per al jugador actual.
    *
    * @param estado L'estat actual del tauler del joc.
    * @param profundidad La profunditat actual del Minimax.
    * @param nivelesExplorados Nombre de nivells explorats fins ara.
    * @param alfa El valor actual d'alfa (la millor opció garantida per al jugador Max).
    * @param beta El valor actual de beta (la millor opció garantida per al jugador Min).
    * @param hash El hash Zobrist de l'estat actual del tauler.
    * @return El valor mínim calculat en aquest ramal de l'arbre Minimax.
    */
    private int MIN(HexGameStatus estado, int profundidad, int nivelesExplorados,int alfa, int beta, long hash) {  
        _nNodes++; // Incrementa el nombre de nodes explorats  
        
        // Si el joc ha acabat i el jugador actual és el guanyador
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        }
        // Consulta a la taula de transposició
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
        
        // Ordena els moviments per heurística ràpida 
        List<MoveNode> movimientos;
        movimientos = ordenarMovimientosRapido(estado);
        int numMovimientosEvaluar = Math.min(movimientos.size(), (150/_profMax)); // Limita el nombre de moviments a avaluar
        
        // Itera pels moviments seleccionats
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            // Crea un nou estat per al moviment actual
            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            // Avalua el valor amb MAX
            int valor = MAX(estadoAux, profundidad - 1, nivelesExplorados + 1, alfa, beta, newHash);
            mejorValor = Math.min(mejorValor, valor);
            beta = Math.min(beta, mejorValor);
            
            // Si beta és menor o igual a alfa, poda
            if (beta <= alfa) {
                transpositionTable.store(hash, profundidad, beta, TranspositionTable.beta, mejorPunto);
                return mejorValor; // Poda
            }
        }
        return mejorValor; // Retorna el millor valor trobat
    }

    /**
    * Mètode MAX de l'algoritme Minimax amb poda alfa-beta.
    * Avalua el moviment del jugador actual per trobar el valor màxim possible.
    *
    * @param estado L'estat actual del tauler del joc.
    * @param profundidad La profunditat actual del Minimax.
    * @param nivelesExplorados Nombre de nivells explorats fins ara.
    * @param alfa El valor actual d'alfa (la millor opció garantida per al jugador Max).
    * @param beta El valor actual de beta (la millor opció garantida per al jugador Min).
    * @param hash El hash Zobrist de l'estat actual del tauler.
    * @return El valor màxim calculat en aquest ramal de l'arbre Minimax.
    */
    private int MAX(HexGameStatus estado, int profundidad, int nivelesExplorados, int alfa, int beta, long hash){
        _nNodes++; // Incrementa el nombre de nodes explorats
        
        // Si el joc ha acabat i el jugador actual és el guanyador
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        } 
        
        // Consulta a la taula de transposició
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
        
        // Si hem arribat a la profunditat màxima, calcula la heurística
        if (profundidad == 0){
            _profExpl = nivelesExplorados;
            return heuristica(estado, _colorPlayer);
        }
        
        int mejorValor = MENYS_INFINIT;
        
        // Ordenar moviments per heurística ràpida
        List<MoveNode> movimientos;
        movimientos = ordenarMovimientosRapido(estado);
        int numMovimientosEvaluar = Math.min(movimientos.size(), (150/_profMax)); // Limita el nombre de moviments a avaluar
        
        // Itera pels moviments seleccionats
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            // Crea un nou estat per al moviment actual
            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            // Avalua el valor amb MIN
            int valor = MIN(estadoAux, profundidad - 1, nivelesExplorados + 1, alfa, beta, newHash);
            mejorValor = Math.max(mejorValor, valor);
            alfa = Math.max(alfa, mejorValor);
            
            // Si beta és menor o igual a alfa, poda
            if (beta <= alfa) {
                transpositionTable.store(hash, profundidad, alfa, TranspositionTable.alfa, mejorPunto);
                return mejorValor; // Poda
            }
        }
        return mejorValor; // Retorna el millor valor trobat
    }

    /**
    * Ordena els moviments disponibles en funció d'una heurística calculada per cada moviment.
    *
    * @param estado L'estat actual del tauler de joc.
    * @return Una llista de moviments ordenats segons la seva puntuació heurística, de major a menor.
    */
    public List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
       // Obtenir la llista inicial de moviments disponibles
       List<MoveNode> movimientos = estado.getMoves();

       // Ordenar la llista de moviments segons la seva puntuació heurística
       movimientos.sort((a, b) -> {
           // Crear còpies independents de l'estat per simular moviments
           HexGameStatus estadoA = new HexGameStatus(estado);
           HexGameStatus estadoB = new HexGameStatus(estado);

           // Aplicar els moviments simulats als estats temporals
           estadoA.placeStone(a.getPoint());
           estadoB.placeStone(b.getPoint());
           
           // Calcular les heurístiques per als estats resultants
           int valorA = heuristica(estadoA, _colorPlayer);
           int valorB = heuristica(estadoB, _colorPlayer);
           
           // Comparar els valors heurístics per determinar l'ordre (de major a menor)
           return Integer.compare(valorB, valorA); 
       });
       
       // Retornar la llista ordenada de moviments
       return movimientos;
    }

    /**
    * Calcula una heurística per avaluar l'estat del joc des de la perspectiva d'un jugador específic.
    *
    * @param estado L'estat actual del tauler de joc.
    * @param color El color del jugador que avaluarà l'estat (1 o -1).
    * @return Un valor heurístic que representa la qualitat de l'estat per al jugador especificat.
    *         Els valors més alts són favorables; els valors baixos són desfavorables.
    */
    public int heuristica(HexGameStatus estado, int color) {
        // Obtenir els resultats del Dijkstra per al jugador actual
        Dijkstra result = _dijkstra.shortestPathWithVirtualNodes(estado, color);

        // Variables que descriuen els camins rellevants
        int caminoPropio = result.shortestPath; // Camí més curt cap a la victòria del jugador
        int caminosViables = result.viablePathsCount; // Nombre de camins viables del jugador
        int caminoEnemigo = result.enemyShortestPath; // Camí més curt cap a la victòria de l'enemic
        int caminosViablesEnemigo = result.viableEnemyPathsCount; // Nombre de camins viables de l'enemic
         /* Debug opcional per analitzar les mètriques del Dijkstra
        System.out.println("Camino propio: " + caminoPropio +
                " Camino enemigo: " + caminoEnemigo +
                " Caminos viables: " + caminosViables +
                " Caminos viables enemigo: " + caminosViablesEnemigo);
        */
        // Verificar si el jugador ha guanyat
        if (caminoPropio == 0) return INFINIT;   // Victoria
        // Verificar si l'enemic ha guanyat
        if (caminoEnemigo == 0) return MENYS_INFINIT; // Derrota 

        // Càlcul de la puntuació heurística basada en diferents factors
        return (10 * (estado.getSize() - caminoPropio))  // Prioritzar camins curts cap a la victòria
             + (3 * caminosViables)                      // Valorar la quantitat de camins viables propis
             - (7 * (estado.getSize() - caminoEnemigo))  // Penalitzar camins curts de l'enemic
             - (3 * caminosViablesEnemigo);              // Penalitzar la quantitat de camins viables de l'enemic
        //return 3*(caminoEnemigo - caminoPropio) + (caminosViables - caminosViablesEnemigo);
    }

    /**
    * Ordena els moviments disponibles de manera ràpida segons una heurística simplificada.
    *
    * Aquesta funció utilitza una heurística ràpida per ordenar els moviments segons la seva qualitat
    * estimada sense necessitat de simular el moviment complet. Això redueix el cost computacional
    * en comparació amb la heurística completa.
    *
    * @param estado L'estat actual del joc.
    * @return Una llista de moviments ordenats de millor a pitjor segons la heurística ràpida.
    */
    public List<MoveNode> ordenarMovimientosRapido(HexGameStatus estado) {
        // Obtenir la llista de moviments possibles
        List<MoveNode> movimientos = estado.getMoves();

        // Ordenar els moviments en funció de la heurística ràpida
        movimientos.sort((a, b) -> {
            // Calcular la heurística ràpida per a cada moviment
            int valorA = heuristicaRapida(estado, a.getPoint());
            int valorB = heuristicaRapida(estado, b.getPoint());
            
            // Comparar els valors heurístics per ordenar de major a menor
            return Integer.compare(valorB, valorA);
        });

        return movimientos; // Retornar els moviments ordenats
    }

    /**
    * Calcula una heurística ràpida per a una casella específica.
    *
    * Aquesta heurística es basa en la proximitat al centre del tauler i la qualitat dels seus veïns,
    * considerant caselles buides, caselles del mateix jugador i caselles de l'enemic.
    *
    * @param estado L'estat actual del joc.
    * @param punto La posició de la casella per a la qual es calcularà la heurística.
    * @return Un valor heurístic que mesura la qualitat de la casella. Valors més alts representen
    *         caselles més favorables.
    */
    public int heuristicaRapida(HexGameStatus estado, Point punto) {
        // Calcular la distància al centre del tauler (desavantatge per a caselles lluny del centre)
        int distanciaCentro = Math.abs(punto.x - estado.getSize() / 2) + Math.abs(punto.y - estado.getSize() / 2);
        // Avaluar els veïns de la casella
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
        
        // Retornar el valor heurístic combinant la distància al centre i la puntuació dels veïns
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

    /**
     * Classe que implementa la taula de transposició per a emmagatzemar i reutilitzar
     * informació de les posicions ja explorades durant el càlcul del Minimax.
     */
    class TranspositionTable {
        // Constants que representen el tipus de valor emmagatzemat
        static final int EXACT = 0; // Valor exacte per a una posició
        static final int alfa = 1;  // Cota inferior (alfa)
        static final int beta = 2;  // Cota superior (beta)

        // Estructura de dades per emmagatzemar les entrades de la taula
        private HashMap<Long, TableEntry> table = new HashMap<>();

        /**
         * Emmagatzema una entrada a la taula de transposició.
         *
         * @param hash     El valor hash únic que representa l'estat del joc.
         * @param depth    La profunditat a la qual es va calcular aquest valor.
         * @param value    El valor heurístic de la posició.
         * @param flag     El tipus de valor (EXACT, alfa, beta).
         * @param bestMove El millor moviment per a aquesta posició.
         */
        public void store(long hash, int depth, int value, int flag, Point bestMove) {
            table.put(hash, new TableEntry(value, depth, flag, bestMove));
        }

        /**
         * Busca una entrada a la taula de transposició.
         *
         * @param hash El valor hash de l'estat a buscar.
         * @return La entrada trobada o null si no hi ha cap.
         */
        public TableEntry lookup(long hash) {
            return table.get(hash);
        }

        /**
         * Classe interna que representa una entrada de la taula de transposició.
         */
        static class TableEntry {
            int value;    // Valor heurístic de la posició
            int depth;    // Profunditat de càlcul d'aquest valor
            int flag;     // EXACT, alfa o beta
            Point bestMove; // Millor moviment associat

            /**
             * Constructor d'una entrada de la taula de transposició.
             *
             * @param value    Valor heurístic.
             * @param depth    Profunditat.
             * @param flag     Tipus de valor (EXACT, alfa, beta).
             * @param bestMove Millor moviment.
             */
            public TableEntry(int value, int depth, int flag, Point bestMove) {
                this.value = value;
                this.depth = depth;
                this.flag = flag;
                this.bestMove = bestMove;
            }
        }
    }

    /**
     * Classe que implementa el hashing Zobrist per representar de manera eficient
     * els estats del joc amb valors hash únics.
     */
    class ZobristHashing {
        private static int boardSize = 11; // Mida del tauler per defecte
        private static long[][][] ZOBRIST_TABLE; // Taula Zobrist per als estats

        /**
         * Configura la mida del tauler i genera la taula Zobrist corresponent.
         *
         * @param newSize La nova mida del tauler.
         */
        public static void setBoardSize(int newSize) {
            boardSize = newSize;
            generateZobristTable();
        }

        /**
         * Genera la taula Zobrist per a la mida actual del tauler.
         */
        private static void generateZobristTable() {
            ZOBRIST_TABLE = new long[boardSize][boardSize][3]; // [0: buit, 1: jugador 1, 2: jugador 2]
            Random random = new Random();
            for (int x = 0; x < boardSize; x++) {
                for (int y = 0; y < boardSize; y++) {
                    for (int k = 0; k < 3; k++) {
                        ZOBRIST_TABLE[x][y][k] = random.nextLong();
                    }
                }
            }
        }

        /**
         * Calcula el valor hash d'un estat del joc.
         *
         * @param estado L'estat del joc.
         * @return El valor hash corresponent.
         */
        public static long calculateHash(HexGameStatus estado) {
            long hash = 0;
            for (int x = 0; x < estado.getSize(); x++) {
                for (int y = 0; y < estado.getSize(); y++) {
                    int piece = estado.getPos(x, y); // 0: buit, 1: jugador 1, -1: jugador 2
                    int index = (piece == 1) ? 1 : (piece == -1) ? 2 : 0;
                    hash ^= ZOBRIST_TABLE[x][y][index];
                }
            }
            return hash;
        }

        /**
         * Actualitza el valor hash després de realitzar un moviment.
         *
         * @param hash      El hash actual.
         * @param move      El moviment realitzat.
         * @param oldState  L'estat anterior de la casella (buit, jugador 1, jugador 2).
         * @param newState  El nou estat de la casella.
         * @return El valor hash actualitzat.
         */
        public static long updateHash(long hash, Point move, int oldState, int newState) {
            int oldIndex = (oldState == 1) ? 1 : (oldState == -1) ? 2 : 0;
            int newIndex = (newState == 1) ? 1 : (newState == -1) ? 2 : 0;
            return hash ^ ZOBRIST_TABLE[move.x][move.y][oldIndex] ^ ZOBRIST_TABLE[move.x][move.y][newIndex];
        }
    }

