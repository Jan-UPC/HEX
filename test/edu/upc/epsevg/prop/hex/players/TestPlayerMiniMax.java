package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.MoveNode;
import edu.upc.epsevg.prop.hex.PlayerType;
import java.awt.Point;
import java.util.List;
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
            { 0, 0, -1, 0, 0 },
              { 0, -1, 0, 0, 0 },
                { 1, -1, 0, 0, 0 },
                  { 1, 0, 0, 0, 0 },
                    { 0, 0, 0, 0, 0 }
        };
        byte[][] board2 = {
            { 0, -1, 0, 0, 0 },
              { 0, -1, 0, 0, 0 },
                { 1, -1, 0, 0, 0 },
                  { 0, 1, 0, 0, 0 },
                    { 0, 0, 0, 0, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        HexGameStatus gs2 = new HexGameStatus(board2, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, 5);

        int heuristica = player.heuristica(gs, 1);
        int heuristica2 = player.heuristica(gs2, 1);
        System.out.println("testHeuristicaSimple: Heurisitca con valor: " + heuristica);
        System.out.println("testHeuristicaSimple: Heurisitca2 con valor: " + heuristica2);
        assertTrue(heuristica2 > heuristica); // Ventaja para el jugador 1
    }
    
    @Test
    public void testHeuristicaSimple2() {
        byte[][] board = {
            { 0, 0, 0, 0, 0 },
              { 0, 0, -1, 0, 0 },
                { 0, 0, 0, 0, 0 },
                  { 0, 0, -1, 0, 0 },
                    { 1, 0, 1, 1, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, 5);

        int heuristica = player.heuristica(gs, 1);
        int heuristica2 = player.heuristica(gs, -1);
        System.out.println("testHeuristicaSimple2: " + heuristica);
        System.out.println("testHeuristicaSimple2: " + heuristica2);
        assertTrue(heuristica < heuristica2); // Ventaja para el jugador 1
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
       System.out.println("testDijkstra1: " + distance.shortestPath);
       assertEquals(5, distance.shortestPath);
    }
    
    @Test
    public void testDijkstra_Bugeado_Player1() {
        byte[][] board = {
            { 0, 0, 0, 0, 0},
              { 0, 0, 0, -1, 0},
                { 0, 1, 1, -1, 0},
                  { 1, -1, 1, -1, 0},
                    { 0, 0, 0, 0, 0}
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        Dijkstra dijkstra = new Dijkstra();
        PlayerMinimaxHexCalculatorsSINOPTIMIZAR player1 = new PlayerMinimaxHexCalculatorsSINOPTIMIZAR("MiniMax", 5, true);
        
        Dijkstra distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);

        System.out.println("testDijkstra_Bugeado_Player1: " + distance.shortestPath);
        System.out.println("testDijkstra_Bugeado_Player1: " + distance.enemyShortestPath);
        System.out.println("testDijkstra_Bugeado_Player1: " + distance.viablePathsCount);
        System.out.println("testDijkstra_Bugeado_Player1: " + distance.viableEnemyPathsCount);
        System.out.println("testDijkstra_Bugeado_Player1: " + player1.heuristica0(gs, 1, 0));
        assertEquals(3, distance.shortestPath);
        assertEquals(2, distance.enemyShortestPath);
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
    public void testOrdenarMovimientosRapido() {
        // Configuración inicial del tablero
        byte[][] board = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, -1, 1, 0, -1, 0, 0},
            {0, 0, 1, -1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, -1, 0, 0, 0},
            {0, 0, 0, 0, -1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0}
        };

        HexGameStatus estado = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("TestPlayer", 5, board.length);
        player._colorPlayer = 1;
        // Ordenar movimientos usando ordenarMovimientosRapido
        List<MoveNode> movimientosOrdenados = player.ordenarMovimientosRapido(estado);

        // Verificar que los movimientos están ordenados correctamente
        System.out.println("Movimientos ordenados (rápido):");
        for (MoveNode movimiento : movimientosOrdenados) {
            Point punto = movimiento.getPoint();
            int heuristica = player.heuristicaRapida(estado, punto);
            System.out.println("Punto: " + punto + ", Heurística: " + heuristica);
        }

        // Comprobación básica: la heurística debe disminuir en la lista ordenada
        for (int i = 1; i < movimientosOrdenados.size(); i++) {
            Point anterior = movimientosOrdenados.get(i - 1).getPoint();
            Point actual = movimientosOrdenados.get(i).getPoint();

            int heuristicaAnterior = player.heuristicaRapida(estado, anterior);
            int heuristicaActual = player.heuristicaRapida(estado, actual);

            assertTrue(heuristicaAnterior >= heuristicaActual);
        }
    }

    @Test
    public void testOrdenarMovimientos() {
        // Configuración inicial del tablero
        byte[][] board = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, -1, 1, 0, -1, 0, 0},
            {0, 0, 1, -1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, -1, 0, 0, 0},
            {0, 0, 0, 0, -1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0}
        };

        HexGameStatus estado = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("TestPlayer", 5, board.length);
        player._colorPlayer = 1;
        // Ordenar movimientos usando ordenarMovimientosRapido
        List<MoveNode> movimientosOrdenados = player.ordenarMovimientos(estado);

        // Verificar que los movimientos están ordenados correctamente
        System.out.println("Movimientos ordenados (normal):");
        for (MoveNode movimiento : movimientosOrdenados) {
            Point punto = movimiento.getPoint();
            System.out.println("Punto: " + punto);
        }

        // Comprobación básica: la heurística debe disminuir en la lista ordenada
        /*for (int i = 1; i < movimientosOrdenados.size(); i++) {
            Point anterior = movimientosOrdenados.get(i - 1).getPoint();
            Point actual = movimientosOrdenados.get(i).getPoint();

            int heuristicaAnterior = player.heuristica(estado, 1);
            int heuristicaActual = player.heuristica(estado, 1);

            assertTrue(heuristicaAnterior >= heuristicaActual);
        }*/
    }
}
