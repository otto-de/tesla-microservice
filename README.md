#tesla-microservice

This is the common basis for some of otto.de's microservices. It is written in clojure using the [component framework](https://github.com/stuartsierra/component).

`[de.otto/tesla-microservice "0.1.15"]`

[![Build Status](https://travis-ci.org/otto-de/tesla-microservice.svg)](https://travis-ci.org/otto-de/tesla-microservice)
[![Dependencies Status](http://jarkeeper.com/otto-de/tesla-microservice/status.svg)](http://jarkeeper.com/otto-de/tesla-microservice)


## Breaking changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. See [CHANGES.md](./CHANGES.md) for instructions on breaking changes.

## Features included

* Load configuration from filesystem.
* Aggregate a status.
* Reply to a health check.
* Deliver a json status report.
* Report to graphite using the metrics library.
* Manage handlers using ring.
* Shutdown gracefully. If necessary delayed, so load-balancers have time to notice.

## Choosing a server

As of version ```0.1.15``` there is no server included any more directly in _tesla-microservice_. 
This gives you the freedom to  a) not use any server at all (e.g. for embedded use) b) choose another server e.g. a non-blocking one like httpkit or immutant. The available options are:

* [tesla-jetty](https://github.com/otto-de/tesla-jetty): The tried and tested embedded jetty.
* [tesla-httpkit](https://github.com/otto-de/tesla-httpkit): The non-blocking httpkit. 

## Addons

The basis included is stripped to the very minimum. Additional functionality is available as addons:

* [tesla-zookeeper-observer](https://github.com/otto-de/tesla-zookeeper-observer): Read only access to zookeeper.
* [tesla-mongo-connect](https://github.com/otto-de/tesla-mongo-connect): Read/write access to mongodb.
* [tesla-cachefile](https://github.com/otto-de/tesla-cachefile): Read and write a cachefile. Locally or in hdfs.

More features will be released at a later time as separate addons.


## Examples

* A growing set of example apllications can be found at [tesla-examples](https://github.com/otto-de/tesla-examples).
* David & Germán created an example application based, among other, on tesla-microservice. They wrote a very instructive [blog post about it](http://blog.agilityfeat.com/2015/03/clojure-walking-skeleton/)
* Moritz created [tesla-pubsub-service](https://bitbucket.org/DerGuteMoritz/tesla-pubsub-service). It showcases how to connect components via core.async channels. Also the embedded jetty was replaced by immutant.

## FAQ

**Q:** Is it any good? **A:** Yes.

**Q:** Why tesla? **A:** It's a reference to the ingenious scientist and inventor.

**Q:** Are there alternatives? **A:** Yes. You might want to look at [modularity.org](https://modularity.org/), [system](https://github.com/danielsz/system) and [duct](https://github.com/weavejester/duct).



## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Sigmund, Kai Brandes, Florian Weyandt

## License
Apache License
