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

/**
 * Classe que implementa un jugador amb Iterative-Deepening (ID) i mecanisme
 * per gestionar el timeout. 
 */
public class PlayerIDHexCalculators implements IPlayer, IAuto {
    // Constants per definir els valors límit
    private final int INFINIT = Integer.MAX_VALUE; // Valor màxim (victòria segura)
    private final int MENYS_INFINIT = Integer.MIN_VALUE; // Valor mínim (derrota segura)

    // Variables de configuració
    private String _name; // Nom del jugador
    private PlayerType _Player; // Tipus de jugador actual (PLAYER1 o PLAYER2)
    private int _colorPlayer; // Color del jugador 
    private int _profTotal; // Profunditat total explorada
    private int _profActual; //Profunditat actual
    private int _nMoves; // Nombre de moviments realitzats
    private int _timeout; // Temps límit per al timeout
    private long _hashTableroVacio; // Hash del tauler buit
    private int _totalTime; //Temps total acumulat en mil·lisegons per a calcular tots els moviments realitzats pel jugador.
    private int _nNodes; // Nombre de nodes explorats en la cerca actual
    private Dijkstra _dijkstra; // Instància del càlcul de camins més curts
    private long timeoutLimit; // Temps límit calculat per al timeout
    private boolean timeoutTriggered; // Indicador si el timeout ha estat activat
    private TranspositionTable transpositionTable; // Taula de transposició per millorar la cerca
    private int profundidadMaxima; // Guardem la profunditat maxima arribada

    /**
     * Constructor per inicialitzar el jugador amb un nom, mida de tauler i límit de temps.
     *
     * @param name      El nom del jugador.
     * @param boardSize La mida del tauler.
     * @param timeout   El límit de temps (en segons).
     */
    public PlayerIDHexCalculators(String name, int boardSize, int timeout) {
        init(name, boardSize, timeout);
    }

    /**
     * Inicialitza les variables del jugador.
     *
     * @param name      El nom del jugador.
     * @param boardSize La mida del tauler.
     * @param timeout   El límit de temps (en segons).
     */
    private void init(String name, int boardSize, int timeout) {
        //System.out.println("==========================================");
        this._name = name;
        this._profTotal = 0; 
        this._timeout = timeout*1000;
        this._nMoves = 0;
        this.profundidadMaxima = 0;
        this._dijkstra = new Dijkstra();
        this.transpositionTable = new TranspositionTable();
        HexGameStatus s = new HexGameStatus(boardSize);
        ZobristHashing.setBoardSize(boardSize);
        this._hashTableroVacio = ZobristHashing.calculateHash(s);
    }
    
