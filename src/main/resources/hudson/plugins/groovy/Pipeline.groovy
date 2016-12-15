public class Pipeline {
    public static Object input() {
        f('input.ser').withObjectInputStream {it.readObject()}
    }
    public static void output(Object o) {
        f('output.ser').withObjectOutputStream {it.writeObject(o)}
    }
    private static File f(String name) {new File(System.getProperty('pipeline.io.dir'), name)}
    private Pipeline() {}
}
