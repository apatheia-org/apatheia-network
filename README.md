# Apatheia Network Layer
This project contains the UDP server and client layers for Apatheia, a Kademlia-based protocol.

## Overview
The UDP server and client layers handle the low-level network communication for Apatheia. The server layer listens for incoming UDP packets, 
and the client layer sends UDP packets to other nodes in the network.

The server and client layers are implemented using the MINA framework, which provides a high-performance, scalable network layer for Java applications. 
The implementation is made in Scala, anyway.

## Build

Before to build this project you need to have an environment variable called `GITHUB_TOKEN` with a Github classic token assossiated with `package:read` scope at least. This is necessary in order for us to be able to inject our own dependencies into apatheia network(`apatheia-p2p-protocol`)

## License
This project is licensed under the MIT License. See the [LICENSE file](LICENSE) for details.
