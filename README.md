#tesla-microservice

This is the common basis for some of otto.de's microservices. It is written in clojure using the [component framework](https://github.com/stuartsierra/component).

[![Build Status](https://travis-ci.org/otto-de/tesla-microservice.svg)](https://travis-ci.org/otto-de/tesla-microservice)
[![Dependencies Status](http://jarkeeper.com/otto-de/tesla-microservice/status.svg)](http://jarkeeper.com/otto-de/tesla-microservice)

## Features included

* Load configuration from filesystem.
* Aggregate a status.
* Reply to a health check.
* Deliver a json status report.
* Report to graphite using the metrics library.
* Manage routes using compojure.
* Serve content with an embedded jetty.
* Shutdown gracefully. If necessary delayed, so load-balancers have time to notice.

## Addons

The basis included is stripped to the very minimum. As a first addon we published a [zookeeper observer](https://github.com/otto-de/tesla-zookeeper-observer).

More features like access to mongodb, redis, etc. will be released at a later time as separate addons.


## Usage

Add this to your project dependencies:

`[de.otto/tesla-microservice "0.1.11"]`

See the included example on how to use it.

## Examples

* A growing set of example apllications can be found at [tesla-examples](https://github.com/otto-de/tesla-example).
* David & Germán created an example application based, among other, on tesla-microservice. They wrote a very instructive [blog post about it](http://blog.agilityfeat.com/2015/03/clojure-walking-skeleton/)
* Moritz created [tesla-pubsub-service](https://bitbucket.org/DerGuteMoritz/tesla-pubsub-service). It showcases how to connect components via core.async channels. Also the embedded jetty was replaced by immutant. 

## FAQ

**Q:** Is it any good? **A:** Yes.

**Q:** Why tesla? **A:** It's a reference to the ingenious scientist and inventor.

## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Sigmund, Kai Brandes, Florian Weyandt

## License
Apache License
