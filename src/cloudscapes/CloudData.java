package cloudscapes;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.lang.Math.*;
import java.util.concurrent.*;

public class CloudData {
	
	/** in-plane regular grid of wind vectors, that evolve over time**/
	public static Vector [][][] advection;
	/**vertical air movement strength, that evolves over time**/ 
	public static float [][][] convection;
	/**cloud type per grid point, evolving over time**/
	public static int [][][] classification;
	/**data dimension**/
	public static int dimx, dimy, dimt;
	/**start time of part of code - used for timing **/
	public static long startTime = 0;
	
	/**Begin timing a block of code**/
	public static void tick(){
		startTime = System.currentTimeMillis();
	}
	/**Finish timing a block of code
	 * @return the time taken for a block of code to run**/
	public static float tock(){
		return (System.currentTimeMillis() - startTime) / 1000.0f; 
	}

	/**Overall number of elements in the timeline grids
	 * @return The overall number of elements in the timeline grids**/
	public static int dim(){
		return dimt*dimx*dimy;
	}

	
	/**Read cloud simulation data from file
	 * <p>
	 * @param fileName name of file to be loaded for processing
	 * **/
	public static void readData(String fileName){ 
		try{ 
			Scanner sc = new Scanner(new File(fileName), "UTF-8").useLocale(Locale.US);
			
			// input grid dimensions and simulation duration in timesteps
			dimt = sc.nextInt();
			dimx = sc.nextInt(); 
			dimy = sc.nextInt();
			
			// initialize and load advection (wind direction and strength) and convection
			advection = new Vector[dimt][dimx][dimy];
			convection = new float[dimt][dimx][dimy];
			for(int t = 0; t < dimt; t++)
				for(int x = 0; x < dimx; x++)
					for(int y = 0; y < dimy; y++){
						advection[t][x][y] = new Vector();
						advection[t][x][y].x = sc.nextFloat();
						advection[t][x][y].y = sc.nextFloat();
						convection[t][x][y] = sc.nextFloat();
					}
			
			classification = new int[dimt][dimx][dimy];
			sc.close(); 
		} 
		catch (IOException e){ 
			System.out.println("Unable to open input file "+fileName);
			e.printStackTrace();
		}
		catch (java.util.InputMismatchException e){ 
			System.out.println("Malformed input file "+fileName);
			e.printStackTrace();
		}
	}
	
	/**Write classification output to file
	 * <p>
	 * @param fileName name of file that results will be written to
	 * @param wind the wind vector calculated useing the calcAvg method
	 * **/
	public static void writeData(String fileName, Vector wind){
		 try{ 
			 FileWriter fileWriter = new FileWriter(fileName);
			 PrintWriter printWriter = new PrintWriter(fileWriter);
			 printWriter.printf("%d %d %d\n", dimt, dimx, dimy);
			 printWriter.printf(String.format(Locale.US, "%f %f\n", wind.x, wind.y));
			 
			 for(int t = 0; t < dimt; t++){
				 for(int x = 0; x < dimx; x++){
					for(int y = 0; y < dimy; y++){
						printWriter.printf("%d ", classification[t][x][y]);
					}
				 }
				 printWriter.printf("\n");
		     }
				 
			 printWriter.close();
		 }
		 catch (IOException e){
			 System.out.println("Unable to open output file "+fileName);
				e.printStackTrace();
		 }
	}
	
	/**Calculate the average wind vector for all air layer elements and timesteps
	 * @return the average wind vector for all air layer elements and timesteps**/
	public static Vector calcAvg(){
		float avgx = 0;
		float avgy = 0;
		
		for(int t = 0; t < dimt; t++){
			for(int x = 0; x < dimx; x++){
				for(int y = 0; y < dimy; y++){
					avgx += advection[t][x][y].x;
					avgy += advection[t][x][y].y;
				}
			}
		}
		
		avgx = avgx / dim();
		avgy = avgy / dim();
		
		Vector avg = new Vector();
		avg.x = avgx;
		avg.y = avgy;
		
		return avg;
			
	}
	
	static final ForkJoinPool fjPool = new ForkJoinPool();
	
	/**Calculate the average wind vector for all air layer elements and timesteps using parallelization
	 * @return the average wind vector for all air layer elements and timesteps**/
	public static Vector calcAvgParallel(){
		float avgx = 0;
		float avgy = 0;
		
		
		for(int t = 0; t < dimt; t++){
			for(int x = 0; x < dimx; x++){
				avgx += ForkJoinPool.commonPool().invoke(new SumArray( advection[t][x], 0, dimy, "x"));
				avgy += ForkJoinPool.commonPool().invoke(new SumArray( advection[t][x], 0, dimy, "y"));
			}
		}
		
		avgx = avgx / dim();
		avgy = avgy / dim();
		
		Vector avg = new Vector();
		avg.x = avgx;
		avg.y = avgy;
		
		return avg;
			
	}
	
	/**Determine the cloud classification for all air layers and timesteps**/
	public static void classify(){
		float avgx  = 0;
		float avgy  = 0;
		int   count = 0;
		
		for(int t = 0; t < dimt; t++){
			for(int x = 0; x < dimx; x++){
				for(int y = 0; y < dimy; y++){
					
					//calculate average local wind vector
					avgx  = 0;
					avgy  = 0;
					count = 0;
					for(int j= (x-1); j<x+2; j++){
						for(int i = (y-1); i<y+2; i++){
							if (j<0 || j== dimy || i<0 || i== dimx ){
								continue;
							}else{
								avgx += advection[t][j][i].x;
								avgy += advection[t][j][i].y;
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
					if ( Math.abs(convection[t][x][y]) > magnitude ){
						classification[t][x][y] = 0;
					}else if ( magnitude > 0.2 && magnitude >= Math.abs(convection[t][x][y]) ){
						classification[t][x][y] = 1;
					}else{
						classification[t][x][y] = 2;
					}
		
				}
			}
		}
	}
	
	/**Determine the cloud classification for all air layers and timesteps using parallelization**/
	public static void classifyParallel(){
		float avgx  = 0;
		float avgy  = 0;
		int   count = 0;
		
		for(int t = 0; t < dimt; t++){
			for(int x = 0; x < dimx; x++){
				classification[t][x] = ForkJoinPool.commonPool().invoke(new SumClassify(advection[t],convection[t][x],0, dimy, dimx, dimy, x));
			}
		}
	}
	
	/**Navigates between various methods depending on 
	 * the arguments passed when running CloudData from the command line.
	 * <p>
	 * @param args[0] the input file to be processed.
	 * @param args[1] the output file which results will be written to.
	 * @param args[2] string that denotes the parallel or serial algorithms will be run.
	 *  **/
	public static void main(String args[]){
		String inputPath = args[0];
		String outputPath = args[1];
		String type;
		if (args.length > 2){
			type = args[2];
		}else
			type = "serial";
		
		readData(inputPath);
		Vector windAvg = new Vector();
		float time = 0;
		System.gc();
		if (type.equals("serial")){
			tick();
			classify();
			windAvg = calcAvg();
			time = tock();
		}else if(type.equals("parallel")){
			tick();
			classifyParallel();
			windAvg = calcAvgParallel();
			time = tock();
		}
		
		/*int[]  res = ForkJoinPool.commonPool().invoke(new SumClassify(0,100));
		for(int i : res){
			System.out.println(i);
		}*/
		
		System.out.println("Run time: " + time);
		writeData(outputPath, windAvg);
	
	}
}
