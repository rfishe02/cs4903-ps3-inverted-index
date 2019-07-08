
/********************************
Name: Renae Fisher
Username: text05
Problem Set: PS2
Due Date: 6/24/19
********************************/

import java.io.*;
import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.PriorityQueue;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class Semantic {

  /**
    Pre-reads the file to obtain |V|. The vocab will be used to
    find the location of terms in the matrix.

    Uses a TreeMap to arrange distinct terms in alphabetic order,
    which are later copied into an ArrayList.

    The wordSearch method uses a binary search on the ArrayList to find
    an index for a given word.
    @param inDir A directory of tiles, with lists of tokens.
    @return An array of terms arraned in alphabetic order.
  */

  public ArrayList<String> getVocab(File[] files) {
    System.out.println("building vocab");
    ArrayList<String> vocab = null;

    try {
      TreeSet<String> set = new TreeSet<>(new VocabComparator());
      BufferedReader br;
      String read;

      for(File f : files) {

        br = new BufferedReader(new FileReader(f));
        while((read = br.readLine())!=null) {
          set.add(read);
        }

        br.close();
      }

      vocab = new ArrayList<>(set.size());

      Iterator<String> it = set.iterator();
      while(it.hasNext()) {
        vocab.add(it.next());
      }

    } catch(IOException ex) {
        ex.printStackTrace();
        System.exit(1);
    }

    return vocab;
  }

  /**
    This method uses a document to build a term-context matrix. It requires
    an ArrayList of strings arranged in alphabetic order (the vocabulary).

    It packs these terms into segments, which countTerms uses to count contextual terms.
    After the term-context matrix is complete, the method weightTerms calculate the PPMI for
    each value.
    @param inDir A directory of tiles, with lists of tokens.
    @param window The window size for context.
    @return A complete term-context matrix.
  */

  public float[][] buildTermContextMatrix(File[] files, ArrayList<String> vocab, int size, int window) {
    BufferedReader br;
    String[] prev;
    String[] next;
    String read;
    int i;

    System.out.println("building term-context matrix of "+size+" x "+size);

    int[] sum = new int[size + 1];
    float[][] tcm = new float[size][size];

    try {

      for(File f : files) {

        prev = null;
        next = new String[window];
        i = 0;

        br = new BufferedReader(new FileReader(f));
        while((read=br.readLine())!=null) {
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
        } // The case where there are leftover terms.

        br.close();

      }

      weightTerms(tcm,sum);

      //printContextMatrix(vocab,tcm);
      //printSums(vocab,tcm,sum);

    } catch(IOException ex) {
        ex.printStackTrace();
        System.exit(1);
    }

    return tcm;
  }

  /**
    It uses two loops with variables and offset values to find the
    contextual terms for each word in the segment. It also finds the contextual terms
    that overlap between two separate segments. If there is no previous segment, it finds
    the contextual terms within a single segment.
    @param tcm A new term-context matrix.
    @param sum An array to store aggregated frequencies.
    @param prev The last segment created by buildTermContextMatrix.
    @param next The newest segment created by builtTermContextMatrix.
    @param pLim The limit used when the two segments are of equal size.
    @param nLim The limit used when a null value is passed through prev.
  */

  public void countTerms(ArrayList<String> vocab, float[][] tcm, int[] sum, String[] prev, String[] next, int pLim, int nLim) {
    String[] comp = (prev == null) ? next : prev;
    int lim = (prev == null) ? nLim : pLim ;
    int off = 0;

    for(int out = 0; out < lim; out++) {
      for(int in = 0; in < lim-out; in++) {

        if(in < lim-(out+1)) {
          addFreq(tcm,sum, wordSearch(vocab, comp[in]), wordSearch(vocab, comp[in+(out+1)]) );
          //System.out.println(comp[in]+": "+in+"    "+comp[in+(out+1)]+": "+(in+(out+1)));
        }

        if(prev != null && comp[in+off] != null && next[out] != null) {
          addFreq(tcm,sum, wordSearch(vocab, comp[in+off]), wordSearch(vocab, next[out]) );
          //System.out.println(comp[(in+off)]+": "+(in+off)+"    "+comp[out]+": "+out);
        } // If prev is null, do not count overlapping terms.

      }
      off+=1;
    }
  }

  public void addFreq(float[][] tcm, int[] sum, int w1, int w2) {
    tcm[ w1 ][ w2 ] ++ ;
    tcm[ w2 ][ w1 ] ++ ;
    sum[ w1 + 1 ] ++; // Count sum, which is used to weight terms.
    sum[ w2 + 1 ] ++;
    sum[0] += 2;
  }

  /**
    Use a binary search to find the reference to a column for a given term.
    This is an attempt to avoid using a HashMap to map Strings to indices.
    However, it takes logarithmic time rather than constant time.

    The method returns the index of the String as the column reference.
    @param target The desired term in the vocabulary.
    @return The index for a term in the term-context matrix.
  */

  public int wordSearch(ArrayList<String> vocab, String target) {
    int ind = -1;
    int l = 0;
    int m;
    int r = vocab.size()-1;

    while(l <= r) {

      m = (l+r)/2;

      if(target.compareTo(vocab.get(m)) > 0) {
        l = m+1;
      } else if(target.compareTo(vocab.get(m)) < 0) {
        r = m-1;
      } else {
        return m;
      }

    }
    return ind;
  }

  /** Uses PPMI to weight all values in the term-context matrix.

    @param tcm A complete term-context matrix.
    @param sum An array of aggregated frequencies from the term-context matrix.
  */

  public void weightTerms(float[][] tcm, int[] sum) {

    System.out.println("applying weights to frequencies");

    double e = Math.pow(sum[0],0.75);

    for(int row = 0; row < tcm.length; row++) {

      for (int col = 0; col < tcm[0].length; col++) {

        if(tcm[row][col] > 0.00001) {
          tcm[row][col] = (float)getV2( tcm[row][col], sum[row+1], sum[col+1], e );
        } // Only weight the terms above a certain threshold (not zero).

        //System.out.printf("%2.2f ",tcm[row][col]);
      }
      //System.out.println();
    }

  }

  /** Calculates cosine similarity, given two rows in the context-term matrix.
    This is written like the exact formula.

    @param a The numerator of the formula.
    @param b The leftmost term in the denominator.
    @param d The total frequencies of all entries in the term-context matrix.
    @param e A pre-calculated value for the denominator of the rightmost term in the equation.
    @return The value PPMI.
  */

  public double getV(float a, double b, float c, int d, double e) {
    double v = ( (double) a / d )
               / ( (b / d) * ( Math.pow( c,0.75 ) / e ) );

    if(v > 0.00001) {
      v = Math.log(v) / Math.log(2);
    }
    v = Math.max(v,0);

    return v;
  }

  /** Calculates cosine similarity, given two rows in the context-term matrix.
    This version was re-written to avoid extra division.

    @param a The numerator.
    @param b The leftmost term in the denominator.
    @param c The rightmost term in the denominator.
    @param e A pre-calculated value for the denominator of the rightmost term in the equation.
    @return The value PPMI.
  */

  public double getV2(float a, float b, float c, double e) {
    double v = (double)a / ( b * ( Math.pow( c,0.75 ) / e ) );

    if(v > 0.00001) {
      v = Math.log(v) / Math.log(2);
    }
    v = Math.max(v,0);

    return v;
  }

  /** Calculates cosine similarity, given two rows in the context-term matrix.
    @param tcm A complete term-context matrix.
    @param u The word in question.
    @param v A term in the vocabulary.
    @return The value cosine similarity.
  */

  public float calculateSimilarity( float[][] tcm, int u, int v ) {
    double one = 0.0;
    double two = 0.0;
    double tot = 0.0;

    for(int col = 0; col < tcm[0].length; col++) {
      tot += ( tcm[u][col] * tcm[v][col] );
      one += Math.pow( tcm[u][col],2 );
      two += Math.pow( tcm[v][col],2 );
    }

    return (float)((tot) / (Math.sqrt(one) * Math.sqrt(two)));
  }

  /**
    Look through V, the rows of the tcm matrix, to find the contextual words.
    It assumes that the ith row in the tcm matrix represents the ith word in V, which is in alphabetic order.
    This method uses a priority queue to return the k most similiar words.
    @param tcm  A complete term-context matrix.
    @param k The number of desired context words.
    @param u The word in question.
    @return A list of the top k context words.
  */

  public String[] getContext(ArrayList<String> vocab, float[][] tcm, int k, int u) {
    System.out.println("searching for context");

    PriorityQueue<ResultObj> pq = new PriorityQueue<>(new ContextComparator());
    String[] res = new String[k];

    for(int i = 0; i < tcm.length; i++) {
      if(i != u) {
        pq.add(new ResultObj(calculateSimilarity(tcm,u,i),i));
      }
    }

    int j = 0;
    while(j < k && !pq.isEmpty()) {
      res[j] = vocab.get(pq.remove().row);
      System.out.println(res[j]);
      j++;
    }

    return res;
  }

  /** The TreeMap in getVocab uses this class to arrange terms in alphabetic order. */

  static class VocabComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareTo(s2);
      }
  }

  /** The PriorityQueue in getContext uses this class to rank all terms. */

  static class ContextComparator implements Comparator<ResultObj> {
    public int compare(ResultObj s1, ResultObj s2) {
      if(s1.score > s2.score) {
        return -1;
      } else if(s1.score < s2.score) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  /** This class stores the results for the getContext method. */

  static class ResultObj {
    float score;
    int row;

    public ResultObj(float score, int row) {
      this.score = score;
      this.row = row;
    }
  }

  //--------------------------------------------------------

  /** Print the contents of the matrix to the console. */

  public void printContextMatrix(ArrayList<String> vocab, float[][] matrix) {
    System.out.printf("%10s ","");
    for(String s : vocab) {
      System.out.printf("%8s ",s);
    }
    System.out.println();

    int colNum = 0;

    for(int i = 0; i < vocab.size(); i++) {

      colNum = 0;
      for(int j = 0; j < vocab.size(); j++) {

        if(colNum == 0) {
          System.out.printf("%10s ",vocab.get(i));
        }
        colNum++;

        System.out.printf("%8.2f ",matrix[i][j]);
      }
      System.out.println();
    }
  }

  /** Print the contents of the sums array. */

  public void printSums(ArrayList<String> vocab, float[][] tcm, int[] sum) {
    for(int i = 0; i < vocab.size(); i++) {

      for(int j = 0; j < vocab.size(); j++) {

        System.out.println( vocab.get(j) +" ("+j+") " + sum[j+1]
        + " " + vocab.get(i) +" ("+i+") "+ tcm[i][j] );

      }
      System.out.println();
    }
  }

  /*
  public void writeTCM(TCM tcm, String filename) {
    try {
      FileOutputStream file = new FileOutputStream(filename);
      ObjectOutputStream out = new ObjectOutputStream(file);

      out.writeObject(tcm);
      out.close();
      file.close();

    } catch(IOException ex) {
       ex.printStackTrace();
       System.exit(1);
    }
  }

  public TCM loadTCM(String filename) {
    TCM tcm = null;

    try {
      FileInputStream file = new FileInputStream(filename);
      ObjectInputStream in = new ObjectInputStream(file);

      tcm = (TCM)in.readObject();
      in.close();
      file.close();

    } catch(Exception ex) {
        ex.printStackTrace();
        System.exit(1);
    }
    return tcm;
  }*/

}
