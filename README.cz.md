# Rate-Limiter - návrh a implementace (CZ)

[![en](https://img.shields.io/badge/lang-en-red.svg)](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/README.md) <- Click to change language


## Zadání

Cílem je navrhnout a implementovat Rate Limiter pro REST API službu, jež bude omezovat počet požadavků na klienta v daném časovém intervalu.
Součastí zadaní je návrh pro Single-node i Multi-node prostředí.

## Postup 


Bez omezení počtu požadavků na službu může dojít k :

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

Celá implementace na odkazu: [Rate Limiter](https://github.com/Dsa-jpg/RateLimiterInverview/tree/master/src/main/java/org/nachtman/ratelimiterdemo)

***1.2 Volba Algoritmu***

Před samotným výběrem konkrétního algoritmu (např.: Token Bucket, Leaky Bucket, Fixed Window Counter), je potřeba nejdříve stanovit a
***pochopit charakteristiku dané služby***.

Bez této analýzy nelze určit optimalní algoritmus, protože každý z nich řeší jíný problém a zároveň má odlišné trade-off mezi přesností,
výkonem a složitostí implementace daného algoritmu.

- ***Traffic pattern*** - zjistit, zda je provoz služby konstatní nebo dochází k špičkám v určítých hodinách.
- ***Požadavky na systém*** - pokud zvolím komplexní algoritmus pro řešení, jež by vyžadovalo jednoduchý scenář, mohu zbůsobit zvýšení latence a snížít výkon služby pro uživatele.
- ***Škálovaní a Flexibilita*** - potřebuji také přemýšlet do budoucna, aby byl algoritmus vhodný při expanzi systém. (tzn. zda služba bude na jedné instanci nebo budu horizontalně škalovat atd.)

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

Při nasazení aplikace ve více instancích dochází k problému, pokud je stav rate limiteru uložen pouze lokálně (například v `ConcurrentHashMap`). Každá instance má totiž vlastní paměť a vlastní counter, takže mezi nimi neexistuje sdílený stav.

V důsledku toho limit neplatí globálně, ale pouze na úrovni jednotlivé instance. Pokud je například nastaven limit 100 požadavků za minutu a aplikace běží na třech instancích, může klient ve výsledku odeslat až 300 požadavků za minutu.

Aby byl limit skutečně globální napříč všemi instancemi, je nutné použít sdílené úložiště (např. Redis), které bude držet centralizovaný counter.

***2.2 Řešení***

Řešením tohoto problému je sdílení stavu pro jednotlivé klienty mezi všemi instancemi prostřednictvím rychlé in-memory databáze `Redis`.

- **Princip:** Namísto lokální mapy by aplikace komunikovala s Redisem, který by sloužil jako centrální čítač. Každý příchozí požadavek by provedl atomickou operaci nad klíčem klienta.
- **Atomické operace:** Pro algoritmus Fixed Window bych využil příkazy `INCR` a `EXPIRE` (TTL - time-to-live). Tyto operace jsou v Redisu atomické, což řeší problém race conditions mezi instancemi.
- **Reaktivní přístup:** V distribuovaném prostředí navrhuji reaktivní přístup, aby síťová latence Redisu neblokovala pracovní vlákna. Mám základní zkušenost s reaktivním programováním v Quarkusu pomocí knihovny Mutiny (objekty Uni a Multi). I když jsem zatím přímo nepoužíval knihovnu Lettuce, vím, že ve Springu plní stejnou roli – funguje jako neblokující ovladač, který místo Uni vrací Mono. Ten princip neblokujícího I/O je v obou světech totožný.

![Alt Text](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/RateLimiter.gif)

## Integrace a produkční nasazení

***3.1 Konfigurace***

Konfiguraci bych uložil do databáze jako zdroj pravdy pro vsechny instance. Zároveň bych na každé instanci měl lokalní cache a TTL lokalní cache bych lehce randomizoval v určitém intervalu (např. 30–45 s), aby nedocházelo k hromadné expiraci na všech instancích zároveň..

Pak to bude fungovat ,že přijde request a nejdříve se podívám do lokalní cache pokud tam je config a je validní tak ho použiji. V případě, že config tam není nebo má vypršelou platnost, tak se podívám do DB, kde mám veškerou konfiguraci. Tím je možné měnit limity dynamicky bez restartu aplikace.

> [!NOTE]  
> Nebo můžu config uložit jako `ConfigMap/Secret` v K8s a při aktualizaci configu provedu `Rolling Update`.

![Alt Text](https://github.com/Dsa-jpg/Task-Interview-tm/blob/main/SequenceDiagram.gif)

***3.2 Testovaní*** 

3.2.1 Unit Tests

Použil bych unit testy k ověření, že algoritmus funguje správně v následujících scénářích:

-Prvních N požadavků vrací HTTP 200 OK.
-(N+1) požadavek vrací HTTP 429 Too Many Requests.
-Limit se správně resetuje po uplynutí každého časového okna.

3.2.2 Concurrency Tests

Otestoval bych thread-safety Rate Limiteru pomocí více paralelních vláken. Použil bych `ExecutorServic` pro spuštění více vláken současně a `CountDownLatch`, aby všechna vlákna začala volat `allowRequest(clientId)` ve stejný okamžik. Cílem je ověřit, že i při současných požadavcích Rate Limiter nikdy nepovolí více requestů než je nastavený limit.

3.2.4 Load Testing

Použil bych Apache JMeter pro simulaci více souběžných uživatelů. Nakonfiguroval bych Thread Group s vysokým počtem vláken a posílal požadavky na endpoint s Rate Limiterem. Poté bych ověřil, že počet odpovědí HTTP 200 nikdy nepřesáhne nastavený limit a všechny zbývající požadavky vrací HTTP 429.

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
