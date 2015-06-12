## Breaking changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. This section will document breaking changes. Likely you will find corresponding changes in [tesla-examples](https://github.com/otto-de/tesla-examples).

### 0.1.17
Replaced property based configuration by EDN based configuration using [gorillalabs/config](https://github.com/gorillalabs/config).
You need to update your configuration files.

### 0.1.16
Moved config parameters:

 * ```status.url``` to ```status.path```
 * and ```health.url``` to ```health.path```.

You do not need to change anything if you did not overwrite the default behaviour.

If you need to get info from the config component, there's a new function config for that.

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


