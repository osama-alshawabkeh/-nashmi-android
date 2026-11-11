# Nashmi v1 implementation notes

Architecture:
- Server APK hosts a TCP control listener (default 7000) and data listener (7001).
- Client APK initiates the control connection, registers device ID + assigned public port + shared token.
- Server binds the assigned public TCP port on its network interface.
- On inbound connection, server sends OPEN + connection ID over the control socket.
- Client opens an outbound data connection to server:7001, authenticates the connection ID, then connects to its local target and relays bytes bidirectionally.

This is a deliberately small proof-of-concept protocol. It is not a hardened production tunnel.