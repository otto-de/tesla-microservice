#tesla-microservice

This is the common basis for some of otto.de's microservices. It is written in clojure using the [component framework](https://github.com/stuartsierra/component).

## Features included

* Load configuration from filesystem.
* Aggregate a status and application health.
* Deliver a json status report.
* Report to graphite using the metrics library.
* Manage routes using compojure.
* Serve content with an embedded jetty.

## Features not included

The basis included is stripped to the very minimum. More features like access to mongodb, redis, zookeeper, etc. will be released at a later time as separate addons.

## Example

Clone the repo. Start the example microservice with

```$ lein run```

Access the example service under ```http://localhost:8080/example```
```http://localhost:8080/example/foo```.

Access the status report under ```http://localhost:8080/status```.

The calculator used for uppercasing is a volume-licensed enterprise software.
So you will notice, that after the uppercasing of 10 Strings, the status of the calculator and consequently the whole application will change from *OK* to *WARNING*.


## FAQ

**Q:** Is it any good? **A:** Yes.

**Q:** Why tesla? **A:** It's a reference to the ingenious scientist and inventor.


## TODO

* release to clojars
* extend documentation
* add one or the other test

## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Siegmund, Kai Brandes, Florian Weyand

## License
Apache License
