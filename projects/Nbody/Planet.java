public class Planet {
    public double xxPos;
    public double yyPos;
    public double xxVel;
    public double yyVel;
    public double mass;
    public String imgFileName;
    public Planet(double xP, double yP, double xV, double yV, double m, String img){
        xxPos = xP;
        yyPos = yP;
        xxVel = xV;
        yyVel = yV;
        mass = m;
        imgFileName = img;
    }
    
    public Planet(Planet p){
        this(p.xxPos, p.yyPos, p.xxVel, p.yyVel, p.mass, p.imgFileName);
    }
    public double calcDistance(Planet planet){
        double dxsquare = Math.pow(this.xxPos-planet.xxPos,2);
        double dysquare = Math.pow(this.yyPos-planet.yyPos,2);
        double distance = Math.sqrt(dxsquare+dysquare);
        return distance;
    }
    public double calcForceExertedBy(Planet planet){
        double G = 6.67*Math.pow(10,-11);
        double force = (G*this.mass*planet.mass)/Math.pow(this.calcDistance(planet),2);
        return force;
    }
    public double calcForceExertedByY(Planet planet){
        double F = this.calcForceExertedBy(planet)*(planet.yyPos-this.yyPos)/this.calcDistance(planet);
        return F;
    }
    public double calcForceExertedByX(Planet planet){
        double F = this.calcForceExertedBy(planet)*(planet.xxPos-this.xxPos)/this.calcDistance(planet);
        return F;
    }
    public double calcNetForceExertedByX(Planet[] allPlanets){
        double netForce=0;
        for (Planet Element: allPlanets){
            if (this.equals(Element)==false){
                netForce += this.calcForceExertedByX(Element);
            }
        }
        return netForce;
    }
    public double calcNetForceExertedByY(Planet[] allPlanets){
        double netForce=0;
        for (Planet Element: allPlanets){
            if (this.equals(Element)==false){
                netForce += this.calcForceExertedByY(Element);
            }
        }
        return netForce;
    }
    public void update(double dt, double fX, double fY){
        double aX = fX/this.mass;
        double aY = fY/this.mass;
        xxVel = this.xxVel + dt*aX;
        yyVel = this.yyVel + dt*aY;
        xxPos = this.xxPos + dt*xxVel;
        yyPos = this.yyPos + dt*yyVel;    
    }
    public void draw(){
        StdDraw.picture(this.xxPos, this.yyPos,"images/" + this.imgFileName);
    }
}
