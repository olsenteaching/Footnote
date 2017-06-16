package assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by po917265 on 6/3/17.
 */
public class Assembler {

    static int lineNo = 1;

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Usage: Assembler infile outfile");
            return;
        }
        Scanner scan = null;
        try {
            scan = new Scanner(new File(args[0]));
        } catch (FileNotFoundException e) {
            System.out.println("Could not open " + args[0]);
            return;
        }
        PrintStream out = null;
        try {
            out = new PrintStream(args[1]);
        } catch (FileNotFoundException e) {
            System.out.println("Could not open " + args[1]);
        }
        ArrayList<String> program = assemble(scan);
        //System.out.println(program);
        for(String s : program) {
            out.print(s + " ");
        }
        scan.close();
        out.close();
    }

    public static ArrayList<String> assemble(Scanner in) {
        lineNo = 1;
        HashMap<String, Integer> symbolTable = new HashMap<String, Integer>();
        ArrayList<String> program = new ArrayList<String>();
        ArrayList<Integer> pc = new ArrayList<Integer>();
        HashMap<Integer, String> stringLocationMap = new HashMap<Integer, String>();
        TreeMap<Integer, Integer> pc2line = new TreeMap<Integer, Integer>();
        int index = 0; //the index for variable addresses.
        int variableCount = 0;
        pc.add(0);
        boolean declare = false;
        while(in.hasNextLine()) {
            String line = in.nextLine();
            line = killComments(line).trim();
            if(line.equals("")) continue;
            if(line.equals(".declare")) {
                declare = true;
            } else if(line.equals(".begin")) {
                declare = false;
            } else if(declare) {
                String[] parts = line.split("\\s+");
                if(parts.length == 1) {
                    symbolTable.put(line, --index);
                    variableCount++;
                } else if(parts.length == 2) {
                    symbolTable.put(parts[0], parse(parts[1]));
                } else if(parts.length == 3 && parts[1].equals("length")) {
                    try {
                        int length = Integer.parseInt(parts[2]);
                        variableCount += length;
                        index -= length;
                        symbolTable.put(parts[0], index);
                    } catch(NumberFormatException nfe) {
                        System.out.println("Illegal array length constant @" + lineNo);
                    }
                } else if(parts.length >= 3 && parts[1].equals("is")) {
                    String[] pieces = line.split("\\s+", 3);
                    pieces[2] = pieces[2].substring(1, pieces[2].length() - 1); //get rid of single quotes.
                    int length = pieces[2].length() + 1;
                    variableCount += length;
                    index -= length;
                    symbolTable.put(pieces[0], index);
                    stringLocationMap.put(index, pieces[2]);
                } else {
                    System.out.println("Illegal constant or variable declaration @" + lineNo);
                }
            } else if(line.startsWith(":")) {
                symbolTable.put(line, pc.get(0));
            } else {
                String[] parts = line.split("\\s+");
                syntaxCheck(parts);
                pc2line.put(pc.get(0), lineNo);
                assemble(parts, symbolTable, program, pc);
            }
            lineNo++;
        }
        for(int i = 0; i < variableCount; i++) {
            program.add("0");
        }
        for(int i : stringLocationMap.keySet()) {
            String s = stringLocationMap.get(i);
            for(char c : s.toCharArray()) {
                program.set(program.size() + i, "" + (int)c);
                i++;
            }
            program.set(program.size() + i, "" + 0);
        }
        for(int i = 0; i < program.size(); i++) {
            if(program.get(i).startsWith(":")) {
                Integer address = symbolTable.get(program.get(i));
                if(address == null) {
                    System.out.println("Undefined branch label @" + pc2line.get(pc2line.floorKey(i)));
                }
                program.set(i, "" + address);
            } else if(program.get(i).startsWith("!")) {
                program.set(i, program.get(i).substring(1));
                Integer address = symbolTable.get(program.get(i));
                if(address == null) {
                    System.out.println("Undefined array name: @" + pc2line.get(pc2line.floorKey(i)));
                } else {
                    program.set(i, "" + (program.size() + address));
                }
            }
        }
        in.close();
        return program;
    }

    private static void syntaxCheck(String[] parts) {
        try { //try to syntax check the instruction.
            Instruction instruction = Instruction.valueOf(parts[0]);
            instruction.checkSyntax(parts, System.out, lineNo);
        } catch(IllegalArgumentException iae) {  //invalid instruction name.  Report this.
            System.out.println("Undefined Instruction " + parts[0] + " @" + lineNo);
        }
    }

    private static void assemble(String[] parts, HashMap<String, Integer> symbolTable, ArrayList<String> program, ArrayList<Integer> pc) {
        try {
            Instruction instruction = Instruction.valueOf(parts[0]);
            String[] assembled = instruction.assemble(parts, symbolTable, pc.get(0), System.out, lineNo);
            //System.out.println(Arrays.toString(assembled));
            pc.set(0, pc.get(0) + assembled.length);
            for (String i : assembled) {
                program.add(i);
            }
        } catch (IllegalArgumentException iae) {
            //do nothing.  This error doesn't really matter.
        }
    }

    private static String killComments(String line) {
        if(line.startsWith(";")) return "";
        if(! line.contains(";")) {
            return line;
        }
        return line.substring(0, line.indexOf(';') - 1);
    }

    public static int parse(String number) {
        try {
            return Integer.parseInt(number);
        } catch(Exception e) {
            System.out.println("Invalid integer value @" + lineNo);
            return Integer.MIN_VALUE;
        }
    }
}
