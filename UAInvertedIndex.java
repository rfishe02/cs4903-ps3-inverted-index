
import java.io.*;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;
import java.nio.charset.*;

/*
  Consider removing termID from application if you do not intend to use it.
*/

public class UAInvertedIndex {

  static final String NA = "NULL";
  static final int DOCID_LEN = 5;
  static final int STR_LEN = 8;
  static final int MAP_LEN = 25;
  static GlobalMap gh;
  static int seed = 5000000;

  static final int RTF_LEN = 8; //0.029304

  public static void main(String[] args) {
    if(args.length < 1) {
      seed = 90000;
      args = new String[2];
      args[0] = "./input2";
      args[1] = "./output";
    }/*************************************************************************/

    try {
      File inDir = new File(args[0]);
      File outDir = new File(args[1]);

      RandomAccessFile stat = new RandomAccessFile(outDir.getPath()+"/stats.raf","rw");
      stat.seek(0);
      stat.writeUTF(NA);

      gh = new GlobalMap(seed); // Initialize global hash table.
      buildInvertedIndex(inDir,outDir,stat);

      stat.writeInt( STR_LEN );
      stat.writeInt( MAP_LEN );
      stat.writeInt( 2 );
      stat.writeInt( 2 );
      stat.writeInt(gh.map.length);
      stat.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  /**
  @param inDir
  @param outDir
  */

  public static void buildInvertedIndex(File inDir, File outDir, RandomAccessFile stat) throws IOException {
    int size = algoOne(inDir,outDir,new File("temp"));
    mergeSort(new File("temp"),size); // Consolidate the temporary files produced by the first algorithm.
    algoTwo(new File("tmp"),outDir,size);

    stat.writeInt(size);

  }

  /**
  @param inDir
  @param outDir
  @param tmpDir
  */

  public static int algoOne(File inDir, File outDir, File tmpDir) {
    SortedMap<String, Integer> ht;  // Used to sort all ht entries by term alphabetically.
    BufferedReader br;
    BufferedWriter bw = null;
    String read;
    int docID = 0;
    int totalFreq;

    System.out.println("running first pass.");
    
    try {
      RandomAccessFile map = new RandomAccessFile(outDir.getPath()+"/map.raf","rw");
      map.seek(0);

      for(File d : inDir.listFiles()) {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(d), "UTF8"));

        ht = new TreeMap<String,Integer>(new TermComparator()); // Initialize a document hash table.
        totalFreq = 0; // Set totalFreq to zero.

        while((read = br.readLine())!=null) {

          //read = new String(read.getBytes("US-ASCII"));
          read = convertText(read,STR_LEN);

          if( ht.containsKey( read ) ) {
            ht.put( read, ht.get( read )+1);
          } else {
            ht.put( read, 1);
          }
          totalFreq++;

        }

        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDir.getPath()+"/doc"+docID+".temp"), "UTF8")); // Open new temporary file f.
        writeTempFile(bw, ht, docID, totalFreq);
        bw.close();  // Close temp file f.

        map.writeUTF( formatString(d.getName(),MAP_LEN) );

