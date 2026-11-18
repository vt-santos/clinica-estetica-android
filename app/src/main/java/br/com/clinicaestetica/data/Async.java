package br.com.clinicaestetica.data;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Executa o acesso ao banco fora da thread principal. */
public final class Async {
    private static final ExecutorService DB = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    public static <T> void load(Supplier<T> work, Consumer<T> done) {
        DB.execute(() -> {
            T result = work.get();
            MAIN.post(() -> done.accept(result));
        });
    }

    public static void run(Runnable work, Runnable done) {
        DB.execute(() -> {
            work.run();
            MAIN.post(done);
        });
    }
}