    /**
    * Executa el moviment del jugador utilitzant Iterative Deepening (ID) amb
    * control de timeout.
    *
    * @param s L'estat actual del tauler.
    * @return PlayerMove El millor moviment calculat pel jugador.
    */
    @Override
    public PlayerMove move(HexGameStatus s) {
        // Incrementar el nombre de moviments i reiniciar els nodes explorats
        _nMoves++;
        _nNodes = 0;
        _profActual = 1;
        _Player = s.getCurrentPlayer();
        _colorPlayer = s.getCurrentPlayerColor();

        // Configurar timeout
        long initialTime = System.currentTimeMillis();
        timeoutTriggered = false;
        timeoutLimit = System.currentTimeMillis() + _timeout; 

        Point mejorMovimiento = null;

        // Calcular el hash inicial per l'estat actual del tauler
        long hash = ZobristHashing.calculateHash(s);
        if(_hashTableroVacio==hash && _nMoves!=1){
            /*System.out.println("======== IDS =========");
            double estadistica = (double)_profTotal/_nMoves;
            System.out.println("Profundidad conseguida con exito: " + _profTotal);
            System.out.println("Numero total de movimientos: " + _nMoves);
            System.out.println("Tiempo total del juego en ms: " + _totalTime);
            System.out.println("Estadistica ProfundidadTotal/Moves: " + estadistica);
            System.out.println("Profundidad maxima llegada: " + profundidadMaxima);*/

            // Reinicialitzar si es detecta un nou joc
            init(_name, s.getSize(), _timeout/1000);
            _nMoves++;
        }

        // Iterative Deepening
        int contadorRepetidas = 0; // Contador de elecciones iguales consecutivas
        for (_profActual = 1; !timeoutTriggered; _profActual++) {
            try {
                // Realitzar la cerca amb la profunditat actual
                Point movimientoActual = realizarBusqueda(s, hash, _profActual);
                if(movimientoActual.equals(mejorMovimiento)){
                    contadorRepetidas++;
                } else {
                    contadorRepetidas = 0;
                }
                if(contadorRepetidas==2){
                    timeoutTriggered = true;
                }
                mejorMovimiento = movimientoActual; // Actualitzar el millor moviment trobat
            } catch (TimeoutException e) {
                // Timeout detectat
                break;
            }
        }
        profundidadMaxima = Math.max(profundidadMaxima, _profActual-1);
        // Actualitzar les estadístiques de profunditat
        _profTotal += _profActual-1;
        long finalTime = System.currentTimeMillis();
        long realTime = finalTime - initialTime;
        _totalTime += realTime;     
        /*double estadistica = (double)_profTotal/_nMoves;
            System.out.println("======== IDS =========");
            System.out.println("Profundidad conseguida con exito: " + _profTotal);
            System.out.println("Numero total de movimientos: " + _nMoves);
            System.out.println("Tiempo total del juego en ms: " + _totalTime);
            System.out.println("Estadistica ProfundidadTotal/Moves: " + estadistica);*/
        // Retornar el millor moviment trobat
        return new PlayerMove(mejorMovimiento, _nNodes, _profActual, SearchType.MINIMAX_IDS);
    }

    /**
    * Realitza la cerca d'un moviment òptim a una profunditat determinada.
    *
    * @param s L'estat actual del tauler.
    * @param hash El hash corresponent a l'estat actual del tauler.
    * @param profundidad La profunditat màxima de la cerca.
    * @return Point El millor moviment trobat fins al moment.
    * @throws TimeoutException Si es detecta que el límit de temps s'ha superat.
    */
    private Point realizarBusqueda(HexGameStatus s, long hash, int profundidad) throws TimeoutException {
       // Determinar la llista de moviments a avaluar
       List<MoveNode> movimientos;
       if (_nMoves < 3) {
           movimientos = ordenarMovimientosRapido(s); // Heurística ràpida per als primers moviments
       } else {
           movimientos = ordenarMovimientos(s); // Heurística més completa per la resta
       }

       // Inicialitzar el millor moviment i el seu valor associat
       Point mejorMovimiento = movimientos.get(0).getPoint();
       int mejorValor = MENYS_INFINIT;

       // Limitar el nombre de moviments a avaluar
       int numMovimientosEvaluar = Math.min(movimientos.size(), (200/_profActual));

       // Avaluar cada moviment seleccionat
       for (int i = 0; i < numMovimientosEvaluar; i++) {
           MoveNode movimiento = movimientos.get(i);

           // Verificar si s'ha superat el límit de temps
           if (System.currentTimeMillis() >= timeoutLimit) {
               timeoutTriggered = true;
               throw new TimeoutException("S'ha assolit el límit de temps durant la cerca.");
           }

           // Incrementar el comptador de nodes explorats
           _nNodes++;

           // Generar un nou estat per al moviment actual
           Point punto = movimiento.getPoint();
           HexGameStatus estadoAux = new HexGameStatus(s);
           long newHash = ZobristHashing.updateHash(hash, punto, s.getPos(punto.x, punto.y), estadoAux.getCurrentPlayerColor());
           estadoAux.placeStone(punto);

           // Comprovar si el moviment actual guanya la partida
           if (estadoAux.isGameOver() && estadoAux.GetWinner() == _Player) {
               return punto;
           }

           // Avaluar el valor del moviment utilitzant la funció MIN
           int valor = MIN(estadoAux, profundidad - 1, MENYS_INFINIT, INFINIT, newHash);

           // Actualitzar el millor moviment si el valor és superior
           if (valor > mejorValor) {
               mejorValor = valor;
               mejorMovimiento = punto;
           }
           /*if(mejorValor == INFINIT){
               timeoutTriggered = true;
           }*/
       }

       // Retornar el millor moviment trobat fins al moment
       return mejorMovimiento;
    }


