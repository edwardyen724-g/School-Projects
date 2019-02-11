public class NBody {
    public static double readRadius(String file){
        In in = new In(file);
        in.readInt();
        double radius = in.readDouble();
        return radius;
    }
    public static Planet[] readPlanets(String file){
        In in = new In(file);
        int planetsNum = in.readInt(); //read of planets number
        Planet[] array = new Planet[planetsNum];
        in.readDouble(); //read of radius of universe
        int counter = 0;
        while (counter < planetsNum){
            Planet planet = new Planet(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readString());
            array[counter] = planet;
            counter += 1;
        }
        return array;
    }
    public static void main(String[] args){
        double T = Double.parseDouble(args[0]);
        double dt = Double.parseDouble(args[1]);
        String filename = args[2];
        Planet[] planets = readPlanets(filename);
        double radius = readRadius(filename);
        //drawing the background
        StdDraw.setCanvasSize();
        StdDraw.setScale(-radius,radius);
        StdDraw.picture(0,0,"images/starfield.jpg");
        //Drawing the planets
        for (Planet Element: planets){
            Element.draw();
        }
        double time = 0;
        while (time < T){
            double[] xForces = new double[planets.length];
            double[] yForces = new double[planets.length];
            for (int i = 0; i < planets.length; i++){
                double xForce = planets[i].calcNetForceExertedByX(planets);
                xForces[i] = xForce;
            }
            for (int i = 0; i < planets.length; i++){
                double yForce = planets[i].calcNetForceExertedByY(planets);
                yForces[i] = yForce;
            }
            for (int i = 0; i< planets.length; i++){
                planets[i].update(dt,xForces[i],yForces[i]);
            }
            StdDraw.picture(0,0,"images/starfield.jpg");
            for (Planet Element: planets){
                Element.draw();
            }
            StdDraw.show(10);
            time += dt;
        }
        StdOut.printf("%d\n", planets.length);
        StdOut.printf("%.2e\n", radius);
        for (int i = 0; i < planets.length; i++) {
            StdOut.printf("%11.4e %11.4e %11.4e %11.4e %11.4e %12s\n",
                         planets[i].xxPos, planets[i].yyPos,planets[i].xxVel,
                         planets[i].yyVel, planets[i].mass,planets[i].imgFileName);
        }
    }
    
}
