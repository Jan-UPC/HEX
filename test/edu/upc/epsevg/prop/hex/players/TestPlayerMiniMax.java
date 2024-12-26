package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.PlayerType;
import java.awt.Point;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestPlayerMiniMax {

    @Test
    public void testDijkstra_SimplePath_Player1() {
        byte[][] board = {
            { 0,  0,  -1,  0,  0},
              { 0,  1,  0,  0,  0},
                { 1,  0,  0,  0,  0},
                  { 0,  0,  1,  0,  1},
                    { 1,  0,  0,  0,  0}
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        Dijkstra dijkstra = new Dijkstra();

        Dijkstra distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
        System.out.println("Distancia calculada para PLAYER1: " + distance.shortestPath);
        assertEquals(2, distance.shortestPath);
    }


    @Test
    public void testDijkstra_SimplePath_Player2() {
        byte[][] board = {
            { 1,  0,  0,  0,  0},
            { 0,  1,  0,  0,  0},
            { 0,  0,  1,  0,  0},
            { 0,  0,  0,  1,  0},
            { 0,  0,  0,  0,  1}
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        Dijkstra dijkstra = new Dijkstra();

        Dijkstra distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
        System.out.println("Resultado testDijkstra_SimplePath_Player2: " + distance.shortestPath);
        assertEquals(4, distance.shortestPath);
    }

    @Test
    public void testHeuristicaSimple() {
        byte[][] board = {
            { 1, 0, -1, 0, 0 },
              { 0, 0, 0, 0, 0 },
                { 1, 0, 0, 0, 0 },
                  { 0, 0, 0, 0, 0 },
                    { 0, 0, 0, 0, 0 }
        };
        byte[][] board2 = {
            { 0, 0, -1, 0, 0 },
              { 0, 0, 0, 0, 0 },
                { 1, 1, 0, 0, 0 },
                  { 0, 0, 0, 0, 0 },
                    { 0, 0, 0, 0, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        HexGameStatus gs2 = new HexGameStatus(board2, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, 5);

        int heuristica = player.heuristica(gs, 1, 0);
        int heuristica2 = player.heuristica(gs2, 1, 0);
        System.out.println("testHeuristicaSimple: Heurisitca con valor: " + heuristica);
        System.out.println("testHeuristicaSimple: Heurisitca2 con valor: " + heuristica2);
        assertTrue(heuristica2 > heuristica); // Ventaja para el jugador 1
    }
    
    @Test
    public void testHeuristicaSimple2() {
        byte[][] board = {
            { 0, 0, 0, 0, 0 },
              { 0, -1, 0, 0, 0 },
                { 0, -1, 0, 0, 0 },
                  { 0, -1, 0, 0, 0 },
                    { 1, 1, 1, 1, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, 5);

        int heuristica = player.heuristica(gs, 1, 0);
        System.out.println("testHeuristicaSimple2: " + heuristica);
        assertTrue(heuristica > 0); // Ventaja para el jugador 1
    }
    
    @Test
    public void testDijkstra1() {
        byte[][] board = {
            { 1, 0, 0, -1, 0 },
              { 1, 0, 0, -1, 0 },
                { 0, 0, 0, 0, 0 },
                  { 0, 0, 0, 0, 0 },
                    { 0, 0, 0, 0, 0 }
        };

       HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER2);
       Dijkstra dijkstra = new Dijkstra();

       Dijkstra distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
       PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, 5);

       int heuristica = player.heuristica(gs, 1, 0);
       System.out.println("testDijkstra1: " + distance.shortestPath);
       assertEquals(5, distance.shortestPath);
    }
    
    @Test
    public void testDijkstra_Bugeado_Player1() {
        byte[][] board = {
            { 1,  1,  1,  1,  1},
              { 0,  0,  0,  0,  0},
                { 0,  0,  0,  0,  0},
                  { 0,  0,  0,  0,  0},
                    { 0,  0,  0,  0,  0}
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        Dijkstra dijkstra = new Dijkstra();

        Dijkstra distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
        System.out.println("testDijkstra_Bugeado_Player1: " + distance.shortestPath);
        assertEquals(0, distance.shortestPath);
    }
    
    @Test
    public void testZobristHashing() {
        ZobristHashing.setBoardSize(5); // Tamaño del tablero
        HexGameStatus estado = new HexGameStatus(5); // Crear un tablero vacío
        HexGameStatus estadoAux = new HexGameStatus(5);
        // Cálculo inicial
        long hashInicial = ZobristHashing.calculateHash(estado);
        System.out.println("Hash inicial: " + hashInicial);

        // Hacer un movimiento y actualizar el hash
        Point move = new Point(2, 3);
        estado.placeStone(move); // Coloca una piedra del jugador 1
        long hashActualizado = ZobristHashing.calculateHash(estado);
        System.out.println("Hash tras movimiento: " + hashActualizado);

        // Revertir el movimiento y recalcular el hash
        long hashRevertido = ZobristHashing.calculateHash(estadoAux);
        System.out.println("Hash tras revertir: " + hashRevertido);

        estadoAux.placeStone(move);
        long hashNuevo = ZobristHashing.calculateHash(estadoAux);
        System.out.println("Ultimo hash: " + hashNuevo);
        
        // Verificaciones
        assertNotEquals(hashInicial, hashActualizado);
        assertEquals(hashInicial, hashRevertido);
        assertEquals(hashNuevo, hashActualizado);
    }
    
    @Test
    public void testTranspositionTable() {
        TranspositionTable table = new TranspositionTable();

        long hash = 123456789L; // Hash simulado
        int depth = 3;
        int value = 42;
        int flag = TranspositionTable.EXACT;
        Point bestMove = new Point(2, 3);

        // Almacenar un nodo
        table.store(hash, depth, value, flag, bestMove);

        // Recuperar el nodo
        TranspositionTable.TableEntry entry = table.lookup(hash);
        assertNotNull(entry);
        assertEquals(value, entry.value);
        assertEquals(depth, entry.depth);
        assertEquals(flag, entry.flag);
        assertEquals(bestMove, entry.bestMove);

        // Verificar un hash inexistente
        assertNull(table.lookup(987654321L));
    }
    
    @Test
    public void testZobristAndTranspositionIntegration() {
        ZobristHashing.setBoardSize(5);
        TranspositionTable table = new TranspositionTable();
        HexGameStatus estado = new HexGameStatus(5); // Tablero de 5x5
        HexGameStatus estadoAux = new HexGameStatus(5);

        // Estado inicial
        long hashInicial = ZobristHashing.calculateHash(estado);

        // Guardar en la tabla
        int initialValue = 100;
        table.store(hashInicial, 3, initialValue, TranspositionTable.EXACT, null);

        // Verificar recuperación
        TranspositionTable.TableEntry entry = table.lookup(hashInicial);
        assertNotNull(entry);
        assertEquals(initialValue, entry.value);

        // Realizar un movimiento
        Point move = new Point(1, 1);
        estado.placeStone(move); // Jugador 1 coloca una piedra
        long hashPostMovimiento = ZobristHashing.calculateHash(estado);

        // Asegurar que el nuevo hash no esté en la tabla
        assertNull(table.lookup(hashPostMovimiento));

        // Revertir el movimiento
        long hashRevertido = ZobristHashing.calculateHash(estadoAux);

        // Asegurar que el hash revertido es igual al inicial
        assertEquals(hashInicial, hashRevertido);
    }

    @Test
    public void testMinimaxWithTransposition() {
        ZobristHashing.setBoardSize(5);
        TranspositionTable table = new TranspositionTable();
        HexGameStatus estado = new HexGameStatus(5);

        long hash = ZobristHashing.calculateHash(estado);
        int depth = 3;

        // Supongamos que ya evaluamos este estado
        table.store(hash, depth, 50, TranspositionTable.EXACT, null);

        // Simular Minimax
        if (table.lookup(hash) != null) {
            System.out.println("Usando valor almacenado en la tabla de transposición.");
        } else {
            System.out.println("Evaluando nodo.");
            // Guardar un nuevo valor tras evaluar
            table.store(hash, depth, 60, TranspositionTable.EXACT, null);
        }

        // Verificar que ahora el valor está en la tabla
        assertNotNull(table.lookup(hash));
    }
    
    @Test
    public void testTranspositionTableFlags() {
        TranspositionTable table = new TranspositionTable();
        long hash = 123456789L;

        table.store(hash, 3, 50, TranspositionTable.EXACT, null);
        TranspositionTable.TableEntry exactEntry = table.lookup(hash);
        assertEquals(TranspositionTable.EXACT, exactEntry.flag);

        table.store(hash, 3, 60, TranspositionTable.UPPER_BOUND, null);
        TranspositionTable.TableEntry upperBoundEntry = table.lookup(hash);
        assertEquals(TranspositionTable.UPPER_BOUND, upperBoundEntry.flag);

        table.store(hash, 3, 40, TranspositionTable.LOWER_BOUND, null);
        TranspositionTable.TableEntry lowerBoundEntry = table.lookup(hash);
        assertEquals(TranspositionTable.LOWER_BOUND, lowerBoundEntry.flag);
    }

    @Test
    public void testZobristHashing2() {
        HexGameStatus estado = new HexGameStatus(new byte[][] {
            { 0, 0, 0 },
              { 0, 1, 0 },
                { 0, 0, -1 }
        }, PlayerType.PLAYER1);

        long hash = ZobristHashing.calculateHash(estado);
        Point move = new Point(3, 4); // Ejemplo de movimiento
        long updatedHash = ZobristHashing.updateHash(hash, move, 0, 1); // Aplicar movimiento

        // Simula el retroceso
        long revertedHash = ZobristHashing.updateHash(updatedHash, move, 1, 0);

        assert hash == revertedHash : "El hash inicial y el revertido no coinciden";

        long recalculatedHash = ZobristHashing.calculateHash(estado);
        assert recalculatedHash == revertedHash : "El hash recalculado no coincide con el revertido";

    }

}
