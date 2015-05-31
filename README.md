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

## Example

Clone the repo. Start the example microservice with

`$ lein run`

Access the example service under `http://localhost:8080/example` and `http://localhost:8080/example/foo`.

Access the status report under `http://localhost:8080/status`.

The calculator used for uppercasing is a volume-licensed enterprise software.
So you will notice, that after the uppercasing of 10 Strings, the status of the calculator and consequently the whole application will change from *OK* to *WARNING*.


## FAQ

**Q:** Is it any good? **A:** Yes.

**Q:** Why tesla? **A:** It's a reference to the ingenious scientist and inventor.


## TODO

* extend documentation
* add one or the other test

## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Sigmund, Kai Brandes, Florian Weyandt

## License
Apache License
