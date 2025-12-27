package me.seroperson.reload.live.runner;

import me.seroperson.reload.live.build.ReloadableServer;
import me.seroperson.reload.live.reflect.Environment;

import java.io.IOException;
import java.util.Map;

public class DevServerWrapper implements ReloadableServer {

    private final StartParams params;
    private final ReloadableServer server;

    private Map<String, String> initialEnv;

    public DevServerWrapper(StartParams params, ReloadableServer server) {
        this.params = params;
        this.server = server;
    }

    @Override
    public void start() {
        this.initialEnv = System.getenv();

        var propagateEnv = params.getPropagateEnv();
        Environment.putEnv(propagateEnv);

        server.start();
    }

    @Override
    public boolean isRunning() {
        return server.isRunning();
    }

    @Override
    public boolean reload() {
        return server.reload();
    }

    @Override
    public void close() throws IOException {
        try {
            server.close();
        } finally {
            Environment.setEnv(initialEnv);
        }
    }

}
