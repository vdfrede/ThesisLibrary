
package thesislib;

import java.lang.reflect.Method;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Diagram {
    private ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    private HashMap<String, HashMap<String, String>> relationships = new HashMap<String, HashMap<String, String>>();
    private String diagramTitle = "";
    private boolean includePackages = false;
    private ArrayList<String> notes = new ArrayList<>();
    private String fileOut;

    public Diagram(String title, String outputFileName, Class<?>... args) {
        diagramTitle = title;
        fileOut = outputFileName;
        for (Class<?> class1 : args) {
            classes.add(class1);
        }
    }

    // Method that adds one class
    public void add(Class<?> newClass) {
        classes.add(newClass);
    }

    // Method that finds the Super class for a class
    private void findRelations(Class<?> c) {
        if (c.getSuperclass().getName() != null && !c.getSuperclass().getName().equals("java.lang.Object")) {
            addRelationship(c, c.getSuperclass());
        }
    }

    // Method that finds the fields of a class
    private String getFields(Class<?> c) {
        String s = "\t\n";
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            String modifier = "";
            int m = field.getModifiers();
            if (m == 1) {
                modifier = "+";
            } else if (m == 2) {
                modifier = "-";
            } else if (m == 4) {
                modifier = "#";
            }
            s = s + "\t" + modifier + field.getType().getSimpleName() + " " + field.getName() + "\t\n";
        }
        return s;
    }

    // Method that finds the methods of a class
    private String getMethods(Class<?> c) {
        String s = "\t\n";
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            s = s + "\t+" + method.getName() + "() " + method.getReturnType().getSimpleName() + "\t\n";
        }
        return s;

    }

    // Method that adds a relationship between two classes
    public void addRelationship(Class<?> c1, Class<?> c2) {
        String nameC1 = c1.getName();
        String nameC2 = c2.getName();
        if (!includePackages) {
            nameC1 = simpleName(nameC1);
            nameC2 = simpleName(nameC2);
        }
        if (relationships.containsKey(nameC1)) {
            if (!relationships.get(nameC1).containsKey(nameC2)) {
                relationships.get(nameC1).put(nameC2, determineRelationship("Extension"));
            }
        } else {
            HashMap<String, String> temp = new HashMap<>();
            temp.put(nameC2, determineRelationship("Extension"));
            relationships.put(nameC1, temp);
        }
    }

    // Following method uses overloading to make String parameter optional when
    // adding a relationship between classes
    public void addRelationship(Class<?> c1, Class<?> c2, String s) {
        String nameC1 = c1.getName();
        String nameC2 = c2.getName();
        if (!includePackages) {
            nameC1 = simpleName(nameC1);
            nameC2 = simpleName(nameC2);
        }
        if (relationships.containsKey(nameC1)) {
            relationships.get(nameC1).put(nameC2, determineRelationship(s));

        } else {
            HashMap<String, String> temp = new HashMap<>();
            temp.put(nameC2, determineRelationship(s));
            relationships.put(nameC1, temp);
        }
    }

    // Method to include or exclude package info from diagram
    public void includePackages(boolean include) {
        includePackages = include;
    }

    // Method that return the simple name of a class
    private String simpleName(String name) {
        if (!includePackages) {
            String[] temp = name.split("[.]");
            name = temp[temp.length - 1];
            return name;
        }
        return name;
    }

    // Method that puts together what should be printed to the plantUML file
    private String returnDiagramPlantUml() {
        String d = "@startuml\n!pragma layout smetana\n title " + diagramTitle + "\n";

        for (Class<?> c : classes) {
            String name = simpleName(c.getName());

            d = d + "\tClass" + " " + name + "{" + getFields(c) + "\n\t" + getMethods(c) + "\n\t}\n";
            findRelations(c);
        }
        if (relationships != null) {
            for (Entry<String, HashMap<String, String>> entry : relationships.entrySet()) {
                String nameC1 = simpleName(entry.getKey());
                for (Entry<String, String> e : entry.getValue().entrySet()) {
                    String nameC2 = simpleName(e.getKey());
                    d = d + nameC2 + e.getValue() + nameC1 + "\n";
                }
            }
            if (!notes.isEmpty()) {
                for (String s : notes) {
                    d = d + s;
                }
            }

        }
        return d + "\n@enduml";
    }

    // Method that prints the diagram to a file
    public void printDiagram() {
        try {
            FileWriter myWriter = new FileWriter(fileOut);
            myWriter.write(returnDiagramPlantUml());
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    // Method that returns syntax for different relationships
    private String determineRelationship(String s) {
        if (s.equals("Extension")) {
            return " <|-- ";
        } else if (s.equals("Composition")) {
            return " *-- ";
        } else if (s.equals("Aggregation")) {
            return " o-- ";
        }
        return "";
    }

    // Method that changes the linestyle between two classes
    public void changeLineStyle(Class<?> c1, Class<?> c2, String lineStyle) {
        addRelationship(c1, c2);
        String holder = relationships.get(c1.getName()).get(c2.getName());
        String temp[] = holder.split("(?<=-)", 2);
        String modifiedLine = temp[0] + "[" + lineStyle + "]" + temp[1];
        HashMap<String, String> t = relationships.get(c1.getName());
        t.put(c2.getName(), modifiedLine);

        relationships.put(c1.getName(), t);
    }

    // Method that adds a floating note
    public void addNote(String s) {
        String id = "N" + notes.size();
        notes.add("note " + "\"" + s + "\"" + " as " + id + "\n");
    }

    // Method that adds a note to one or more classes
    public void addNote(String s, Class<?>... args) {
        String id = "N" + notes.size();
        String note = "note " + "\"" + s + "\"" + " as " + id + "\n";
        for (Class<?> class1 : args) {
            String name;
            if (includePackages) {
                name = class1.getName();
            } else {
                name = simpleName(class1.getName());
            }
            note += "\n" + id + " .. " + name + "\n";
        }
        notes.add(note);
    }

    // Methods that exports the diagram as a file
    public void exportDiagram() {
        String command = "java -jar plantuml.jar " + fileOut;
        try {
            Process process = Runtime.getRuntime().exec(command);

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e);
        }
    }

    // Methods that exports the diagram as a file with a specific format
    public void exportDiagram(String format) {
        String command = "java -jar plantuml.jar " + fileOut + " -" + format;
        try {
            Process process = Runtime.getRuntime().exec(command);

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e);
        }
    }
}