        docID++;
        br.close();
      }
      map.close();

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return docID;
  }

  /**
  @param bw
  @param ht
  @param docID
  @param totalFreq
  */

  public static void writeTempFile(BufferedWriter bw, SortedMap<String, Integer> ht, int docID, int totalFreq) throws IOException {
    String word;
    TermData t;

    for(Map.Entry<String,Integer> entry : ht.entrySet()) {
      word = entry.getKey();

      if( ( t = gh.get( word ) ) != null ) {
        t.setCount(t.getCount() + 1);
        //gh.put(t);

      } else {

        t = new TermData(word,1); // put( t, <termID, # documents = 1> )
        gh.put(t);

      } // If a term hasn't been found in prior documents.

      bw.write( formatString( word, STR_LEN, docID, ((double)entry.getValue()/totalFreq) ) + "\n" ); // f.write( t, documentID, (tf / totalFrequency) )

    }  // For all term t in document hash table ht, do this.

  }

  /**
  @param inDir
  @param outDir
  @param size
  */

  public static void algoTwo(File inDir, File outDir, int size) {
    System.out.println("running second pass.");

    TermData t;
    String read = "";
    String top = "";
    int topInd = 0;
    int recordCount = 0;
    int nullCount = 0;
    float rtf;
    float idf;

    try {
      File[] files = inDir.listFiles();

      BufferedReader[] br = new BufferedReader[files.length];
      for(int a = 0; a < files.length; a++) {
        br[a] = new BufferedReader(new InputStreamReader(new FileInputStream(files[a]), "UTF8"));
      }

      RandomAccessFile post = new RandomAccessFile(outDir.getPath()+"/post.raf","rw"); // Create & open a new file for postings, post.raf .
      post.seek(0);

      while(nullCount < br.length) {
        nullCount = 0;
        br[topInd].mark(100);
        top = br[topInd].readLine();

        for(int b = 0; b < br.length; b++) {

          if(b != topInd) {

            br[b].mark(100);

            if(top == null) {
              br[topInd].mark(100);
              nullCount++;

              top = br[b].readLine();
              topInd = b;

            } else if( (read = br[b].readLine()) != null ) {

              if( read.substring(0,STR_LEN).compareTo(top.substring(0,STR_LEN)) < 0 ) {
                br[topInd].reset();
                top = read;
                topInd = b;
              } else {
                if( read.substring(0,STR_LEN).compareTo(top.substring(0,STR_LEN)) == 0 ) {

                  if( Integer.parseInt(read.substring(STR_LEN+1,STR_LEN+1 + DOCID_LEN).trim()) < Integer.parseInt(top.substring(STR_LEN+1,STR_LEN+1 + DOCID_LEN).trim()) ) {
                    br[topInd].reset();
                    top = read;
                    topInd = b;
                  } else {
                    br[b].reset();
                  }

                } else {
                  br[b].reset();
                }
              } // If the two are similiar, compare the document IDs.

            } else {
              br[b].mark(100);
              nullCount++;
            }

          }
        } // find token that is alphabetically first in the buffer.

        //System.out.println(top.substring(0,STR_LEN).trim());

        t = gh.get( top.substring(0,STR_LEN).trim() );

        rtf = (float) Double.parseDouble( top.substring( (STR_LEN+1 + DOCID_LEN), top.length() ) );
        idf = (float) Math.log( (double) size / t.getCount() ); // Calculate inverse document frequency for term from gh(t).numberOfDocuments .

        t.setStart(recordCount);  // Update the start field for the token in the global hash table.
        //gh.put( t );
        
        post.writeInt( Integer.parseInt( top.substring(STR_LEN+1,STR_LEN+1 + DOCID_LEN).trim() ) ); // Write postings record for the token (documentID, termFrequency, OR rtf * idf) .
        post.writeFloat( rtf * idf );
          
        recordCount = recordCount + 1;

      } // While all postings haven't been written do this.

      post.close();

      for(BufferedReader b : br) {
        b.close();
      }

      writeDictionary(outDir);

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  /**
  @param outDir
  */

  public static void writeDictionary(File outDir) throws IOException {
    System.out.println("writing dictionary file.");

    RandomAccessFile dict = new RandomAccessFile(outDir.getPath()+"/dict.raf","rw"); //write global hash table to disk as dictionary file dict.raf
    dict.seek(0);

    String term;
    int count;
    int start;

    for(int i = 0; i < gh.map.length; i++) {

      if(gh.map[i] != null) {
        term = gh.map[i].getT();
        count = gh.map[i].getCount();
        start = gh.map[i].getStart();
      } else {
        term = NA;
        count = -1;
        start = -1;
      }

      dict.writeUTF( formatXString(term,STR_LEN) );
      dict.writeInt( count );
      dict.writeInt( start );
      
    }

    dict.close();
  }

  /** Use an iterative merge sort to combine files. The basis for this sort is
      directly from the bottom up sort shown on Wikipedia. However, the merge
      portion is different from the example on the website. It doesn't iterate over
      the array, it just uses the values p and q to combine files.
      @param inDir
      @param n
  */

  public static void mergeSort(File inDir, int n) {
    System.out.println("merging temporary files.");

    BufferedReader L = null;
    BufferedReader R = null;
    BufferedWriter bw = null;
    int size;

    File[] A = inDir.listFiles();
    int len = Math.min(500,(int)Math.log(A.length,2));

    try {
      for(int c = 1; c < n; c = 2 * c) {

        size = (int)( (double)(n-1) / c );

        if( size > len ) {
          for(int p = 0; p < n-1; p += 2 * c) {

            int q = Math.min(p + (c-1), n-1);
            int r = Math.min(p + 2*(c-1), n-1);

            if( (q+1) < A.length ) {
              merge(A, L, R, bw, p, q, r);
            }
          }
        } else {
          break;  // Stop when the split reaches a certain size.
        }
      }

    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /** Instead of iterating accross the whole array, it opens the files at p and q+1,
     and merges them. It erases the previous files and stores the new file at A[p].

     If we have five files, it may merge files until it merges the
     files at index 0 and 4. The last merge creates a file that holds the data of all
     other files.

     p: 0  q: 1 --> merge the files at index zero and one.
     p: 2  q: 3 --> merge the files at index two and three.
     p: 0  q: 2 --> merge the files at index zero and two.
     p: 0  q: 4 --> merge the files at index zero and four.
     @param A
     @param L
     @param R
     @param bw
     @param p
     @param q
     @param r
  */

  public static void merge(File[] A, BufferedReader L, BufferedReader R, BufferedWriter bw, int p, int q, int r) throws IOException {
    int z = z = q + 1;
    String filename = "tmp/"+p+""+z+""+r+".tmp";

    L = new BufferedReader(new InputStreamReader(new FileInputStream(A[p]), "UTF8")); // Open the files at the given indices.
    R = new BufferedReader(new InputStreamReader(new FileInputStream(A[z]), "UTF8"));

    bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF8"));  // Create a new file, which will contain the merged data.

    String s1 = L.readLine();
    String s2 = R.readLine();

    while(s1 != null && s2 != null) {

      if( s1.substring(0,STR_LEN).compareTo(s2.substring(0,STR_LEN)) < 0 ) {
        bw.write(s1+"\n");
        s1 = L.readLine();
      } else {
        if( s1.substring(0,STR_LEN).compareTo(s2.substring(0,STR_LEN)) == 0 ) {
          if( Integer.parseInt(s1.substring(STR_LEN+1,STR_LEN+1 + DOCID_LEN).trim()) < Integer.parseInt(s2.substring(STR_LEN+1, STR_LEN+1 + DOCID_LEN).trim()) ) {
            bw.write(s1+"\n");
            s1 = L.readLine();
          } else {
            bw.write(s2+"\n");
            s2 = R.readLine();
          }
        } else {
          bw.write(s2+"\n");
          s2 = R.readLine();
        }
      }

    } // Compare the lines of the files.

    while(s1 != null) {
      bw.write(s1+"\n");
      s1 = L.readLine();
    } // Write any remaining lines to the file.

    while(s2 != null) {
      bw.write(s2+"\n");
      s2 = R.readLine();
    }

    L.close();
    R.close();
    bw.close();

    if(A[p].exists()) {
      A[p].delete();
    } // Remove the original files.
    if(A[z].exists()) {
      A[z].delete();
    }

    A[p] = new File(filename); // Replace the file at A[p]. Future merges will use the newly merged file.
  }

  /**
  @param s1
  @param s2
  */

  static class TermComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  }
  
  /**
  @param stem 
  @param str 
  @param limit
  */

  public static String convertText(String s, int limit) {
    String out = "";
    int len;
    
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

    if(out.length() > 7) {
      out = out.substring(0,8);
    }
    
    return out;
  }

  /**
  @param str
  @param limit
  */

  public static String formatString(String str, int limit) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+limit+"s",str);
  }

  /**
  @param str
  @param limit
  @param id
  @param rtf
  */

  public static String formatString(String str, int limit, int id, double rtf) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+STR_LEN+"s %-"+DOCID_LEN+"d %-"+(RTF_LEN/2)+"."+(RTF_LEN/2)+"f",str,id,rtf);
  }

  /**
  @param str
  @param limit
  */

  public static String formatXString(String str, int limit) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+limit+"s",str);
  }

}
