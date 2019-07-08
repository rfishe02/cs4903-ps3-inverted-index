
import java.io.*;
import java.util.HashMap;

public class UAQuery {

  // Don't create a TCM for all documents, just those in the query.

  static final int DICT_LEN = 8+4+4;
  static final int POST_LEN = 4+4;
  static final int DOC_LEN = 25;
  static final int seed = 5000;

  public static int hash(String str, int i) {
    return ( Math.abs(str.hashCode()) + i ) % seed;
  }

  public static void main(String[] args) {

    String[] test = {"this","is","a","test"};

    runQuery(test);

  }

  public static String[] runQuery(String[] query) {

    try {

      RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");

      dict.seek(hash("youtube",0) * (DICT_LEN+2));
      String record = dict.readUTF();
      int count = dict.readInt();
      int start = dict.readInt();

      System.out.println(record+" "+count+" "+start);

      RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
      RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");

      int docID;

      post.seek(((start-count)+1) * POST_LEN);
      for(int i = 0; i < count; i++) {
        docID = post.readInt();
        System.out.println(docID+" "+post.readFloat());

        map.seek(docID * (DOC_LEN + 2));
        System.out.println(map.readUTF());

      }


      dict.close();
      post.close();
      map.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return null;

  }

  /* METHODS USED TO CONSTRUCT A TCM FOR A LIST OF FILES */

  public static float[][] buildTermContextMatrix(File[] files, HashMap<String,Integer> vocab, int size, int window) {
    BufferedReader br;
    String[] prev;
    String[] next;
    String read;
    int i;
    int ind = 0;

    int[] sum = new int[size + 1];
    float[][] tcm = new float[size][size];

    try {
      for(File f : files) {
        prev = null;
        next = new String[window];
        i = 0;

        br = new BufferedReader(new FileReader(f));
        while((read=br.readLine())!=null) {
          if(!vocab.containsKey(read)) {
            vocab.put(read,ind);
            ind++;
          }

          next[i] = read;
          i++;
          if(i == window) {
            if(prev != null) {
              countTerms(vocab,tcm,sum,prev,next,window,i); // Begin to count overlapping terms.
            }
            i = 0;
            prev = next;
            next = new String[window];
          }
        }
        countTerms(vocab,tcm,sum,prev,next,window,i);

        if(prev != null && i < window) {
          countTerms(vocab,tcm,sum,null,next,window,i);
        }
        br.close();
      }
      weightTerms(tcm,sum);

    } catch(IOException ex) {
        ex.printStackTrace();
        System.exit(1);
    }
    return tcm;
  }

  public static void countTerms(HashMap<String,Integer> vocab, float[][] tcm, int[] sum, String[] prev, String[] next, int pLim, int nLim) {
    String[] comp = (prev == null) ? next : prev;
    int lim = (prev == null) ? nLim : pLim ;
    int off = 0;

    for(int out = 0; out < lim; out++) {
      for(int in = 0; in < lim-out; in++) {

        if(in < lim-(out+1)) {
          addFreq(tcm,sum, vocab.get(comp[in]), vocab.get(comp[in+(out+1)]) );
        }
        if(prev != null && comp[in+off] != null && next[out] != null) {
          addFreq(tcm,sum, vocab.get(comp[in+off]), vocab.get(next[out]) );
        }

      }
      off+=1;
    }
  }

  public static void addFreq(float[][] tcm, int[] sum, int w1, int w2) {
    tcm[ w1 ][ w2 ] ++ ;
    tcm[ w2 ][ w1 ] ++ ;
    sum[ w1 + 1 ] ++; // Count sum, which is used to weight terms.
    sum[ w2 + 1 ] ++;
    sum[0] += 2;
  }

  public static void weightTerms(float[][] tcm, int[] sum) {
    double e = Math.pow(sum[0],0.75);

    for(int row = 0; row < tcm.length; row++) {
      for (int col = 0; col < tcm[0].length; col++) {
        if(tcm[row][col] > 0.00001) {
          tcm[row][col] = (float)calcWeight( tcm[row][col], sum[row+1], sum[col+1], e );
        }
      }
    }
  }

  public static double calcWeight(float a, float b, float c, double e) {
    double v = (double)a / ( b * ( Math.pow( c,0.75 ) / e ) );

    if(v > 0.00001) {
      v = Math.log(v) / Math.log(2);
    }
    v = Math.max(v,0);

    return v;
  }

}
