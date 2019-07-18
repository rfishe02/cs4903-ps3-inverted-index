
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebServlet(name = "Search", urlPatterns = "/Search")
public class Search extends HttpServlet {

   public void init() throws ServletException {
   
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      // Set response content type
      response.setContentType("text/html");

      // Actual logic goes here.
      PrintWriter out = response.getWriter();
      out.println("<h1>test</h1>");
      out.close();
      
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
   
   }

   public void destroy() {
   
   }
}
