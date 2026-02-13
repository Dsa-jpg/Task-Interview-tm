# Rate-Limiter - návrh a implementace (CZ)

[![en](https://img.shields.io/badge/lang-en-red.svg)](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/README.md)


## Zadání

Cílem je navrhnout a implementovat Rate Limiter pro REST API službu, jež bude omezovat počet požadavků na klienta v daném časovém intervalu.
Součastí zadaní je návrh pro Single-node i Multi-node prostředí.

## Postup 


Bez omezení počtu požadavků na službu může dojít:

- přetížení aplikační vyrstvy
- využítí všech systémových zdrojů (CPU, RAM, DB connection Pool)
- zvýšení latence pro uživatele
- zneužití API (např.: web scraping)
- nárůstu provozních nákladů, pokud služba využívá externí placené API nebo cloud zdroje, jež jsou učtované za provedené dotazy (pay-per-request model).

## Základní design (Single node) - jedna instance


***1.1 Definice API***

Návrh jednoduchého rozhraní pro Rate Limiter za učelem znovu použitelnosti a
oddělení logiky rate limiteru od business logiky aplikace.

```java
public interface RateLimiter {
    /*
     * @param clientId - identifikátor klienta (např.: api-key, userID nebo IP adresa) 
     * @return true pokud je požadavek povolen na zakladně algoritmu,
     *         false pokud by byl překročen limit
     */
    boolean allowRequest(String clientId); 
}
```

Celá implementace na odkazu: [Rate Limiter]()

***1.2 Volba Algoritmu***

Před samotným výběrem konkretního algoritmu (např.: Token Bucket, Leaky Bucket, Fixed Window Counter) je potřeba nejdříve stanovit a
***pochopit charakteristiku dané služby***.

Bez této analýzy nelze určit optimalní algoritmus, protože každý z nich řeší jíný problém a zárověň mají odlišné trade-off mezi přesností,
výkonem a složitostí implementace daného algoritmu.

- ***Traffic pattern*** - zjistit, zda je provoz služby konstatní nebo dochází k špičkám v určítých hodinách.
- ***Požadavky na systém*** - pokud zvolím komplexní algoritmus pro řešení, jež by vyžadovalo jednoduchý scenář, mohu zbůsobit zvýšení latence a snížít výkon služby pro uživatele.
- ***Škálovaní a Flexibilita*** - potřebuji taky přemýšlet do budoucna, aby byl algoritmus vhodný při expanzi systém. (tzn. zda služba bude na jedné instanci nebo budu horizontalně škalovat atd.)

Pro učel tohoto ůkolu bych vybral pro single node ***Fixed Window Counter***, protože:

- je jednoduchý na implementaci
- má nízkou paměťovou náročnost
- je vhodný pro základní omezení počtu požadavků v definovaném časovém intervalu 

Jsem si vědom, že tento algoritmus má problém při špičkách v provozu a pokud by systém vyžadoval přesnost zvažoval bych mezi Token Bucket nebo Sliding Window algoritmu.


***1.3 Datová struktura a Concurrency***

Pro ukladání jednotlivých stavů klientů bych zvolil `ConcurrentHashMap<K,V>`,protože se jedná o thread safe implementace `Map<K,V>`, která umožňuje bezpečný souběžný přístup z více vláken.
`ConcurrentHashMap` umožňuje bezpečně více vláknům číst a zapisovat souběžně. Minimalizuje potřebu globalní synchronizace celé mapy.

```java
ConcurrentHashMap<String, RequestCounter> clientsMap; // String reprezetuje id uzivatele 
```

```java
record RequestCounter(AtomicInteger count, long startTime){}
```
> [!NOTE]  
> `AtomicInteger` používám pro atomickou inkrementaci hodnoty `count`, aby nedocházeno k race conditions při paralelismu.

## Distribuovaný systém (Multi node)

***2.1 Problém***

***2.2 Řešení***

## Integrace a produkční nasazení

***3.1 Konfigurace***

***3.2 Testovaní***

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