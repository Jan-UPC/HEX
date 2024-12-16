package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.PlayerType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPlayerMiniMax {

    @Test
    public void testDijkstra_SimplePath_Player1() {
        byte[][] board = {
            { 0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0},
            { 1,  0,  0,  0,  1}
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        Dijkstra dijkstra = new Dijkstra();

        int distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
        System.out.println("Distancia calculada para PLAYER1: " + distance);
        assertEquals(3, distance);
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

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER2);
        Dijkstra dijkstra = new Dijkstra();

        int distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
        System.out.println("Resultado testDijkstra_SimplePath_Player2: " + distance);
        assertEquals(4, distance);
    }

    @Test
    public void testHeuristicaSimple() {
        byte[][] board = {
            { 1, 0, 0, 0, 0 },
            { 0, 1, 0, 0, 0 },
            { 0, 0, 1, 0, 0 },
            { 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, true);

        int heuristica = player.heuristica(gs, 1, 0);
        System.out.println("Heurisitca con valor: " + heuristica);
        assertTrue(heuristica > 0); // Ventaja para el jugador 1
    }
    
    @Test
    public void testHeuristicaSimple2() {
        byte[][] board = {
            { 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0 },
            { 1, 1, 1, 1, 0 }
        };

        HexGameStatus gs = new HexGameStatus(board, PlayerType.PLAYER1);
        PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, true);

        int heuristica = player.heuristica(gs, 1, 0);
        System.out.println("Heurisitca con valor: " + heuristica);
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

       int distance = dijkstra.shortestPathWithVirtualNodes(gs, 1);
       PlayerMinimaxHexCalculators player = new PlayerMinimaxHexCalculators("Test", 3, true);

       int heuristica = player.heuristica(gs, 1, 0);
       System.out.println("testDijkstra1 heurisitca con valor: " + heuristica);
       System.out.println("testDijkstra1: " + distance);
       assertEquals(5, distance);
    }
}
