## Breaking changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. This section will document breaking changes.

### 0.1.4
The `routes`-component was abandoned in favour of the `handler`-component.
In the ring library, handlers are the thing to push around (wrapping routes and middleware).
Thus, moving from routes to handlers being managed, dependencies are untangled.

You may now choose your routing library in your components, not being tied to [compojure](https://github.com/weavejester/compojure). Go have a look at [bidi](https://github.com/juxt/bidi).
in the components that use routes.


If you have components dependant on the `routes` component, you need to follow our transition:

Instead of adding a vector of (compojure)-routes using ```de.otto.tesla.stateful.routes/register-routes```, you now add a single ring handler
 using ```de.otto.tesla.stateful.handler/register-handler handler ```

```clojure
      (de.otto.tesla.stateful.routes/register-routes
        routes
        [(c/GET "/test" [] (test-fn))])
```

```clojure
      (handlers/register-handler
        handler
        (c/routes [(c/GET "/test" [] (test-fn))]))
```


### 0.1.3
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


