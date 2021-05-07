import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;
import java.util.*;

public class FileShare {

    private static final int PORT = 8080;
    private static final int NO_THREADS = 10;

    public static void main(String args[]) throws Exception {

        createFileList();

        //get IP dynamically
        String hostname = InetAddress.getLocalHost().getHostAddress();
        InetSocketAddress sockAddr = new InetSocketAddress(hostname, PORT);
        HttpServer server = HttpServer.create( sockAddr, 0);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NO_THREADS);

        server.createContext("/files/", new FileHttpHandler() );
        server.createContext("/", new PageHttpHandler() );
        server.setExecutor(executor);
        server.start();

        System.out.println("Server started at:" + sockAddr.toString());
    }

    //generate .file_list.txt
    public static void createFileList() throws IOException {
        FileWriter writer = new FileWriter("html/files/.file_list.txt");
        File listOfFiles[] = new File("html/files/").listFiles();
        Arrays.sort(listOfFiles);
        for( File file : listOfFiles) {
            String filename = file.toString().split("/")[2];
            if(!filename.startsWith("."))
                writer.write(filename.toString() + "\n");
        }
        writer.flush();
        writer.close();
    }
}

class PageHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {        
        OutputStream output = exchange.getResponseBody();
        String responseString = "";

        try {
            responseString = generateHTMLPage();
            exchange.sendResponseHeaders(200, responseString.length());
            output.write( responseString.getBytes() );
            output.flush();
            output.close();
        }
        catch(Exception ex) {
            responseString = "<html><body>Some error occurred :/</body></html>";
            exchange.sendResponseHeaders(200, responseString.length());
            output.write( responseString.getBytes() );
            output.flush();
            output.close();

            ex.printStackTrace();
        }
    }

    public String generateHTMLPage() {
        String htmlPage = "";
        try {
            boolean hasContent = false;
            htmlPage = getStringFromHtmlFile("html/begin.html");
            BufferedReader reader = new BufferedReader(new FileReader("html/files/.file_list.txt"));
            String line;

            while( (line = reader.readLine()) != null ) {
                hasContent = true;
                htmlPage += "<a href=\"/files/" + line + "\" download>" + line + "</a><br><br>";
            }
            if(!hasContent)
                htmlPage += "No files available for download :)";
            
            htmlPage += getStringFromHtmlFile("html/end.html");

            reader.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
            htmlPage = "<html><body>Some error occurred :/</body></html>";
        }
        return htmlPage;
    }

    public String getStringFromHtmlFile(String path) throws Exception {
        File htmlFile = new File(path);
        StringBuilder file = new StringBuilder();
        String tempStr = "";
        BufferedReader br = new BufferedReader(new FileReader(htmlFile));

        while( (tempStr = br.readLine()) != null)
            file.append(tempStr);

        br.close();
        
        return file.toString();
    }
}

class FileHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String absoluteURI = System.getProperty("user.dir") + File.separator + "html" + File.separator + requestURI.toString();
        
        absoluteURI = absoluteURI.replace("%20", " ");
        byte[] fileContent;
        try {
            System.out.println("File: " + absoluteURI + "\n\n");

	        File file = new File(absoluteURI);
            FileInputStream fileStream = new FileInputStream(file);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=\"UTF-8\"");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.sendResponseHeaders(200, file.length());

            OutputStream output = exchange.getResponseBody();
	        fileStream.transferTo(output);
            output.flush();
            output.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
