package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import java.awt.Point;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class Dijkstra {
    public int shortestPath;         // Camino más corto
    public int viablePathsCount;     // Número de caminos viables cercanos al óptimo
    public int enemyShortestPath;    // Camino más corto del enemigo

    public Dijkstra(int shortestPath, int viablePathsCount, int enemyShortestPath) {
        this.shortestPath = shortestPath;
        this.viablePathsCount = viablePathsCount;
        this.enemyShortestPath = enemyShortestPath;
    }

    public Dijkstra() {
        this.shortestPath = Integer.MAX_VALUE;
        this.viablePathsCount = 0;
        this.enemyShortestPath = Integer.MAX_VALUE;
    }

    public Dijkstra shortestPathWithVirtualNodes(HexGameStatus estat, int color) {
        int midaTauler = estat.getSize();
        int[] distancias = new int[midaTauler * midaTauler];
        int[] distanciasEnemigo = new int[midaTauler * midaTauler];
        boolean[][] visitado = new boolean[midaTauler][midaTauler];

        PriorityQueue<Point> cua = new PriorityQueue<>((a, b) ->
            Integer.compare(distancias[a.x * midaTauler + a.y], distancias[b.x * midaTauler + b.y])
        );

        PriorityQueue<Point> cuaEnemigo = new PriorityQueue<>((a, b) ->
            Integer.compare(distanciasEnemigo[a.x * midaTauler + a.y], distanciasEnemigo[b.x * midaTauler + b.y])
        );

        // Inicialización de distancias para jugador y enemigo
        for (int i = 0; i < midaTauler * midaTauler; i++) {
            distancias[i] = Integer.MAX_VALUE;
            distanciasEnemigo[i] = Integer.MAX_VALUE;

            int columna = i / midaTauler;
            int fila = i % midaTauler;
            int colorCasella = estat.getPos(columna, fila);

            // Inicialización para el jugador
            if ((color == 1 && columna == 0) || (color == -1 && fila == 0)) {
                if (colorCasella != -color) { // No agregar si es del enemigo
                    cua.add(new Point(columna, fila));
                    distancias[i] = (colorCasella == color) ? 0 : 1;
                }
            }

            // Inicialización para el enemigo
            if ((-color == 1 && columna == 0) || (-color == -1 && fila == 0)) {
                if (colorCasella != color) { // No agregar si es del jugador actual
                    cuaEnemigo.add(new Point(columna, fila));
                    distanciasEnemigo[i] = (colorCasella == -color) ? 0 : 1;
                }
            }
        }

        // Ejecutar Dijkstra para el jugador
        int shortestPath = Integer.MAX_VALUE;
        while (!cua.isEmpty()) {
            Point pActual = cua.poll();
            if (visitado[pActual.x][pActual.y]) continue;
            visitado[pActual.x][pActual.y] = true;

            ArrayList<Point> vecinos = estat.getNeigh(pActual);
            for (Point vecino : vecinos) {
                int indexActual = pActual.x * midaTauler + pActual.y;
                int indexVecino = vecino.x * midaTauler + vecino.y;
                int colorVecino = estat.getPos(vecino.x, vecino.y);

                // Ignorar si el vecino está bloqueado por el enemigo
                if (colorVecino == -color) continue;

                int nuevoCosto = distancias[indexActual] +
                    ((colorVecino == color) ? 0 : 1);

                if (nuevoCosto < distancias[indexVecino]) {
                    distancias[indexVecino] = nuevoCosto;
                    cua.add(vecino);

                    if ((color == 1 && vecino.x == midaTauler - 1) || 
                        (color == -1 && vecino.y == midaTauler - 1)) {
                        shortestPath = Math.min(shortestPath, nuevoCosto);
                    }
                }
            }
        }

        // Ejecutar Dijkstra para el enemigo
        int enemyShortestPath = Integer.MAX_VALUE;
        while (!cuaEnemigo.isEmpty()) {
            Point pActual = cuaEnemigo.poll();
            ArrayList<Point> vecinos = estat.getNeigh(pActual);
            for (Point vecino : vecinos) {
                int indexActual = pActual.x * midaTauler + pActual.y;
                int indexVecino = vecino.x * midaTauler + vecino.y;
                int colorVecino = estat.getPos(vecino.x, vecino.y);

                // Ignorar si el vecino está bloqueado por el jugador
                if (colorVecino == color) continue;

                int nuevoCosto = distanciasEnemigo[indexActual] +
                    ((colorVecino == -color) ? 0 : 1);

                if (nuevoCosto < distanciasEnemigo[indexVecino]) {
                    distanciasEnemigo[indexVecino] = nuevoCosto;
                    cuaEnemigo.add(vecino);

                    if ((-color == 1 && vecino.x == midaTauler - 1) || 
                        (-color == -1 && vecino.y == midaTauler - 1)) {
                        enemyShortestPath = Math.min(enemyShortestPath, nuevoCosto);
                    }
                }
            }
        }

        // Contar caminos viables
        int viablePathsCount = 0;
        for (int i = 0; i < midaTauler * midaTauler; i++) {
            if (distancias[i] <= shortestPath + 1) {
                viablePathsCount++;
            }
        }

        return new Dijkstra(shortestPath, viablePathsCount, enemyShortestPath);
    }

}
