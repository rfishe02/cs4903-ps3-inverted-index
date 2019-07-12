
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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

    String[] test = {"cat","videos","youtube"};

    runQuery(test);

  }

  public static String[] runQuery(String[] query) {

    String filename;

    try {

      HashMap<Integer,Integer> docMap = new HashMap<>();
      HashMap<String,Integer> termMap = new HashMap<>();
      HashSet<String> q = new HashSet<>();

      mapRowsCols(termMap,docMap,q,query);
      float[][] tdm = buildTDM(termMap,docMap,q);

      //printTDM(tdm);

      getDocs(docMap,tdm,5);

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  public static void mapRowsCols(HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> q, String[] query) throws IOException {
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

  public static float[][] buildTDM(HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> query) throws IOException {
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

  public static String[] getDocs(HashMap<Integer,Integer> docMap, float[][] tdm, int k)  throws IOException {
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

  static class Result {
    float score;
    String name;

    public Result(float score, String name) {
      this.score = score;
      this.name = name;
    }
  }

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

  public static float calcCosineSim(float[][] tdm, int d, int q) {
    double one = 0.0;
    double two = 0.0;
    double tot = 0.0;

    /*
      w_(i,j) * w_(i,q) /
      sqrt( w_(i,j)^2 ) * sqrt( w_(i,q)^2 )
    */

    for(int i = 0; i < tdm.length; i++) {
      tot += ( tdm[i][d] * tdm[i][q] );
      one += Math.pow( tdm[i][d],2 );
      two += Math.pow( tdm[i][q],2 );
    }

    return (float)( (tot) / (Math.sqrt(one) * Math.sqrt(two)) );
  }

  public static void printTDM(float[][] tdm) {
    for(int a = 0; a < tdm.length; a++) {
      System.out.printf("[ %-3s ] ",a);
      for(int b = 0; b < tdm[0].length; b++) {
        System.out.printf("%-3.2f ",tdm[a][b]);
      }
      System.out.println();
    }
  }

  /*
  dict.seek(hash("youtube",0) * (DICT_LEN+2));
  String record = dict.readUTF();
  int count = dict.readInt();
  int start = dict.readInt();

  System.out.println(record+" "+count+" "+start);

  File[] files = new File[count];
  int docID;
  int a = 0;

  post.seek(((start-count)+1) * POST_LEN);
  for(int i = 0; i < count; i++) {
    docID = post.readInt();
    System.out.println(docID+" "+post.readFloat());

    map.seek(docID * (DOC_LEN + 2));
    files[a] = new File("input/"+map.readUTF());
    a++;

  }

  Semantic s = new Semantic();
  ArrayList<String> vocab = s.getVocab(files);
  float[][] tcm = s.buildTermContextMatrix(files,vocab,vocab.size(),8);

  int u = s.wordSearch(vocab, "youtube");
  s.getContext(vocab,tcm,10,u);
  */

  /*
  try {

    Iterator it = mp.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();

      br = new BufferedReader(new FileReader(inDir.getPath()+"/"+(String)pair.getKey()));

      while((read=br.readLine())!=null) {

        if(termMap.get(read)!=null) {
          res[ termMap.get( read ) ][ (int)pair.getValue() ]++;
        }

      }

      br.close()
    }

  } catch(IOException ex) {
    ex.printStackTrace();
    System.exit(1);
  }*/

}
