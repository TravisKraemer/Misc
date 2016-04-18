package radixsort;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a simple radix sort that I wrote for fun. It definitely is not 
 * the a well optimized radix sort.
 * @author Travis Kraemer
 */
public class RadixSort
{

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args)
   {   
      final int NUMSORT = 1000000;
      final int NUMITER = 10;
      runVerify();
      System.out.println("Single Thread");
      benchmark(NUMSORT, NUMITER, false);
      System.out.println("Dual Thread");
      benchmark(NUMSORT, NUMITER, true);
   }
   
   /**
    * Tests both dual threaded and single threaded sort with 100000 elements.
    * Prints good if it correct, false otherwise.
    */
   static public void runVerify()
   {
        final int NUMSORT = 100000;
        if(verifyCorrect(NUMSORT, false) && verifyCorrect(NUMSORT, true))
        {
            System.out.println("Good");
        }
        else
        {
            System.out.println("Bad");
        }
   }
   
   /**
    * Uses my radix sort to sort the array and checks it against 
    * Arrays sort method to make sure it gets the correct result
    * @param numSort - Size of array to use
    * @param dualThread - Use dual thread or single thread
    * @return true if they are the same, false otherwise
    */
   public static boolean verifyCorrect(int numSort, boolean dualThread)
   {
        Random rand = new Random();
        long[] array = new long[numSort];
        long[] array2 = new long[numSort];
        for(int numBits = 2; numBits < 18; numBits++)
        {
            for(int j = 0; j < numSort; j++)
            {
               array2[j] = array[j] = rand.nextLong();
            }
            array = sort(array, numSort, numBits, dualThread);
            Arrays.sort(array2);
            for(int i = 0; i < numSort; i++)
            {
                if(array[i] != array2[i])
                {
                    return false;
                }
            }
        }
        return true;
   }
   
   /**
    * Times how long it takes to sort the random array
    * @param numSort - Number of elements in the array
    * @param numIter - Number of times to repeat sorting a random array
    * @param dualThread - Specify whether to use two threads when sorting
    */
   public static void benchmark(int numSort, int numIter, boolean dualThread)
   {
       Random rand = new Random();
       long[] array = new long[numSort];
       for(int numBits = 2; numBits < 18; numBits++)
       {
            double totalDuration = 0.0;
            for(int i = 0; i < numIter; i++)
            {
                for(int j = 0; j < numSort; j++)
                {
                   array[j] = rand.nextLong();
                }
                long startTime = System.nanoTime();
                array = sort(array, numSort, numBits, dualThread);
                long endTime = System.nanoTime();
                totalDuration+= ((double)(endTime - startTime)) / 1000000000;
            }
            System.out.println("# Bits: " + numBits + " Time: " + totalDuration);
      }
   }

   /**
    * Sorts an array of longs
    * @param ar - The array to be sorted
    * @param size - Number of elements in the array
    * @param group - The number of bits to use at a time when sorting
    * @param dualThread - Specifies whether to use two threads when sorting
    * @return Reference to sorted array
    */
   public static long[] sort(long[] ar, int size, int group, boolean dualThread)
   {
        long[] pos = new long[size];
        int posCount = 0;
        int negCount = 0;
        for(int i = 0; i < size; i++)
        {
           if((ar[i] & ((long) 1 << 63)) == 0)
           {
              pos[posCount++] = ar[i];
           }
           else
           {
              ar[negCount++] = ar[i];
           }
        }
        if(!dualThread)
        {
            ar = subSort(ar, negCount, group, size);
            pos = subSort(pos, posCount, group, posCount);
        }
        else
        {
            SortThread negThread = new SortThread(ar, negCount, group, size);
            negThread.start();
            pos = subSort(pos, posCount, group, posCount);
            try 
            {
                negThread.join();
            } 
            catch (InterruptedException ex) 
            {
                 Logger.getLogger(RadixSort.class.getName()).log(Level.SEVERE, null, ex);
            }
            ar = negThread.getArray();
        }
        System.arraycopy(pos, 0, ar, negCount, posCount);
        return ar;
   }
   
   /**
    * Class for a thread that sorts a part of an array of longs
    */
    static public class SortThread extends Thread 
    {
        private long[] ar;
        private int size;
        private int bitGroup;
        private int aSize;
        
        /**
         * Returns reference to sorted arrray
         * @return sorted long[] reference
         */
        public long[] getArray()
        {
            return ar;
        }
        
        /**
         * Initialize data for run
         * @param ar - array to sort
         * @param size - size of array to sort
         * @param bitGroup - number of bits to use for buckets
         * @param aSize - length of output array
         */
        public SortThread(long[] ar, int size, int bitGroup, int aSize) 
        {
            this.ar = ar;
            this.size = size;
            this.bitGroup = bitGroup;
            this.aSize = aSize;
        }

        /**
         * Sorts the array
         */
        public void run() 
        {
            long[] temp = new long[aSize]; //For larger values
            int groups = 2 << (bitGroup - 1);
            long mask = 0xFFFFFFFF >>> (64 - bitGroup);
            int[] counts = new int[groups];
            long[] ar1,ar2;
            ar1 = ar;
            boolean arToTemp = true;
            for(int j = 0; j < 63; j = j + bitGroup)
            {
               if(arToTemp)
               {
                  ar1 = temp;
                  ar2 = ar;
               }
               else
               {
                  ar1 = ar;
                  ar2 = temp;
               }
               arToTemp = !arToTemp;
               for(int i = 0; i < groups; i++)
               {
                  counts[i] = 0;
               }
               for(int i = 0; i < size; i++)
               {
                  counts[(int) ((ar2[i] & (mask << j)) >>> j)]++;
               }
               for(int i = groups - 1; i > 0; i--)
               {
                  counts[i] = counts[i-1];
               }
               counts[0] = 0;
               for(int i = 2; i < groups; i++)
               {
                  counts[i] += counts[i-1];
               }
               for(int i = 0; i < size; i++)
               {
                  int count = (int) ((ar2[i] & (mask << j)) >>> j);
                  int idx = counts[count]++;
                  ar1[idx] = ar2[i];
               }
               if((j + bitGroup) > 63)
               {
                   groups = 2 << (63 - j);
               }
            }
            if(arToTemp)
            {
               System.arraycopy(ar1, 0, temp, 0, size);
            }
            ar = temp;  
        }
    }
    
   /**
   * Single threaded subSort
   * @param ar - array to sort
   * @param size - size of array to sort
   * @param bitGroup - number of bits to use for buckets
   * @param aSize - length of output array
   * @return 
   */
    public static long[] subSort(long[] ar, int size, int bitGroup, int aSize) 
        {
            long[] temp = new long[aSize]; //For larger values
            int groups = 2 << (bitGroup - 1);
            long mask = 0xFFFFFFFF >>> (64 - bitGroup);
            int[] counts = new int[groups];
            long[] ar1,ar2;
            ar1 = ar;
            boolean arToTemp = true;
            for(int j = 0; j < 63; j = j + bitGroup)
            {
               if(arToTemp)
               {
                  ar1 = temp;
                  ar2 = ar;
               }
               else
               {
                  ar1 = ar;
                  ar2 = temp;
               }
               arToTemp = !arToTemp;
               for(int i = 0; i < groups; i++)
               {
                  counts[i] = 0;
               }
               for(int i = 0; i < size; i++)
               {
                  counts[(int) ((ar2[i] & (mask << j)) >>> j)]++;
               }
               for(int i = groups - 1; i > 0; i--)
               {
                  counts[i] = counts[i-1];
               }
               counts[0] = 0;
               for(int i = 2; i < groups; i++)
               {
                  counts[i] += counts[i-1];
               }
               for(int i = 0; i < size; i++)
               {
                  int count = (int) ((ar2[i] & (mask << j)) >>> j);
                  int idx = counts[count]++;
                  ar1[idx] = ar2[i];
               }
               if((j + bitGroup) > 63)
               {
                   groups = 2 << (63 - j);
               }
            }
            if(arToTemp)
            {
               System.arraycopy(ar1, 0, temp, 0, size);
            }
            return temp;  
        }
}
