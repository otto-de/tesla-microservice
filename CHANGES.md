## Changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. This section will document changes and give instructions on breaking ones. Likely you will find corresponding changes in [tesla-examples](https://github.com/otto-de/tesla-examples).

### 0.15.0
Changed securing of internal endpoints. Moved from providing separate auth-functions
to components app-status and metering to an auth-middleware provided to the base-system, 
see [Securing internal info endpoints](https://github.com/otto-de/tesla-microservice#securing-internal-info-endpoints)

### 0.14.0
This release cleans up remnants of past eperiments and unused functionality. This leads to breaking changes. 

- Remove ```register-timed-handler``` from ```handler``` namespace. Use ```goo/timing-middleware``` instead.
- Remove ```register-response-fn``` from ```handler``` namespace. This was only used internally.
- Move internally used middlewares to separate namespaces.
- Remove support for reporting via graphite from ```metering``` namespace. Only support prometheus reporting via goo and iapetos for the moment.
- Remove ```SchedulerPool``` protocol from ```scheduler``` namespace. Use ```(:pool scheduler)``` instead of ```(SchedulerPool/pool scheduler)```
           


### 0.11.0
Utilize the iapetos library as main metrics library. Tesla-Microservice is now able to report to graphite as well as prometheus.
For configuration of graphite and prometheus reporters please see the updated README.

de.otto.tesla.metrics.prometheus.core now provides some useful instrumentation functions/macros.

### 0.8.0

You are now able to override the name of the base config file via the runtime config. The following example will make the 
configuring component disgregard  ```default.edn``` and use ```not-default.edn``` instead. This might be useful when deploying several applications from one repo.

```edn
{
    :default-cfg-file-name "not-default"
}
```    

### 0.6.0

The behaviour of loading configuration changed. 

* When using configuration via `properties` files, system properties and environment variables are not loaded by default any more. Use `:merge-env-to-properties-config true` in runtime config to achieve prior behaviour.
* For the config-file `application.edn`/`application.properties` (name can be overriden by env-var `$CONFIG_FILE`)
 is now with preference loaded as a resource from classpath. If the resource is not found, it is tried to load it as a file.

### 0.5.0  

The scheduler is now part of the tesla-base-system.  
Per default no threads are kept in the thread-pool it manages.

### 0.4.0

The scheduler does not have the `de.otto.tesla.stateful.scheduler/schedule` function anymore.  
Instead it only wraps the overtone pool and provides it via `de.otto.tesla.stateful.scheduler/pool`.  
The pool then can be used with the overtone API like that:

```clj
(overtone.at-at/every 100 #(println "Hello world") (de.otto.tesla.stateful.scheduler/pool scheduler) :desc "HelloWord Task")
```

### 0.1.24

Config can be provided via EDN-files.

Those files are looked up and merged:

* `default.edn`
* `{your-custom}.edn`
* `local.edn`

The `{your-custom.edn}` can be specified via a ENV-variable named `$CONFIG_FILE`. All
EDN-config-files have to be located somewhere in the class path.

Even though the old properties-files are considered deprecated and will go away with 
future releases, you can still use them, if you specify `:property-file-preferred` in the
runtime-config of your system:

```edn
{
    :property-file-preferred true
}
```    

### 0.1.17

Fix wrapping of middleware to not apply to all routes in the application, which created problems with POST-request.

### 0.1.16

Speedup of unit-tests (and possibly runtime behaviour) by simpler implmentation of the `:keep-alive`-component.

### 0.1.15
The function ```de.otto.tesla.system/start-system``` is renamed to ```start```, ```de.otto.tesla.system/empty-system``` is renamed to ```base-system```. 

_tesla-microservice_ does not come with an embedded jetty server out of the box anymore. 

To go on with jetty as before, add the new dependency in ```project.clj```:

```clojure
  [de.otto/tesla-microservice "0.1.15"]
  [de.otto/tesla-jetty "0.1.0"]
``` 

Add the server to your system before you start it. Pass any additional dependencies of the server (```:example-page``` in this case).

```clojure
(system/start (serving-with-jetty/add-server (example-system {}) :example-page))
```

A working example for this is in the [simple-example](https://github.com/otto-de/tesla-examples/tree/master/simple-example). 
You can also use the ```->```-threading macro as demonstrated in the [mongo-example](https://github.com/otto-de/tesla-examples/tree/master/mongo-example).  

### 0.1.14
The `routes`-component was abandoned in favour of the `handler`-component.
In the ring library, handlers are the thing to push around (wrapping routes and middleware). You can choose your routing library now. Instead of [compojure](https://github.com/weavejester/compojure) you could also use e.g. [bidi](https://github.com/juxt/bidi).

Change components relying on the old ```routes```-component should be trivial: Instead of adding a vector of (compojure)-routes using ```de.otto.tesla.stateful.routes/register-routes```,

```clojure
      (routes/register-routes
        routes
        [(c/GET "/test" [] (test-fn))])
```

just add a single ring handler using ```de.otto.tesla.stateful.handler/register-handler``` like this:

```clojure
      (handlers/register-handler
        handler
        (c/routes (c/GET "/test" [] (test-fn))))
```

Add multiple routes like this:

```clojure
      (handlers/register-handler
        handler
        (c/routes (c/GET "/route1" [] (test-fn))
                  (c/GET "/route1" [] (test-fn2))))
```


Note that the keyword for the dependency changed from ```:routes``` to ```:handler``` in the base system.


### 0.1.13
Specific logging-dependencies and the escaping-messageconverter have been removed. You now have to (read: you are able to) configure logging yourself in your app. To exactly restore the old behaviour add these dependencies to you own application:

```clojure
[org.slf4j/slf4j-api "1.7.12"]
[ch.qos.logback/logback-core "1.1.3"]
[ch.qos.logback/logback-classic "1.1.3"]
[de.otto/escaping-messageconverter "0.1.1"]
```

in your ```logback.xml``` replace
```xml
<conversionRule conversionWord="mescaped"
                       converterClass="de.otto.tesla.util.escapingmessageconverter" />
```

with

```xml
<conversionRule conversionWord="mescaped"
                       converterClass="de.otto.util.escapingmessageconverter" />
```


