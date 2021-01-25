package cloudscapes;

import java.util.concurrent.*;
import java.util.*;

public class SumClassify extends RecursiveTask<int[]> {
	/** in-plane regular grid of wind vectors, that evolve over time**/
	public Vector[][] advec;
	/**vertical air movement strength, that evolves over time**/
	public float[] convec;
	/**Starting index of the array to be processed**/
	public int lo;
	/**Closing index of the array to be prossed**/
	public int hi;
	/**Maximum x index up to which processing can be done**/
	public int maxX;
	/**Maximum y index up to which processing can be done**/
	public int maxY;
	/**The x index of the elements being worked on.**/
	public int xval;
	
	/**Creates a SumClassify object with the given parameters.
	 * <p>
	 * @param a Vector array to be processed.
	 * @param c Array of uplift values.
	 * @param l Starting index of the array to be processed.
	 * @param h Closing index of the array to be processed.
	 * @param x Maximum x index up to which processing can be done.
	 * @param y Maximum y index up to which processing can be done.
	 * @param xval The x index of the elements being worked on.
	 * **/
	SumClassify (Vector[][] a, float[] c, int l, int h, int x, int y, int xval){
		this.advec = a;
		this.convec = c;
		this.lo = l;
		this.hi = h;
		this.maxX = x;
		this.maxY = y;
		this.xval = xval;
	}
	
	/**Determines the cloud classification of cells in the array and creates threads to do so.**/
	protected int[] compute(){  // return answer
		if(hi - lo <= 2500) {
			int[] result = new int[hi -lo];
			float avgx;
			float avgy;
			int count;
			for(int y = lo; y < hi; y++){
				
				//calculate average local wind vector
				avgx  = 0;
				avgy  = 0;
				count = 0;
				for(int j= (xval-1); j<xval+2; j++){
					for(int i = (y-1); i<y+2; i++){
						if (j<0 || j== maxY || i<0 || i== maxX ){
							continue;
						}else{
							avgx += advec[j][i].x;
							avgy += advec[j][i].y;
							count++;
						}
					}
				}
				avgx = avgx / count;
				avgy = avgy / count;
				
				Vector avg = new Vector();
				avg.x = avgx;
				avg.y = avgy;
				
				float magnitude = avg.getLength();
				
				//determine cloud classification
				if ( Math.abs(convec[y]) > magnitude ){
					result[y - lo] = 0;
				}else if ( magnitude > 0.2 && magnitude >= Math.abs(convec[y]) ){
					result[y - lo] = 1;
				}else{
					result[y - lo] = 2;
				}
			}
			return result;
		}else{
			SumClassify left  = new SumClassify(advec, convec, lo,(hi+lo)/2, maxX, maxY, xval);
			SumClassify right = new SumClassify(advec, convec, (hi+lo)/2,hi, maxX, maxY, xval);
			
			left.fork();
			int[] rightAns= right.compute();
			int[] leftAns= left.join();
	
			int[] arr = Arrays.copyOf(leftAns, leftAns.length + rightAns.length);
			System.arraycopy(rightAns,0,arr, leftAns.length, rightAns.length);
			
			return arr;
		} 
	}
}
