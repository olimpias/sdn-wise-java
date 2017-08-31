# sdn-wise-java
[![Build Status](https://travis-ci.org/sdnwiselab/sdn-wise-java.svg?branch=master)](https://travis-ci.org/sdnwiselab/sdn-wise-java) [![Codacy Badge](https://api.codacy.com/project/badge/grade/0ff5041b31c44911b81060d17b3e6eba)](https://www.codacy.com/app/sdnwiselab/sdn-wise-java)

The stateful Software Defined Networking solution for the Internet of Things. This repository contains a Java implementation of SDN-WISE. The repository is splitted into three folders.

* core: which contains the definitions of the flowtable, the packets, and some utility classes
* ctrl: containing a small Java control plane that can be used to manage an emulated SDN-WISE network 
* data: a Java emulated SDN-WISE sensor node

### Installation

Clone the GitHub repository and use Maven to compile sdn-wise-java:

```shell
git clone https://github.com/sdnwiselab/sdn-wise-java.git
cd sdn-wise-java
mvn clean install
cd ctrl/build
java -jar sdn-wise-ctrl-X.X.X-jar-with-dependencies.jar 
```

When the network discovery is complete the SDN-WISE Java Control Plane interface will popup. 
Using this window you can send a packet to a node, set the properties of a node, and check the content of its flow table.


### Documentation 

[http://sdn-wise.dieei.unict.it](http://sdn-wise.dieei.unict.it/#Documentation)

### Versioning

* This project uses [semantic versioning](http://semver.org).

### Licensing

* See [LICENSE](LICENSE).