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

***1. První řešení by bylo použití setter injection tzn. že mužu vytvořit třídy bez nutnosti předaní závislostí v konstruktoru***

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


---

# Rate-Limiter - design and implementation (ENG)

## Task

The goal is to design and implement Rate Limiter for REST API service, which will be limiting number of requests per client in given time.
Part of the task is to design it in away to be correct either in Single-node or Multi-node environment.

## Base design (Single node)

## Distributed system (Multi node)

---

## Solution of cyclical dependencies

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
