# README

Author: `Milo Cardenas`

## How to run

The `Dockerfile` defines a self-contained Java/Gradle reference environment.
Build and run the program using [Docker](https://docs.docker.com/get-started/get-docker/):
```
$ docker build -t challenge .
$ docker run --rm -it challenge --auth=<token>
```
Feel free to modify the `Dockerfile` as you see fit.

If java `21` or later is installed locally, run the program directly for convenience:
```
$ ./gradlew run --args="--auth=<token>"
```

## Discard criteria

`Stored orders are sorted by freshness and discarded based on the least fresh order in the shelf storage, for O(n log n.A PriorityQueue could've been used to achive O(log n) selection, but opted for using a List for simplicity and to avoid staleness when picking up or moving orders`
