## Breaking changes

_tesla-microservice_ is used for a number of different services now. Still it is a work in progress. This section will document breaking changes.

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


