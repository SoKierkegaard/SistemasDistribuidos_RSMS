# Practica 3 JGroups

Proyecto completo de la Parte 1 y la Parte 2 de la practica. Requiere JDK 17 o superior y Maven 3.8 o superior.

## Compilar

```powershell
mvn clean compile
```

## Parte 1

Ejecute cada nodo en una terminal distinta:

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.NodoBasico -Dexec.args="A"
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.ChatGrupo -Dexec.args="A"
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.PizarraGrupo -Dexec.args="A"
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.ContadorRPC -Dexec.args="A"
```

Cambie `A` por un nombre diferente en cada terminal.

## Parte 2 RemateUSFX

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.RemateUSFX -Dexec.args="ana"
```

Comandos obligatorios:

- `/crear monitor 100 120`
- `/pujar monitor 150`
- `/subastas`
- `/estado monitor`
- `/quien`
- `/salir`

Tambien se implemento el bonus `/ganadas`.

Las propuestas de creacion y puja viajan por unicast al coordinador. Solo este las valida y difunde la decision por multicast. Las decisiones de un unico emisor conservan orden FIFO y todos los nodos aplican exactamente la misma secuencia. El estado completo se replica y se transfiere al incorporarse un nodo. Los instantes de cierre son absolutos y el nuevo coordinador reprograma todos los cierres abiertos.

## Ejecucion entre dos equipos con UDP

En cada equipo use su IPv4 real de la red comun:

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.RemateUSFX -Dexec.args="ana" -Djgroups.bind_addr=192.168.1.25 -Djava.net.preferIPv4Stack=true
```

En el segundo equipo cambie el nombre y `bind_addr`. Si ambos aparecen en `/quien`, UDP multicast funciona.

## Ejecucion entre dos equipos con TCP

```powershell
mvn exec:java -Dexec.mainClass=bo.edu.usfx.jgroups.RemateUSFX -Dexec.args="ana" -Dconfig=tcp.xml -Djgroups.bind_addr=192.168.1.25 -Djgroups.tcpping.initial_hosts=192.168.1.25[7800],192.168.1.30[7800] -Djava.net.preferIPv4Stack=true
```

Repita en el segundo equipo cambiando solo el nombre y `bind_addr`. Si algún proceso tomó 7801 o 7802, agregue también esos puertos a `initial_hosts`.

## Estructura

- `NodoBasico`, `ChatGrupo`, `PizarraGrupo` y `ContadorRPC`: pasos 2 a 7.
- `Puja` y `Subasta`: modelo serializable.
- `MensajeRemate`: protocolo tipado.
- `RemateUSFX`: canal, replicacion, coordinacion y temporizadores.
- `ConsolaRemate`: lectura y validacion de comandos.
