Para compilar:
```bash 
javac src/main/java/StableMulticast/StableMulticast.java src/main/java/StableMulticast/IStableMulticast.java src/main/java/client/Client.java
```

Para rodar, em diferentes terminais:
```bash 
java -cp src/main/java client.Client <ip> <port>
```
`<port>`: Utilizar valores de 49152 a 65535
