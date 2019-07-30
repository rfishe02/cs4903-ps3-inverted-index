
package src.main.java;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Queue;
import java.util.HashSet;
import java.util.LinkedList;

/**
The servlet that conducts a search for a query from the web interface.
*/

//@WebServlet("/search")
public class Search extends HttpServlet {

   /*
   public void init() throws ServletException {

   }*/

   /** The method GET is not unsupported for this application. */
   
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("Not supported.");
      out.close();

   }

   /** This method uses a query object to conduct a search with the form data from the web interface.
   If the result is valid, this method formats the String for the Search.js script. The Search.js script
   splits the result at certain points and creates cards for each document the result.
   @param request A request from the interface.
   @param response A response to the interface.
   */

   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      ServletContext con = getServletContext();

      try {

        String message = request.getParameter("search-box");
        String[] spl = message.split("([@\\.\\s-&])+");

        /*File inDir = new File( con.getResource("/home/srv-read/clean/output").toURI() );
        File rafDir = new File( con.getResource("/home/srv-read/ps3/output").toURI() );*/

        /*
        out.println(inDir.canRead() && inDir.exists());
        out.println(rafDir.canRead() && rafDir.exists());
        out.println(new File(rafDir.getPath()+"/dict.raf").canRead());
        */

        File inDir = new File( "/home/srv-read/clean/output" );
        File rafDir = new File( "/home/srv-read/ps3/output" );

        UAQuery query = new UAQuery(rafDir,"stats.raf");
        String[] res = query.runQuery(inDir,rafDir,spl);

        if(res != null) {

            out.print("resources/input ");

            Queue<String> prev = new LinkedList<String>();
            HashSet<String> qSet = new HashSet<String>(50);
            for(String s : spl) {
                qSet.add(s);
            }

            BufferedReader br;
            String read;
            int i = 0;
            int size = 3;
            int wrds;
            int lmt = 100;
            boolean flag = false;

            for(String name : res) {
                if(name != null) {

                    out.print(name.trim()+",");

                    br = new BufferedReader(new FileReader(inDir.getPath()+"/"+name.split(",")[0]));

                    wrds = 0;
                    while(!prev.isEmpty()) {
                        prev.remove();
                    }
                    flag = false;

                    while((read=br.readLine())!=null && wrds < lmt) {

                        if(flag) {

                            if(qSet.contains(read)) {
                                out.print("<strong>"+read+"</strong>");
                            } else {
                                out.print(read);
                            }

                            if(i >= size || wrds >= lmt) {
                                out.print("&nbsp;...&nbsp;");
                                i = 0;
                                flag = false;
                            } else {
                                out.print(",");
                                i++;
                            }

                            wrds++;
                        } else {

                            if(qSet.contains(read)) {
                                while(!prev.isEmpty()) {
                                    out.print(prev.remove()+",");
                                    wrds++;
                                }

                                out.print("<strong>"+read+"</strong>");
                                if(wrds >= lmt) {
                                    out.print("&nbsp;...&nbsp;");
                                } else {
                                    out.print(",");
                                }

                                flag = true;
                                wrds++;
                            } else {

                                prev.add(read);

                                if(prev.size() > size) {
                                    prev.remove();
                                }
                            }
                        }
                    }

                    out.print(" ");
                    br.close();

                }
            }

        } else {
          out.print("No&nbsp;results&nbsp;found.");
        }

      } catch( Exception ex ) {
        out.write("Exception&nbsp;occured.");
        System.exit(1);
      }

      out.close();
   }

   /*
   public void destroy() {

   }*/

}
