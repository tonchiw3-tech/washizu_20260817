import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class App {
    // ★変更 List と、次に振る番号を main の外に置く
    static List<Todo> todos = new ArrayList<>();
    // ★変更 次に振る番号は 1 から始める
    static int nextId = 1;

    public static void main(String[] args) throws Exception {
        // ★変更 起動時にサンプルの Todo を2件入れる
        todos.add(new Todo(nextId++, "牛乳を買う"));
        Todo egg = new Todo(nextId++, "卵を買う");
        egg.setDone(true);
        todos.add(egg);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String message;
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String value = body.substring(5);
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (!title.isEmpty()) {
                    // ★変更 フォームの内容から Todo を1件作って List に追加する
                    todos.add(new Todo(nextId, title));
                    // ★変更 次の Todo に使う番号を進める
                    nextId++;
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
                // ★追加 /done?id=数字を受け取り、該当するTodoを完了にする
            } else if (path.equals("/done")) {
                String query = exchange.getRequestURI().getQuery();
                int id;
                try {
                    // ★追加 id= の後ろを数字に変換する
                    if (query == null || !query.startsWith("id=")) {
                        throw new NumberFormatException();
                    }
                    id = Integer.parseInt(query.substring(3));
                } catch (NumberFormatException e) {
                    // ★追加 idがない、または数字でないときは何も変えず一覧へ戻す
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                // ★追加 idが一致するTodoを1件だけ完了にする
                for (Todo todo : todos) {
                    if (todo.getId() == id) {
                        todo.setDone(true);
                        break;
                    }
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
                // ★追加 /delete?id=数字を受け取り、該当するTodoを削除する
            } else if (path.equals("/delete")) {
                String query = exchange.getRequestURI().getQuery();
                int id;
                try {
                    // ★追加 id= の後ろを数字に変換する
                    if (query == null || !query.startsWith("id=")) {
                        throw new NumberFormatException();
                    }
                    id = Integer.parseInt(query.substring(3));
                } catch (NumberFormatException e) {
                    // ★追加 idがない、または数字でないときは何も変えず一覧へ戻す
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                // ★追加 idが一致するTodoを1件だけListから削除する
                todos.removeIf(todo -> todo.getId() == id); // ★修正 URLのidと一致したTodoだけを削除
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                // ★追加 画面全体の見た目を整えるHTMLとCSS
                String html = "<!DOCTYPE html><html lang='ja'><head>"
                        + "<meta charset='UTF-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                        + "<title>わたしのTodo</title>"
                        + "<style>"
                        + "*{box-sizing:border-box;}"
                        + "body{max-width:720px;margin:0 auto;padding:16px;font-family:sans-serif;}"
                        + ".hero{margin-bottom:16px;}"
                        + ".hero h1{margin:0 0 8px;font-size:24px;}"
                        + ".hero p{margin:0;}"
                        + ".add-form{margin-bottom:16px;}"
                        + ".add-form input{width:70%;padding:8px;font-size:16px;}"
                        + ".add-form button{padding:8px;font-size:16px;}"
                        + ".todo-list{margin:0;padding-left:20px;}"
                        + ".todo-item{margin-bottom:8px;}"
                        + ".todo-title{font-size:1.05rem;overflow-wrap:anywhere;}"
                        + ".todo-item.done .todo-title{color:#94a3b8;text-decoration:line-through;}"
                        + ".actions a{margin-left:8px;}"
                        + "</style></head><body><main class='container'>"
                        + "<header class='hero'><h1>わたしのTodo</h1><p>今日やることを、すっきり管理しましょう。</p></header>"
                        + "<form class='add-form' method='post' action='/add'>"
                        + "<input name='todo' placeholder='新しいTodoを入力' autocomplete='off'>"
                        + "<button type='submit'>追加する</button></form>"
                        + "<ul class='todo-list'>";
                if (todos.isEmpty()) {
                    html += "<li>やることは、いまゼロです</li>";
                } else {
                    // ★変更 Todo の title を表示し、done のときだけ印を付ける
                    for (Todo todo : todos) {
                        String mark = "";
                        if (todo.isDone()) {
                            mark = " 〔済〕";
                        }
                        // ★追加 Todoの状態に応じたクラスと、見た目を整えたリンクを付ける
                        String itemClass = todo.isDone() ? " done" : "";
                        html += "<li class='todo-item" + itemClass + "'>"
                                + "<span class='todo-title'>" + todo.getTitle() + mark + "</span>"
                                + "<span class='actions'>"
                                + "<a class='done-link' href='/done?id=" + todo.getId() + "'>完了</a>"
                                + "<a class='delete-link' href='/delete?id=" + todo.getId() + "'>削除</a>"
                                + "</span></li>";
                    }
                }
                // ★追加 一覧画面のHTMLを閉じる
                html += "</ul></main></body></html>";
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                // ★変更 未使用の /hello・/bye ルーティングを削除
                message = "ページが見つかりません";
            }
            byte[] responseBody = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (止めるときは Ctrl+C)");
    }
}

// ★変更 Todo を表すクラスを追加
class Todo {
    private final int id;
    private final String title;
    private boolean done;

    // ★変更 Todo は done=false で初期化する
    Todo(int id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
    }

    // ★変更 id を読み出すメソッド
    int getId() {
        return id;
    }

    // ★変更 title を読み出すメソッド
    String getTitle() {
        return title;
    }

    // ★変更 done を読み出すメソッド
    boolean isDone() {
        return done;
    }

    // ★変更 done を書き換えるメソッド
    void setDone(boolean done) {
        this.done = done;
    }
}
