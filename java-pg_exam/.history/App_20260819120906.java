// GET /api/todos 専用の入口を追加する
server.createContext("/api/todos", exchange -> {
    // GET 以外のメソッドは受け付けない
    if (!"GET".equals(exchange.getRequestMethod())) {
        // 許可されていないメソッドであることを返す
        exchange.sendResponseHeaders(405, -1);
        // リクエストを閉じる
        exchange.close();
        // 以降の処理を終了する
        return;
    }
    // Todo 一覧を JSON 文字列に変換する
    byte[] responseBody = todosToJson().getBytes(StandardCharsets.UTF_8);
    // Content-Type に charset を付けずに設定する
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    // JSON のバイト数を指定して成功レスポンスを返す
    exchange.sendResponseHeaders(200, responseBody.length);
    // JSON 本文を書き込む
    exchange.getResponseBody().write(responseBody);
    // レスポンスを閉じる
    exchange.getResponseBody().close();
});

// 全 Todo を JSON 配列に変換する
static String todosToJson() {
    // JSON 配列の組み立てを開始する
    StringBuilder json = new StringBuilder("[");
    // Todo を順番に変換する
    for (int i = 0; i < todos.size(); i++) {
        // 2件目以降の前にカンマを追加する
        if (i > 0) {
            json.append(',');
        }
        // 現在の Todo を取得する
        Todo todo = todos.get(i);
        // Todo の title と done を追加する
        json.append("{\"title\":\"")
                .append(escapeJson(todo.getTitle()))
                .append("\",\"done\":")
                .append(todo.isDone())
                .append('}');
    }
    // JSON 配列を閉じて返す
    return json.append(']').toString();
}

// JSON 文字列内の特殊文字をエスケープする
static String escapeJson(String value) {
    // エスケープ済み文字列を作成する
    StringBuilder escaped = new StringBuilder();
    // 文字列を1文字ずつ処理する
    for (int i = 0; i < value.length(); i++) {
        // 現在の文字を取得する
        char c = value.charAt(i);
        // 特殊文字をJSON形式へ変換する
        switch (c) {
            // 引用符をエスケープする
            case '"':
                escaped.append("\\\"");
                break;
            // バックスラッシュをエスケープする
            case '\\':
                escaped.append("\\\\");
                break;
            // バックスペースをエスケープする
            case '\b':
                escaped.append("\\b");
                break;
            // 改ページをエスケープする
            case '\f':
                escaped.append("\\f");
                break;
            // 改行をエスケープする
            case '\n':
                escaped.append("\\n");
                break;
            // 復帰をエスケープする
            case '\r':
                escaped.append("\\r");
                break;
            // タブをエスケープする
            case '\t':
                escaped.append("\\t");
                break;
            // その他の制御文字を処理する
            default:
                // 制御文字をUnicodeエスケープする
                if (c < 0x20) {
                    // Unicodeエスケープの接頭辞を追加する
                    escaped.append("\\u");
                    // 16進数表現を取得する
                    String hex = Integer.toHexString(c);
                    // 4桁になるまで0を補う
                    for (int j = hex.length(); j < 4; j++) {
                        escaped.append('0');
                    }
                    // 16進数表現を追加する
                    escaped.append(hex);
                } else {
                    // 通常の文字をそのまま追加する
                    escaped.append(c);
                }
                break;
        }
    }
    // エスケープ済み文字列を返す
    return escaped.toString();
}