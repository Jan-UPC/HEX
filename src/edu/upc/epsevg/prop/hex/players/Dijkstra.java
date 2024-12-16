package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import java.awt.Point;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class Dijkstra {
    public int shortestPathWithVirtualNodes(HexGameStatus estat, int color) {
        //if (nivellsExplorats > _profExplorada) _profExplorada = nivellsExplorats;
        int midaTauler = estat.getSize();
        int[] distancies = new int[midaTauler*midaTauler];
        PriorityQueue<Point> cua = new PriorityQueue<>((a,b)->distancies[a.x*midaTauler + a.y] - distancies[b.x*midaTauler + b.y]);
        
        // Inicialitzar vector de distàncies
        for (int i = 0; i < midaTauler*midaTauler; i++) {
            distancies[i] = midaTauler;
            // Aprofitem el bucle per afegir també les caselles inicials de l'algorisme i actualitzar el vector distàncies per aquelles caselles
            int columna = i/midaTauler; // també se li pot anomenar x
            int fila = i%midaTauler; // també se li pot anomenar y
            
            if (color == 1 && columna == 0) {
                // El jugador amb color "1" ha d'unir la banda esquerra (columna==0) amb la banda dreta (columna==midaTauler-1)
                int colorCasella = estat.getPos(columna, fila);
                if (colorCasella != -color) {
                    // La casella té una fitxa del jugador de color 1 o està buida
                    cua.add(new Point(columna, fila));
                    if (colorCasella == color) {
                        // Casella amb fitxa del jugador de color 1: té distància 0
                        distancies[i] = 0;
                    }
                    else if (colorCasella == 0) {
                        // Casella buida: té distància 1
                        distancies[i] = 1;
                    }
                }
            }
            
            if (color == -1 && fila == 0) {
                // El jugador amb color "-1" ha d'unir la banda superior (fila==0) amb la banda inferior (fila==midaTauler-1)
                int colorCasella = estat.getPos(columna, fila);
                if (colorCasella != -color) {
                    // La casella té una fitxa del jugador de color -1 o està buida
                    cua.add(new Point(columna, fila));
                    if (colorCasella == color) {
                        // Casella amb fitxa del jugador de color -1: té distància 0
                        distancies[i] = 0;
                    }
                    else if (colorCasella == 0) {
                        // Casella buida: té distància 1
                        distancies[i] = 1;
                    }
                }
            }
            
        }
        
        // Algorisme de Dijkstra
        while (!cua.isEmpty()) {
            Point pActual = cua.poll();
            //System.out.println("Mirando casilla: " + pActual.x + " " + pActual.y);
            int indexActual = pActual.x * midaTauler + pActual.y;
            
            if ((color == 1 && pActual.x == midaTauler-1) || (color == -1 && pActual.y == midaTauler-1)) {
                // Hem arribat a l'altra banda del tauler, no necessitem explorar més
                return distancies[indexActual];
            }
            
            ArrayList<Point> veins = estat.getNeigh(pActual);
            for (int i = 0; i < veins.size(); i++) {
                Point vei = veins.get(i);
                int columnaVei = vei.x;
                int filaVei = vei.y;
                int colorVei = estat.getPos(vei);
                int indexVei = columnaVei*midaTauler + filaVei;
                //System.out.println("Analizando casillas vecinas, con valor: " + vei.x + " " + vei.y);
                if (colorVei == color) {
                    // Casella amb fitxa del jugador de color "color": té distància 0
                    int nouCost = distancies[indexActual];
                    if (nouCost < distancies[indexVei]) {
                        //System.out.println("Actualizando coste con valor existente: " + distancies[indexActual]);
                        distancies[indexVei] = nouCost;
                        cua.add(vei);
                    }
                }
                else if (colorVei == 0) {
                    // Casella buida: té distància 1
                    int nouCost = distancies[indexActual]+1;
                    if (nouCost < distancies[indexVei]) {
                        distancies[indexVei] = nouCost;
                        cua.add(vei);
                    }
                }
            }
        }
        
        return midaTauler;
    }
}
class PriorityNode implements Comparable<PriorityNode> {
    Point point;
    int priority;

    public PriorityNode(Point point, int priority) {
        this.point = point;
        this.priority = priority;
    }

    @Override
    public int compareTo(PriorityNode other) {
        return Integer.compare(this.priority, other.priority);
    }
}
