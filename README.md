# Rate-Limiter - design and implementation (ENG)

[![cz](https://img.shields.io/badge/lang-cz-blue.svg)](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/README.cz.md)

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

***2.2 Solution***


## Integration and Production deployment

***3.1 Configuration***

***3.2 Testing***

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


