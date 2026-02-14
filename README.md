# Rate-Limiter - design and implementation (ENG)

[![cz](https://img.shields.io/badge/lang-cz-blue.svg)](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/README.cz.md) <- Click to change language

## Task

The goal is to design and implement Rate Limiter for REST API service, which will be limiting number of requests per client in given time.
Part of the task is to design it in away to be correct either in Single-node or Multi-node environment.


## Approach


Without limiting number of requests per user it can lead to following:

- overload of application layer
- exceeding the limits of system resources (CPU, RAM, DB connection Pool)
- rapid increase of latency for users
- exploit API (E.g. web scraping)
- steep increase of services costs, if service is using external paid API or cloud resources, which are being billed per request (pay-per-request model).


## Base design (Single node) - one instance


***1.1 Definition of API***

Design of simple API for Rate Limiter for the purpose of reusability and
decoupling logic of Rate Limiter from business logic.

```java
public interface RateLimiter {
    /*
     * @param clientId - clients identifier (E.g. api-key, userID or IP address) 
     * @return true if the request is allowed based on the algorithm,
     *         false if the limit was exceeded.
     */
    boolean allowRequest(String clientId); 
}
```
Whole implementation is available on this link: [Rate Limiter](https://github.com/Dsa-jpg/RateLimiterInverview/tree/master/src/main/java/org/nachtman/ratelimiterdemo)

***1.2 Choice of algorithm***

Before choosing a specific algorithm (E.g. Token Bucket, Leaky Bucket, Fixed Window Counter) there is need to propose and ***understand characteristics of whole service***.

Without this analysis I would not be able to decide suitable algorithm, because every algorithm excels and solves other problems and in fact has different trade-offs. 
(E.g. accuracy, performance and complexity of algorithm implementation )

- ***Traffic pattern*** - find out, if the service traffic is constant or if there is sudden peaks in specific times.
- ***Requirements for system*** - if I choose complex algorithm for solution, which would have required simpler approach. It can lead to latency rise.
- ***Scaling and Flexibility*** - I need to think about it at coarser scale. In a way that I would not be forced in future to reconsider whole approach. (i.e potential rise of user population -> horizontal scaling etc. ) 

For purpose of this task I have chosen for single node***Fixed Window Counter*** algorithm, because:

- is simple for implementation 
- has low memory demand 
- is suitable for base limitation for number of requests per user in given time period. 


I'm well aware that this algorithm has its shortcomings (e.g. in traffic peaks or if accuracy would be required I would consider these two algorithms Token Bucket and Sliding Window ).


***1.3 Data structures and Concurrency*** 

For saving every single state of client requests I would choose `ConcurrentHashMap<K,V>`, because it is thread safe implementation of `Map<K,V>`
,which provides safe concurrent access from multiple threads. `ConcurrentHashMap` allows multiple threads to safely write and read concurrently. While it minimalizes the need of global synchronization of whole map.

```java
ConcurrentHashMap<String, RequestCounter> clientsMap; // String represents id of user
```

```java
record RequestCounter(AtomicInteger count, long startTime){}
```
> [!NOTE]  
> While `ConcurrentHashMap` ensures thread-safe access to the map itself, atomic updates to `RequestCounter` are necessary to avoid race conditions.



## Distributed system (Multi node)

***2.1 Issue***


With multiple instances of the application, a problem arises because the state of the rate limiter is stored locally on each instance (E.g. in a `ConcurrentHashMap`). As a result, each instance maintains its own memory and counter, and there is no shared state between them.

E.g. there is limit set for 100 requests/minute per user and application is running in 3 instances, client can sent close to 300 req/min.


***2.2 Solution***

To solve this issue, I would like to propose global shared database (E.g. `Redis`), which will atomically change counter for each client across all instances.

- **Atomic operations:** For chosen Fixed Window algoritm I would use built-in commands `INCR` a `EXPIRE` (TTL - time-to-live). This atomic operations solves the issues with race conditions between instances. 
- **Reactive approach:** I would introduce reactive approach specifically in distributed environment. This way solves communication to Redis won't block working threads. From my basic experience with reactive programing in Quarkus with Munity framework (objects Uni a Multi). Even thought I have not used reactive approach in Spring yet. I have found out there is similar convention Mono/Flux.

![Alt Text](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/RateLimiter.gif)

## Integration and Production deployment

***3.1 Configuration***

The configuration will be store in separate database as source of truth for all instances. At the same time, each instance will maintain a local cache with a randomized TTL (e.g., 30–45 seconds) to prevent simultaneous expiration on multiple instances.

The flow would work as follows: when a request arrives, the application first checks the local cache for a valid configuration. If the configuration is missing or expired, a query is made to the database to retrieve the current configuration and update the local cache.

> [!NOTE]  
> Or possibly save config as `ConfigMap/Secret` in K8s and preform `Rolling Update`.

![Alt Text](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/RateLimiter.gif)

***3.2 Testing***

***3.2.1 Unit Tests***

***3.2.2 Concurrency Tests***

***3.2.3 Integration Tests***

***3.2.4 Load Testing***

---

## Resolving cyclic dependencies

If class A depends on Class B and vice versa. This circular dependency creates problems in code base, for e.g. bad sustainability
in which the modules are tightly connected, therefore we are unable to change a signature or reuse without it effecting the other one.

```java
class ClassA  {
    
    public ClassA (ClassB b){
        this.b = b;
    }

}

class ClassB {
    
    public ClassB (ClassA a){
        this.a = a;
    }
    
}
```

***1. First solution would be to use setter injection i.e. I can create classes without a need to pass dependency in their constructor.***

```java
class ClassA  {
    private ClassB b;
    
    public void setB (ClassB b){
        this.b = b;
    }

}

class ClassB {
    private ClassA a;
    
    public void setA (ClassA a){
        this.a = a;
    }
    
}

// main
public static void main(String[] args) {
    ClassA a = new ClassA();
    ClassB b = new ClassB();
    a.setB(b);
    b.setA(a);
}
```

***2. Second solution is overcoming thanks to abstraction and one-way dependency.***

The goal is to completely get rid of bidirectional dependency between these classes.

* `ClassB` depends on interfejs 
* `ClassA` implements this interfejs
* `ClassA` already does not have direct dependence on `ClassB`

Complete solution is available here:
[Solution with abstraction](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/cyclic-dependency/Main.java)

***Bonus-Spring***

In Spring framework there is few options how to solve circular dependencies.

> [!IMPORTANT]  
> Solution with `@Lazy` annotation or setter injection can lead to ignoring root cause.
> E.g. if business logic is the case, there is better to consider redesign and removing the cause of the cycle.


***1. With use of annotation`@Lazy` inside of constructor, where I perform DI (dependency injection).
This solution will provide Spring ability to create proxy for dependency, which will be resolved in runtime.***

Example:

```java
@Component
public class ClassA {
    private final ClassB b;
    
    public ClassA(@Lazy ClassB b) {
        this.b = b;
    }
}

@Component
public class ClassB {
    private final ClassA a;

    public ClassB( ClassA a) {
        this.a = a;
    }
}
```
***2. Possible usage of setter injection with annotation `@Autowired` -> 
this approach injects dependency during the creation of `Bean` without dependencies, which are 
filled in initial contextual phase.***


```java
@Component
public class ClassA {
    private ClassB b;

    @Autowired
    public void setB(ClassB b) {
        this.b = b;
    }
}

@Component
public class ClassB {
    private ClassA a;

    @Autowired
    public void setA(ClassA a) {
        this.a = a;
    }
}
```


