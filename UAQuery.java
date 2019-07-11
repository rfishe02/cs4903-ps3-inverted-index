
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

      /*
      Build a document term matrix, then use RTFIDF to calculate cosine similarity.
      Built a |V| x |D| matrix, of floats.

      known: |V|
      unknown: |D|
      */

      HashMap<Integer,Integer> docMap = new HashMap<>();
      HashMap<String,Integer> termMap = new HashMap<>();
      HashSet<String> q = new HashSet<>();

      mapRowsCols(termMap,docMap,q,query);
      float[][] tdm = buildTDM(termMap,docMap,q);

      printTDM(tdm);

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
    int col = 1;
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

    docMap.put(-1,0); // Map query column to the first column.

    dict.close();
    post.close();
    map.close();

  } // Either approach this in two stages, or just use a LinkedList.

  public static float[][] buildTDM(HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashSet<String> query) throws IOException {
    float[][] tdm = new float[termMap.size()][docMap.size()];

    RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
    RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");
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

      post.seek(((start-count)+1) * POST_LEN);
      for(int x = 0; x < count; x++) {
        docID = post.readInt();
        rtfIDF = post.readFloat();

        if(docMap.containsKey(docID)) {

          tdm[ entry.getValue() ][ docMap.get(docID) ] = rtfIDF;

          if( query.contains( entry.getKey() ) ) {
            System.out.println( entry.getKey() +" "+entry.getValue()+" "+ rtfIDF);
            tdm[ entry.getValue() ][ 0 ] = rtfIDF;
          }

        }

      } // Read each posting for the term.

    }

    dict.close();
    post.close();
    map.close();

    return tdm;
  }

  public static void printTDM(float[][] tdm) {
    for(int a = 0; a < tdm.length; a++) {
      for(int b = 0; b < tdm[0].length; b++) {
        System.out.printf("%-3.2f ",tdm[a][b]);
      }
      System.out.println();
    }
  }

  public static float calcCosineSimularity(float[][] tdm, int d, int q) {
    double one = 0.0;
    double two = 0.0;
    double tot = 0.0;

    int j = 0;

    for(int i = 0; i < tdm[0].length; i++) {
      //tot += ( tcm[i][d] * tcm[][] );
      //one += Math.pow( tcm[][],2 );
      //two += Math.pow( tcm[][],2 );
    }

    /*
    w_(i,j) * w_(i,q) /
    sqrt( w_(i,j)^2 ) * sqrt( w_(i,q)^2 )
    */
    // for document, cols -- fixed
    // query, rows -- fixed?

    return (float)( (tot) / (Math.sqrt(one) * Math.sqrt(two)) );

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
