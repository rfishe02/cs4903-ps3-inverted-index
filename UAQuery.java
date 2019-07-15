
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.nio.charset.*;

public class UAQuery {

  static final String NA = "NULL";
  static final int DICT_LEN = 8+8+8+6;
  //static final int STR_LEN = 8;
  static final int POST_LEN = 4+4;
  static final int MAP_LEN = 25;
  static int seed;

  /**
  The same hash function used to construct the global hash table.
  @param str  A key.
  @param i An index.
  @return A hashcode for a given String.
  */

  public static int hash(String str, int i, int n) {
    return ( Math.abs(str.hashCode()) + i ) % n;
  } // h(k,i) = (h'(k) + i) mod m

  public static void main(String[] args) {
    if(args.length < 1) {
      String[] test = {"input","input","output","cat","video","youtube"};
      args = test;
    }/*************************************************************************/

    File inDir = new File(args[0]);
    File outDir = new File(args[1]);
    File rafDir = new File(args[2]);

    try {
      RandomAccessFile stat = new RandomAccessFile(rafDir.getPath()+"/stats.raf","rw");
      stat.seek(0);

      seed = stat.readInt();
      stat.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    runQuery(inDir,outDir,rafDir,args);

  }

  /**
  The function used to process a query and return a list of results.
  @param query A query as an array of words.
  @return A list of the top results for the given query.
  */

  public static String[] runQuery(File inDir, File outDir, File rafDir, String[] query) {
    String[] result = null;

    try {
      HashMap<Integer,Integer> docMap = new HashMap<>();
      HashMap<String,Integer> termMap = new HashMap<>();
      HashSet<String> q = new HashSet<>();

      mapRowsCols(inDir,outDir,rafDir,termMap,docMap,q,query);
      float[][] tdm = buildTDM(rafDir,termMap,docMap,q);

      //printTDM(tdm);

      result = getDocs(rafDir,docMap,tdm,5);

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

  public static void mapRowsCols(File inDir, File outDir, File rafDir, HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> q, String[] query) throws IOException {
    System.out.println("mapping terms and documents to rows and columns.");

    RandomAccessFile dict = new RandomAccessFile(rafDir.getPath()+"/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile(rafDir.getPath()+"/post.raf","rw");
    RandomAccessFile map = new RandomAccessFile(rafDir.getPath()+"/map.raf","rw");

    BufferedReader br;
    String read;
    String record;
    byte[] term;
    int row = 0;
    int col = 1; // Reserve first column for the query.
    int count;
    int start;
    int docID;
    int i;

    String[] spl;

    for(int a = 2; a < query.length; a++) {

      i = 0;  // Find the term in the dictionary.
      do {
        dict.seek( hash(query[a],i,seed) * (DICT_LEN + 2) );

        record = dict.readUTF();
        spl = record.split("(\\s|\\p{Space}|\u0020)+");

        /*
        term = new byte[STR_LEN];
        dict.read(term);
        record = new String(term);*/

        i++;
      } while( i < seed && spl[1].trim().compareToIgnoreCase(NA) != 0 && spl[1].trim().compareToIgnoreCase(query[a]) != 0);
      //while( i < seed && record.trim().compareToIgnoreCase(NA) != 0 && record.trim().compareToIgnoreCase(query[a]) != 0);

      //if(record.trim().compareToIgnoreCase(NA) != 0)
      if( spl[1].trim().compareTo(NA) != 0 ) {
        if(!termMap.containsKey(query[a])) {
          termMap.put(query[a],row);
          row++;
        } // Map terms to rows.

        if(!q.contains(query[a])) {
          q.add(query[a]);
        } // Add terms in query to HashSet.

        count = Integer.parseInt(spl[2]);
        start = Integer.parseInt(spl[3]);

        /*count = dict.readInt();
        start = dict.readInt();*/

        post.seek(((start-count)+1) * POST_LEN);
        for(int x = 0; x < count; x++) {
          docID = post.readInt();
          post.readFloat(); // rtfIDF

          if(!docMap.containsKey(docID)) {
            docMap.put(docID,col);
            col++;
          } // Map document ID to a column.

          map.seek(docID * (MAP_LEN + 2));
          String filename = map.readUTF();
          br = new BufferedReader(new InputStreamReader(new FileInputStream( outDir.getPath()+"/"+filename.trim() ), "UTF8"));

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

  public static float[][] buildTDM(File rafDir, HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> query) throws IOException {
    System.out.println("building the term document matrix.");

    RandomAccessFile dict = new RandomAccessFile(rafDir.getPath()+"/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile(rafDir.getPath()+"/post.raf","rw");
    float[][] tdm = new float[termMap.size()][docMap.size()+1]; // Add the query column.
    String record;
    byte[] term;
    float rtfIDF;
    int count;
    int start;
    int docID;
    int i;

    String[] spl;

    for( Map.Entry<String,Integer> entry : termMap.entrySet() ) {
      i = 0;
      do {
        dict.seek( hash(entry.getKey(),i,seed) * (DICT_LEN + 2) );

        record = dict.readUTF();
        spl = record.split("(\\s|\\p{Space}|\u0020)+");

        /*
        term = new byte[STR_LEN];
        dict.read(term);
        record = new String(term);*/

        System.out.println(record+" END OF RECORD");

        i++;
      } while( i < seed && spl[1].trim().compareToIgnoreCase(NA) != 0 && spl[1].trim().compareToIgnoreCase(entry.getKey()) != 0);
      //while( i < seed && record.trim().compareToIgnoreCase(NA) != 0 && record.trim().compareToIgnoreCase(entry.getKey()) != 0);

      // if(record.trim().compareToIgnoreCase(NA) != 0)
      if( spl[1].trim().compareTo(NA) != 0 ) {

        count = Integer.parseInt(spl[2]);
        start = Integer.parseInt(spl[3]);

        /*
        count = dict.readInt();
        start = dict.readInt();*/

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

  public static String[] getDocs(File rafDir, HashMap<Integer,Integer> docMap, float[][] tdm, int k)  throws IOException {
    System.out.println("finding relevant documents.");

    RandomAccessFile map = new RandomAccessFile(rafDir.getPath()+"/map.raf","rw");
    map.seek(0);
    PriorityQueue<Result> pq = new PriorityQueue<>(new ResultComparator());
    String[] res = new String[k];

    for( Map.Entry<Integer,Integer> entry : docMap.entrySet() ) {
      map.seek(entry.getKey() * (MAP_LEN + 2));
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

  /**
  @param tdm
  @param d
  @param q
  */

  public static float calcCosineSim(float[][] tdm, int d, int q) {
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

  public static void printTDM(float[][] tdm) {
    for(int a = 0; a < tdm.length; a++) {
      System.out.printf("[ %-3s ] ",a);
      for(int b = 0; b < tdm[0].length; b++) {
        System.out.printf("%-3.2f ",tdm[a][b]);
      }
      System.out.println();
    }
  }

  /** */

  static class Result {
    float score;
    String name;

    public Result(float score, String name) {
      this.score = score;
      this.name = name;
    }
  }

  /** */

  static class ResultComparator implements Comparator<Result> {
    public int compare(Result s1, Result s2) {
      if(s1.score > s2.score) {
        return -1;
      } else if(s1.score < s2.score) {
        return 1;
      } else {
        return 0;        }
    }
  }

}
