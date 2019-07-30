
/********************************
Name: Renae Fisher
Username: text05
Problem Set: PS3
Due Date: 7/15/19
********************************/

import java.io.*;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;
import java.nio.charset.*;

/** A class that builds an inverted index from a directory of tokenized files.
Each file must have a single token on each line. */

public class UAInvertedIndex {
  static final String NA = "NULL";
  static final int DOCID_LEN = 5;
  static final int STR_LEN = 8;
  static final int MAP_LEN = 25;
  static GlobalMap gh;
  static int seed = 5000000;
  static final int RTF_LEN = 8; //0.029304

  /**
  @param args Accepts the following arugments from the command line: [input tokenized files] [output for random access files].
  */

  public static void main(String[] args) {

    if(args == null || args.length < 2) {

      System.out.println("the application requires the arguments: [input dir.] [output dir.]");

    } else {

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

  }

  /** This method coordinates the construction of the inverted index. It calls three methods
  that each perform a different task.
  @param inDir A directory of tokenized files.
  @param outDir The destination directory for the random access files.
  @param stat A random access file used to write statistics that will be used by the UAQuery class.
  */

  public static void buildInvertedIndex(File inDir, File outDir, RandomAccessFile stat) throws IOException {
    int size = algoOne(inDir,outDir,new File("temp"));
    mergeSort(new File("temp"),size); // Consolidate the temporary files produced by the first algorithm.
    algoTwo(new File("tmp"),outDir,size);

    stat.writeInt(size);
  }

  /** This method is the first phase of the algorithm that creates an inverted index.
  It uses a map to count the frequencies of all terms in an individual document, and it
  arranges the distinct terms in sorted order. Afterwards, it writes the terms within the map to the hard drive,
  in sorted order, as temporary files. The global hash table will store the document frequency for each term.
  The method also creates the map.raf file, which maps document IDs to document names.
  @param inDir An input directory of tokenized files.
  @param outDir The output directory for random access files.
  @param tmpDir The output directory for the temporary files.
  @return The number of documents, or document IDs, processed by the method.
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

  /** This method writes sorted files to a temporary directory. It also uses the global hash table to count
  the number of documents that each distinct term appears in.
  @param bw The BufferedWriter linked to the temporary directory.
  @param ht A data structure that has terms in sorted order.
  @param docID The document ID for the current document.
  @param totalFreq The total number of tokens in the document.
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

  /** This method is the second phase of the algorithm that creates an inverted index.
  It takes a directory of merged files and combines them into a postings file. Aftwards, the method writes the global hash table to the
  hard drive in hash order as the dictionary file.
  @param inDir A directory of merged files.
  @param outDir The directory for the random access files.
  @param size The total number of documents.
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

  /**  This method writes the dictionary file to disk. It iterates over the global
  hash map and writes every term, including null terms, as a fixed length String.
  This implementation does not support characters that are more than a single byte.
  @param outDir The output directory for the random access files.
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
      @param inDir A directory of sorted, temporary, files.
      @param n The number files in the temporary directory.
  */

  public static void mergeSort(File inDir, int n) {
    System.out.println("merging temporary files.");

    BufferedReader L = null;
    BufferedReader R = null;
    BufferedWriter bw = null;
    int size;

    File[] A = inDir.listFiles();
    int len = Math.min( 500, (int)Math.log(A.length) );

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
     For example, suppose we have five files. First, it would merge the files at index 0 and 1.
     Then, it would merge the files at 2 and 3. Now, the size of the segment will grow. The method
     would merge file 0 and file 2. Last, the method would merge file 0 and 4.
     @param A The array of sorted, temporary, files.
     @param L A file at location p.
     @param R A file at location z.
     @param bw A new file that will contain the two files from L and R.
     @param p The leftmost file.
     @param q The rightmost file.
     @param r Not used in sort, but provides additional information for filename.
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

  /** The comparator used to arrange terms in the TreeMap.
  */

  static class TermComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  }

  /** A method used to perform normalization on Strings. It replaces non-ASCII characters with
  single byte characters.
  @param s An input String.
  @param limit The final size of the output String.
  @return A normalized String.
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

  /** A method used to format a String record. It trims Strings to a particular length, and
  pads the output String with spaces. This is used to format records for the map.raf file.
  @param str An input String.
  @param limit The final size of the output String.
  @return A formatted String.
  */

  public static String formatString(String str, int limit) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+limit+"s",str);
  }

  /** A method used to format the records for the temporary files. This application uses
  a substring to compare terms, so it's necessary to write fixed length records.
  @param str An input String.
  @param limit The final size of the output String.
  @param id A document ID.
  @param rtf The relative frequency of a term.
  @return A formatted String.
  */

  public static String formatString(String str, int limit, int id, double rtf) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+STR_LEN+"s %-"+DOCID_LEN+"d %-"+(RTF_LEN/2)+"."+(RTF_LEN/2)+"f",str,id,rtf);
  }

  /** The method used to format the String of the dictionary file.
  @param str An input String.
  @param limit The final size of the output String.
  @return A formatted String.
  */

  public static String formatXString(String str, int limit) {
    if(str.length() > limit) {
      str = str.substring(0,limit);
    }
    return String.format("%-"+limit+"s",str);
  }

}
