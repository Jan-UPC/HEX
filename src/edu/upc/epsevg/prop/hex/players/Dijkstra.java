package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import java.awt.Point;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Classe que implementa el càlcul de camins mínims i altres mètriques associades
 * utilitzant l'algorisme de Dijkstra.
 */
public class Dijkstra {
    public int shortestPath;         // Camino más corto
    public int viablePathsCount;     // Número de caminos viables cercanos al óptimo
    public int viableEnemyPathsCount; // Número de caminos viables cercanos al óptimo del enemigo
    public int enemyShortestPath;    // Camino más corto del enemigo

    /**
     * Constructor amb paràmetres.
     *
     * @param shortestPath Distància del camí més curt del jugador actual.
     * @param viablePathsCount Nombre de camins viables propers al camí més curt del jugador.
     * @param viableEnemyPathsCount Nombre de camins viables propers al camí més curt de l'enemic.
     * @param enemyShortestPath Distància del camí més curt de l'enemic.
     */
    public Dijkstra(int shortestPath, int viablePathsCount, int viableEnemyPathsCount,  int enemyShortestPath) {
        this.shortestPath = shortestPath;
        this.viablePathsCount = viablePathsCount;
        this.enemyShortestPath = enemyShortestPath;
        this.viableEnemyPathsCount = viableEnemyPathsCount;
    }

    /**
     * Constructor per defecte.
     * Inicialitza els valors per defecte, establint les distàncies com a màximes
     * i els comptadors de camins viables a zero.
     */
    public Dijkstra() {
        this.shortestPath = Integer.MAX_VALUE;
        this.viablePathsCount = 0;
        this.viableEnemyPathsCount = 0;
        this.enemyShortestPath = Integer.MAX_VALUE;
    }

    /**
    * Calcula el camí més curt i altres mètriques utilitzant nodes virtuals per simular 
    * les vores del tauler. Aquesta funció aplica l'algorisme de Dijkstra per determinar
    * la distància mínima tant per al jugador actual com per a l'enemic.
    *
    * @param estat Estat actual del tauler.
    * @param color Color del jugador actual (1 o -1).
    * @return Una instància de la classe Dijkstra amb les mètriques calculades.
    */
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
            // Direcciones: {deltaX1, deltaY1, deltaX2, deltaY2, deltaX3, deltaY3}
            int[][] direcciones = {
                {0, -1, 1, -1, 1, -2}, // Arriba
                {-1, 0, 0, -1, -1, -1}, // Arriba-Izquierda            
                {-1, 1, -1, 0, -2, 1}, // Abajo-Izquierda             
                {0, 1, -1, 1, -1, 2},  // Abajo
                {1, 0, 0, 1, 1, 1},    // Abajo-Derecha
                {1, -1, 1, 0, 2, -1}  // Arriba-Derecha
            };

            // Comprobar las seis casillas para cada dirección
            for (int[] dir : direcciones) {
                int interX1 = pActual.x + dir[0];
                int interY1 = pActual.y + dir[1];
                int interX2 = pActual.x + dir[2];
                int interY2 = pActual.y + dir[3];
                int finalX = pActual.x + dir[4];
                int finalY = pActual.y + dir[5];

                // Verificar si las coordenadas están dentro del tablero
                if (interX1 >= 0 && interX1 < midaTauler && interY1 >= 0 && interY1 < midaTauler &&
                    interX2 >= 0 && interX2 < midaTauler && interY2 >= 0 && interY2 < midaTauler &&
                    finalX >= 0 && finalX < midaTauler && finalY >= 0 && finalY < midaTauler) {

                    // Comprobar las condiciones de las casillas
                    if (estat.getPos(interX1, interY1) == 0 && estat.getPos(interX2, interY2) == 0 &&
                        estat.getPos(finalX, finalY) == color) {
                        // Añadir la casilla final como vecino directo
                        vecinos.add(new Point(finalX, finalY));
                    }
                }
            }

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
            int[][] direcciones = {
                {0, -1, 1, -1, 1, -2}, // Arriba
                {-1, 0, 0, -1, -1, -1}, // Arriba-Izquierda            
                {-1, 1, -1, 0, -2, 1}, // Abajo-Izquierda             
                {0, 1, -1, 1, -1, 2},  // Abajo
                {1, 0, 0, 1, 1, 1},    // Abajo-Derecha
                {1, -1, 1, 0, 2, -1}  // Arriba-Derecha
            };

            // Comprobar las seis casillas para cada dirección
            for (int[] dir : direcciones) {
                int interX1 = pActual.x + dir[0];
                int interY1 = pActual.y + dir[1];
                int interX2 = pActual.x + dir[2];
                int interY2 = pActual.y + dir[3];
                int finalX = pActual.x + dir[4];
                int finalY = pActual.y + dir[5];

                // Verificar si las coordenadas están dentro del tablero
                if (interX1 >= 0 && interX1 < midaTauler && interY1 >= 0 && interY1 < midaTauler &&
                    interX2 >= 0 && interX2 < midaTauler && interY2 >= 0 && interY2 < midaTauler &&
                    finalX >= 0 && finalX < midaTauler && finalY >= 0 && finalY < midaTauler) {

                    // Comprobar las condiciones de las casillas
                    if (estat.getPos(interX1, interY1) == 0 && estat.getPos(interX2, interY2) == 0 &&
                        estat.getPos(finalX, finalY) == -color) {
                        // Añadir la casilla final como vecino directo
                        vecinos.add(new Point(finalX, finalY));
                    }
                }
            }
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
        viablePathsCount = 0;
        viableEnemyPathsCount = 0;
        if (color == 1) {
        // Casillas finales para el jugador 1 (última fila)
            for (int fila = 0; fila < midaTauler; fila++) {
                //int index = fila * midaTauler + (midaTauler - 1); // Última fila
                int index = (midaTauler - 1) * midaTauler + fila;
                if (distancias[index] <= shortestPath) {
                    viablePathsCount++;
                }
            }
        } else if (color == -1) {
            // Casillas finales para el jugador -1 (última columna)
            for (int columna = 0; columna < midaTauler; columna++) {
                //int index = (midaTauler - 1) * midaTauler + columna; // Última columna
                int index = columna * midaTauler + (midaTauler - 1);
                if (distancias[index] <= shortestPath) {
                    viablePathsCount++;
                }
            }
        }

        // Repetir para el enemigo (-color)
        if (-color == 1) {
            // Casillas finales para el enemigo hacia la derecha
            for (int fila = 0; fila < midaTauler; fila++) {
                //int index = fila * midaTauler + (midaTauler - 1); // Última fila
                int index = (midaTauler - 1) * midaTauler + fila;
                if (distanciasEnemigo[index] <= enemyShortestPath) {
                    viableEnemyPathsCount++;
                }
            }
        } else if (-color == -1) {
            // Casillas finales para el enemigo hacia abajo
            for (int columna = 0; columna < midaTauler; columna++) {
                //int index = (midaTauler - 1) * midaTauler + columna; // Última columna
                int index = columna * midaTauler + (midaTauler - 1);
                if (distanciasEnemigo[index] <= enemyShortestPath) {
                    viableEnemyPathsCount++;
                }
            }
        }

        return new Dijkstra(shortestPath, viablePathsCount, viableEnemyPathsCount, enemyShortestPath);
    }

}