    /**
    * Implementa la funció MIN de l'algorisme Minimax amb poda Alfa-Beta.
    *
    * @param estado L'estat actual del tauler.
    * @param profundidad La profunditat actual de la recerca.
    * @param alfa El valor d'alfa (cota inferior) per a la poda.
    * @param beta El valor de beta (cota superior) per a la poda.
    * @param hash L'hash actual del tauler per optimització amb transposicions.
    * @return El valor heurístic mínim calculat en aquesta branca.
    * @throws TimeoutException Si s'ha assolit el límit de temps durant la recerca.
    */
    private int MIN(HexGameStatus estado, int profundidad, int alfa, int beta, long hash) throws TimeoutException {
       // Verifica si s'ha superat el límit de temps
       if (System.currentTimeMillis() >= timeoutLimit) {
           timeoutTriggered = true;
           throw new TimeoutException("S'ha assolit el límit de temps en MIN.");
       }

       // Incrementar el comptador de nodes explorats
       _nNodes++;

       // Comprovar si l'estat actual és terminal (victòria o derrota)
       if (estado.isGameOver() && estado.GetWinner() == _Player) {
           return INFINIT;
       }

       // Consultar la taula de transposició
       TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
       Point mejorPunto = null;
       if (entry != null) {
           switch (entry.flag) {
               case TranspositionTable.alfa:
                   if (entry.value > alfa) {
                       alfa = entry.value;
                       mejorPunto = entry.bestMove;
                   }
                   break;
               case TranspositionTable.beta:
                   if (entry.value < beta) {
                       beta = entry.value;
                       mejorPunto = entry.bestMove;
                   }
                   break;
               default:
                   break;
           }

           // Poda si alfa és més gran o igual que beta
           if (alfa >= beta) {
               return entry.value;
           }
       }

       // Si s'ha arribat a la profunditat màxima o a un estat terminal
       if (profundidad == 0 || estado.isGameOver()) {
           return heuristica(estado, _colorPlayer); // Calcula la heurística
       }

       int mejorValor = INFINIT;

       // Ordenar moviments basant-se en una heurística
       List<MoveNode> movimientos = ordenarMovimientosRapido(estado);
       int numMovimientosEvaluar = Math.min(movimientos.size(), 200/_profActual);

       // Explorar cada moviment ordenat
       for (int i = 0; i < numMovimientosEvaluar; i++) {
           MoveNode movimiento = movimientos.get(i);
           Point punto = movimiento.getPoint();

           // Crear un nou estat per al moviment actual
           HexGameStatus estadoAux = new HexGameStatus(estado);
           long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
           estadoAux.placeStone(punto);

           // Calcular el valor de MAX per al moviment actual
           int valor = MAX(estadoAux, profundidad - 1, alfa, beta, newHash);
           mejorValor = Math.min(mejorValor, valor);
           beta = Math.min(beta, mejorValor);

           // Poda si beta és menor o igual que alfa
           if (beta <= alfa) {
               transpositionTable.store(hash, profundidad, beta, TranspositionTable.beta, mejorPunto);
               return mejorValor; // Retorna immediatament
           }
       }

       // Retorna el millor valor trobat
       return mejorValor;
    }

