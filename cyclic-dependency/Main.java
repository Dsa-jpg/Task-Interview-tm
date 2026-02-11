/*
 *   The first approach would be to use DIP - dependency inversion pattern
 *   Classes depend on interfaces (decoupling)
 *   Setter injection for setting dependency after creation of objects
 */


class ClassA implements IA {

    private IB b;

    public void setB(IB b) {this.b = b;}

    @Override
    public void doA (){
        System.out.println("A is doing something");
        if (b != null)  b.doB();

    }


}

class ClassB implements IB{
    private IC c;

    public void setC(IC c) {this.c = c;}

    @Override
    public void doB() {
        System.out.println("B is doing something");
        if (c != null)c.doC();
    }

}

class ClassC implements IC {
    private IA a;
    public void setA(IA a) { this.a = a; }

    @Override
    public void doC() {
        System.out.println("C is doing something");
    }
}


interface IA { void doA(); }
interface IB { void doB(); }
interface IC { void doC(); }

public class Main{


    public static void main(String[] args) {


        ClassA a = new ClassA();
        ClassB b = new ClassB();
        ClassC c = new ClassC();

        a.setB(b);
        b.setC(c);
        c.setA(a);

        a.doA();
    }

}