
public class Sort {

  /*
  temp file
  term, document ID, term frequency (within document)

  post.raf -- the documents where the term appears
  document ID, term frequency (within document) or rtf*idf

  dict.raf -- the global hash table
  term or termID, document count, start

  map.raf
  */

  /*
  try {
    RandomAccessFile test = new RandomAccessFile("dict.raf","rw");

    test.seek(gh.hash("cat",0) * (RECORD_LENGTH + 2));
    String record = test.readUTF();
    int count = test.readInt();

    System.out.println(record+" "+count);

  } catch(IOException ex) {
    ex.printStackTrace();
  }*/

  public static void algoTwo(File input) {
    String top = "";
    int topInd = 0;

    try {
      File[] files = input.listFiles();

      /*

      */

      BufferedReader[] br = new BufferedReader[files.length];

      for(int a = 0; a < files.length; a++) {
        br[a] = new BufferedReader(new FileReader(files[a]));
      }

      RandomAccessFile post = new RandomAccessFile("post.raf","rw"); // Create & open a new file for postings, post.raf
      String read = "";
      int nullCount = 0;
      int recordCount = 0;

      while(nullCount < br.length) {

        br[topInd].mark(100);
        top = br[topInd].readLine();
        nullCount = 0;

        for(int b = 0; b < br.length; b++) {

          br[b].mark(100);
          if((read = br[b].readLine()) != null) {

            if(read.substring(0,(RECORD_LENGTH - SUB)).compareTo(top.substring(0,(RECORD_LENGTH - SUB))) < 0) {

              br[topInd].reset();
              top = read;
              topInd = b;

            } else {
              br[b].reset();
            }

          } else {
            nullCount++;
          }

        } // find token that is alphabetically first in the buffer

        System.out.println(top); // Need to write to postings.

        // update the start field for the token in the global hash table.
        // calculate inverse document frequency for term from gh(t).numberOfDocuments
        // write postings record for the token (documentID, termFrequency, OR rtf * idf)
        // recordCount = recordCount + 1;

      } // while all postings haven't been written do

      System.out.println("DONE!");

      post.close();

      for(BufferedReader b : br) {
        b.close();
      }

      writeDictionary();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  /*
  MERGE-SORT(A,p,r)
    if p < r
      q = floor( (p+r)/2 )
      MERGE-SORT(A,p,q)
      MERGE-SORT(A,q+1,r)
      MERGE(A,p,q,r)

  MERGE(A,p,q,r)
    n1 = q-p + 1
    n2 = r-q
    let L[1 .. n1 + 1] and R[1 .. n2 + 1] be new arrays (two open files)
    for i = 1 to n1
      L[i] = A[p+i-1]
    for j = 1 to n2
      R[j] = A[q+j]
    L[n1 + 1] = infinity
    R[n2 + 1] = infinity
    i = 1
    j = 1
    for k = p to r
      if L[i] <= R[j]
        A[k] = L[i] (What is A? A RAF? A newer, slightly bigger file? What if we used filnames? Index locations?)
        i = i + 1
      else A[k] = R[j]
        j = j + 1
  */

  /*
    Try to perform the merge here?
    Instead of 1 : 1 , try consolidating the files.

    Use an array for the files, then merge pairs of files.
    Writing the new temp file to disk.

    Open leftmost file, don't treat it like an array.
    Make rightmost files null as we merge, & delete them on filesystem.
    Or, just always read the leftmost, or first, file.

    We're writing a new temp file each time, but we need to access them later
    when merging.
  */

  public static void mergeSort(String[] A, int n) {

    for(int c = 1; c < n-1; c = 2 * c) {

      for(int l = 0; l < n-1; l += 2 * c) {

        int m = Math.min(l + c - 1, n-1);
        int r = Math.min(l + 2*c - 1, n-1);

        merge(A,l, m, r);

      }
    }

  }

  public static void merge(String[] A, int p, int q, int r) {

    int n1 = q - p + 1;
    int n2 = r - q;

    String[] L = new String[n1];
    String[] R = new String[n2];

    for(int a = 0; a < n1; a++) {
      L[a] = A[p + a];
    }
    for(int b = 0; b < n2; b++) {
      R[b] = A[q + 1 + b];
    }

    int i = 0;
    int j = 0;
    int k = p;

    while( i < n1 && j < n2) {

      if(L[i].compareTo(R[j]) <= 0) {
        A[k] = L[i];
        i++;
      } else {
        A[k] = R[j];
        j++;
      }
      k++;
    }

    while(i < n1) {
      A[k] = L[i];
      i++;
      k++;
    }
    while(j < n2) {
      A[k] = R[j];
      j++;
      k++;
    }

  }

  /*
  public static void mergeSort(String[] A, int p, int r) {

    try {

      if((p) < r) {

        int q = (p+r)/2;
        mergeSort(A,p,q);
        mergeSort(A,q+1,r);
        merge(A,p,q,r);

        for(int i = 0; i < A.length; i++) {
          System.out.print("[ "+i+" "+A[i] + "] ");
        }
        System.out.println();

      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public static void merge(String[] A, int p, int q, int r) {

    String G = "";

    try {

      int n1 = q-p + 1;
      int n2 = r-q;

      String[] L = new String[n1];
      String[] R = new String[n2];

      int x = 0;
      for(int i = 0; i < n1; i++) {
        L[x] = new String(A[p+i]);
        A[p+i] = "";
        x++;
      }

      int y = 0;
      for(int j = 0; j < n2; j++) {
        R[y] = new String(A[q+1+j]);
        A[q+1+j] = "";
        y++;
      }

      int i = 0;
      int j = 0;

      while( i < n1 & j < n2 ) {

        if(L[i].compareToIgnoreCase(R[j]) <= 0) {

          A[p] += L[i] +" ";
          i++;

        } else {
          A[q] += R[j] +" ";
          j++;
        }

      }

      while(i < n1) {
        if(L[i] != null) {
          A[p] += L[i] +" ";
          i++;
        }
      }
      while(j < n2) {
        if(R[j] != null) {
          A[q] += R[j] +" ";
          j++;
        }
      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }*/
  
  /*
  for( Map.Entry<Integer,Integer> mapEntry : docMap.entrySet() ) {
          
          int l = (start+1)-count;
          int r = (start+1);
  
          while(l <= r) {
          
            int m = (l+r)/2;
            post.seek(m * POST_LEN);
            docID = post.readInt();
            
            if( docID > mapEntry.getKey() ) {
              r = m - 1;
            } else if( docID < mapEntry.getKey() ) {
              l = m + 1;
            } else {
              break;
            }
          
            System.out.println(entry.getKey());
            System.out.println(l+" "+m+" "+r);
          }
          
          if(docID == mapEntry.getKey()) {
            rtfIDF = post.readFloat();
            tdm[ entry.getValue() ][ docMap.get(docID) ] = rtfIDF;
          }
          
        }
  */
  
  /*
  while((read=br.readLine())!=null) {
              read = convertText(read,STR_LEN);

              
              String tmp;
              i = 0;  // Find the term in the dictionary.
              do {
                dict.seek( hash(read,i,seed) * (DICT_LEN + 2) );
                tmp = dict.readUTF();
                i++;
              } while( i < seed && tmp.trim().compareToIgnoreCase(NA) != 0 && tmp.trim().compareToIgnoreCase(read) != 0);
     
              if(tmp.trim().compareToIgnoreCase(NA) != 0 && dict.readInt() < 2000) {
              
                if(!termMap.containsKey(read)) {
                  termMap.put(read,row);
                  row++;
                }
              
              }
              
       
            } // Open file & map terms within to columns.
  */

}
