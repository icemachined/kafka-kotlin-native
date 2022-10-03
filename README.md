# Kafka-Kotlin-Native

## <img src="/kkn.png" width="174px"/>

![Releases](https://img.shields.io/github/v/release/icemachined/kafka-kotlin-native)
![Maven Central](https://img.shields.io/maven-central/v/com.icemachined/kafka-client)
![License](https://img.shields.io/github/license/icemachined/kafka-kotlin-native)
![Build and test](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/build_and_test.yml/badge.svg?branch=main)
![Lines of code](https://img.shields.io/tokei/lines/github/icemachined/kafka-kotlin-native)
![Hits-of-Code](https://hitsofcode.com/github/icemachined/kafka-kotlin-native?branch=main)
![GitHub repo size](https://img.shields.io/github/repo-size/icemachined/kafka-kotlin-native)
![Run deteKT](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/detekt.yml/badge.svg?branch=main)
![Run diKTat](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/diktat.yml/badge.svg?branch=main)

Fully Native [Apache Kafka](https://kafka.apache.org/) client for [Kotlin Native](https://kotlinlang.org/docs/native-overview.html).
Uses native [c-interop](https://kotlinlang.org/docs/native-c-interop.html) with highly performant and reliable [librdkafka](https://github.com/edenhill/librdkafka) c client library. 
This library contains no JVM dependencies, no jvm runtime required.
It uses [Kotlin Native memory model](https://kotlinlang.org/docs/multiplatform-mobile-concurrency-overview.html) and 
[Multithreaded coroutines](https://kotlinlang.org/docs/multiplatform-mobile-concurrency-and-coroutines.html#multithreaded-coroutines) 
for non-blocking interaction with native callbacks and asynchronous workers. 


## Contribution
We will be glad if you will test `kafka-kotlin-native` or contribute to this project.
In case you don't have much time for this - at least spend 5 seconds to give us a star to attract other contributors!

**Thanks!** :pray: :partying_face:

## Acknowledgement
Special thanks to those awesome developers who give us great suggestions, help us to maintain and improve this project:
@olonho, @akuleshov7.

## Supported platforms
The code has both **common** and **native** part. </br>
It can be built for each platform for which librdkafka has support. 
Currently this is Linux, Windows and Mac OSX. </br>
For more information about platforms and how to install librdkafka see [librdkafka installation](https://github.com/edenhill/librdkafka#installation) 

## Dependency
The library is hosted on the [Maven Central](https://search.maven.org/artifact/com.icemachined/kafka-client).
To import `kafka-kotlin-native` library you need to add following dependencies to your code:
<details>
<summary>Maven</summary>

```pom
<dependency>
  <groupId>com.icemachined</groupId>
  <artifactId>kafka-client</artifactId>
  <version>0.1.0</version>
</dependency>
```
</details>

<details>
<summary>Gradle Groovy</summary>

```groovy
implementation 'com.icemachined:kafka-client:0.1.0'
```
</details>

<details>
<summary>Gradle Kotlin</summary>

```kotlin
implementation("com.icemachined:kafka-client:0.1.0")
```
</details>

## Features

* Synchronous and asynchronous send
* Leverages kotlin-native coroutines and memory model
* Polling kafka consumer
* Parallel polling kafka consumer
* Headers enrichment
* Error handling extension points
* Possibility to leverage kotlinx.serialization features


## How to use

See example of usage in [example project](https://github.com/icemachined/kafka-client-test)
This example shows how to start/stop producer and consimer and how to configure it

## Configuration

* [librdkafka configuration](https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md) properties,
which you can pass to producer and consumer constructor </br>
* Serializer/Deserializer for key and value.