    /**
     * Implementa la funció MAX de l'algorisme Minimax amb poda Alfa-Beta.
     *
     * @param estado L'estat actual del tauler.
     * @param profundidad La profunditat actual de la recerca.
     * @param alfa El valor d'alfa (cota inferior) per a la poda.
     * @param beta El valor de beta (cota superior) per a la poda.
     * @param hash L'hash actual del tauler per optimització amb transposicions.
     * @return El valor heurístic màxim calculat en aquesta branca.
     * @throws TimeoutException Si s'ha assolit el límit de temps durant la recerca.
     */
    private int MAX(HexGameStatus estado, int profundidad, int alfa, int beta, long hash) throws TimeoutException {
        // Comprovar si s'ha superat el límit de temps
        if (System.currentTimeMillis() >= timeoutLimit) {
            timeoutTriggered = true;
            throw new TimeoutException("S'ha assolit el límit de temps en MAX.");
        }

        // Incrementar el comptador de nodes explorats
        _nNodes++;

        // Comprovar si l'estat actual és terminal (victòria o derrota)
        if (estado.isGameOver() && estado.GetWinner() == _Player) {
            return INFINIT;
        }

        // Consultar la taula de transposició
        TranspositionTable.TableEntry entry = transpositionTable.lookup(hash);
        Point mejorPunto = null;
        if (entry != null) {
            switch (entry.flag) {
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

            // Poda si alfa és més gran o igual que beta
            if (alfa >= beta) {
                return entry.value;
            }
        }

        // Si s'ha arribat a la profunditat màxima o a un estat terminal
        if (profundidad == 0 || estado.isGameOver()) {
            return heuristica(estado, _colorPlayer); // Calcula la heurística
        }

        int mejorValor = MENYS_INFINIT;

        // Ordenar moviments basant-se en una heurística
        List<MoveNode> movimientos = ordenarMovimientosRapido(estado);
        int numMovimientosEvaluar = Math.min(movimientos.size(), 200/_profActual);

        // Explorar cada moviment ordenat
        for (int i = 0; i < numMovimientosEvaluar; i++) {
            MoveNode movimiento = movimientos.get(i);
            Point punto = movimiento.getPoint();

            // Crear un nou estat per al moviment actual
            HexGameStatus estadoAux = new HexGameStatus(estado);
            long newHash = ZobristHashing.updateHash(hash, punto, estado.getPos(punto.x, punto.y), estado.getCurrentPlayerColor());
            estadoAux.placeStone(punto);

            // Calcular el valor de MIN per al moviment actual
            int valor = MIN(estadoAux, profundidad - 1, alfa, beta, newHash);
            mejorValor = Math.max(mejorValor, valor);
            alfa = Math.max(alfa, mejorValor);

            // Poda si beta és menor o igual que alfa
            if (beta <= alfa) {
                transpositionTable.store(hash, profundidad, alfa, TranspositionTable.alfa, mejorPunto);
                return mejorValor; // Retorna immediatament
            }
        }

        // Retorna el millor valor trobat
        return mejorValor;
    }


    /**
    * Ordena els moviments disponibles en funció d'una heurística calculada per cada moviment.
    *
    * @param estado L'estat actual del tauler de joc.
    * @return Una llista de moviments ordenats segons la seva puntuació heurística, de major a menor.
    */
    private List<MoveNode> ordenarMovimientos(HexGameStatus estado) {
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
        if (caminoPropio == 0) return INFINIT;
        // Verificar si l'enemic ha guanyat
        if (caminoEnemigo == 0) return MENYS_INFINIT;

        // Càlcul de la puntuació heurística basada en diferents factors
        return (10 * (estado.getSize() - caminoPropio)) 
             + (3 * caminosViables) 
             - (7 * (estado.getSize() - caminoEnemigo))
             - (3 * caminosViablesEnemigo);
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
        timeoutTriggered = true;
    }
}

class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }
}
