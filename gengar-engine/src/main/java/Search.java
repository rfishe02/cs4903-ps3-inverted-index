
package src.main.java;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Queue;
import java.util.HashSet;
import java.util.LinkedList;

//@WebServlet("/search")
public class Search extends HttpServlet {

   public void init() throws ServletException {
   
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("Not supported.");
      out.close();
      
   }
   
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
            int size = 4;
            int wrds;
            boolean flag = false;
            
            for(String name : res) {
                if(name != null) {
                
                    out.print(name.trim()+",");
                    
                    br = new BufferedReader(new FileReader(inDir.getPath()+"/"+name.trim()));
                    wrds = 0;
                    while((read=br.readLine())!=null && wrds < 100) {
                    
                        if(flag) { 
                            if(i < size-1) {
                                out.print(read+",");
                                i++;
                            } else {
                                out.print(read+"&nbsp;...&nbsp;");
                                i = 0;
                                flag = false;
                            }
                            wrds++;
                        } else {
                        
                            prev.add(read);
                        
                            if(prev.size() > size) {
                                prev.remove();
                            }
                        
                            if(qSet.contains(read) && prev.size() >= size) {
                                while(!prev.isEmpty()) {
                                    out.print(prev.remove()+",");
                                    wrds++;
                                }
                            
                                flag = true;
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

   public void destroy() {
   
   }
   
}
