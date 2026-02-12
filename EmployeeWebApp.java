import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

public class EmployeeWebApp {

    static File userFile = new File("users.txt");
    static File taskFile = new File("tasks.txt");

    public static void main(String[] args) throws Exception {

        if (!userFile.exists()) {
            Files.write(userFile.toPath(),
                    "manager|manager123|manager\n".getBytes(),
                    StandardOpenOption.CREATE);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", ex -> send(ex, loginPage()));
        server.createContext("/register", ex -> handleRegister(ex));
        server.createContext("/login", ex -> handleLogin(ex));
        server.createContext("/employee", ex -> handleEmployee(ex));
        server.createContext("/manager", ex -> handleManager(ex));

        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    // ================= LOGIN =================
    static void handleLogin(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String,String> form = parse(ex);
            String username = form.get("username");
            String password = form.get("password");

            List<String> users = Files.readAllLines(userFile.toPath());
            for(String line:users){
                String[] u=line.split("\\|");
                if(u[0].equals(username)&&u[1].equals(password)){
                    if(u[2].equals("manager"))
                        redirect(ex,"/manager");
                    else
                        redirect(ex,"/employee?user="+username);
                    return;
                }
            }
            send(ex,"<h3 style='color:red;text-align:center'>Invalid Login</h3>");
        }
    }

    // ================= REGISTER =================
    static void handleRegister(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String,String> form = parse(ex);
            String record = form.get("username")+"|"+
                    form.get("password")+"|employee\n";
            Files.write(userFile.toPath(), record.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            redirect(ex,"/");
        } else {
            send(ex, registerPage());
        }
    }

    // ================= EMPLOYEE =================
    static void handleEmployee(HttpExchange ex) throws IOException {

        Map<String,String> query =
                parseQuery(ex.getRequestURI().getQuery());
        String user = query.get("user");

        if ("POST".equals(ex.getRequestMethod())) {
            Map<String,String> form = parse(ex);
            String record =
                    user+"|"+
                    form.get("title")+"|"+
                    form.get("description")+"|"+
                    form.get("date")+"|"+
                    form.get("time")+"|"+
                    form.get("status")+"|"+
                    form.get("progress")+"\n";

            Files.write(taskFile.toPath(), record.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        String rows="";
        if(taskFile.exists()){
            List<String> lines=Files.readAllLines(taskFile.toPath());
            for(String line:lines){
                String[] d=line.split("\\|");
                if(d[0].equals(user)){
                    rows+="<tr><td>"+d[1]+"</td><td>"+d[2]+"</td><td>"+
                            d[3]+"</td><td>"+d[4]+"</td><td>"+
                            d[5]+"</td><td>"+d[6]+"%</td></tr>";
                }
            }
        }

        String page =
        "<html><head>"+css()+
        "</head><body>"+
        navbar("Employee Dashboard - "+user, "#4e73df")+
        "<div class='container'>"+
        "<h3>Submit Daily Status</h3>"+
        "<form method='post'>"+
        "<input name='title' placeholder='Task Title' required>"+
        "<input name='description' placeholder='Description' required>"+
        "<input type='date' name='date' required>"+
        "<input type='time' name='time' required>"+
        "<select name='status'>"+
        "<option>Completed</option>"+
        "<option>Pending</option></select>"+
        "<input type='number' name='progress' min='0' max='100' placeholder='Progress %' required>"+
        "<button>Submit</button>"+
        "</form>"+
        "<h3>Your Submitted Status</h3>"+
        "<table>"+
        "<tr><th>Title</th><th>Description</th><th>Date</th><th>Time</th><th>Status</th><th>Progress</th></tr>"+
        rows+
        "</table>"+
        "<a class='logout' href='/'>Logout</a>"+
        "</div></body></html>";

        send(ex,page);
    }

    // ================= MANAGER =================
    static void handleManager(HttpExchange ex) throws IOException {

        String rows="";
        if(taskFile.exists()){
            List<String> lines=Files.readAllLines(taskFile.toPath());
            for(String line:lines){
                String[] d=line.split("\\|");
                rows+="<tr><td>"+d[0]+"</td><td>"+d[1]+"</td><td>"+
                        d[2]+"</td><td>"+d[3]+"</td><td>"+
                        d[4]+"</td><td>"+d[5]+"</td><td>"+
                        d[6]+"%</td></tr>";
            }
        }

        String page =
        "<html><head>"+css()+
        "</head><body>"+
        navbar("Manager Dashboard", "#e74a3b")+
        "<div class='container'>"+
        "<h3>All Employee Daily Status</h3>"+
        "<table>"+
        "<tr><th>User</th><th>Title</th><th>Description</th>"+
        "<th>Date</th><th>Time</th><th>Status</th><th>Progress</th></tr>"+
        rows+
        "</table>"+
        "<a class='logout' href='/'>Logout</a>"+
        "</div></body></html>";

        send(ex,page);
    }

    // ================= CSS =================
    static String css(){
        return "<style>"+
                "body{margin:0;font-family:Arial;background:#f8f9fc}"+
                ".navbar{padding:15px;color:white;font-size:20px}"+
                ".container{padding:20px}"+
                "input,select{padding:8px;margin:5px;border:1px solid #ccc;border-radius:4px}"+
                "button{padding:8px 15px;background:#1cc88a;color:white;border:none;border-radius:4px}"+
                "button:hover{background:#17a673}"+
                "table{width:100%;border-collapse:collapse;margin-top:20px;background:white}"+
                "th,td{border:1px solid #ddd;padding:10px;text-align:center}"+
                "th{background:#4e73df;color:white}"+
                "tr:nth-child(even){background:#f2f2f2}"+
                ".logout{display:inline-block;margin-top:15px;color:red;text-decoration:none}"+
                "</style>";
    }

    static String navbar(String title,String color){
        return "<div class='navbar' style='background:"+color+"'>"+title+"</div>";
    }

    static String loginPage(){
        return "<html><head>"+css()+"</head><body>"+
                navbar("Employee Status System","#4e73df")+
                "<div class='container' style='text-align:center'>"+
                "<h2>Login</h2>"+
                "<form method='post' action='/login'>"+
                "<input name='username' placeholder='Username' required><br>"+
                "<input type='password' name='password' placeholder='Password' required><br>"+
                "<button>Login</button></form>"+
                "<p><a href='/register'>Register</a></p>"+
                "</div></body></html>";
    }

    static String registerPage(){
        return "<html><head>"+css()+"</head><body>"+
                navbar("Register Employee","#36b9cc")+
                "<div class='container' style='text-align:center'>"+
                "<h2>Create Account</h2>"+
                "<form method='post'>"+
                "<input name='username' placeholder='Username' required><br>"+
                "<input type='password' name='password' placeholder='Password' required><br>"+
                "<button>Register</button></form>"+
                "</div></body></html>";
    }

    static void send(HttpExchange ex,String response) throws IOException{
        ex.sendResponseHeaders(200,response.getBytes().length);
        OutputStream os=ex.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    static void redirect(HttpExchange ex,String path) throws IOException{
        ex.getResponseHeaders().add("Location",path);
        ex.sendResponseHeaders(302,-1);
        ex.close();
    }

    static Map<String,String> parse(HttpExchange ex) throws IOException{
        String body=new String(ex.getRequestBody().readAllBytes());
        return parseQuery(body);
    }

    static Map<String,String> parseQuery(String query){
        Map<String,String> map=new HashMap<>();
        if(query==null)return map;
        for(String param:query.split("&")){
            String[] pair=param.split("=");
            if(pair.length>1)
                map.put(pair[0],pair[1]);
        }
        return map;
    }
}
