
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
  
  /*
  dict.raf
    8 bytes (string) <-- term
    -- 4 bytes (int)    <-- termID
    4 bytes (float)  <-- termFrequency / RTF
    4 bytes (int)    <-- start

  post.raf
    4 bytes (int)   <-- documentID
    4 bytes (float) <-- RTF*IDF

  map.raf
    -- 4 bytes (int)     --> docID
    25 bytes (string) --> filename
  */

  static final int STR_LEN = 8;
  static final int DOCID_LEN = 5;
  static final int RTFIDF_LEN = 8; //0.029304
  static final int DOC_LEN = 25;

  static GlobalMap gh;
  static int seed = 2000000;

  public static void main(String[] args) {
    if(args.length < 1) {
      seed = 90000;
      args = new String[2];
      args[0] = "./input";
      args[1] = "./output";
    }/*************************************************************************/

    try {
      File inDir = new File(args[0]);
      File outDir = new File(args[1]);

      gh = new GlobalMap(seed); // Initialize global hash table.
      buildInvertedIndex(inDir,outDir);

      RandomAccessFile stat = new RandomAccessFile(outDir.getPath()+"/stats.raf","rw");
      stat.seek(0);
      stat.writeInt(gh.map.length);
      stat.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public static void buildInvertedIndex(File inDir, File outDir) {

    int size = algoOne(inDir,outDir,new File("temp"));

    mergeSort(new File("temp"),size); // Consolidate the temporary files produced by the first algorithm.
    algoTwo(new File("tmp"),outDir,size);

  }

  public static int algoOne(File inDir, File outDir, File tmpDir) {
    System.out.println("running first pass.");

    SortedMap<String, Integer> ht;  // Used to sort all ht entries by term alphabetically.
    BufferedReader br;
    BufferedWriter bw = null;
    String read;
    int docID = 0;
    int termID = 0;
    int totalFreq;

    try {
      RandomAccessFile map = new RandomAccessFile(outDir.getPath()+"/map.raf","rw");
      map.seek(0);

      for(File d : inDir.listFiles()) {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(d), "UTF8"));

        ht = new TreeMap<String,Integer>(new TermComparator()); // Initialize a document hash table.
        totalFreq = 0; // Set totalFreq to zero.

        while((read = br.readLine())!=null) {

          read = new String(read.getBytes(), Charset.forName("UTF-8"));

          if(read.length() > STR_LEN) {
            read = read.substring(0,STR_LEN);
          }

          if( ht.containsKey( read ) ) {
            ht.put( read, ht.get( read )+1);
          } else {
            ht.put( read, 1);
          }
          totalFreq++;
        }
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDir.getPath()+"/doc"+docID+".temp"), "UTF8")); // Open new temporary file f.
        termID = writeTempFile(bw, ht, docID, termID, totalFreq);
        bw.close();  // Close temp file f.

        //map.writeInt(docID);
        map.writeUTF( formatString(d.getName(),DOC_LEN) );

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

  public static int writeTempFile(BufferedWriter bw, SortedMap<String, Integer> ht, int docID, int termID, int totalFreq) throws IOException {

    String out;
    TermData t;

    for(Map.Entry<String,Integer> entry : ht.entrySet()) {

      if( ( t = gh.get( entry.getKey() ) ) != null ) {
        t.setCount(t.getCount() + 1);
        //gh.put(t);

      } else {

        t = new TermData(entry.getKey(),termID,1); // put( t, <termID, # documents = 1> )
        gh.put(t);
        termID = termID + 1;

      } // If a term hasn't been found in prior documents.

      bw.write( formatString( entry.getKey(), STR_LEN, docID, ((float)entry.getValue()/totalFreq) ) + "\n" ); // f.write( t, documentID, (tf / totalFrequency) )

    }  // For all term t in document hash table ht, do this.

    return termID;
  }

  public static void algoTwo(File inDir, File outDir, int size) {
    System.out.println("running second pass.");

    TermData t;
    String read = "";
    String top = "";
    int topInd = 0;
    int recordCount = 0;
    int nullCount = 0;
    float rtfIDF;

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
        t.setStart(recordCount);  // Update the start field for the token in the global hash table.
        //gh.put( t );

        rtfIDF = (float) ( Double.parseDouble( top.substring( (STR_LEN+1 + DOCID_LEN), top.length() ) )
               * Math.log( (double) size / t.getCount() ) ); // Calculate inverse document frequency for term from gh(t).numberOfDocuments .

        post.writeInt(Integer.parseInt(top.substring(STR_LEN+1,STR_LEN+1 + DOCID_LEN).trim())); // Write postings record for the token (documentID, termFrequency, OR rtf * idf) .
        post.writeFloat(rtfIDF);

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

  public static void writeDictionary(File outDir) throws IOException {
    System.out.println("writing dictionary file.");

    RandomAccessFile dict = new RandomAccessFile(outDir.getPath()+"/dict.raf","rw"); //write global hash table to disk as dictionary file dict.raf
    dict.seek(0);

    String term;
    int id;
    int ct;
    int st;

    for(int i = 0; i < gh.map.length; i++) {
      if(gh.map[i] != null) {
        term = gh.map[i].getT();
        //id = t.getID();
        ct = gh.map[i].getCount();
        st = gh.map[i].getStart();
      } else {
        term = "END";
        id = -1;
        ct = -1;
        st = -1;
      }

      term = new String(term.getBytes(), Charset.forName("UTF-8"));

      dict.writeUTF( formatString( term, STR_LEN, ct, st ) );
      //dict.writeInt( id );
      //dict.writeInt( ct );
      //dict.writeInt( st );
    }

    dict.close();
  }

  public static String formatString(String str, int limit) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+limit+"s",str);
  }

  public static String formatString(String str, int limit, int id, float rtfIDF) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+STR_LEN+"s %-"+DOCID_LEN+"d %-"+(RTFIDF_LEN/2)+"."+(RTFIDF_LEN/2)+"f",str,id,rtfIDF);
  }

  public static String formatString(String str, int limit, int count, int start) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }

    return String.format("%-"+STR_LEN+"s %0"+8+"d %0"+8+"d",str,count,start);
  }

  static class TermComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  }

  /** Use an iterative merge sort to combine files. The basis for this sort is
      directly from the bottom up sort shown on Wikipedia. However, the merge
      portion is different from the example on the website. It doesn't iterate over
      the array, it just uses the values p and q to combine files.
  */

  public static void mergeSort(File inDir, int n) {
    System.out.println("merging temporary files.");

    BufferedReader L = null;
    BufferedReader R = null;
    BufferedWriter bw = null;
    int size;

    File[] A = inDir.listFiles();

    try {
      for(int c = 1; c < n; c = 2 * c) {

        size = (int)( (double)(n-1) / c );

        if( size > Math.sqrt(A.length) ) {
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
}
