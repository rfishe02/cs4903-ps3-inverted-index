
package src.main.java;

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

  String NA;
  int size;
  int STR_LEN;
  int MAP_LEN;
  int DICT_LEN;
  int POST_LEN;
  int seed;
  
  public UAQuery(File rafDir, String filename) {
    try {
        RandomAccessFile stat = new RandomAccessFile(rafDir.getPath()+"/"+filename,"r");
        stat.seek(0);
        NA = stat.readUTF();
        size = stat.readInt();
        STR_LEN = stat.readInt();
        MAP_LEN = stat.readInt();
        DICT_LEN = STR_LEN + (stat.readInt() * 4);
        POST_LEN = stat.readInt() * 4;
        seed = stat.readInt();
        stat.close();
    
    } catch(IOException ex) {
        ex.printStackTrace();
        System.exit(1);
    }
  }

  public String[] runQuery(File inDir, File rafDir, String[] query) {
    String[] result = null;

    try {
      HashMap<Integer,Integer> docMap = new HashMap<Integer,Integer>(80000);
      HashMap<String,Integer> termMap = new HashMap<String,Integer>(2000000);
      HashMap<String,Integer> q = new HashMap<String,Integer>(1000);

      mapRowsCols(inDir,rafDir,termMap,docMap,q,query);
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

  public void mapRowsCols(File inDir, File rafDir, HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashMap<String,Integer> q, String[] query) throws IOException {
    System.out.println("mapping terms and documents to rows and columns.");

    RandomAccessFile dict = new RandomAccessFile(rafDir.getPath()+"/dict.raf","r");
    RandomAccessFile post = new RandomAccessFile(rafDir.getPath()+"/post.raf","r");
    RandomAccessFile map = new RandomAccessFile(rafDir.getPath()+"/map.raf","r");

    BufferedReader br;
    String read;
    String record;
    int row = 0;
    int col = 1; // Reserve first column for the query.
    int count;
    int start;
    int docID;
    int i;

    for(int a = 0; a < query.length; a++) {
      query[a] = convertText(query[a],STR_LEN);

      i = 0;  // Find the term in the dictionary.
      do {
        dict.seek( hash(query[a],i,seed) * (DICT_LEN + 2) );
        record = dict.readUTF();

        //System.out.println(record+"END OF RECORD");

        i++;
      } while( i < seed && record.trim().compareToIgnoreCase(NA) != 0 && record.trim().compareToIgnoreCase(query[a]) != 0);

      if(record.trim().compareToIgnoreCase(NA) != 0) {
        if(!termMap.containsKey(query[a])) {
          termMap.put(query[a],row);
          row++;
        } // Map terms to rows.

        if(q.containsKey(query[a])) {
          q.put(query[a],q.get(query[a])+1);
        } else {
          q.put(query[a],1);
        }  // Count the frequency of terms in the query.

        count = dict.readInt();
        start = dict.readInt();

        post.seek(((start-count)+1) * POST_LEN);
        for(int x = 0; x < count; x++) {
          docID = post.readInt();
          post.readFloat(); // rtf

          if(!docMap.containsKey(docID)) {
            docMap.put(docID,col);
            col++;
          } // Map document ID to a column.

          map.seek(docID * (MAP_LEN + 2));
          br = new BufferedReader(new InputStreamReader(new FileInputStream( inDir.getPath()+"/"+map.readUTF().trim() ), "UTF8"));

          while((read=br.readLine())!=null) {
            read = read.replaceAll("[@\\.]", " ");
            read = convertText(read,STR_LEN);

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

  public float[][] buildTDM(File rafDir, HashMap<String,Integer> termMap, HashMap<Integer,Integer> docMap, HashMap<String,Integer> query) throws IOException {
    System.out.println("building the term document matrix of "+termMap.size()+" x "+docMap.size()+" size");

    RandomAccessFile dict = new RandomAccessFile(rafDir.getPath()+"/dict.raf","r");
    RandomAccessFile post = new RandomAccessFile(rafDir.getPath()+"/post.raf","r");
    float[][] tdm = new float[termMap.size()][docMap.size()+1]; // Add the query column.
    String record = NA;
    float rtfIDF;
    int count;
    int start;
    int docID;
    int i;

    for( Map.Entry<String,Integer> entry : termMap.entrySet() ) {
      i = 0;
      do {
        dict.seek( hash(entry.getKey(),i,seed) * (DICT_LEN + 2) );
        record = dict.readUTF();

        //System.out.println(record+"END OF RECORD");

        i++;
      } while( i < seed && record.trim().compareToIgnoreCase(NA) != 0 && record.trim().compareToIgnoreCase(entry.getKey()) != 0);

      if(record.trim().compareToIgnoreCase(NA) != 0) {

        count = dict.readInt();
        start = dict.readInt();

        if( query.containsKey( entry.getKey() ) ) {
          tdm[ entry.getValue() ][ 0 ] = (float) ( query.get( entry.getKey() ) * Math.log(size / count) ); // Need to determine the correct value, ie: TF-IDF.
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

  public String[] getDocs(File rafDir, HashMap<Integer,Integer> docMap, float[][] tdm, int k)  throws IOException {
    System.out.println("finding relevant documents.");

    RandomAccessFile map = new RandomAccessFile(rafDir.getPath()+"/map.raf","r");
    PriorityQueue<Result> pq = new PriorityQueue<Result>( new ResultComparator() );
    String[] res = new String[k];

    for( Map.Entry<Integer,Integer> entry : docMap.entrySet() ) {
      map.seek( entry.getKey() * (MAP_LEN + 2) );
      pq.add( new Result( calcCosineSim(tdm, entry.getValue(), 0), map.readUTF() ) );
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

  /**
  The same hash function used to construct the global hash table.
  @param str  A key.
  @param i An index.
  @return A hashcode for a given String.
  */

  public int hash(String str, int i, int n) {
    return ( Math.abs(str.hashCode()) + i ) % (n-1);
  } // h(k,i) = (h'(k) + i) mod m

  /** */

  public String convertText(Stemmer stem, String str, int limit) {
    String out = "";
    int len;

    String[] spl = str.split("([\\s-&])+");
    for(String s : spl) {
      len = Math.min(s.length(),limit);

      for(int i = 0; i < len; i++) {
        if((int)s.charAt(i) > 127) {
          out += "?";
        } else if ( (int)s.charAt(i) > 47 && (int)s.charAt(i) < 58 ||
          (int)s.charAt(i) > 96 && (int)s.charAt(i) < 123 ) {
          out += s.charAt(i);
        } else if( (int)s.charAt(i) > 64 && (int)s.charAt(i) < 91 ) {
          out += (char)((int)s.charAt(i) + 32);
        }
      }
    }
    
    out = stem.stemString(str);
    
    if(out.length() > 7) {
      out = out.substring(0,8);
    }
    
    return out;
  }
  
  /*
  public String cleanWord(String str) {

    str = str.replaceAll("['’\\.\u2019]|(&#(0)?(39|46|x27|14[56]|18[36]);)",""); // Remove all punctuation.

    if(str.contains("&")) {
      str = str.replaceAll("&([aAeEiIoOuU]([a-z]){3,6}|#[1234]([0-9]){2});","?"); // Substitute accented characters with a '?'.
      str = str.replaceAll("[&]([#])?([0x])? ( ([3-9]){2} | amp | minus | 8722 | 2212 ) [;]"," "); // Replace certain non-punctuation with a space.
    }

    return str;
  }

  public String cleanHTML(String str) {
    return str.replaceAll("[\\.]", " ");
  }

  public String cleanEmail(String str) {
    return str.replaceAll("[@\\.]", " ");
  }

  public String cleanPhone(String str) {
    String tmp = str;
    String[] spl = tmp.split("[\\s\\.-]");

    if(spl.length > 2) {
      tmp = spl[spl.length-3] +""+ spl[spl.length-2] +""+ spl[spl.length-1];
    } else if(spl.length > 1) {
      tmp = spl[spl.length-2] +""+ spl[spl.length-1];
    }

    tmp = tmp.replaceAll("[^0-9]", "");
    return tmp;
  }

  public String cleanPrice() {
    return null; // Still need to figure this out.
  }
  */

  /*
  public static String formatString(Stemmer stem, String[] spl, String str) {
    spl = str.split("([\\s-&])+");

    str = "";
    for(String s : spl) {
      str += s;
    }

    str = stem.stemString(str);
    
    if(str.length() > 7) {
      str = str.substring(0,8);
    }
    
    return str;
  }
  */
  
}
