import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import static java.lang.Math.sqrt;


/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    // Long is the id of the node
    Map<Long, Node> nodes = new HashMap<>();
    Map<Long, LinkedList<Long>> edges = new HashMap<>();
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputFile, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }
    // Node helper class
    public class Node {
        double lat, lon;
        String name;
        long id;
        double distance;
        double he;
        Node(long id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.name = "";
        }
    }
    Node getNode(Long id) {
        return nodes.get(id);
    }
    // take in id as a String and convert it into long before putting into the hashMap
    void addNode(String id, String lat, String lon) {
        Node n = new Node(Long.parseLong(id), Double.parseDouble(lat), Double.parseDouble(lon));
        nodes.put(Long.parseLong(id), n);
    }

    void addEdge(LinkedList<String> adding) {
        Long ref;
        if (adding.size() == 2) {
            ref = Long.parseLong(adding.get(0));
            edges.get(ref).addLast(Long.parseLong(adding.get(1)));
            edges.get(Long.parseLong(adding.get(1))).addLast(ref);

        }
        for (int i = 1; i < adding.size() - 1; i++) {
            ref = Long.parseLong(adding.get(i));
            edges.get(ref).addLast(Long.parseLong(adding.get(i - 1)));
            edges.get(Long.parseLong(adding.get(i - 1))).addLast(ref);
            edges.get(ref).addLast(Long.parseLong(adding.get(i + 1)));
            edges.get(Long.parseLong(adding.get(i + 1))).addLast(ref);
        }
    }
    // add the name to the Node with id "id"
    void addName(String id, String name) {
        nodes.get(Long.parseLong(id)).name = name;
    }
    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        for (Long nd: edges.keySet()) {
             if (edges.get(nd).size() == 0) {
                 nodes.remove(nd);
             }
        }
    }

    /** Returns an iterable of all vertex IDs in the graph. */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return nodes.keySet();
    }

    /** Returns ids of all vertices adjacent to v. */
    Iterable<Long> adjacent(long v) {
        if (this.edges.containsKey(v)) {
            return this.edges.get(v);
        } else {
            return null;
        }
    }

    /** Returns the Euclidean distance between vertices v and w, where Euclidean distance
     *  is defined as sqrt( (lonV - lonV)^2 + (latV - latV)^2 ). */
    double distance(long v, long w) {
        Node first = nodes.get(v);
        Node second = nodes.get(w);
        return sqrt((first.lon - second.lon) * (first.lon - second.lon) + (first.lat - second.lat) * (first.lat - second.lat));
    }
    /** Returns the vertex id closest to the given longitude and latitude. */
    long closest(double lon, double lat) {
        Node closestNode = new Node(0000, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Node temp = closestNode;
        Node targetNode = new Node(1, lat, lon);
        addNode(Long.toString(targetNode.id), Double.toString(lat), Double.toString(lon));
        addNode(Long.toString(closestNode.id), Double.toString(closestNode.lat), Double.toString(closestNode.lon));
        for (Map.Entry<Long, Node> nd: nodes.entrySet()) {
            Node curNode = nd.getValue();
            if (curNode.id != closestNode.id && curNode.id != targetNode.id) {
                if (curNode.lon == lon && curNode.lat == lat) {
                    return curNode.id;
                } else if (distance(curNode.id, targetNode.id) < distance(closestNode.id, targetNode.id)) {
                    closestNode = curNode;
                }
            }
        }
        nodes.remove(targetNode.id);
        nodes.remove(temp.id);
        return closestNode.id;
    }

    /** Longitude of vertex v. */
    double lon(long v) {
        return nodes.get(v).lon;
    }

    /** Latitude of vertex v. */
    double lat(long v) {
        return nodes.get(v).lat;
    }
}
