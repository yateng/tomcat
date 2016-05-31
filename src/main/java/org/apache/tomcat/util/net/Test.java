package org.apache.tomcat.util.net;

public class Test {

    public static void main(String[] args) {
        ArrStack as = new ArrStack(5);
        as.push(new Arr(1));
        as.push(new Arr(2));
        as.push(new Arr(3));
        as.push(new Arr(4));
        as.push(new Arr(5));
        as.pop();
        as.push(new Arr(6));

        System.out.println(as);
    }
}

class ArrStack {

    Arr r[];

    int size;

    public ArrStack(int size) {
        r = new Arr[size];
    }

    public Arr pop() {
        return r[--size];
    }

    public void push(Arr data) {
        r[size++] = data;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int z = 0; z < size; z++) {
            sb.append(r[z]).append(" ");
        }
        return sb.toString();
    }

}

class Arr {

    private int n;

    public Arr(int i) {
        this.n = i;
    }

    public String toString() {
        return n + "";
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

}
