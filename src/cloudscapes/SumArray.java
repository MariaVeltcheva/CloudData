package cloudscapes;

import java.util.concurrent.*;

public class SumArray extends RecursiveTask<Float> {
	/**Starting index of the array to be processed**/
	public int lo;
	/**Closing index of the array to be prossed**/
	public int hi;
	/**Array to be processed**/
	public Vector[] arr;
	/**Dictates whether the x or y values of the vectors in the array will be summed**/
	public String XorY;
	
	/**Creates a SumArray object with the given parameters.
	 * <p>
	 * @param a Vector array to be processed.
	 * @param l Starting index of the array to be processed.
	 * @param h Closing index of the array to be processed.
	 * @param XorY Dictates whether the x or y values of the vecors in the array will be summed.
	 * **/
	SumArray (Vector[] a, int l, int h, String XorY){
		this.lo = l;
		this.hi = h;
		this.arr = a;
		this.XorY = XorY;
	}
	
	/**Sums up the x or y values of vectors in the array and creates threads to do so.**/
	protected Float compute(){  // return answer
		if(hi - lo < 2) {
			float ans= 0;
			for(int i=lo; i< hi; i++){
				if(XorY.equals("x")){
					ans+= arr[i].x;
				}else{
					ans+= arr[i].y;
				}
			}
			return ans;
		}else{
			SumArray left  = new SumArray(arr,lo,(hi+lo)/2, XorY);
			SumArray right = new SumArray(arr,(hi+lo)/2,hi, XorY);
			left.fork();
			float rightAns= right.compute();
			float leftAns= left.join();
			return leftAns+ rightAns;
		} 
	}
}
