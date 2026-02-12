# Rate-Limiter - design and implementation (ENG)

[![cz](https://img.shields.io/badge/lang-cz-blue.svg)](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/README.cz.md)

## Task

The goal is to design and implement Rate Limiter for REST API service, which will be limiting number of requests per client in given time.
Part of the task is to design it in away to be correct either in Single-node or Multi-node environment.

## Base design (Single node) - one instance

## Distributed system (Multi node)

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

***1. First solution would be to use setter injection i.e. i can create classes without a need to pass dependency in their constructor.***

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
filled in init contextual phase.***


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


