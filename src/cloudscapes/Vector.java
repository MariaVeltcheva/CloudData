package cloudscapes;

import java.lang.Math.*;

public class Vector{
	
	/**x co-ordinate of the vector**/
	public float x;
	/**y co-ordinate of the vector**/
	public float y;
	
	/**Returns the length of a vector
	 * @return the length of the vector
	 * **/
	public float getLength(){
		return (float) Math.sqrt( (x*x) + (y*y) );
	}

}
