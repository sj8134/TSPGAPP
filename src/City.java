import java.io.IOException;

import edu.rit.io.InStream;
import edu.rit.io.OutStream;
import edu.rit.io.Streamable;

/**
 * Class city represents a city in the traveling sales man problem
 * 
 * @author Sahil Jasrotia, Lokesh Agrawal
 *
 */
public class City implements Streamable{

	public double x;	// The x coordinate of the city
	public double y;	// The y coordinate of the city
	public int id;		// Id of the city
	
	/**
	 * Default constructor to construct a city
	 */
	public City(){		
	}
	
	/**
	 * Method to compare two cities
	 * 
	 * @param obj the city object
	 */
	@Override
	public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof City)) {
            return false;
        }
        City city = (City) obj;
        return (city.x == x && city.y == y);
    }
	
	/**
	 * Parameterized constructor to create a city
	 * 
	 * @param x the x coordinate of the city
	 * @param y the y coordinate of the city
	 * @param id the identifier of the city
	 */
	public City(double x, double y, int id){
		this.x = x;
		this.y = y;
		this.id = id;
	}
	
	/**
	 * Returns x coordinate of the city
	 * 
	 * @return x coordinate
	 */
	public double getX() {
		return x;
	}
	
	/**
	 * Sets the x coordinate of the city
	 * 
	 * @param x sets x coordinate
	 */
	public void setX(double x) {
		this.x = x;
	}
	
	/**
	 * Returns the Y coordinate of the city
	 * 
	 * @return y coordinate
	 */
	public double getY() {
		return y;
	}
	
	/**
	 * Sets y coordinate of the city
	 * 
	 * @param y coordinate of city
	 */
	public void setY(double y) {
		this.y = y;
	}	
	
	/**
	 * Read the fields of this streamable class from the given input stream
	 * 
	 * @param in The input stream
	 * 
	 * @exception  IOException Throws the IO exception if there is an error
	 */
	public void readIn(InStream inStream) throws IOException {
		x = inStream.readDouble();
		y = inStream.readDouble();
		id = inStream.readInt();
	}

	/**
	 * Write the fields of this class to the out stream
	 * 
	 * @param out The output stream
	 * 
	 * @exception  IOException Throws the IO exception if there is an error
	 */
	public void writeOut(OutStream outStream) throws IOException {
		outStream.writeDouble(x);
		outStream.writeDouble(y);
		outStream.writeInt(id);
	}
}
