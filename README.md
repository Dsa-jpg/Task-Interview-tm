# Rate-Limiter - návrh a implementace (CZ)

## Zadání

Cílem je navrhnout a implementovat Rate Limiter pro REST API službu, jež bude omezovat počet požadavků na klienta v daném časovém intervalu. 
Součastí zadaní je návrh pro Single-node i Multi-node prostředí.

## Základní design (Single node)



## Distribuovaný systém (Multi node)


---

## Řešení cyklických závislotí

Pokud třída A zavisí na třídě B a zároveň závisí na třídě A. Tato závislost vytváří problémy v code base, a to 
např.: špatná udržitelnost jelikož moduly jsou uzce propojeny - tudíž nelze upravit nebo znovu použít, aniž by to ovlivnilo ten druhý.


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

***1. První řešení by bylo použití setter injection tzn. že můžu vytvořit třídy bez nutnosti předaní závislostí v konstruktoru***

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

***2. Druhé řešení je pomocí abstrakce (interfejs) a jednosměrné závislosti.***

Cílem je odstranit obousměrnou závislost mezi konkretními třídami.

* `ClassB` zavisí na rozhraní
* `ClassA` implementuje toto rozhraní
* `ClassA` již nemá přímou závislost na `ClassB`

Podrobné řešení je zde:
[Řešení pomocí abstrakce](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/cyclic-dependency/Main.java)


***Bonus-Spring***

V Spring framework, je několik možností, jak řesit cyklické závisloti. 

> [!IMPORTANT]  
> Řešení pomocí `@Lazy` nebo setter injection může problém pouze obcházát.
> Pokud se např.: jedná o bussiness logiku, je lepší zvážit redisign a odstranit samotnou příčinu cyklu.

***1. Můžeme použít anotaci `@Lazy` v konstruktoru, kde provádím DI (dependency injection). Toto řešení umožní Springu vytvořit proxy pro dependency, která je vyřešena v runtime.***

Příklad: 

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
***2. Možné použítí setter injection s anotací `@Autowired` -> ta injektuje závislosti při vytváření `Bean` bez závislosti, která je doplněna ve fázi inicializace kontextu.***


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

---

# Rate-Limiter - design and implementation (ENG)

## Task

The goal is to design and implement Rate Limiter for REST API service, which will be limiting number of requests per client in given time.
Part of the task is to design it in away to be correct either in Single-node or Multi-node environment.

## Base design (Single node)

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


