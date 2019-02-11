import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    // Recommended: QuadTree instance variable. You'll need to make
    //              your own QuadTree since there is no built-in quadtree in Java.

    /**
     * imgRoot is the name of the directory containing the images.
     * You may not actually need this for your class.
     */
    double ROOT_ULLAT, ROOT_ULLON, ROOT_LRLAT, ROOT_LRLON;

    public Rasterer(String imgRoot) {
        this.ROOT_ULLAT = MapServer.ROOT_ULLAT;
        this.ROOT_ULLON = MapServer.ROOT_ULLON;
        this.ROOT_LRLAT = MapServer.ROOT_LRLAT;
        this.ROOT_LRLON = MapServer.ROOT_LRLON;
    }

    public static class QuadTree {
        double ullon, ullat, lrlon, lrlat;
        String name;
        int depth;
        final int maxDepth = 7;

        // consist of the coordinate of the current square
        QuadTree(String name, double ullon, double ullat, double lrlon, double lrlat, int depth) {
            this.ullon = ullon;
            this.ullat = ullat;
            this.lrlon = lrlon;
            this.lrlat = lrlat;
            this.name = name;
            this.depth = depth;

        }

        // find the four child of the parent and return as a QuadTree array
        private QuadTree[] findChild(QuadTree parent) {
            double point1_lon = parent.ullon;
            double point1_lat = parent.ullat;
            double point2_lon = (parent.lrlon + parent.ullon) / 2;
            double point2_lat = parent.ullat;
            double point3_lon = parent.ullon;
            double point3_lat = (parent.ullat + parent.lrlat) / 2;
            double point4_lon = (parent.lrlon + parent.ullon) / 2;
            double point4_lat = (parent.ullat + parent.lrlat) / 2;
            double point5_lon = (parent.lrlon + parent.ullon) / 2;
            double point5_lat = parent.lrlat;
            double point6_lon = parent.lrlon;
            double point6_lat = parent.lrlat;
            double point7_lon = parent.lrlon;
            double point7_lat = (parent.ullat + parent.lrlat) / 2;
            QuadTree ul, ur, ll, lr;
            if (parent.name.equals("root")) {
                ul = new QuadTree("1", point1_lon, point1_lat, point4_lon, point4_lat, parent.depth + 1);
                ur = new QuadTree("2", point2_lon, point2_lat, point7_lon, point7_lat, parent.depth + 1);
                ll = new QuadTree("3", point3_lon, point3_lat, point5_lon, point5_lat, parent.depth + 1);
                lr = new QuadTree("4", point4_lon, point4_lat, point6_lon, point6_lat, parent.depth + 1);
            } else {
                ul = new QuadTree(parent.name + "1", point1_lon, point1_lat, point4_lon, point4_lat, parent.depth + 1);
                ur = new QuadTree(parent.name + "2", point2_lon, point2_lat, point7_lon, point7_lat, parent.depth + 1);
                ll = new QuadTree(parent.name + "3", point3_lon, point3_lat, point5_lon, point5_lat, parent.depth + 1);
                lr = new QuadTree(parent.name + "4", point4_lon, point4_lat, point6_lon, point6_lat, parent.depth + 1);
            }
            QuadTree[] child = new QuadTree[4];
            child[0] = ul;
            child[1] = ur;
            child[2] = ll;
            child[3] = lr;
            return child;
        }

        private boolean checkOverlap(QuadTree ta) {
            if (ta.ullon >= this.lrlon || ta.lrlon <= this.ullon || ta.ullat <= this.lrlat || ta.lrlat >= this.ullat) {
                return false;
            }
            return true;
        }

        //return true if the lonDPP of the current box is smaller that the target londpp
        //hence don't need to find more depth
        private boolean checkResolution(double tar_LonDPP) {
            double LonDPP = (this.lrlon - this.ullon) / 256;
            return LonDPP <= tar_LonDPP;
        }
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     * The grid of images must obey the following properties, where image in the
     * grid is referred to as a "tile".
     * <ul>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * (LonDPP) possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * </p>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image
     * string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     * forget to set this to true! <br>
     * //@see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
//       System.out.println(params);
        //query box info
        double tar_lrlon, tar_ullon, tar_ullat, tar_lrlat, tar_w, tar_h, tar_LonDPP;
        tar_lrlon = params.get("lrlon");
        tar_ullon = params.get("ullon");
        tar_ullat = params.get("ullat");
        tar_lrlat = params.get("lrlat");
        tar_w = params.get("w");
        tar_h = params.get("h");
        tar_LonDPP = (tar_lrlon - tar_ullon) / tar_w;
        Map<String, Object> results = new HashMap<>();
        //check if query box make sense
        QuadTree rootTree = new QuadTree("root", ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT, 0);
        QuadTree targetTree = new QuadTree("target", tar_ullon, tar_ullat, tar_lrlon, tar_lrlat, 0);
        if (tar_ullon >= tar_lrlon || tar_ullat <= tar_lrlat || !rootTree.checkOverlap(targetTree)) {
            results.put("raster_ul_lon", 0);
            results.put("raster_ul_lat", 0);
            results.put("raster_lr_lon", 0);
            results.put("raster_lr_lat", 0);
            results.put("query_success:", false);
        } else {
            work(rootTree, targetTree, results, tar_LonDPP);
        }
        return results;
    }

    //put all the information into result
    private void work(QuadTree current, QuadTree target, Map<String, Object> results, double tar_LonDPP) {
        LinkedList<QuadTree> grid = new LinkedList<>();
        LinkedList<QuadTree> Linked = new LinkedList<>();
        grid.addLast(current);
        while (grid.size() != 0) {
            QuadTree cur = grid.removeFirst();
            if (cur.checkResolution(tar_LonDPP) && cur.checkOverlap(target) || cur.checkOverlap(target) && cur.depth == 7) {
                Linked.addLast(cur);
            } else if (cur.checkOverlap(target) && cur.depth < cur.maxDepth) {
                QuadTree[] children = cur.findChild(cur);
                for (QuadTree c : children) {
                    grid.addLast(c);
                }
            }
        }
        String[][] childrenNames = childrenNames(Linked);

        results.put("render_grid", childrenNames);
        double[] fourPoints = findFourPoints(Linked);
        results.put("raster_ul_lon", fourPoints[0]);
        results.put("raster_lr_lon", fourPoints[1]);
        results.put("raster_lr_lat", fourPoints[2]);
        results.put("raster_ul_lat", fourPoints[3]);
        results.put("depth", Linked.getFirst().depth);
        results.put("query_success", true);
    }

    //return empty String[][] of the right dimension
    private String[][] twoDArray(LinkedList<QuadTree> Linked) {
        if (Linked.size() < 1) {
            return new String[0][0];
        } else if (Linked.size() == 1) {
            return new String[1][1];
        } else {
            int rowCount, columnCount;
            Set<Double> count = new HashSet<>();
            for (QuadTree q : Linked) {
                count.add(q.ullat);
            }
            rowCount = count.size();
            columnCount = Linked.size() / rowCount;
            return new String[rowCount][columnCount];
        }
    }

    // return a String[][] with the childrenNames with the file format
    private String[][] childrenNames(LinkedList<QuadTree> Linked) {
        LinkedList<Double> latName = new LinkedList<>();
        Map<Double, LinkedList<String>> res = new HashMap<>();
        for (QuadTree q : Linked) {
            if (!res.containsKey(q.ullat)) {
                res.put(q.ullat, new LinkedList<>());
                latName.addLast(q.ullat);
            }
            res.get(q.ullat).addLast(q.name);
        }

        String[][] emptyTree = twoDArray(Linked);
        for (int row = 0; row < emptyTree.length; row++) {
            for (int col = 0; col < emptyTree[0].length; col++) {
                emptyTree[row][col] = "img/" + res.get(latName.get(row)).get(col) + ".png";
            }
        }
        return emptyTree;
    }

    private double[] findFourPoints(LinkedList<QuadTree> Linked) {
        double[] four = new double[4];

        four[0] = Linked.getFirst().ullon;
        four[1] = Linked.getLast().lrlon;
        four[2] = Linked.getLast().lrlat;
        four[3] = Linked.getFirst().ullat;
        return four;
    }

    public static void main(String[] args) {
        QuadTree root = new QuadTree("root", 0, 512, 512, 0, 0);
        QuadTree sec = new QuadTree("s", 129, 383, 383, 129, 0);
        LinkedList<QuadTree> grid = new LinkedList<>();
        LinkedList<QuadTree> Linked = new LinkedList<>();
        grid.addLast(root);
        double tar_LonDPP = 0.25;
        while (grid.size() != 0) {
            QuadTree cur = grid.removeFirst();
            if (cur.checkResolution(tar_LonDPP) && cur.checkOverlap(sec)) {
                Linked.addLast(cur);
            } else if (cur.depth < cur.maxDepth && cur.checkOverlap(sec)) {
                QuadTree[] children = cur.findChild(cur);
                for (QuadTree c : children) {
                    grid.addLast(c);
                }
            }
        }
        for (QuadTree q : Linked) {
            System.out.println(q.name);
        }
    }
}
