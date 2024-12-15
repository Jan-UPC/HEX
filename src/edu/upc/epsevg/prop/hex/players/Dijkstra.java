package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import java.awt.Point;
import java.util.PriorityQueue;

public class Dijkstra {
    private int boardSize;

    public Dijkstra(int boardSize) {
        this.boardSize = boardSize;
    }

    public int shortestPathWithVirtualNodes(HexGameStatus status, int playerColor) {
        int size = status.getSize();
        PriorityQueue<PriorityNode> queue = new PriorityQueue<>();
        int[][] distances = new int[size][size];
        boolean[][] visited = new boolean[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        if (playerColor == 1) {
            for (int i = 0; i < size; i++) {
                if (status.getPos(i, 0) != -playerColor) {
                    distances[i][0] = (status.getPos(i, 0) == playerColor) ? 0 : 1;
                    queue.add(new PriorityNode(new Point(i, 0), distances[i][0]));
                }
            }
        } else if (playerColor == -1) {
            for (int j = 0; j < size; j++) {
                if (status.getPos(0, j) != -playerColor) {
                    distances[0][j] = (status.getPos(0, j) == playerColor) ? 0 : 1;
                    queue.add(new PriorityNode(new Point(0, j), distances[0][j]));
                }
            }
        }

        while (!queue.isEmpty()) {
            PriorityNode current = queue.poll();
            Point point = current.point;

            if (visited[point.x][point.y]) continue;
            visited[point.x][point.y] = true;

            if ((playerColor == 1 && point.y == size - 1) || (playerColor == -1 && point.x == size - 1)) {
                return distances[point.x][point.y];
            }

            for (Point neighbor : status.getNeigh(point)) {
                if (!visited[neighbor.x][neighbor.y] && status.getPos(neighbor) != -playerColor) {
                    int cost = (status.getPos(neighbor) == playerColor) ? 0 : 1;
                    int newDist = distances[point.x][point.y] + cost;

                    if (newDist < distances[neighbor.x][neighbor.y]) {
                        distances[neighbor.x][neighbor.y] = newDist;
                        queue.add(new PriorityNode(neighbor, newDist));
                    }
                }
            }
        }

        return Integer.MAX_VALUE;
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
