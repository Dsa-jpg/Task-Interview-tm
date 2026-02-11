# Rate-Limiter - návrh a implementace (CZ)

## Zadání

Cílem je navrhnout a implementovat Rate Limiter pro REST API službu, jež bude omezovat počet požadavků na klienta v daném časovém intervalu. 
Součastí zadaní je návrh pro Single-node i Multi-node prostředí.

## Základní design (Single node)



## Distribuovaný systém (Multi node)


---

## Řešení cyklických závislotí



```java
class ClassC implements IC {
    private IA a;
    public void setA(IA a) { this.a = a; } // setter injection  

    @Override
    public void doC() {
        System.out.println("C is doing something");
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

## Solution of cyclical dependencies

```java
class ClassC implements IC {
    private IA a;
    public void setA(IA a) { this.a = a; } // setter injection  

    @Override
    public void doC() {
        System.out.println("C is doing something");
    }
}
```
