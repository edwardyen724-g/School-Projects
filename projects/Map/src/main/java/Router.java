
import java.util.*;


/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a LinkedList of <code>Long</code>s representing the shortest path from st to dest,
     * where the longs are node IDs.
     */
    public static LinkedList<Long> shortestPath
    (GraphDB g, double stlon, double stlat, double destlon, double destlat) {
        Comparator<GraphDB.Node> comparator = new NodeComparator();
        double initialDis = Double.POSITIVE_INFINITY;
        Map<Long, LinkedList<Object>> info = new HashMap<>();
        PriorityQueue<GraphDB.Node> fringePQ = new PriorityQueue<>(g.nodes.size(), comparator);
        // set start Node and end Node
        Long startNode = g.closest(stlon, stlat);
        Long endNode = g.closest(destlon, destlat);
        for (Long id: g.nodes.keySet()) {
            info.put(id, new LinkedList<>());
            disTo(info, id, initialDis);
            edgeTo(info, id, null);
            fringePQ.add(g.nodes.get(id));
            g.nodes.get(id).distance = initialDis;
            g.nodes.get(id).he = g.distance(id, endNode);
        }
        fringePQ.remove(g.getNode(startNode));

        // set the disTo of the startNode as 0
        disTo(info, startNode, 0);
        // set the adjacent nodes of the starting node to constants
        for (Long adj: g.adjacent(startNode)) {
            double adjDistance = g.distance(startNode, adj);
            disTo(info, adj, adjDistance);
            edgeTo(info, adj, startNode);
            g.getNode(adj).distance = adjDistance;
            g.getNode(adj).he = g.distance(adj, endNode);
            fringePQ.remove(g.getNode(adj));
            fringePQ.add(g.getNode(adj));
        }
    //-----------------above is done-----------------
        System.out.println("hi");
        while (fringePQ.peek().id != endNode) {
            GraphDB.Node v = fringePQ.poll();
           for (Long adj : g.adjacent(v.id)) {
               double potentialDis = g.distance(v.id, adj) + v.distance;
                if (disTo(info, adj) > potentialDis) {
                    disTo(info, adj, potentialDis);
                    edgeTo(info, adj, v.id);
                    g.getNode(adj).distance = potentialDis;
                    g.getNode(adj).he = g.distance(adj, endNode);
                    fringePQ.add(g.getNode(adj));
                }
           }
        }

        //----------------below is done------------------
        System.out.println("hi");
        LinkedList<Long> almostThere = new LinkedList<>();
        Long currentId = endNode;
        almostThere.addFirst(currentId);
        while (almostThere.get(0) != startNode) {
            currentId = edgeTo(info, currentId);
            almostThere.addFirst(currentId);
        }
        return almostThere;
    }

    public static class NodeComparator implements Comparator<GraphDB.Node> {
        @Override
        public int compare(GraphDB.Node a, GraphDB.Node b) {
            return (int) Math.signum(a.distance + a.he - b.distance - b.he);
        }
    }
    static void disTo(Map<Long, LinkedList<Object>> info, Long id, double distance) {
        info.get(id).add(0, distance);
    }
    static double disTo(Map<Long, LinkedList<Object>> info, Long id) {
        return (double) info.get(id).get(0);
    }
    static void edgeTo(Map<Long, LinkedList<Object>> info, Long id, Long whereFrom) {
        info.get(id).add(1, whereFrom);
    }
    static Long edgeTo(Map<Long, LinkedList<Object>> info, Long id) {
        return (Long) info.get(id).get(1);
    }
}
