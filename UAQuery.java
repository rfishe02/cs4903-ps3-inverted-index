
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class UAQuery {

  final int DICT_LEN = 8+4+4;
  final int POST_LEN = 4+4;
  final int DOC_LEN = 25;
  final int seed = 5000;

  /**
  The same hash function used to construct the global hash table.
  @param str  A key.
  @param i An index.
  @return A hashcode for a given String.
  */

  public int hash(String str, int i) {
    return ( Math.abs(str.hashCode()) + i ) % seed;
  }

  /**
  The function used to process a query and return a list of results.
  @param query A query as an array of words.
  @return A list of the top results for the given query.
  */

  public String[] runQuery(String[] query) {
    String[] result = null;

    try {
      HashMap<Integer,Integer> docMap = new HashMap<>();
      HashMap<String,Integer> termMap = new HashMap<>();
      HashSet<String> q = new HashSet<>();

      mapRowsCols(termMap,docMap,q,query);
      float[][] tdm = buildTDM(termMap,docMap,q);

      //printTDM(tdm);

      result = getDocs(docMap,tdm,5);

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return result;
  }

  /**
  Use the query and raf files to map terms and document IDs to rows and columns. This information
  will be used to build the term document matrix.
  @param termMap A hash table that maps terms to rows in the term document matrix.
  @param docMap A hash table that maps document IDs to columns in the term document matrix.
  @param q A hash set that will contain all distinct words in the query.
  @param query A query as an array of words.
  */

  public void mapRowsCols(HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> q, String[] query) throws IOException {
    RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
    RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");
    BufferedReader br;
    String read;
    String record;
    int row = 0;
    int col = 1; // Reserve first column for the query.
    int count;
    int start;
    int docID;
    int i;

    for(String s : query) {
      i = 0;
      do {
        dict.seek(hash(s,i) * (DICT_LEN+2));
        record = dict.readUTF();
        i++;
      } while(record.trim().compareTo("NA") != 0 && record.trim().compareTo(s) != 0); // Find the term in the dictionary.

      if(record.trim().compareTo("NA") != 0) {
        if(!termMap.containsKey(s)) {
          termMap.put(s,row);
          row++;
        } // Map terms to rows.

        if(!q.contains(s)) {
          q.add(s);
        } // Add terms in query to HashSet.

        count = dict.readInt();
        start = dict.readInt();

        post.seek(((start-count)+1) * POST_LEN);
        for(int x = 0; x < count; x++) {
          docID = post.readInt();
          post.readFloat(); // rtfIDF

          if(!docMap.containsKey(docID)) {
            docMap.put(docID,col);
            col++;
          } // Map document ID to a column.

          map.seek(docID * (DOC_LEN + 2));
          br = new BufferedReader(new FileReader("input/"+map.readUTF()));

          while((read=br.readLine())!=null) {
            if(!termMap.containsKey(read)) {
              termMap.put(read,row);
              row++;
            }
          } // Open file & map terms within to columns.

          br.close();

        } // Read each posting for the term.
      }
    } // Map terms & documets to columns.

    dict.close();
    post.close();
    map.close();
  }

  /**
  Use hash tables to construct the term document matrix.
  @param termMap A hash table that maps terms to rows in the term document matrix.
  @param docMap A hash table that maps document IDs to columns in the term document matrix.
  @param query A hash set that will contain all distinct words in the query.
  */

  public float[][] buildTDM(HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> query) throws IOException {
    float[][] tdm = new float[termMap.size()][docMap.size()+1]; // Add the query column.

    RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
    String record;
    float rtfIDF;
    int count;
    int start;
    int docID;
    int i;

    for( Map.Entry<String,Integer> entry : termMap.entrySet()) {
      i = 0;
      do {
        dict.seek( hash( entry.getKey() , i ) * (DICT_LEN+2) );
        record = dict.readUTF();
        i++;
      } while(record.trim().compareTo( entry.getKey() ) != 0); // Find the term in the dictionary.

      count = dict.readInt();
      start = dict.readInt();

      if( query.contains( entry.getKey() ) ) {
        tdm[ entry.getValue() ][ 0 ] = count; // Need to determine the correct value, ie: TF-IDF.
      }

      post.seek(((start-count)+1) * POST_LEN);
      for(int x = 0; x < count; x++) {
        docID = post.readInt();
        rtfIDF = post.readFloat();

        if(docMap.containsKey(docID)) {
          tdm[ entry.getValue() ][ docMap.get(docID) ] = rtfIDF;
        }
      } // Read each posting for the term.
    }

    dict.close();
    post.close();

    return tdm;
  }

  /**
  @param docMap
  @param tdm
  @param k
  */

  public String[] getDocs(HashMap<Integer,Integer> docMap, float[][] tdm, int k)  throws IOException {
    PriorityQueue<Result> pq = new PriorityQueue<>(new ResultComparator());
    RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");
    String[] res = new String[k];

    for( Map.Entry<Integer,Integer> entry : docMap.entrySet() ) {
      map.seek(entry.getKey() * (DOC_LEN + 2));
      pq.add(new Result( calcCosineSim(tdm, entry.getValue(), 0), map.readUTF() ));
    }

    int j = 0;
    while(j < k && !pq.isEmpty()) {
      res[j] = pq.remove().name;
      System.out.println(res[j]);
      j++;
    }

    map.close();
    return res;
  }

  /** */

  class Result {
    float score;
    String name;

    public Result(float score, String name) {
      this.score = score;
      this.name = name;
    }
  }

  /** */

  class ResultComparator implements Comparator<Result> {
    public int compare(Result s1, Result s2) {
      if(s1.score > s2.score) {
        return -1;
      } else if(s1.score < s2.score) {
        return 1;
      } else {
        return 0;        }
    }
  }

  /**
  @param tdm
  @param d
  @param q
  */

  public float calcCosineSim(float[][] tdm, int d, int q) {
    double one = 0.0;
    double two = 0.0;
    double tot = 0.0;

    for(int i = 0; i < tdm.length; i++) {
      tot += ( tdm[i][d] * tdm[i][q] );
      one += Math.pow( tdm[i][d],2 );
      two += Math.pow( tdm[i][q],2 );
    }

    return (float)( (tot) / (Math.sqrt(one) * Math.sqrt(two)) );
  }

  /**
  @param tdm
  */

  public void printTDM(float[][] tdm) {
    for(int a = 0; a < tdm.length; a++) {
      System.out.printf("[ %-3s ] ",a);
      for(int b = 0; b < tdm[0].length; b++) {
        System.out.printf("%-3.2f ",tdm[a][b]);
      }
      System.out.println();
    }
  }

}
