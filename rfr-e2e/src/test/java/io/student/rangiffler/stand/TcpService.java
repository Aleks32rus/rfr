package io.student.rangiffler.stand;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * Проверяет готовность сервиса по доступности TCP-порта.
 */
public final class TcpService implements Service {
    private final String name;
    private final String hint;
    private final URI address;

    public TcpService(String name, String hint, URI address) {
        this.name = name;
        this.hint = hint;
        this.address = address;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String hint() {
        return hint;
    }

    @Override
    public boolean responds() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address.getHost(), address.getPort()), 5000);
            return true;
        } catch (IOException failure) {
            return false;
        }
    }
}
