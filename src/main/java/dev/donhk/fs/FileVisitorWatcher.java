package dev.donhk.fs;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import dev.donhk.http.HttpContextHandler;
import dev.donhk.http.handlers.DirectoryContextHandler;
import dev.donhk.http.handlers.FileContextHandler;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeSet;

public class FileVisitorWatcher implements FileVisitor<Path> {

    private final HttpContextHandler server;

    public FileVisitorWatcher(HttpContextHandler server) {
        this.server = server;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        final String contextName = dir.toString().replace(server.getWebDirectory().toString(), "").replace("\\", "/");
        final String context;
        if (contextName.length() == 0) {
            context = "/";
        } else {
            context = contextName;
        }
        System.out.println("adding dir context " + context);
        final HttpContext statusContext = server.getServer().createContext(context);
        HttpHandler httpHandler = new DirectoryContextHandler(server.getWebDirectory(), dir);
        statusContext.setHandler(httpHandler);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        final String contextName = file.toString().replace(server.getWebDirectory().toString(), "").replace("\\", "/");
        System.out.println("adding context " + contextName);
        final HttpContext statusContext = server.getServer().createContext(contextName);
        HttpHandler httpHandler = null;
        if (Files.isRegularFile(file)) {
            httpHandler = new FileContextHandler(file);
        }
        if (Files.isDirectory(file)) {
            httpHandler = new DirectoryContextHandler(server.getWebDirectory(), file);
        }
        if (httpHandler == null) {
            server.getServer().removeContext(file.toString());
            return FileVisitResult.CONTINUE;
        }
        statusContext.setHandler(httpHandler);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    public HttpContextHandler getServer() {
        return server;
    }
}
