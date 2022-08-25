# kafka-kotlin-native

## <img src="/kafka-kotlin-native.png" width="300px"/>

![Releases](https://img.shields.io/github/v/release/icemachined/kafka-kotlin-native)
![Maven Central](https://img.shields.io/maven-central/v/io.github.icemachined/icemachined/kafka-kotlin-native)
![License](https://img.shields.io/github/license/icemachined/kafka-kotlin-native)
![Build and test](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/build_and_test.yml/badge.svg?branch=main)
![Lines of code](https://img.shields.io/tokei/lines/github/icemachined/kafka-kotlin-native/)
![Hits-of-Code](https://hitsofcode.com/github/icemachined/kafka-kotlin-native)
![GitHub repo size](https://img.shields.io/github/repo-size/icemachined/kafka-kotlin-native)
![codebeat badge](https://codebeat.co/badges/0518ea49-71ed-4bfd-8dd3-62da7034eebd)
![maintainability](https://api.codeclimate.com/v1/badges/c75d2d6b0d44cea7aefe/maintainability)
![Run deteKT](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/detekt.yml/badge.svg?branch=main)
![Run diKTat](https://github.com/icemachined/kafka-kotlin-native/actions/workflows/diktat.yml/badge.svg?branch=main)

Fully Native [Apache Kafka](https://kafka.apache.org/) client for [Kotlin Native](https://kotlinlang.org/docs/native-overview.html).
Uses native [cinterop](https://kotlinlang.org/docs/native-c-interop.html) with highly performant and reliable [librdkafka](https://github.com/edenhill/librdkafka) c client library. 
This library contains no JVM dependencies, no garbage collector and other jvm stuff.
I uses [Kotlin Native memory model](https://kotlinlang.org/docs/multiplatform-mobile-concurrency-overview.html) and 
[Multithreaded coroutines](https://kotlinlang.org/docs/multiplatform-mobile-concurrency-and-coroutines.html#multithreaded-coroutines) 
for non blocking interaction with native callbacks and asyncronious workers. 


## Contribution
We will be glad if you will test `kafka-kotlin-native` or contribute to this project.
In case you don't have much time for this - at least spend 5 seconds to give us a star to attract other contributors!

**Thanks!** :pray: :partying_face:

## Acknowledgement
Special thanks to those awesome developers who give us great suggestions, help us to maintain and improve this project:
@olonho, @icemachined.

## Supported platforms
The code has both **common** and native part. 
It can be built for each platform for which librdkafka has support, this is Linux, Windows and Mac OSX.
For more information about platforms and how to install see [librdkafka installation](https://github.com/edenhill/librdkafka#installation) 

