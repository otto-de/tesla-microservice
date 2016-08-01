#tesla-microservice

> "If Edison had a needle to find in a haystack, he would proceed at once with the diligence of the bee to examine straw after straw until he found the object of his search." - Nikola Tesla

This is the common basis for some of otto.de's microservices. It is written in clojure using the [component framework](https://github.com/stuartsierra/component).

[![Clojars Project](http://clojars.org/de.otto/tesla-microservice/latest-version.svg)](http://clojars.org/de.otto/tesla-microservice)

[![Build Status](https://travis-ci.org/otto-de/tesla-microservice.svg)](https://travis-ci.org/otto-de/tesla-microservice)
[![Dependencies Status](http://jarkeeper.com/otto-de/tesla-microservice/status.svg)](http://jarkeeper.com/otto-de/tesla-microservice)


## Breaking changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. See [CHANGES.md](./CHANGES.md) for instructions on breaking changes.

## Features included

* Load configuration from filesystem.
* Aggregate a status.
* Execute functions with a scheduler
* Reply to a health check.
* Deliver a json status report.
* Report to graphite using the metrics library.
* Manage handlers using ring.
* Shutdown gracefully. If necessary delayed, so load-balancers have time to notice.

## Examples

* A growing set of example apllications can be found at [tesla-examples](https://github.com/otto-de/tesla-examples).
* David & Germán created an example application based, among other, on tesla-microservice. They wrote a very instructive [blog post about it](http://blog.agilityfeat.com/2015/03/clojure-walking-skeleton/)
* Moritz created [tesla-pubsub-service](https://bitbucket.org/DerGuteMoritz/tesla-pubsub-service). It showcases how to connect components via core.async channels. Also the embedded jetty was replaced by immutant.

### Scheduler

The scheduler executes functions based on a schedule. It is based on [overtones at-at](https://github.com/overtone/at-at) project.

To use the scheduler, you have to hook the `scheduler component` into your system. 
```clj
(assoc your-system :scheduler (c/using (sch/new-scheduler) []))
```

`your-system` is the result from the function `base-system` from `de.otto.tesla.system`.

To actually use it you have to pass the `:scheduler` to the component in which it is invoked like this:
```clj
(de.otto.tesla.stateful.scheduler/schedule scheduler scheduled-function interval)
```

where the `scheduled-function` is executed every `interval` in milliseconds. 

### app-status

The app-status indicates the current status of your microservice. To use it you can register a status function to it.

Here is a simple example for a function that checks if an atom is empty or not.

```clj
(de.otto.tesla.stateful.app-status/register-status-fun app-status #(status atom))
``` 

The `app-status` is injected under the keyword :app-status from the base system.

```clj
(defn status [atom]
      (let [status (if @atom :error :ok)
            message (if @atom "Atom is empty" "Atom is not empty")]
           (de.otto.status/status-detail :status-id status message)))
```

For further information and usages take a look at the: [status library](https://github.com/otto-de/status)

## Choosing a server

As of version ```0.1.15``` there is no server included any more directly in _tesla-microservice_. 
This gives you the freedom to  a) not use any server at all (e.g. for embedded use) b) choose another server e.g. a non-blocking one like httpkit or immutant. The available options are:

* [tesla-jetty](https://github.com/otto-de/tesla-jetty): The tried and tested embedded jetty.
* [tesla-httpkit](https://github.com/otto-de/tesla-httpkit): The non-blocking httpkit. 

## Configuring

Applications build with `tesla-microservices` can be configured via 
`edn`-files, that have to be located in the class path.

### Order of loading and merging

1. A file named `default.edn` is loaded. 
2. A file named by the ENV-variable `$CONFIG_FILE` is loaded.
3. A file name `local.edn` is loaded.

The configuration hash-map in those files is loaded and merged in the
specified order. Which mean configurations for the same key is overridden
by the latter occurrence.

### ENV-variables

In contrast to former versions of `tesla-microservice` ENV-variables are not
merged into the configuration.

But you can easily specify ENV-variables, that should be accessible in
your configuration:

```edn
{
 :my-app-secret  #ts/env [:my-env-dep-app-secret "default"]
}
```

## Addons

The basis included is stripped to the very minimum. Additional functionality is available as addons:

* [tesla-zookeeper-observer](https://github.com/otto-de/tesla-zookeeper-observer): Read only access to zookeeper.
* [tesla-mongo-connect](https://github.com/otto-de/tesla-mongo-connect): Read/write access to mongodb.
* [tesla-cachefile](https://github.com/otto-de/tesla-cachefile): Read and write a cachefile. Locally or in hdfs.

More features will be released at a later time as separate addons.

## FAQ

**Q:** Is it any good? **A:** Yes.

**Q:** Why tesla? **A:** It's a reference to the ingenious scientist and inventor.

**Q:** Are there alternatives? **A:** Yes. You might want to look at [modularity.org](https://modularity.org/), [system](https://github.com/danielsz/system) and [duct](https://github.com/weavejester/duct).



## Initial Contributors

Christian Stamm, Felix Bechstein, Ralf Sigmund, Kai Brandes, Florian Weyandt

## License
Released under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.
