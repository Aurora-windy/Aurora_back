package com.aurora.ai.mcp.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Controls only the fixed local MCP demo child process. */
@Slf4j
@Component
public class McpDemoProcessManager {
    private static final long START_TIMEOUT_MS = 20_000L;
    private final int port;
    private Process process;

    public McpDemoProcessManager(@Value("${aurora.ai.mcp.demo.port:1208}") int port) {
        this.port = port;
    }

    public synchronized DemoStatus status() {
        boolean alive = process != null && process.isAlive();
        return new DemoStatus(alive && isPortOpen(port), port);
    }

    public synchronized DemoStatus start() {
        DemoStatus current = status();
        if (current.running()) return current;
        if (isPortOpen(port)) throw new IllegalStateException("MCP demo port " + port + " is already in use");
        Path jar = locateJar();
        Path jackson = Path.of(System.getProperty("user.home"), ".m2", "repository", "com", "fasterxml", "jackson", "core", "jackson-annotations", "2.20", "jackson-annotations-2.20.jar");
        if (!Files.isRegularFile(jackson)) throw new IllegalStateException("Missing MCP Jackson dependency: " + jackson);
        try {
            Path logFile = jar.getParent().resolve("mcp-demo-peer.log");
            List<String> command = List.of(javaExecutable(), "-cp", jackson + ";" + jar,
                    "org.springframework.boot.loader.launch.JarLauncher", "--spring.profiles.active=mcp",
                    "--server.port=" + port, "--aurora.ai.mcp.client.startup-sync-enabled=false");
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(logFile.toFile()).start();
            long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                if (isPortOpen(port)) return status();
                if (!process.isAlive()) throw new IllegalStateException("MCP demo process exited during startup");
                Thread.sleep(200L);
            }
            stopProcess();
            throw new IllegalStateException("MCP demo process did not listen on port " + port);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start MCP demo process: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            stopProcess();
            throw new IllegalStateException("MCP demo startup was interrupted", ex);
        }
    }

    public synchronized DemoStatus stop() {
        stopProcess();
        return status();
    }

    private void stopProcess() {
        if (process == null) return;
        process.descendants().forEach(child -> child.destroyForcibly());
        process.destroyForcibly();
        try { process.waitFor(3, TimeUnit.SECONDS); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        process = null;
    }

    private Path locateJar() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        List<Path> candidates = new ArrayList<>();
        for (Path path = current; path != null; path = path.getParent()) {
            candidates.add(path.resolve("aurora-server/target/aurora-server.jar"));
            candidates.add(path.resolve("target/aurora-server.jar"));
        }
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IllegalStateException("Packaged aurora-server.jar was not found"));
    }

    private String javaExecutable() {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        return Files.isExecutable(java) ? java.toString() : "java";
    }

    private boolean isPortOpen(int targetPort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", targetPort), 200);
            return true;
        } catch (IOException ex) { return false; }
    }

    public record DemoStatus(boolean running, int port) { }
}
